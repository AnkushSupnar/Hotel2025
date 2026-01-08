package com.frontend.controller;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.SessionService;
import com.frontend.view.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
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
    @FXML private Label lblShopeeName;
    @FXML private Label lblSidebarShopName;
    @FXML private javafx.scene.layout.VBox sidebarHeader;
    @FXML private BorderPane mainPane;
    @FXML private HBox menuDashboard;
    @FXML private HBox menuSales;
    @FXML private HBox menuPurchase;
    @FXML private HBox menuMaster;
    @FXML private HBox menuReport;
    @FXML private HBox menuSettings;
    @FXML private Text txtUserName;
    @FXML private HBox menuExit;
    @FXML private Label lblTodayRevenue;
    @FXML private Label lblTodayOrders;
    @FXML private Label lblActiveTables;

    // NEW: Sidebar user profile
    @FXML private Label lblSidebarUserName;
    @FXML private Label lblOnlineStatus;

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

            // Display logged-in user information
            if (SessionService.isLoggedIn()) {
                String username = SessionService.getCurrentUsername();
                String role = SessionService.getCurrentUserRole();
                txtUserName.setText(username + " (" + role + ")");

                // NEW: Set sidebar username
                if (lblSidebarUserName != null) {
                    lblSidebarUserName.setText(username);
                }
            } else {
                txtUserName.setText("Guest User");
                if (lblSidebarUserName != null) {
                    lblSidebarUserName.setText("Guest User");
                }
            }

            // NEW: Setup global search
            setupGlobalSearch();

            // Store initial dashboard content
            initialDashboard = mainPane.getCenter();

            // Store in mainPane properties for access from other controllers
            mainPane.getProperties().put("initialDashboard", initialDashboard);

            // Make sidebar header clickable to return to dashboard
            sidebarHeader.setOnMouseClicked(e -> showInitialDashboard());

            initializeDashboardData();
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

        menuSettings.setOnMouseClicked(e->{
            try {
                pane = loader.getPage("/fxml/setting/SettingMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Loaded Settings Menu");
            } catch (Exception ex) {
                LOG.error("Error loading settings: ", ex);
            }
        });
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
        try {
            lblTodayRevenue.setText("₹0.00");
            lblTodayOrders.setText("0");
            lblActiveTables.setText("0");
        } catch (Exception e) {
            LOG.error("Error initializing dashboard data: ", e);
        }
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