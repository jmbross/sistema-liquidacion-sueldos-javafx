package com.jmbross.payroll.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Receipt(
        Long id,
        long workerId,
        LocalDate period,
        BigDecimal grossAmount,
        BigDecimal deductionsAmount,
        BigDecimal netAmount,
        List<ReceiptLine> lines) {
    public Receipt {
        lines = List.copyOf(lines);
    }
}
