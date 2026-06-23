/**
 * Message types for the AG-UI protocol.
 *
 * <p>The {@link io.github.agui4j.core.message.Message} sealed interface models a
 * single conversation message; its permitted implementations
 * ({@link io.github.agui4j.core.message.DeveloperMessage},
 * {@link io.github.agui4j.core.message.SystemMessage},
 * {@link io.github.agui4j.core.message.AssistantMessage},
 * {@link io.github.agui4j.core.message.UserMessage} and
 * {@link io.github.agui4j.core.message.ToolMessage}) correspond to the message
 * types described in the AG-UI documentation.
 *
 * @see <a href="https://docs.ag-ui.com/concepts/messages">AG-UI Messages</a>
 */
package io.github.agui4j.core.message;
