package io.github.varun51.adaptiveflow;

import java.util.List;
import java.util.Map;

/**
 * Immutable, validated execution plan with its dependency order frozen.
 */
public class ExecutionPlan {

    private final String name;
    private final Map<String, TaskSpec> tasks;
    private final List<String> order;

    ExecutionPlan(String name, Map<String, TaskSpec> tasks, List<String> order) {
        this.name = name;
        this.tasks = Map.copyOf(tasks);
        this.order = List.copyOf(order);
    }

    public String name() {
        return name;
    }

    public Map<String, TaskSpec> tasks() {
        return tasks;
    }

    /** Task ids in dependency order (roots first). */
    public List<String> ids() {
        return order;
    }
}
