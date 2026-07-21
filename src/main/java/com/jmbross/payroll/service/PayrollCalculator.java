package com.jmbross.payroll.service;

import com.jmbross.payroll.domain.Rate;
import com.jmbross.payroll.domain.ReceiptLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PayrollCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public Calculation calculate(BigDecimal grossSalary, List<Rate> rates) {
        if (grossSalary == null || grossSalary.signum() < 0) {
            throw new ValidationException("Gross salary cannot be negative");
        }
        BigDecimal gross = grossSalary.setScale(2, RoundingMode.HALF_UP);
        List<ReceiptLine> lines = rates.stream()
                .filter(Rate::enabled)
                .map(rate -> new ReceiptLine(
                        rate.description(),
                        rate.percentage().setScale(4, RoundingMode.HALF_UP),
                        gross.multiply(rate.percentage()).divide(HUNDRED, 2, RoundingMode.HALF_UP)))
                .toList();
        BigDecimal deductions = lines.stream()
                .map(ReceiptLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new Calculation(gross, deductions, net, lines);
    }

    public record Calculation(BigDecimal gross, BigDecimal deductions, BigDecimal net, List<ReceiptLine> lines) {
        public Calculation {
            lines = List.copyOf(lines);
        }
    }
}
