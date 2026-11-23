"""Compila GLSL y verifica píxeles reales en el navegador seleccionado."""
import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import threading

ROOT = Path(__file__).resolve().parents[1]
STATIC = ROOT / "mathtutor-graphics/src/main/resources/static"
EVIDENCE = ROOT / "mathtutor-graphics/build/reports/client-browser"


class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/api/csrf-token":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"csrfToken":"smoke"}')
        elif self.path in ("/smoke.html", "/frontend.html", "/performance.html"):
            content = (ROOT / "mathtutor-graphics/src/test/browser" / self.path.lstrip("/")).read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(content)
        else:
            super().do_GET()

    def do_POST(self):
        # Servidor estático de pruebas: ninguna sesión persistente.
        self.send_response(401)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{}')

    def log_message(self, *_):
        pass


def run_native(name, expected_major, origin):
    """Ejecuta las mismas aserciones de píxeles con Firefox o Safari oficiales."""
    from selenium import webdriver
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait

    if name == "firefox":
        options = webdriver.FirefoxOptions()
        options.add_argument("-headless")
        options.set_preference("app.update.disabledForTesting", True)
        if binary := os.environ.get("BROWSER_BIN"):
            options.binary_location = binary
        browser = webdriver.Firefox(options=options)
    else:
        browser = webdriver.Safari()

    with browser:
        version = browser.capabilities["browserVersion"]
        print(f"{name} nativo {version}", flush=True)
        if expected_major and version.split(".")[0] != expected_major:
            raise AssertionError(f"Se esperaba la versión principal {expected_major}, se ejecutó {version}")

        def result(driver):
            return driver.execute_script("return document.body.dataset.result")

        browser.get(origin + "/smoke.html")
        status = WebDriverWait(browser, 30).until(result)
        if status != "passed":
            raise AssertionError(browser.find_element(By.ID, "status").text)
        browser.get(origin + "/visor/index.html#/laboratorio")
        WebDriverWait(browser, 15).until(
            lambda driver: "Visor listo." in driver.find_element(By.ID, "graphic-status").text)
        print("WebGL 2 nativo correcto en las pruebas de píxeles y en el laboratorio")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--native", choices=("firefox", "safari"))
    mode.add_argument("--performance", action="store_true", help="mide RNF-02 con aceleración gráfica")
    parser.add_argument("--expect-major")
    args = parser.parse_args()
    candidates = [os.environ.get("CHROME_BIN"), shutil.which("google-chrome"),
                  shutil.which("chromium"), shutil.which("chromium-browser"),
                  "C:/Program Files/Google/Chrome/Application/chrome.exe",
                  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"]
    browser = next((path for path in candidates if path and Path(path).is_file()), None)
    if not args.native and not browser:
        raise SystemExit("Falta Chromium/Chrome/Edge para verificar WebGL2; configura CHROME_BIN.")
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer(("127.0.0.1", 0), partial(Handler, directory=str(STATIC)))
    threading.Thread(target=server.serve_forever, daemon=True).start()
    try:
        if args.performance:
            subprocess.run(["node", str(ROOT / "scripts/performance-webgl.mjs"), browser,
                            f"http://127.0.0.1:{server.server_port}/performance.html", str(EVIDENCE)],
                           check=True, timeout=45)
            return
        if args.native:
            run_native(args.native, args.expect_major, f"http://127.0.0.1:{server.server_port}")
            return
        for name, route, expected in [("pipeline", "/smoke.html", 'data-result="passed"'),
                                      ("page", "/visor/index.html", "Visor listo.")]:
            with tempfile.TemporaryDirectory(prefix="mathtutor-browser-") as profile:
                result = subprocess.run([browser, "--headless", "--no-first-run", "--no-default-browser-check",
                    "--no-sandbox", "--use-angle=swiftshader", "--enable-unsafe-swiftshader",
                    "--disable-background-networking", f"--user-data-dir={profile}",
                    "--virtual-time-budget=10000", "--dump-dom",
                    f"http://127.0.0.1:{server.server_port}{route}"],
                    capture_output=True, timeout=45, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
                dom = result.stdout.decode("utf-8", errors="replace")
                (EVIDENCE / f"{name}.html").write_text(dom, encoding="utf-8")
                if result.returncode or expected not in dom:
                    (EVIDENCE / f"{name}.log").write_bytes(result.stderr)
                    raise SystemExit(f"Prueba de navegador fallida: {name}; consulta {EVIDENCE}")
                print(f"WebGL2 real: {name} correcto")
        subprocess.run(["node", str(ROOT / "scripts/smoke-frontend.mjs"), browser,
                        f"http://127.0.0.1:{server.server_port}", str(EVIDENCE)],
                       check=True, timeout=90)
    finally:
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    main()
