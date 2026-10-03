package com.hassanhibbert.coffee_tracker.exception;

/** A request that is well-formed but breaks a coffee rule. Mapped to 422 Unprocessable Content. */
public class BrewRuleViolationException extends RuntimeException {

    public BrewRuleViolationException(String message) {
        super(message);
    }
}
