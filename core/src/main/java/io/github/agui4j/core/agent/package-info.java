/**
 * Agent run types for the AG-UI protocol.
 *
 * <p>{@link io.github.agui4j.core.agent.RunAgentInput} is the payload an agent
 * receives for a single run, bundling the conversation
 * {@link io.github.agui4j.core.message.Message messages},
 * available {@link io.github.agui4j.core.tool.Tool tools},
 * {@link io.github.agui4j.core.agent.Context} entries and state. An agent
 * responds by emitting a stream of {@link io.github.agui4j.core.event.Event}s.
 *
 * @see <a href="https://docs.ag-ui.com/concepts/agents">AG-UI Agents</a>
 */
package io.github.agui4j.core.agent;
