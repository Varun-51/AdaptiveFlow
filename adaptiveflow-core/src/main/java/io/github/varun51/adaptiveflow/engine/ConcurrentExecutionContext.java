package io.github.varun51.adaptiveflow.engine;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.varun51.adaptiveflow.ExecutionContext;

/**
 * Thread-safe context for the engine's own writes, exposed to tasks as the
 * read-only {@link ExecutionContext}.
 */
public final class ConcurrentExecutionContext implements ExecutionContext {

    /** ConcurrentHashMap rejects null values; this sentinel stands in for null. */
    private static final Object NULL_MARKER = new Object();

    private final Map<String, Object> completed = new ConcurrentHashMap<>();

    /** Default constructor; instances are filled by the engine during a run. */
    public ConcurrentExecutionContext() {
    }

    /**
     * Records the output of a finished task.
     *
     * @param taskId id of the task
     * @param value  produced value, possibly {@code null}
     */
    public void put(String taskId, Object value) {
        completed.put(taskId, value == null ? NULL_MARKER : value);
    }

    @Override
    public boolean hasResult(String taskId) {
        return completed.containsKey(taskId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T result(String taskId) {
        if (!completed.containsKey(taskId)) {
            throw new IllegalArgumentException("Task '" + taskId + "' has not completed");
        }
        return (T) unwrap(completed.get(taskId));
    }

    private Object unwrap(Object value) {
        return value == NULL_MARKER ? null : value;
    }

    @Override
    public Map<String, Object> snapshot() {
        Map<String, Object> copy = new ConcurrentHashMap<>();
        completed.forEach((id, value) -> copy.put(id, unwrap(value)));
        return Collections.unmodifiableMap(copy);
    }
}
