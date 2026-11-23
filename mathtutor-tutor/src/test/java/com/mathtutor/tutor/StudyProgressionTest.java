package com.mathtutor.tutor;

import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.common.port.ConversationStore.BankExercise;
import com.mathtutor.common.port.ConversationStore.Progression;
import com.mathtutor.common.port.ConversationStore.Run;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StudyProgressionTest {
    private final ConversationStore store = mock(ConversationStore.class);
    private final StudyProgression service = new StudyProgression(store, new ProgressionPlanner());
    private final BankExercise next = new BankExercise(3L, "Enunciado", "tema", "TEORICO", 1);

    @Test
    void activatesTheRecommendedRunWithoutAnActiveExercise() {
        when(store.progression(1L, 2L)).thenReturn(new Progression(null, List.of(next), Set.of()));
        assertThat(service.advance(1L, 2L)).contains(new StudyProgression.Practice("Enunciado", "TEORICO"));
        verify(store).select(1L, 2L, 3L, null, null);
    }

    @Test
    void retainsTheExerciseWhenTheSameStageIsRequestedAgain() {
        when(store.progression(1L, 2L)).thenReturn(new Progression(active(3L), List.of(next), Set.of()));
        assertThat(service.advance(1L, 2L)).contains(new StudyProgression.Practice("Enunciado", "TEORICO"));
        verify(store, never()).select(any(), any(), any(), any(), any());
    }

    @Test
    void activatesTheNextStageWhenThePreviousProseExerciseIsStillOpen() {
        when(store.progression(1L, 2L)).thenReturn(new Progression(active(4L), List.of(next), Set.of()));
        assertThat(service.advance(1L, 2L)).isPresent();
        verify(store).select(1L, 2L, 3L, null, null);
    }

    @Test
    void leavesTheActiveExerciseUntouchedWhenTheSequenceIsComplete() {
        when(store.progression(1L, 2L)).thenReturn(new Progression(active(3L), List.of(next), Set.of("TEORICO")));
        assertThat(service.advance(1L, 2L)).isEmpty();
        verify(store, never()).select(any(), any(), any(), any(), any());
    }

    private Run active(long exerciseId) {
        return new Run(7L, exerciseId, "Enunciado", null, "tema", "COMENZADO", 0, 0, List.of());
    }
}
