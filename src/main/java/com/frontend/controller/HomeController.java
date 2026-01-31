package com.frontend.controller;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.DashboardService;
import com.frontend.service.SessionService;
import com.frontend.view.StageManager;
import javafx.application.Platform;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;
@Component
public class HomeController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);
    @Autowired @Lazy
    StageManager stageManager;
    @Autowired
    SpringFXMLLoader loader;
    @Autowired
    SessionService sessionService;
    @Autowired
    DashboardService dashboardService;

    // Number formatter for currency
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @FXML private Label lblShopeeName;
    @FXML private Label lblSidebarShopName;
    @FXML private javafx.scene.layout.VBox sidebarHeader;
    @FXML private BorderPane mainPane;
    @FXML private HBox menuDashboard;
    @FXML private HBox menuSales;
    @FXML private HBox menuPurchase;
    @FXML private HBox menuMaster;
    @FXML private HBox menuReport;
    @FXML private HBox menuEmployeeService;
    @FXML private HBox menuSettings;
    @FXML private Text txtUserName;
    @FXML private Text txtDesignation;
    @FXML private HBox menuExit;
    @FXML private HBox menuSupport;
    @FXML private Label lblTotalTables;
    @FXML private Label lblActiveTables;
    @FXML private Label lblClosedBillTables;
    @FXML private Label lblAvailableTables;
    @FXML private Label lblTodayCompleted;
    @FXML private HBox btnRefreshDashboard;
    @FXML private FontAwesomeIcon refreshIcon;


    // NEW: Menu badges
    @FXML private Label lblSalesBadge;
    @FXML private Label lblPurchaseBadge;

    // NEW: Global search
    @FXML private TextField txtGlobalSearch;

    private Pane pane;
    private javafx.scene.Node initialDashboard;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            // Set restaurant name from session
            String restaurantName = "Hotel Management System";
            if (SessionService.isLoggedIn() && SessionService.getCurrentShop() != null) {
                restaurantName = SessionService.getCurrentRestaurantName();
                lblShopeeName.setText(restaurantName);
                lblSidebarShopName.setText(restaurantName);
            } else {
                lblShopeeName.setText(restaurantName);
                lblSidebarShopName.setText("Restaurant Name");
            }

            // Display logged-in user information in header
            if (SessionService.isLoggedIn()) {
                String username = SessionService.getCurrentUsername();
                String designation = SessionService.getCurrentEmployeeDesignation();
                txtUserName.setText(username);
                // Set designation if available, otherwise show role
                if (designation != null && !designation.isEmpty()) {
                    txtDesignation.setText(designation);
                } else {
                    String role = SessionService.getCurrentUserRole();
                    txtDesignation.setText(role != null ? role : "");
                }
            } else {
                txtUserName.setText("Guest User");
                txtDesignation.setText("");
            }

            // NEW: Setup global search
            setupGlobalSearch();

            // Apply Kiran font to restaurant name and user fields
            applyKiranFont();

            // Store initial dashboard content
            initialDashboard = mainPane.getCenter();

            // Store in mainPane properties for access from other controllers
            mainPane.getProperties().put("initialDashboard", initialDashboard);

            // Make sidebar header clickable to return to dashboard
            sidebarHeader.setOnMouseClicked(e -> showInitialDashboard());

            initializeDashboardData();

            // Refresh button handler with spin animation
            if (btnRefreshDashboard != null) {
                btnRefreshDashboard.setOnMouseClicked(e -> {
                    LOG.info("Refresh button clicked - reloading dashboard data");
                    // Spin animation on refresh icon
                    if (refreshIcon != null) {
                        javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                                javafx.util.Duration.millis(600), refreshIcon);
                        rt.setByAngle(360);
                        rt.setCycleCount(1);
                        rt.play();
                    }
                    initializeDashboardData();
                });
            }
        } catch (Exception e) {
            LOG.error("Error initializing user data: ", e);
        }
        menuDashboard.setOnMouseClicked(e->{
            try {
                pane = loader.getPage("/fxml/dashboard/Dashboard.fxml");
                mainPane.setCenter(pane);
                LOG.info("Loaded comprehensive dashboard");
            } catch (Exception ex) {
                LOG.error("Error loading dashboard: ", ex);
            }
        });
        menuSales.setOnMouseClicked(e -> {
            try {
                pane = loader.getPage("/fxml/transaction/SalesMenu.fxml");
                mainPane.setCenter(pane);
            } catch (Exception ex) {
                LOG.error("Error loading sales menu: ", ex);
            }
        });

        menuPurchase.setOnMouseClicked(e -> {
            try {
                pane = loader.getPage("/fxml/transaction/PurchaseMenu.fxml");
                mainPane.setCenter(pane);
            } catch (Exception ex) {
                LOG.error("Error loading purchase menu: ", ex);
            }
        });
