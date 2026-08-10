package io.github.varun51.adaptiveflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.varun51.adaptiveflow.RetryPolicy;
import io.github.varun51.adaptiveflow.TaskStatus;
import io.github.varun51.adaptiveflow.Workflow;
import io.github.varun51.adaptiveflow.WorkflowBuilder;
import io.github.varun51.adaptiveflow.WorkflowResult;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConcurrentExecutionContextTest {

    @Test
    void putAndRead() {
        ConcurrentExecutionContext ctx = new ConcurrentExecutionContext();
        ctx.put("a", 42);
        assertTrue(ctx.hasResult("a"));
        assertEquals(42, ctx.<Integer>result("a"));
        assertFalse(ctx.hasResult("missing"));
    }

    @Test
    void readUnknownThrows() {
        ConcurrentExecutionContext ctx = new ConcurrentExecutionContext();
        assertThrows(IllegalArgumentException.class, () -> ctx.result("missing"));
    }

    @Test
    void snapshotIsStable() {
        ConcurrentExecutionContext ctx = new ConcurrentExecutionContext();
        ctx.put("a", 1);
        Map<String, Object> snapshot = ctx.snapshot();
        ctx.put("b", 2);
        assertEquals(1, snapshot.size());
        assertFalse(snapshot.containsKey("b"));
    }
}

class ExecutionEngineTest {

    @Test
    void runsWithVirtualThreadExecutor() {
        Workflow workflow = WorkflowBuilder.builder("vt")
                .task("a", ctx -> 1)
                .task("b", ctx -> ctx.<Integer>result("a") + 1)
                .build();
        WorkflowResult result = new ExecutionEngine().execute(workflow);
        assertTrue(result.isSuccess());
        assertEquals(2, result.<Integer>result("b"));
    }

    @Test
    void runsWithCustomExecutor() {
        Workflow workflow = WorkflowBuilder.builder("custom")
                .task("a", ctx -> "x")
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            WorkflowResult result = new ExecutionEngine().run(workflow, executor);
            assertTrue(result.isSuccess());
            assertEquals("x", result.result("a"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void failureStopsWorkflowAndReportsError() {
        AtomicInteger downstream = new AtomicInteger();
        Workflow workflow = WorkflowBuilder.builder("fail")
                .task("boom", ctx -> {
                    throw new IllegalArgumentException("kaboom");
                })
                .task("after", ctx -> downstream.incrementAndGet())
                .build();
        WorkflowResult result = new ExecutionEngine().execute(workflow);
        assertFalse(result.isSuccess());
        assertEquals(0, downstream.get());
        assertTrue(result.taskResult("boom").error() instanceof IllegalArgumentException);
        assertEquals(TaskStatus.SKIPPED, result.taskResult("after").status());
    }

    @Test
    void virtualThreadExecutorFactoryWorks() {
        ExecutorService executor = VirtualThreadExecutor.newVirtualThreadPerTaskExecutor();
        assertFalse(executor.isShutdown());
        executor.shutdown();
    }
}
