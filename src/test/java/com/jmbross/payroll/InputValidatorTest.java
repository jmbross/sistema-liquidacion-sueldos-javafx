package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jmbross.payroll.service.InputValidator;
import com.jmbross.payroll.service.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InputValidatorTest {
    @Test
    void normalizesEmailAndMoney() {
        assertEquals("admin@demo.local", InputValidator.email(" Admin@Demo.Local "));
        assertEquals(new BigDecimal("10.01"), InputValidator.money("10.005"));
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(ValidationException.class, () -> InputValidator.email("not-an-email"));
        assertThrows(ValidationException.class, () -> InputValidator.percentage("101"));
        assertThrows(ValidationException.class, () -> InputValidator.password("short"));
    }
}
