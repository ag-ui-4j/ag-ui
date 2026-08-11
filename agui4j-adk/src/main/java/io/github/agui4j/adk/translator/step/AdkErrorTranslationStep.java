package io.github.agui4j.adk.translator.step;

import com.google.adk.events.Event;
import io.github.agui4j.adk.translator.TranslationContext;
import io.github.agui4j.core.event.RunErrorEvent;
import io.reactivex.rxjava3.core.Flowable;

/** Converts ADK error-bearing events into one terminal protocol error. */
public enum AdkErrorTranslationStep implements EventTranslationStep {
    INSTANCE;

    private static final String ADK_EXECUTION_FAILURE = "ADK_EXECUTION_FAILURE";
    private static final String CANCELLED = "CANCELLED";

    @Override
    public Flowable<io.github.agui4j.core.event.Event> translate(Event event, TranslationContext context) {
        if (context.isTerminal()) {
            return Flowable.empty();
        }
        if (event.interrupted().orElse(false)) {
            return terminal(context, "interrupted", CANCELLED);
        }
        if (event.errorCode().isPresent()) {
            String code = event.errorCode().orElseThrow().toString();
            return terminal(context, event.errorMessage().orElse(code), code);
        }
        if (event.errorMessage().isPresent()) {
            return terminal(context, event.errorMessage().orElseThrow(), ADK_EXECUTION_FAILURE);
        }
        return Flowable.empty();
    }

    /**
     * Emits closure barriers before the public terminal error.
     *
     * @param context translation state
     * @param message terminal error message
     * @param code stable error code
     * @return closed lifecycle followed by terminal error
     */
    private static Flowable<io.github.agui4j.core.event.Event> terminal(
            TranslationContext context, String message, String code) {
        context.markTerminal();
        Flowable<io.github.agui4j.core.event.Event> closeText = context.forceCloseStreamingMessage()
                .<io.github.agui4j.core.event.Event>map(io.github.agui4j.core.event.TextMessageEndEvent::new)
                .map(Flowable::just).orElse(Flowable.empty());
        return closeText.concatWith(ReasoningTranslationStep.closeReasoning(context))
                .concatWith(Flowable.just(new RunErrorEvent(message, code, null, null)));
    }
}
