package com.frontend.controller;

import com.frontend.config.SpringFXMLLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
    private StackPane categoryCard;
    
    @FXML
    private StackPane itemCard;
    
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
        setupMenuActions();
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
                // Navigate to existing item management
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/create/AddItem.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error loading item management: ", ex);
            }
        });
        
        // Customer Management
        customerCard.setOnMouseClicked(e -> {
            try {
                // Navigate to existing customer management
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/create/AddCustomer.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error loading customer management: ", ex);
            }
        });
        
        // Employee Management
        employeeCard.setOnMouseClicked(e -> {
            try {
                // Navigate to existing employee management
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/create/AddEmployee.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error loading employee management: ", ex);
            }
        });
        
        // Supplier Management
        supplierCard.setOnMouseClicked(e -> {
            try {
                // Navigate to existing supplier management
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/create/AddPurchaseParty.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error loading supplier management: ", ex);
            }
        });
        
        // User Management
        userCard.setOnMouseClicked(e -> {
            try {
                // Navigate to existing user management
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/create/AddUser.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error loading user management: ", ex);
            }
        });
        
        // Add hover effects
        addHoverEffects();
    }
    
    private void loadCategoryManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/master/AddCategory.fxml");
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
    
    private void addHoverEffects() {
        StackPane[] cards = {categoryCard, itemCard, customerCard, employeeCard, supplierCard, userCard};
        
        for (StackPane card : cards) {
            // Mouse enter effect
            card.setOnMouseEntered(e -> {
                card.setStyle(card.getStyle() + "; -fx-scale-x: 1.05; -fx-scale-y: 1.05; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.5, 0, 2);");
            });
            
            // Mouse exit effect
            card.setOnMouseExited(e -> {
                card.setStyle(card.getStyle().replaceAll("; -fx-scale-x: 1.05; -fx-scale-y: 1.05; -fx-effect: dropshadow\\(gaussian, rgba\\(0,0,0,0.2\\), 10, 0.5, 0, 2\\);", ""));
            });
        }
    }
}