package io.github.agui4j.core.message;

import java.util.Objects;

/**
 * A tool call requested by the assistant, as carried on an
 * {@link AssistantMessage}.
 *
 * @param id       a unique identifier for the tool call (required)
 * @param function the function to invoke (required)
 * @see <a href="https://docs.ag-ui.com/concepts/messages">AG-UI Messages</a>
 */
public record ToolCall(String id, FunctionCall function) {

    /** The only supported tool call type in the AG-UI protocol. */
    public static final String TYPE = "function";

    public ToolCall {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * @return the tool call type, always {@value #TYPE}
     */
    public String type() {
        return TYPE;
    }
}
