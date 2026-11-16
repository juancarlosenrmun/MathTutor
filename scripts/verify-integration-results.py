"""Falla si CI omite las integraciones deterministas de PostgreSQL y pgvector."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

REQUIRED = {
    "com.mathtutor.application.MathTutorApplicationTest",
    "com.mathtutor.application.controller.AuthControllerIntegrationTest",
    "com.mathtutor.application.retriever.SpringAiPgVectorRetrieverIntegrationTest",
    "com.mathtutor.application.Rnf03NoOpenTransactionDuringLlmCallTest",
    "com.mathtutor.application.controller.TutorApiIntegrationTest",
    "com.mathtutor.application.NoteStartupIntegrationTest",
}


def verify(results: Path) -> list[str]:
    suites = [ET.parse(path).getroot() for path in results.glob("TEST-*.xml")]
    errors = []
    for required in sorted(REQUIRED):
        # Gradle usa @DisplayName para el nombre de suite: la identidad estable
        # está en classname de cada testcase, incluidas las clases @Nested.
        cases = [case for suite in suites for case in suite.findall("testcase")
                 if (case.get("classname", suite.get("name", "")) == required
                     or case.get("classname", suite.get("name", "")).startswith(required + "$"))]
        if not cases:
            errors.append(f"Sin resultados: {required}")
        elif any(case.find("skipped") is not None or case.find("failure") is not None
                 or case.find("error") is not None for case in cases):
            errors.append(f"Integración omitida o fallida: {required}")
    return errors


if __name__ == "__main__":
    result_dir = Path(__file__).resolve().parents[1] / "mathtutor-application/build/test-results/test"
    problems = verify(result_dir)
    for problem in problems:
        print(problem)
    if problems:
        sys.exit(1)
    print("Integraciones PostgreSQL/pgvector ejecutadas sin omisiones ni fallos.")
