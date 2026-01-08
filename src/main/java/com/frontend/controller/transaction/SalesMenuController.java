package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.enums.ScreenPermission;
import com.frontend.service.SessionService;
import com.frontend.util.NavigationGuard;
import com.frontend.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
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
    private Button btnBilling;

    @FXML
    private Button btnReceivePayment;

    @FXML
    private Button btnViewReceipts;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing SalesMenuController");
        setupBackButton();
        setupEventHandlers();
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
            btnBilling.setOnAction(e -> openBillingWindow());
        }

        if (btnReceivePayment != null) {
            btnReceivePayment.setOnAction(e -> navigateToReceivePayment());
        }

        if (btnViewReceipts != null) {
            btnViewReceipts.setOnAction(e -> navigateToReceivePayment());
        }
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

    private BorderPane getMainPane() {
        try {
            if (btnBack != null && btnBack.getScene() != null) {
                return (BorderPane) btnBack.getScene().lookup("#mainPane");
            } else if (btnBilling != null && btnBilling.getScene() != null) {
                return (BorderPane) btnBilling.getScene().lookup("#mainPane");
            } else if (btnReceivePayment != null && btnReceivePayment.getScene() != null) {
                return (BorderPane) btnReceivePayment.getScene().lookup("#mainPane");
            } else if (btnViewReceipts != null && btnViewReceipts.getScene() != null) {
                return (BorderPane) btnViewReceipts.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
        }
        return null;
    }
}
