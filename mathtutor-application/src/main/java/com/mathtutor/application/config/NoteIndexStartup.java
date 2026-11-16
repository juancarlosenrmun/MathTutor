package com.mathtutor.application.config;

import com.mathtutor.common.port.NoteIndex;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.tutor.LatexNotes;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/** @brief Valida todos los recursos antes de modificar el índice; un error impide el arranque. */
@Component @Profile("notes-indexing")
public class NoteIndexStartup implements ApplicationRunner {
    private final NoteIndex index;
    public NoteIndexStartup(NoteIndex index) { this.index = index; }

    @Override public void run(ApplicationArguments arguments) throws Exception {
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:notes/*.tex");
        Arrays.sort(resources, Comparator.comparing(r -> r.getFilename()));
        if (resources.length == 0) throw new IllegalStateException("No hay apuntes LaTeX en notes/");
        var chunks = new ArrayList<NoteChunk>();
        var parser = new LatexNotes();
        for (var resource : resources) {
            try (var input = resource.getInputStream()) {
                chunks.addAll(parser.parse(resource.getFilename(), new String(input.readAllBytes(), StandardCharsets.UTF_8)));
            }
        }
        index.synchronize(chunks);
    }
}
