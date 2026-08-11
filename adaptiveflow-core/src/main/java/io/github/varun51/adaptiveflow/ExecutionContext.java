package io.github.varun51.adaptiveflow;

import java.util.Map;

/**
 * Read-only view of completed task outputs. Safe to read concurrently.
 */
public interface ExecutionContext {

    /**
     * Output of a completed task, or {@code null} if it produced null.
     *
     * @param <T>     expected output type
     * @param taskId  id of the task
     * @return the task output, possibly {@code null}
     * @throws IllegalArgumentException if the id is unknown or the task has not completed
     */
    <T> T result(String taskId);

    /** Whether a task has completed and recorded an output.
     *
     * @param taskId id of the task
     * @return whether the task has completed and recorded an output
     */
    boolean hasResult(String taskId);

    /** Immutable snapshot of every completed output, keyed by task id.
     *
     * @return immutable snapshot of every completed output, keyed by task id
     */
    Map<String, Object> snapshot();
}
