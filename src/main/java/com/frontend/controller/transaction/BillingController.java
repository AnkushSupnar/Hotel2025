package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Customer;
import com.frontend.entity.TableMaster;
import com.frontend.service.CustomerService;
import com.frontend.service.SessionService;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class BillingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(BillingController.class);

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private TableMasterService tableMasterService;

    @Autowired
    private CustomerService customerService;

    @FXML
    private VBox sectionsContainer;

    @FXML
    private VBox tablesContainer;

    @FXML
    private VBox orderDetailsContainer;

    @FXML
    private Button btnRefreshTables;

    // Customer Search Fields
    @FXML
    private TextField txtCustomerSearch;

    @FXML
    private HBox selectedCustomerDisplay;

    @FXML
    private Label lblCustomerName;

    @FXML
    private Label lblCustomerMobile;

    @FXML
    private Button btnClearCustomer;

    @FXML
    private Button btnAddNewCustomer;

    @FXML
    private TextField txtTableNumber;

    @FXML
    private TextField txtCategoryName;

    @FXML
    private TextField txtWaitorName;

    // Autocomplete and customer tracking
    private AutoCompleteTextField customerAutoComplete;
    private List<Customer> allCustomers;
    private Customer selectedCustomer;

    private VBox draggedBox = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Billing screen initialized");
        setupRefreshButton();
        setupCustomFont();
        setupKiranFontPersistence();
        setupCustomerSearch();
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
     * Setup customer search functionality with autocomplete
     */
    private void setupCustomerSearch() {
        try {
            // Load all customers from database
            allCustomers = customerService.getAllCustomers();
            LOG.info("Loaded {} customers for search", allCustomers.size());

            // Create suggestions list with customer names and mobile numbers
            List<String> suggestions = new ArrayList<>();
            for (Customer customer : allCustomers) {
                // Add full name with mobile format: "FirstName MiddleName LastName Mobile"
                String suggestion = customer.getFullName() + " " + customer.getMobileNo();
                suggestions.add(suggestion);
            }

            // Initialize autocomplete with custom suggestions
            customerAutoComplete = new AutoCompleteTextField(txtCustomerSearch, suggestions);

            // Add listener to detect when user selects a customer
            txtCustomerSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.trim().isEmpty()) {
                    // Check if the entered text matches a customer
                    findAndSelectCustomer(newValue.trim());
                }
            });

            // Setup button actions
            btnClearCustomer.setOnAction(e -> clearSelectedCustomer());
            btnAddNewCustomer.setOnAction(e -> addNewCustomer());

            // Add button hover effects
            setupButtonHoverEffect(btnAddNewCustomer,
                "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 0, 1);",
                "-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);");

            LOG.info("Customer search with autocomplete initialized successfully");

        } catch (Exception e) {
            LOG.error("Error setting up customer search", e);
        }
    }

    /**
     * Setup Kiran font persistence for Category and Waiter name fields
     * Prevents JavaFX CSS from overriding the font on focus/click
     */
    private void setupKiranFontPersistence() {
        try {
            // Get Kiran font at size 25 for text fields
            Font kiranFont25 = SessionService.getCustomFont(25.0);

            if (kiranFont25 != null) {
                // Apply to txtCategoryName
                txtCategoryName.setFont(kiranFont25);
                txtCategoryName.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    txtCategoryName.setFont(kiranFont25);
                });
                txtCategoryName.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (txtCategoryName.getFont() != kiranFont25) {
                        txtCategoryName.setFont(kiranFont25);
                    }
                });

                // Apply to txtWaitorName
                txtWaitorName.setFont(kiranFont25);
                txtWaitorName.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    txtWaitorName.setFont(kiranFont25);
                });
                txtWaitorName.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (txtWaitorName.getFont() != kiranFont25) {
                        txtWaitorName.setFont(kiranFont25);
                    }
                });

                LOG.info("Kiran font persistence applied to Category and Waiter name fields");
            }
        } catch (Exception e) {
            LOG.error("Error setting up Kiran font persistence: ", e);
        }
    }

    /**
     * Setup custom font for customer search text field
     * Ensures Kiran font (Marathi) persists in all states
     */
    private void setupCustomFont() {
        try {
            // Use size 25 for Kiran font (Marathi typing)
            Font customFont = SessionService.getCustomFont(25.0);
            if (customFont != null) {
                // Apply custom font
                txtCustomerSearch.setFont(customFont);

                // Get the font family name to use in inline CSS
                String fontFamily = customFont.getFamily();

                // Apply inline style with custom font to ensure it persists in all states
                // This overrides any CSS that might change the font on focus/hover
                String style = String.format(
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: transparent; " +
                    "-fx-text-fill: #212121; " +
                    "-fx-prompt-text-fill: #9E9E9E; " +
                    "-fx-font-family: '%s'; " +
                    "-fx-font-size: 25px; " +
                    "-fx-padding: 6 4; " +
                    "-fx-focus-color: transparent; " +
                    "-fx-faint-focus-color: transparent;",
                    fontFamily
                );
                txtCustomerSearch.setStyle(style);

                // Force font to persist even when text changes or field is focused
                txtCustomerSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (txtCustomerSearch.getFont() != customFont) {
                        txtCustomerSearch.setFont(customFont);
                    }
                });

                txtCustomerSearch.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (txtCustomerSearch.getFont() != customFont) {
                        txtCustomerSearch.setFont(customFont);
                    }
                    // Reapply style to prevent CSS overrides
                    txtCustomerSearch.setStyle(style);
                });

                LOG.info("Custom Kiran font (size 25) applied to customer search field with persistence");
            } else {
                LOG.debug("No custom font available, using default font");
                // Fallback style without custom font
                txtCustomerSearch.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: transparent; " +
                    "-fx-text-fill: #212121; " +
                    "-fx-prompt-text-fill: #9E9E9E; " +
                    "-fx-font-size: 12px; " +
                    "-fx-padding: 6 4;"
                );
            }
        } catch (Exception e) {
            LOG.error("Error setting custom font: ", e);
        }
    }

    /**
     * Setup button hover effect
     */
    private void setupButtonHoverEffect(Button button, String normalStyle, String hoverStyle) {
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    /**
     * Find and select customer from the entered text
     */
    private void findAndSelectCustomer(String searchText) {
        try {
            // Format is: "FirstName MiddleName LastName Mobile"
            // Mobile number is the last word
            String trimmedText = searchText.trim();
            int lastSpaceIndex = trimmedText.lastIndexOf(' ');

            if (lastSpaceIndex > 0) {
                String fullName = trimmedText.substring(0, lastSpaceIndex).trim();
                String mobile = trimmedText.substring(lastSpaceIndex + 1).trim();

                // Find the customer by name and mobile
                for (Customer customer : allCustomers) {
                    if (customer.getFullName().equals(fullName) && customer.getMobileNo().equals(mobile)) {
                        selectedCustomer = customer;
                        displaySelectedCustomer(customer);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error finding customer: ", e);
        }
    }

    /**
     * Display selected customer information
     */
    private void displaySelectedCustomer(Customer customer) {
        if (customer != null) {
            lblCustomerName.setText(customer.getFullName());
            lblCustomerMobile.setText(customer.getMobileNo());

            // Apply custom Kiran font to selected customer labels
            Font customFont = SessionService.getCustomFont(11.0);
            if (customFont != null) {
                lblCustomerName.setFont(customFont);
                lblCustomerMobile.setFont(customFont);

                // Apply inline style with custom font to ensure persistence
                String fontFamily = customFont.getFamily();
                lblCustomerName.setStyle(String.format(
                    "-fx-font-size: 20px; " +
                    "-fx-text-fill: #1565C0; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-family: '%s';",
                    fontFamily
                ));
                lblCustomerMobile.setStyle(String.format(
                    "-fx-font-size: 20px; " +
                    "-fx-text-fill: #424242; " +
                    "-fx-font-family: '%s';",
                    fontFamily
                ));
            }

            selectedCustomerDisplay.setVisible(true);
            txtCustomerSearch.clear();

            LOG.info("Customer selected: {} (ID: {}) - {}", customer.getFullName(), customer.getId(), customer.getMobileNo());
        }
    }

    /**
     * Clear selected customer
     */
    private void clearSelectedCustomer() {
        selectedCustomer = null;
        lblCustomerName.setText("-");
        lblCustomerMobile.setText("-");

        // Reset label styles
        Font customFont = SessionService.getCustomFont(11.0);
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            lblCustomerName.setStyle(String.format(
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #1565C0; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: '%s';",
                fontFamily
            ));
            lblCustomerMobile.setStyle(String.format(
                "-fx-font-size: 10px; " +
                "-fx-text-fill: #424242; " +
                "-fx-font-family: '%s';",
                fontFamily
            ));
        }

        selectedCustomerDisplay.setVisible(false);
        txtCustomerSearch.clear();

        LOG.info("Customer selection cleared");
    }

    /**
     * Get currently selected customer
     */
    public Customer getSelectedCustomer() {
        return selectedCustomer;
    }

    /**
     * Add new customer
     */
    private void addNewCustomer() {
        LOG.info("Add new customer button clicked");
        // TODO: Open customer registration dialog/window to add new customer
        // After adding, reload customers: reloadCustomers()
    }

    /**
     * Reload customers after adding new customer
     */
    public void reloadCustomers() {
        try {
            allCustomers = customerService.getAllCustomers();
            List<String> suggestions = new ArrayList<>();
            for (Customer customer : allCustomers) {
                String suggestion = customer.getFullName() + " " + customer.getMobileNo();
                suggestions.add(suggestion);
            }
            if (customerAutoComplete != null) {
                customerAutoComplete.setSuggestions(suggestions);
            }
            LOG.info("Reloaded {} customers", allCustomers.size());
        } catch (Exception e) {
            LOG.error("Error reloading customers", e);
        }
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
