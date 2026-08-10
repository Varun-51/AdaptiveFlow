package io.github.varun51.adaptiveflow;

import java.time.Duration;
import java.util.Map;

/**
 * Immutable outcome of a whole workflow run.
 */
public class WorkflowResult {

    private final String name;
    private final Map<String, TaskResult> taskResults;
    private final Duration totalDuration;

    public WorkflowResult(String name, Map<String, TaskResult> taskResults,
                          Duration totalDuration) {
        this.name = name;
        this.taskResults = Map.copyOf(taskResults);
        this.totalDuration = totalDuration;
    }

    public String name() {
        return name;
    }

    /** True only if every task succeeded. */
    public boolean isSuccess() {
        return taskResults.values().stream().allMatch(TaskResult::isSuccess);
    }

    /**
     * Per-task outcome.
     *
     * @throws IllegalArgumentException if the id is unknown
     */
    public TaskResult taskResult(String taskId) {
        TaskResult result = taskResults.get(taskId);
        if (result == null) {
            throw new IllegalArgumentException("Unknown task: " + taskId);
        }
        return result;
    }

    /** Typed output of a task. Cast is the caller's responsibility. */
    @SuppressWarnings("unchecked")
    public <T> T result(String taskId) {
        return (T) taskResult(taskId).output();
    }

    public Map<String, TaskResult> taskResults() {
        return taskResults;
    }

    public Duration totalDuration() {
        return totalDuration;
    }
}
