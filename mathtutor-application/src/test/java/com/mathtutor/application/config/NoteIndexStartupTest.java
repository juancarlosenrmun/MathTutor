package com.mathtutor.application.config;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.NoteIndex;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/** UC-14: los recursos reales cumplen el contrato y se entregan antes del arranque. */
class NoteIndexStartupTest {
    @Test void loadsAllBundledTopicsInOneSnapshot() throws Exception {
        var index = mock(NoteIndex.class);
        new NoteIndexStartup(index).run(null);
        @SuppressWarnings("unchecked") ArgumentCaptor<List<NoteChunk>> capture = ArgumentCaptor.forClass(List.class);
        verify(index).synchronize(capture.capture());
        assertThat(capture.getValue()).hasSize(121).extracting(NoteChunk::moduleCode)
                .containsOnly("funciones", "derivadas-integrales", "metodos-3d");
        assertThat(capture.getValue()).extracting(NoteChunk::topicCode).containsOnly(
                "numeros-reales-complejos", "funciones-continuidad", "calculo-diferencial", "calculo-integral",
                "matrices", "espacios-vectoriales", "diagonalizacion", "geometria");
        assertThat(capture.getValue()).allSatisfy(chunk -> {
            assertThat(chunk.sourceFile()).startsWith("industriales-");
            assertThat(chunk.content()).doesNotContain("\\tcblower", "\\label{");
        });
    }
    @Test void propagatesIndexFailureSoStartupDoesNotPretendSuccess() {
        var index = mock(NoteIndex.class);
        doThrow(new IllegalStateException("fallo de índice")).when(index).synchronize(anyList());
        assertThatThrownBy(() -> new NoteIndexStartup(index).run(null)).hasMessage("fallo de índice");
    }
}
