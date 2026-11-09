package com.sathish.taskmanager.exception;

/** Thrown when the user sends invalid data (HTTP 400). */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
