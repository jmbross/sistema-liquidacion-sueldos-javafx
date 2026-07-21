package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jmbross.payroll.domain.Rate;
import com.jmbross.payroll.service.PayrollCalculator;
import com.jmbross.payroll.service.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayrollCalculatorTest {
    private final PayrollCalculator calculator = new PayrollCalculator();

    @Test
    void calculatesNetSalaryAndRateDeductionsWithBigDecimal() {
        var rates = List.of(
                new Rate(1L, 7, "Retirement", new BigDecimal("11.0000"), true),
                new Rate(2L, 7, "Health", new BigDecimal("3.0000"), true));

        var result = calculator.calculate(new BigDecimal("100000.00"), rates);

        assertEquals(new BigDecimal("14000.00"), result.deductions());
        assertEquals(new BigDecimal("86000.00"), result.net());
        assertEquals(2, result.lines().size());
    }

    @Test
    void roundsEachMonetaryLineHalfUp() {
        var rate = new Rate(1L, 7, "Fractional", new BigDecimal("1.0050"), true);
        var result = calculator.calculate(new BigDecimal("100.00"), List.of(rate));
        assertEquals(new BigDecimal("1.01"), result.deductions());
        assertEquals(new BigDecimal("98.99"), result.net());
    }

    @Test
    void ignoresDisabledRates() {
        var rate = new Rate(1L, 7, "Disabled", new BigDecimal("50"), false);
        assertEquals(
                new BigDecimal("100.00"),
                calculator.calculate(new BigDecimal("100"), List.of(rate)).net());
    }

    @Test
    void rejectsNegativeSalary() {
        assertThrows(ValidationException.class, () -> calculator.calculate(new BigDecimal("-1"), List.of()));
    }
}
