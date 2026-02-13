package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.util.NavigationGuard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
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

    @Autowired
    private NavigationGuard navigationGuard;

    @FXML
    private Button btnBack;

    @FXML
    private VBox btnPurchaseOrder;

    @FXML
    private VBox btnPurchaseInvoice;

    @FXML
    private VBox btnPayReceipt;

    @FXML
    private Label lblPurchaseOrderTitle;

    @FXML
    private Label lblPurchaseInvoiceTitle;

    @FXML
    private Label lblPayReceiptTitle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing PurchaseMenuController");
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
                Node initialDashboard = (Node) mainPane.getProperties().get("initialDashboard");

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
        if (btnPurchaseOrder != null) {
            btnPurchaseOrder.setOnMouseClicked(e -> navigateToPurchaseOrder());
            addCardHoverEffect(btnPurchaseOrder,
                "rgba(255, 152, 0, 0.4)",
                "rgba(255, 152, 0, 0.55)");
        }

        if (btnPurchaseInvoice != null) {
            btnPurchaseInvoice.setOnMouseClicked(e -> navigateToPurchaseInvoice());
            addCardHoverEffect(btnPurchaseInvoice,
                "rgba(124, 77, 255, 0.4)",
                "rgba(124, 77, 255, 0.55)");
        }

        if (btnPayReceipt != null) {
            btnPayReceipt.setOnMouseClicked(e -> navigateToPayReceipt());
            addCardHoverEffect(btnPayReceipt,
                "rgba(255, 83, 112, 0.4)",
                "rgba(255, 83, 112, 0.55)");
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

    private void navigateToPurchaseOrder() {
        LOG.info("Navigating to Purchase Order Frame");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/transaction/PurchaseOrderFrame.fxml");
        }
    }

    private void navigateToPurchaseInvoice() {
        LOG.info("Navigating to Purchase Invoice Frame");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/transaction/PurchaseBillFrame.fxml");
        }
    }

    private void navigateToPayReceipt() {
        LOG.info("Navigating to Pay Receipt Frame");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/transaction/PayReceiptFrame.fxml");
        }
    }

    private void applyKiranFont() {
        try {
            Font kiranFont25 = Font.loadFont(getClass().getResourceAsStream("/fonts/kiran.ttf"), 25);
            if (kiranFont25 != null) {
                String fontFamily = kiranFont25.getFamily();
                String fontStyle = "-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: white;";

                Label[] labels = {lblPurchaseOrderTitle, lblPurchaseInvoiceTitle, lblPayReceiptTitle};
                for (Label lbl : labels) {
                    if (lbl != null) {
                        lbl.setFont(kiranFont25);
                        lbl.setStyle(fontStyle);
                        Platform.runLater(() -> {
                            lbl.setFont(kiranFont25);
                            lbl.setStyle(fontStyle);
                        });
                    }
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
            } else if (btnPurchaseOrder != null && btnPurchaseOrder.getScene() != null) {
                return (BorderPane) btnPurchaseOrder.getScene().lookup("#mainPane");
            } else if (btnPurchaseInvoice != null && btnPurchaseInvoice.getScene() != null) {
                return (BorderPane) btnPurchaseInvoice.getScene().lookup("#mainPane");
            } else if (btnPayReceipt != null && btnPayReceipt.getScene() != null) {
                return (BorderPane) btnPayReceipt.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
        }
        return null;
    }
}
