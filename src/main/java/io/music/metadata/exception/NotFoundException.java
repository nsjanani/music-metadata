package io.music.metadata.exception;

/**
 * A requested resource doesn't exist. Returned as {@code 404 Not Found}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super(resource + " " + id + " doesn't exist.");
    }

}
