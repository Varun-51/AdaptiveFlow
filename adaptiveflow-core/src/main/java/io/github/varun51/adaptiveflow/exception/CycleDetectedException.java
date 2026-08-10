package io.github.varun51.adaptiveflow.exception;

/**
 * Thrown when a workflow definition contains a cycle and therefore cannot be
 * topologically sorted.
 */
public class CycleDetectedException extends ValidationException {

    public CycleDetectedException(String message) {
        super(message);
    }
}
