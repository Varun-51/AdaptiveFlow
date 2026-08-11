package io.github.varun51.adaptiveflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void noneNeverRetries() {
        RetryPolicy policy = RetryPolicy.none();
        assertFalse(policy.hasMoreAttemptsAfter(0));
        assertEquals(Duration.ZERO, policy.delayBefore(0));
    }

    @Test
    void fixedDelayRepeatsSameGap() {
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(50));
        assertTrue(policy.hasMoreAttemptsAfter(0));
        assertTrue(policy.hasMoreAttemptsAfter(1));
        assertFalse(policy.hasMoreAttemptsAfter(2));
        assertEquals(Duration.ofMillis(50), policy.delayBefore(1));
        assertEquals(Duration.ofMillis(50), policy.delayBefore(2));
    }

    @Test
    void exponentialBackoffDoublesAndCaps() {
        RetryPolicy policy = RetryPolicy.exponentialBackoff(
                5, Duration.ofMillis(10), Duration.ofMillis(100));
        assertEquals(Duration.ofMillis(10), policy.delayBefore(0));
        assertEquals(Duration.ofMillis(20), policy.delayBefore(1));
        assertEquals(Duration.ofMillis(40), policy.delayBefore(2));
        assertEquals(Duration.ofMillis(80), policy.delayBefore(3));
        assertEquals(Duration.ofMillis(100), policy.delayBefore(4));
    }

    @Test
    void exponentialBackoffWithoutCap() {
        RetryPolicy policy = RetryPolicy.exponentialBackoff(4, Duration.ofMillis(5));
        assertEquals(Duration.ofMillis(5), policy.delayBefore(0));
        assertEquals(Duration.ofMillis(10), policy.delayBefore(1));
        assertEquals(Duration.ofMillis(20), policy.delayBefore(2));
    }

    @Test
    void rejectsZeroAttempts() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.fixedDelay(0, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsAttemptsAboveShiftRange() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.fixedDelay(64, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsNegativeAttempts() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.exponentialBackoff(-1, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsNegativeDelay() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.fixedDelay(2, Duration.ofMillis(-1)));
    }

    @Test
    void rejectsNullDelay() {
        assertThrows(NullPointerException.class,
                () -> RetryPolicy.fixedDelay(2, null));
    }

    @Test
    void singleAttemptHasNoRetries() {
        RetryPolicy policy = RetryPolicy.fixedDelay(1, Duration.ofSeconds(1));
        assertFalse(policy.hasMoreAttemptsAfter(0));
    }

    @Test
    void jitterScattersExponentialDelayWithinRange() {
        RetryPolicy policy = RetryPolicy.exponentialBackoff(
                5, Duration.ofMillis(10), Duration.ofMillis(100));
        for (int i = 0; i < 50; i++) {
            long j = policy.delayBeforeJittered(2).toMillis();
            assertTrue(j >= 0 && j < 40, "jittered delay out of range: " + j);
        }
    }

    @Test
    void jitterLeavesFixedAndDisabledDelaysUntouched() {
        RetryPolicy fixed = RetryPolicy.fixedDelay(3, Duration.ofMillis(25));
        RetryPolicy none = RetryPolicy.none();
        for (int i = 0; i < 10; i++) {
            assertEquals(Duration.ofMillis(25), fixed.delayBeforeJittered(1));
            assertEquals(Duration.ZERO, none.delayBeforeJittered(0));
        }
    }

    @Test
    void exponentialDelayNeverOverflows() {
        RetryPolicy capped = RetryPolicy.exponentialBackoff(
                63, Duration.ofMillis(8), Duration.ofMillis(100));
        for (int attempt = 0; attempt < 63; attempt++) {
            long ms = capped.delayBefore(attempt).toMillis();
            assertTrue(ms > 0 && ms <= 100, "capped delay out of range: " + ms);
        }
        RetryPolicy uncapped = RetryPolicy.exponentialBackoff(63, Duration.ofSeconds(1));
        assertTrue(uncapped.delayBefore(62).toMillis() > 0, "uncapped delay must stay positive");
    }

    @Test
    void rejectsMaxDelaySmallerThanInitialDelay() {
        assertThrows(IllegalArgumentException.class, () -> RetryPolicy.exponentialBackoff(
                3, Duration.ofSeconds(2), Duration.ofSeconds(1)));
    }
}

class TaskResultTest {

    @Test
    void successHasNoError() {
        TaskResult r = new TaskResult("a", "out", null, 1, Duration.ofMillis(5), TaskStatus.COMPLETED);
        assertTrue(r.isSuccess());
        assertEquals("out", r.output());
        assertNull(r.error());
    }

    @Test
    void failureCarriesError() {
        Exception err = new IllegalStateException("boom");
        TaskResult r = new TaskResult("a", null, err, 3, Duration.ofMillis(5), TaskStatus.FAILED);
        assertFalse(r.isSuccess());
        assertEquals(3, r.attempts());
    }

    @Test
    void skippedIsNotSuccess() {
        TaskResult r = new TaskResult("a", null, null, 0, Duration.ZERO, TaskStatus.SKIPPED);
        assertFalse(r.isSuccess());
        assertEquals(0, r.attempts());
    }
}
