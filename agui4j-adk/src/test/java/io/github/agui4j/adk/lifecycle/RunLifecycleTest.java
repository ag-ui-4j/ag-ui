package io.github.agui4j.adk.lifecycle;

import io.github.agui4j.core.event.Event;
import io.github.agui4j.core.event.RunErrorEvent;
import io.github.agui4j.core.event.RunFinishedEvent;
import io.github.agui4j.core.event.RunStartedEvent;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RunLifecycleTest {
    @Test
    void publicLifecycleSuccessfulRunEmitsExactlyOneStartAndOneFinish() {
        List<Event> events = RunLifecycle.forRun("session", "run")
                .apply(Flowable.empty())
                .toList().blockingGet();

        assertThat(events).containsExactly(
                new RunStartedEvent("session", "run"),
                new RunFinishedEvent("session", "run"));
    }

    @Test
    void acceptedRunEmitsErrorThenStructuredFinishedOutcome() {
        List<Event> events = RunLifecycle.forRun("session", "run")
                .apply(Flowable.concatArray(
                        Flowable.just(new RunErrorEvent("failed", "ADK_EXECUTION_FAILURE", null, null)),
                        Flowable.just(new RunFinishedEvent("session", "run"))))
                .toList().blockingGet();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo(new RunStartedEvent("session", "run"));
        assertThat(events.get(1)).isEqualTo(new RunErrorEvent("failed", "ADK_EXECUTION_FAILURE", null, null));
        assertErrorFinish(events.get(2), "ADK_EXECUTION_FAILURE", "failed", null);
    }

    @Test
    void convertsUnexpectedFailureIntoCodedErrorAndStructuredFinish() {
        List<Event> events = RunLifecycle.forRun("session", "run")
                .apply(Flowable.error(new IllegalStateException("ADK unavailable")))
                .toList().blockingGet();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo(new RunStartedEvent("session", "run"));
        RunErrorEvent error = (RunErrorEvent) events.get(1);
        assertThat(error.message()).isEqualTo("ADK unavailable");
        assertThat(error.code()).isEqualTo("ADK_EXECUTION_FAILURE");
        assertThat(error.rawEvent()).isNull();
        assertErrorFinish(events.get(2), "ADK_EXECUTION_FAILURE", "ADK unavailable", IllegalStateException.class);
    }

    @Test
    void publicLifecycleUnexpectedFailureExposesFourFieldAdkExecutionFailure() {
        List<Event> events = RunLifecycle.forRun("session", "run")
                .apply(Flowable.error(new IllegalStateException("runner exploded")))
                .toList().blockingGet();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo(new RunStartedEvent("session", "run"));
        RunErrorEvent error = (RunErrorEvent) events.get(1);
        assertThat(error.message()).isEqualTo("runner exploded");
        assertThat(error.code()).isEqualTo("ADK_EXECUTION_FAILURE");
        assertThat(error.rawEvent()).isNull();
        assertErrorFinish(events.get(2), "ADK_EXECUTION_FAILURE", "runner exploded", IllegalStateException.class);
    }

    @Test
    void valuedErrorFollowedByUpstreamFailureKeepsTheFirstTerminalOwner() {
        List<Event> events = RunLifecycle.forRun("session", "run")
                .apply(Flowable.concatArray(
                        Flowable.just(new RunErrorEvent("first", "SESSION_FAILURE", null, null)),
                        Flowable.error(new IllegalStateException("late ADK failure"))))
                .toList().blockingGet();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo(new RunStartedEvent("session", "run"));
        assertThat(events.get(1)).isEqualTo(new RunErrorEvent("first", "SESSION_FAILURE", null, null));
        assertErrorFinish(events.get(2), "SESSION_FAILURE", "first", null);
    }

    @Test
    void publicLifecycleValuedErrorRetainsSoleTerminalOwnershipAcrossFinishAndFailure() {
        RunErrorEvent valuedError = new RunErrorEvent(
                "session unavailable", "SESSION_FAILURE", null, null);

        List<Event> events = RunLifecycle.forRun("session", "run")
                .apply(Flowable.concatArray(
                        Flowable.just(valuedError),
                        Flowable.just(new RunFinishedEvent("session", "run")),
                        Flowable.error(new IllegalStateException("late reactive failure"))))
                .toList().blockingGet();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo(new RunStartedEvent("session", "run"));
        assertThat(events.get(1)).isEqualTo(valuedError);
        assertErrorFinish(events.get(2), "SESSION_FAILURE", "session unavailable", null);
    }

    @SuppressWarnings("unchecked")
    private static void assertErrorFinish(
            Event event, String code, String message, Object expectedRawError) {
        assertThat(event).isInstanceOf(RunFinishedEvent.class);
        RunFinishedEvent finished = (RunFinishedEvent) event;
        assertThat(finished.threadId()).isEqualTo("session");
        assertThat(finished.runId()).isEqualTo("run");
        java.util.Map<String, Object> outcome = (java.util.Map<String, Object>) finished.outcome();
        assertThat(outcome).containsEntry("status", "error")
                .containsEntry("code", code)
                .containsEntry("message", message);
        if (expectedRawError instanceof Class<?> type) {
            assertThat(outcome.get("rawError")).isInstanceOf(type);
            assertThat(finished.rawEvent()).isInstanceOf(type);
        } else {
            assertThat(outcome).containsEntry("rawError", expectedRawError);
            assertThat(finished.rawEvent()).isSameAs(expectedRawError);
        }
    }
}
