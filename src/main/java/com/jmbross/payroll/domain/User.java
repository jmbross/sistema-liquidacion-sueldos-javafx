package com.jmbross.payroll.domain;

import java.util.Objects;

public record User(
        Long id,
        String firstName,
        String lastName,
        String documentId,
        String registrationId,
        String email,
        String passwordHash,
        Role role,
        boolean active) {
    public User {
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
        Objects.requireNonNull(documentId);
        Objects.requireNonNull(email);
        Objects.requireNonNull(passwordHash);
        Objects.requireNonNull(role);
    }

    public String displayName() {
        return firstName + " " + lastName;
    }
}
