"""Regresiones de la puerta de integraciones de CI, sin dependencias externas."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location(
    "integration_gate", Path(__file__).with_name("verify-integration-results.py"))
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


class IntegrationGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.results = Path(self.temp.name)

    def write_suites(self, child=None):
        for index, name in enumerate(sorted(gate.REQUIRED)):
            suite = ET.Element("testsuite", name=name + "$Contrato")
            case = ET.SubElement(suite, "testcase", name="contrato")
            if child:
                ET.SubElement(case, child)
            ET.ElementTree(suite).write(self.results / f"TEST-{index}.xml")

    def test_missing_reports_fail(self):
        self.assertEqual(len(gate.verify(self.results)), len(gate.REQUIRED))

    def test_executed_nested_suites_pass(self):
        self.write_suites()
        self.assertEqual(gate.verify(self.results), [])

    def test_junit_display_names_use_testcase_classnames(self):
        for index, name in enumerate(sorted(gate.REQUIRED)):
            suite = ET.Element("testsuite", name="Contrato legible")
            ET.SubElement(suite, "testcase", name="caso", classname=name + "$Contrato")
            ET.ElementTree(suite).write(self.results / f"TEST-{index}.xml")
        self.assertEqual(gate.verify(self.results), [])

    def test_skips_failures_and_errors_block_ci(self):
        for marker in ("skipped", "failure", "error"):
            with self.subTest(marker=marker):
                self.write_suites(marker)
                self.assertEqual(len(gate.verify(self.results)), len(gate.REQUIRED))


if __name__ == "__main__":
    unittest.main()
