package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Rate;
import java.util.List;

public interface RateRepository {
    List<Rate> findAll();

    List<Rate> findByWorker(long workerId);

    Rate save(Rate rate);

    Rate update(Rate rate);

    void delete(long id);
}
