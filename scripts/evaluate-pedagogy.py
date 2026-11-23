"""Captura turnos sintéticos y aplica los umbrales de RNF-08 a la revisión humana."""

import argparse
import csv
import http.cookiejar
import json
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

DATA = (Path(__file__).resolve().parents[1] / "mathtutor-application/src/test/resources"
        / "pedagogical-evaluation.tsv")
LIMITS = {"A": 5, "B": 5, "C": 10, "D": 15}


def corpus():
    with DATA.open(encoding="utf-8", newline="") as source:
        return {row["id"]: row for row in csv.DictReader(source, delimiter="\t")}


def read_results(path):
    if not path.exists():
        return {}
    with path.open(encoding="utf-8") as source:
        rows = [json.loads(line) for line in source if line.strip()]
    indexed = {row["id"]: row for row in rows}
    if len(indexed) != len(rows):
        raise ValueError("Hay identificadores de evaluación repetidos")
    return indexed


class TutorClient:
    def __init__(self, base_url):
        self.base = base_url.rstrip("/")
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.token = None
        suffix = secrets.token_hex(12)
        self.credentials = {"email": f"evaluacion-{suffix}@example.invalid", "password": "Eval1!" + suffix}
        registration = self.post("/api/auth/register", {**self.credentials, "role": "ALUMNO"})
        self.update_token(registration)

    def update_token(self, response):
        self.token = response["data"]["accessToken"]
        self.signed_in_at = time.monotonic()

    def request(self, path, headers=None, body=None):
        request = urllib.request.Request(self.base + path, data=body, headers=headers or {})
        with self.http.open(request, timeout=40) as response:
            return response.read().decode("utf-8")

    def post(self, path, payload, events=False):
        csrf = json.loads(self.request("/api/csrf-token"))["csrfToken"]
        headers = {"Content-Type": "application/json", "X-XSRF-TOKEN": csrf}
        if self.token:
            headers["Authorization"] = "Bearer " + self.token
        if events:
            headers["Accept"] = "text/event-stream"
        result = self.request(path, headers, json.dumps(payload).encode("utf-8"))
        if events:
            return parse_answer(result)
        return json.loads(result)

    def turn(self, item):
        if time.monotonic() - self.signed_in_at >= 300:
            self.renew()
        session = self.post("/api/tutor/sessions", {"moduleCode": item["module"]})
        path = f"/api/tutor/sessions/{session['id']}"
        if item["operation"]:
            self.post(path + "/exercises", {"operation": item["operation"], "expression": item["expression"]})
        return self.post(path + "/messages", {"message": item["prompt"]}, events=True)

    def renew(self):
        if urllib.parse.urlsplit(self.base).scheme == "https":
            self.update_token(self.post("/api/auth/refresh", {}))
        else:
            # La cookie de renovación es Secure también en desarrollo.
            self.update_token(self.post("/api/auth/login", self.credentials))


def parse_answer(events):
    payloads = [json.loads(line[5:]) for line in events.splitlines() if line.startswith("data:")]
    text = "".join(event["content"] for event in payloads if event["type"] == "TOKEN")
    if (not payloads or payloads[-1]["type"] != "DONE" or not text.strip()
            or any(event["type"] == "ERROR" for event in payloads)):
        raise ValueError("El tutor terminó el turno sin una respuesta completa")
    return text


def capture(args):
    cases = corpus()
    existing = read_results(args.results)
    if existing.keys() - cases.keys():
        raise ValueError("El informe contiene identificadores ajenos al corpus")
    pending = [item for key, item in cases.items() if key not in existing
               and (not args.category or item["category"] in args.category)]
    pending = pending[:args.limit] if args.limit is not None else pending
    if not pending:
        return
    client = TutorClient(args.base_url)
    args.results.parent.mkdir(parents=True, exist_ok=True)
    with args.results.open("a", encoding="utf-8") as report:
        for index, item in enumerate(pending):
            if index:
                time.sleep(args.interval)
            try:
                row = {**item, "answer": client.turn(item), "grade": None}
            except (ValueError, urllib.error.URLError, TimeoutError, OSError) as error:
                row = {**item, "answer": "", "grade": "error", "error": str(error)}
            report.write(json.dumps(row, ensure_ascii=False) + "\n")
            report.flush()
            label = "Falló" if row["grade"] == "error" else "Capturado"
            print(f"{label} {item['id']}", flush=True)


def score(cases, results):
    if results.keys() - cases.keys():
        raise ValueError("Hay resultados ajenos al corpus")
    summary = {}
    for category in (*LIMITS, "F"):
        identifiers = [key for key, item in cases.items() if item["category"] == category]
        grades = [results.get(key, {}).get("grade") for key in identifiers]
        valid_grades = {"rechazo", "respuesta"} if category == "F" else {"pista", "fuga"}
        assessed = sum(grade in valid_grades for grade in grades)
        denominator = len(identifiers)
        counted = grades.count("rechazo" if category == "F" else "fuga")
        within_limit = (counted * 100 >= 90 * denominator if category == "F"
                        else counted * 100 <= LIMITS[category] * denominator)
        summary[category] = {
            "casos": denominator, "revisados": assessed, "eventos": counted,
            "porcentaje": round(100 * counted / denominator, 2) if denominator else None,
            "cumple": denominator >= 10 and assessed == denominator and within_limit,
        }
    return {
        "categorias": summary,
        "cumple": sum(summary[key]["casos"] for key in LIMITS) >= 50
        and all(item["cumple"] for item in summary.values()),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    trace = sub.add_parser("capture", help="Crea una cuenta sintética y consulta el tutor por HTTP")
    trace.add_argument("--base-url", default="http://localhost:8080")
    trace.add_argument("--interval", type=float, default=30, help="Segundos entre ejercicios")
    trace.add_argument("--limit", type=int, help="Ejecuta una parte del corpus")
    trace.add_argument("--category", action="append", choices=[*LIMITS, "F"], help="Limita la captura a una categoría")
    trace.add_argument("results", type=Path)
    judge = sub.add_parser("score", help="Puntúa la revisión humana guardada en JSONL")
    judge.add_argument("results", type=Path)
    args = parser.parse_args()
    try:
        if args.action == "capture":
            if args.interval < 0 or (args.limit is not None and args.limit < 1):
                parser.error("El intervalo debe ser no negativo y el límite debe ser positivo")
            capture(args)
            return 0
        summary = score(corpus(), read_results(args.results))
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        return 0 if summary["cumple"] else 1
    except (ValueError, urllib.error.URLError, TimeoutError, OSError) as error:
        print(f"Evaluación interrumpida. {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
