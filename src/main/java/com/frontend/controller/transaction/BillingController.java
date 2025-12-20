package com.frontend.controller.transaction;

import com.frontend.common.CommonMethod;
import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.customUI.AutoCompleteTextField_old;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.entity.CategoryMaster;
import com.frontend.entity.Customer;
import com.frontend.entity.Employee;
import com.frontend.entity.Item;
import com.frontend.entity.TableMaster;
import com.frontend.entity.TempTransaction;
import com.frontend.service.BillService;
import com.frontend.service.CategoryApiService;
import com.frontend.service.CustomerService;
import com.frontend.service.EmployeeService;
import com.frontend.service.ItemService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import com.frontend.service.TempTransactionService;
import com.frontend.entity.Bill;
import com.frontend.entity.Transaction;
import com.frontend.print.BillPrint;
import com.frontend.print.KOTOrderPrint;
import com.frontend.view.AlertNotification;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.*;
import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.extern.java.Log;

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

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TempTransactionService tempTransactionService;

    @Autowired
    private BillService billService;

    @Autowired
    private KOTOrderPrint kotOrderPrint;

    @Autowired
    private BillPrint billPrint;

    @Autowired
    AlertNotification alert;

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
    private CheckBox chkAllItems;

    @FXML
    private ComboBox<String> cmbWaitorName;

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

    // Bill Action Buttons
    @FXML
    private Button btnClose;

    @FXML
    private Button btnPaid;

    @FXML
    private Button btnCredit;

    @FXML
    private Button btnOldBill;

    @FXML
    private Button btnEditBill;

    @FXML
    private TableView<TempTransaction> tblTransaction;

    @FXML
    private TableColumn<TempTransaction, Float> colAmount;

    @FXML
    private TableColumn<TempTransaction, String> colItemName;

    @FXML
    private TableColumn<TempTransaction, Float> colQuantity;

    @FXML
    private TableColumn<TempTransaction, Float> colRate;

    @FXML
    private TableColumn<TempTransaction, Integer> colSrNo;

    // Payment/Cash Counter Fields
    @FXML
    private Label lblTotalQuantity;
    @FXML
    private Label lblBillAmount;

    @FXML
    private TextField txtCashReceived;

    @FXML
    private TextField txtReturnToCustomer;

    @FXML
    private Label lblChange;

    @FXML
    private Label lblBalance;

    @FXML
    private Label lblDiscount;

    @FXML
    private Label lblNetAmount;

    // Autocomplete and customer tracking
    private AutoCompleteTextField_old customerAutoComplete;
    private List<Customer> allCustomers;
    private Customer selectedCustomer;

    private List<String> allCategoryNames;
    private List<String> allItemNames;
    private List<CategoryMasterDto> allCategories;
    private CategoryMasterDto selectedCategory;
    private List<String> allWaitorNames;
    private ObservableList<TempTransaction> tempTransactionList = FXCollections.observableArrayList();
    private TempTransaction selectedTransaction = null;
    private boolean isEditMode = false;

    // Track current closed bill for selected table (null if no closed bill exists)
    private Bill currentClosedBill = null;

    // Map to store table button references by tableId for status updates
    private java.util.Map<Integer, Button> tableButtonMap = new java.util.HashMap<>();

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
        setUpTempTransactionTable();
        setupActionButtons();
        setupCashCounter();
    }

    private void setupActionButtons() {
        btnAdd.setOnAction(e -> add());
        btnEdit.setOnAction(e -> editSelectedItem());
        btnRemove.setOnAction(e -> removeSelectedItem());
        btnClear.setOnAction(e -> clearItemForm());
        btnOrder.setOnAction(e -> processOrder());

        // Bill Action Buttons
        btnClose.setOnAction(e -> closeTable());
        btnPaid.setOnAction(e -> markAsPaid());
        btnCredit.setOnAction(e -> markAsCredit());
        btnOldBill.setOnAction(e -> showOldBills());
        btnEditBill.setOnAction(e -> editBill());

        // Enable row selection for edit/remove
        tblTransaction.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateFormFromSelection(newSelection);
            }
        });
    }

    private void setupRefreshButton() {
        btnRefreshTables.setOnAction(e -> refreshTables());
    }

    private void refreshTables() {
        LOG.info("Refreshing tables...");
        loadSections();
    }

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

            // Get custom Kiran font for suggestions dropdown (size 25 for consistency)
            Font kiranFont25 = SessionService.getCustomFont(25.0);

            // Initialize autocomplete with custom suggestions and custom font
            if (kiranFont25 != null) {
                customerAutoComplete = new AutoCompleteTextField_old(txtCustomerSearch, suggestions, kiranFont25);
                LOG.info("Customer autocomplete initialized with Kiran font (size 25)");
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

    private void setupKiranFontPersistence() {
        try {
            // Use size 25 for Kiran font (same as txtCustomerSearch for consistency)
            Font kiranFont25 = SessionService.getCustomFont(25.0);

            if (kiranFont25 != null) {
                String fontFamily = kiranFont25.getFamily();
                String fontStyle = String.format("-fx-font-family: '%s'; -fx-font-size: 25px;", fontFamily);

                // Apply to Category field (regular TextField with autocomplete)
                txtCategoryName.setFont(kiranFont25);
                txtCategoryName.setStyle(fontStyle);

                // Apply to all text fields
                applyFontToTextField(txtCode, kiranFont25, fontStyle);
                applyFontToTextField(txtItemName, kiranFont25, fontStyle);
                applyFontToTextField(txtQuantity, kiranFont25, fontStyle);
                applyFontToTextField(txtPrice, kiranFont25, fontStyle);
                applyFontToTextField(txtAmount, kiranFont25, fontStyle);

                LOG.info("Kiran font (size 25) applied to all billing text fields");
            }
        } catch (Exception e) {
            LOG.error("Error setting up Kiran font: ", e);
        }
    }

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

    private void displaySelectedCustomer(Customer customer) {
        if (customer != null) {
            lblCustomerName.setText(customer.getFullName());
            lblCustomerMobile.setText(customer.getMobileNo());

            // Apply custom Kiran font to selected customer labels (size 25 for consistency)
            Font customFont = SessionService.getCustomFont(25.0);
            if (customFont != null) {
                lblCustomerName.setFont(customFont);
                lblCustomerMobile.setFont(customFont);

                // Apply inline style with custom font to ensure persistence
                String fontFamily = customFont.getFamily();
                lblCustomerName.setStyle(String.format(
                        "-fx-font-size: 25px; " +
                                "-fx-font-family: '%s';",
                        fontFamily));
                lblCustomerMobile.setStyle(String.format(
                        "-fx-font-size: 25px; " +
                                "-fx-font-family: '%s';",
                        fontFamily));
            }

            selectedCustomerDisplay.setVisible(true);
            txtCustomerSearch.clear();

            LOG.info("Customer selected: {} (ID: {}) - {}", customer.getFullName(), customer.getId(),
                    customer.getMobileNo());
        }
    }

    private void clearSelectedCustomer() {
        selectedCustomer = null;
        lblCustomerName.setText("-");
        lblCustomerMobile.setText("-");

        // Reset label styles with size 25 for consistency
        Font customFont = SessionService.getCustomFont(25.0);
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            lblCustomerName.setStyle(String.format(
                    "-fx-font-size: 25px; " +
                            "-fx-font-family: '%s';",
                    fontFamily));
            lblCustomerMobile.setStyle(String.format(
                    "-fx-font-size: 25px; " +
                            "-fx-font-family: '%s';",
                    fontFamily));
        }

        selectedCustomerDisplay.setVisible(false);
        txtCustomerSearch.clear();

        LOG.info("Customer selection cleared");
    }

    public Customer getSelectedCustomer() {
        return selectedCustomer;
    }

    private void addNewCustomer() {
        LOG.info("Add new customer button clicked");
        // TODO: Open customer registration dialog/window to add new customer
        // After adding, reload customers: reloadCustomers()
    }

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

            // Setup "All Items" checkbox listener
            setupAllItemsCheckbox();

            txtCategoryName.focusedProperty().addListener((observable, oldValue, newValue) -> {
                // Only handle category selection if "All Items" is not checked
                if (!chkAllItems.isSelected()) {
                    if (newValue) { // On focus gained
                        if (!txtCategoryName.getText().isEmpty()) {
                            loadItemsByCategory(txtCategoryName.getText());
                        } else {
                            txtCategoryName.requestFocus();
                        }
                    } else {
                        if (!txtCategoryName.getText().isEmpty()) {
                            loadItemsByCategory(txtCategoryName.getText());
                        } else {
                            txtCategoryName.requestFocus();
                        }
                    }
                }
            });

            LOG.info("Category search initialized successfully (Swing-style)");

        } catch (Exception e) {
            LOG.error("Error setting up category search", e);
        }
    }

    /**
     * Setup checkbox to toggle between category-wise and all items mode
     */
    private void setupAllItemsCheckbox() {
        chkAllItems.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Checkbox checked - disable category, load all items
                txtCategoryName.setDisable(true);
                txtCategoryName.clear();
                txtCategoryName.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #BDBDBD; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 6 8;");
                loadAllItems();
                LOG.info("All Items mode enabled - showing all items");
            } else {
                // Checkbox unchecked - enable category, clear items
                txtCategoryName.setDisable(false);
                txtCategoryName.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 6 8;");
                allItemNames = new ArrayList<>();
                txtItemName.clear();
                LOG.info("Category mode enabled - select category to load items");
            }
        });
    }

    /**
     * Setup waiter ComboBox with all waiter names
     */
    private void setupWaitorComboBox() {
        if (allWaitorNames != null && !allWaitorNames.isEmpty()) {
            cmbWaitorName.getItems().clear();
            cmbWaitorName.getItems().addAll(allWaitorNames);

            // Apply Kiran font to ComboBox using custom cell factory
            if (kiranFont != null) {
                String fontFamily = kiranFont.getFamily();

                // Custom cell factory for dropdown list items
                cmbWaitorName.setCellFactory(listView -> new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                            setFont(kiranFont);
                            setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: 20px;", fontFamily));
                        }
                    }
                });

                // Custom button cell for selected item display
                cmbWaitorName.setButtonCell(new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                            setFont(kiranFont);
                            setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: 20px;", fontFamily));
                        }
                    }
                });

                // Apply base styling to ComboBox
                cmbWaitorName.setStyle(String.format(
                    "-fx-font-family: '%s'; -fx-font-size: 20px; " +
                    "-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; " +
                    "-fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;",
                    fontFamily));
            }

            LOG.info("Loaded {} waiters into ComboBox with Kiran font", allWaitorNames.size());
        } else {
            LOG.warn("No waiters found to load into ComboBox");
        }
    }

    /**
     * Load all items from database (when "All Items" checkbox is checked)
     */
    private void loadAllItems() {
        try {
            allItemNames = itemService.getAllItemNames();
            if (allItemNames != null && !allItemNames.isEmpty()) {
                new AutoCompleteTextField(txtItemName, allItemNames, kiranFont, txtQuantity);
                LOG.info("Loaded {} items for all items mode", allItemNames.size());
            } else {
                LOG.warn("No items found in database");
            }
        } catch (Exception e) {
            LOG.error("Error loading all items", e);
        }
    }

    private void setUpItemSearch() {
        // Setup numeric-only validation
        setupNumericValidation();

        // Setup field listeners
        setupItemNameFocusListener();
        setupQuantityListeners();

        // Setup Enter key handlers
        setupEnterKeyHandlers();
    }

    private void setupNumericValidation() {
        // Prevent non-numeric input in code field
        txtCode.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtCode.setText(oldValue);
            }
        });

        // Prevent non-numeric input in quantity field
        txtQuantity.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtQuantity.setText(oldValue);
            }
        });
    }

    private void setupItemNameFocusListener() {
        txtItemName.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // On focus gained
                handleItemNameFocus();
            } else {
                System.out.println("Focus lost on Item Name");
                handleItemNameFocus();
            }
        });

    }

    private void handleItemNameFocus() {

        if (!txtItemName.getText().isEmpty()) {
            Item item = itemService.getItemByName(txtItemName.getText().trim()).orElse(null);
            if (item != null) {
                setItem(item);
            }
        }
    }

    private void setupQuantityListeners() {
        // Calculate amount when quantity loses focus
        txtQuantity.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !txtQuantity.getText().trim().isEmpty()) { // On focus lost
                calculateAndSetAmount();
            }
        });
    }

    private void setUpTempTransactionTable() {
        // Load waiter names into ComboBox
        allWaitorNames = employeeService.getWaitorNames();
        setupWaitorComboBox();

        Font systemFont14 = Font.font(14);

        // Get Kiran font with size 25 (consistent with all other Kiran font usage)
        Font kiranFontForTable = SessionService.getCustomFont(25.0);
        if (kiranFontForTable == null) {
            kiranFontForTable = Font.font(25);
            LOG.warn("Kiran font not available for table, using system font");
        }

        // Setup Sr.No column to show row number (not database ID)
        setupSrNoColumn(colSrNo, systemFont14);
        setupColumnWithFont(colItemName, "itemName", kiranFontForTable, true);
        setupColumnWithFont(colQuantity, "qty", systemFont14, false);
        setupColumnWithFont(colRate, "rate", systemFont14, false);
        setupColumnWithFont(colAmount, "amt", systemFont14, false);

        tblTransaction.setItems(tempTransactionList);

        LOG.info("Transaction table setup complete with Kiran font (size 25) for ItemName column");
    }

    /**
     * Setup Sr.No column to display row number instead of database ID
     */
    private void setupSrNoColumn(TableColumn<TempTransaction, Integer> column, Font font) {
        column.setCellFactory(col -> new TableCell<TempTransaction, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    // Display row number (1-based index)
                    setText(String.valueOf(getTableRow().getIndex() + 1));
                    setFont(font);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private <S, T> void setupColumnWithFont(TableColumn<TempTransaction, T> column,
            String propertyName, Font font, boolean isKiranFont) {
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));

        final Font cellFont = font;
        final String fontFamily = isKiranFont && cellFont != null ? cellFont.getFamily() : null;

        column.setCellFactory(col -> new TableCell<TempTransaction, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setFont(cellFont);

                    // Apply inline style for Kiran font to ensure persistence (size 25)
                    if (isKiranFont && fontFamily != null) {
                        setStyle(String.format(
                            "-fx-font-family: '%s'; -fx-font-size: 25px;",
                            fontFamily
                        ));
                    }
                }
            }
        });
    }

    // ============= Enter Key Handlers =============
    private void setupEnterKeyHandlers() {
        // Code field - Enter key handler
        txtCode.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleCodeEnter();
            }
        });

        // Item name field - Enter key handler
        txtItemName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                System.out.println("Enter pressed on Item Name");
                handleItemNameEnter();
            }
        });

        // Quantity field - Enter key handler
        txtQuantity.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleQuantityEnter();
            }
        });
        txtPrice.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handlePriceEnter();
                btnAdd.fire();
            }
        });
    }

    private void handlePriceEnter() {
        calculateAndSetAmount();
    }

    private void handleCodeEnter() {
        String code = txtCode.getText().trim();
        String categoryName = txtCategoryName.getText().trim();

        if (!code.isEmpty() && !categoryName.isEmpty()) {
            searchItemByCode(code, categoryName);
        } else if (!code.isEmpty()) {
            // Search by code only
            try {
                int itemCode = Integer.parseInt(code);
                Item item = itemService.getItemByCode(itemCode);
                if (item != null) {
                    setItem(item);
                } else {
                    showAlert("Item not found with code: " + code);
                }
            } catch (NumberFormatException e) {
                showAlert("Invalid code format");
            }
        }

        txtItemName.requestFocus();
    }

    private void handleItemNameEnter() {
        System.out.println("Handeling Item Name Enter");
        String itemName = txtItemName.getText().trim();

        if (!itemName.isEmpty()) {
            Item item = itemService.getItemByName(itemName).orElse(null);
            if (item != null) {
                setItem(item);
                txtQuantity.requestFocus();
            } else {
                showAlert("Item not found: " + itemName);
                txtItemName.clear();
                txtItemName.requestFocus();
            }
        } else {
            showAlert("Please enter item name");
        }
    }

    private void handleQuantityEnter() {
        String itemName = txtItemName.getText().trim();
        String quantity = txtQuantity.getText().trim();

        if (itemName.isEmpty()) {
            showAlert("Please select an item first");
            txtItemName.requestFocus();
            return;
        }

        if (quantity.isEmpty()) {
            showAlert("Please enter quantity");
            return;
        }

        calculateAndSetAmount();

        // Move to next field or add to bill
        txtPrice.requestFocus(); // or wherever you want to go next
    }

    // ============= Helper Methods =============
    private void searchItemByCode(String code, String categoryName) {
        try {
            int itemCode = Integer.parseInt(code);

            if (!categoryName.isEmpty()) {
                // Search by category and code
                CategoryMaster category = categoryApiService.getCategoryByName(categoryName).orElse(null);

                if (category != null) {
                    Item item = itemService.findByCategoryIdAndItemCode(category.getId(), itemCode);
                    if (item != null) {
                        setItem(item);
                    } else {
                        showAlert("Item not found for code " + itemCode + " in category " + categoryName);
                    }
                } else {
                    showAlert("Category not found: " + categoryName);
                }
            } else {
                // Search by code only
                Item item = itemService.getItemByCode(itemCode);
                if (item != null) {
                    setItem(item);
                } else {
                    showAlert("Item not found with code: " + itemCode);
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid code format: " + code);
        }
    }

    private void loadItemsByCategory(String categoryName) {
        CategoryMaster category = categoryApiService.getCategoryByName(categoryName).orElse(null);

        if (category != null) {
            allItemNames = itemService.getItemNameByCategoryId(category.getId());
            if (allItemNames != null && !allItemNames.isEmpty()) {
                new AutoCompleteTextField(txtItemName, allItemNames, kiranFont, txtQuantity);
            }
        } else {
            showAlert("Category not found: " + categoryName);
        }
    }

    private void calculateAndSetAmount() {
        String itemName = txtItemName.getText().trim();
        String quantityText = txtQuantity.getText().trim();
        String priceText = txtPrice.getText().trim();

        if (itemName.isEmpty() || quantityText.isEmpty()) {
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityText);

            // Use price from field if user modified it, otherwise fetch from item
            float rate;
            if (!priceText.isEmpty()) {
                rate = Float.parseFloat(priceText);
            } else {
                Item item = itemService.getItemByName(itemName).orElse(null);
                if (item == null) {
                    showAlert("Item not found: " + itemName);
                    return;
                }
                rate = item.getRate();
                txtPrice.setText(String.valueOf(rate));
            }

            if (quantity <= 0) {
                showAlert("Quantity must be greater than zero");
                txtQuantity.clear();
                return;
            }

            if (rate <= 0) {
                showAlert("Rate must be greater than zero");
                return;
            }

            float total = quantity * rate;
            txtAmount.setText(String.format("%.2f", total));

        } catch (NumberFormatException e) {
            showAlert("Invalid quantity or price format");
        }
    }

    public void setItem(Item item) {
        if (item == null) {
            return;
        }
        if (!allItemNames.contains(item.getItemName())) {
            txtCode.setText("");
            txtItemName.setText("");
            txtQuantity.setText("");
            txtPrice.setText("");
            txtAmount.setText("");
        }

        txtItemName.setText(item.getItemName());
        txtCode.setText(String.valueOf(item.getItemCode()));
        txtPrice.setText(String.valueOf(item.getRate()));

        // Auto-calculate amount if quantity is already entered
        String quantityText = txtQuantity.getText().trim();
        if (!quantityText.isEmpty()) {
            try {
                int quantity = Integer.parseInt(quantityText);
                float total = quantity * item.getRate();
                txtAmount.setText(String.format("%.2f", total));
            } catch (NumberFormatException e) {
                // Ignore if quantity is invalid
            }
        }
    }

    // ============= Utility Methods =============
    private void showAlert(String message) {
        alert.showWarning(message);
    }

    // Clear item entry form fields
    private void clearItemForm() {
        txtCode.clear();
        txtItemName.clear();
        txtQuantity.clear();
        txtPrice.clear();
        txtAmount.clear();

        // Reset edit mode if active
        if (isEditMode) {
            resetEditMode();
        }
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
                noTablesLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
                flowPane.getChildren().add(noTablesLabel);
            } else {
                for (TableMaster table : tables) {
                    Button tableButton = createTableButton(table);
                    flowPane.getChildren().add(tableButton);
                    tableButton.setOnAction(e -> {
                        txtTableNumber.setText(table.getTableName());
                        // Load existing transactions for this table from database
                        loadTransactionsForTable(table.getId());
                        LOG.info("Table selected: {} (ID: {})", table.getTableName(), table.getId());
                    });
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

        // Check if table has ongoing transactions or closed bills
        boolean hasTempTransactions = tempTransactionService.hasTransactions(table.getId());
        boolean hasClosedBill = billService.hasClosedBill(table.getId());

        String status;
        if (hasClosedBill) {
            status = "Closed";
        } else if (hasTempTransactions) {
            status = "Ongoing";
        } else {
            status = "Available";
        }

        // Apply CSS classes based on status
        button.getStyleClass().add("table-button");
        applyTableButtonStatus(button, status);

        // Store button in map for later status updates
        tableButtonMap.put(table.getId(), button);

        // Store status in user data
        button.setUserData(status);

        return button;
    }

    /**
     * Apply CSS style class to table button based on status
     */
    private void applyTableButtonStatus(Button button, String status) {
        // Remove all status classes first
        button.getStyleClass().removeAll(
            "table-button-available",
            "table-button-occupied",
            "table-button-selected",
            "table-button-ongoing",
            "table-button-closed"
        );

        // Apply new status class
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
            case "Ongoing":
                button.getStyleClass().add("table-button-ongoing");
                break;
            case "Closed":
                button.getStyleClass().add("table-button-closed");
                break;
            default:
                button.getStyleClass().add("table-button-available");
        }

        // Update user data
        button.setUserData(status);
    }

    /**
     * Update table button status based on whether it has transactions or closed bills
     */
    private void updateTableButtonStatus(Integer tableId) {
        Button button = tableButtonMap.get(tableId);
        if (button != null) {
            boolean hasTempTransactions = tempTransactionService.hasTransactions(tableId);
            boolean hasClosedBill = billService.hasClosedBill(tableId);

            String newStatus;
            if (hasClosedBill) {
                newStatus = "Closed";
            } else if (hasTempTransactions) {
                newStatus = "Ongoing";
            } else {
                newStatus = "Available";
            }
            applyTableButtonStatus(button, newStatus);
            LOG.info("Table {} button status updated to: {} (temp: {}, closed: {})",
                    tableId, newStatus, hasTempTransactions, hasClosedBill);
        }
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

    private void add() {
        if (!validate()) {
            return;
        }

        if (isEditMode && selectedTransaction != null) {
            // Update existing transaction in database
            updateSelectedTransaction();
        } else {
            // Add new transaction to database and TableView
            TempTransaction tempTransaction = createTempTransactionFromForm();
            addTempTransactionToDatabase(tempTransaction);
        }

        // Clear form and update totals
        clearItemForm();
        updateTotals();
        txtCategoryName.requestFocus();
    }

    /**
     * Add transaction to database and sync with TableView
     * If item exists with same name and rate, updates quantity instead
     */
    private void addTempTransactionToDatabase(TempTransaction transaction) {
        try {
            Integer tableNo = transaction.getTableNo();

            // Save to database - service handles add/update logic
            TempTransaction savedTransaction = tempTransactionService.addOrUpdateTransaction(transaction);

            // Reload all transactions for this table to sync TableView with database
            loadTransactionsForTable(tableNo);

            // Update table button status to "Ongoing" (green)
            updateTableButtonStatus(tableNo);

            LOG.info("Transaction saved to database: {}", savedTransaction);
        } catch (Exception e) {
            LOG.error("Error saving transaction to database", e);
            alert.showError("Error saving transaction: " + e.getMessage());
        }
    }

    /**
     * Load all transactions for a table from database into TableView
     * This includes both:
     * 1. Transactions from closed bill (if exists) - shown with negative IDs to distinguish
     * 2. New temp_transactions for this table
     */
    private void loadTransactionsForTable(Integer tableNo) {
        try {
            // Clear TableView first
            tempTransactionList.clear();

            // 1. Check for closed bill and load its transactions
            currentClosedBill = billService.getClosedBillForTable(tableNo);
            if (currentClosedBill != null) {
                List<Transaction> closedBillTransactions = billService.getTransactionsForBill(currentClosedBill.getBillNo());
                LOG.info("Found {} transactions from closed bill #{} for table {}",
                        closedBillTransactions.size(), currentClosedBill.getBillNo(), tableNo);

                // Convert Transaction to TempTransaction for display
                // Use negative IDs to distinguish from temp_transaction items
                int negativeId = -1;
                for (Transaction trans : closedBillTransactions) {
                    TempTransaction displayTrans = new TempTransaction();
                    displayTrans.setId(negativeId--); // Negative ID indicates closed bill item
                    displayTrans.setItemName(trans.getItemName());
                    displayTrans.setQty(trans.getQty());
                    displayTrans.setRate(trans.getRate());
                    displayTrans.setAmt(trans.getAmt());
                    displayTrans.setTableNo(tableNo);
                    displayTrans.setWaitorId(currentClosedBill.getWaitorId());
                    displayTrans.setPrintQty(0f); // Already printed items from closed bill

                    tempTransactionList.add(displayTrans);
                }

                // Set waiter from closed bill
                if (currentClosedBill.getWaitorId() != null) {
                    try {
                        Employee waitor = employeeService.getEmployeeById(currentClosedBill.getWaitorId());
                        if (waitor != null) {
                            cmbWaitorName.getSelectionModel().select(waitor.getFirstName());
                            LOG.info("Waiter from closed bill: {} (ID: {})", waitor.getFirstName(), currentClosedBill.getWaitorId());
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not load waiter with ID: {}", currentClosedBill.getWaitorId());
                    }
                }

                // Set customer from closed bill if available
                if (currentClosedBill.getCustomerId() != null) {
                    try {
                        for (Customer customer : allCustomers) {
                            if (customer.getId().equals(currentClosedBill.getCustomerId())) {
                                selectedCustomer = customer;
                                displaySelectedCustomer(customer);
                                LOG.info("Customer from closed bill: {} (ID: {})", customer.getFullName(), customer.getId());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not load customer with ID: {}", currentClosedBill.getCustomerId());
                    }
                }
            } else {
                LOG.info("No closed bill found for table {}", tableNo);
            }

            // 2. Load new temp_transactions for this table
            List<TempTransaction> tempTransactions = tempTransactionService.getTransactionsByTableNo(tableNo);
            LOG.info("Found {} temp transactions for table {}", tempTransactions.size(), tableNo);

            // Add temp transactions (these have positive IDs from database)
            tempTransactionList.addAll(tempTransactions);

            // Set waiter from temp transactions if no closed bill waiter was set
            if (currentClosedBill == null && !tempTransactions.isEmpty()) {
                Integer waitorId = tempTransactions.get(0).getWaitorId();
                if (waitorId != null) {
                    try {
                        Employee waitor = employeeService.getEmployeeById(waitorId);
                        if (waitor != null) {
                            cmbWaitorName.getSelectionModel().select(waitor.getFirstName());
                            LOG.info("Waiter from temp transactions: {} (ID: {})", waitor.getFirstName(), waitorId);
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not load waiter with ID: {}", waitorId);
                    }
                }
            }

            // Clear waiter selection only if no items at all
            if (tempTransactionList.isEmpty()) {
                cmbWaitorName.getSelectionModel().clearSelection();
            }

            tblTransaction.refresh();
            updateTotals();

            LOG.info("Total {} items loaded for table {} (closed bill: {}, new: {})",
                    tempTransactionList.size(), tableNo,
                    currentClosedBill != null ? currentClosedBill.getBillNo() : "none",
                    tempTransactions.size());
        } catch (Exception e) {
            LOG.error("Error loading transactions for table {}", tableNo, e);
        }
    }

    private void editSelectedItem() {
        TempTransaction selected = tblTransaction.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.showError("Please select an item to edit");
            return;
        }

        selectedTransaction = selected;
        isEditMode = true;
        populateFormFromSelection(selected);

        // Change Add button text to indicate edit mode
        btnAdd.setText("UPDATE");
        btnAdd.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 95; -fx-cursor: hand;");

        txtQuantity.requestFocus();
        LOG.info("Edit mode enabled for item: {}", selected.getItemName());
    }

    private void updateSelectedTransaction() {
        if (selectedTransaction == null) {
            return;
        }

        try {
            float newQty = Float.parseFloat(txtQuantity.getText().trim());
            float newRate = Float.parseFloat(txtPrice.getText().trim());
            float newAmt = newQty * newRate;

            selectedTransaction.setQty(newQty);
            selectedTransaction.setRate(newRate);
            selectedTransaction.setAmt(newAmt);
            selectedTransaction.setPrintQty(newQty);

            // Save updated transaction to database
            tempTransactionService.updateTransaction(selectedTransaction);

            // Reload transactions to sync
            loadTransactionsForTable(selectedTransaction.getTableNo());

            LOG.info("Transaction updated in database: {}", selectedTransaction);

            // Reset edit mode
            resetEditMode();
        } catch (NumberFormatException e) {
            alert.showError("Invalid quantity or price");
        } catch (Exception e) {
            LOG.error("Error updating transaction", e);
            alert.showError("Error updating transaction: " + e.getMessage());
        }
    }

    private void resetEditMode() {
        isEditMode = false;
        selectedTransaction = null;
        btnAdd.setText("ADD");
        btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 95; -fx-cursor: hand;");
        tblTransaction.getSelectionModel().clearSelection();
    }

    private void removeSelectedItem() {
        TempTransaction selected = tblTransaction.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.showError("Please select an item to remove");
            return;
        }

        // Confirm removal
        if (alert.showConfirmation("Remove Item", "Are you sure you want to remove '" + selected.getItemName() + "'?")) {
            try {
                // Delete from database
                tempTransactionService.deleteTransaction(selected.getId());

                // Reload transactions for this table to sync
                Integer tableNo = tableMasterService.getTableByName(txtTableNumber.getText()).getId();
                loadTransactionsForTable(tableNo);

                // Update table button status (may change to "Available" if last item removed)
                updateTableButtonStatus(tableNo);

                clearItemForm();
                LOG.info("Item removed from database: {}", selected.getItemName());
            } catch (Exception e) {
                LOG.error("Error removing item from database", e);
                alert.showError("Error removing item: " + e.getMessage());
            }
        }
    }

    private void renumberTransactions() {
        for (int i = 0; i < tempTransactionList.size(); i++) {
            tempTransactionList.get(i).setId(i + 1);
        }
        tblTransaction.refresh();
    }

    private void populateFormFromSelection(TempTransaction transaction) {
        if (transaction == null) {
            return;
        }

        txtItemName.setText(transaction.getItemName());
        txtQuantity.setText(String.valueOf(transaction.getQty().intValue()));
        txtPrice.setText(String.valueOf(transaction.getRate()));
        txtAmount.setText(String.format("%.2f", transaction.getAmt()));
    }

    private void processOrder() {
        if (tempTransactionList.isEmpty()) {
            alert.showError("No items to order. Please add items first.");
            return;
        }

        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        try {
            String tableName = txtTableNumber.getText();
            Integer tableId = tableMasterService.getTableByName(tableName).getId();

            // Get items with printQty > 0 (items that need kitchen printing)
            List<TempTransaction> printableItems = tempTransactionService.getPrintableItemsByTableNo(tableId);

            if (printableItems.isEmpty()) {
                alert.showInfo("No new items to print. All items have already been sent to kitchen.");
                return;
            }

            // Get waiter ID from first transaction
            Integer waitorId = printableItems.get(0).getWaitorId();

            LOG.info("Processing order for table: {} with {} printable items",
                    tableName, printableItems.size());

            // Print KOT to thermal printer (with dialog for printer selection)
            boolean printSuccess = kotOrderPrint.printKOTWithDialog(tableName, tableId, printableItems, waitorId);

            if (printSuccess) {
                // Reset printQty to 0 after successful print
                tempTransactionService.resetPrintQtyForTable(tableId);

                // Reload transactions to reflect updated printQty
                loadTransactionsForTable(tableId);

                alert.showInfo("KOT printed successfully! " + printableItems.size() + " items sent to kitchen.");
                LOG.info("KOT printed and printQty reset for table {}", tableName);
            } else {
                LOG.warn("KOT print cancelled or failed for table {}", tableName);
            }

        } catch (Exception e) {
            LOG.error("Error processing order", e);
            alert.showError("Error processing order: " + e.getMessage());
        }
    }

    private void updateTotals() {

        float totalQty = 0;
        float totalAmt = 0;

        for (TempTransaction t : tempTransactionList) {
            totalQty += t.getQty();
            totalAmt += t.getAmt();
        }

        if (lblTotalQuantity != null) {
            lblTotalQuantity.setText(String.format("%.0f", totalQty));
        }
        if (lblBillAmount != null) {
            lblBillAmount.setText(String.format("₹ %.2f", totalAmt));
        }

        // Recalculate payment values
        calculatePayment();
    }

    /**
     * Setup cash counter listeners for real-time calculation
     */
    private void setupCashCounter() {
        // Add listener for Cash Received field
        if (txtCashReceived != null) {
            txtCashReceived.textProperty().addListener((obs, oldVal, newVal) -> {
                // Allow only numbers and decimal point
                if (!newVal.matches("\\d*\\.?\\d*")) {
                    txtCashReceived.setText(oldVal);
                } else {
                    calculatePayment();
                }
            });
        }

        // Add listener for Return to Customer field
        if (txtReturnToCustomer != null) {
            txtReturnToCustomer.textProperty().addListener((obs, oldVal, newVal) -> {
                // Allow only numbers and decimal point
                if (!newVal.matches("\\d*\\.?\\d*")) {
                    txtReturnToCustomer.setText(oldVal);
                } else {
                    calculatePayment();
                }
            });
        }
    }

    /**
     * Calculate payment values based on bill amount, cash received, and return to customer
     *
     * Practical Logic:
     * - Change = What we SHOULD return to customer (Cash Received - Bill Amount, if positive)
     * - Balance = What customer still OWES (Bill Amount - Cash Received, if positive)
     * - Discount = Only when we return MORE than the calculated change
     *              OR when we accept less than bill (Balance forgiven at payment time)
     * - Net Amount = Actual amount we're keeping (Cash Received - Return to Customer)
     *
     * Examples:
     * 1. Bill=400, Cash=500 → Change=100, Balance=0, if Return=100 → Discount=0, Net=400
     * 2. Bill=400, Cash=500, Return=150 → Change=100, Discount=50, Net=350
     * 3. Bill=20, Cash=10 → Change=0, Balance=10, Discount=0 (customer still owes)
     * 4. Bill=500, Cash=500, Return=100 → Change=0, Discount=100, Net=400
     */
    private void calculatePayment() {
        try {
            // Get bill amount from total
            float billAmount = 0;
            for (TempTransaction t : tempTransactionList) {
                billAmount += t.getAmt();
            }

            // Get cash received
            float cashReceived = 0;
            if (txtCashReceived != null && !txtCashReceived.getText().isEmpty()) {
                cashReceived = Float.parseFloat(txtCashReceived.getText());
            }

            // Get return to customer (manual entry)
            float returnToCustomer = 0;
            if (txtReturnToCustomer != null && !txtReturnToCustomer.getText().isEmpty()) {
                returnToCustomer = Float.parseFloat(txtReturnToCustomer.getText());
            }

            // Calculate change (what we SHOULD return based on cash received)
            float change = Math.max(0, cashReceived - billAmount);

            // Calculate balance (what customer still OWES)
            float balance = Math.max(0, billAmount - cashReceived);

            // Calculate discount
            // Discount only applies when customer has paid enough (no balance)
            // and we return more than the calculated change
            float discount = 0;
            if (balance == 0 && returnToCustomer > change) {
                // Customer paid full or more, but we're returning extra
                discount = returnToCustomer - change;
            }

            // Net amount = what we're actually keeping
            // Net = Cash Received - Return to Customer
            // But cannot be more than Bill Amount (we don't keep extra)
            float netAmount = cashReceived - returnToCustomer;
            if (netAmount > billAmount) {
                netAmount = billAmount;
            }
            if (netAmount < 0) {
                netAmount = 0;
            }

            // If there's still a balance, show it; Net should reflect actual collection
            // Net Pay shows what we're collecting, Balance shows what's pending

            // Update labels
            if (lblChange != null) {
                lblChange.setText(String.format("₹ %.2f", change));
                if (change > 0) {
                    lblChange.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9C27B0;");
                } else {
                    lblChange.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #757575;");
                }
            }

            if (lblBalance != null) {
                lblBalance.setText(String.format("₹ %.2f", balance));
                if (balance > 0) {
                    lblBalance.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
                } else {
                    lblBalance.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                }
            }

            if (lblDiscount != null) {
                lblDiscount.setText(String.format("₹ %.2f", discount));
                if (discount > 0) {
                    lblDiscount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #E91E63;");
                } else {
                    lblDiscount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #757575;");
                }
            }

            if (lblNetAmount != null) {
                lblNetAmount.setText(String.format("₹ %.2f", netAmount));
            }

        } catch (NumberFormatException e) {
            // Invalid number input - ignore
            LOG.debug("Invalid number in payment calculation: {}", e.getMessage());
        }
    }

    /**
     * Clear payment/cash counter fields
     */
    private void clearPaymentFields() {
        if (txtCashReceived != null) txtCashReceived.clear();
        if (txtReturnToCustomer != null) txtReturnToCustomer.clear();
        if (lblBillAmount != null) lblBillAmount.setText("₹ 0.00");
        if (lblChange != null) lblChange.setText("₹ 0.00");
        if (lblBalance != null) lblBalance.setText("₹ 0.00");
        if (lblDiscount != null) lblDiscount.setText("₹ 0.00");
        if (lblNetAmount != null) lblNetAmount.setText("₹ 0.00");
    }

    private TempTransaction createTempTransactionFromForm() {
        TempTransaction tempTransaction = new TempTransaction();
        String itemName = txtItemName.getText();
        Float qty = Float.parseFloat(txtQuantity.getText());

        tempTransaction.setItemName(itemName);
        tempTransaction.setQty(qty);
        tempTransaction.setRate(Float.parseFloat(txtPrice.getText()));
        tempTransaction.setAmt(Float.parseFloat(txtAmount.getText()));
        tempTransaction.setTableNo(tableMasterService.getTableByName(txtTableNumber.getText()).getId());
        String selectedWaitor = cmbWaitorName.getSelectionModel().getSelectedItem();
        tempTransaction.setWaitorId(employeeService.searchByFirstName(selectedWaitor).get(0).getId());

        // Set printQty based on category stock
        // If category stock = 'N' (no stock tracking), set printQty = qty (needs to be printed for kitchen)
        // If category stock = 'Y' (has stock), set printQty = 0 (doesn't need kitchen print)
        Float printQty = calculatePrintQty(itemName, qty);
        tempTransaction.setPrintQty(printQty);

        LOG.info("Created transaction: item={}, qty={}, printQty={}", itemName, qty, printQty);
        return tempTransaction;
    }

    /**
     * Calculate printQty based on item's category stock setting
     * If category stock = 'N', item needs to be printed (printQty = qty)
     * If category stock = 'Y', item doesn't need printing (printQty = 0)
     */
    private Float calculatePrintQty(String itemName, Float qty) {
        try {
            // Get the item with its category
            Item item = itemService.getItemByName(itemName).orElse(null);
            if (item == null) {
                LOG.warn("Item not found: {}, defaulting printQty to qty", itemName);
                return qty;
            }

            // Get category to check stock setting
            Integer categoryId = item.getCategoryId();
            if (categoryId != null) {
                CategoryMasterDto category = categoryApiService.getCategoryById(categoryId);
                if (category != null && "N".equalsIgnoreCase(category.getStock())) {
                    // Stock = 'N' means no stock tracking, needs to be printed for kitchen
                    LOG.debug("Category '{}' has stock='N', setting printQty={}", category.getCategory(), qty);
                    return qty;
                } else {
                    // Stock = 'Y' or other means has stock, doesn't need kitchen print
                    LOG.debug("Category '{}' has stock='{}', setting printQty=0",
                            category != null ? category.getCategory() : "unknown",
                            category != null ? category.getStock() : "null");
                    return 0f;
                }
            }
        } catch (Exception e) {
            LOG.error("Error calculating printQty for item: {}", itemName, e);
        }

        // Default to qty if unable to determine
        return qty;
    }

    private void addTempTransactionInTableView(TempTransaction tempTransaction) {
        if (tblTransaction.getItems().isEmpty()) {
            tempTransaction.setId(1);
            tempTransactionList.add(tempTransaction);
            LOG.info("1st TempTransaction added: {}", tempTransaction);
        } else {
            LOG.info("Checking existing transactions: {}", tempTransaction);
            boolean foundMatch = false;

            for (int i = 0; i < tempTransactionList.size(); i++) {
                TempTransaction oldTransaction = tempTransactionList.get(i);
                if (oldTransaction.getItemName().equals(tempTransaction.getItemName())
                        && Float.compare(oldTransaction.getRate(), tempTransaction.getRate()) == 0) {
                    System.out.println("Match found at index " + i);

                    oldTransaction.setQty(oldTransaction.getQty() + tempTransaction.getQty());
                    oldTransaction.setAmt(oldTransaction.getAmt() + tempTransaction.getAmt());

                    // Trigger TableView update (if using properties) or refresh
                    tblTransaction.refresh();
                    foundMatch = true;
                    break; // Use break instead of return to stay in method
                }
            }

            if (!foundMatch) {
                tempTransaction.setId(tempTransactionList.size() + 1);
                tempTransactionList.add(tempTransaction);
                LOG.info("New item added: {}", tempTransaction);
                tblTransaction.refresh();
            }
        }
    }

    private boolean validate() {
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Select Table First");
            return false;
        }
        if (cmbWaitorName.getSelectionModel().isEmpty()) {
            alert.showError("Select Waitor First");
            cmbWaitorName.requestFocus();
            return false;
        }
        if (txtItemName.getText().isEmpty()) {
            txtItemName.requestFocus();
            alert.showError("Enter Item Name");
            return false;
        }
        if (txtQuantity.getText().isEmpty()) {
            alert.showError("Enter Quantity");
            txtQuantity.requestFocus();
            return false;
        }
        if (txtPrice.getText().isEmpty()) {
            alert.showError("Enter Rate");
            txtPrice.requestFocus();
            return false;
        }

        return true;
    }

    // ============= Bill Action Button Methods =============

    /**
     * Close the current table order
     * - If closed bill exists and new temp_transactions: add new items to existing bill
     * - If no closed bill: create new bill with CLOSE status
     */
    private void closeTable() {
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        if (tempTransactionList.isEmpty()) {
            alert.showInfo("No items to close");
            return;
        }

        try {
            String tableName = txtTableNumber.getText();
            Integer tableId = tableMasterService.getTableByName(tableName).getId();

            // Get new temp transactions (items with positive IDs are from temp_transaction table)
            List<TempTransaction> newTempTransactions = tempTransactionService.getTransactionsByTableNo(tableId);

            // Check if we have an existing closed bill
            boolean hasClosedBill = currentClosedBill != null;
            boolean hasNewItems = !newTempTransactions.isEmpty();

            // If only closed bill items and no new items, nothing to add
            if (hasClosedBill && !hasNewItems) {
                alert.showInfo("No new items to add.\nClosed Bill #" + currentClosedBill.getBillNo() + " already exists for this table.");
                return;
            }

            // Build confirmation message
            String confirmMessage;
            if (hasClosedBill && hasNewItems) {
                confirmMessage = "This will ADD " + newTempTransactions.size() + " new item(s) to existing CLOSED Bill #" +
                        currentClosedBill.getBillNo() + "\n\nExisting Bill: ₹" + String.format("%.2f", currentClosedBill.getBillAmt()) +
                        "\nNew Items: ₹" + String.format("%.2f", calculateTempTransactionsTotal(newTempTransactions)) +
                        "\n\nProceed?";
            } else {
                confirmMessage = "This will save the bill as CLOSED and clear the table.\nBill Amount: " + lblBillAmount.getText() + "\n\nProceed?";
            }

            // Confirm close
            String confirmTitle = hasClosedBill ? "Add Items to Existing CLOSED Bill" : "Close Table - Save Bill";
            if (alert.showConfirmation(confirmTitle, confirmMessage)) {
                try {
                    Bill savedBill;

                    if (hasClosedBill && hasNewItems) {
                        // Add new items to existing closed bill
                        savedBill = billService.addTransactionsToClosedBill(tableId, newTempTransactions);
                        LOG.info("Added {} new items to closed bill #{}", newTempTransactions.size(), savedBill.getBillNo());
                    } else {
                        // Create new closed bill
                        // Get waiter ID from combo box
                        Integer waitorId = null;
                        String selectedWaitor = cmbWaitorName.getSelectionModel().getSelectedItem();
                        if (selectedWaitor != null && !selectedWaitor.isEmpty()) {
                            List<Employee> waiters = employeeService.searchByFirstName(selectedWaitor);
                            if (!waiters.isEmpty()) {
                                waitorId = waiters.get(0).getId();
                            }
                        }

                        // Get customer ID if selected
                        Integer customerId = null;
                        if (selectedCustomer != null) {
                            customerId = selectedCustomer.getId();
                        }

                        // Get current user ID from session
                        Long userIdLong = SessionService.getCurrentUserId();
                        Integer userId = userIdLong != null ? userIdLong.intValue() : null;

                        savedBill = billService.createClosedBill(tableId, customerId, waitorId, userId, newTempTransactions);
                        LOG.info("Created new closed bill #{} with {} items", savedBill.getBillNo(), newTempTransactions.size());
                    }

                    // Print the bill
                    final Bill billToPrint = savedBill;
                    final String tableNameForPrint = tableName;
                    new Thread(() -> {
                        try {
                            billPrint.printBill(billToPrint, tableNameForPrint);
                        } catch (Exception e) {
                            LOG.error("Error printing bill: {}", e.getMessage(), e);
                        }
                    }).start();

                    // Clear TableView and reset
                    tempTransactionList.clear();
                    currentClosedBill = null;
                    updateTotals();

                    // Update table button status
                    updateTableButtonStatus(tableId);

                    // Clear form
                    clearItemForm();
                    clearPaymentFields();
                    clearSelectedCustomer();
                    cmbWaitorName.getSelectionModel().clearSelection();
                    txtTableNumber.clear();

                    alert.showInfo("Bill #" + savedBill.getBillNo() + " saved as CLOSED\nTotal Amount: ₹" + String.format("%.2f", savedBill.getBillAmt()));
                    LOG.info("Table {} closed. Bill #{} saved as CLOSED with amount {}", tableName, savedBill.getBillNo(), savedBill.getBillAmt());

                } catch (Exception e) {
                    LOG.error("Error closing table", e);
                    alert.showError("Error closing table: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("Error preparing to close table", e);
            alert.showError("Error: " + e.getMessage());
        }
    }

    /**
     * Calculate total amount of temp transactions
     */
    private float calculateTempTransactionsTotal(List<TempTransaction> transactions) {
        float total = 0f;
        for (TempTransaction t : transactions) {
            total += t.getAmt();
        }
        return total;
    }

    /**
     * Mark the current order as PAID
     * TODO: Generate bill, save to transaction history, clear temp_transaction
     */
    private void markAsPaid() {
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        if (tempTransactionList.isEmpty()) {
            alert.showError("No items to bill");
            return;
        }

        LOG.info("PAID button clicked for table: {}", txtTableNumber.getText());
        // TODO: Implement full payment processing
        // 1. Generate bill number
        // 2. Save to transaction/bill table
        // 3. Print bill
        // 4. Clear temp_transaction
        // 5. Update table status
        alert.showInfo("Payment processing - TODO: Implement bill generation");
    }

    /**
     * Mark the current order as CREDIT
     * TODO: Save as credit bill for selected customer
     */
    private void markAsCredit() {
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        if (tempTransactionList.isEmpty()) {
            alert.showError("No items to bill");
            return;
        }

        if (selectedCustomer == null) {
            alert.showError("Please select a customer for credit billing");
            txtCustomerSearch.requestFocus();
            return;
        }

        LOG.info("CREDIT button clicked for table: {}, customer: {}",
                txtTableNumber.getText(), selectedCustomer.getFullName());
        // TODO: Implement credit billing
        // 1. Generate bill number
        // 2. Save to transaction/bill table with credit flag
        // 3. Update customer credit balance
        // 4. Print bill
        // 5. Clear temp_transaction
        alert.showInfo("Credit billing - TODO: Implement credit processing for " + selectedCustomer.getFullName());
    }

    /**
     * Show old/previous bills
     * TODO: Open dialog to search and view old bills
     */
    private void showOldBills() {
        LOG.info("OLD BILL button clicked");
        // TODO: Implement old bill viewer
        // 1. Open dialog/window
        // 2. Search by date, bill number, customer
        // 3. Display bill details
        // 4. Option to reprint
        alert.showInfo("Old Bills - TODO: Implement bill history viewer");
    }

    /**
     * Edit an existing bill
     * TODO: Load bill for editing
     */
    private void editBill() {
        LOG.info("EDIT BILL button clicked");
        // TODO: Implement bill editing
        // 1. Search and select bill to edit
        // 2. Load bill items
        // 3. Allow modifications
        // 4. Save changes
        alert.showInfo("Edit Bill - TODO: Implement bill editing");
    }

}
