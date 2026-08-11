package io.github.agui4j.adk.history;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.agui4j.core.message.AssistantMessage;
import io.github.agui4j.core.message.Message;
import io.github.agui4j.core.message.ToolCall;
import io.github.agui4j.core.message.ToolMessage;

import java.util.List;

import org.junit.jupiter.api.Test;

class ToolResultExtractorTest {

    @Test
    void resolvesToolNamesAndSkipsSyntheticConfirmChanges() {
        AssistantMessage am = new AssistantMessage("a1", null, null, List.of(
                new ToolCall("c1", new io.github.agui4j.core.message.FunctionCall("create_item", "{}")),
                new ToolCall("c2", new io.github.agui4j.core.message.FunctionCall("confirm_changes", "{}"))));
        ToolMessage item = new ToolMessage("t1", "ok", "c1", null);
        ToolMessage confirm = new ToolMessage("t2", "approved", "c2", null);
        List<ToolResultExtractor.ToolResult> out = ToolResultExtractor
                .extractToolResults(List.of(am, item, confirm));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).toolName()).isEqualTo("create_item");
        assertThat(out.get(0).message()).isSameAs(item);
    }

    @Test
    void fallsBackToUnknownNameWithoutPrecedingCall() {
        ToolMessage lone = new ToolMessage("t9", "x", "no-call", null);
        List<ToolResultExtractor.ToolResult> out = ToolResultExtractor.extractToolResults(List.of(lone));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).toolName()).isEqualTo("unknown");
        assertThat(out.get(0).message()).isSameAs(lone);
    }
}
