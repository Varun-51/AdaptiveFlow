package io.github.varun51.adaptiveflow.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.github.varun51.adaptiveflow.RetryPolicy;
import io.github.varun51.adaptiveflow.Task;
import io.github.varun51.adaptiveflow.TaskResult;
import io.github.varun51.adaptiveflow.TaskSpec;
import io.github.varun51.adaptiveflow.TaskStatus;
import io.github.varun51.adaptiveflow.Workflow;
import io.github.varun51.adaptiveflow.WorkflowResult;
import io.github.varun51.adaptiveflow.exception.AdaptiveFlowException;

/**
 * Runs a {@link Workflow}: a task becomes runnable when all its dependencies
 * complete, so independent branches execute concurrently. Retries per task as
 * configured.
 */
public final class ExecutionEngine {

    /** Fresh per-task virtual-thread executor; caller owns its lifecycle. */
    public static ExecutorService defaultExecutor() {
        return VirtualThreadExecutor.newVirtualThreadPerTaskExecutor();
    }

    /** Runs on a default virtual-thread executor, created and shut down here. */
    public WorkflowResult execute(Workflow workflow) {
        ExecutorService executor = defaultExecutor();
        try {
            return run(workflow, executor);
        } finally {
            executor.shutdown();
        }
    }

    /** Runs on the given executor; lifecycle stays with the caller. */
    public WorkflowResult run(Workflow workflow, Executor executor) {
        Map<String, TaskSpec> tasks = workflow.plan().tasks();
        ConcurrentExecutionContext context = new ConcurrentExecutionContext();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        Map<String, TaskResult> results = new ConcurrentHashMap<>();
        Map<String, CompletableFuture<Void>> gates = new ConcurrentHashMap<>();
        Instant started = Instant.now();

        for (String id : tasks.keySet()) {
            gates.put(id, new CompletableFuture<>());
        }
        for (TaskSpec spec : tasks.values()) {
            List<CompletableFuture<Void>> dependencies = spec.dependencies().stream()
                    .map(gates::get)
                    .toList();
            CompletableFuture.allOf(dependencies.toArray(new CompletableFuture[0]))
                    .whenComplete((ignored, error) -> schedule(spec, executor, context,
                            results, gates, firstFailure));
        }

        try {
            CompletableFuture.allOf(gates.values().toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new AdaptiveFlowException("Workflow execution failed internally", e.getCause());
        }
        Instant ended = Instant.now();
        return new WorkflowResult(workflow.name(), Map.copyOf(results),
                Duration.between(started, ended));
    }

    private void schedule(TaskSpec spec, Executor executor, ConcurrentExecutionContext context,
                          Map<String, TaskResult> results,
                          Map<String, CompletableFuture<Void>> gates,
                          AtomicReference<Throwable> firstFailure) {
        if (gates.get(spec.id()).isDone()) {
            return;
        }
        if (firstFailure.get() != null) {
            gates.get(spec.id()).complete(null);
            results.put(spec.id(), skipped(spec.id()));
            return;
        }
        try {
            executor.execute(() -> runTask(spec, context, results, gates, firstFailure));
        } catch (RejectedExecutionException e) {
            if (gates.get(spec.id()).completeExceptionally(e)) {
                firstFailure.compareAndSet(null, e);
            }
        }
    }

    private void runTask(TaskSpec spec, ConcurrentExecutionContext context,
                         Map<String, TaskResult> results,
                         Map<String, CompletableFuture<Void>> gates,
                         AtomicReference<Throwable> firstFailure) {
        Throwable escaped = null;
        try {
            if (firstFailure.get() != null) {
                results.put(spec.id(), skipped(spec.id()));
                return;
            }

            RetryPolicy policy = spec.retryPolicy();
            int attempts = 0;
            while (true) {
                attempts++;
                Instant attemptStart = Instant.now();
                Throwable error = null;
                Object output = null;
                try {
                    Task<?> rawTask = spec.task();
                    output = rawTask.execute(context);
                } catch (Exception e) {
                    error = e;
                } catch (Error fatal) {
                    recordFailure(spec, fatal, attempts, attemptStart, results, firstFailure);
                    return;
                }
                if (error == null) {
                    context.put(spec.id(), output);
                    results.put(spec.id(), new TaskResult(spec.id(), output, null,
                            attempts, Duration.between(attemptStart, Instant.now()), TaskStatus.COMPLETED));
                    return;
                }
                if (!policy.hasMoreAttemptsAfter(attempts - 1)) {
                    firstFailure.compareAndSet(null, error);
                    results.put(spec.id(), new TaskResult(spec.id(), output, error,
                            attempts, Duration.between(attemptStart, Instant.now()), TaskStatus.FAILED));
                    return;
                }
                if (!sleep(policy.delayBeforeJittered(attempts))) {
                    firstFailure.compareAndSet(null, error);
                    results.put(spec.id(), new TaskResult(spec.id(), output, error,
                            attempts, Duration.between(attemptStart, Instant.now()), TaskStatus.FAILED));
                    return;
                }
            }
        } catch (Throwable t) {
            escaped = t;
        } finally {
            if (escaped != null) {
                gates.get(spec.id()).completeExceptionally(escaped);
            } else {
                gates.get(spec.id()).complete(null);
            }
        }
    }

    private static TaskResult skipped(String id) {
        return new TaskResult(id, null, null, 0, Duration.ZERO, TaskStatus.SKIPPED);
    }

    private static void recordFailure(TaskSpec spec, Throwable error, int attempts,
                                      Instant attemptStart, Map<String, TaskResult> results,
                                      AtomicReference<Throwable> firstFailure) {
        firstFailure.compareAndSet(null, error);
        results.put(spec.id(), new TaskResult(spec.id(), null, error,
                attempts, Duration.between(attemptStart, Instant.now()), TaskStatus.FAILED));
    }

    private boolean sleep(Duration duration) {
        try {
            TimeUnit.MILLISECONDS.sleep(duration.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
