package com.mathtutor.tutor;

import com.mathtutor.common.dto.NoteChunk;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** @brief UC-14: extrae secciones del contrato de autores; no ejecuta ni compila LaTeX. */
public final class LatexNotes {
    private static final Pattern BLOCK = Pattern.compile("\\\\bloque\\{(funciones|derivadas-integrales|metodos-3d)\\}");
    private static final Pattern TOPIC = Pattern.compile("\\\\tema\\{([a-z0-9-]{1,50})\\}\\{([^{}]+)\\}");
    private static final Pattern DIFFICULTY = Pattern.compile("\\\\dificultad\\{([123])\\}");
    private static final Pattern SECTION = Pattern.compile("\\\\section\\{([^{}]+)\\}");

    /**
     * @param source Nombre estable del recurso incluido en el repositorio.
     * @param latex Documento UTF-8 con bloque, tema, dificultad y secciones de 150–400 palabras.
     * @return Fragmentos con fórmulas originales y versión derivada del contenido completo.
     * @throws IllegalArgumentException Si el documento no satisface el contrato.
     */
    public List<NoteChunk> parse(String source, String latex) {
        String clean = latex.replaceAll("(?m)(?<!\\\\)%[^\\r\\n]*", "");
        String module = required(BLOCK, clean), topic = required(TOPIC, clean);
        required(DIFFICULTY, clean);
        var sections = SECTION.matcher(clean);
        var titles = new ArrayList<String>();
        var starts = new ArrayList<Integer>();
        var ends = new ArrayList<Integer>();
        while (sections.find()) {
            titles.add(sections.group(1)); starts.add(sections.end()); ends.add(sections.start());
        }
        if (titles.isEmpty()) throw new IllegalArgumentException(source + ": faltan secciones autocontenidas");
        String version = version(latex);
        var chunks = new ArrayList<NoteChunk>();
        for (int i = 0; i < titles.size(); i++) {
            String content = clean.substring(starts.get(i), i + 1 < titles.size() ? ends.get(i + 1) : clean.length()).strip();
            int words = content.isBlank() ? 0 : content.split("\\s+").length;
            if (words < 150 || words > 400) {
                throw new IllegalArgumentException(source + ": cada sección debe contener 150–400 palabras");
            }
            chunks.add(new NoteChunk(null, titles.get(i) + "\n" + content, null, "OTRO", source, module, topic, version));
        }
        return List.copyOf(chunks);
    }

    private static String required(Pattern pattern, String content) {
        var match = pattern.matcher(content);
        if (!match.find()) throw new IllegalArgumentException("Metadatos LaTeX ausentes o inválidos: " + pattern);
        return match.group(1);
    }

    private static String version(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8))).substring(0, 20);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }
}
