package io.github.agui4j.adk;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.github.agui4j.core.agent.RunAgentInput;
import io.github.agui4j.core.message.Message;
import io.github.agui4j.core.message.Role;

import java.util.List;
import java.util.UUID;

/**
 * Immutable per-run values derived from an official AG-UI input.
 *
 * @param appName Google ADK application name
 * @param userId resolved Google ADK user identifier
 * @param sessionId Google ADK session identifier
 * @param runId AG-UI run identifier
 * @param latestMessage latest user message converted to Google content
 */
record RunContext(String appName, String userId, String sessionId, String runId, Content latestMessage) {

    /**
     * Creates a run context from the official request model.
     *
     * @param input official AG-UI run input
     * @param appName Google ADK application name
     * @param userId resolved Google ADK user identifier
     */
    RunContext(RunAgentInput input, String appName, String userId) {
        this(appName, userId, input.threadId(), extractRunId(input), extractContentFromLatestMessage(input));
    }

    /**
     * Converts the latest user message into Google content.
     *
     * @param input official AG-UI run input
     * @return converted content, or {@code null} when no user message exists
     */
    private static Content extractContentFromLatestMessage(RunAgentInput input) {
        List<Message> messages = input.messages();
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.stream()
                .filter(message -> message.role() == Role.USER)
                .reduce((first, second) -> second) // Get the last user message
                .map(message -> Content.builder()
                        .role("user")
                        .parts(io.github.agui4j.adk.input.MessageContentPartsConverter
                                .fromMessageContent(message.content()))
                        .build())
                .orElse(null);
    }

    /**
     * Uses the request run ID or generates the baseline fallback.
     *
     * @param input official AG-UI run input
     * @return request or generated run identifier
     */
    private static String extractRunId(RunAgentInput input) {
        return input.runId() != null ? input.runId() : UUID.randomUUID().toString();
    }
}
