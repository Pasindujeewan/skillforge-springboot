package com.skillforge.authservice.exception;

public class UserServiceCircuitBreakerOpenException extends RuntimeException {

    public UserServiceCircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
