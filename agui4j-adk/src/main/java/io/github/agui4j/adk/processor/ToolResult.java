package io.github.agui4j.adk.processor;

import io.github.agui4j.core.message.ToolMessage;

/**
 * Imported validated tool result submitted back to Google ADK.
 *
 * @param toolName resolved tool name
 * @param message official AG-UI tool message
 */
public record ToolResult(String toolName, ToolMessage message) {
}
