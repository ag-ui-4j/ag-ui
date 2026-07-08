package io.github.agui4j.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.agui4j.core.interrupt.Interrupt;
import io.github.agui4j.core.interrupt.InterruptOutcome;
import io.github.agui4j.core.interrupt.RunOutcome;
import io.github.agui4j.core.interrupt.SuccessOutcome;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunFinishedEventTest {

    @Test
    void requiresThreadAndRunId() {
        assertThrows(NullPointerException.class, () -> new RunFinishedEvent(null, "r1"));
        assertThrows(NullPointerException.class, () -> new RunFinishedEvent("t1", null));
    }

    @Test
    void hasRunFinishedType() {
        assertEquals(EventType.RUN_FINISHED, new RunFinishedEvent("t1", "r1").type());
    }

    @Test
    void convenienceConstructorLeavesOutcomeNull() {
        assertNull(new RunFinishedEvent("t1", "r1").outcome());
    }

    @Test
    void carriesASuccessOutcome() {
        RunOutcome outcome = new SuccessOutcome();
        RunFinishedEvent event = new RunFinishedEvent("t1", "r1", outcome, null, null, null);
        assertEquals(outcome, event.outcome());
    }

    @Test
    void carriesAnInterruptOutcome() {
        InterruptOutcome outcome =
                new InterruptOutcome(List.of(new Interrupt("i1", "confirmation", "Proceed?")));
        RunFinishedEvent event = new RunFinishedEvent("t1", "r1", outcome, null, null, null);

        InterruptOutcome actual = assertInstanceOf(InterruptOutcome.class, event.outcome());
        assertEquals("i1", actual.interrupts().get(0).id());
    }
}
