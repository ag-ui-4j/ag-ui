package io.github.agui4j.adk.encoding;

import io.github.agui4j.core.event.ToolCallChunkEvent;

/** Encodes one official frontend-call event before it can become client-visible. */
@FunctionalInterface
public interface CanonicalEventEncoder {
    EncodedEvent encode(ToolCallChunkEvent event);
}
