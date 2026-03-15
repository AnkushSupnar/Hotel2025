package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.enums.ScreenPermission;
import com.frontend.service.SessionService;
import com.frontend.util.NavigationGuard;
import com.frontend.view.AlertNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Sales Menu
 * Handles navigation to Billing and Receive Payment screens
 */
@Component
public class SalesMenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SalesMenuController.class);

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private NavigationGuard navigationGuard;

    @Autowired
    private AlertNotification alertNotification;

    @FXML
    private Button btnBack;

    @FXML
    private VBox btnBilling;

    @FXML
    private VBox btnReceivePayment;

    @FXML
    private Label lblBillingTitle;

    @FXML
    private Label lblReceivePaymentTitle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing SalesMenuController");
        setupBackButton();
        setupEventHandlers();
        applyKiranFont();
    }

    private void setupBackButton() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> {
                try {
                    LOG.info("Back button clicked - returning to home dashboard");
                    showInitialDashboard();
                } catch (Exception ex) {
                    LOG.error("Error returning to home dashboard: ", ex);
                }
            });
        }
    }

    private void showInitialDashboard() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                javafx.scene.Node initialDashboard = (javafx.scene.Node) mainPane.getProperties().get("initialDashboard");

                if (initialDashboard != null) {
                    mainPane.setCenter(initialDashboard);
                    LOG.info("Successfully restored initial dashboard");
                } else {
                    Pane pane = loader.getPage("/fxml/dashboard/Home.fxml");
                    mainPane.setCenter(pane);
                    LOG.info("Loaded dashboard from file");
                }
            }
        } catch (Exception e) {
            LOG.error("Error showing initial dashboard: ", e);
        }
    }

    private void setupEventHandlers() {
        if (btnBilling != null) {
            btnBilling.setOnMouseClicked(e -> openBillingWindow());
            addCardHoverEffect(btnBilling,
                "rgba(64, 153, 255, 0.4)",
                "rgba(64, 153, 255, 0.55)");
        }

        if (btnReceivePayment != null) {
            btnReceivePayment.setOnMouseClicked(e -> navigateToReceivePayment());
            addCardHoverEffect(btnReceivePayment,
                "rgba(46, 216, 182, 0.4)",
                "rgba(46, 216, 182, 0.55)");
        }
    }

    private void addCardHoverEffect(VBox card, String normalShadowColor, String hoverShadowColor) {
        String normalEffect = "-fx-effect: dropshadow(three-pass-box, " + normalShadowColor + ", 12, 0, 0, 4);";
        String hoverEffect = "-fx-effect: dropshadow(three-pass-box, " + hoverShadowColor + ", 18, 0, 0, 6);";

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
            String style = card.getStyle().replaceAll("-fx-effect:[^;]+;", hoverEffect);
            card.setStyle(style);
        });

        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            String style = card.getStyle().replaceAll("-fx-effect:[^;]+;", normalEffect);
            card.setStyle(style);
        });

        card.setOnMousePressed(e -> {
            card.setScaleX(0.95);
            card.setScaleY(0.95);
        });

        card.setOnMouseReleased(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
    }

    /**
     * Open Billing screen in a new fullscreen window
     */
    private void openBillingWindow() {
        // Check permission first
        if (!navigationGuard.canAccess(ScreenPermission.BILLING)) {
            alertNotification.showError("Access Denied: You don't have permission to access Billing");
            LOG.warn("Access denied for user {} to Billing screen", SessionService.getCurrentUsername());
            return;
        }

        try {
            LOG.info("Opening Billing window");

            // Load the FXML file
            Parent billingRoot = (Parent) loader.load("/fxml/transaction/BillingFrame.fxml");

            // Create a new stage (window)
            Stage billingStage = new Stage();
            billingStage.setTitle("Billing - " + SessionService.getCurrentRestaurantName());

            // Create scene
            Scene billingScene = new Scene(billingRoot);
            billingStage.setScene(billingScene);

            // Set window to fullscreen or maximized
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            billingStage.setX(bounds.getMinX());
            billingStage.setY(bounds.getMinY());
            billingStage.setWidth(bounds.getWidth());
            billingStage.setHeight(bounds.getHeight());

            // Optionally, set to maximized
            billingStage.setMaximized(true);

            // Show the window
            billingStage.show();

            LOG.info("Billing window opened successfully");

        } catch (Exception e) {
            LOG.error("Error opening billing window: ", e);
        }
    }

    private void navigateToReceivePayment() {
        LOG.info("Navigating to Receive Payment Frame");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/transaction/ReceivePaymentFrame.fxml");
        }
    }

    private void applyKiranFont() {
        try {
            Font kiranFont25 = Font.loadFont(getClass().getResourceAsStream("/fonts/kiran.ttf"), 25);
            if (kiranFont25 != null) {
                String fontFamily = kiranFont25.getFamily();
                String fontStyle = "-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: white;";

                if (lblBillingTitle != null) {
                    lblBillingTitle.setFont(kiranFont25);
                    lblBillingTitle.setStyle(fontStyle);
                    Platform.runLater(() -> {
                        lblBillingTitle.setFont(kiranFont25);
                        lblBillingTitle.setStyle(fontStyle);
                    });
                }
                if (lblReceivePaymentTitle != null) {
                    lblReceivePaymentTitle.setFont(kiranFont25);
                    lblReceivePaymentTitle.setStyle(fontStyle);
                    Platform.runLater(() -> {
                        lblReceivePaymentTitle.setFont(kiranFont25);
                        lblReceivePaymentTitle.setStyle(fontStyle);
                    });
                }
                LOG.info("Applied Kiran font '{}' (25px) to card titles", fontFamily);
            } else {
                LOG.warn("Could not load Kiran font from /fonts/kiran.ttf");
            }
        } catch (Exception e) {
            LOG.error("Error applying Kiran font: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            if (btnBack != null && btnBack.getScene() != null) {
                return (BorderPane) btnBack.getScene().lookup("#mainPane");
            } else if (btnBilling != null && btnBilling.getScene() != null) {
                return (BorderPane) btnBilling.getScene().lookup("#mainPane");
            } else if (btnReceivePayment != null && btnReceivePayment.getScene() != null) {
                return (BorderPane) btnReceivePayment.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
        }
        return null;
    }
}
