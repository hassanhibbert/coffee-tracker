package com.hassanhibbert.coffee_tracker.exception;

/** Thrown when deleting something other rows still reference. Mapped to 409 Conflict. */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
