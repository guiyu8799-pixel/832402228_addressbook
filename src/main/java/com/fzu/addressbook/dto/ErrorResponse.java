package com.fzu.addressbook.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Uniform error body returned by {@code GlobalExceptionHandler}.
 */
public class ErrorResponse {

    private String code;
    private String message;
    private List<String> errors = new ArrayList<>();

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String code, String message, List<String> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
