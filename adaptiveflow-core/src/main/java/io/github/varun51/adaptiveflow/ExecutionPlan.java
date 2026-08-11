package io.github.varun51.adaptiveflow;

import java.util.List;
import java.util.Map;

/**
 * Immutable, validated execution plan with its dependency order frozen.
 */
public final class ExecutionPlan {

    private final String name;
    private final Map<String, TaskSpec> tasks;
    private final List<String> order;

    ExecutionPlan(String name, Map<String, TaskSpec> tasks, List<String> order) {
        this.name = name;
        this.tasks = Map.copyOf(tasks);
        this.order = List.copyOf(order);
    }

    /** Name of the workflow.
     *
     * @return name of the workflow
     */
    public String name() {
        return name;
    }

    /** Task definitions keyed by id.
     *
     * @return task definitions keyed by id
     */
    public Map<String, TaskSpec> tasks() {
        return tasks;
    }

    /** Task ids in dependency order (roots first).
     *
     * @return ids in dependency order
     */
    public List<String> ids() {
        return order;
    }
}
