package io.github.varun51.adaptiveflow;

import java.util.List;
import java.util.Map;

import io.github.varun51.adaptiveflow.internal.TaskSpec;

/**
 * Immutable, validated, dependency-ordered plan ready for execution.
 */
public class ExecutionPlan {

    private final String name;
    private final Map<String, TaskSpec> tasks;

    ExecutionPlan(String name, Map<String, TaskSpec> tasks) {
        this.name = name;
        this.tasks = Map.copyOf(tasks);
    }

    public String name() {
        return name;
    }

    public Map<String, TaskSpec> tasks() {
        return tasks;
    }

    public List<String> ids() {
        return tasks.values().stream().map(TaskSpec::id).toList();
    }
}
