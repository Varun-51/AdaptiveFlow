package io.github.varun51.adaptiveflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import io.github.varun51.adaptiveflow.exception.ValidationException;
import org.junit.jupiter.api.Test;

class WorkflowBuilderTest {

    @Test
    void blankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> WorkflowBuilder.builder(" "));
        assertThrows(IllegalArgumentException.class, () -> WorkflowBuilder.builder(null));
    }

    @Test
    void builderCannotBeReused() {
        WorkflowBuilder builder = WorkflowBuilder.builder("reuse").task("a", ctx -> 1);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void retryWithNoTaskThrows() {
        assertThrows(IllegalStateException.class,
                () -> WorkflowBuilder.builder("noretry").retry(RetryPolicy.none()));
    }

    @Test
    void executeOnCustomExecutorViaBuilder() {
        WorkflowResult result = WorkflowBuilder.builder("custom-ex")
                .task("a", ctx -> 5)
                .execute(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        assertTrue(result.isSuccess());
        assertEquals(5, result.<Integer>result("a"));
    }

    @Test
    void retryAfterParallelAppliesToWholeGroup() {
        RetryPolicy policy = RetryPolicy.exponentialBackoff(3, Duration.ofMillis(5));
        Workflow workflow = WorkflowBuilder.builder("group-retry")
                .parallel(
                        WorkflowBuilder.TaskRef.of("p1", ctx -> 1),
                        WorkflowBuilder.TaskRef.of("p2", ctx -> 2))
                .retry(policy)
                .build();
        assertSame(policy, workflow.plan().tasks().get("p1").retryPolicy());
        assertSame(policy, workflow.plan().tasks().get("p2").retryPolicy());
    }

    @Test
    void retryAfterSingleTaskAppliesOnlyToIt() {
        RetryPolicy policy = RetryPolicy.fixedDelay(2, Duration.ofMillis(5));
        Workflow workflow = WorkflowBuilder.builder("single-retry")
                .task("a", ctx -> 1)
                .task("b", ctx -> 2)
                .retry(policy)
                .build();
        assertEquals(RetryPolicy.none().maxAttempts(), workflow.plan().tasks().get("a").retryPolicy().maxAttempts());
        assertSame(policy, workflow.plan().tasks().get("b").retryPolicy());
    }

    @Test
    void plannerBuildsExecutablePlanFromWorkflow() {
        Workflow workflow = WorkflowBuilder.builder("plan")
                .task("a", ctx -> 1)
                .task("b", ctx -> 2)
                .build();
        assertEquals(2, workflow.plan().ids().size());
        assertEquals("plan", workflow.plan().name());
        assertTrue(workflow.plan().tasks().containsKey("a"));
    }
}
