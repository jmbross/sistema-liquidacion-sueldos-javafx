package com.jmbross.payroll.domain;

import java.math.BigDecimal;

public record ReceiptLine(String description, BigDecimal percentage, BigDecimal amount) {}
