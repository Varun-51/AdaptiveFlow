package io.github.varun51.adaptiveflow.exception;

/**
 * Thrown when a workflow definition is invalid: duplicate task ids, references
 * to unknown tasks, empty workflows, or other structural violations.
 */
public class ValidationException extends AdaptiveFlowException {

    public ValidationException(String message) {
        super(message);
    }
}
