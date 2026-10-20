package io.music.metadata.exception;

/**
 * The request conflicts with the current state of a resource, for example a stale {@code version}.
 * Returned as {@code 409 Conflict}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
