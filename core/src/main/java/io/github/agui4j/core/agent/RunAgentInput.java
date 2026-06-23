package io.github.agui4j.core.agent;

import io.github.agui4j.core.message.Message;
import io.github.agui4j.core.tool.Tool;
import java.util.List;
import java.util.Objects;

/**
 * The input payload an AG-UI agent receives for a single run. It bundles the
 * conversation so far together with the tools, context and state available to
 * the agent.
 *
 * @param threadId       the conversation thread id (required)
 * @param runId          the agent run id (required)
 * @param state          the current agent state as a free-form JSON value, or
 *                       {@code null} (optional)
 * @param messages       the conversation messages; never {@code null}
 *                       (copied to an unmodifiable list)
 * @param tools          the tools available to the agent; never {@code null}
 *                       (copied to an unmodifiable list)
 * @param context        additional context entries; never {@code null}
 *                       (copied to an unmodifiable list)
 * @param forwardedProps free-form properties forwarded to the agent, or
 *                       {@code null} (optional)
 * @see <a href="https://docs.ag-ui.com/concepts/agents">AG-UI Agents</a>
 */
public record RunAgentInput(String threadId, String runId, Object state, List<Message> messages,
                            List<Tool> tools, List<Context> context, Object forwardedProps) {

    public RunAgentInput {
        Objects.requireNonNull(threadId, "threadId must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
        context = context == null ? List.of() : List.copyOf(context);
    }

    /**
     * Creates a run input with the conversation messages and tools but no state,
     * context or forwarded properties.
     *
     * @param threadId the conversation thread id
     * @param runId    the agent run id
     * @param messages the conversation messages
     * @param tools    the tools available to the agent
     */
    public RunAgentInput(String threadId, String runId, List<Message> messages, List<Tool> tools) {
        this(threadId, runId, null, messages, tools, List.of(), null);
    }
}
