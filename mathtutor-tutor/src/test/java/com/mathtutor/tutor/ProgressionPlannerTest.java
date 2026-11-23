package com.mathtutor.tutor;

import com.mathtutor.common.port.ConversationStore.BankExercise;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionPlannerTest {
    private final ProgressionPlanner planner = new ProgressionPlanner();
    private final BankExercise procedure = exercise(3L, "PROCEDIMENTAL");
    private final BankExercise theory = exercise(2L, "TEORICO");
    private final BankExercise interpretation = exercise(1L, "INTERPRETACION");
    private final List<BankExercise> options = List.of(interpretation, theory, procedure);

    @Test
    void followsThePedagogicalOrderRegardlessOfCatalogOrder() {
        assertThat(planner.next(options, Set.of())).contains(procedure);
        assertThat(planner.next(options, Set.of("PROCEDIMENTAL"))).contains(theory);
        assertThat(planner.next(options, Set.of("PROCEDIMENTAL", "TEORICO"))).contains(interpretation);
        assertThat(planner.next(options, Set.of("PROCEDIMENTAL", "TEORICO", "INTERPRETACION"))).isEmpty();
    }

    @Test
    void skipsUnavailableTypesAndKeepsTheEarliestUnpracticedStage() {
        assertThat(planner.next(List.of(interpretation, theory), Set.of())).contains(theory);
        assertThat(planner.next(List.of(interpretation), Set.of("TEORICO"))).contains(interpretation);
        assertThat(planner.next(options, Set.of("TEORICO"))).contains(procedure);
        assertThat(planner.next(List.of(), Set.of())).isEmpty();
    }

    private BankExercise exercise(long id, String type) {
        return new BankExercise(id, "Enunciado " + id, "tema", type, 1);
    }
}
