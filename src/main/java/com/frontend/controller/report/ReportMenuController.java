package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.util.NavigationGuard;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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

    @Autowired
    NavigationGuard navigationGuard;

    // Header
    @FXML private Button btnBack;

    // Tab Chips
    @FXML private HBox chipSales;
    @FXML private HBox chipPurchase;
    @FXML private HBox chipMiscellaneous;
    @FXML private HBox chipStock;
    @FXML private FontAwesomeIcon iconSales;
    @FXML private FontAwesomeIcon iconPurchase;
    @FXML private FontAwesomeIcon iconMiscellaneous;
    @FXML private FontAwesomeIcon iconStock;
    @FXML private Label lblSales;
    @FXML private Label lblPurchase;
    @FXML private Label lblMiscellaneous;
    @FXML private Label lblStock;

    // Content Areas
    @FXML private VBox salesContent;
    @FXML private VBox purchaseContent;
    @FXML private VBox miscellaneousContent;
    @FXML private VBox stockContent;

    // Report Card Title Labels
    @FXML private Label lblSalesReportTitle;
    @FXML private Label lblPaymentReceivedTitle;
    @FXML private Label lblPurchaseReportTitle;
    @FXML private Label lblPayReceiptReportTitle;
    @FXML private Label lblBillSearchTitle;
    @FXML private Label lblReducedItemTitle;
    @FXML private Label lblStockReportTitle;
    @FXML private Label lblLowStockTitle;

    // Kiran font family name (set during applyKiranFont, used by tab switch methods)
    private String kiranFontFamily;
    private Font kiranFont25;

    // Report Cards
    @FXML private VBox salesReportCard;
    @FXML private VBox purchaseReportCard;
    @FXML private VBox paymentReceivedCard;
    @FXML private VBox payReceiptReportCard;
    @FXML private VBox reducedItemReportCard;
    @FXML private VBox billSearchReportCard;
    @FXML private VBox stockReportCard;
    @FXML private VBox lowStockAlertCard;

    // Track active tab (0=Sales, 1=Purchase, 2=Miscellaneous, 3=Stock)
    private int activeTab = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing ReportMenuController");
        setupBackButton();
        applyKiranFont();
        setupTabChips();
        setupReportCards();
        restoreActiveTab();
    }

    /**
     * Restore the previously active tab after FXML reload.
     * Since this controller is a Spring singleton, activeTab persists across reloads.
     * The FXML always starts with Sales chip styled as active, so we must
     * programmatically switch to the correct tab to keep UI and state in sync.
     */
    private void restoreActiveTab() {
        int tabToRestore = activeTab;
        // Reset to force the switch methods to work (they check activeTab != X)
        activeTab = -1;
        switch (tabToRestore) {
            case 1: switchToPurchaseTab(); break;
            case 2: switchToMiscellaneousTab(); break;
            case 3: switchToStockTab(); break;
            default: switchToSalesTab(); break;
        }
    }

    private void applyKiranFont() {
        try {
            kiranFont25 = Font.loadFont(getClass().getResourceAsStream("/fonts/kiran.ttf"), 25);
            if (kiranFont25 != null) {
                kiranFontFamily = kiranFont25.getFamily();
                String cardFontStyle = "-fx-font-family: '" + kiranFontFamily + "'; -fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: white;";

                // Apply to all card title labels
                Label[] cardLabels = {lblSalesReportTitle, lblPaymentReceivedTitle,
                        lblPurchaseReportTitle, lblPayReceiptReportTitle,
                        lblBillSearchTitle, lblReducedItemTitle,
                        lblStockReportTitle, lblLowStockTitle};
                for (Label lbl : cardLabels) {
                    if (lbl != null) {
                        lbl.setFont(kiranFont25);
                        lbl.setStyle(cardFontStyle);
                        Platform.runLater(() -> {
                            lbl.setFont(kiranFont25);
                            lbl.setStyle(cardFontStyle);
                        });
                    }
                }

                // Apply to tab chip labels (their styles are managed by switchTo*Tab methods too)
                Label[] tabLabels = {lblSales, lblPurchase, lblMiscellaneous, lblStock};
                for (Label lbl : tabLabels) {
                    if (lbl != null) {
                        lbl.setFont(kiranFont25);
                    }
                }

                LOG.info("Applied Kiran font '{}' (25px) to report labels", kiranFontFamily);
            } else {
                LOG.warn("Could not load Kiran font from /fonts/kiran.ttf");
            }
        } catch (Exception e) {
            LOG.error("Error applying Kiran font: ", e);
        }
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
        // Make chips pickable on entire bounds (not just non-transparent areas)
        chipSales.setPickOnBounds(true);
        chipPurchase.setPickOnBounds(true);
        chipMiscellaneous.setPickOnBounds(true);
        chipStock.setPickOnBounds(true);

        // Make child elements (icons and labels) mouse transparent so clicks go to parent
        iconSales.setMouseTransparent(true);
        iconPurchase.setMouseTransparent(true);
        iconMiscellaneous.setMouseTransparent(true);
        iconStock.setMouseTransparent(true);
        lblSales.setMouseTransparent(true);
        lblPurchase.setMouseTransparent(true);
        lblMiscellaneous.setMouseTransparent(true);
        lblStock.setMouseTransparent(true);

        // Sales Tab Click - use event filter to ensure we receive the event
        chipSales.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (activeTab != 0) {
                switchToSalesTab();
            }
            e.consume();
        });

        // Purchase Tab Click
        chipPurchase.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (activeTab != 1) {
                switchToPurchaseTab();
            }
            e.consume();
        });

        // Miscellaneous Tab Click
        chipMiscellaneous.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (activeTab != 2) {
                switchToMiscellaneousTab();
            }
            e.consume();
        });

        // Stock Tab Click
        chipStock.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (activeTab != 3) {
                switchToStockTab();
            }
            e.consume();
        });

        // Add hover effects for better UX
        setupChipHoverEffects();
    }

    private void setupChipHoverEffects() {
        // Sales chip hover and press effects
        chipSales.setOnMouseEntered(e -> {
            if (activeTab != 0) {
                chipSales.setStyle("-fx-background-color: #C8E6C9; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.3), 10, 0, 0, 4);");
            }
        });
        chipSales.setOnMouseExited(e -> {
            if (activeTab != 0) {
                chipSales.setStyle("-fx-background-color: #E8EAF6; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");
            }
        });
        chipSales.setOnMousePressed(e -> {
            if (activeTab != 0) {
                chipSales.setScaleX(0.95);
                chipSales.setScaleY(0.95);
            }
        });
        chipSales.setOnMouseReleased(e -> {
            chipSales.setScaleX(1.0);
            chipSales.setScaleY(1.0);
        });

        // Purchase chip hover and press effects
        chipPurchase.setOnMouseEntered(e -> {
            if (activeTab != 1) {
                chipPurchase.setStyle("-fx-background-color: #E1BEE7; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(123, 31, 162, 0.3), 10, 0, 0, 4);");
            }
        });
        chipPurchase.setOnMouseExited(e -> {
            if (activeTab != 1) {
                chipPurchase.setStyle("-fx-background-color: #E8EAF6; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");
            }
        });
        chipPurchase.setOnMousePressed(e -> {
            if (activeTab != 1) {
                chipPurchase.setScaleX(0.95);
                chipPurchase.setScaleY(0.95);
            }
        });
        chipPurchase.setOnMouseReleased(e -> {
            chipPurchase.setScaleX(1.0);
            chipPurchase.setScaleY(1.0);
        });

        // Miscellaneous chip hover and press effects
        chipMiscellaneous.setOnMouseEntered(e -> {
            if (activeTab != 2) {
                chipMiscellaneous.setStyle("-fx-background-color: #FFE0B2; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(230, 81, 0, 0.3), 10, 0, 0, 4);");
            }
        });
        chipMiscellaneous.setOnMouseExited(e -> {
            if (activeTab != 2) {
                chipMiscellaneous.setStyle("-fx-background-color: #E8EAF6; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");
            }
        });
        chipMiscellaneous.setOnMousePressed(e -> {
            if (activeTab != 2) {
                chipMiscellaneous.setScaleX(0.95);
                chipMiscellaneous.setScaleY(0.95);
            }
        });
        chipMiscellaneous.setOnMouseReleased(e -> {
            chipMiscellaneous.setScaleX(1.0);
            chipMiscellaneous.setScaleY(1.0);
        });

        // Stock chip hover and press effects
        chipStock.setOnMouseEntered(e -> {
            if (activeTab != 3) {
                chipStock.setStyle("-fx-background-color: #B2DFDB; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 137, 123, 0.3), 10, 0, 0, 4);");
            }
        });
        chipStock.setOnMouseExited(e -> {
            if (activeTab != 3) {
                chipStock.setStyle("-fx-background-color: #E8EAF6; " +
                        "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);");
            }
        });
        chipStock.setOnMousePressed(e -> {
            if (activeTab != 3) {
                chipStock.setScaleX(0.95);
                chipStock.setScaleY(0.95);
            }
        });
        chipStock.setOnMouseReleased(e -> {
            chipStock.setScaleX(1.0);
            chipStock.setScaleY(1.0);
        });
    }

    private static final String INACTIVE_CHIP_STYLE = "-fx-background-color: #E8EAF6; " +
            "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 3, 0, 0, 1);";

    private void resetAllChips() {
        chipSales.setStyle(INACTIVE_CHIP_STYLE);
        chipPurchase.setStyle(INACTIVE_CHIP_STYLE);
        chipMiscellaneous.setStyle(INACTIVE_CHIP_STYLE);
        chipStock.setStyle(INACTIVE_CHIP_STYLE);

        iconSales.setFill(Color.valueOf("#4CAF50"));
        iconPurchase.setFill(Color.valueOf("#7B1FA2"));
        iconMiscellaneous.setFill(Color.valueOf("#E65100"));
        iconStock.setFill(Color.valueOf("#00897B"));

        String fontPart = kiranFontFamily != null ? "-fx-font-family: '" + kiranFontFamily + "'; " : "";
        lblSales.setStyle(fontPart + "-fx-text-fill: #5E35B1; -fx-font-size: 25px; -fx-font-weight: bold;");
        lblPurchase.setStyle(fontPart + "-fx-text-fill: #5E35B1; -fx-font-size: 25px; -fx-font-weight: bold;");
        lblMiscellaneous.setStyle(fontPart + "-fx-text-fill: #E65100; -fx-font-size: 25px; -fx-font-weight: bold;");
        lblStock.setStyle(fontPart + "-fx-text-fill: #00897B; -fx-font-size: 25px; -fx-font-weight: bold;");

        salesContent.setVisible(false);
        salesContent.setManaged(false);
        purchaseContent.setVisible(false);
        purchaseContent.setManaged(false);
        miscellaneousContent.setVisible(false);
        miscellaneousContent.setManaged(false);
        stockContent.setVisible(false);
        stockContent.setManaged(false);
    }

    private void switchToSalesTab() {
        LOG.info("Switching to Sales tab");
        activeTab = 0;
        resetAllChips();

        chipSales.setStyle("-fx-background-color: linear-gradient(to right, #4CAF50, #388E3C); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.4), 8, 0, 0, 3);");
        iconSales.setFill(Color.WHITE);
        String fp = kiranFontFamily != null ? "-fx-font-family: '" + kiranFontFamily + "'; " : "";
        lblSales.setStyle(fp + "-fx-text-fill: white; -fx-font-size: 25px; -fx-font-weight: bold;");

        salesContent.setVisible(true);
        salesContent.setManaged(true);
    }

    private void switchToPurchaseTab() {
        LOG.info("Switching to Purchase tab");
        activeTab = 1;
        resetAllChips();

        chipPurchase.setStyle("-fx-background-color: linear-gradient(to right, #7B1FA2, #6A1B9A); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(123, 31, 162, 0.4), 8, 0, 0, 3);");
        iconPurchase.setFill(Color.WHITE);
        String fp1 = kiranFontFamily != null ? "-fx-font-family: '" + kiranFontFamily + "'; " : "";
        lblPurchase.setStyle(fp1 + "-fx-text-fill: white; -fx-font-size: 25px; -fx-font-weight: bold;");

        purchaseContent.setVisible(true);
        purchaseContent.setManaged(true);
    }

    private void switchToMiscellaneousTab() {
        LOG.info("Switching to Miscellaneous tab");
        activeTab = 2;
        resetAllChips();

        chipMiscellaneous.setStyle("-fx-background-color: linear-gradient(to right, #E65100, #BF360C); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(230, 81, 0, 0.4), 8, 0, 0, 3);");
        iconMiscellaneous.setFill(Color.WHITE);
        String fp2 = kiranFontFamily != null ? "-fx-font-family: '" + kiranFontFamily + "'; " : "";
        lblMiscellaneous.setStyle(fp2 + "-fx-text-fill: white; -fx-font-size: 25px; -fx-font-weight: bold;");

        miscellaneousContent.setVisible(true);
        miscellaneousContent.setManaged(true);
    }

    private void switchToStockTab() {
        LOG.info("Switching to Stock tab");
        activeTab = 3;
        resetAllChips();

        chipStock.setStyle("-fx-background-color: linear-gradient(to right, #00897B, #00695C); " +
                "-fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand; -fx-min-width: 160; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 137, 123, 0.4), 8, 0, 0, 3);");
        iconStock.setFill(Color.WHITE);
        String fp3 = kiranFontFamily != null ? "-fx-font-family: '" + kiranFontFamily + "'; " : "";
        lblStock.setStyle(fp3 + "-fx-text-fill: white; -fx-font-size: 25px; -fx-font-weight: bold;");

        stockContent.setVisible(true);
        stockContent.setManaged(true);
    }

    private void setupReportCards() {
        // Sales tab cards
        setupCard(salesReportCard, "rgba(76, 175, 80, 0.4)", "rgba(76, 175, 80, 0.55)", this::loadSalesReport);
        setupCard(paymentReceivedCard, "rgba(46, 216, 182, 0.4)", "rgba(46, 216, 182, 0.55)", this::loadPaymentReceivedReport);

        // Purchase tab cards
        setupCard(purchaseReportCard, "rgba(124, 77, 255, 0.4)", "rgba(124, 77, 255, 0.55)", this::loadPurchaseReport);
        setupCard(payReceiptReportCard, "rgba(255, 152, 0, 0.4)", "rgba(255, 152, 0, 0.55)", this::loadPayReceiptReport);

        // Miscellaneous tab cards
        setupCard(billSearchReportCard, "rgba(230, 81, 0, 0.4)", "rgba(230, 81, 0, 0.55)", this::loadBillSearchReport);
        setupCard(reducedItemReportCard, "rgba(255, 83, 112, 0.4)", "rgba(255, 83, 112, 0.55)", this::loadReducedItemReport);

        // Stock tab cards
        setupCard(stockReportCard, "rgba(0, 137, 123, 0.4)", "rgba(0, 137, 123, 0.55)", this::loadStockReport);
        setupCard(lowStockAlertCard, "rgba(255, 160, 0, 0.4)", "rgba(255, 160, 0, 0.55)", this::loadStockReport);
    }

    private void setupCard(VBox card, String normalShadowColor, String hoverShadowColor, Runnable action) {
        if (card == null) return;
        card.setOnMouseClicked(e -> action.run());
        addCardHoverEffect(card, normalShadowColor, hoverShadowColor);
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

    private void loadSalesReport() {
        LOG.info("Loading Sales Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/SalesReport.fxml");
        }
    }

    private void loadPurchaseReport() {
        LOG.info("Loading Purchase Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/PurchaseReport.fxml");
        }
    }

    private void loadPaymentReceivedReport() {
        LOG.info("Loading Payment Received Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/PaymentReceivedReport.fxml");
        }
    }

    private void loadPayReceiptReport() {
        LOG.info("Loading Pay Receipt Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/PayReceiptReport.fxml");
        }
    }

    private void loadReducedItemReport() {
        LOG.info("Loading Reduced Item Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/ReducedItemReport.fxml");
        }
    }

    private void loadBillSearchReport() {
        LOG.info("Loading Bill Search Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/BillSearch.fxml");
        }
    }

    private void loadStockReport() {
        LOG.info("Loading Stock Report");
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/report/StockReport.fxml");
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
