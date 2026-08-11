package io.github.varun51.adaptiveflow;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-task retry configuration. Immutable.
 */
public final class RetryPolicy {

    private enum Backoff {
        NONE, FIXED, EXPONENTIAL
    }

    private final int maxAttempts;
    private final Backoff backoff;
    private final Duration initialDelay;
    private final Duration maxDelay;

    private RetryPolicy(int maxAttempts, Backoff backoff,
                        Duration initialDelay, Duration maxDelay) {
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }

    /** No retries: a task fails after its first attempt. */
    public static RetryPolicy none() {
        return new RetryPolicy(1, Backoff.NONE, Duration.ZERO, Duration.ZERO);
    }

    /**
     * Fixed wait between attempts.
     *
     * @param maxAttempts total attempts including the first (1 = no retry)
     */
    public static RetryPolicy fixedDelay(int maxAttempts, Duration delay) {
        Duration d = requireValidDelay(delay);
        return new RetryPolicy(requireAttempts(maxAttempts), Backoff.FIXED, d, d);
    }

    /**
     * Delay doubles after every failure: 1s, 2s, 4s, ... capped at {@code maxDelay}.
     *
     * @param maxDelay upper bound per attempt, or {@code null} for no cap
     */
    public static RetryPolicy exponentialBackoff(int maxAttempts,
                                                 Duration initialDelay,
                                                 Duration maxDelay) {
        Duration initial = requireValidDelay(initialDelay);
        Duration cap = maxDelay == null ? null : requireValidDelay(maxDelay);
        if (cap != null && cap.compareTo(initial) < 0) {
            throw new IllegalArgumentException("maxDelay must not be smaller than initialDelay");
        }
        return new RetryPolicy(requireAttempts(maxAttempts), Backoff.EXPONENTIAL, initial, cap);
    }

    /** Same as above without a cap. */
    public static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialDelay) {
        return exponentialBackoff(maxAttempts, initialDelay, null);
    }

    /**
     * Whether a further attempt is allowed after {@code attempted} failures (0-based).
     */
    public boolean hasMoreAttemptsAfter(int attempted) {
        return attempted < maxAttempts - 1;
    }

    /**
     * Wait before running the given attempt (0-based).
     */
    public Duration delayBefore(int attempt) {
        return switch (backoff) {
            case NONE -> Duration.ZERO;
            case FIXED -> initialDelay;
            case EXPONENTIAL -> delayForExponential(attempt);
        };
    }

    private Duration delayForExponential(int attempt) {
        long millis;
        try {
            millis = Math.multiplyExact(initialDelay.toMillis(), 1L << Math.min(attempt, 62));
        } catch (ArithmeticException overflow) {
            millis = Long.MAX_VALUE;
        }
        if (maxDelay != null && millis >= maxDelay.toMillis()) {
            return maxDelay;
        }
        return Duration.ofMillis(millis);
    }

    /**
     * Wait before the given attempt, with full jitter added to exponential
     * backoff so synchronized failures do not thundering-herd the same wait.
     * Fixed and disabled policies keep their exact delay.
     */
    public Duration delayBeforeJittered(int attempt) {
        Duration base = delayBefore(attempt);
        if (backoff != Backoff.EXPONENTIAL || base.isZero()) {
            return base;
        }
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(base.toMillis()));
    }

    int maxAttempts() {
        return maxAttempts;
    }

    private static int requireAttempts(int maxAttempts) {
        if (maxAttempts < 1 || maxAttempts > 63) {
            throw new IllegalArgumentException("maxAttempts must be between 1 and 63");
        }
        return maxAttempts;
    }

    private static Duration requireValidDelay(Duration delay) {
        Objects.requireNonNull(delay, "delay must not be null");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        return delay;
    }
}
