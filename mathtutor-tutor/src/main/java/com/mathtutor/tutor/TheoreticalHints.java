package com.mathtutor.tutor;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** @brief Guía los razonamientos teóricos con preguntas de los temas del curso. */
final class TheoreticalHints {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern REASONING = Pattern.compile(
            "^(?:¿\\s*)?(?:por que|como (?:se deduce|se demuestra|justificas|demuestras|se justifica)"
                    + "|demuestra|justifica|razona|explica por que)\\b");
    private static final List<Rule> RULES = List.of(
            rule("funciones", "funciones-continuidad", "composicion|componer", "continu",
                    "¿Puedes escribir la expresión como f(g(x)) e indicar qué función se aplica primero?"),
            rule("funciones", "funciones-continuidad", "limites? laterales?", "continu|removible|evitable",
                    "¿Qué dos límites escribirías por separado al acercarte al punto desde cada lado?"),
            rule("funciones", "funciones-continuidad", "intervalo cerrado|compact", "maxim|minim|extrem",
                    "¿Qué hipótesis comprobarías sobre el dominio antes de estudiar si se alcanza un extremo?"),
            rule("funciones", "numeros-reales-complejos", "raices?|raiz|ceros?", "polinom|coeficient",
                    "Si sustituyes la raíz por una incógnita z, ¿qué ecuación polinómica tendrías que transformar?"),
            rule("funciones", "numeros-reales-complejos", "conjugad", "divid|division|cociente|denominador",
                    "¿Cómo escribirías una fracción equivalente aplicando el mismo factor arriba y abajo?"),
            rule("derivadas-integrales", "calculo-diferencial", "deriv", "creciente|decreciente|monoton",
                    "Si eliges dos puntos ordenados del intervalo, ¿qué diferencia de valores de la función estudiarías?"),
            rule("derivadas-integrales", "calculo-diferencial", "valor absoluto", "deriv|continu",
                    "¿Cómo escribirías los cocientes incrementales a izquierda y derecha del punto?"),
            rule("derivadas-integrales", "calculo-diferencial", "derivada (?:nula|cero|se anula)", "extrem|maxim|minim",
                    "¿En qué dos intervalos junto al punto podrías examinar el signo de la derivada?"),
            rule("derivadas-integrales", "calculo-integral", "integr(?:al|ar|acion)", "impar|paridad",
                    "Si separas la integral en el origen, ¿qué límites quedarían en cada mitad?"),
            rule("derivadas-integrales", "calculo-integral", "integra(?:cion|r) por partes", "deriv|regla|formula",
                    "¿Qué funciones elegirías como factores para escribir un producto antes de derivarlo?"),
            rule("metodos-3d", "matrices", "determinante", "(?:intercambi|permut|transpos).*filas|filas.*(?:intercambi|permut)",
                    "¿Cómo etiquetarías las filas de una matriz pequeña para distinguir el orden de las filas antes y después del cambio?"),
            rule("metodos-3d", "espacios-vectoriales", "union", "subespacio",
                    "¿Qué condición de clausura probarías con un vector tomado de cada subespacio?"),
            rule("metodos-3d", "diagonalizacion", "vectores propios|autovectores", "independ|proporcional",
                    "Si sospechas que dos vectores son proporcionales, ¿cómo escribirías esa hipótesis antes de aplicar la matriz?"),
            rule("metodos-3d", "diagonalizacion", "multiplicidad", "diagonaliz|valores? propios?",
                    "¿Qué dos cantidades comprobarías por separado en el polinomio característico y en el espacio propio?"),
            rule("metodos-3d", "geometria", "producto escalar", "ortogonal|perpendicular",
                    "¿Qué datos geométricos necesitarías para comparar la dirección de dos vectores no nulos?")
    );

    private TheoreticalHints() {}

    /** @brief Normaliza el enunciado para reconocer formulaciones equivalentes. */
    static String normalize(String message) {
        return MARKS.matcher(Normalizer.normalize(message, Normalizer.Form.NFD)).replaceAll("")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    /** @brief Distingue una petición de razonamiento de una consulta de definición. */
    static boolean requestsReasoning(String normalized) {
        return REASONING.matcher(normalized).find();
    }

    /**
     * @param normalized Enunciado normalizado.
     * @param module Módulo activo del alumno.
     * @return Pregunta de apoyo y tema cuando los conceptos pertenecen al módulo.
     */
    static Optional<Hint> find(String normalized, String module) {
        return RULES.stream()
                .filter(rule -> rule.module().equals(module) && rule.subject().matcher(normalized).find()
                        && rule.context().matcher(normalized).find())
                .map(Rule::hint).findFirst();
    }

    private static Rule rule(String module, String topic, String subject, String context, String question) {
        return new Rule(module, Pattern.compile(subject), Pattern.compile(context), new Hint(topic, question));
    }

    record Hint(String topic, String question) {}
    private record Rule(String module, Pattern subject, Pattern context, Hint hint) {}
}
