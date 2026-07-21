package com.jmbross.payroll.service;

import com.jmbross.payroll.domain.Rate;
import com.jmbross.payroll.domain.Receipt;
import com.jmbross.payroll.domain.Role;
import com.jmbross.payroll.domain.Session;
import com.jmbross.payroll.domain.User;
import com.jmbross.payroll.domain.Worker;
import com.jmbross.payroll.repository.RateRepository;
import com.jmbross.payroll.repository.ReceiptRepository;
import com.jmbross.payroll.repository.UserRepository;
import com.jmbross.payroll.repository.WorkerRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public final class PayrollApplicationService {
    private final UserRepository users;
    private final WorkerRepository workers;
    private final RateRepository rates;
    private final ReceiptRepository receipts;
    private final PasswordService passwords;
    private final AuthenticationService authentication;
    private final AuthorizationService authorization;
    private final PayrollCalculator calculator;
    private final ReceiptPdfService pdfs;

    public PayrollApplicationService(
            UserRepository users,
            WorkerRepository workers,
            RateRepository rates,
            ReceiptRepository receipts,
            PasswordService passwords) {
        this.users = users;
        this.workers = workers;
        this.rates = rates;
        this.receipts = receipts;
        this.passwords = passwords;
        this.authentication = new AuthenticationService(users, passwords);
        this.authorization = new AuthorizationService();
        this.calculator = new PayrollCalculator();
        this.pdfs = new ReceiptPdfService();
    }

    public java.util.Optional<Session> authenticate(String email, String password) {
        return authentication.authenticate(email, password);
    }

    public List<User> listUsers(Session session) {
        authorization.requireAdmin(session);
        return users.findAll();
    }

    public User createUser(
            Session session,
            String firstName,
            String lastName,
            String documentId,
            String registrationId,
            String email,
            String rawPassword,
            Role role) {
        authorization.requireAdmin(session);
        InputValidator.password(rawPassword);
        User user = new User(
                null,
                InputValidator.required(firstName, "First name", 80),
                InputValidator.required(lastName, "Last name", 80),
                InputValidator.required(documentId, "Document", 32),
                optional(registrationId, 40),
                InputValidator.email(email),
                passwords.hash(rawPassword),
                role,
                true);
        return users.save(user);
    }

    public User updateUser(
            Session session,
            User selected,
            String firstName,
            String lastName,
            String documentId,
            String registrationId,
            String email,
            String optionalNewPassword,
            Role role,
            boolean active) {
        authorization.requireAdmin(session);
        User current =
                users.findById(selected.id()).orElseThrow(() -> new ValidationException("User no longer exists"));
        String passwordHash = current.passwordHash();
        if (optionalNewPassword != null && !optionalNewPassword.isBlank()) {
            passwordHash = passwords.hash(optionalNewPassword);
        }
        return users.update(new User(
                current.id(),
                InputValidator.required(firstName, "First name", 80),
                InputValidator.required(lastName, "Last name", 80),
                InputValidator.required(documentId, "Document", 32),
                optional(registrationId, 40),
                InputValidator.email(email),
                passwordHash,
                role,
                active));
    }

    public void deleteUser(Session session, long id) {
        authorization.requireAdmin(session);
        if (session.userId() == id) {
            throw new ValidationException("The current signed-in user cannot be deleted");
        }
        users.delete(id);
    }

    public List<Worker> listWorkers(Session session) {
        requireSession(session);
        return session.isAdmin() ? workers.findAll() : workers.findByUser(session.userId());
    }

    public Worker createWorker(
            Session session,
            String firstName,
            String lastName,
            String documentId,
            String email,
            String phone,
            String grossSalary) {
        authorization.requireAdmin(session);
        Worker worker = new Worker(
                null,
                InputValidator.required(firstName, "First name", 80),
                InputValidator.required(lastName, "Last name", 80),
                InputValidator.required(documentId, "Document", 32),
                optionalEmail(email),
                optional(phone, 40),
                InputValidator.money(grossSalary),
                true);
        return workers.save(worker);
    }

    public Worker updateWorker(
            Session session,
            Worker current,
            String firstName,
            String lastName,
            String documentId,
            String email,
            String phone,
            String grossSalary,
            boolean active) {
        authorization.requireAdmin(session);
        return workers.update(new Worker(
                current.id(),
                InputValidator.required(firstName, "First name", 80),
                InputValidator.required(lastName, "Last name", 80),
                InputValidator.required(documentId, "Document", 32),
                optionalEmail(email),
                optional(phone, 40),
                InputValidator.money(grossSalary),
                active));
    }

    public void deleteWorker(Session session, long id) {
        authorization.requireAdmin(session);
        workers.delete(id);
    }

    public void assignWorker(Session session, long userId, long workerId) {
        authorization.requireAdmin(session);
        users.findById(userId).orElseThrow(() -> new ValidationException("User not found"));
        workers.findById(workerId).orElseThrow(() -> new ValidationException("Worker not found"));
        workers.assignToUser(userId, workerId);
    }

    public void unassignWorker(Session session, long userId, long workerId) {
        authorization.requireAdmin(session);
        workers.unassignFromUser(userId, workerId);
    }

    public List<Rate> listRates(Session session) {
        requireSession(session);
        if (session.isAdmin()) {
            return rates.findAll();
        }
        HashSet<Long> permitted = new HashSet<>(
                workers.findByUser(session.userId()).stream().map(Worker::id).toList());
        return rates.findAll().stream()
                .filter(rate -> permitted.contains(rate.workerId()))
                .toList();
    }

    public Rate createRate(Session session, long workerId, String description, String percentage) {
        authorization.requireAdmin(session);
        workers.findById(workerId).orElseThrow(() -> new ValidationException("Worker not found"));
        return rates.save(new Rate(
                null,
                workerId,
                InputValidator.required(description, "Description", 120),
                InputValidator.percentage(percentage),
                true));
    }

    public Rate updateRate(
            Session session, Rate current, long workerId, String description, String percentage, boolean enabled) {
        authorization.requireAdmin(session);
        return rates.update(new Rate(
                current.id(),
                workerId,
                InputValidator.required(description, "Description", 120),
                InputValidator.percentage(percentage),
                enabled));
    }

    public void deleteRate(Session session, long id) {
        authorization.requireAdmin(session);
        rates.delete(id);
    }

    public ReceiptResult generateReceipt(Session session, long workerId, LocalDate period, Path outputDirectory) {
        requireSession(session);
        Worker worker = accessibleWorker(session, workerId);
        if (period == null) {
            throw new ValidationException("Period is required");
        }
        PayrollCalculator.Calculation calculation =
                calculator.calculate(worker.grossSalary(), rates.findByWorker(workerId));
        Receipt receipt = receipts.save(new Receipt(
                null,
                workerId,
                period.withDayOfMonth(1),
                calculation.gross(),
                calculation.deductions(),
                calculation.net(),
                calculation.lines()));
        Path output = outputDirectory.resolve("receipt-" + worker.documentId() + "-" + receipt.period() + ".pdf");
        pdfs.generate(receipt, worker, output);
        return new ReceiptResult(receipt, output);
    }

    public List<Receipt> listReceipts(Session session) {
        requireSession(session);
        if (session.isAdmin()) {
            return receipts.findAll();
        }
        return workers.findByUser(session.userId()).stream()
                .flatMap(worker -> receipts.findByWorker(worker.id()).stream())
                .toList();
    }

    private Worker accessibleWorker(Session session, long workerId) {
        Worker worker = workers.findById(workerId).orElseThrow(() -> new ValidationException("Worker not found"));
        if (!session.isAdmin()
                && workers.findByUser(session.userId()).stream().noneMatch(candidate -> candidate.id() == workerId)) {
            throw new SecurityException("Worker is not assigned to the current user");
        }
        return worker;
    }

    private static void requireSession(Session session) {
        if (session == null) {
            throw new SecurityException("Authentication required");
        }
    }

    private static String optional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return InputValidator.required(value, "Value", maxLength);
    }

    private static String optionalEmail(String value) {
        return value == null || value.isBlank() ? null : InputValidator.email(value);
    }

    public record ReceiptResult(Receipt receipt, Path pdf) {}
}
