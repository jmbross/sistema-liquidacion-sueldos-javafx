package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jmbross.payroll.domain.Role;
import com.jmbross.payroll.domain.User;
import com.jmbross.payroll.repository.UserRepository;
import com.jmbross.payroll.service.AuthenticationService;
import com.jmbross.payroll.service.PasswordService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock
    UserRepository users;

    @Test
    void loadsByIdentifierThenChecksBcryptInJava() {
        PasswordService passwords = new PasswordService(10);
        String hash = passwords.hash("StrongDemo123");
        when(users.findByEmail("admin@demo.local"))
                .thenReturn(Optional.of(
                        new User(1L, "Admin", "Demo", "DEMO", null, "admin@demo.local", hash, Role.ADMIN, true)));
        var authentication = new AuthenticationService(users, passwords);

        var session =
                authentication.authenticate("admin@demo.local", "StrongDemo123").orElseThrow();

        assertEquals(Role.ADMIN, session.role());
        assertTrue(
                authentication.authenticate("admin@demo.local", "WrongDemo123").isEmpty());
    }

    @Test
    void inactiveUserCannotAuthenticate() {
        PasswordService passwords = new PasswordService(10);
        when(users.findByEmail("user@demo.local"))
                .thenReturn(Optional.of(new User(
                        2L,
                        "User",
                        "Demo",
                        "DEMO2",
                        null,
                        "user@demo.local",
                        passwords.hash("StrongDemo123"),
                        Role.USER,
                        false)));
        assertTrue(new AuthenticationService(users, passwords)
                .authenticate("user@demo.local", "StrongDemo123")
                .isEmpty());
    }
}
