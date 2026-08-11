package io.github.varun51.adaptiveflow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import io.github.varun51.adaptiveflow.engine.ExecutionEngine;
import io.github.varun51.adaptiveflow.exception.ValidationException;

/**
 * Fluent builder. Dependencies are implicit: a new task depends on everything
 * declared before it; {@link #parallel(TaskRef...)} groups fan out and the next
 * task depends on the whole group. Not thread-safe; use one thread per builder.
 */
public final class WorkflowBuilder {

    private final String name;
    private final List<TaskSpec> specs = new ArrayList<>();
    private final Set<String> lastAdded = new LinkedHashSet<>();
    private boolean finished;

    private WorkflowBuilder(String name) {
        this.name = name;
    }

    /**
     * Starts a new workflow definition.
     *
     * @param name name of the workflow
     * @return a new builder
     * @throws IllegalArgumentException if name is blank
     */
    public static WorkflowBuilder builder(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name must not be blank");
        }
        return new WorkflowBuilder(name);
    }

    /** Adds a task that depends on all previously added tasks.
     *
     * @param id   unique task id
     * @param task work to run
     * @return this builder
     */
    public WorkflowBuilder task(String id, Task<?> task) {
        registerTask(pendingSpec(id, task, List.copyOf(lastAdded)));
        replaceFrontier(id);
        return this;
    }

    /** Synonym for {@link #task(String, Task)} that reads like a sequence.
     *
     * @param id   unique task id
     * @param task work to run
     * @return this builder
     */
    public WorkflowBuilder then(String id, Task<?> task) {
        return task(id, task);
    }

    /** Declares siblings that run concurrently, each depending on the prior frontier.
     *
     * @param tasks members of the parallel group
     * @return this builder
     * @throws IllegalArgumentException if no members are declared
     */
    public WorkflowBuilder parallel(TaskRef... tasks) {
        if (tasks == null || tasks.length == 0) {
            throw new IllegalArgumentException("parallel() needs at least one task");
        }
        Set<String> frontier = List.copyOf(lastAdded).isEmpty()
                ? Set.of()
                : Set.copyOf(lastAdded);
        Set<String> group = new LinkedHashSet<>();
        for (TaskRef t : tasks) {
            registerTask(toSpec(t, frontier));
            group.add(t.id());
        }
        lastAdded.clear();
        lastAdded.addAll(group);
        return this;
    }

    /**
     * Attaches a retry policy to the most recently added task or parallel
     * group. Members that already declare their own retry policy keep it;
     * the group policy applies to everything else.
     *
     * @param policy retry configuration for the last-added group
     * @return this builder
     * @throws IllegalStateException if no task was added yet
     */
    public WorkflowBuilder retry(RetryPolicy policy) {
        if (specs.isEmpty()) {
            throw new IllegalStateException("No task to attach the retry policy to");
        }
        List<TaskSpec> withPolicy = new ArrayList<>(specs.size());
        for (TaskSpec spec : specs) {
            if (lastAdded.contains(spec.id()) && spec.retryPolicy().maxAttempts() == 1) {
                withPolicy.add(spec.withRetryPolicy(policy));
            } else {
                withPolicy.add(spec);
            }
        }
        specs.clear();
        specs.addAll(withPolicy);
        return this;
    }

    /** Builds and runs on a fresh virtual-thread executor that is shut down here.
     *
     * @return the outcome of the whole run
     */
    public WorkflowResult execute() {
        return new ExecutionEngine().execute(build());
    }

    /** Builds and runs on the given executor; lifecycle stays with the caller.
     *
     * @param executor backend used for task execution
     * @return the outcome of the whole run
     */
    public WorkflowResult execute(Executor executor) {
        return new ExecutionEngine().run(build(), executor);
    }

    /**
     * Validates and freezes the graph: duplicate ids, unknown dependencies, cycles.
     *
     * @return the immutable, validated workflow
     * @throws ValidationException if the definition is structurally invalid
     */
    public Workflow build() {
        if (finished) {
            throw new IllegalStateException("Builder cannot be reused");
        }
        finished = true;
        if (specs.isEmpty()) {
            throw new ValidationException("A workflow must contain at least one task");
        }
        return new Workflow(name, DagPlanner.plan(name, specs));
    }

    private TaskSpec pendingSpec(String id, Task<?> task, List<String> dependencies) {
        return new TaskSpec(id, task, new LinkedHashSet<>(dependencies), RetryPolicy.none());
    }

    private void registerTask(TaskSpec spec) {
        if (spec.id() == null || spec.id().isBlank()) {
            throw new ValidationException("Task id must not be blank");
        }
        specs.add(spec);
    }

    private void replaceFrontier(String id) {
        lastAdded.clear();
        lastAdded.add(id);
    }

    private TaskSpec toSpec(TaskRef ref, Set<String> frontier) {
        return new TaskSpec(ref.id(), ref.task(), frontier, ref.retryPolicy());
    }

    /** A single parallel-group member with its own id and retry policy. */
    public static final class TaskRef {

        private final String id;
        private final Task<?> task;
        private final RetryPolicy retryPolicy;

        private TaskRef(String id, Task<?> task, RetryPolicy retryPolicy) {
            this.id = id;
            this.task = task;
            this.retryPolicy = retryPolicy;
        }

        /** Declares a member for {@link WorkflowBuilder#parallel(TaskRef...)}.
         *
         * @param id   unique task id
         * @param task work to run
         * @return a member declaration without a retry policy
         */
        public static TaskRef of(String id, Task<?> task) {
            return new TaskRef(id, task, RetryPolicy.none());
        }

        String id() {
            return id;
        }

        Task<?> task() {
            return task;
        }

        RetryPolicy retryPolicy() {
            return retryPolicy;
        }

        /** Returns a copy of this declaration with the given policy.
         *
         * @param policy retry configuration for this member
         * @return a new member declaration
         */
        public TaskRef retry(RetryPolicy policy) {
            return new TaskRef(id, task, policy);
        }
    }
}
