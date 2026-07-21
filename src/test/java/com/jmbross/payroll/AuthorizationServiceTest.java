package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jmbross.payroll.domain.Role;
import com.jmbross.payroll.domain.Session;
import com.jmbross.payroll.service.AuthorizationService;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {
    private final AuthorizationService authorization = new AuthorizationService();

    @Test
    void adminIsAuthorized() {
        assertDoesNotThrow(
                () -> authorization.requireAdmin(new Session(1, "admin@demo.local", "Admin Demo", Role.ADMIN)));
    }

    @Test
    void regularUserIsRejected() {
        assertThrows(
                SecurityException.class,
                () -> authorization.requireAdmin(new Session(2, "user@demo.local", "User Demo", Role.USER)));
    }
}
