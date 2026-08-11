package io.github.agui4j.adk.execution;

/** A held execution admission that releases its successor when closed. */
@FunctionalInterface
public interface ExecutionLease extends AutoCloseable {

    /** Releases this lease. Repeated calls are harmless. */
    @Override
    void close();
}
