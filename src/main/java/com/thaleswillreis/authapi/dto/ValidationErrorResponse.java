package com.thaleswillreis.authapi.dto;

import java.util.Map;

public class ValidationErrorResponse {

    private final String message;
    private final Map<String, String> fieldErrors;

    public ValidationErrorResponse(String message, Map<String, String> fieldErrors) {
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

}