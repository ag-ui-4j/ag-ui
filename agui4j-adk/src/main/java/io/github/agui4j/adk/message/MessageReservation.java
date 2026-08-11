package io.github.agui4j.adk.message;

import io.github.agui4j.adk.session.ResolvedSession;
import io.github.agui4j.core.message.Message;

import java.util.List;
import java.util.Objects;

/** In-flight reservation of message IDs for one bridge invocation. */
public record MessageReservation(ResolvedSession session, List<Message> messages, String invocationId) {

    /** Defensively captures the reservation payload. */
    public MessageReservation {
        session = Objects.requireNonNull(session, "session");
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        invocationId = Objects.requireNonNull(invocationId, "invocationId");
        if (invocationId.isBlank()) {
            throw new IllegalArgumentException("invocationId must not be blank");
        }
    }
}
