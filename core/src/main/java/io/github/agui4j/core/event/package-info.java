/**
 * Event types for the AG-UI protocol.
 *
 * <p>The {@link io.github.agui4j.core.event.Event} sealed interface models a
 * single event emitted by an agent; each permitted implementation corresponds to
 * one of the event types described in the AG-UI documentation, grouped into
 * lifecycle, text message, tool call, reasoning, state management, activity and
 * special categories. The {@link io.github.agui4j.core.event.EventType} enum is
 * the discriminator carried by every event.
 *
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events</a>
 */
package io.github.agui4j.core.event;
