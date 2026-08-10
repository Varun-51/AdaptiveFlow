package io.github.varun51.adaptiveflow;

/**
 * Unit of work: reads the context, returns an output. May throw to trigger retry/failure.
 */
@FunctionalInterface
public interface Task<O> {

    O execute(ExecutionContext ctx);
}
