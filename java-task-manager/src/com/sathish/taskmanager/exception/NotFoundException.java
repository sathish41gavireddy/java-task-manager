package com.sathish.taskmanager.exception;

/** Thrown when a task id does not exist (HTTP 404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
