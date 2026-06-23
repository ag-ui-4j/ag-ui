package io.github.agui4j.core.tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The JSON Schema describing the arguments a {@link Tool} accepts. This models
 * the {@code object} schema used by AG-UI tool definitions.
 *
 * @param type       the schema type, always {@value #TYPE} for tool parameters
 *                   (required)
 * @param properties the parameter definitions keyed by name, where each value
 *                   is a JSON Schema fragment; never {@code null} (required,
 *                   copied to an unmodifiable map)
 * @param required   the names of the required parameters; never {@code null}
 *                   (required, copied to an unmodifiable list)
 * @see <a href="https://docs.ag-ui.com/concepts/tools">AG-UI Tools</a>
 */
public record ToolParameters(String type, Map<String, Object> properties, List<String> required) {

    /** The only supported schema type for tool parameters. */
    public static final String TYPE = "object";

    public ToolParameters {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        Objects.requireNonNull(required, "required must not be null");
        properties = Map.copyOf(properties);
        required = List.copyOf(required);
    }

    /**
     * Creates a tool parameters schema of type {@value #TYPE}.
     *
     * @param properties the parameter definitions keyed by name
     * @param required   the names of the required parameters
     */
    public ToolParameters(Map<String, Object> properties, List<String> required) {
        this(TYPE, properties, required);
    }
}
