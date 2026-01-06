package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Purchase Menu
 * Handles navigation to Purchase Order, Purchase Invoice, and Pay Receipt screens
 */
@Component
public class PurchaseMenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseMenuController.class);

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private Button btnPurchaseOrder;

    @FXML
    private Button btnViewOrders;

    @FXML
    private Button btnPurchaseInvoice;

    @FXML
    private Button btnViewInvoices;

    @FXML
    private Button btnPayReceipt;

    @FXML
    private Button btnViewPayments;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing PurchaseMenuController");
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        if (btnPurchaseOrder != null) {
            btnPurchaseOrder.setOnAction(e -> navigateToPurchaseOrder());
        }

        if (btnViewOrders != null) {
            btnViewOrders.setOnAction(e -> navigateToPurchaseOrder());
        }

        if (btnPurchaseInvoice != null) {
            btnPurchaseInvoice.setOnAction(e -> navigateToPurchaseInvoice());
        }

        if (btnViewInvoices != null) {
            btnViewInvoices.setOnAction(e -> navigateToPurchaseInvoice());
        }

        if (btnPayReceipt != null) {
            btnPayReceipt.setOnAction(e -> navigateToPayReceipt());
        }

        if (btnViewPayments != null) {
            btnViewPayments.setOnAction(e -> navigateToPayReceipt());
        }
    }

    private void navigateToPurchaseOrder() {
        try {
            LOG.info("Navigating to Purchase Order Frame");
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/transaction/PurchaseOrderFrame.fxml");
                mainPane.setCenter(pane);
                LOG.info("Successfully navigated to Purchase Order Frame");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to Purchase Order Frame: ", e);
        }
    }

    private void navigateToPurchaseInvoice() {
        try {
            LOG.info("Navigating to Purchase Invoice Frame");
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/transaction/PurchaseBillFrame.fxml");
                mainPane.setCenter(pane);
                LOG.info("Successfully navigated to Purchase Invoice Frame");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to Purchase Invoice Frame: ", e);
        }
    }

    private void navigateToPayReceipt() {
        try {
            LOG.info("Navigating to Pay Receipt Frame");
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/transaction/PayReceiptFrame.fxml");
                mainPane.setCenter(pane);
                LOG.info("Successfully navigated to Pay Receipt Frame");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to Pay Receipt Frame: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            if (btnPurchaseOrder != null && btnPurchaseOrder.getScene() != null) {
                return (BorderPane) btnPurchaseOrder.getScene().lookup("#mainPane");
            } else if (btnPurchaseInvoice != null && btnPurchaseInvoice.getScene() != null) {
                return (BorderPane) btnPurchaseInvoice.getScene().lookup("#mainPane");
            } else if (btnPayReceipt != null && btnPayReceipt.getScene() != null) {
                return (BorderPane) btnPayReceipt.getScene().lookup("#mainPane");
            } else if (btnViewPayments != null && btnViewPayments.getScene() != null) {
                return (BorderPane) btnViewPayments.getScene().lookup("#mainPane");
            } else if (btnViewOrders != null && btnViewOrders.getScene() != null) {
                return (BorderPane) btnViewOrders.getScene().lookup("#mainPane");
            } else if (btnViewInvoices != null && btnViewInvoices.getScene() != null) {
                return (BorderPane) btnViewInvoices.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
        }
        return null;
    }
}
