package com.jmbross.payroll.service;

public final class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
