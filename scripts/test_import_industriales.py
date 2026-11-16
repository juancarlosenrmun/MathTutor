"""UC-14: contrato del importador reproducible de apuntes, sin servicios externos."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("industriales", Path(__file__).with_name("import_industriales.py"))
notes = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notes)


class ImportNotesTest(unittest.TestCase):
    def test_expands_known_macros_without_touching_longer_commands(self):
        text = notes.normalize(r"\R \Re \N \C \Q \Z \tq \pt \M")
        self.assertIn(r"\mathbb{R}", text)
        self.assertIn(r"\Re", text)
        self.assertNotIn(r"\tq", text)
        self.assertIn("Mathematica", text)

    def test_comments_and_editorial_commands_do_not_enter_the_index(self):
        text = notes.normalize("% oculto\n50\\% visible\n\\label{interno}\n\\section{Tema}\n")
        self.assertNotIn("oculto", text)
        self.assertIn(r"50\%", text)
        self.assertNotIn(r"\section", text)
        self.assertIn(r"\textbf{Tema}", text)

    def test_theory_extraction_preserves_solved_examples_but_not_the_future_bank(self):
        text = r"\chapter{Tema}" + "\n" + r"\begin{solvedexercise}Ejemplo\end{solvedexercise}" + "\n" + r"\section*{Ejercicios propuestos}" + "\nBANCO"
        theory = notes.theory(text)
        self.assertIn("solvedexercise", theory)
        self.assertNotIn("BANCO", theory)

    def test_atoms_preserve_multiline_math_and_nested_environments(self):
        text = "Primer párrafo.\n\n\\begin{example}\n\\[\nx^2\n\n+1\n\\]\n\\begin{itemize}\n\\item valor\n\\end{itemize}\n\\end{example}\n\nFinal."
        atoms = notes.atoms(text)
        self.assertEqual(len(atoms), 3)
        self.assertIn("x^2\n\n+1", atoms[1])
        self.assertIn(r"\end{example}", atoms[1])

    def test_unbalanced_environments_or_math_fail_instead_of_truncating(self):
        for text in [r"\begin{example}incompleto", r"\[sin cierre", r"\end{example}"]:
            with self.subTest(text=text), self.assertRaises(ValueError):
                notes.atoms(text)

    def test_array_row_spacing_is_not_a_display_math_delimiter(self):
        text = "\\[\n\\begin{array}{cc}\na & b \\\\[2mm]\nc & d\n\\end{array}\n\\]"
        self.assertEqual(notes.atoms(text), [text])

    def test_inline_math_in_display_text_keeps_the_outer_delimiter(self):
        text = r"\[ x=1\quad\text{para todo $x$}\]"
        self.assertEqual(notes.atoms(text), [text])

    def test_partition_uses_complete_atoms_and_balances_the_last_fragment(self):
        atoms = ["uno " * 100, "dos " * 100, "tres " * 100, "cuatro " * 120]
        chunks = notes.partition(atoms)
        self.assertEqual(len(chunks), 2)
        self.assertTrue(all(150 <= len(c.split()) <= 400 for c in chunks))
        self.assertIn(atoms[0].strip(), chunks[0])

    def test_theorem_and_its_proof_are_one_indivisible_atom(self):
        theorem = r"\begin{theorem}Todo enunciado.\end{theorem}"
        proof = r"\begin{proof}Su demostración.\end{proof}"
        blocks = notes.atoms("Introducción.\n\n" + theorem + "\n\n" + proof + "\n\nConclusión.")
        self.assertEqual(len(blocks), 3)
        self.assertEqual(blocks[1], theorem + "\n\n" + proof)

    def test_exact_boundaries_and_impossible_partitions(self):
        for count in [150, 400]:
            self.assertEqual(len(notes.partition(["palabra " * count])), 1)
        for blocks in [[], ["palabra " * 149], ["palabra " * 401]]:
            with self.subTest(blocks=len(blocks)), self.assertRaises(ValueError):
                notes.partition(blocks)

    def test_whole_import_is_deterministic_and_check_detects_drift(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source, output = root / "source", root / "output"
            (source / "Capitulos").mkdir(parents=True)
            for filename, _, _ in notes.CHAPTERS:
                (source / "Capitulos" / filename).write_text("\\chapter{Tema}\n\n" + "contenido " * 180, encoding="utf-8")
            for filename in notes.SUPPORT:
                target = source / filename
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text("Referencia original", encoding="utf-8")
            notes.import_book(source, output)
            before = {p.relative_to(output): p.read_bytes() for p in output.rglob("*") if p.is_file()}
            notes.import_book(source, output)
            after = {p.relative_to(output): p.read_bytes() for p in output.rglob("*") if p.is_file()}
            self.assertEqual(before, after)
            notes.import_book(source, output, check=True)
            generated = next(output.glob("industriales-*.tex"))
            generated.write_text("alterado", encoding="utf-8")
            with self.assertRaises(ValueError):
                notes.import_book(source, output, check=True)


if __name__ == "__main__":
    unittest.main()
