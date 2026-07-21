package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmbross.payroll.service.PasswordService;
import org.junit.jupiter.api.Test;

class PasswordServiceTest {
    private final PasswordService passwords = new PasswordService(10);

    @Test
    void hashesAndVerifiesWithoutStoringPlaintext() {
        String hash = passwords.hash("StrongDemo123");
        assertNotEquals("StrongDemo123", hash);
        assertTrue(hash.startsWith("$2a$10$"));
        assertTrue(passwords.matches("StrongDemo123", hash));
        assertFalse(passwords.matches("WrongDemo123", hash));
    }

    @Test
    void malformedHashFailsClosed() {
        assertFalse(passwords.matches("StrongDemo123", "not-a-hash"));
    }
}
