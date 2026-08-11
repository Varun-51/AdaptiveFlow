package io.github.varun51.adaptiveflow.exception;

/**
 * Thrown when a workflow definition contains a cycle and therefore cannot be
 * topologically sorted.
 */
public class CycleDetectedException extends ValidationException {

    /** Constructs with a description of the detected cycle.
     *
     * @param message details of the detected cycle
     */
    public CycleDetectedException(String message) {
        super(message);
    }
}
