package io.github.varun51.adaptiveflow;

/**
 * Unit of work: reads the context, returns an output. May throw to trigger retry/failure.
 *
 * @param <O> type of the output produced
 */
@FunctionalInterface
public interface Task<O> {

    /**
     * Runs this task against the current execution context.
     *
     * @param ctx read-only view of completed task outputs
     * @return the output of this task, or {@code null}
     */
    O execute(ExecutionContext ctx);
}