//        menuCreate.setOnMouseClicked(e->{
//            pane =loader.getPage("/fxml/create/CreateMenu.fxml");
//            mainPane.setCenter(pane);
//        });

        menuReport.setOnMouseClicked(e->{
            try {
                pane = loader.getPage("/fxml/report/ReportMenu.fxml");
                mainPane.setCenter(pane);
            } catch (Exception ex) {
                LOG.error("Error loading reports: ", ex);
            }
        });
        menuExit.setOnMouseClicked(e -> logout());
        menuMaster.setOnMouseClicked(e->{
            try {
                pane = loader.getPage("/fxml/master/MasterMenu.fxml");
                mainPane.setCenter(pane);
            } catch (Exception ex) {
                LOG.error("Error loading master: ", ex);
            }
        });

        menuEmployeeService.setOnMouseClicked(e -> {
            try {
                pane = loader.getPage("/fxml/employee/EmployeeServiceMenu.fxml");
                mainPane.setCenter(pane);
            } catch (Exception ex) {
                LOG.error("Error loading employee service: ", ex);
            }
        });

        menuSettings.setOnMouseClicked(e->{
            try {
                pane = loader.getPage("/fxml/setting/SettingMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Loaded Settings Menu");
            } catch (Exception ex) {
                LOG.error("Error loading settings: ", ex);
            }
        });

        menuSupport.setOnMouseClicked(e -> showSupportDialog());
    }
    
    /**
     * Show initial dashboard with statistics
     */
    private void showInitialDashboard() {
        try {
            LOG.info("Showing initial dashboard");
            // Restore the initial dashboard content
            if (initialDashboard != null) {
                mainPane.setCenter(initialDashboard);
                // Refresh dashboard data
                initializeDashboardData();
            }
        } catch (Exception e) {
            LOG.error("Error showing initial dashboard: ", e);
        }
    }

    private void initializeDashboardData() {
        // Set loading state
        lblTotalTables.setText("...");
        lblActiveTables.setText("...");
        lblClosedBillTables.setText("...");
        lblAvailableTables.setText("...");
        lblTodayCompleted.setText("...");

        // Load data asynchronously to avoid blocking UI
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Long> tableStatus = dashboardService.getTableStatus();

                final long total = tableStatus.getOrDefault("total", 0L);
                final long active = tableStatus.getOrDefault("active", 0L);
                final long closedBill = tableStatus.getOrDefault("closedBill", 0L);
                final long available = tableStatus.getOrDefault("available", 0L);
                final long todayCompleted = tableStatus.getOrDefault("todayCompleted", 0L);

                Platform.runLater(() -> {
                    try {
                        lblTotalTables.setText(String.valueOf(total));
                        lblActiveTables.setText(String.valueOf(active));
                        lblClosedBillTables.setText(String.valueOf(closedBill));
                        lblAvailableTables.setText(String.valueOf(available));
                        lblTodayCompleted.setText(String.valueOf(todayCompleted));

                        LOG.info("Dashboard table data loaded - Total: {}, Active: {}, ClosedBill: {}, Available: {}, Completed: {}",
                                total, active, closedBill, available, todayCompleted);
                    } catch (Exception e) {
                        LOG.error("Error updating dashboard UI: ", e);
                    }
                });

            } catch (Exception e) {
                LOG.error("Error fetching dashboard data: ", e);
                Platform.runLater(() -> {
                    lblTotalTables.setText("0");
                    lblActiveTables.setText("0");
                    lblClosedBillTables.setText("0");
                    lblAvailableTables.setText("0");
                    lblTodayCompleted.setText("0");
                });
            }
        });
    }

    private void logout() {
        try {
            // Clear user session
            sessionService.clearSession();

            // Navigate back to login
            stageManager.switchScene(com.frontend.view.FxmlView.LOGIN);

            LOG.info("User logged out successfully");
        } catch (Exception e) {
            LOG.error("Error during logout: ", e);
        }
    }

    /**
     * Show Support Contact Information Dialog
     * Uses BorderPane layout: fixed header (top), scrollable content (center), fixed footer (bottom)
     * Responsive sizing based on owner window dimensions
     */
    private void showSupportDialog() {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initOwner(mainPane.getScene().getWindow());

            // Responsive sizing from owner window
            Stage ownerStage = (Stage) mainPane.getScene().getWindow();
            double ownerW = ownerStage.getWidth();
            double ownerH = ownerStage.getHeight();
            double cardW = Math.min(460, ownerW * 0.38);
            double cardH = Math.min(ownerH * 0.88, 640);

            // === Overlay (click outside to close) ===
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.45);");
            overlay.setOnMouseClicked(e -> {
                if (e.getPickResult().getIntersectedNode() == overlay) {
                    dialogStage.close();
                }
            });

            // === Dialog Card (BorderPane = fixed header/footer, scrollable center) ===
            BorderPane dialogCard = new BorderPane();
            dialogCard.setMaxSize(cardW, cardH);
            dialogCard.setPrefSize(cardW, cardH);
            dialogCard.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;"
            );
            dialogCard.setEffect(new DropShadow(25, Color.rgb(0, 0, 0, 0.25)));
            // Prevent clicks on card from closing overlay
            dialogCard.setOnMouseClicked(e -> e.consume());

            // ==================== TOP: Header ====================
            VBox header = new VBox(6);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(20, 20, 15, 20));
            header.setStyle(
                "-fx-background-color: linear-gradient(to right, #1976D2, #1565C0, #0D47A1);" +
                "-fx-background-radius: 16 16 0 0;"
            );

            // Close X button row (top-right)
            HBox headerTopRow = new HBox();
            headerTopRow.setAlignment(Pos.CENTER_RIGHT);
            Label closeXBtn = new Label("\u2715");
            closeXBtn.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-text-fill: rgba(255,255,255,0.7);" +
                "-fx-cursor: hand;" +
                "-fx-padding: 2 6;"
            );
            closeXBtn.setOnMouseEntered(e -> closeXBtn.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 2 6;" +
                "-fx-background-color: rgba(255,255,255,0.15);" +
                "-fx-background-radius: 12;"
            ));
            closeXBtn.setOnMouseExited(e -> closeXBtn.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-text-fill: rgba(255,255,255,0.7);" +
                "-fx-cursor: hand;" +
                "-fx-padding: 2 6;"
            ));
            closeXBtn.setOnMouseClicked(e -> dialogStage.close());
            headerTopRow.getChildren().add(closeXBtn);

            // Icon circle
            StackPane iconCircle = new StackPane();
            Circle circle = new Circle(30);
            circle.setFill(Color.rgb(255, 255, 255, 0.2));
            circle.setStroke(Color.rgb(255, 255, 255, 0.5));
            circle.setStrokeWidth(2);
            FontAwesomeIcon headsetIcon = new FontAwesomeIcon();
            headsetIcon.setGlyphName("HEADPHONES");
            headsetIcon.setSize("2em");
            headsetIcon.setFill(Color.WHITE);
            iconCircle.getChildren().addAll(circle, headsetIcon);

            Label headerTitle = new Label("Contact Support");
            headerTitle.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFFFFF;"
            );
            Label headerSubtitle = new Label("We're here to help you 24/7");
            headerSubtitle.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: rgba(255,255,255,0.85);"
            );

            header.getChildren().addAll(headerTopRow, iconCircle, headerTitle, headerSubtitle);
            dialogCard.setTop(header);

            // ==================== CENTER: Scrollable Content ====================
            VBox contentBox = new VBox(6);
            contentBox.setPadding(new Insets(15, 22, 10, 22));
            contentBox.setStyle("-fx-background-color: #FFFFFF;");

            // Developer name & role
            Label devNameLabel = new Label("Ankush Supnar");
            devNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            devNameLabel.setMaxWidth(Double.MAX_VALUE);
            devNameLabel.setAlignment(Pos.CENTER);

            Label devRoleLabel = new Label("Software Developer & Support");
            devRoleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
            devRoleLabel.setMaxWidth(Double.MAX_VALUE);
            devRoleLabel.setAlignment(Pos.CENTER);

            Separator sep = new Separator();
            sep.setPadding(new Insets(4, 0, 4, 0));

            // Contact cards
            HBox mobileCard = createContactCard("MOBILE_PHONE", "#4CAF50", "Mobile Number", "+91 8329394603, +91 9960855742");
            HBox whatsappCard = createContactCard("WHATSAPP", "#25D366", "WhatsApp", "+91 8329394603");
            HBox emailCard = createContactCard("ENVELOPE", "#F44336", "Email Address", "ankushsupnar@gmail.com");
            //HBox websiteCard = createContactCard("GLOBE", "#2196F3", "Website", "www.ankushchavan.com");
            HBox addressCard = createContactCard("MAP_MARKER", "#FF9800", "Location", "Ahilyanagar, Maharashtra, India");

            contentBox.getChildren().addAll(
                devNameLabel, devRoleLabel, sep,
                mobileCard, whatsappCard, emailCard,  addressCard
            );

            // ScrollPane wrapping content - ensures it never overflows
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(contentBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle(
                "-fx-background: #FFFFFF;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-border-width: 0;"
            );
            dialogCard.setCenter(scrollPane);

            // ==================== BOTTOM: Footer (always visible) ====================
            VBox footer = new VBox(10);
            footer.setAlignment(Pos.CENTER);
            footer.setPadding(new Insets(12, 22, 16, 22));
            footer.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-border-color: #EEEEEE;" +
                "-fx-border-width: 1 0 0 0;" +
                "-fx-background-radius: 0 0 16 16;"
            );

            // Availability badge
            HBox availabilityBadge = new HBox(6);
            availabilityBadge.setAlignment(Pos.CENTER);
            availabilityBadge.setStyle(
                "-fx-background-color: #E8F5E9;" +
                "-fx-background-radius: 16;" +
                "-fx-padding: 6 14;"
            );
            Circle greenDot = new Circle(4, Color.web("#4CAF50"));
            Label availLabel = new Label("Available Mon - Sat, 9:00 AM - 9:00 PM");
            availLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2E7D32; -fx-font-weight: 600;");
            availabilityBadge.getChildren().addAll(greenDot, availLabel);

            // Close button
            Button closeBtn = new Button("Close");
            closeBtn.setPrefWidth(180);
            closeBtn.setPrefHeight(36);
            closeBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #1976D2, #1565C0);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 18; -fx-cursor: hand;"
            );
            closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #1565C0, #0D47A1);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 18; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(25,118,210,0.4), 8, 0, 0, 2);"
            ));
            closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #1976D2, #1565C0);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 18; -fx-cursor: hand;"
            ));
            closeBtn.setOnAction(e -> dialogStage.close());

            footer.getChildren().addAll(availabilityBadge, closeBtn);
            dialogCard.setBottom(footer);

            // ==================== Assemble & Show ====================
            overlay.getChildren().add(dialogCard);

            Scene dialogScene = new Scene(overlay);
            dialogScene.setFill(Color.TRANSPARENT);
            dialogScene.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    dialogStage.close();
                }
            });

            // Match owner window position & size so overlay covers it fully
            dialogStage.setX(ownerStage.getX());
            dialogStage.setY(ownerStage.getY());
            dialogStage.setWidth(ownerW);
            dialogStage.setHeight(ownerH);
            dialogStage.setScene(dialogScene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            LOG.error("Error showing support dialog: ", e);
        }
    }

    /**
     * Create a styled contact information card row
     */
    private HBox createContactCard(String iconName, String iconColor, String label, String value) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        String defaultStyle =
            "-fx-background-color: #F8F9FA;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #E8E8E8;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;";
        String hoverStyle =
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + iconColor + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 6, 0, 0, 2);";
        card.setStyle(defaultStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        // Icon
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(36, 36);
        iconBox.setMaxSize(36, 36);
        iconBox.setStyle("-fx-background-color: " + iconColor + "20; -fx-background-radius: 8;");
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName(iconName);
        icon.setSize("1.2em");
        icon.setFill(Color.web(iconColor));
        iconBox.getChildren().add(icon);

        // Text
        VBox textBox = new VBox(1);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 10px; -fx-text-fill: #9E9E9E; -fx-font-weight: 600;");
        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        textBox.getChildren().addAll(labelText, valueText);

        // Copy icon
        FontAwesomeIcon copyIcon = new FontAwesomeIcon();
        copyIcon.setGlyphName("COPY");
        copyIcon.setSize("1em");
        copyIcon.setFill(Color.web("#BDBDBD"));

        card.getChildren().addAll(iconBox, textBox, copyIcon);

        // Click to copy
        card.setOnMouseClicked(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(value);
            clipboard.setContent(content);

            // Visual feedback
            copyIcon.setGlyphName("CHECK");
            copyIcon.setFill(Color.web("#4CAF50"));
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        copyIcon.setGlyphName("COPY");
                        copyIcon.setFill(Color.web("#BDBDBD"));
                    });
                }
            }, 1500);
        });

        return card;
    }

    /**
     * Open Billing screen in a new window
     */
    private void openBillingWindow() {
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

    /**
     * Setup global search functionality
     */
    private void setupGlobalSearch() {
        if (txtGlobalSearch == null) return;

        txtGlobalSearch.setOnAction(e -> {
            String searchText = txtGlobalSearch.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                performGlobalSearch(searchText.trim());
            }
        });
    }

    /**
     * Apply Kiran font to restaurant name and user name fields
     * - lblShopeeName (header title): 28px (larger for better appearance)
     * - lblSidebarShopName (sidebar title): 20px
     * - txtUserName (user info): 16px
     * - txtDesignation: English font (not Kiran)
     */
    private void applyKiranFont() {
        try {
            // Load Kiran fonts with different sizes
            Font kiranFont28 = Font.loadFont(getClass().getResourceAsStream("/fonts/kiran.ttf"), 28);
            Font kiranFont20 = Font.loadFont(getClass().getResourceAsStream("/fonts/kiran.ttf"), 20);
            Font kiranFont16 = Font.loadFont(getClass().getResourceAsStream("/fonts/kiran.ttf"), 16);

            if (kiranFont28 != null && kiranFont20 != null && kiranFont16 != null) {
                String fontFamily = kiranFont28.getFamily();
                LOG.info("Kiran font loaded successfully, family: {}", fontFamily);

                // Apply to header shop name (28px - larger for better appearance)
                if (lblShopeeName != null) {
                    lblShopeeName.setFont(kiranFont28);
                    lblShopeeName.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
                }

                // Apply to sidebar shop name (20px)
                if (lblSidebarShopName != null) {
                    lblSidebarShopName.setFont(kiranFont20);
                    lblSidebarShopName.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
                }

                // Apply Kiran font to user name only (16px)
                if (txtUserName != null) {
                    txtUserName.setFont(kiranFont16);
                    txtUserName.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 16px; -fx-font-weight: bold;");
                }

                // Apply English font to designation (system font)
                if (txtDesignation != null) {
                    txtDesignation.setStyle("-fx-font-family: 'Segoe UI', 'Arial', sans-serif; -fx-font-size: 12px; -fx-fill: #757575;");
                }

                LOG.info("Kiran font applied to lblShopeeName (28px), lblSidebarShopName (20px), txtUserName (16px). English font for txtDesignation.");
            } else {
                LOG.warn("Could not load Kiran font from bundled resources");
            }
        } catch (Exception e) {
            LOG.error("Error applying Kiran font: ", e);
        }
    }

    /**
     * Perform global search across the application
     */
    private void performGlobalSearch(String searchText) {
        LOG.info("Global search triggered for: {}", searchText);
        // TODO: Implement global search functionality
        // This could search across customers, bills, items, etc.
    }

    /**
     * Update sales menu badge count
     */
    public void updateSalesBadge(int count) {
        if (lblSalesBadge != null) {
            if (count > 0) {
                lblSalesBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                lblSalesBadge.setVisible(true);
            } else {
                lblSalesBadge.setVisible(false);
            }
        }
    }

    /**
     * Update purchase menu badge count
     */
    public void updatePurchaseBadge(int count) {
        if (lblPurchaseBadge != null) {
            if (count > 0) {
                lblPurchaseBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                lblPurchaseBadge.setVisible(true);
            } else {
                lblPurchaseBadge.setVisible(false);
            }
        }
    }
}