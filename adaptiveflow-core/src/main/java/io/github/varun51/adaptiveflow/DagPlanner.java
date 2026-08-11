package io.github.varun51.adaptiveflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.varun51.adaptiveflow.internal.DagValidator;
import io.github.varun51.adaptiveflow.internal.TopologicalSort;

/**
 * Gateway from task definitions to an executable {@link ExecutionPlan}:
 * validate, order, freeze.
 */
public final class DagPlanner {

    private DagPlanner() {
    }

    /**
     * Validates and orders the given specs into an immutable plan.
     *
     * @param name  name of the workflow
     * @param specs task definitions to plan
     * @return an immutable, validated execution plan
     */
    public static ExecutionPlan plan(String name, List<TaskSpec> specs) {
        Map<String, TaskSpec> byId = new LinkedHashMap<>();
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        for (TaskSpec spec : specs) {
            byId.put(spec.id(), spec);
            dependencies.put(spec.id(), Set.copyOf(spec.dependencies()));
        }

        DagValidator.validate(specs, dependencies);

        List<String> order = TopologicalSort.sort(byId.keySet(), dependencies);

        return new ExecutionPlan(name, byId, order);
    }
}
