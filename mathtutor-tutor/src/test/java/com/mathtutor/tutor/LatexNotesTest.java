package com.mathtutor.tutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/** UC-14: contrato de autores, fórmulas y fronteras del tamaño de sección. */
class LatexNotesTest {
    final LatexNotes parser = new LatexNotes();
    String note(String words) {
        return "\\bloque{funciones}\n\\tema{dominio}{Dominio}\n\\dificultad{1}\n\\section{Definición}\n" + words;
    }
    @Test void extractsMetadataAndPreservesLatexWithDeterministicVersion() {
        String tex = note("palabra ".repeat(150) + "$\\frac{x^{2}}{2}$");
        var chunks = parser.parse("dominio.tex", tex);
        assertThat(chunks).hasSize(1);
        var chunk = chunks.getFirst();
        assertThat(chunk.moduleCode()).isEqualTo("funciones");
        assertThat(chunk.topicCode()).isEqualTo("dominio");
        assertThat(chunk.content()).contains("\\frac{x^{2}}{2}");
        assertThat(chunk.version()).hasSize(20);
        assertThat(parser.parse("dominio.tex", tex)).extracting(c -> c.version()).containsExactly(chunk.version());
        assertThat(parser.parse("dominio.tex", tex + " cambio").getFirst().version()).isNotEqualTo(chunk.version());
    }
    @Test void ignoresCommentsButKeepsEscapedPercent() {
        var chunk = parser.parse("dominio.tex", "% \\bloque{intruso}\n" + note("palabra ".repeat(150) + "50\\% % oculto\n")).getFirst();
        assertThat(chunk.content()).contains("50\\%").doesNotContain("oculto");
    }
    @Test void multipleSelfContainedSectionsProduceIndependentChunks() {
        assertThat(parser.parse("dominio.tex", note("uno ".repeat(150)) + "\n\\section{Ejemplo}\n" + "dos ".repeat(150)))
                .hasSize(2).allSatisfy(c -> assertThat(c.sourceFile()).isEqualTo("dominio.tex"));
    }
    @ParameterizedTest @ValueSource(ints = {149, 401})
    void rejectsSectionsOutsideTheAuthorContract(int count) {
        assertThatThrownBy(() -> parser.parse("dominio.tex", note("palabra ".repeat(count))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("150–400");
    }
    @ParameterizedTest @ValueSource(ints = {150, 400})
    void acceptsTheExactWordBoundaries(int count) {
        assertThat(parser.parse("dominio.tex", note("palabra ".repeat(count)))).hasSize(1);
    }
    @Test void validatesModuleTopicDifficultyAndStructure() {
        String tex = note("palabra ".repeat(150));
        for (String invalid : new String[]{"", tex.replace("funciones", "otro"),
                tex.replace("{1}", "{4}"), tex.replace("\\tema{dominio}{Dominio}", ""),
                tex.replace("\\section{Definición}", ""), tex.replace("\\bloque{funciones}", "")}) {
            assertThatThrownBy(() -> parser.parse("dominio.tex", invalid)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
