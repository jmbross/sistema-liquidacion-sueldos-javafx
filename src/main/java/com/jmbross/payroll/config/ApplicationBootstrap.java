package com.jmbross.payroll.config;

import com.jmbross.payroll.repository.JdbcRateRepository;
import com.jmbross.payroll.repository.JdbcReceiptRepository;
import com.jmbross.payroll.repository.JdbcUserRepository;
import com.jmbross.payroll.repository.JdbcWorkerRepository;
import com.jmbross.payroll.service.PasswordService;
import com.jmbross.payroll.service.PayrollApplicationService;

public final class ApplicationBootstrap implements AutoCloseable {
    private final DatabaseManager database;
    private final PayrollApplicationService applicationService;

    public ApplicationBootstrap(DatabaseSettings settings) {
        database = new DatabaseManager(settings);
        database.migrate();
        var users = new JdbcUserRepository(database.dataSource());
        var workers = new JdbcWorkerRepository(database.dataSource());
        var rates = new JdbcRateRepository(database.dataSource());
        var receipts = new JdbcReceiptRepository(database.dataSource());
        applicationService = new PayrollApplicationService(users, workers, rates, receipts, new PasswordService(12));
    }

    public PayrollApplicationService applicationService() {
        return applicationService;
    }

    @Override
    public void close() {
        database.close();
    }
}
