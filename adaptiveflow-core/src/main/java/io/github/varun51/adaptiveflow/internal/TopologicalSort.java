package io.github.varun51.adaptiveflow.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.varun51.adaptiveflow.exception.CycleDetectedException;

/**
 * Kahn's algorithm: every dependency ordered before its dependent.
 */
public final class TopologicalSort {

    private TopologicalSort() {
    }

    /**
     * Task ids in dependency order (roots first).
     *
     * @param tasks        ids of every task in the graph
     * @param dependencyId map of task id to its prerequisite ids
     * @return task ids in dependency order
     * @throws CycleDetectedException if the graph contains a cycle
     */
    public static List<String> sort(Set<String> tasks,
                                    Map<String, Set<String>> dependencyId) {
        Map<String, Integer> remainingDependencies = new HashMap<>();
        for (String id : tasks) {
            remainingDependencies.put(id, dependencyId.getOrDefault(id, Set.of()).size());
        }

        List<String> ready = new ArrayList<>();
        Map<String, Set<String>> dependents = buildDependents(tasks, dependencyId);
        for (String id : tasks) {
            if (remainingDependencies.get(id) == 0) {
                ready.add(id);
            }
        }

        List<String> order = new ArrayList<>();
        int processed = 0;
        while (!ready.isEmpty()) {
            String current = ready.remove(ready.size() - 1);
            order.add(current);
            processed++;
            Set<String> downstream = dependents.getOrDefault(current, Set.of());
            for (String dependent : downstream) {
                int left = remainingDependencies.get(dependent) - 1;
                remainingDependencies.put(dependent, left);
                if (left == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (processed != tasks.size()) {
            String cycled = remainingDependencies.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new CycleDetectedException(
                    "Workflow contains a cycle involving tasks: " + cycled);
        }
        return List.copyOf(order);
    }

    private static Map<String, Set<String>> buildDependents(Set<String> tasks,
                                                            Map<String, Set<String>> dependencyId) {
        Map<String, Set<String>> dependents = new HashMap<>();
        for (String id : tasks) {
            dependents.put(id, new LinkedHashSet<>());
        }
        for (Map.Entry<String, Set<String>> entry : dependencyId.entrySet()) {
            for (String dependency : entry.getValue()) {
                dependents.get(dependency).add(entry.getKey());
            }
        }
        return dependents;
    }
}
