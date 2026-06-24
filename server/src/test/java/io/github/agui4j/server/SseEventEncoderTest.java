package io.github.agui4j.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.agui4j.core.event.RunStartedEvent;
import org.junit.jupiter.api.Test;

class SseEventEncoderTest {

    @Test
    void encodesSingleLinePayloadAsOneDataFieldWithBlankLine() {
        SseEventEncoder encoder = new SseEventEncoder(
                FakeSerializer.serializingWith(value -> "{\"type\":\"RUN_STARTED\"}"));

        String frame = encoder.encode(new RunStartedEvent("t", "r"));

        assertEquals("data: {\"type\":\"RUN_STARTED\"}\n\n", frame);
    }

    @Test
    void encodesMultiLinePayloadAsMultipleDataFields() {
        SseEventEncoder encoder = new SseEventEncoder(
                FakeSerializer.serializingWith(value -> "line1\nline2"));

        String frame = encoder.encode(new RunStartedEvent("t", "r"));

        assertEquals("data: line1\ndata: line2\n\n", frame);
    }
}
