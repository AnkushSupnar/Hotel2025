package com.frontend.util;

import com.frontend.view.AlertNotification;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simple test class to verify Material Design alerts
 * This is for testing purposes only
 */
public class AlertTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        AlertNotification alertNotification = new AlertNotification();
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        Button successBtn = new Button("Test Success Alert");
        successBtn.setOnAction(e -> alertNotification.showSuccess("This is a success message with Material Design!"));
        
        Button errorBtn = new Button("Test Error Alert");
        errorBtn.setOnAction(e -> alertNotification.showError("This is an error message with Material Design!"));
        
        Button infoBtn = new Button("Test Info Alert");
        infoBtn.setOnAction(e -> alertNotification.showInfo("This is an information message with Material Design!"));
        
        Button warningBtn = new Button("Test Warning Alert");
        warningBtn.setOnAction(e -> alertNotification.showWarning("This is a warning message with Material Design!"));
        
        Button confirmBtn = new Button("Test Confirmation");
        confirmBtn.setOnAction(e -> {
            boolean result = alertNotification.showConfirmation(
                "Confirm Action", 
                "Are you sure you want to proceed with this action?"
            );
            alertNotification.showInfo("You selected: " + (result ? "Yes" : "No/Cancel"));
        });
        
        root.getChildren().addAll(successBtn, errorBtn, infoBtn, warningBtn, confirmBtn);
        
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.setTitle("Material Design Alerts Test");
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}