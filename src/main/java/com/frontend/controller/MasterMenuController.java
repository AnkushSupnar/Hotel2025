package com.frontend.controller;

import com.frontend.config.SpringFXMLLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupMenuActions();
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
    }
    
    private void loadCategoryManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddCategory.fxml");
            mainPane.setCenter(pane);
        }
    }

    private void loadItemManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddItem.fxml");
            mainPane.setCenter(pane);
        }
    }

    private void loadTableManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddTable.fxml");
            mainPane.setCenter(pane);
        }
    }

    private void loadCustomerManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddCustomer.fxml");
            mainPane.setCenter(pane);
        }
    }

    private void loadEmployeeManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddEmployee.fxml");
            mainPane.setCenter(pane);
        }
    }

    private void loadSupplierManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddSupplier.fxml");
            mainPane.setCenter(pane);
        }
    }

    private void loadUserManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddUser.fxml");
            mainPane.setCenter(pane);
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
}