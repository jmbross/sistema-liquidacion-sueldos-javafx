package com.jmbross.payroll.service;

import com.jmbross.payroll.domain.Session;
import com.jmbross.payroll.domain.User;
import com.jmbross.payroll.repository.UserRepository;
import java.util.Optional;

public final class AuthenticationService {
    private final UserRepository users;
    private final PasswordService passwords;

    public AuthenticationService(UserRepository users, PasswordService passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public Optional<Session> authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return users.findByEmail(email.trim())
                .filter(User::active)
                .filter(user -> passwords.matches(password, user.passwordHash()))
                .map(user -> new Session(user.id(), user.email(), user.displayName(), user.role()));
    }
}
