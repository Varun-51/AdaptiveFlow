package io.github.varun51.adaptiveflow.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.varun51.adaptiveflow.RetryPolicy;
import io.github.varun51.adaptiveflow.Task;
import io.github.varun51.adaptiveflow.TaskSpec;
import io.github.varun51.adaptiveflow.exception.CycleDetectedException;
import io.github.varun51.adaptiveflow.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DagValidatorTest {

    private final Task<Object> noop = ctx -> null;

    @Test
    void acceptsValidDag() {
        TaskSpec a = new TaskSpec("a", noop, Set.of(), RetryPolicy.none());
        TaskSpec b = new TaskSpec("b", noop, Set.of("a"), RetryPolicy.none());
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of());
        deps.put("b", Set.of("a"));
        DagValidator.validate(List.of(a, b), deps);
    }

    @Test
    void rejectsDuplicateIds() {
        TaskSpec a = new TaskSpec("a", noop, Set.of(), RetryPolicy.none());
        TaskSpec b = new TaskSpec("a", noop, Set.of(), RetryPolicy.none());
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of());
        deps.put("a", Set.of());
        ValidationException e = assertThrows(ValidationException.class,
                () -> DagValidator.validate(List.of(a, b), deps));
        assertTrue(e.getMessage().contains("Duplicate"));
    }

    @Test
    void rejectsUnknownDependency() {
        TaskSpec b = new TaskSpec("b", noop, Set.of("ghost"), RetryPolicy.none());
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("b", Set.of("ghost"));
        ValidationException e = assertThrows(ValidationException.class,
                () -> DagValidator.validate(List.of(b), deps));
        assertTrue(e.getMessage().contains("unknown"));
    }

    @Test
    void acceptsSingleIsolatedTask() {
        TaskSpec a = new TaskSpec("a", noop, Set.of(), RetryPolicy.none());
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of());
        DagValidator.validate(List.of(a), deps);
    }

    @Test
    void acceptsMultipleIndependentRoots() {
        TaskSpec a = new TaskSpec("a", noop, Set.of(), RetryPolicy.none());
        TaskSpec b = new TaskSpec("b", noop, Set.of(), RetryPolicy.none());
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of());
        deps.put("b", Set.of());
        DagValidator.validate(List.of(a, b), deps);
    }
}

class TopologicalSortTest {

    @Test
    void sortsDependenciesFirst() {
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of());
        deps.put("b", Set.of("a"));
        deps.put("c", Set.of("a", "b"));
        List<String> order = TopologicalSort.sort(Set.of("a", "b", "c"), deps);
        assertEquals(3, order.size());
        assertTrue(order.indexOf("a") < order.indexOf("b"));
        assertTrue(order.indexOf("b") < order.indexOf("c"));
    }

    @Test
    void detectsCycle() {
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of("b"));
        deps.put("b", Set.of("a"));
        assertThrows(CycleDetectedException.class,
                () -> TopologicalSort.sort(Set.of("a", "b"), deps));
    }

    @Test
    void cycleErrorNamesOnlyCycleMembers() {
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of("b"));
        deps.put("b", Set.of("a"));
        deps.put("done", Set.of());
        CycleDetectedException error = assertThrows(CycleDetectedException.class,
                () -> TopologicalSort.sort(Set.of("a", "b", "done"), deps));
        String message = error.getMessage();
        assertTrue(message.contains("a") && message.contains("b"), message);
        assertFalse(message.contains("done"), message);
    }

    @Test
    void ordersIndependentRoots() {
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        deps.put("a", Set.of());
        deps.put("b", Set.of());
        List<String> order = TopologicalSort.sort(Set.of("a", "b"), deps);
        assertEquals(2, order.size());
    }
}
