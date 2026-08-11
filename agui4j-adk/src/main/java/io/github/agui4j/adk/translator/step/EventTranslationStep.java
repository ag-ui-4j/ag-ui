package io.github.agui4j.adk.translator.step;

import io.github.agui4j.adk.translator.TranslationContext;
import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;

/**
 * The high-level component interface for a step in the event translation process.
 */
@FunctionalInterface
public interface EventTranslationStep {
    /**
     * Translates an ADK Event.
     * @param event The incoming ADK Event.
     * @param context The shared context for the translation stream.
     * @return A Flowable stream of BaseEvents.
     */
    Flowable<io.github.agui4j.core.event.Event> translate(Event event, TranslationContext context);
}
