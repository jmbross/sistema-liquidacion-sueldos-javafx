package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Receipt;
import java.util.List;

public interface ReceiptRepository {
    Receipt save(Receipt receipt);

    List<Receipt> findAll();

    List<Receipt> findByWorker(long workerId);
}
