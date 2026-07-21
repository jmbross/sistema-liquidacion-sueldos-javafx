package com.jmbross.payroll.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Rate(Long id, long workerId, String description, BigDecimal percentage, boolean enabled) {
    public Rate {
        Objects.requireNonNull(description);
        Objects.requireNonNull(percentage);
    }
}
