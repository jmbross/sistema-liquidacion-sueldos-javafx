package com.jmbross.payroll.service;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordService {
    private final int cost;

    public PasswordService(int cost) {
        if (cost < 10 || cost > 14) {
            throw new IllegalArgumentException("BCrypt cost must be between 10 and 14");
        }
        this.cost = cost;
    }

    public String hash(String password) {
        InputValidator.password(password);
        return BCrypt.hashpw(password, BCrypt.gensalt(cost));
    }

    public boolean matches(String password, String hash) {
        if (password == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
