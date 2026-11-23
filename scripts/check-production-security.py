"""Verifica TLS y ejecuta ZAP pasivo sobre una instancia efímera en Docker."""
import argparse
import json
import os
from pathlib import Path
import secrets
import ssl
import subprocess
from tempfile import TemporaryDirectory
import time
from urllib.error import URLError
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / "build/reports/security"


def command(*args, env=None, check=True, timeout=120):
    return subprocess.run(args, env=env, check=check, timeout=timeout, encoding="utf-8", errors="replace",
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT)


def await_database(container):
    for _ in range(60):
        result = command("docker", "exec", container, "pg_isready", "-h", "127.0.0.1", check=False)
        if result.returncode == 0:
            return
        time.sleep(1)
    raise RuntimeError("PostgreSQL no inició")


def certificate(directory, password, cert_env):
    keystore = directory / "server.p12"
    cert = directory / "server.crt"
    command("keytool", "-genkeypair", "-alias", "mathtutor", "-keyalg", "RSA", "-keysize", "2048",
            "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,dns:mathtutor",
            "-validity", "1", "-storetype", "PKCS12", "-keystore", str(keystore),
            "-storepass:env", password, env=cert_env)
    command("keytool", "-exportcert", "-rfc", "-alias", "mathtutor", "-keystore", str(keystore),
            "-storepass:env", password, "-file", str(cert), env=cert_env)
    return keystore, cert


def ssl_context(cert, version):
    context = ssl.create_default_context(cafile=str(cert))
    context.minimum_version = version
    context.maximum_version = version
    return context


def verify_https(base, cert):
    context = ssl_context(cert, ssl.TLSVersion.TLSv1_2)
    for _ in range(90):
        try:
            with urlopen(base + "/actuator/health", context=context, timeout=2) as response:
                if json.load(response) == {"status": "UP"}:
                    break
        except (URLError, OSError):
            pass
        time.sleep(1)
    else:
        raise RuntimeError("La aplicación no respondió correctamente por TLS 1.2")

    with urlopen(base + "/api/csrf-token", context=ssl_context(cert, ssl.TLSVersion.TLSv1_3),
                 timeout=5) as response:
        cookie = response.headers.get("Set-Cookie", "")
        if not all(part in cookie for part in ("__Host-XSRF-TOKEN=", "Secure", "HttpOnly", "SameSite=Strict")):
            raise RuntimeError("Cookie de producción incompleta")
        if not all(response.headers.get(header) for header in ("Content-Security-Policy",
                                                                "Strict-Transport-Security", "Referrer-Policy")):
            raise RuntimeError("Cabeceras de producción incompletas")
    legacy = ssl_context(cert, ssl.TLSVersion.TLSv1_1)
    legacy.set_ciphers("DEFAULT:@SECLEVEL=0")
    try:
        with urlopen(base + "/actuator/health", context=legacy, timeout=5):
            raise RuntimeError("El servidor permite TLS 1.1")
    except URLError as error:
        if not isinstance(error.reason, ssl.SSLError):
            raise
    print("TLS 1.2 y 1.3 aceptados, TLS 1.1 rechazado. Cookie CSRF y cabeceras de producción correctas.")


def verify_zap(network, scanner):
    result = command("docker", "run", "--name", scanner, "--network", network,
                     "--user", "root", "--workdir", "/zap/wrk", "ghcr.io/zaproxy/zaproxy:stable",
                     "zap-baseline.py", "-t", "https://mathtutor:8443/visor/index.html",
                     "-J", "report.json", "-r", "report.html", "-s", "-T", "4", check=False, timeout=360)
    (REPORTS / "zap.log").write_text(result.stdout, encoding="utf-8")
    if result.returncode not in (0, 2):
        raise RuntimeError(f"ZAP terminó con código {result.returncode}. Consulta zap.log.")
    for artifact in ("report.json", "report.html"):
        command("docker", "cp", f"{scanner}:/zap/wrk/{artifact}", str(REPORTS / artifact))
    report = json.loads((REPORTS / "report.json").read_text(encoding="utf-8"))
    sites = report["site"]
    if not sites or not any(site["@host"] == "mathtutor" for site in sites):
        raise RuntimeError("ZAP no registró el visor que debía inspeccionar")
    alerts = [alert for site in sites for alert in site["alerts"]]
    if any(int(alert["riskcode"]) >= 3 for alert in alerts):
        raise RuntimeError("ZAP detectó alertas altas o críticas. Consulta report.html.")
    print(f"ZAP pasivo sin alertas altas o críticas. {len(alerts)} tipos de alerta en {REPORTS}.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, help="Ruta del bootJar de mathtutor-application")
    jar = parser.parse_args().jar.resolve(strict=True)
    REPORTS.mkdir(parents=True, exist_ok=True)
    network = "mathtutor-security-" + secrets.token_hex(6)
    database, app, scanner = (f"{network}-{name}" for name in ("db", "app", "zap"))
    env = dict(os.environ, POSTGRES_PASSWORD=secrets.token_hex(24),
               TLS_KEYSTORE_PASSWORD=secrets.token_hex(16), JWT_SECRET=secrets.token_hex(32),
               TLS_KEYSTORE="file:/run/server.p12", POSTGRES_HOST="postgres",
               POSTGRES_DB="mathtutor", SPRING_PROFILES_ACTIVE="production")
    command("docker", "network", "create", network)
    try:
        with TemporaryDirectory(dir=REPORTS) as temporary:
            keystore, cert = certificate(Path(temporary), "TLS_KEYSTORE_PASSWORD", env)
            command("docker", "run", "-d", "--name", database, "--network", network,
                    "--network-alias", "postgres", "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_DB",
                    "pgvector/pgvector:pg16", env=env)
            await_database(database)
            variables = ("POSTGRES_PASSWORD", "TLS_KEYSTORE_PASSWORD", "JWT_SECRET", "TLS_KEYSTORE",
                         "POSTGRES_HOST", "POSTGRES_DB", "SPRING_PROFILES_ACTIVE")
            command("docker", "run", "-d", "--name", app, "--network", network, "--network-alias", "mathtutor",
                    "-p", "127.0.0.1::8443", "--workdir", "/app", "--entrypoint", "java",
                    *(item for name in variables for item in ("-e", name)),
                    "--mount", f"type=bind,source={jar},target=/app/mathtutor.jar,readonly",
                    "--mount", f"type=bind,source={keystore},target=/run/server.p12,readonly",
                    "eclipse-temurin:21-jre", "-jar", "/app/mathtutor.jar", env=env)
            port = command("docker", "port", app, "8443/tcp").stdout.strip().rsplit(":", 1)[1]
            verify_https(f"https://localhost:{port}", cert)
            verify_zap(network, scanner)
    finally:
        for container in (scanner, app, database):
            command("docker", "rm", "-f", container, check=False)
        command("docker", "network", "rm", network, check=False)


if __name__ == "__main__":
    main()
