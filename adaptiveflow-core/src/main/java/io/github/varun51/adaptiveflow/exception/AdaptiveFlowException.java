package io.github.varun51.adaptiveflow.exception;

/**
 * Base runtime exception for all AdaptiveFlow errors.
 */
public class AdaptiveFlowException extends RuntimeException {

    public AdaptiveFlowException(String message) {
        super(message);
    }

    public AdaptiveFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
