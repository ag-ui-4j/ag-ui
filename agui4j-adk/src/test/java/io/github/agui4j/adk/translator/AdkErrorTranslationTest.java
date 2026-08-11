package io.github.agui4j.adk.translator;

import com.google.adk.events.Event;
import com.google.adk.events.EventActions;
import com.google.genai.types.FinishReason;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.Part;
import io.github.agui4j.adk.translator.step.AdkErrorTranslationStep;
import io.github.agui4j.adk.translator.step.ReasoningTranslationStep;
import io.github.agui4j.adk.translator.step.StateTranslationStep;
import io.github.agui4j.adk.translator.step.TextTranslationStep;
import io.github.agui4j.adk.translator.step.ToolCallTranslationStep;
import io.github.agui4j.adk.translator.step.ToolResultTranslationStep;
import io.github.agui4j.core.event.RunErrorEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdkErrorTranslationTest {
    @Test
    void translatesProviderCodeMessageInterruptionAndMalformedEventsToOneTerminalError() {
        List<io.github.agui4j.core.event.Event> coded = translate(Event.builder()
                .errorCode(new FinishReason("SAFETY")).errorMessage("blocked").build());
        assertThat(coded).containsExactly(new RunErrorEvent("blocked", "SAFETY", null, null));

        List<io.github.agui4j.core.event.Event> interrupted = translate(Event.builder().interrupted(true).build());
        assertThat(interrupted).containsExactly(new RunErrorEvent("interrupted", "CANCELLED", null, null));

        List<io.github.agui4j.core.event.Event> malformed = translate(Event.builder().errorMessage("broken").build());
        assertThat(malformed).containsExactly(new RunErrorEvent("broken", "ADK_EXECUTION_FAILURE", null, null));
    }

    @Test
    void translatorGeneratedTerminalErrorSuppressesLaterProviderTranslationAndCompletionFlushes() {
        Event conflictingCalls = Event.builder().author("model").content(com.google.genai.types.Content.builder().parts(
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("first").args(java.util.Map.of()).build()).build(),
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("second").args(java.util.Map.of()).build()).build())
                .build()).build();
        Event laterState = Event.builder().actions(EventActions.builder().stateDelta(java.util.Map.of("phase", "later")).build()).build();

        List<io.github.agui4j.core.event.Event> events = io.reactivex.rxjava3.core.Flowable.fromPublisher(
                EventTranslatorFactory.INSTANCE.create("thread", "run")
                        .apply(io.reactivex.rxjava3.core.Flowable.just(conflictingCalls, laterState))).toList().blockingGet();

        assertThat(events).containsExactly(new RunErrorEvent("Event encoding failed", "ENCODING_ERROR", null, null));
    }

    @Test
    void translatorGeneratedEncodingErrorIncludesStableCode() {
        Event conflictingCalls = Event.builder().author("model").content(com.google.genai.types.Content.builder().parts(
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("first").args(java.util.Map.of()).build()).build(),
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("second").args(java.util.Map.of()).build()).build())
                .build()).build();

        assertThat(translate(conflictingCalls)).containsExactly(
                new RunErrorEvent("Event encoding failed", "ENCODING_ERROR", null, null));
    }

    @Test
    void closesPartialReasoningBeforeTranslatorGeneratedEncodingError() {
        Event partialReasoning = Event.builder().id("thought-1").author("model").partial(true)
                .content(com.google.genai.types.Content.builder().parts(
                        Part.builder().text("considering").thought(true).build()).build()).build();
        Event conflictingCalls = Event.builder().author("model").content(com.google.genai.types.Content.builder().parts(
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("first").args(java.util.Map.of()).build()).build(),
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("second").args(java.util.Map.of()).build()).build())
                .build()).build();

        List<io.github.agui4j.core.event.Event> events = io.reactivex.rxjava3.core.Flowable.fromPublisher(
                EventTranslatorFactory.INSTANCE.create("thread", "run")
                        .apply(io.reactivex.rxjava3.core.Flowable.just(partialReasoning, conflictingCalls)))
                .toList().blockingGet();

        assertThat(events).extracting(Object::getClass).containsExactly(
                io.github.agui4j.core.event.ReasoningStartEvent.class,
                io.github.agui4j.core.event.ReasoningMessageStartEvent.class,
                io.github.agui4j.core.event.ReasoningMessageContentEvent.class,
                io.github.agui4j.core.event.ReasoningMessageEndEvent.class,
                io.github.agui4j.core.event.ReasoningEndEvent.class,
                RunErrorEvent.class);
        assertThat(events.getLast()).isEqualTo(new RunErrorEvent("Event encoding failed", "ENCODING_ERROR", null, null));
    }

    @Test
    void ignoresLongRunningToolIdsAfterTranslatorGeneratedTerminalError() {
        TranslationContext context = new TranslationContext("thread", "run");
        context.populateLongRunningToolIds(Set.of("before"));
        EventTranslator translator = new EventTranslator(context, List.of(
                AdkErrorTranslationStep.INSTANCE, ReasoningTranslationStep.INSTANCE, TextTranslationStep.INSTANCE,
                ToolCallTranslationStep.INSTANCE, ToolResultTranslationStep.INSTANCE, StateTranslationStep.INSTANCE));
        Event conflictingCalls = Event.builder().author("model").content(com.google.genai.types.Content.builder().parts(
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("first").args(java.util.Map.of()).build()).build(),
                Part.builder().functionCall(FunctionCall.builder().id("duplicate").name("second").args(java.util.Map.of()).build()).build())
                .build()).build();
        Event laterLongRunningToolIds = Event.builder().longRunningToolIds(Set.of("after")).build();

        List<io.github.agui4j.core.event.Event> events = io.reactivex.rxjava3.core.Flowable.fromPublisher(
                translator.apply(io.reactivex.rxjava3.core.Flowable.just(conflictingCalls, laterLongRunningToolIds)))
                .toList().blockingGet();

        assertThat(events).containsExactly(new RunErrorEvent("Event encoding failed", "ENCODING_ERROR", null, null));
        assertThat(context.isLongRunningTool("before")).isTrue();
        assertThat(context.isLongRunningTool("after")).isFalse();
    }

    @Test
    void closesPartialTextBeforeProviderTerminalError() {
        Event partial = Event.builder().author("model").partial(true)
                .content(com.google.genai.types.Content.builder().parts(com.google.genai.types.Part.fromText("Working")).build()).build();
        Event error = Event.builder().errorCode(new FinishReason("SAFETY")).errorMessage("blocked").build();

        List<io.github.agui4j.core.event.Event> events = io.reactivex.rxjava3.core.Flowable.fromPublisher(
                EventTranslatorFactory.INSTANCE.create("thread", "run").apply(io.reactivex.rxjava3.core.Flowable.just(partial, error)))
                .toList().blockingGet();

        assertThat(events).extracting(Object::getClass).containsExactly(
                io.github.agui4j.core.event.TextMessageStartEvent.class,
                io.github.agui4j.core.event.TextMessageContentEvent.class,
                io.github.agui4j.core.event.TextMessageEndEvent.class, RunErrorEvent.class);
        assertThat(events.getLast()).isEqualTo(new RunErrorEvent("blocked", "SAFETY", null, null));
    }

    private static List<io.github.agui4j.core.event.Event> translate(Event event) {
        return io.reactivex.rxjava3.core.Flowable.fromPublisher(EventTranslatorFactory.INSTANCE.create("thread", "run")
                .apply(io.reactivex.rxjava3.core.Flowable.just(event))).toList().blockingGet();
    }
}
