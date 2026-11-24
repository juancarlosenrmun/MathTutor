package com.mathtutor.tutor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reconoce temas del módulo para admitir ejercicios nuevos aunque el índice no encuentre un ejemplo parecido. */
final class SyllabusTopics {
    private static final Map<String, List<Topic>> MODULES = Map.of(
            "funciones", List.of(
                    topic("numeros-reales-complejos", "complej\\w*|imaginari\\w*|conjugad\\w*|numeros? reales?"),
                    topic("funciones-continuidad", "funcion(?:es)?|continu(?:a|as|o|os|idad)|discontinu\\w*|limites?"
                            + "|dominio|recorrido|inyectiv\\w*|sobreyectiv\\w*|biyectiv\\w*|asintot\\w*"
                            + "|polinomi\\w*|ecuacion(?:es)?|curvas?|grafica[rs]?|representa(?:r|cion)?|f(?=\\s*\\(\\s*x\\s*\\)\\s*=)")),
            "derivadas-integrales", List.of(
                    topic("calculo-integral", "integr(?:al\\w*|ar|a|acion)|primitiva[rs]?"),
                    topic("calculo-diferencial", "deriv\\w*|tangente[rs]?|regla de la cadena|criterio de l.hopital"
                            + "|taylor|optimiz\\w*|maxim\\w*|minim\\w*")),
            "metodos-3d", List.of(
                    topic("diagonalizacion", "diagonaliz\\w*|autovalor\\w*|autovector\\w*|(?:valor|vector)(?:es)? propi\\w*"),
                    topic("espacios-vectoriales", "subespacio\\w*|espacio\\w* vectorial\\w*"
                            + "|bases? (?:canonic\\w*|ortonormal\\w*|del espacio|de vectores)"
                            + "|(?:in)?dependencia lineal|(?:in)?dependientes linealmente|combinacion lineal"),
                    topic("matrices", "matri[zc]\\w*|determinante\\w*|gauss|sistemas? (?:lineales?|de ecuaciones)"),
                    topic("geometria", "vectores?|producto (?:escalar|vectorial)|ortogonal\\w*|rectas?|planos?"
                            + "|superficies?|tridimensional\\w*|3d")));

    private SyllabusTopics() {}

    static Optional<String> find(String module, String normalized) {
        if (module.equals("funciones")
                && (match("derivadas-integrales", normalized).isPresent()
                    || match("metodos-3d", normalized).filter(topic -> !topic.equals("geometria")).isPresent())) {
            return Optional.empty();
        }
        return match(module, normalized);
    }

    private static Optional<String> match(String module, String normalized) {
        return MODULES.getOrDefault(module, List.of()).stream()
                .filter(topic -> topic.terms().matcher(normalized).find())
                .map(Topic::code).findFirst();
    }

    private static Topic topic(String code, String terms) {
        return new Topic(code, Pattern.compile("\\b(?:" + terms + ")\\b"));
    }

    private record Topic(String code, Pattern terms) {}
}
