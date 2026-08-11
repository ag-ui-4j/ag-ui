package io.github.agui4j.adk.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.agui4j.core.message.AssistantMessage;
import io.github.agui4j.core.message.Message;
import io.github.agui4j.core.message.ToolCall;
import io.github.agui4j.core.message.ToolMessage;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AgentResolverTest {

    private static ToolCall call(String id) {
        return new ToolCall(id, new io.github.agui4j.core.message.FunctionCall("toolName", "{}"));
    }

    @Test
    void resolvesAgentFromLatestToolResultViaPriorAssistantCall() {
        AssistantMessage am = new AssistantMessage("a1", null, "agentA", List.of(call("c1")));
        ToolMessage tm = new ToolMessage("t1", "result", "c1", null);
        Map<String, String> registry = Map.of("agentA", "agentA-instance");
        assertThat(AgentResolver.resolveAgentFromMessageHistory(List.of(am, tm), registry))
                .isEqualTo("agentA-instance");
    }

    @Test
    void returnsNullForNoToolLastMessageOrUnknownName() {
        Map<String, String> registry = Map.of("agentA", "agentA-instance");
        // empty / last not a tool message
        assertThat(AgentResolver.resolveAgentFromMessageHistory(List.of(), registry)).isNull();
        assertThat(AgentResolver.resolveAgentFromMessageHistory(
                List.of(new AssistantMessage("a1", "hi", "agentA", null)), registry)).isNull();
        // tool result but no matching prior assistant call -> null
        ToolMessage tm = new ToolMessage("t1", "r", "nope", null);
        assertThat(AgentResolver.resolveAgentFromMessageHistory(
                List.of(new AssistantMessage("a1", null, "agentA", List.of(call("c1"))), tm), registry)).isNull();
        // matching assistant without a name -> null
        ToolMessage tm2 = new ToolMessage("t2", "r", "c1", null);
        assertThat(AgentResolver.resolveAgentFromMessageHistory(
                List.of(new AssistantMessage("a1", null, null, List.of(call("c1"))), tm2), registry)).isNull();
        // name not present in registry -> null
        ToolMessage tm3 = new ToolMessage("t3", "r", "c1", null);
        assertThat(AgentResolver.resolveAgentFromMessageHistory(
                List.of(new AssistantMessage("a1", null, "ghost", List.of(call("c1"))), tm3), registry)).isNull();
    }
}
