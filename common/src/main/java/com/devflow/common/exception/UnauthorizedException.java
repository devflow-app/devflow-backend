package com.devflow.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends DevFlowException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public UnauthorizedException() {
        super("Authentication required", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
