package io.github.agui4j.adk.translator;

import io.github.agui4j.adk.translator.step.EventTranslationStep;
import com.google.adk.events.Event;
import io.github.agui4j.core.event.CustomEvent;
import io.github.agui4j.core.event.MessagesSnapshotEvent;
import io.github.agui4j.core.event.StateSnapshotEvent;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventTranslatorBaselineTest {

    private EventTranslator eventTranslator;

    @Mock
    private TranslationContext context;
    @Mock
    private EventTranslationStep step1;
    @Mock
    private EventTranslationStep step2;

    @BeforeEach
    void setUp() {
        eventTranslator = new EventTranslator(context, List.of(step1, step2));
    }

    @Test
    void shouldTranslateStreamAndAppendDeferredEvents_whenApplied() {
        // Arrange
        Event inputEvent = Event.builder().build();
        io.github.agui4j.core.event.Event translatedEvent =
                new CustomEvent("translated", "value");
        io.github.agui4j.core.event.Event deferredEvent =
                new CustomEvent("deferred", "value");

        when(step1.translate(any(Event.class), any(TranslationContext.class))).thenReturn(Flowable.just(translatedEvent));
        when(step2.translate(any(Event.class), any(TranslationContext.class))).thenReturn(Flowable.empty());
        when(context.getAndClearDeferredConfirmEvents()).thenReturn(List.of(deferredEvent));
        when(context.stateSnapshot()).thenReturn(java.util.Map.of());

        Flowable<Event> upstream = Flowable.just(inputEvent);

        // Act
        TestSubscriber<io.github.agui4j.core.event.Event> testSubscriber = Flowable.fromPublisher(eventTranslator.apply(upstream)).test();

        // Assert
        testSubscriber.assertValueCount(2);
        testSubscriber.assertComplete();
        testSubscriber.assertValues(translatedEvent, deferredEvent);
    }
    @Test
    void terminalTailMatchesPythonStateMessagesDeferredStateOrder() {
        StateSnapshotEvent state = new StateSnapshotEvent(java.util.Map.of("status", "executing"));
        MessagesSnapshotEvent messages = new MessagesSnapshotEvent(List.of());
        CustomEvent deferred = new CustomEvent("confirm_changes", "value");
        when(context.getAndClearDeferredConfirmEvents()).thenReturn(List.of(deferred));

        eventTranslator.terminalTail(Flowable.just(state), Flowable.just(messages)).test()
                .assertComplete()
                .assertValues(state, messages, deferred, state);
        verify(context).getAndClearDeferredConfirmEvents();
    }

    @Test
    void shouldDelegateToAllSteps_whenTranslateIsCalled() {
        Event inputEvent = Event.builder().build();
        when(step1.translate(any(Event.class), any(TranslationContext.class))).thenReturn(Flowable.empty());
        when(step2.translate(any(Event.class), any(TranslationContext.class))).thenReturn(Flowable.empty());

        eventTranslator.translate(inputEvent).test().assertComplete();

        verify(step1, times(1)).translate(inputEvent, context);
        verify(step2, times(1)).translate(inputEvent, context);
    }
    @Test
    void shouldDelegateToContext_whenDeferredEventsAreCleared() {
        when(context.getAndClearDeferredConfirmEvents())
                .thenReturn(List.of(new CustomEvent("deferred", "value")));

        List<io.github.agui4j.core.event.Event> result = eventTranslator.getAndClearDeferredConfirmEvents();

        verify(context, times(1)).getAndClearDeferredConfirmEvents();
        assertEquals(1, result.size());
    }
}
