package com.jmbross.payroll;

import com.jmbross.payroll.config.ApplicationBootstrap;
import com.jmbross.payroll.config.DatabaseSettings;
import com.jmbross.payroll.domain.Session;
import com.jmbross.payroll.ui.AppView;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PayrollApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayrollApplication.class);
    private static volatile boolean startupFailed;
    private ApplicationBootstrap bootstrap;
    private Stage stage;
    private AppView views;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Payroll modernization case study");
        try {
            bootstrap = new ApplicationBootstrap(DatabaseSettings.fromEnvironment(System.getenv()));
            views = new AppView(bootstrap.applicationService());
            showLogin();
        } catch (RuntimeException exception) {
            startupFailed = true;
            LOGGER.error("Application startup failed", exception);
            stage.setScene(new Scene(
                    new StackPane(
                            new Label(
                                    "The application could not connect to its database. Check environment variables and service health.")),
                    760,
                    240));
        }
        stage.show();
        if (Boolean.parseBoolean(System.getenv("APP_SMOKE_TEST"))) {
            PauseTransition shutdown = new PauseTransition(javafx.util.Duration.seconds(3));
            shutdown.setOnFinished(event -> Platform.exit());
            shutdown.play();
        }
    }

    private void showLogin() {
        stage.setScene(views.loginScene(this::showDashboard));
    }

    private void showDashboard(Session session) {
        stage.setScene(views.dashboard(session, this::showLogin).scene());
    }

    @Override
    public void stop() {
        if (bootstrap != null) {
            bootstrap.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
        if (startupFailed) {
            System.exit(1);
        }
    }
}
