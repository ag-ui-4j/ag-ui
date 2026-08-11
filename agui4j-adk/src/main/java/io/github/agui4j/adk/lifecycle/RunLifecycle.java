package io.github.agui4j.adk.lifecycle;

import io.github.agui4j.adk.error.AdkAgUiErrorCode;
import io.github.agui4j.adk.error.AdkAgUiException;
import io.github.agui4j.adk.context.RequestResourceRegistry;
import io.github.agui4j.adk.execution.CancellationToken;
import io.github.agui4j.core.event.Event;
import io.github.agui4j.core.event.RunErrorEvent;
import io.github.agui4j.core.event.RunFinishedEvent;
import io.github.agui4j.core.event.RunStartedEvent;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
/** Owns public run boundaries: one start, one error signal when needed, and one finish. */
public final class RunLifecycle {
    private final String sessionId;
    private final String runId;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicReference<RunErrorEvent> failure = new AtomicReference<>();
    private final AtomicReference<Object> rawFailure = new AtomicReference<>();

    private RunLifecycle(String sessionId, String runId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.runId = Objects.requireNonNull(runId, "runId");
    }

    /**
     * Creates one lifecycle owner for an accepted public run.
     *
     * @param sessionId public session identifier
     * @param runId public run identifier
     * @return lifecycle owner
     */
    public static RunLifecycle forRun(String sessionId, String runId) {
        return new RunLifecycle(sessionId, runId);
    }

    /**
     * Adds the start and one terminal boundary to the supplied accepted work.
     *
     * @param events accepted run work
     * @return bounded public event stream
     */
    public Flowable<Event> apply(Flowable<Event> events) {
        Objects.requireNonNull(events, "events");
        Flowable<Event> accepted = events
                .takeUntil(this::isRunError)
                .concatWith(Flowable.defer(this::finishEvent))
                .onErrorResumeNext(error -> Flowable.defer(() -> {
                    RunErrorEvent runError = errorEvent(error);
                    failure.compareAndSet(null, runError);
                    rawFailure.compareAndSet(null, error);
                    return Flowable.just((Event) runError).concatWith(finishEvent());
                }));
        return Flowable.just((Event) new RunStartedEvent(sessionId, runId)).concatWith(accepted);
    }

    /**
     * Adds request cancellation and exactly-once request-resource closure to an accepted run.
     *
     * @param events accepted run work
     * @param cancellation request-local cancellation token
     * @param resources request-local closeable resource registry
     * @return bounded public event stream with request cleanup
     */
    public Flowable<Event> apply(
            Flowable<Event> events,
            CancellationToken cancellation,
            RequestResourceRegistry resources) {
        Objects.requireNonNull(cancellation, "cancellation");
        Objects.requireNonNull(resources, "resources");
        return apply(events)
                .doOnCancel(cancellation::cancel)
                .doFinally(() -> closeResources(resources));
    }

    /**
     * Records a valued run error and stops accepting further upstream events.
     *
     * @param event accepted upstream event
     * @return whether this event ends upstream delivery
     */
    private boolean isRunError(Event event) {
        if (event instanceof RunErrorEvent runError) {
            failure.compareAndSet(null, runError);
            return true;
        }
        return event instanceof RunFinishedEvent;
    }

    /**
     * Emits the bridge-owned final boundary, including a structured error outcome on failure.
     *
     * @return zero or one final boundary
     */
    private Flowable<Event> finishEvent() {
        if (!finished.compareAndSet(false, true)) {
            return Flowable.empty();
        }
        RunErrorEvent runError = failure.get();
        if (runError == null) {
            return Flowable.just(new RunFinishedEvent(sessionId, runId));
        }
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("status", "error");
        outcome.put("code", runError.code());
        outcome.put("message", runError.message());
        Object rawError = rawFailure.get() == null ? runError.rawEvent() : rawFailure.get();
        outcome.put("rawError", rawError);
        return Flowable.just(new RunFinishedEvent(
                sessionId, runId, java.util.Collections.unmodifiableMap(outcome), null, null, rawError));
    }

    /**
     * Converts an unexpected execution failure into a stable terminal error.
     *
     * @param error execution failure
     * @return coded terminal event
     */
    private static RunErrorEvent errorEvent(Throwable error) {
        if (error instanceof AdkAgUiException coded) {
            return new RunErrorEvent(message(error), coded.code().name(), null, null);
        }
        return new RunErrorEvent(message(error), AdkAgUiErrorCode.ADK_EXECUTION_FAILURE.name(), null, null);
    }

    /**
     * Closes request resources without allowing cleanup failure to create a second terminal event.
     *
     * @param resources request-owned registry
     */
    private static void closeResources(RequestResourceRegistry resources) {
        try {
            resources.close();
        } catch (RuntimeException ignored) {
            // A terminal public event already owns failure reporting.
        }
    }

    private static String message(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "Google ADK run failed" : error.getMessage();
    }
}
