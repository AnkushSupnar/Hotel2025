package com.frontend.view;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AlertNotification {

    /**
     * Show success notification with Material Design styling
     */
    public void showSuccess(String msg) {
        try {
            Notifications.create()
                    .title("Success")
                    .text(msg)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_CENTER)
                    .graphic(createNotificationIcon("CHECK_CIRCLE", "#4caf50"))
                    .showInformation();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show error notification with Material Design styling
     */
    public void showError(String msg) {
        try {
            Notifications.create()
                    .title("Error")
                    .text(msg)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_CENTER)
                    .graphic(createNotificationIcon("TIMES_CIRCLE", "#f44336"))
                    .showError();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show warning notification with Material Design styling
     */
    public void showWarning(String msg) {
        try {
            Notifications.create()
                    .title("Warning")
                    .text(msg)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_CENTER)
                    .graphic(createNotificationIcon("EXCLAMATION_TRIANGLE", "#ff9800"))
                    .showWarning();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show info notification with Material Design styling
     */
    public void showInfo(String msg) {
        try {
            Notifications.create()
                    .title("Information")
                    .text(msg)
                    .hideAfter(Duration.seconds(8))
                    .position(Pos.TOP_CENTER)
                    .graphic(createNotificationIcon("INFO_CIRCLE", "#2196f3"))
                    .showInformation();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show confirmation dialog with Material Design styling
     */
    public boolean showConfirmation(String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);

        // Create Material Design styled Alert
        Alert notice = new Alert(Alert.AlertType.CONFIRMATION);
        notice.setTitle(title);
        notice.setHeaderText(null);
        notice.setContentText(message);

        // Custom buttons with Material Design
        ButtonType btnYes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("No", ButtonBar.ButtonData.NO);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        notice.getButtonTypes().setAll(btnYes, btnNo, btnCancel);

        // Apply Material Design styling
        applyMaterialStyling(notice);

        notice.showAndWait().ifPresent(type -> {
            if (type.getText().equals("Yes")) {
                result.set(true);
            } else if (type.getText().equals("No")) {
                result.set(false);
            } else {
                result.set(false);
            }
        });

        return result.get();
    }

    /**
     * Create FontAwesome icon for notifications
     */
    private FontAwesomeIcon createNotificationIcon(String iconName, String color) {
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName(iconName);
        icon.setFill(Color.web(color));
        icon.setSize("1.5em");
        return icon;
    }

    /**
     * Apply Material Design styling to Alert dialogs
     */
    private void applyMaterialStyling(Alert alert) {
        try {
            // Get the dialog pane
            DialogPane dialogPane = alert.getDialogPane();

            // Apply Material Design CSS
            dialogPane.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-radius: 8; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5); " +
                            "-fx-padding: 20;"
            );

            // Style the content area
            dialogPane.lookup(".content.label").setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-family: 'System';"
            );

            // Style buttons with Material Design
            dialogPane.lookupAll(".button").forEach(node -> {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    String buttonText = button.getText();

                    if ("Yes".equals(buttonText)) {
                        button.setStyle(getMaterialButtonStyle("#4caf50"));
                    } else if ("No".equals(buttonText)) {
                        button.setStyle(getMaterialButtonStyle("#f44336"));
                    } else if ("Cancel".equals(buttonText)) {
                        button.setStyle(getMaterialButtonStyle("#9e9e9e"));
                    } else {
                        button.setStyle(getMaterialButtonStyle("#2196f3"));
                    }
                }
            });

        } catch (Exception e) {
            // If styling fails, the dialog will still work with default styling
            e.printStackTrace();
        }
    }

    /**
     * Get Material Design button style
     */
    private String getMaterialButtonStyle(String color) {
        return "-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 2);";
    }

}