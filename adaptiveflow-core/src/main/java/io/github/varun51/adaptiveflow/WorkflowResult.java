package io.github.varun51.adaptiveflow;

import java.time.Duration;
import java.util.Map;

/**
 * Immutable outcome of a whole workflow run.
 */
public final class WorkflowResult {

    private final String name;
    private final Map<String, TaskResult> taskResults;
    private final Duration totalDuration;

    /** Creates an immutable snapshot of a finished run.
     *
     * @param name          workflow name
     * @param taskResults   per-task outcomes, keyed by task id
     * @param totalDuration wall time of the whole run
     */
    public WorkflowResult(String name, Map<String, TaskResult> taskResults,
                          Duration totalDuration) {
        this.name = name;
        this.taskResults = Map.copyOf(taskResults);
        this.totalDuration = totalDuration;
    }

    /** Workflow name.
     *
     * @return workflow name
     */
    public String name() {
        return name;
    }

    /** True only if every task succeeded.
     *
     * @return whether the run completed without failures
     */
    public boolean isSuccess() {
        return taskResults.values().stream().allMatch(TaskResult::isSuccess);
    }

    /**
     * Per-task outcome.
     *
     * @param taskId id of the task
     * @return the task's outcome
     * @throws IllegalArgumentException if the id is unknown
     */
    public TaskResult taskResult(String taskId) {
        TaskResult result = taskResults.get(taskId);
        if (result == null) {
            throw new IllegalArgumentException("Unknown task: " + taskId);
        }
        return result;
    }

    /** Typed output of a task. Cast is the caller's responsibility.
     *
     * @param <T>    expected output type
     * @param taskId id of the task
     * @return the task output, possibly {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T result(String taskId) {
        return (T) taskResult(taskId).output();
    }

    /** Immutable per-task outcomes.
     *
     * @return immutable per-task outcomes
     */
    public Map<String, TaskResult> taskResults() {
        return taskResults;
    }

    /** Wall time of the whole run.
     *
     * @return wall time of the whole run
     */
    public Duration totalDuration() {
        return totalDuration;
    }
}
