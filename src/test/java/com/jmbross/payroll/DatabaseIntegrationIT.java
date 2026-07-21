package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmbross.payroll.config.DatabaseManager;
import com.jmbross.payroll.config.DatabaseSettings;
import com.jmbross.payroll.domain.Rate;
import com.jmbross.payroll.domain.Role;
import com.jmbross.payroll.domain.Session;
import com.jmbross.payroll.domain.User;
import com.jmbross.payroll.domain.Worker;
import com.jmbross.payroll.repository.JdbcRateRepository;
import com.jmbross.payroll.repository.JdbcReceiptRepository;
import com.jmbross.payroll.repository.JdbcUserRepository;
import com.jmbross.payroll.repository.JdbcWorkerRepository;
import com.jmbross.payroll.service.PasswordService;
import com.jmbross.payroll.service.PayrollApplicationService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DatabaseIntegrationIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("payroll_test")
            .withUsername("payroll_test")
            .withPassword("local_test_only");

    @TempDir
    Path output;

    @Test
    void migratesAuthenticatesAndPersistsTheCompletePayrollFlow() throws Exception {
        DatabaseSettings settings = new DatabaseSettings(
                MYSQL.getHost(),
                MYSQL.getMappedPort(3306),
                MYSQL.getDatabaseName(),
                MYSQL.getUsername(),
                MYSQL.getPassword());
        try (DatabaseManager database = new DatabaseManager(settings)) {
            database.migrate();
            var users = new JdbcUserRepository(database.dataSource());
            var workers = new JdbcWorkerRepository(database.dataSource());
            var rates = new JdbcRateRepository(database.dataSource());
            var receipts = new JdbcReceiptRepository(database.dataSource());
            var application = new PayrollApplicationService(users, workers, rates, receipts, new PasswordService(10));

            Session seededAdmin = application
                    .authenticate("admin@demo.local", "DemoAdmin!2026")
                    .orElseThrow();
            assertEquals(Role.ADMIN, seededAdmin.role());
            assertTrue(application
                    .authenticate("admin@demo.local", "wrong-password")
                    .isEmpty());

            User user = application.createUser(
                    seededAdmin,
                    "Integration",
                    "User",
                    "DEMO-IT-USER",
                    "DEMO-IT-MAT",
                    "integration@demo.local",
                    "Integration123",
                    Role.USER);
            Worker worker = application.createWorker(
                    seededAdmin,
                    "Integration",
                    "Worker",
                    "DEMO-IT-WORKER",
                    "worker-it@demo.local",
                    "000-000-0099",
                    "100000.00");
            application.assignWorker(seededAdmin, user.id(), worker.id());
            Rate rate = application.createRate(seededAdmin, worker.id(), "Integration deduction", "12.5000");

            Session regular = application
                    .authenticate("integration@demo.local", "Integration123")
                    .orElseThrow();
            assertEquals(1, application.listWorkers(regular).size());
            assertEquals(rate.id(), application.listRates(regular).getFirst().id());

            var result = application.generateReceipt(regular, worker.id(), LocalDate.of(2026, 8, 17), output);
            assertEquals(new BigDecimal("87500.00"), result.receipt().netAmount());
            assertTrue(java.nio.file.Files.exists(result.pdf()));
            assertEquals(1, receipts.findByWorker(worker.id()).size());

            try (var connection = database.dataSource().getConnection();
                    var statement = connection.prepareStatement(
                            "SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE");
                    var rows = statement.executeQuery()) {
                rows.next();
                assertTrue(rows.getInt(1) >= 2);
            }
        }
    }
}
