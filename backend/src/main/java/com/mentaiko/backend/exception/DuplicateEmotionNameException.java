package com.mentaiko.backend.exception;

public class DuplicateEmotionNameException extends RuntimeException {

    public DuplicateEmotionNameException(String message) {
        super(message);
    }
}
