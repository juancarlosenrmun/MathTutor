package com.mathtutor.cas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extractor de contenido LaTeX desde archivos .tex.
 * Extrae macros de plantilla y entornos definidos en el contrato LaTeX.
 *
 * Contrato LaTeX (según Restricciones y alcance):
 * - Macros: \bloque{...}, \tema{...}, \dificultad{1-3}
 * - Entornos: ejercicio, enunciado, solución, paso, error típico, concepto clave
 *
 * Symja no compila LaTeX, solo interpreta fórmulas: el sistema extrae antes
 * entornos y fórmulas del .tex.
 */
@Component
public class LatexContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(LatexContentExtractor.class);

    // Patrones para macros de plantilla
    private static final Pattern BLOQUE_PATTERN = Pattern.compile("\\\\bloque\\s*\\{([^}]+)\\}");
    private static final Pattern TEMA_PATTERN = Pattern.compile("\\\\tema\\s*\\{([^}]+)\\}");
    private static final Pattern DIFICULTAD_PATTERN = Pattern.compile("\\\\dificultad\\s*\\{([^}]+)\\}");

    // Patrones para entornos LaTeX
    private static final Pattern ENVIRONMENT_PATTERN = Pattern.compile(
            "\\\\begin\\{([^}]+)\\}(.*?)\\\\end\\{\\1\\}",
            Pattern.DOTALL
    );

    // Nombres de entornos válidos según el contrato
    private static final String[] VALID_ENVIRONMENTS = {
            "ejercicio", "enunciado", "solucion", "solución",
            "paso", "errorTipico", "errorTípico", "conceptoClave", "conceptoClave"
    };

    /**
     * Resultado de la extracción de un archivo .tex.
     */
    public static class ExtractionResult {
        private final String bloque;
        private final String tema;
        private final Integer dificultad;
        private final Map<String, List<String>> environments;
        private final List<String> formulas;
        private final List<String> errors;

        private ExtractionResult(Builder builder) {
            this.bloque = builder.bloque;
            this.tema = builder.tema;
            this.dificultad = builder.dificultad;
            this.environments = builder.environments;
            this.formulas = builder.formulas;
            this.errors = builder.errors;
        }

        public String getBloque() { return bloque; }
        public String getTema() { return tema; }
        public Integer getDificultad() { return dificultad; }
        public Map<String, List<String>> getEnvironments() { return environments; }
        public List<String> getFormulas() { return formulas; }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return !errors.isEmpty(); }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private String bloque;
            private String tema;
            private Integer dificultad;
            private Map<String, List<String>> environments = new HashMap<>();
            private List<String> formulas = new ArrayList<>();
            private List<String> errors = new ArrayList<>();

            public Builder bloque(String bloque) { this.bloque = bloque; return this; }
            public Builder tema(String tema) { this.tema = tema; return this; }
            public Builder dificultad(Integer dificultad) { this.dificultad = dificultad; return this; }
            public Builder environments(Map<String, List<String>> environments) { this.environments = environments; return this; }
            public Builder addEnvironment(String name, String content) {
                environments.computeIfAbsent(name, k -> new ArrayList<>()).add(content.trim());
                return this;
            }
            public Builder formulas(List<String> formulas) { this.formulas = formulas; return this; }
            public Builder addFormula(String formula) { this.formulas.add(formula); return this; }
            public Builder errors(List<String> errors) { this.errors = errors; return this; }
            public Builder addError(String error) { this.errors.add(error); return this; }

            public ExtractionResult build() { return new ExtractionResult(this); }
        }
    }

    /**
     * Extrae contenido de un archivo .tex completo.
     *
     * @param texContent Contenido completo del archivo .tex
     * @return Resultado con macros, entornos y fórmulas extraídas
     */
    public ExtractionResult extract(String texContent) {
        if (texContent == null || texContent.trim().isEmpty()) {
            return ExtractionResult.builder()
                    .addError("Contenido .tex nulo o vacío")
                    .build();
        }

        ExtractionResult.Builder builder = ExtractionResult.builder();
        List<String> errors = new ArrayList<>();

        // 1. Extraer macros de plantilla
        extractMacros(texContent, builder, errors);

        // 2. Extraer entornos
        extractEnvironments(texContent, builder, errors);

        // 3. Extraer fórmulas LaTeX (inline $...$ y display $$...$$)
        extractFormulas(texContent, builder);

        if (!errors.isEmpty()) {
            builder.errors(errors);
        }

        log.debug("Extracción LaTeX completada: bloque={}, tema={}, dificultad={}, entornos={}, fórmulas={}",
                builder.bloque, builder.tema, builder.dificultad,
                builder.environments.size(), builder.formulas.size());

        return builder.build();
    }

    /**
     * Extrae las macros de plantilla: \bloque, \tema, \dificultad.
     */
    private void extractMacros(String texContent, ExtractionResult.Builder builder, List<String> errors) {
        // \bloque{...}
        Matcher bloqueMatcher = BLOQUE_PATTERN.matcher(texContent);
        if (bloqueMatcher.find()) {
            builder.bloque(bloqueMatcher.group(1).trim());
        } else {
            errors.add("Macro \\bloque no encontrada");
        }

        // \tema{...}
        Matcher temaMatcher = TEMA_PATTERN.matcher(texContent);
        if (temaMatcher.find()) {
            builder.tema(temaMatcher.group(1).trim());
        } else {
            errors.add("Macro \\tema no encontrada");
        }

        // \dificultad{...}
        Matcher dificultadMatcher = DIFICULTAD_PATTERN.matcher(texContent);
        if (dificultadMatcher.find()) {
            try {
                builder.dificultad(Integer.parseInt(dificultadMatcher.group(1).trim()));
            } catch (NumberFormatException e) {
                errors.add("Macro \\dificultad no es un número válido: " + dificultadMatcher.group(1));
            }
        } else {
            errors.add("Macro \\dificultad no encontrada");
        }
    }

    /**
     * Extrae los entornos LaTeX definidos en el contrato.
     */
    private void extractEnvironments(String texContent, ExtractionResult.Builder builder, List<String> errors) {
        Matcher envMatcher = ENVIRONMENT_PATTERN.matcher(texContent);

        while (envMatcher.find()) {
            String envName = envMatcher.group(1).trim();
            String envContent = envMatcher.group(2).trim();

            // Verificar si es un entorno válido según el contrato
            boolean isValid = false;
            for (String validEnv : VALID_ENVIRONMENTS) {
                if (validEnv.equalsIgnoreCase(envName)) {
                    isValid = true;
                    // Normalizar nombre (usar versión sin acentos)
                    String normalizedName = normalizeEnvironmentName(envName);
                    builder.addEnvironment(normalizedName, envContent);
                    break;
                }
            }

            if (!isValid) {
                log.debug("Entorno LaTeX no reconocido (ignorado): {}", envName);
            }
        }

        // Verificar que al menos un entorno de contenido existe
        if (builder.environments.isEmpty()) {
            errors.add("No se encontraron entornos válidos (ejercicio, enunciado, solucion, paso, errorTipico, conceptoClave)");
        }
    }

    /**
     * Normaliza nombres de entornos (quita acentos).
     */
    private String normalizeEnvironmentName(String name) {
        return name.replace("ó", "o").replace("á", "a").replace("í", "i")
                   .replace("ú", "u").replace("ñ", "n");
    }

    /**
     * Extrae fórmulas LaTeX inline ($...$) y display ($$...$$).
     * Primero extrae fórmulas display y las marca, luego extrae inline del resto.
     */
    private void extractFormulas(String texContent, ExtractionResult.Builder builder) {
        // Primero extraer fórmulas display $$...$$ y reemplazarlas con placeholders
        // para que no interfieran con la extracción inline
        StringBuilder textWithoutDisplay = new StringBuilder(texContent);
        Pattern displayPattern = Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
        Matcher displayMatcher = displayPattern.matcher(texContent);

        int offset = 0;
        while (displayMatcher.find()) {
            String formula = displayMatcher.group(1).trim();
            if (!formula.isEmpty()) {
                builder.addFormula(formula);
            }
            // Reemplazar la fórmula display completa con espacios del mismo largo
            int start = displayMatcher.start() + offset;
            int end = displayMatcher.end() + offset;
            for (int i = start; i < end; i++) {
                if (i < textWithoutDisplay.length()) {
                    textWithoutDisplay.setCharAt(i, ' ');
                }
            }
        }

        // Ahora extraer fórmulas inline $...$ del texto sin displays
        // Evitar $ escapados \$
        Pattern inlinePattern = Pattern.compile("(?<!\\\\)\\$(.*?)(?<!\\\\)\\$");
        Matcher inlineMatcher = inlinePattern.matcher(textWithoutDisplay);
        while (inlineMatcher.find()) {
            String formula = inlineMatcher.group(1).trim();
            if (!formula.isEmpty()) {
                builder.addFormula(formula);
            }
        }
    }

    /**
     * Extrae fórmulas de un entorno específico.
     * Útil para obtener fórmulas de un paso, solución, etc.
     */
    public List<String> extractFormulasFromEnvironment(String environmentContent) {
        List<String> formulas = new ArrayList<>();
        if (environmentContent == null) {
            return formulas;
        }

        // Mismo enfoque: primero display, luego inline
        StringBuilder textWithoutDisplay = new StringBuilder(environmentContent);
        Pattern displayPattern = Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
        Matcher displayMatcher = displayPattern.matcher(environmentContent);

        int offset = 0;
        while (displayMatcher.find()) {
            String formula = displayMatcher.group(1).trim();
            if (!formula.isEmpty()) {
                formulas.add(formula);
            }
            int start = displayMatcher.start() + offset;
            int end = displayMatcher.end() + offset;
            for (int i = start; i < end; i++) {
                if (i < textWithoutDisplay.length()) {
                    textWithoutDisplay.setCharAt(i, ' ');
                }
            }
        }

        // Fórmulas inline $...$
        Pattern inlinePattern = Pattern.compile("(?<!\\\\)\\$(.*?)(?<!\\\\)\\$");
        Matcher inlineMatcher = inlinePattern.matcher(textWithoutDisplay);
        while (inlineMatcher.find()) {
            String formula = inlineMatcher.group(1).trim();
            if (!formula.isEmpty()) {
                formulas.add(formula);
            }
        }

        return formulas;
    }
}