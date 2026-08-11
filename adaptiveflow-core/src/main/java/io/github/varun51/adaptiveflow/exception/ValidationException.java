package io.github.varun51.adaptiveflow.exception;

/**
 * Thrown when a workflow definition is invalid: duplicate task ids, references
 * to unknown tasks, empty workflows, or other structural violations.
 */
public class ValidationException extends AdaptiveFlowException {

    /** Constructs with a description of the validation failure.
     *
     * @param message details of the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }
}
