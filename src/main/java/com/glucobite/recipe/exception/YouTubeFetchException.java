package com.glucobite.recipe.exception;

public class YouTubeFetchException extends RuntimeException {

    public YouTubeFetchException(String message) {
        super(message);
    }

    public YouTubeFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
