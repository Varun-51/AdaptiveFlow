package io.github.varun51.adaptiveflow;

import java.time.Duration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Immutable outcome of a single task: output, error, attempts, cost, status.
 */
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP"},
        justification = "Throwable is shared by design: copies break stack traces and suppressed exceptions.")
public record TaskResult(String taskId, Object output, Throwable error,
                         int attempts, Duration duration, TaskStatus status) {

    /** True only for tasks that actually ran and succeeded. */
    public boolean isSuccess() {
        return status == TaskStatus.COMPLETED;
    }
}
