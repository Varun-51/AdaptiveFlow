package io.github.varun51.adaptiveflow;

import java.time.Duration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Immutable outcome of a single task: output, error, attempts, cost, status.
 *
 * @param taskId   id of the task
 * @param output   produced value, or {@code null} on failure
 * @param error    failure cause, or {@code null} on success
 * @param attempts total attempts made ({@code 0} when skipped)
 * @param duration wall time of the final attempt
 * @param status   terminal status of the task
 */
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP"},
        justification = "Throwable is shared by design: copies break stack traces and suppressed exceptions.")
public record TaskResult(String taskId, Object output, Throwable error,
                         int attempts, Duration duration, TaskStatus status) {

    /**
     * True only for tasks that actually ran and succeeded.
     *
     * @return whether the task completed successfully
     */
    public boolean isSuccess() {
        return status == TaskStatus.COMPLETED;
    }
}
