package com.skillforge.authservice.exception;

public class ProfileConflictException extends RuntimeException {
    public ProfileConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
