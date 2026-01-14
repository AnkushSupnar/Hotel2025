package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.SessionService;
import com.frontend.util.NavigationGuard;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class MasterMenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MasterMenuController.class);

    @Autowired
    SpringFXMLLoader loader;

    @Autowired
    NavigationGuard navigationGuard;

    @FXML
    private Button btnBack;

    @FXML
    private StackPane categoryCard;

    @FXML
    private StackPane itemCard;

    @FXML
    private StackPane tableCard;

    @FXML
    private StackPane customerCard;

    @FXML
    private StackPane employeeCard;

    @FXML
    private StackPane supplierCard;

    @FXML
    private StackPane userCard;

    @FXML
    private StackPane bankCard;

    // Card title labels
    @FXML
    private Label lblCategoryTitle;

    @FXML
    private Label lblItemTitle;

    @FXML
    private Label lblTableTitle;

    @FXML
    private Label lblCustomerTitle;

    @FXML
    private Label lblEmployeeTitle;

    @FXML
    private Label lblSupplierTitle;

    @FXML
    private Label lblUserTitle;

    @FXML
    private Label lblBankTitle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupMenuActions();
        applyCustomFonts();
    }

    /**
     * Setup back button to return to initial home dashboard
     */
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

    /**
     * Show initial home dashboard with statistics
     */
    private void showInitialDashboard() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                // Get the stored initial dashboard from HomeController
                javafx.scene.Node initialDashboard = (javafx.scene.Node) mainPane.getProperties().get("initialDashboard");

                if (initialDashboard != null) {
                    mainPane.setCenter(initialDashboard);
                    LOG.info("Successfully restored initial dashboard");
                } else {
                    // Fallback: load Home.fxml dashboard page
                    Pane pane = loader.getPage("/fxml/dashboard/Home.fxml");
                    mainPane.setCenter(pane);
                    LOG.info("Loaded dashboard from file");
                }
            }
        } catch (Exception e) {
            LOG.error("Error showing initial dashboard: ", e);
        }
    }

    private void setupMenuActions() {
        // Category Management
        categoryCard.setOnMouseClicked(e -> {
            try {
                loadCategoryManagement();
            } catch (Exception ex) {
                LOG.error("Error loading category management: ", ex);
            }
        });
        
        // Item Management
        itemCard.setOnMouseClicked(e -> {
            try {
                loadItemManagement();
            } catch (Exception ex) {
                LOG.error("Error loading item management: ", ex);
            }
        });

        // Table Management
        tableCard.setOnMouseClicked(e -> {
            try {
                loadTableManagement();
            } catch (Exception ex) {
                LOG.error("Error loading table management: ", ex);
            }
        });

        // Customer Management
        customerCard.setOnMouseClicked(e -> {
            try {
                loadCustomerManagement();
            } catch (Exception ex) {
                LOG.error("Error loading customer management: ", ex);
            }
        });
        
        // Employee Management
        employeeCard.setOnMouseClicked(e -> {
            try {
                loadEmployeeManagement();
            } catch (Exception ex) {
                LOG.error("Error loading employee management: ", ex);
            }
        });
        
        // Supplier Management
        supplierCard.setOnMouseClicked(e -> {
            try {
                loadSupplierManagement();
            } catch (Exception ex) {
                LOG.error("Error loading supplier management: ", ex);
            }
        });
        
        // User Management
        userCard.setOnMouseClicked(e -> {
            try {
                loadUserManagement();
            } catch (Exception ex) {
                LOG.error("Error loading user management: ", ex);
            }
        });

        // Bank Management
        bankCard.setOnMouseClicked(e -> {
            try {
                loadBankManagement();
            } catch (Exception ex) {
                LOG.error("Error loading bank management: ", ex);
            }
        });
    }
    
    private void loadCategoryManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddCategory.fxml");
        }
    }

    private void loadItemManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddItem.fxml");
        }
    }

    private void loadTableManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddTable.fxml");
        }
    }

    private void loadCustomerManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddCustomer.fxml");
        }
    }

    private void loadEmployeeManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddEmployee.fxml");
        }
    }

    private void loadSupplierManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddSupplier.fxml");
        }
    }

    private void loadUserManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddUser.fxml");
        }
    }

    private void loadBankManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/master/AddBank.fxml");
        }
    }

    private BorderPane getMainPane() {
        try {
            // Find the main BorderPane in the parent hierarchy
            return (BorderPane) categoryCard.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    /**
     * Apply custom font from SessionService to card title labels
     * Uses inline style to override CSS font-family
     */
    private void applyCustomFonts() {
        Font customFont = SessionService.getCustomFont();
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            // Use inline style to override CSS - must include all card-title properties
            String fontStyle = "-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: #1a237e;";

            if (lblCategoryTitle != null) lblCategoryTitle.setStyle(fontStyle);
            if (lblItemTitle != null) lblItemTitle.setStyle(fontStyle);
            if (lblTableTitle != null) lblTableTitle.setStyle(fontStyle);
            if (lblCustomerTitle != null) lblCustomerTitle.setStyle(fontStyle);
            if (lblEmployeeTitle != null) lblEmployeeTitle.setStyle(fontStyle);
            if (lblSupplierTitle != null) lblSupplierTitle.setStyle(fontStyle);
            if (lblUserTitle != null) lblUserTitle.setStyle(fontStyle);
            if (lblBankTitle != null) lblBankTitle.setStyle(fontStyle);

            LOG.info("Applied custom font '{}' to card titles", fontFamily);
        }
    }
}