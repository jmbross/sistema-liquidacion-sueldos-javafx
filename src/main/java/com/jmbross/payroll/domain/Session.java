package com.jmbross.payroll.domain;

public record Session(long userId, String email, String displayName, Role role) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
