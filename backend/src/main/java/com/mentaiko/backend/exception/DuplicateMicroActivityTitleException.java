package com.mentaiko.backend.exception;

public class DuplicateMicroActivityTitleException extends RuntimeException {

    public DuplicateMicroActivityTitleException(String message) {
        super(message);
    }
}
