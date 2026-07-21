package com.jmbross.payroll.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Worker(
        Long id,
        String firstName,
        String lastName,
        String documentId,
        String email,
        String phone,
        BigDecimal grossSalary,
        boolean active) {
    public Worker {
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
        Objects.requireNonNull(documentId);
        Objects.requireNonNull(grossSalary);
    }

    public String displayName() {
        return firstName + " " + lastName;
    }
}
