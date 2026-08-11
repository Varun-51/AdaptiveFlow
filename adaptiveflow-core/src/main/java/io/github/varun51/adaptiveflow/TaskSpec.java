package io.github.varun51.adaptiveflow;

import java.util.Set;

/**
 * Immutable per-task definition: logic, prerequisite ids, retry policy.
 */
public final class TaskSpec {

    private final String id;
    private final Task<?> task;
    private final Set<String> dependencies;
    private final RetryPolicy retryPolicy;

    /** Creates a task definition.
     *
     * @param id           unique task id
     * @param task         work to run
     * @param dependencies prerequisite task ids
     * @param retryPolicy  retry configuration
     */
    public TaskSpec(String id, Task<?> task, Set<String> dependencies, RetryPolicy retryPolicy) {
        this.id = id;
        this.task = task;
        this.dependencies = Set.copyOf(dependencies);
        this.retryPolicy = retryPolicy;
    }

    /** Copy of this spec with a different retry policy.
     *
     * @param policy new retry configuration
     * @return a new spec with the given policy
     */
    public TaskSpec withRetryPolicy(RetryPolicy policy) {
        return new TaskSpec(id, task, dependencies, policy);
    }

    /** Unique task id.
     *
     * @return unique task id
     */
    public String id() {
        return id;
    }

    /** Work to run.
     *
     * @return work to run
     */
    public Task<?> task() {
        return task;
    }

    /** Prerequisites that must complete before this runs.
     *
     * @return prerequisite task ids
     */
    public Set<String> dependencies() {
        return dependencies;
    }

    /** Retry configuration for this task.
     *
     * @return retry configuration
     */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }
}
