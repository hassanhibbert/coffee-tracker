package com.hassanhibbert.coffee_tracker.exception;

import java.time.Duration;

public class TooManyRequestsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyRequestsException(Duration retryAfter) {
        super("Too many attempts. Try again later.");
        // Round up: telling a client "0 seconds" would invite an immediate retry that fails again.
        this.retryAfterSeconds = Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
