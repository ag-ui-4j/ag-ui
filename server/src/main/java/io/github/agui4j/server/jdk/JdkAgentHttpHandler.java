package io.github.agui4j.server.jdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.agui4j.core.agent.Agent;
import io.github.agui4j.core.agent.RunAgentInput;
import io.github.agui4j.core.serialization.SerializationException;
import io.github.agui4j.core.serialization.Serializer;
import io.github.agui4j.server.AgentRunHandler;
import io.github.agui4j.server.EventSink;
import io.github.agui4j.server.OutputStreamEventSink;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletionException;

/**
 * An {@link HttpHandler} that exposes an {@link Agent} over the AG-UI protocol
 * using only the JDK's built-in HTTP server ({@code com.sun.net.httpserver}). It
 * is the server-side mirror of the JDK-based client and adds no third-party
 * dependencies.
 *
 * <p>The handler accepts a {@code POST} whose JSON body is a
 * {@link RunAgentInput}, runs the agent, and streams the resulting events back
 * as {@code text/event-stream}. Malformed input is rejected with
 * {@code 400 Bad Request} before streaming begins; non-{@code POST} requests
 * receive {@code 405 Method Not Allowed}.
 *
 * <pre>{@code
 * HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
 * server.createContext("/agent", new JdkAgentHttpHandler(agent, serializer));
 * server.start();
 * }</pre>
 */
public final class JdkAgentHttpHandler implements HttpHandler {

    private final AgentRunHandler handler;

    /**
     * Creates a handler for the given agent and serializer.
     *
     * @param agent      the agent to run for each request (required)
     * @param serializer the serializer used to read input and encode events
     *                   (required)
     */
    public JdkAgentHttpHandler(Agent agent, Serializer serializer) {
        this.handler = new AgentRunHandler(agent, serializer);
    }

    /**
     * Creates a handler that delegates to a pre-built {@link AgentRunHandler}.
     *
     * @param handler the run handler to delegate to (required)
     */
    public JdkAgentHttpHandler(AgentRunHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            RunAgentInput input;
            try {
                input = handler.parse(body);
            } catch (SerializationException e) {
                respondPlain(exchange, 400, "Invalid AG-UI request: " + e.getMessage());
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            // 0 => response body of arbitrary length (chunked transfer encoding).
            exchange.sendResponseHeaders(200, 0);

            EventSink sink = new OutputStreamEventSink(exchange.getResponseBody());
            try {
                // Block until the agent's event stream has been fully relayed, so
                // the exchange is not closed mid-stream. Run failures are surfaced
                // in band as a RUN_ERROR frame, so completion is never exceptional.
                handler.run(input, sink).join();
            } catch (CompletionException ignored) {
                // Defensive: the response has already started, so there is nothing
                // left to signal beyond what was relayed in band.
            }
        } finally {
            exchange.close();
        }
    }

    private static void respondPlain(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
