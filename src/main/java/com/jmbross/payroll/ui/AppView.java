package com.jmbross.payroll.ui;

import com.jmbross.payroll.domain.Rate;
import com.jmbross.payroll.domain.Receipt;
import com.jmbross.payroll.domain.Role;
import com.jmbross.payroll.domain.Session;
import com.jmbross.payroll.domain.User;
import com.jmbross.payroll.domain.Worker;
import com.jmbross.payroll.service.PayrollApplicationService;
import com.jmbross.payroll.service.ValidationException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppView {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppView.class);
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
    private final PayrollApplicationService service;

    public AppView(PayrollApplicationService service) {
        this.service = service;
    }

    public Scene loginScene(Consumer<Session> onAuthenticated) {
        Label title = new Label("Payroll modernization case study");
        title.getStyleClass().add("app-title");
        Label description = new Label("Sign in with the fictitious local demo account");
        description.getStyleClass().add("subtitle");
        VBox heading = new VBox(4, title, description);
        heading.getStyleClass().add("top-bar");

        TextField email = new TextField("admin@demo.local");
        email.setPromptText("Email");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label status = new Label();
        Button signIn = new Button("Sign in");
        signIn.setDefaultButton(true);
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setOnAction(event -> {
            status.setText("");
            try {
                service.authenticate(email.getText(), password.getText()).ifPresentOrElse(onAuthenticated, () -> {
                    status.getStyleClass().setAll("status-error");
                    status.setText("Invalid email or password");
                });
            } catch (RuntimeException exception) {
                LOGGER.error("Authentication failed", exception);
                status.getStyleClass().setAll("status-error");
                status.setText("Authentication is temporarily unavailable");
            } finally {
                password.clear();
            }
        });
        GridPane form = formGrid();
        form.addRow(0, new Label("Email"), email);
        form.addRow(1, new Label("Password"), password);
        VBox card = new VBox(12, new Label("Demo access"), form, signIn, status);
        card.getStyleClass().add("card");
        card.setMaxWidth(440);
        StackPane center = new StackPane(card);
        center.setPadding(new Insets(40));
        BorderPane root = new BorderPane(center);
        root.setTop(heading);
        root.getStyleClass().add("app-shell");
        return styled(new Scene(root, 960, 640));
    }

    public DashboardView dashboard(Session session, Runnable onLogout) {
        Label title = new Label("Payroll administration");
        title.getStyleClass().add("app-title");
        Label identity = new Label(session.displayName() + " · " + session.role());
        identity.getStyleClass().add("subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = secondary("Log out");
        logout.setOnAction(event -> onLogout.run());
        HBox top = new HBox(12, new VBox(2, title, identity), spacer, logout);
        top.getStyleClass().add("top-bar");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(overviewTab(session));
        if (session.isAdmin()) {
            tabs.getTabs().add(usersTab(session));
        }
        tabs.getTabs().add(workersTab(session));
        tabs.getTabs().add(ratesTab(session));
        tabs.getTabs().add(receiptsTab(session));

        BorderPane root = new BorderPane(tabs);
        root.setTop(top);
        root.getStyleClass().add("app-shell");
        BorderPane.setMargin(tabs, new Insets(16));
        return new DashboardView(root, tabs);
    }

    private Tab overviewTab(Session session) {
        int workers = service.listWorkers(session).size();
        int rates = service.listRates(session).size();
        int receipts = service.listReceipts(session).size();
        HBox metrics =
                new HBox(18, metric("Workers", workers), metric("Active rates", rates), metric("Receipts", receipts));
        VBox content = new VBox(
                18,
                new Label("Verified application overview"),
                metrics,
                new Label("Java 21 · JavaFX 21 · MySQL 8 · Flyway · BCrypt · PDFBox"),
                new Label("All visible records are fictitious demo data."));
        content.setPadding(new Insets(24));
        return new Tab("Dashboard", content);
    }

    private Tab usersTab(Session session) {
        TableView<User> table = new TableView<>();
        table.getColumns().add(textColumn("Name", User::displayName));
        table.getColumns().add(textColumn("Email", User::email));
        table.getColumns().add(textColumn("Document", User::documentId));
        table.getColumns().add(textColumn("Role", user -> user.role().name()));
        table.getColumns().add(textColumn("Status", user -> user.active() ? "Active" : "Disabled"));

        TextField first = new TextField();
        TextField last = new TextField();
        TextField document = new TextField();
        TextField registration = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.setValue(Role.USER);
        CheckBox active = new CheckBox("Active");
        active.setSelected(true);
        GridPane form = formGrid();
        form.addRow(0, new Label("First name"), first, new Label("Last name"), last);
        form.addRow(1, new Label("Document"), document, new Label("Registration"), registration);
        form.addRow(2, new Label("Email"), email, new Label("Role"), role);
        form.addRow(3, new Label("Password"), password, active);
        Label status = new Label();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(service.listUsers(session)));
        Button create = new Button("Create");
        Button update = secondary("Update");
        Button delete = danger("Delete");
        create.setOnAction(event -> run(status, () -> {
            service.createUser(
                    session,
                    first.getText(),
                    last.getText(),
                    document.getText(),
                    registration.getText(),
                    email.getText(),
                    password.getText(),
                    role.getValue());
            clear(first, last, document, registration, email, password);
            refresh.run();
        }));
        update.setOnAction(event -> run(status, () -> {
            User selected = requireSelection(table, "Select a user to update");
            service.updateUser(
                    session,
                    selected,
                    first.getText(),
                    last.getText(),
                    document.getText(),
                    registration.getText(),
                    email.getText(),
                    password.getText(),
                    role.getValue(),
                    active.isSelected());
            refresh.run();
        }));
        delete.setOnAction(event -> run(status, () -> {
            User selected = requireSelection(table, "Select a user to delete");
            service.deleteUser(session, selected.id());
            refresh.run();
        }));
        table.getSelectionModel().selectedItemProperty().addListener((ignored, oldValue, user) -> {
            if (user != null) {
                first.setText(user.firstName());
                last.setText(user.lastName());
                document.setText(user.documentId());
                registration.setText(user.registrationId());
                email.setText(user.email());
                role.setValue(user.role());
                active.setSelected(user.active());
                password.clear();
            }
        });
        refresh.run();
        return new Tab("Users", section(table, form, new HBox(8, create, update, delete), status));
    }

    private Tab workersTab(Session session) {
        TableView<Worker> table = new TableView<>();
        table.getColumns().add(textColumn("Name", Worker::displayName));
        table.getColumns().add(textColumn("Document", Worker::documentId));
        table.getColumns().add(textColumn("Email", worker -> safe(worker.email())));
        table.getColumns().add(textColumn("Gross salary", worker -> MONEY.format(worker.grossSalary())));
        table.getColumns().add(textColumn("Status", worker -> worker.active() ? "Active" : "Disabled"));
        TextField first = new TextField();
        TextField last = new TextField();
        TextField document = new TextField();
        TextField email = new TextField();
        TextField phone = new TextField();
        TextField salary = new TextField();
        CheckBox active = new CheckBox("Active");
        active.setSelected(true);
        GridPane form = formGrid();
        form.addRow(0, new Label("First name"), first, new Label("Last name"), last);
        form.addRow(1, new Label("Document"), document, new Label("Email"), email);
        form.addRow(2, new Label("Phone"), phone, new Label("Gross salary"), salary);
        form.add(active, 1, 3);
        Label status = new Label();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(service.listWorkers(session)));
        HBox actions = new HBox(8);
        if (session.isAdmin()) {
            Button create = new Button("Create");
            Button update = secondary("Update");
            Button delete = danger("Delete");
            create.setOnAction(event -> run(status, () -> {
                service.createWorker(
                        session,
                        first.getText(),
                        last.getText(),
                        document.getText(),
                        email.getText(),
                        phone.getText(),
                        salary.getText());
                clear(first, last, document, email, phone, salary);
                refresh.run();
            }));
            update.setOnAction(event -> run(status, () -> {
                Worker selected = requireSelection(table, "Select a worker to update");
                service.updateWorker(
                        session,
                        selected,
                        first.getText(),
                        last.getText(),
                        document.getText(),
                        email.getText(),
                        phone.getText(),
                        salary.getText(),
                        active.isSelected());
                refresh.run();
            }));
            delete.setOnAction(event -> run(status, () -> {
                Worker selected = requireSelection(table, "Select a worker to delete");
                service.deleteWorker(session, selected.id());
                refresh.run();
            }));
            ComboBox<User> user = new ComboBox<>(FXCollections.observableArrayList(service.listUsers(session)));
            user.setConverter(converter(User::displayName));
            Button assign = secondary("Assign to user");
            Button unassign = secondary("Unassign");
            assign.setOnAction(event -> run(
                    status,
                    () -> service.assignWorker(
                            session,
                            requireCombo(user, "Select a user").id(),
                            requireSelection(table, "Select a worker").id())));
            unassign.setOnAction(event -> run(
                    status,
                    () -> service.unassignWorker(
                            session,
                            requireCombo(user, "Select a user").id(),
                            requireSelection(table, "Select a worker").id())));
            actions.getChildren().addAll(create, update, delete, new Label("User"), user, assign, unassign);
        } else {
            form.setDisable(true);
            actions.getChildren().add(new Label("Read-only: only workers assigned to your account are shown."));
        }
        table.getSelectionModel().selectedItemProperty().addListener((ignored, oldValue, worker) -> {
            if (worker != null) {
                first.setText(worker.firstName());
                last.setText(worker.lastName());
                document.setText(worker.documentId());
                email.setText(worker.email());
                phone.setText(worker.phone());
                salary.setText(worker.grossSalary().toPlainString());
                active.setSelected(worker.active());
            }
        });
        refresh.run();
        return new Tab("Workers", section(table, form, actions, status));
    }

    private Tab ratesTab(Session session) {
        TableView<Rate> table = new TableView<>();
        var workers = service.listWorkers(session);
        table.getColumns().add(textColumn("Worker", rate -> workers.stream()
                .filter(w -> w.id() == rate.workerId())
                .map(Worker::displayName)
                .findFirst()
                .orElse("#" + rate.workerId())));
        table.getColumns().add(textColumn("Description", Rate::description));
        table.getColumns()
                .add(textColumn("Percentage", rate -> rate.percentage().toPlainString() + "%"));
        table.getColumns().add(textColumn("Status", rate -> rate.enabled() ? "Enabled" : "Disabled"));
        ComboBox<Worker> worker = new ComboBox<>(FXCollections.observableArrayList(workers));
        worker.setConverter(converter(Worker::displayName));
        TextField description = new TextField();
        TextField percentage = new TextField();
        CheckBox enabled = new CheckBox("Enabled");
        enabled.setSelected(true);
        GridPane form = formGrid();
        form.addRow(0, new Label("Worker"), worker, new Label("Description"), description);
        form.addRow(1, new Label("Percentage"), percentage, enabled);
        Label status = new Label();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(service.listRates(session)));
        HBox actions = new HBox(8);
        if (session.isAdmin()) {
            Button create = new Button("Create");
            Button update = secondary("Update");
            Button delete = danger("Delete");
            create.setOnAction(event -> run(status, () -> {
                service.createRate(
                        session,
                        requireCombo(worker, "Select a worker").id(),
                        description.getText(),
                        percentage.getText());
                clear(description, percentage);
                refresh.run();
            }));
            update.setOnAction(event -> run(status, () -> {
                Rate selected = requireSelection(table, "Select a rate to update");
                service.updateRate(
                        session,
                        selected,
                        requireCombo(worker, "Select a worker").id(),
                        description.getText(),
                        percentage.getText(),
                        enabled.isSelected());
                refresh.run();
            }));
            delete.setOnAction(event -> run(status, () -> {
                service.deleteRate(
                        session,
                        requireSelection(table, "Select a rate to delete").id());
                refresh.run();
            }));
            actions.getChildren().addAll(create, update, delete);
        } else {
            form.setDisable(true);
            actions.getChildren().add(new Label("Read-only rates for assigned workers."));
        }
        table.getSelectionModel().selectedItemProperty().addListener((ignored, oldValue, rate) -> {
            if (rate != null) {
                worker.getSelectionModel()
                        .select(workers.stream()
                                .filter(w -> w.id() == rate.workerId())
                                .findFirst()
                                .orElse(null));
                description.setText(rate.description());
                percentage.setText(rate.percentage().toPlainString());
                enabled.setSelected(rate.enabled());
            }
        });
        refresh.run();
        return new Tab("Rates", section(table, form, actions, status));
    }

    private Tab receiptsTab(Session session) {
        TableView<Receipt> table = new TableView<>();
        var workers = service.listWorkers(session);
        table.getColumns().add(textColumn("Worker", receipt -> workers.stream()
                .filter(w -> w.id() == receipt.workerId())
                .map(Worker::displayName)
                .findFirst()
                .orElse("#" + receipt.workerId())));
        table.getColumns().add(textColumn("Period", receipt -> receipt.period().toString()));
        table.getColumns().add(textColumn("Gross", receipt -> MONEY.format(receipt.grossAmount())));
        table.getColumns().add(textColumn("Deductions", receipt -> MONEY.format(receipt.deductionsAmount())));
        table.getColumns().add(textColumn("Net", receipt -> MONEY.format(receipt.netAmount())));
        ComboBox<Worker> worker = new ComboBox<>(FXCollections.observableArrayList(workers));
        worker.setConverter(converter(Worker::displayName));
        DatePicker period = new DatePicker(LocalDate.now().withDayOfMonth(1));
        Label status = new Label();
        Button generate = new Button("Generate and save receipt");
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(service.listReceipts(session)));
        generate.setOnAction(event -> run(status, () -> {
            var result = service.generateReceipt(
                    session, requireCombo(worker, "Select a worker").id(), period.getValue(), Path.of("receipts"));
            status.setText("Saved PDF: " + result.pdf().toAbsolutePath());
            refresh.run();
        }));
        GridPane form = formGrid();
        form.addRow(0, new Label("Worker"), worker, new Label("Period"), period);
        refresh.run();
        return new Tab("Receipts", section(table, form, new HBox(8, generate), status));
    }

    private static VBox section(TableView<?> table, GridPane form, Node actions, Label status) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox formCard = new VBox(10, form, actions, status);
        formCard.getStyleClass().add("card");
        VBox content = new VBox(14, table, formCard);
        content.setPadding(new Insets(12));
        return content;
    }

    private static VBox metric(String label, int value) {
        Label number = new Label(String.valueOf(value));
        number.getStyleClass().add("metric");
        VBox card = new VBox(4, new Label(label), number);
        card.getStyleClass().add("card");
        card.setMinWidth(180);
        return card;
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        return grid;
    }

    private static Button secondary(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary");
        return button;
    }

    private static Button danger(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger");
        return button;
    }

    private static void clear(TextField... fields) {
        for (TextField field : fields) {
            field.clear();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static <T> T requireSelection(TableView<T> table, String message) {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new ValidationException(message);
        }
        return selected;
    }

    private static <T> T requireCombo(ComboBox<T> combo, String message) {
        T selected = combo.getValue();
        if (selected == null) {
            throw new ValidationException(message);
        }
        return selected;
    }

    private static <T> StringConverter<T> converter(java.util.function.Function<T, String> label) {
        return new StringConverter<>() {
            public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }

            public T fromString(String value) {
                return null;
            }
        };
    }

    private static <T> TableColumn<T, String> textColumn(String title, java.util.function.Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(value.apply(cell.getValue()))));
        return column;
    }

    private static Scene styled(Scene scene) {
        String css = AppView.class.getResource("/css/app.css").toExternalForm();
        scene.getStylesheets().add(css);
        return scene;
    }

    private static void run(Label status, Runnable action) {
        status.setText("");
        try {
            action.run();
            status.getStyleClass().setAll("status-ok");
            if (status.getText().isBlank()) {
                status.setText("Operation completed");
            }
        } catch (ValidationException | SecurityException exception) {
            status.getStyleClass().setAll("status-error");
            status.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Application operation failed", exception);
            status.getStyleClass().setAll("status-error");
            status.setText("The operation could not be completed. Check logs without exposing credentials.");
        }
    }

    public record DashboardView(BorderPane root, TabPane tabs) {
        public Scene scene() {
            return styled(new Scene(root, 1200, 760));
        }

        public Parent content() {
            return root;
        }
    }
}
