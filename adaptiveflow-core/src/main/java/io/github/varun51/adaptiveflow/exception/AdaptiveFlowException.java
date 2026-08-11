package io.github.varun51.adaptiveflow.exception;

/**
 * Base runtime exception for all AdaptiveFlow errors.
 */
public class AdaptiveFlowException extends RuntimeException {

    /** Constructs with a detail message.
     *
     * @param message detail message
     */
    public AdaptiveFlowException(String message) {
        super(message);
    }

    /** Constructs with a detail message and a cause.
     *
     * @param message detail message
     * @param cause   underlying cause
     */
    public AdaptiveFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
