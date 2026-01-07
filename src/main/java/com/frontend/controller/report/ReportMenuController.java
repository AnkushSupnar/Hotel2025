package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class ReportMenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ReportMenuController.class);

    @Autowired
    SpringFXMLLoader loader;

    // Header
    @FXML private Button btnBack;

    // Tab Chips
    @FXML private HBox chipSales;
    @FXML private HBox chipPurchase;
    @FXML private HBox chipMiscellaneous;
    @FXML private FontAwesomeIcon iconSales;
    @FXML private FontAwesomeIcon iconPurchase;
    @FXML private FontAwesomeIcon iconMiscellaneous;
    @FXML private Label lblSales;
    @FXML private Label lblPurchase;
    @FXML private Label lblMiscellaneous;

    // Content Areas
    @FXML private VBox salesContent;
    @FXML private VBox purchaseContent;
    @FXML private VBox miscellaneousContent;

    // Report Cards
    @FXML private StackPane salesReportCard;
    @FXML private StackPane purchaseReportCard;
    @FXML private StackPane paymentReceivedCard;
    @FXML private StackPane payReceiptReportCard;
    @FXML private StackPane reducedItemReportCard;

    // Report Buttons
    @FXML private Button btnViewSalesReport;
    @FXML private Button btnViewPurchaseReport;
    @FXML private Button btnViewPaymentReceived;
    @FXML private Button btnViewPayReceipt;
    @FXML private Button btnViewReducedItemReport;

    // Track active tab (0=Sales, 1=Purchase, 2=Miscellaneous)
    private int activeTab = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing ReportMenuController");
        setupBackButton();
        setupTabChips();
        setupReportCards();
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to home dashboard");
                showInitialDashboard();
            } catch (Exception ex) {
                LOG.error("Error returning to home dashboard: ", ex);
            }
        });
    }

    private void setupTabChips() {
        // Sales Tab Click
        chipSales.setOnMouseClicked(e -> {
            if (activeTab != 0) {
                switchToSalesTab();
            }
        });

        // Purchase Tab Click
        chipPurchase.setOnMouseClicked(e -> {
            if (activeTab != 1) {
                switchToPurchaseTab();
            }
        });

        // Miscellaneous Tab Click
        chipMiscellaneous.setOnMouseClicked(e -> {
            if (activeTab != 2) {
                switchToMiscellaneousTab();
            }
        });
    }

    private void switchToSalesTab() {
        LOG.info("Switching to Sales tab");
        activeTab = 0;

        // Update chip styles using inline styles for proper rendering
        chipSales.setStyle("-fx-background-color: linear-gradient(to right, #4CAF50, #388E3C); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.4), 8, 0, 0, 3);");

        chipPurchase.setStyle("-fx-background-color: #E8EAF6; " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");

        chipMiscellaneous.setStyle("-fx-background-color: #E8EAF6; " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");

        // Update icons
        iconSales.setFill(Color.WHITE);
        iconPurchase.setFill(Color.valueOf("#7B1FA2"));
        iconMiscellaneous.setFill(Color.valueOf("#E65100"));

        // Update labels
        lblSales.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblPurchase.setStyle("-fx-text-fill: #5E35B1; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblMiscellaneous.setStyle("-fx-text-fill: #E65100; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Show/Hide content
        salesContent.setVisible(true);
        salesContent.setManaged(true);
        purchaseContent.setVisible(false);
        purchaseContent.setManaged(false);
        miscellaneousContent.setVisible(false);
        miscellaneousContent.setManaged(false);
    }

    private void switchToPurchaseTab() {
        LOG.info("Switching to Purchase tab");
        activeTab = 1;

        // Update chip styles using inline styles for proper rendering
        chipPurchase.setStyle("-fx-background-color: linear-gradient(to right, #7B1FA2, #6A1B9A); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(123, 31, 162, 0.4), 8, 0, 0, 3);");

        chipSales.setStyle("-fx-background-color: #E8EAF6; " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");

        chipMiscellaneous.setStyle("-fx-background-color: #E8EAF6; " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");

        // Update icons
        iconPurchase.setFill(Color.WHITE);
        iconSales.setFill(Color.valueOf("#4CAF50"));
        iconMiscellaneous.setFill(Color.valueOf("#E65100"));

        // Update labels
        lblPurchase.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblSales.setStyle("-fx-text-fill: #5E35B1; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblMiscellaneous.setStyle("-fx-text-fill: #E65100; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Show/Hide content
        purchaseContent.setVisible(true);
        purchaseContent.setManaged(true);
        salesContent.setVisible(false);
        salesContent.setManaged(false);
        miscellaneousContent.setVisible(false);
        miscellaneousContent.setManaged(false);
    }

    private void switchToMiscellaneousTab() {
        LOG.info("Switching to Miscellaneous tab");
        activeTab = 2;

        // Update chip styles using inline styles for proper rendering
        chipMiscellaneous.setStyle("-fx-background-color: linear-gradient(to right, #E65100, #BF360C); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(230, 81, 0, 0.4), 8, 0, 0, 3);");

        chipSales.setStyle("-fx-background-color: #E8EAF6; " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");

        chipPurchase.setStyle("-fx-background-color: #E8EAF6; " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");

        // Update icons
        iconMiscellaneous.setFill(Color.WHITE);
        iconSales.setFill(Color.valueOf("#4CAF50"));
        iconPurchase.setFill(Color.valueOf("#7B1FA2"));

        // Update labels
        lblMiscellaneous.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblSales.setStyle("-fx-text-fill: #5E35B1; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblPurchase.setStyle("-fx-text-fill: #5E35B1; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Show/Hide content
        miscellaneousContent.setVisible(true);
        miscellaneousContent.setManaged(true);
        salesContent.setVisible(false);
        salesContent.setManaged(false);
        purchaseContent.setVisible(false);
        purchaseContent.setManaged(false);
    }

    private void setupReportCards() {
        // Sales Report Card - loads SalesReport.fxml
        if (salesReportCard != null) {
            salesReportCard.setOnMouseClicked(e -> loadSalesReport());
        }

        // Purchase Report Card - loads PurchaseReport.fxml
        if (purchaseReportCard != null) {
            purchaseReportCard.setOnMouseClicked(e -> loadPurchaseReport());
        }

        // Payment Received Card - loads PaymentReceivedReport.fxml
        if (paymentReceivedCard != null) {
            paymentReceivedCard.setOnMouseClicked(e -> loadPaymentReceivedReport());
        }

        // Pay Receipt Report Card - loads PayReceiptReport.fxml
        if (payReceiptReportCard != null) {
            payReceiptReportCard.setOnMouseClicked(e -> loadPayReceiptReport());
        }

        // Reduced Item Report Card - loads ReducedItemReport.fxml
        if (reducedItemReportCard != null) {
            reducedItemReportCard.setOnMouseClicked(e -> loadReducedItemReport());
        }

        // Button handlers
        if (btnViewSalesReport != null) {
            btnViewSalesReport.setOnAction(e -> loadSalesReport());
        }

        if (btnViewPurchaseReport != null) {
            btnViewPurchaseReport.setOnAction(e -> loadPurchaseReport());
        }

        if (btnViewPaymentReceived != null) {
            btnViewPaymentReceived.setOnAction(e -> loadPaymentReceivedReport());
        }

        if (btnViewPayReceipt != null) {
            btnViewPayReceipt.setOnAction(e -> loadPayReceiptReport());
        }

        if (btnViewReducedItemReport != null) {
            btnViewReducedItemReport.setOnAction(e -> loadReducedItemReport());
        }
    }

    private void loadSalesReport() {
        LOG.info("Loading Sales Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            try {
                Pane pane = loader.getPage("/fxml/report/SalesReport.fxml");
                mainPane.setCenter(pane);
                LOG.info("Sales Report loaded successfully");
            } catch (Exception e) {
                LOG.error("Error loading Sales Report: ", e);
            }
        }
    }

    private void loadPurchaseReport() {
        LOG.info("Loading Purchase Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            try {
                Pane pane = loader.getPage("/fxml/report/PurchaseReport.fxml");
                mainPane.setCenter(pane);
                LOG.info("Purchase Report loaded successfully");
            } catch (Exception e) {
                LOG.error("Error loading Purchase Report: ", e);
            }
        }
    }

    private void loadPaymentReceivedReport() {
        LOG.info("Loading Payment Received Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            try {
                Pane pane = loader.getPage("/fxml/report/PaymentReceivedReport.fxml");
                mainPane.setCenter(pane);
                LOG.info("Payment Received Report loaded successfully");
            } catch (Exception e) {
                LOG.error("Error loading Payment Received Report: ", e);
            }
        }
    }

    private void loadPayReceiptReport() {
        LOG.info("Loading Pay Receipt Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            try {
                Pane pane = loader.getPage("/fxml/report/PayReceiptReport.fxml");
                mainPane.setCenter(pane);
                LOG.info("Pay Receipt Report loaded successfully");
            } catch (Exception e) {
                LOG.error("Error loading Pay Receipt Report: ", e);
            }
        }
    }

    private void loadReducedItemReport() {
        LOG.info("Loading Reduced Item Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            try {
                Pane pane = loader.getPage("/fxml/report/ReducedItemReport.fxml");
                mainPane.setCenter(pane);
                LOG.info("Reduced Item Report loaded successfully");
            } catch (Exception e) {
                LOG.error("Error loading Reduced Item Report: ", e);
            }
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

    private BorderPane getMainPane() {
        try {
            if (chipSales != null && chipSales.getScene() != null) {
                return (BorderPane) chipSales.getScene().lookup("#mainPane");
            } else if (btnBack != null && btnBack.getScene() != null) {
                return (BorderPane) btnBack.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
        }
        return null;
    }
}
