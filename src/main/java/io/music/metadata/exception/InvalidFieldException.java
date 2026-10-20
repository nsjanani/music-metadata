package io.music.metadata.exception;

/**
 * A field passed schema validation but breaks a business rule (for example a name that's blank after trimming).
 * Returned as {@code 400 Bad Request}, with the field listed in {@code errors}.
 */
public class InvalidFieldException extends RuntimeException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }

}
