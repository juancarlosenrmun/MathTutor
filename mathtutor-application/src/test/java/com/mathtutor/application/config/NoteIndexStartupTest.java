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
        assertThat(capture.getValue()).hasSize(3).extracting(NoteChunk::moduleCode)
                .containsExactlyInAnyOrder("funciones", "derivadas-integrales", "metodos-3d");
    }
    @Test void propagatesIndexFailureSoStartupDoesNotPretendSuccess() {
        var index = mock(NoteIndex.class);
        doThrow(new IllegalStateException("fallo de índice")).when(index).synchronize(anyList());
        assertThatThrownBy(() -> new NoteIndexStartup(index).run(null)).hasMessage("fallo de índice");
    }
}
