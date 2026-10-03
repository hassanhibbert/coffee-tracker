package com.hassanhibbert.coffee_tracker.exception;

/** A query parameter the API understands syntactically but won't accept. Mapped to 400. */
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
