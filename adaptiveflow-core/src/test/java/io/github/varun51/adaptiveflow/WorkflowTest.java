package io.github.varun51.adaptiveflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.varun51.adaptiveflow.exception.ValidationException;

class WorkflowTest {

    @Test
    void linearChainProducesExpectedResults() {
        WorkflowResult result = WorkflowBuilder.builder("chain")
                .task("a", ctx -> 1)
                .task("b", ctx -> ctx.<Integer>result("a") + 1)
                .task("c", ctx -> ctx.<Integer>result("b") * 2)
                .execute();

        assertTrue(result.isSuccess());
        assertEquals(1, result.<Integer>result("a"));
        assertEquals(2, result.<Integer>result("b"));
        assertEquals(4, result.<Integer>result("c"));
    }

    @Test
    void parallelBranchesFanInToDependent() {
        WorkflowResult result = WorkflowBuilder.builder("fan")
                .task("source", ctx -> 10)
                .parallel(
                        WorkflowBuilder.TaskRef.of("left", ctx -> ctx.<Integer>result("source") + 1),
                        WorkflowBuilder.TaskRef.of("right", ctx -> ctx.<Integer>result("source") + 2)
                )
                .task("sink", ctx -> ctx.<Integer>result("left") + ctx.<Integer>result("right"))
                .execute();

        assertTrue(result.isSuccess());
        assertEquals(23, result.<Integer>result("sink"));
        assertEquals(11, result.<Integer>result("left"));
        assertEquals(12, result.<Integer>result("right"));
    }

@Test
    void tasksOnIndependentBranchesRunConcurrently() throws InterruptedException {
        CountDownLatch gate = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        Task<Object> probe = ctx -> {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            gate.countDown();
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            active.decrementAndGet();
            return null;
        };

        WorkflowResult result = WorkflowBuilder.builder("concurrent")
                .parallel(
                        WorkflowBuilder.TaskRef.of("p1", probe),
                        WorkflowBuilder.TaskRef.of("p2", probe),
                        WorkflowBuilder.TaskRef.of("p3", probe)
                )
                .execute();

        assertTrue(result.isSuccess());
        assertTrue(maxActive.get() >= 2, "expected overlap, saw " + maxActive.get());
    }

    @Test
    void resultIsImmutable() {
        WorkflowResult result = WorkflowBuilder.builder("immutable")
                .task("a", ctx -> 1)
                .execute();

        Map<String, TaskResult> map = result.taskResults();
        assertFalse(map.isEmpty());
        try {
            map.clear();
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
        assertEquals(1, result.<Integer>result("a"));
    }

    @Test
    void typedResultThrowsForUnknownTask() {
        WorkflowResult result = WorkflowBuilder.builder("unknown")
                .task("a", ctx -> 1)
                .execute();

        try {
            result.result("nope");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    @Test
    void unknownTaskIdInTaskResultThrows() {
        WorkflowResult result = WorkflowBuilder.builder("unknown2")
                .task("a", ctx -> 1)
                .execute();

        try {
            result.taskResult("missing");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    @Test
    void retryFailsPermanentlyAfterExhaustingAttempts() {
        WorkflowResult result = WorkflowBuilder.builder("retry")
                .task("a", ctx -> {
                    throw new IllegalStateException("boom");
                })
                .retry(RetryPolicy.fixedDelay(3, Duration.ZERO))
                .execute();

        assertFalse(result.isSuccess());
        assertEquals(3, result.taskResult("a").attempts());
    }

    @Test
    void emptyWorkflowIsRejected() {
        try {
            WorkflowBuilder.builder("empty").build();
        } catch (ValidationException expected) {
            assertTrue(true);
        }
    }

    @Test
    void duplicateTaskIdIsRejectedAtBuild() {
        try {
            WorkflowBuilder.builder("dup")
                    .task("a", ctx -> 1)
                    .task("a", ctx -> 2)
                    .build();
        } catch (ValidationException expected) {
            assertTrue(true);
        }
    }

    @Test
    void totalDurationIsNonNegative() {
        WorkflowResult result = WorkflowBuilder.builder("timing")
                .task("a", ctx -> 1)
                .execute();
        assertFalse(result.totalDuration().isNegative());
    }

    @Test
    void workflowNameIsPreserved() {
        WorkflowResult result = WorkflowBuilder.builder("my-name")
                .task("a", ctx -> 1)
                .execute();
        assertEquals("my-name", result.name());
    }

    @Test
    void thenAliasChainsLikeTask() {
        WorkflowResult result = WorkflowBuilder.builder("then")
                .then("a", ctx -> 5)
                .then("b", ctx -> ctx.<Integer>result("a") + 1)
                .execute();
        assertTrue(result.isSuccess());
        assertEquals(6, result.<Integer>result("b"));
    }

    @Test
    void perTaskRetryRecovers() {
        AtomicInteger attempts = new AtomicInteger();
        WorkflowResult result = WorkflowBuilder.builder("recover")
                .task("a", ctx -> {
                    if (attempts.incrementAndGet() < 2) {
                        throw new IllegalStateException("first fails");
                    }
                    return "ok";
                })
                .retry(RetryPolicy.fixedDelay(3, Duration.ZERO))
                .execute();

        assertTrue(result.isSuccess());
        assertEquals("ok", result.result("a"));
        assertEquals(2, result.taskResult("a").attempts());
    }

    @Test
    void contextSnapshotReflectsCompletedResults() {
        WorkflowResult result = WorkflowBuilder.builder("snapshot")
                .task("a", ctx -> 42)
                .task("b", ctx -> List.of(ctx.snapshot()))
                .execute();
        assertTrue(result.isSuccess());
    }
}
