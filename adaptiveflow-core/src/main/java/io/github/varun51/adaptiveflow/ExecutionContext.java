package io.github.varun51.adaptiveflow;

import java.util.Map;

/**
 * Read-only view of completed task outputs. Safe to read concurrently.
 */
public interface ExecutionContext {

    /**
     * Output of a completed task, or {@code null} if it produced null.
     *
     * @throws IllegalArgumentException if the id is unknown or the task has not completed
     */
    <T> T result(String taskId);

    boolean hasResult(String taskId);

    /** Immutable snapshot of every completed output. */
    Map<String, Object> snapshot();
}
