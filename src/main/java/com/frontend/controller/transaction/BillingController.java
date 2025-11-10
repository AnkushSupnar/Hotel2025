package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.TableMaster;
import com.frontend.service.TableMasterService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class BillingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(BillingController.class);

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private TableMasterService tableMasterService;

    @FXML
    private VBox sectionsContainer;

    @FXML
    private VBox tablesContainer;

    @FXML
    private VBox orderDetailsContainer;

    @FXML
    private Button btnRefreshTables;

    @FXML
    private TextField txtCustomerName;

    @FXML
    private TextField txtMobileNo;

    @FXML
    private TextField txtCustomerAddress;

    @FXML
    private Button btnSearchCustomer;

    @FXML
    private Button btnAddCustomer;

    private VBox draggedBox = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Billing screen initialized");
        setupRefreshButton();
        loadSections();
    }

    /**
     * Setup refresh button with hover effects
     */
    private void setupRefreshButton() {
        // Normal style
        final String normalStyle =
            "-fx-background-color: #2196F3;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 4;" +
            "-fx-cursor: hand;";

        // Hover style
        final String hoverStyle =
            "-fx-background-color: #1976D2;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 4;" +
            "-fx-cursor: hand;";

        btnRefreshTables.setStyle(normalStyle);

        // Add hover effect
        btnRefreshTables.setOnMouseEntered(e -> btnRefreshTables.setStyle(hoverStyle));
        btnRefreshTables.setOnMouseExited(e -> btnRefreshTables.setStyle(normalStyle));

        // Add click handler
        btnRefreshTables.setOnAction(e -> refreshTables());
    }

    /**
     * Refresh all table sections
     */
    private void refreshTables() {
        LOG.info("Refreshing tables...");
        loadSections();
    }

    /**
     * Load all unique sections from tablemaster and display as boxes
     */
    private void loadSections() {
        try {
            LOG.info("Loading sections from tablemaster");
            List<String> sections = tableMasterService.getUniqueDescriptions();

            Platform.runLater(() -> {
                sectionsContainer.getChildren().clear();

                for (String section : sections) {
                    VBox sectionBox = createSectionBox(section);
                    sectionsContainer.getChildren().add(sectionBox);
                }

                LOG.info("Loaded {} sections successfully", sections.size());
            });

        } catch (Exception e) {
            LOG.error("Error loading sections", e);
        }
    }

    /**
     * Create a styled box for a section with drag and drop support
     */
    private VBox createSectionBox(String sectionName) {
        // Main container box with Material Design elevation and colored border
        VBox box = new VBox();
        String sectionColor = getMaterialColorForSection(sectionName);

        // Optimized: Create style string once
        String boxStyle =
            "-fx-background-color: #ffffff;" +
            "-fx-border-color: " + sectionColor + ";" +
            "-fx-border-width: 2 2 2 6;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-cursor: move;";
        box.setStyle(boxStyle);

        // Enable caching for better scrolling performance
        box.setCache(true);
        box.setCacheHint(CacheHint.SPEED);

        // FlowPane for table buttons
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(6);
        flowPane.setVgap(6);
        flowPane.setPadding(new Insets(8));
        flowPane.setAlignment(Pos.CENTER_LEFT);
        flowPane.setStyle("-fx-background-color: #ffffff;");

        // Get tables for this section
        try {
            List<TableMaster> tables = tableMasterService.getTablesByDescription(sectionName);

            if (tables.isEmpty()) {
                Label noTablesLabel = new Label("No tables in this section");
                noTablesLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 12px;");
                flowPane.getChildren().add(noTablesLabel);
            } else {
                for (TableMaster table : tables) {
                    Button tableButton = createTableButton(table);
                    flowPane.getChildren().add(tableButton);
                }
            }
        } catch (Exception e) {
            LOG.error("Error loading tables for section: {}", sectionName, e);
            Label errorLabel = new Label("Error loading tables");
            errorLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
            flowPane.getChildren().add(errorLabel);
        }

        // Add only the flowPane to the box
        box.getChildren().add(flowPane);

        // Enable drag and drop - optimized
        setupDragAndDrop(box, sectionName, boxStyle);

        return box;
    }

    /**
     * Get Material Design color for each section
     */
    private String getMaterialColorForSection(String sectionName) {
        // Material Design color palette
        switch (sectionName.toUpperCase()) {
            case "A": return "#1976D2"; // Blue 700
            case "B": return "#7B1FA2"; // Purple 700
            case "C": return "#C2185B"; // Pink 700
            case "D": return "#D32F2F"; // Red 700
            case "E": return "#F57C00"; // Orange 700
            case "G": return "#388E3C"; // Green 700
            case "V": return "#0097A7"; // Cyan 700
            case "P": return "#5D4037"; // Brown 700
            case "HP": return "#455A64"; // Blue Grey 700
            case "W": return "#00796B"; // Teal 700
            default: return "#616161"; // Grey 700
        }
    }

    /**
     * Create a button for a table with Material Design (optimized)
     */
    private Button createTableButton(TableMaster table) {
        Button button = new Button(table.getTableName());

        // TODO: Determine table status from database (Available, Occupied, Selected)
        // For now, all tables are "Available" - you'll need to add status logic
        String status = "Available"; // This should come from table status in database

        // Cache styles for performance
        final String normalStyle = getButtonStyleForStatus(status);
        final String hoverStyle = getButtonHoverStyleForStatus(status);

        button.setStyle(normalStyle);
        // No fixed width/height - let button size naturally based on text and padding

        // Store status in user data
        button.setUserData(status);

        // Optimized hover effect - use cached styles
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));

        // Click handler
        button.setOnAction(e -> {
            LOG.info("Table selected: {} (ID: {})", table.getTableName(), table.getId());
            handleTableSelection(table);
        });

        return button;
    }

    /**
     * Get Material Design button style based on table status (optimized)
     */
    private String getButtonStyleForStatus(String status) {
        // Natural sizing with 18px font like Swing version
        String baseStyle =
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
           // "-fx-padding: 8 12;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-cursor: hand;";

        switch (status) {
            case "Available":
                // Light, neutral color to show table is not occupied
                return baseStyle +
                    "-fx-background-color: #FFFFFF;" +
                    "-fx-text-fill: #424242;" +
                    "-fx-border-color: #BDBDBD;" +
                    "-fx-border-width: 2;";

            case "Occupied":
                // Material Red 500
                return baseStyle +
                    "-fx-background-color: #F44336;" +
                    "-fx-text-fill: white;";

            case "Selected":
                // Material Green 500
                return baseStyle +
                    "-fx-background-color: #4CAF50;" +
                    "-fx-text-fill: white;";

            default:
                return baseStyle +
                    "-fx-background-color: #E0E0E0;" +
                    "-fx-text-fill: #616161;";
        }
    }

    /**
     * Get hover style for button based on status (optimized)
     */
    private String getButtonHoverStyleForStatus(String status) {
        String baseStyle =
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            //"-fx-padding: 8 12;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-cursor: hand;";

        switch (status) {
            case "Available":
                // Slight grey background on hover
                return baseStyle +
                    "-fx-background-color: #F5F5F5;" +
                    "-fx-text-fill: #212121;" +
                    "-fx-border-color: #9E9E9E;" +
                    "-fx-border-width: 2;";

            case "Occupied":
                // Material Red 700
                return baseStyle +
                    "-fx-background-color: #D32F2F;" +
                    "-fx-text-fill: white;";

            case "Selected":
                // Material Green 700
                return baseStyle +
                    "-fx-background-color: #388E3C;" +
                    "-fx-text-fill: white;";

            default:
                return baseStyle +
                    "-fx-background-color: #BDBDBD;" +
                    "-fx-text-fill: #424242;";
        }
    }

    /**
     * Handle table selection
     */
    private void handleTableSelection(TableMaster table) {
        LOG.info("Table {} selected from section {}", table.getTableName(), table.getDescription());
        // TODO: Implement table selection logic - load orders, show in right panel, etc.
    }

    /**
     * Setup drag and drop handlers for section reordering (optimized)
     */
    private void setupDragAndDrop(VBox box, String sectionName, String originalStyle) {
        // Cache highlight style to avoid recreation
        String sectionColor = getMaterialColorForSection(sectionName);
        final String highlightStyle =
            "-fx-background-color: #E3F2FD;" +
            "-fx-border-color: " + sectionColor + ";" +
            "-fx-border-width: 2 2 2 6;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-cursor: move;";

        // Make the box draggable
        box.setOnDragDetected(event -> {
            draggedBox = box;
            Dragboard dragboard = box.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(sectionName);
            dragboard.setContent(content);
            box.setOpacity(0.6);
            event.consume();
        });

        box.setOnDragOver(event -> {
            if (event.getGestureSource() != box && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        box.setOnDragEntered(event -> {
            if (event.getGestureSource() != box && event.getDragboard().hasString()) {
                box.setStyle(highlightStyle);
            }
            event.consume();
        });

        box.setOnDragExited(event -> {
            box.setStyle(originalStyle);
            event.consume();
        });

        box.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString() && draggedBox != null) {
                int draggedIndex = sectionsContainer.getChildren().indexOf(draggedBox);
                int targetIndex = sectionsContainer.getChildren().indexOf(box);

                if (draggedIndex != targetIndex && draggedIndex >= 0 && targetIndex >= 0) {
                    sectionsContainer.getChildren().remove(draggedBox);
                    if (targetIndex > draggedIndex) {
                        targetIndex--;
                    }
                    sectionsContainer.getChildren().add(targetIndex, draggedBox);
                }
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });

        box.setOnDragDone(event -> {
            if (draggedBox != null) {
                draggedBox.setOpacity(1.0);
                draggedBox = null;
            }
            event.consume();
        });
    }

   }
