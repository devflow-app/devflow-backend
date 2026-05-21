package com.devflow.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DevFlowException {

    public ResourceNotFoundException(String resource, String id) {
        super(String.format("%s not found with id: %s", resource, id),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
