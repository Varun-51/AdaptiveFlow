package io.github.varun51.adaptiveflow.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.varun51.adaptiveflow.exception.ValidationException;

/**
 * Structural validation of the task graph before execution.
 */
public final class DagValidator {

    private DagValidator() {
    }

    /**
     * Rejects duplicate ids and references to unknown tasks. Multiple
     * independent roots are allowed on purpose - that is how parallel
     * entry-points are expressed.
     *
     * @throws ValidationException if the graph is malformed
     */
    public static void validate(List<TaskSpec> tasks,
                                Map<String, Set<String>> dependencyId) {
        List<String> ids = tasks.stream().map(TaskSpec::id).toList();

        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new ValidationException("Duplicate task ids: " + duplicates);
        }

        Set<String> allIds = Set.copyOf(ids);
        for (Map.Entry<String, Set<String>> entry : dependencyId.entrySet()) {
            for (String dependency : entry.getValue()) {
                if (!allIds.contains(dependency)) {
                    throw new ValidationException("Task '" + entry.getKey()
                            + "' depends on unknown task '" + dependency + "'");
                }
            }
        }
    }
}
