"""UC-14: adapta los fuentes de Industriales sin compilar ni ejecutar LaTeX.

Las fórmulas y los entornos se conservan como unidades indivisibles. El resultado
cumple el contrato de autores del indexador; --check comprueba su reproducibilidad.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

CHAPTERS = (
    ("Tema1_Numeros_Reales_Complejos.tex", "funciones", "numeros-reales-complejos"),
    ("Tema2_Funciones_Continuidad.tex", "funciones", "funciones-continuidad"),
    ("Tema3_Calculo_Diferencial.tex", "derivadas-integrales", "calculo-diferencial"),
    ("Tema4_Calculo_Integral.tex", "derivadas-integrales", "calculo-integral"),
    ("Tema5_Matrices.tex", "metodos-3d", "matrices"),
    ("Tema6_Espacios_Vectoriales_Aplicaciones_Lineales.tex", "metodos-3d", "espacios-vectoriales"),
    ("Tema7_Diagonalizacion.tex", "metodos-3d", "diagonalizacion"),
    ("Tema8_Geometria.tex", "metodos-3d", "geometria"),
)
SUPPORT = ("Industriales.tex", "classicthesis-config.tex", "Bibliography.bib", "Capitulos/Glosario_Mathematica.tex")
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "mathtutor-application/src/main/resources/notes"


def without_comments(text):
    """El porcentaje inicia comentario salvo si lo precede un número impar de barras."""
    lines = []
    for line in text.splitlines():
        for i, char in enumerate(line):
            if char != "%":
                continue
            preceding = len(line[:i]) - len(line[:i].rstrip("\\"))
            if preceding % 2 == 0:
                line = line[:i]
                break
        lines.append(line)
    return "\n".join(lines)


def theory(text):
    """Conserva los ejemplos resueltos; el listado final se reserva para UC-22."""
    return re.split(r"\\section\*?\{Ejercicios propuestos\}", without_comments(text), maxsplit=1)[0]


def normalize(text):
    """Expande solo macros conocidas del preámbulo y retira formato editorial."""
    text = without_comments(text)
    macros = {letter: r"\mathbb{" + letter + "}" for letter in "RNCQZ"}
    macros.update({"tq": r"\text{ : }", "pt": r"\;\forall\;", "M": "(Mathematica)", "MatCmd": r"\texttt"})
    for name, expansion in macros.items():
        text = re.sub(r"\\" + name + r"(?![A-Za-z])", lambda _: expansion, text)
    text = re.sub(r"\\(?:chapter|section|subsection|subsubsection)\*?\{", r"\\textbf{", text)
    text = re.sub(r"\\label\{[^}]*\}", "", text)
    text = re.sub(r"\\renewcommand\{\\arraystretch\}\{[^}]*\}", "", text)
    text = re.sub(r"\\tcblower(?![A-Za-z])", "", text)
    return "\n".join(line.rstrip() for line in text.strip().splitlines())


def atoms(text):
    """Separa párrafos únicamente cuando no deja abiertos entornos, grupos o matemáticas."""
    blocks, current, environments = [], [], []
    math, braces = [], 0
    tokens = re.compile(r"\\(begin|end)\{([^}]+)\}|(?<!\\)\\[\[\]()]|(?<!\\)\$\$?")
    for line in text.splitlines():
        for token in tokens.finditer(line):
            if token[1] == "begin":
                environments.append(token[2])
            elif token[1] == "end":
                if not environments or environments.pop() != token[2]:
                    raise ValueError("Entornos LaTeX desbalanceados")
            else:
                value = token[0]
                if value in (r"\[", r"\("):
                    if math:
                        raise ValueError("Matemáticas LaTeX anidadas incorrectamente")
                    math.append({r"\[": r"\]", r"\(": r"\)"}[value])
                elif value in (r"\]", r"\)"):
                    if not math or math[-1] != value:
                        raise ValueError("Delimitadores matemáticos incorrectos")
                    math.pop()
                else:
                    if math and math[-1] == value:
                        math.pop()
                    else:
                        math.append(value)
        for token in re.finditer(r"(?<!\\)[{}]", line):
            braces += 1 if token[0] == "{" else -1
            if braces < 0:
                raise ValueError("Grupos LaTeX desbalanceados")
        if not line.strip() and not environments and not math and braces == 0:
            if current:
                blocks.append("\n".join(current).strip())
                current = []
        else:
            current.append(line)
    if environments or math or braces:
        raise ValueError("Documento LaTeX incompleto")
    if current:
        blocks.append("\n".join(current).strip())
    combined = []
    for block in filter(None, blocks):
        if (block.startswith(r"\begin{proof}") and combined
                and re.search(r"\\end\{(?:theorem|proposition|lemma|corollary)\}$", combined[-1])):
            combined[-1] += "\n\n" + block
        else:
            combined.append(block)
    return combined


def partition(blocks):
    """Partición global de 150–400 palabras, sin dividir bloques ni dejar una cola pequeña."""
    sizes = [len(block.split()) for block in blocks]
    best = {len(blocks): (0, None)}
    for start in range(len(blocks) - 1, -1, -1):
        words = 0
        for end in range(start + 1, len(blocks) + 1):
            words += sizes[end - 1]
            if words > 400:
                break
            if words >= 150 and end in best:
                cost = (words - 250) ** 2 + best[end][0]
                if start not in best or cost < best[start][0]:
                    best[start] = (cost, end)
    if not blocks or 0 not in best:
        raise ValueError("No es posible fragmentar en 150–400 palabras sin partir fórmulas o entornos")
    result, position = [], 0
    while position < len(blocks):
        end = best[position][1]
        result.append("\n\n".join(block.strip() for block in blocks[position:end]))
        position = end
    return result


def import_book(source, output, check=False):
    """Prepara el corpus completo antes de escribir y copia únicamente fuentes conocidas."""
    source, output = Path(source), Path(output)
    files, chapters = {}, []
    for number, (filename, module, topic) in enumerate(CHAPTERS, 1):
        relative = Path("Capitulos") / filename
        raw = (source / relative).read_bytes()
        text = raw.decode("utf-8-sig")
        title = re.search(r"\\chapter\{([^{}]+)\}", text)
        if title is None:
            raise ValueError(f"Capítulo sin título: {filename}")
        title = title[1]
        chunks = partition(atoms(normalize(theory(text))))
        target = f"industriales-{number:02d}-{topic}.tex"
        digest = hashlib.sha256(raw).hexdigest()
        header = (f"% Fuente: Industriales/{relative.as_posix()}\n% SHA-256 original: {digest}\n"
                  f"\\bloque{{{module}}}\n\\tema{{{topic}}}{{{title}}}\n\\dificultad{{1}}\n")
        content = header + "\n".join(f"\n\\section{{{title} - fragmento {i}}}\n{chunk}\n" for i, chunk in enumerate(chunks, 1))
        files[Path(target)] = content.encode("utf-8")
        files[Path("sources/industriales") / relative] = raw
        chapters.append({"source": relative.as_posix(), "sha256": digest, "resource": target,
                         "module": module, "topic": topic, "chunks": len(chunks)})
    for filename in SUPPORT:
        files[Path("sources/industriales") / filename] = (source / filename).read_bytes()
    manifest = {"title": "Matemáticas I — Ingenierías de la rama industrial",
                "authors": ["Diego García Zamora", "Nuria V. Castillo González"],
                "institution": "Universidad de Jaén", "chapters": chapters,
                "excluded_from_index": ["Ejercicios propuestos (reservados para UC-22)", "Glosario de Mathematica", "Preámbulo y bibliografía"]}
    files[Path("industriales-manifest.json")] = (json.dumps(manifest, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    for relative, content in files.items():
        target = output / relative
        if check:
            if not target.is_file() or target.read_bytes() != content:
                raise ValueError(f"Recurso no reproducible: {relative}")
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=OUTPUT / "sources/industriales")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    parser.add_argument("--check", action="store_true")
    options = parser.parse_args()
    manifest = import_book(options.source, options.output, options.check)
    print(f"Industriales: {len(manifest['chapters'])} capítulos, {sum(c['chunks'] for c in manifest['chapters'])} fragmentos; reproducibilidad correcta.")
