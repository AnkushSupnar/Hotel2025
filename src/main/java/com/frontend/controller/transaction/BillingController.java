package com.frontend.controller.transaction;

import com.frontend.common.CommonMethod;
import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.customUI.AutoCompleteTextField_old;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.entity.CategoryMaster;
import com.frontend.entity.Customer;
import com.frontend.entity.Item;
import com.frontend.entity.TableMaster;
import com.frontend.service.CategoryApiService;
import com.frontend.service.CustomerService;
import com.frontend.service.ItemService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

    @Autowired
    private CategoryApiService categoryApiService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CommonMethod commonMethod;

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

    @FXML
    private TextField txtCode;

    @FXML
    private TextField txtItemName;

    @FXML
    private TextField txtQuantity;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtAmount;

    // Action Buttons
    @FXML
    private Button btnAdd;

    @FXML
    private Button btnOrder;

    @FXML
    private Button btnEdit;

    @FXML
    private Button btnRemove;

    @FXML
    private Button btnClear;

    // Autocomplete and customer tracking
    private AutoCompleteTextField_old customerAutoComplete;
    private List<Customer> allCustomers;
    private Customer selectedCustomer;

    // Category search components (Swing-style implementation)
    private ContextMenu categoryPopup;
    private ListView<String> categoryListView;
    private List<String> allCategoryNames;
    private List<String> allItemNames;
    private List<CategoryMasterDto> allCategories;
    private CategoryMasterDto selectedCategory;

    private VBox draggedBox = null;
    Font kiranFont;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Billing screen initialized");
        kiranFont = SessionService.getCustomFont(25.0);
        setupRefreshButton();
        setupCustomFont(); // Re-enabled to fix font issue
        setupKiranFontPersistence();
        setupCustomerSearch();
        setupCategorySearch();
        setUpItemSearch();
        loadSections();
    }

    /**
     * Setup refresh button action
     */
    private void setupRefreshButton() {
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

            // Get custom Kiran font for suggestions dropdown (size 20 for readability)
            Font kiranFont = SessionService.getCustomFont(20.0);

            // Initialize autocomplete with custom suggestions and custom font
            if (kiranFont != null) {
                customerAutoComplete = new AutoCompleteTextField_old(txtCustomerSearch, suggestions, kiranFont);
                LOG.info("Customer autocomplete initialized with Kiran font");
            } else {
                customerAutoComplete = new AutoCompleteTextField_old(txtCustomerSearch, suggestions);
                LOG.warn("Kiran font not available, using default font for customer suggestions");
            }

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

            LOG.info("Customer search with autocomplete initialized successfully");

        } catch (Exception e) {
            LOG.error("Error setting up customer search", e);
        }
    }

    /**
     * Setup Kiran font persistence for all text fields
     * Applies Kiran font for Marathi text support
     */
    private void setupKiranFontPersistence() {
        try {
            Font kiranFont20 = SessionService.getCustomFont(20.0);

            if (kiranFont20 != null) {
                String fontFamily = kiranFont20.getFamily();
                String fontStyle = String.format("-fx-font-family: '%s'; -fx-font-size: 20px;", fontFamily);

                // Apply to Category field (regular TextField with autocomplete)
                txtCategoryName.setFont(kiranFont20);
                txtCategoryName.setStyle(fontStyle);

                // Apply to all text fields
                applyFontToTextField(txtWaitorName, kiranFont20, fontStyle);
                applyFontToTextField(txtCode, kiranFont20, fontStyle);
                applyFontToTextField(txtItemName, kiranFont20, fontStyle);
                applyFontToTextField(txtQuantity, kiranFont20, fontStyle);
                applyFontToTextField(txtPrice, kiranFont20, fontStyle);
                applyFontToTextField(txtAmount, kiranFont20, fontStyle);

                LOG.info("Kiran font applied to all billing text fields");
            }
        } catch (Exception e) {
            LOG.error("Error setting up Kiran font: ", e);
        }
    }

    /**
     * Helper method to apply custom font to text field
     */
    private void applyFontToTextField(TextField field, Font font, String style) {
        if (field != null) {
            field.setFont(font);
            field.setStyle(style);
        }
    }

    private void setupCustomFont() {
        try {
            // Use size 25 for Kiran font (Marathi typing)
            Font customFont = SessionService.getCustomFont(25.0);
            if (customFont != null) {
                // Apply custom font
                txtCustomerSearch.setFont(customFont);

                // Get the font family name to use in inline CSS
                String fontFamily = customFont.getFamily();

                // Apply inline style with custom font and bold effect
                String style = String.format(
                        "-fx-font-family: '%s'; " +
                                "-fx-font-size: 25px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 0, 0, 0.5, 0);",
                        fontFamily);
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
                    // Reapply style with bold effect to prevent CSS overrides
                    String boldStyle = String.format(
                            "-fx-font-family: '%s'; " +
                                    "-fx-font-size: 25px; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 0, 0, 0.5, 0);",
                            customFont.getFamily());
                    txtCustomerSearch.setStyle(boldStyle);
                });

                LOG.info("Custom Kiran font (size 25) applied to customer search field with persistence");
            } else {
                LOG.debug("No custom font available, using default font");
            }
        } catch (Exception e) {
            LOG.error("Error setting custom font: ", e);
        }
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
                                "-fx-font-family: '%s';",
                        fontFamily));
                lblCustomerMobile.setStyle(String.format(
                        "-fx-font-size: 20px; " +
                                "-fx-font-family: '%s';",
                        fontFamily));
            }

            selectedCustomerDisplay.setVisible(true);
            txtCustomerSearch.clear();

            LOG.info("Customer selected: {} (ID: {}) - {}", customer.getFullName(), customer.getId(),
                    customer.getMobileNo());
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
                            "-fx-font-family: '%s';",
                    fontFamily));
            lblCustomerMobile.setStyle(String.format(
                    "-fx-font-size: 10px; " +
                            "-fx-font-family: '%s';",
                    fontFamily));
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
     * Setup category search functionality (Swing-style implementation)
     * Matches the behavior from BillingFrame3.java generateCategorySearchBox()
     */
    private void setupCategorySearch() {
        try {
            // Load all categories from database
            allCategories = categoryApiService.getAllCategories();
            LOG.info("Loaded {} categories for search", allCategories.size());

            // Create list of category names
            allCategoryNames = new ArrayList<>();
            for (CategoryMasterDto category : allCategories) {
                allCategoryNames.add(category.getCategory());
            }

            // Get custom Kiran font for list

            new AutoCompleteTextField(txtCategoryName, allCategoryNames, kiranFont, txtCode);

            LOG.info("Category search initialized successfully (Swing-style)");

        } catch (Exception e) {
            LOG.error("Error setting up category search", e);
        }
    }

    private void setUpItemSearch() {
        txtItemName.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    System.out.println("Focused!" + txtCategoryName.getText());
                    if (!txtCode.getText().isEmpty()) {
                        int code = commonMethod.checkStringIsInteger(txtCode.getText());
                        Item item = itemService.getItemByCode(code);
                        if (item != null) {
                            setItem(item);
                        }
                    }
                    if (!txtCategoryName.getText().isEmpty()) {
                        CategoryMaster category = categoryApiService.getCategoryByName(txtCategoryName.getText())
                                .orElse(null);

                        if (category != null) {
                            allItemNames = itemService.getItemNameByCategoryId(category.getId());
                            new AutoCompleteTextField(txtItemName, allItemNames, kiranFont, txtQuantity);
                        }
                    }

                } else {
                    System.out.println("Focus lost!");
                }
            }
        });

        txtCode.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!txtCategoryName.getText().isEmpty()) {
                    int code = commonMethod.checkStringIsInteger(txtCode.getText());
                    String category = txtCategoryName.getText();
                    int catId = categoryApiService.getCategoryByName(category).orElse(null).getId();
                    Item item = itemService.findByCategoryIdAndItemCode(catId, code);
                    if (item != null) {
                        setItem(item);
                    }
                }

                System.out.println("ENter pressed on CODE");
                txtItemName.requestFocus();
            }
        });
        txtItemName.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!txtItemName.getText().isEmpty()) {
                    String itemName = txtItemName.getText();
                    Item item = itemService.getItemByName(itemName).orElse(null);
                    if (item != null) {
                        setItem(item);
                    }
                }

                System.out.println("ENter pressed on ITEM");
                txtQuantity.requestFocus();
            }
        });
    }

    public void setItem(Item item) {
        txtItemName.setText(item.getItemName());
        txtPrice.setText(item.getRate().toString());

    }

    public CategoryMasterDto getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Clear selected category
     */
    public void clearSelectedCategory() {
        selectedCategory = null;
        txtCategoryName.clear();
        LOG.info("Category selection cleared");
    }

    /**
     * Reload categories after adding new category
     */
    public void reloadCategories() {
        try {
            allCategories = categoryApiService.getAllCategories();

            // Update category names list
            allCategoryNames = new ArrayList<>();
            for (CategoryMasterDto category : allCategories) {
                allCategoryNames.add(category.getCategory());
            }

            LOG.info("Reloaded {} categories", allCategories.size());
        } catch (Exception e) {
            LOG.error("Error reloading categories", e);
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

        // Use CSS class
        box.getStyleClass().add("section-box");
        // Apply dynamic border color
        box.setStyle("-fx-border-color: " + sectionColor + ";");

        // Enable caching for better scrolling performance
        box.setCache(true);
        box.setCacheHint(CacheHint.SPEED);

        // FlowPane for table buttons
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(8);
        flowPane.setVgap(8);
        flowPane.getStyleClass().add("section-content");
        flowPane.setAlignment(Pos.CENTER_LEFT);

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

        // Enable drag and drop
        setupDragAndDrop(box, sectionName, sectionColor);

        return box;
    }

    /**
     * Get Material Design color for each section
     */
    private String getMaterialColorForSection(String sectionName) {
        // Material Design color palette
        switch (sectionName.toUpperCase()) {
            case "A":
                return "#1976D2"; // Blue 700
            case "B":
                return "#7B1FA2"; // Purple 700
            case "C":
                return "#C2185B"; // Pink 700
            case "D":
                return "#D32F2F"; // Red 700
            case "E":
                return "#F57C00"; // Orange 700
            case "G":
                return "#388E3C"; // Green 700
            case "V":
                return "#0097A7"; // Cyan 700
            case "P":
                return "#5D4037"; // Brown 700
            case "HP":
                return "#455A64"; // Blue Grey 700
            case "W":
                return "#00796B"; // Teal 700
            default:
                return "#616161"; // Grey 700
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

        // Apply CSS classes based on status
        button.getStyleClass().add("table-button");

        switch (status) {
            case "Available":
                button.getStyleClass().add("table-button-available");
                break;
            case "Occupied":
                button.getStyleClass().add("table-button-occupied");
                break;
            case "Selected":
                button.getStyleClass().add("table-button-selected");
                break;
            default:
                button.getStyleClass().add("table-button-available");
        }

        // Store status in user data
        button.setUserData(status);

        // Click handler
        button.setOnAction(e -> {
            LOG.info("Table selected: {} (ID: {})", table.getTableName(), table.getId());
            handleTableSelection(table);
        });

        return button;
    }

    /**
     * Handle table selection
     */
    private void handleTableSelection(TableMaster table) {
        LOG.info("Table {} selected from section {}", table.getTableName(), table.getDescription());
        // TODO: Implement table selection logic - load orders, show in right panel,
        // etc.
    }

    /**
     * Setup drag and drop handlers for section reordering (optimized)
     */
    private void setupDragAndDrop(VBox box, String sectionName, String sectionColor) {
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
                box.setStyle("-fx-background-color: #E3F2FD; -fx-border-color: " + sectionColor + ";");
            }
            event.consume();
        });

        box.setOnDragExited(event -> {
            box.setStyle("-fx-background-color: #ffffff; -fx-border-color: " + sectionColor + ";");
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
