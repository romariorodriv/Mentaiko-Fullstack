package com.mentaiko.backend.exception;

public class UserInactiveException extends RuntimeException {

    public UserInactiveException(String message) {
        super(message);
    }
}
