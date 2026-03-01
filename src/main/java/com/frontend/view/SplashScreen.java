package com.frontend.view;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Splash screen shown during application startup while Spring context bootstraps.
 * Fully programmatic (no FXML/Spring needed since it loads before Spring context).
 */
public class SplashScreen {

    private final Stage stage;
    private final ProgressBar progressBar;
    private final Label statusLabel;

    public SplashScreen() {
        stage = new Stage(StageStyle.UNDECORATED);

        // --- Logo icon inside a circle ---
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName("CUTLERY");
        icon.setFill(Color.web("#1976D2"));
        icon.setSize("3em");

        Circle logoCircle = new Circle(45);
        logoCircle.setFill(Color.WHITE);

        StackPane logoPane = new StackPane(logoCircle, icon);
        logoPane.setAlignment(Pos.CENTER);

        // --- Title & subtitle ---
        Label title = new Label("Hotel Management");
        title.setStyle(
                "-fx-font-size: 28px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'System';"
        );

        Label subtitle = new Label("Setting up your workspace...");
        subtitle.setStyle(
                "-fx-font-size: 14px; " +
                "-fx-text-fill: rgba(255,255,255,0.85); " +
                "-fx-font-family: 'System';"
        );

        // --- Progress bar ---
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setMaxWidth(400);
        progressBar.setPrefHeight(8);
        // Only set accent color here; track and bar styled via lookup after show()
        progressBar.setStyle("-fx-accent: white;");

        // --- Status label ---
        statusLabel = new Label("Initializing...");
        statusLabel.setStyle(
                "-fx-font-size: 12px; " +
                "-fx-text-fill: rgba(255,255,255,0.8); " +
                "-fx-font-family: 'System';"
        );

        // --- Layout ---
        VBox content = new VBox(18, logoPane, title, subtitle, progressBar, statusLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60));

        // Blue gradient background matching login brand panel
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1976D2")),
                new Stop(1, Color.web("#0D47A1"))
        );
        content.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(content, 600, 400);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public Stage getStage() {
        return stage;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    /**
     * Thread-safe method to update progress and status from any thread.
     */
    public void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            statusLabel.setText(message);
        });
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();

        // Force CSS application so lookups work, then style track & bar
        progressBar.applyCss();
        Node track = progressBar.lookup(".track");
        if (track != null) {
            track.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.25); " +
                    "-fx-background-radius: 4; " +
                    "-fx-background-insets: 0;"
            );
        }
        Node bar = progressBar.lookup(".bar");
        if (bar != null) {
            bar.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-background-radius: 4; " +
                    "-fx-background-insets: 0;"
            );
        }
    }
}
