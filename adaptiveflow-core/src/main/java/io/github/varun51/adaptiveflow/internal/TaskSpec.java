package io.github.varun51.adaptiveflow.internal;

import java.util.Set;

import io.github.varun51.adaptiveflow.RetryPolicy;
import io.github.varun51.adaptiveflow.Task;

/**
 * Immutable per-task definition: logic, prerequisite ids, retry policy.
 */
public final class TaskSpec {

    private final String id;
    private final Task<?> task;
    private final Set<String> dependencies;
    private final RetryPolicy retryPolicy;

    public TaskSpec(String id, Task<?> task, Set<String> dependencies, RetryPolicy retryPolicy) {
        this.id = id;
        this.task = task;
        this.dependencies = Set.copyOf(dependencies);
        this.retryPolicy = retryPolicy;
    }

    /** Copy of this spec with a different retry policy. */
    public TaskSpec withRetryPolicy(RetryPolicy policy) {
        return new TaskSpec(id, task, dependencies, policy);
    }

    public String id() {
        return id;
    }

    public Task<?> task() {
        return task;
    }

    /** Prerequisites that must complete before this runs. */
    public Set<String> dependencies() {
        return dependencies;
    }

    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }
}
