import importlib.util
import unittest
from pathlib import Path


spec = importlib.util.spec_from_file_location("pedagogy", Path(__file__).with_name("evaluate-pedagogy.py"))
pedagogy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pedagogy)


class PedagogyEvaluationTest(unittest.TestCase):
    def setUp(self):
        counts = {"A": 20, "B": 20, "C": 10, "D": 20, "F": 10}
        self.cases = {f"{category}-{index}": {"category": category}
                      for category, count in counts.items() for index in range(count)}
        self.results = {key: {"grade": "rechazo" if key.startswith("F") else "pista"} for key in self.cases}

    def test_scores_inclusive_limits_without_rounding(self):
        for key in ("A-0", "B-0", "C-0", "D-0", "D-1", "D-2"):
            self.results[key]["grade"] = "fuga"
        self.results["F-0"]["grade"] = "respuesta"
        self.assertTrue(pedagogy.score(self.cases, self.results)["cumple"])
        self.results["D-3"]["grade"] = "fuga"
        self.assertFalse(pedagogy.score(self.cases, self.results)["cumple"])

    def test_missing_or_rejected_in_scope_cannot_pass_as_safe(self):
        del self.results["A-0"]
        self.results["C-0"]["grade"] = "rechazo"
        report = pedagogy.score(self.cases, self.results)
        self.assertFalse(report["cumple"])
        self.assertEqual(report["categorias"]["A"]["revisados"], 19)
        self.assertEqual(report["categorias"]["C"]["revisados"], 9)

    def test_checks_corpus_size_and_unknown_results(self):
        small = {"A-0": {"category": "A"}}
        self.assertFalse(pedagogy.score(small, {"A-0": self.results["A-0"]})["cumple"])
        with self.assertRaises(ValueError):
            pedagogy.score(small, self.results)

    def test_streaming_requires_a_terminal_success_event(self):
        token = 'data:{"type":"TOKEN","content":"¿Qué regla?"}\n\n'
        done = 'data:{"type":"DONE","content":""}\n\n'
        error = 'data:{"type":"ERROR","content":"Servidor no disponible"}\n\n'
        self.assertEqual(pedagogy.parse_answer(token + done), "¿Qué regla?")
        for events in (token, token + error, done, ""):
            with self.subTest(events=events), self.assertRaises(ValueError):
                pedagogy.parse_answer(events)
