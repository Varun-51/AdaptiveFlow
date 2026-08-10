package io.github.varun51.adaptiveflow;

/**
 * Terminal state of a task within a workflow run.
 */
public enum TaskStatus {
    /** Ran to completion and produced an output. */
    COMPLETED,
    /** Exhausted its retries and failed. */
    FAILED,
    /** Never ran: an upstream task failed first. */
    SKIPPED
}
