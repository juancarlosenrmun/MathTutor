package com.mathtutor.cas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DisplayName;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Test parametrizado que valida el corpus LaTeX → Symja.
 * Cubre 9 categorías con ≥8 casos cada una + negativos.
 */
class LatexToSymjaCorpusTest {

    private final LatexToSymjaConverter converter = new LatexToSymjaConverter();
    private final ObjectMapper mapper = new ObjectMapper();

    @TestFactory
    @DisplayName("Corpus LaTeX→Symja: equivalencias positivas por categoría")
    Stream<DynamicTest> testPositiveEquivalences() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream("/latex-symja-corpus.json")) {
            JsonNode root = mapper.readTree(is);
            JsonNode categories = root.get("categories");

            categories.fields().forEachRemaining(entry -> {
                String categoryName = entry.getKey();
                JsonNode category = entry.getValue();
                JsonNode cases = category.get("cases");

                Iterator<JsonNode> it = cases.elements();
                while (it.hasNext()) {
                    JsonNode tc = it.next();
                    String id = tc.get("id").asText();
                    String latex = tc.get("latex").asText();
                    String expectedEquivalent = tc.get("expectedEquivalent").asText();

                    String testName = categoryName + "." + id + ": " + latex + " ≡ " + expectedEquivalent;
                    tests.add(DynamicTest.dynamicTest(testName, () -> {
                        boolean equiv = converter.areEquivalent(latex, expectedEquivalent);
                        assertThat(equiv)
                            .as("Fallo en %s: '%s' debería ser equivalente a '%s'", categoryName, latex, expectedEquivalent)
                            .isTrue();
                    }));
                }
            });
        }

        return tests.stream();
    }

    @TestFactory
    @DisplayName("Corpus LaTeX→Symja: no-equivalencias negativas por categoría")
    Stream<DynamicTest> testNegativeEquivalences() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream("/latex-symja-corpus.json")) {
            JsonNode root = mapper.readTree(is);
            JsonNode categories = root.get("categories");

            categories.fields().forEachRemaining(entry -> {
                String categoryName = entry.getKey();
                JsonNode category = entry.getValue();
                JsonNode negatives = category.get("negatives");

                if (negatives == null) return;

                Iterator<JsonNode> it = negatives.elements();
                while (it.hasNext()) {
                    JsonNode tc = it.next();
                    String id = tc.get("id").asText();
                    String latex = tc.get("latex").asText();
                    String notEquivalentTo = tc.get("notEquivalentTo").asText();

                    String testName = categoryName + "." + id + ": " + latex + " ≢ " + notEquivalentTo;
                    tests.add(DynamicTest.dynamicTest(testName, () -> {
                        boolean equiv = converter.areEquivalent(latex, notEquivalentTo);
                        assertThat(equiv)
                            .as("Falso positivo en %s: '%s' NO debería ser equivalente a '%s'", categoryName, latex, notEquivalentTo)
                            .isFalse();
                    }));
                }
            });
        }

        return tests.stream();
    }

    @TestFactory
    @DisplayName("Corpus LaTeX→Symja: parseo a Symja AST")
    Stream<DynamicTest> testParseToSymja() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream("/latex-symja-corpus.json")) {
            JsonNode root = mapper.readTree(is);
            JsonNode categories = root.get("categories");

            categories.fields().forEachRemaining(entry -> {
                String categoryName = entry.getKey();
                JsonNode category = entry.getValue();
                JsonNode cases = category.get("cases");

                Iterator<JsonNode> it = cases.elements();
                while (it.hasNext()) {
                    JsonNode tc = it.next();
                    String id = tc.get("id").asText();
                    String latex = tc.get("latex").asText();
                    String expectedSymja = tc.get("symja").asText();

                    String testName = categoryName + "." + id + ": parse '" + latex + "'";
                    tests.add(DynamicTest.dynamicTest(testName, () -> {
                        String symja = converter.convert(latex);
                        assertThat(symja)
                            .as("Fallo parseo en %s: '%s' debería parsearse", categoryName, latex)
                            .isNotNull();
                        // Verificar que al menos parsea (Symja AST no vacío)
                        assertThat(symja).isNotEmpty();
                    }));
                }
            });
        }

        return tests.stream();
    }
}