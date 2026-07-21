package com.jmbross.payroll.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private InputValidator() {}

    public static String required(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new ValidationException(field + " is required");
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    public static String email(String value) {
        String normalized = required(value, "Email", 190).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new ValidationException("Email format is invalid");
        }
        return normalized;
    }

    public static BigDecimal money(String value) {
        try {
            BigDecimal amount = new BigDecimal(required(value, "Amount", 30));
            if (amount.signum() < 0) {
                throw new ValidationException("Amount cannot be negative");
            }
            return amount.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Amount must be numeric");
        }
    }

    public static BigDecimal percentage(String value) {
        try {
            BigDecimal percentage = new BigDecimal(required(value, "Percentage", 12));
            if (percentage.signum() < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
                throw new ValidationException("Percentage must be between 0 and 100");
            }
            return percentage.setScale(4, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Percentage must be numeric");
        }
    }

    public static void password(String value) {
        if (value == null || value.length() < 12 || value.length() > 72) {
            throw new ValidationException("Password must contain between 12 and 72 characters");
        }
        if (!value.matches(".*[A-Z].*") || !value.matches(".*[a-z].*") || !value.matches(".*\\d.*")) {
            throw new ValidationException("Password must include upper-case, lower-case, and numeric characters");
        }
    }
}
