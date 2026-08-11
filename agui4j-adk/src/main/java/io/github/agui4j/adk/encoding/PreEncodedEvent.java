package io.github.agui4j.adk.encoding;

import io.github.agui4j.core.event.ToolCallChunkEvent;

import java.util.Objects;

/** Exact wire JSON paired with the official tool-call event at the sealed Event boundary. */
public record PreEncodedEvent(ToolCallChunkEvent delegate, String json) {
    public PreEncodedEvent {
        delegate = Objects.requireNonNull(delegate, "delegate");
        json = Objects.requireNonNull(json, "json");
    }
}
