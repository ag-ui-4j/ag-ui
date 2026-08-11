package io.github.agui4j.adk.input;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compatibility data unsupported by the official AG-UI core input type.
 *
 * @param parentRunId optional parent run identifier
 * @param rawToolSchemas raw root tool schemas keyed by request position
 * @param action optional request-scoped compatibility action
 */
public record AdkRunExtensions(
        String parentRunId,
        List<RawToolSchema> rawToolSchemas,
        RequestAction action) {

    public static final String FORWARDED_PROPS_KEY = "_ag_ui_4j_adk_extensions";

    /**
     * Creates an extension with no request-scoped action.
     *
     * @param parentRunId optional parent run identifier
     * @param rawToolSchemas raw root tool schemas keyed by request position
     */
    public AdkRunExtensions(String parentRunId, List<RawToolSchema> rawToolSchemas) {
        this(parentRunId, rawToolSchemas, null);
    }

    /**
     * Creates an immutable compatibility extension value.
     */
    public AdkRunExtensions {
        rawToolSchemas = List.copyOf(rawToolSchemas);
        long distinctPositions = rawToolSchemas.stream()
                .map(RawToolSchema::position)
                .distinct()
                .count();
        if (distinctPositions != rawToolSchemas.size()) {
            throw new IllegalArgumentException("raw tool schema positions must be unique");
        }
    }

    /**
     * A request action that the official AG-UI 0.2.0 input cannot express.
     */
    public sealed interface RequestAction permits ConfirmationAction, AuthAction {
    }

    /**
     * Native ADK confirmation response correlated to the synthetic confirmation function call.
     *
     * @param invocationId synthetic ADK confirmation function-call identifier
     * @param toolCallId original ADK tool-call identifier
     * @param approved whether the client approved the tool call
     */
    public record ConfirmationAction(String invocationId, String toolCallId, boolean approved)
            implements RequestAction {

        /**
         * Validates native ADK correlation identifiers.
         */
        public ConfirmationAction {
            requireId(invocationId, "invocationId");
            requireId(toolCallId, "toolCallId");
        }
    }

    /**
     * Auth input delegated only through an explicitly installed adapter.
     *
     * @param requestId client request identifier
     * @param input immutable adapter-owned input
     */
    public record AuthAction(String requestId, Map<String, Object> input) implements RequestAction {

        /**
         * Validates and copies the auth input.
         */
        public AuthAction {
            requireId(requestId, "requestId");
            Objects.requireNonNull(input, "input");
            input = Map.copyOf(new LinkedHashMap<>(input));
        }
    }

    /**
     * Requires a nonblank action identifier.
     *
     * @param value candidate identifier
     * @param name identifier name
     */
    private static void requireId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
