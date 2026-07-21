package com.jmbross.payroll.ui;

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
import com.jmbross.payroll.service.PasswordService;
import com.jmbross.payroll.service.PayrollApplicationService;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class ScreenshotExporter extends Application {
    private final Path output = Path.of("docs", "screenshots");

    @Override
    public void start(Stage stage) throws Exception {
        Files.createDirectories(output);
        DemoData demo = new DemoData();
        PayrollApplicationService service = demo.service();
        AppView view = new AppView(service);

        Scene login = view.loginScene(ignored -> {});
        capture(stage, login, output.resolve("login.png"));

        Session admin = new Session(1, "admin@demo.local", "Admin Demo", Role.ADMIN);
        service.generateReceipt(admin, 1, LocalDate.of(2026, 7, 1), output);
        AppView.DashboardView dashboard = view.dashboard(admin, () -> {});
        Scene scene = dashboard.scene();
        String[] names = {"dashboard", "users", "workers", "rates", "receipts"};
        for (int index = 0; index < names.length; index++) {
            dashboard.tabs().getSelectionModel().select(index);
            capture(stage, scene, output.resolve(names[index] + ".png"));
        }
        Path pdf = Files.list(output)
                .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                .findFirst()
                .orElseThrow();
        renderPdf(pdf, output.resolve("receipt-pdf.png"));
        stage.close();
    }

    private static void capture(Stage stage, Scene scene, Path file) throws Exception {
        stage.setScene(scene);
        stage.show();
        Parent root = scene.getRoot();
        root.applyCss();
        root.layout();
        WritableImage image = root.snapshot(null, null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
    }

    private static void renderPdf(Path pdf, Path png) throws Exception {
        try (var document = Loader.loadPDF(pdf.toFile())) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 120, ImageType.RGB);
            ImageIO.write(image, "png", png.toFile());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final class DemoData {
        private final MemoryUsers users = new MemoryUsers();
        private final MemoryWorkers workers = new MemoryWorkers();
        private final MemoryRates rates = new MemoryRates();
        private final MemoryReceipts receipts = new MemoryReceipts();

        private DemoData() {
            PasswordService passwordService = new PasswordService(10);
            users.rows.add(new User(
                    1L,
                    "Admin",
                    "Demo",
                    "DEMO-ADMIN-001",
                    "DEMO-MAT-001",
                    "admin@demo.local",
                    passwordService.hash("DemoAdmin!2026"),
                    Role.ADMIN,
                    true));
            users.rows.add(new User(
                    2L,
                    "Usuario",
                    "Demo",
                    "DEMO-USER-001",
                    "DEMO-MAT-002",
                    "usuario@demo.local",
                    passwordService.hash("DemoUser!2026"),
                    Role.USER,
                    true));
            workers.rows.add(new Worker(
                    1L,
                    "Empleado",
                    "Ejemplo Uno",
                    "DEMO-WORKER-001",
                    "empleado@demo.local",
                    "000-000-0001",
                    new BigDecimal("1250000.00"),
                    true));
            workers.rows.add(new Worker(
                    2L,
                    "Persona",
                    "Ejemplo Dos",
                    "DEMO-WORKER-002",
                    "persona@demo.local",
                    "000-000-0002",
                    new BigDecimal("980000.00"),
                    true));
            workers.assigned.add(1L);
            rates.rows.add(new Rate(1L, 1, "Retirement demo", new BigDecimal("11.0000"), true));
            rates.rows.add(new Rate(2L, 1, "Health insurance demo", new BigDecimal("3.0000"), true));
            rates.rows.add(new Rate(3L, 2, "Retirement demo", new BigDecimal("11.0000"), true));
            rates.rows.add(new Rate(4L, 2, "Health insurance demo", new BigDecimal("3.0000"), true));
        }

        private PayrollApplicationService service() {
            return new PayrollApplicationService(users, workers, rates, receipts, new PasswordService(10));
        }
    }

    private static final class MemoryUsers implements UserRepository {
        private final List<User> rows = new ArrayList<>();

        public Optional<User> findByEmail(String email) {
            return rows.stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
        }

        public Optional<User> findById(long id) {
            return rows.stream().filter(u -> u.id() == id).findFirst();
        }

        public List<User> findAll() {
            return List.copyOf(rows);
        }

        public User save(User user) {
            User saved = new User(
                    (long) rows.size() + 1,
                    user.firstName(),
                    user.lastName(),
                    user.documentId(),
                    user.registrationId(),
                    user.email(),
                    user.passwordHash(),
                    user.role(),
                    user.active());
            rows.add(saved);
            return saved;
        }

        public User update(User user) {
            rows.removeIf(u -> u.id().equals(user.id()));
            rows.add(user);
            return user;
        }

        public void delete(long id) {
            rows.removeIf(u -> u.id() == id);
        }
    }

    private static final class MemoryWorkers implements WorkerRepository {
        private final List<Worker> rows = new ArrayList<>();
        private final List<Long> assigned = new ArrayList<>();

        public Optional<Worker> findById(long id) {
            return rows.stream().filter(w -> w.id() == id).findFirst();
        }

        public List<Worker> findAll() {
            return List.copyOf(rows);
        }

        public List<Worker> findByUser(long userId) {
            return rows.stream().filter(w -> assigned.contains(w.id())).toList();
        }

        public Worker save(Worker worker) {
            Worker saved = new Worker(
                    (long) rows.size() + 1,
                    worker.firstName(),
                    worker.lastName(),
                    worker.documentId(),
                    worker.email(),
                    worker.phone(),
                    worker.grossSalary(),
                    worker.active());
            rows.add(saved);
            return saved;
        }

        public Worker update(Worker worker) {
            rows.removeIf(w -> w.id().equals(worker.id()));
            rows.add(worker);
            return worker;
        }

        public void delete(long id) {
            rows.removeIf(w -> w.id() == id);
        }

        public void assignToUser(long userId, long workerId) {
            if (!assigned.contains(workerId)) assigned.add(workerId);
        }

        public void unassignFromUser(long userId, long workerId) {
            assigned.remove(workerId);
        }
    }

    private static final class MemoryRates implements RateRepository {
        private final List<Rate> rows = new ArrayList<>();

        public List<Rate> findAll() {
            return List.copyOf(rows);
        }

        public List<Rate> findByWorker(long workerId) {
            return rows.stream()
                    .filter(r -> r.workerId() == workerId && r.enabled())
                    .toList();
        }

        public Rate save(Rate rate) {
            Rate saved = new Rate(
                    (long) rows.size() + 1, rate.workerId(), rate.description(), rate.percentage(), rate.enabled());
            rows.add(saved);
            return saved;
        }

        public Rate update(Rate rate) {
            rows.removeIf(r -> r.id().equals(rate.id()));
            rows.add(rate);
            return rate;
        }

        public void delete(long id) {
            rows.removeIf(r -> r.id() == id);
        }
    }

    private static final class MemoryReceipts implements ReceiptRepository {
        private final List<Receipt> rows = new ArrayList<>();
        private final AtomicLong sequence = new AtomicLong();

        public Receipt save(Receipt receipt) {
            Receipt saved = new Receipt(
                    sequence.incrementAndGet(),
                    receipt.workerId(),
                    receipt.period(),
                    receipt.grossAmount(),
                    receipt.deductionsAmount(),
                    receipt.netAmount(),
                    receipt.lines());
            rows.add(saved);
            return saved;
        }

        public List<Receipt> findAll() {
            return List.copyOf(rows);
        }

        public List<Receipt> findByWorker(long workerId) {
            return rows.stream().filter(r -> r.workerId() == workerId).toList();
        }
    }
}
