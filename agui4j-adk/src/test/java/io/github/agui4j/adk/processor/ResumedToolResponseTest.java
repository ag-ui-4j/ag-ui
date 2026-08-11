package io.github.agui4j.adk.processor;

import io.github.agui4j.adk.hitl.BufferedToolResult;
import io.github.agui4j.adk.hitl.NormalizedToolResult;
import io.github.agui4j.adk.hitl.PendingCallGroupKey;
import io.github.agui4j.adk.hitl.PendingCallKey;
import io.github.agui4j.adk.hitl.PendingCallScope;
import io.github.agui4j.adk.hitl.PendingStatus;
import io.github.agui4j.adk.hitl.PendingToolCall;
import io.github.agui4j.adk.hitl.ToolResultNormalizer;
import com.google.genai.types.FunctionResponse;
import io.github.agui4j.core.event.ToolCallChunkEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumedToolResponseTest {
    @Test
    void usesPersistedProviderCallIdAndExactToolNameForResumption() {
        PendingCallScope scope = new PendingCallScope("app", "user", "session");
        PendingCallGroupKey group = new PendingCallGroupKey(scope, "invocation");
        PendingToolCall call = new PendingToolCall(new PendingCallKey(group, "provider-call-id"),
                new ToolCallChunkEvent("provider-call-id", "exact_tool_name", "parent", "{}", 1L, null),
                "{}", PendingStatus.PENDING);

        FunctionResponse response = MessageProcessor.INSTANCE.constructResumedMessage(List.of(
                        new BufferedToolResult(call, new NormalizedToolResult("provider-call-id", Map.of("ok", true)))))
                .parts().orElseThrow().getFirst().functionResponse().orElseThrow();

        assertEquals("provider-call-id", response.id().orElseThrow());
        assertEquals("exact_tool_name", response.name().orElseThrow());
        assertEquals(Map.of("ok", true), response.response().orElseThrow());
    }

    @Test
    void resumedFunctionResponseCarriesExactNestedNumbers() {
        PendingCallScope scope = new PendingCallScope("app", "user", "session");
        PendingCallGroupKey group = new PendingCallGroupKey(scope, "invocation");
        PendingToolCall call = new PendingToolCall(new PendingCallKey(group, "provider-call-id"),
                new ToolCallChunkEvent("provider-call-id", "exact_tool_name", "parent", "{}", 1L, null),
                "{}", PendingStatus.PENDING);
        NormalizedToolResult normalized = new ToolResultNormalizer().normalize(
                new io.github.agui4j.core.message.ToolMessage("m",
                        "{\"decimal\":1234567890.123456789012345678901234567890,"
                                + "\"nested\":[92233720368547758081234567890]}",
                        "provider-call-id"));

        Map<?, ?> response = MessageProcessor.INSTANCE.constructResumedMessage(List.of(
                        new BufferedToolResult(call, normalized)))
                .parts().orElseThrow().getFirst().functionResponse().orElseThrow().response().orElseThrow();

        assertEquals(new BigDecimal("1234567890.123456789012345678901234567890"), response.get("decimal"));
        assertEquals(new BigInteger("92233720368547758081234567890"), ((List<?>) response.get("nested")).getFirst());
    }
}
