"""Compila GLSL y verifica píxeles reales en Chromium/Edge, sin paquetes externos."""
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
        elif self.path in ("/smoke.html", "/frontend.html"):
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


def main():
    candidates = [os.environ.get("CHROME_BIN"), shutil.which("google-chrome"),
                  shutil.which("chromium"), shutil.which("chromium-browser"),
                  "C:/Program Files/Google/Chrome/Application/chrome.exe",
                  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"]
    browser = next((path for path in candidates if path and Path(path).is_file()), None)
    if not browser:
        raise SystemExit("Falta Chromium/Chrome/Edge para verificar WebGL2; configura CHROME_BIN.")
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer(("127.0.0.1", 0), partial(Handler, directory=str(STATIC)))
    threading.Thread(target=server.serve_forever, daemon=True).start()
    try:
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
