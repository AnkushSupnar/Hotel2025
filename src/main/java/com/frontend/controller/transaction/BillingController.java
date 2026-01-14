package com.frontend.controller.transaction;

import com.frontend.common.CommonMethod;
import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.customUI.AutoCompleteTextField_old;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.entity.CategoryMaster;
import com.frontend.entity.Customer;
import com.frontend.entity.Employees;
import com.frontend.entity.Item;
import com.frontend.entity.TableMaster;
import com.frontend.entity.TempTransaction;
import com.frontend.service.BillService;
import com.frontend.service.CategoryApiService;
import com.frontend.service.CustomerService;
import com.frontend.service.EmployeesService;
import com.frontend.service.ItemService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import com.frontend.service.TempTransactionService;
import com.frontend.entity.Bank;
import com.frontend.entity.Bill;
import com.frontend.entity.Transaction;
import com.frontend.print.BillPrint;
import com.frontend.service.BankService;
import com.frontend.service.BankTransactionService;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
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
    private EmployeesService employeesService;

    @Autowired
    private TempTransactionService tempTransactionService;

    @Autowired
    private BillService billService;

    @Autowired
    private KOTOrderPrint kotOrderPrint;

    @Autowired
    private BillPrint billPrint;

    @Autowired
    private BankService bankService;

    @Autowired
    private BankTransactionService bankTransactionService;

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
    private Button btnShiftTable;

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

    // Payment Mode ComboBox (CASH + Banks)
    @FXML
    private ComboBox<PaymentOption> cmbPaymentMode;

    /**
     * Inner class to represent payment options (Cash bank or other Banks)
     * Cash is treated as a bank with IFSC code "cash" for unified transaction tracking
     */
    public static class PaymentOption {
        private final String displayName;
        private final Bank bank;
        private static final String CASH_IFSC = "cash";

        public PaymentOption(String displayName, Bank bank) {
            this.displayName = displayName;
            this.bank = bank;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Bank getBank() {
            return bank;
        }

        /**
         * Check if this is a cash payment (bank with IFSC code "cash")
         */
        public boolean isCash() {
            return bank != null && CASH_IFSC.equalsIgnoreCase(bank.getIfsc());
        }

        /**
         * Check if this is a regular bank payment (not cash)
         */
        public boolean isBank() {
            return bank != null && !CASH_IFSC.equalsIgnoreCase(bank.getIfsc());
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Bill History Search Fields
    @FXML
    private DatePicker dpSearchDate;

    @FXML
    private TextField txtSearchBillNo;

    @FXML
    private TextField txtSearchCustomer;

    @FXML
    private HBox billHistoryCustomerSearchContainer;

    @FXML
    private Button btnSearchBills;

    @FXML
    private Button btnClearSearch;

    @FXML
    private Button btnRefreshBills;

    // AutoComplete for bill history customer search
    private AutoCompleteTextField billHistoryCustomerAutoComplete;

    @FXML
    private TableView<Bill> tblBillHistory;

    @FXML
    private TableColumn<Bill, Integer> colBillNo;

    @FXML
    private TableColumn<Bill, String> colBillDate;

    @FXML
    private TableColumn<Bill, Integer> colBillCustomer;

    @FXML
    private TableColumn<Bill, Float> colBillAmount;

    @FXML
    private TableColumn<Bill, String> colBillStatus;

    @FXML
    private Label lblTotalCash;

    @FXML
    private Label lblTotalCredit;

    @FXML
    private Label lblTotalBills;

    // Bill history data
    private ObservableList<Bill> billHistoryList = FXCollections.observableArrayList();

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

    // Shift table mode tracking
    private boolean isShiftTableMode = false;
    private Integer shiftSourceTableId = null;
    private String shiftSourceTableName = null;

    // Edit bill mode tracking
    private boolean isEditBillMode = false;
    private Bill billBeingEdited = null;

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
        setupPaymentMode();
        setupBillHistory();
    }

    private void setupActionButtons() {
        btnAdd.setOnAction(e -> add());
        btnEdit.setOnAction(e -> editSelectedItem());
        btnRemove.setOnAction(e -> removeSelectedItem());
        btnClear.setOnAction(e -> clearItemForm());
        btnOrder.setOnAction(e -> processOrder());

        // Bill Action Buttons
        btnClose.setOnAction(e -> {
            if (isEditBillMode) {
                saveEditedBill();
            } else {
                closeTable();
            }
        });
        btnPaid.setOnAction(e -> markAsPaid());
        btnShiftTable.setOnAction(e -> {
            if (isShiftTableMode) {
                cancelShiftTableMode();
            } else {
                startShiftTableMode();
            }
        });
        btnOldBill.setOnAction(e -> showOldBills());
        btnEditBill.setOnAction(e -> {
            if (isEditBillMode) {
                cancelEditBillMode();
            } else {
                editBill();
            }
        });

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

    /**
     * Refresh all table buttons - reloads sections and updates table statuses
     * Called when user clicks the Refresh button
     */
    private void refreshTables() {
        LOG.info("Refreshing tables...");

        // Disable button during refresh to prevent multiple clicks
        btnRefreshTables.setDisable(true);
        btnRefreshTables.setText("⏳");

        // Clear the table button map (will be repopulated in loadSections)
        tableButtonMap.clear();

        // Reload sections with table buttons
        loadSections();

        // Re-enable button after a short delay
        Platform.runLater(() -> {
            btnRefreshTables.setDisable(false);
            btnRefreshTables.setText("🔄 REFRESH");
            LOG.info("Tables refreshed successfully");
        });
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

                // Apply Kiran font to Item Name field (for Marathi typing)
                applyFontToTextField(txtItemName, kiranFont25, fontStyle);

                // Apply English font (18px plain) to numeric/code fields
                Font englishFont18 = Font.font("System", 18.0);
                String englishFontStyle = "-fx-font-family: 'System'; -fx-font-size: 18px;";
                applyFontToTextField(txtCode, englishFont18, englishFontStyle);
                applyFontToTextField(txtQuantity, englishFont18, englishFontStyle);
                applyFontToTextField(txtPrice, englishFont18, englishFontStyle);
                applyFontToTextField(txtAmount, englishFont18, englishFontStyle);

                // Set English prompt text for numeric/code fields
                txtCode.setPromptText("Code");
                txtQuantity.setPromptText("Quantity");
                txtPrice.setPromptText("Rate");
                txtAmount.setPromptText("Amount");

                LOG.info("Kiran font applied to category/item fields, English font (18px) applied to code/numeric fields");
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
                    // On focus lost, load items if category is selected
                    if (!newValue && !txtCategoryName.getText().isEmpty()) {
                        loadItemsByCategory(txtCategoryName.getText());
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

            // Move focus to category field when dropdown closes (after selection is complete)
            // Using onHidden instead of selectedItemProperty to allow arrow key navigation in dropdown
            cmbWaitorName.setOnHidden(event -> {
                String selectedWaitor = cmbWaitorName.getSelectionModel().getSelectedItem();
                if (selectedWaitor != null && !selectedWaitor.isEmpty()) {
                    Platform.runLater(() -> txtCategoryName.requestFocus());
                }
            });

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

        // Allow numeric input with optional minus sign and decimal point for quantity
        // Pattern: optional minus sign followed by digits with optional decimal (e.g., "5", "1.5", "-2.5")
        txtQuantity.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*\\.?\\d*")) {
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
        allWaitorNames = employeesService.getWaiterNames();
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
        //String itemName = txtItemName.getText().trim();
        String itemName = txtItemName.getText();

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

        // Skip calculation for empty or incomplete input (e.g., "-", ".", "-.")
        if (itemName.isEmpty() || quantityText.isEmpty() ||
            quantityText.equals("-") || quantityText.equals(".") || quantityText.equals("-.")) {
            return;
        }

        try {
            float quantity = Float.parseFloat(quantityText);

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

            // Allow negative quantity for reducing existing items
            // Validation for negative qty is done in validate() method
            if (quantity == 0) {
                showAlert("Quantity cannot be zero");
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
        txtCategoryName.clear();
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
     * Load all tables and display in a grid with section separators
     */
    private void loadSections() {
        try {
            LOG.info("Loading all tables into grid with section separators");

            // Main container for all sections
            VBox mainContainer = new VBox();
            mainContainer.setSpacing(8);
            mainContainer.setStyle("-fx-background-color: transparent;");

            // Get all sections and their tables
            List<String> sections = tableMasterService.getUniqueDescriptions();
            int totalTables = 0;

            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i);
                List<TableMaster> tables = tableMasterService.getTablesByDescription(section);

                if (tables.isEmpty()) continue;

                // Create TilePane for this section's tables
                TilePane tilePane = new TilePane();
                tilePane.setHgap(5);
                tilePane.setVgap(5);
                tilePane.setPrefColumns(7); // 7 columns for wider buttons
                tilePane.setTileAlignment(Pos.CENTER);
                tilePane.setAlignment(Pos.TOP_LEFT);
                tilePane.setStyle("-fx-background-color: transparent; -fx-padding: 2 0;");

                for (TableMaster table : tables) {
                    Button tableButton = createTableButton(table);
                    tilePane.getChildren().add(tableButton);

                    // Set up click handler
                    tableButton.setOnAction(e -> {
                        // Block table selection during edit bill mode
                        if (isEditBillMode) {
                            alert.showWarning("Cannot change table while editing a bill. Save or cancel first.");
                            return;
                        }

                        // Check if we are in shift table mode
                        if (isShiftTableMode) {
                            handleShiftTableTarget(table.getId(), table.getTableName());
                            return;
                        }

                        txtTableNumber.setText(table.getTableName());

                        // Check if table has any existing transactions before loading
                        boolean hasExistingTransactions = tempTransactionService.hasTransactions(table.getId())
                                || billService.hasClosedBill(table.getId());

                        // Load existing transactions for this table from database
                        loadTransactionsForTable(table.getId());
                        LOG.info("Table selected: {} (ID: {})", table.getTableName(), table.getId());

                        // If table is fresh (no transactions), focus on waiter dropdown
                        // If table has existing transactions, focus on category field for faster data entry
                        if (!hasExistingTransactions) {
                            Platform.runLater(() -> {
                                cmbWaitorName.requestFocus();
                                cmbWaitorName.show();
                            });
                        } else {
                            Platform.runLater(() -> txtCategoryName.requestFocus());
                        }
                    });
                    totalTables++;
                }

                mainContainer.getChildren().add(tilePane);

                // Add separator line between sections (not after the last one)
                if (i < sections.size() - 1) {
                    Region separator = new Region();
                    separator.setMinHeight(1);
                    separator.setMaxHeight(1);
                    separator.setStyle("-fx-background-color: #E0E0E0;");
                    mainContainer.getChildren().add(separator);
                }
            }

            final int count = totalTables;
            Platform.runLater(() -> {
                sectionsContainer.getChildren().clear();
                sectionsContainer.getChildren().add(mainContainer);
                LOG.info("Loaded {} tables in {} sections", count, sections.size());
            });

        } catch (Exception e) {
            LOG.error("Error loading tables", e);
        }
    }

    /**
     * Create a styled box for a section with drag and drop support - Legacy method kept for compatibility
     */
    private VBox createSectionBox(String sectionName) {
        // This method is now unused but kept for potential future use
        VBox box = new VBox();
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
     * States: Open (Available), Running (Ongoing), Closed
     */
    private void applyTableButtonStatus(Button button, String status) {
        // Remove all status classes first
        button.getStyleClass().removeAll(
            "table-button-available",
            "table-button-ongoing",
            "table-button-closed"
        );

        // Apply new status class
        switch (status) {
            case "Ongoing":  // Running - has items in temp_transaction (Green)
                button.getStyleClass().add("table-button-ongoing");
                break;
            case "Closed":   // Closed - has closed bill (Red)
                button.getStyleClass().add("table-button-closed");
                break;
            case "Available": // Open - no items (White/No color)
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
            if (hasTempTransactions) {
                // Temp transactions mean table is active/open (even if it has a closed bill)
                newStatus = "Ongoing";
            } else if (hasClosedBill) {
                newStatus = "Closed";
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
        // Store original style for reset
        String originalStyle = "-fx-background-color: #FFFFFF; -fx-background-radius: 6; " +
                              "-fx-border-radius: 6; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);";
        String dragOverStyle = "-fx-background-color: #E3F2FD; -fx-background-radius: 6; " +
                              "-fx-border-radius: 6; -fx-effect: dropshadow(gaussian, rgba(25,118,210,0.2), 6, 0, 0, 2);";

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
                box.setStyle(dragOverStyle);
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
                draggedBox.setStyle(originalStyle);
                draggedBox = null;
            }
            event.consume();
        });
    }

    private void add() {
        // Calculate amount first before validation (in case user didn't lose focus from quantity)
        calculateAndSetAmount();

        if (!validate()) {
            return;
        }

        if (isEditBillMode) {
            // Edit bill mode - work with observable list only, no database operations
            if (isEditMode && selectedTransaction != null) {
                // Update existing item in observable list
                updateSelectedTransactionInList();
            } else {
                // Add new item to observable list
                addItemToListOnly();
            }
        } else if (isEditMode && selectedTransaction != null) {
            // Normal mode - Update existing transaction in database
            updateSelectedTransaction();
        } else {
            // Normal mode - Add new transaction to database and TableView
            TempTransaction tempTransaction = createTempTransactionFromForm();
            addTempTransactionToDatabase(tempTransaction);
        }

        // Clear form and update totals
        clearItemForm();
        updateTotals();
        txtCategoryName.requestFocus();
    }

    /**
     * Add item to observable list only (for edit bill mode)
     * Does not save to database - that happens on SAVE
     * Supports negative quantity to reduce existing item quantity
     */
    private void addItemToListOnly() {
        try {
            String itemName = txtItemName.getText().trim();
            float qty = Float.parseFloat(txtQuantity.getText().trim());
            float rate = Float.parseFloat(txtPrice.getText().trim());

            // Check if item with same name and rate already exists in the list
            TempTransaction existing = null;
            int existingIndex = -1;
            for (int i = 0; i < tempTransactionList.size(); i++) {
                TempTransaction t = tempTransactionList.get(i);
                if (t.getItemName().equals(itemName) && t.getRate().equals(rate)) {
                    existing = t;
                    existingIndex = i;
                    break;
                }
            }

            if (existing != null) {
                // Update existing item - add quantity (handles negative qty for reduction)
                float newQty = existing.getQty() + qty;

                if (newQty <= 0) {
                    // Remove item from list if quantity becomes 0 or negative
                    tempTransactionList.remove(existingIndex);
                    tblTransaction.refresh();
                    LOG.info("Removed item from list (qty became {}): {}", newQty, itemName);
                } else {
                    existing.setQty(newQty);
                    existing.setAmt(newQty * existing.getRate());
                    tblTransaction.refresh();
                    LOG.info("Updated existing item in list: {} qty={}", itemName, newQty);
                }
            } else {
                // Don't allow adding negative quantity for non-existing item
                if (qty < 0) {
                    alert.showError("Cannot reduce quantity. Item not found in the order.");
                    return;
                }
                // Add new item to list
                float amt = qty * rate;
                TempTransaction newTrans = new TempTransaction();
                newTrans.setId(tempTransactionList.size() + 1); // Temporary ID for display
                newTrans.setItemName(itemName);
                newTrans.setQty(qty);
                newTrans.setRate(rate);
                newTrans.setAmt(amt);
                newTrans.setTableNo(billBeingEdited != null ? billBeingEdited.getTableNo() : 0);
                newTrans.setPrintQty(0f);

                tempTransactionList.add(newTrans);
                LOG.info("Added new item to list: {} x {} @ {} = {}", qty, itemName, rate, amt);
            }
        } catch (NumberFormatException e) {
            alert.showError("Invalid quantity or price");
        }
    }

    /**
     * Update selected transaction in observable list only (for edit bill mode)
     * Does not save to database - that happens on SAVE
     */
    private void updateSelectedTransactionInList() {
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

            tblTransaction.refresh();
            LOG.info("Updated item in list: {} qty={}, rate={}, amt={}",
                    selectedTransaction.getItemName(), newQty, newRate, newAmt);

            // Reset edit mode
            resetEditMode();
        } catch (NumberFormatException e) {
            alert.showError("Invalid quantity or price");
        }
    }

    /**
     * Add transaction to database and sync with TableView
     * If item exists with same name and rate, updates quantity instead
     * Tracks reduced kitchen items when quantity is negative
     */
    private void addTempTransactionToDatabase(TempTransaction transaction) {
        try {
            Integer tableNo = transaction.getTableNo();

            // Get current user info for tracking
            Long userId = SessionService.getCurrentUserId();
            String userName = SessionService.getCurrentUsername();

            // Save to database - use tracking method for negative quantities
            TempTransaction savedTransaction;
            if (transaction.getQty() < 0) {
                // Negative quantity - use tracking method to track kitchen item reductions
                savedTransaction = tempTransactionService.addOrUpdateTransactionWithTracking(
                        transaction,
                        userId != null ? userId.intValue() : null,
                        userName
                );
            } else {
                // Positive quantity - use normal method
                savedTransaction = tempTransactionService.addOrUpdateTransaction(transaction);
            }

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
                        Employees waitor = employeesService.getEmployeeById(currentClosedBill.getWaitorId());
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
                        Employees waitor = employeesService.getEmployeeById(waitorId);
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

            // Update Close button state
            // Disable if there's a closed bill but no new temp_transactions
            updateCloseButtonState(currentClosedBill != null, tempTransactions.isEmpty());

            LOG.info("Total {} items loaded for table {} (closed bill: {}, new: {})",
                    tempTransactionList.size(), tableNo,
                    currentClosedBill != null ? currentClosedBill.getBillNo() : "none",
                    tempTransactions.size());
        } catch (Exception e) {
            LOG.error("Error loading transactions for table {}", tableNo, e);
        }
    }

    /**
     * Update Close button state based on table status
     * Disable if table has closed bill but no new items to add
     */
    private void updateCloseButtonState(boolean hasClosedBill, boolean noNewItems) {
        if (hasClosedBill && noNewItems) {
            // Table already closed with no new items - disable Close button
            btnClose.setDisable(true);
            btnClose.setStyle("-fx-background-color: #BDBDBD; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: default;");
        } else {
            // Enable Close button
            btnClose.setDisable(false);
            btnClose.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
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
                if (isEditBillMode) {
                    // Edit bill mode - just remove from observable list
                    tempTransactionList.remove(selected);
                    updateTotals();
                    clearItemForm();
                    LOG.info("Item removed from list (edit bill mode): {}", selected.getItemName());
                } else {
                    // Normal mode - delete from database with tracking
                    // Get current user info for tracking
                    Long userId = SessionService.getCurrentUserId();
                    String userName = SessionService.getCurrentUsername();

                    // Use tracking method to track kitchen item removals
                    tempTransactionService.removeTransactionWithTracking(
                            selected.getId(),
                            userId != null ? userId.intValue() : null,
                            userName
                    );

                    // Reload transactions for this table to sync
                    Integer tableNo = tableMasterService.getTableByName(txtTableNumber.getText()).getId();
                    loadTransactionsForTable(tableNo);

                    // Update table button status (may change to "Available" if last item removed)
                    updateTableButtonStatus(tableNo);

                    clearItemForm();
                    LOG.info("Item removed from database: {}", selected.getItemName());
                }
            } catch (Exception e) {
                LOG.error("Error removing item", e);
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
        // Display quantity with decimals if needed, otherwise show as integer
        float qty = transaction.getQty();
        txtQuantity.setText(qty == Math.floor(qty) ? String.valueOf((int) qty) : String.valueOf(qty));
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
            kotOrderPrint.clearLastPrintError();
            boolean printSuccess = kotOrderPrint.printKOTWithDialog(tableName, tableId, printableItems, waitorId);

            if (printSuccess) {
                // Reset printQty to 0 after successful print
                tempTransactionService.resetPrintQtyForTable(tableId);

                // Reload transactions to reflect updated printQty
                loadTransactionsForTable(tableId);

                alert.showInfo("KOT printed successfully! " + printableItems.size() + " items sent to kitchen.");
                LOG.info("KOT printed and printQty reset for table {}", tableName);
            } else {
                // Check if there was an error (not just user cancellation)
                String printError = kotOrderPrint.getLastPrintError();
                if (printError != null && !printError.isEmpty()) {
                    alert.showError("Print failed: " + printError);
                    LOG.error("KOT print failed for table {}: {}", tableName, printError);
                } else {
                    LOG.info("KOT print cancelled by user for table {}", tableName);
                }
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
     * Setup payment mode dropdown with all banks (including cash bank with IFSC "cash")
     */
    private void setupPaymentMode() {
        if (cmbPaymentMode == null) {
            LOG.warn("Payment mode combo box not initialized");
            return;
        }

        try {
            // Get custom font for bank names (18px to match other places)
            Font customFont = SessionService.getCustomFont(18.0);
            final String fontFamily = customFont != null ? customFont.getFamily() : null;

            // Create payment options list
            ObservableList<PaymentOption> paymentOptions = FXCollections.observableArrayList();

            // Add all active banks (including cash bank with IFSC "cash")
            List<Bank> banks = bankService.getActiveBanks();
            PaymentOption cashBankOption = null;

            for (Bank bank : banks) {
                PaymentOption option = new PaymentOption(bank.getBankName(), bank);
                paymentOptions.add(option);
                // Track the cash bank option (IFSC = "cash") to select it by default
                if ("cash".equalsIgnoreCase(bank.getIfsc())) {
                    cashBankOption = option;
                }
            }

            cmbPaymentMode.setItems(paymentOptions);

            // Custom cell factory for dropdown list with custom font and readable colors
            cmbPaymentMode.setCellFactory(param -> new ListCell<PaymentOption>() {
                @Override
                protected void updateItem(PaymentOption item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        setText(item.getDisplayName());
                        // Style with readable colors - green for cash, blue for banks
                        if (item.isCash()) {
                            // Cash bank - green color with custom font
                            if (fontFamily != null) {
                                setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-font-size: 18px; -fx-background-color: white; -fx-padding: 5 10;");
                            } else {
                                setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-font-size: 18px; -fx-background-color: white; -fx-padding: 5 10;");
                            }
                        } else {
                            // Other banks - blue color with custom font
                            if (fontFamily != null) {
                                setStyle("-fx-font-family: '" + fontFamily + "'; -fx-text-fill: #1565C0; -fx-font-size: 18px; -fx-background-color: white; -fx-padding: 5 10;");
                            } else {
                                setStyle("-fx-text-fill: #1565C0; -fx-font-size: 18px; -fx-background-color: white; -fx-padding: 5 10;");
                            }
                        }
                    }
                }
            });

            // Button cell (what shows when dropdown is closed) with custom font
            cmbPaymentMode.setButtonCell(new ListCell<PaymentOption>() {
                @Override
                protected void updateItem(PaymentOption item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                        setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        setText(item.getDisplayName());
                        // Apply custom font for all bank names in button cell (18px)
                        if (fontFamily != null) {
                            setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
                        }
                    }
                }
            });

            // Select cash bank by default, otherwise first option
            if (cashBankOption != null) {
                cmbPaymentMode.getSelectionModel().select(cashBankOption);
                LOG.info("Selected cash bank as default payment mode");
            } else if (!paymentOptions.isEmpty()) {
                cmbPaymentMode.getSelectionModel().selectFirst();
                LOG.warn("Cash bank (IFSC='cash') not found, selecting first bank as default");
            }

            LOG.info("Payment mode dropdown setup with {} banks (custom font: {})", banks.size(), fontFamily);

        } catch (Exception e) {
            LOG.error("Error setting up payment mode: ", e);
        }
    }

    /**
     * Get the currently selected payment option
     */
    private PaymentOption getSelectedPaymentOption() {
        if (cmbPaymentMode != null && cmbPaymentMode.getValue() != null) {
            return cmbPaymentMode.getValue();
        }
        // Return null if nothing selected - caller should handle this
        return null;
    }

    // ============= Bill History Methods =============

    /**
     * Setup bill history table and search functionality
     */
    private void setupBillHistory() {
        try {
            // Get custom font for bill history table (size 18 for compact display)
            Font customFont = SessionService.getCustomFont(18.0);
            final String fontFamily = customFont != null ? customFont.getFamily() : null;

            // Setup table columns
            if (colBillNo != null) {
                colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNo"));
            }
            if (colBillDate != null) {
                colBillDate.setCellValueFactory(new PropertyValueFactory<>("billDate"));
            }
            if (colBillAmount != null) {
                colBillAmount.setCellValueFactory(new PropertyValueFactory<>("billAmt"));
                // Format amount with ₹ symbol
                colBillAmount.setCellFactory(column -> new TableCell<Bill, Float>() {
                    @Override
                    protected void updateItem(Float item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(String.format("₹%.0f", item));
                        }
                    }
                });
            }
            if (colBillStatus != null) {
                colBillStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
                // Color code status
                colBillStatus.setCellFactory(column -> new TableCell<Bill, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            if ("PAID".equalsIgnoreCase(item)) {
                                setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            } else if ("CREDIT".equalsIgnoreCase(item)) {
                                setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #757575;");
                            }
                        }
                    }
                });
            }
            if (colBillCustomer != null) {
                // Custom cell factory to show customer name with custom font
                colBillCustomer.setCellValueFactory(new PropertyValueFactory<>("customerId"));
                colBillCustomer.setCellFactory(column -> new TableCell<Bill, Integer>() {
                    @Override
                    protected void updateItem(Integer customerId, boolean empty) {
                        super.updateItem(customerId, empty);
                        if (empty) {
                            setText(null);
                            setStyle("");
                        } else if (customerId != null) {
                            Customer customer = customerService.getCustomerById(customerId);
                            String customerName = customer != null ? customer.getFullName() : "";
                            setText(customerName);
                            // Apply custom font for customer name
                            if (fontFamily != null && !customerName.isEmpty()) {
                                setFont(customFont);
                                setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: 18px;", fontFamily));
                            }
                        } else {
                            // Cash payment - no customer, show empty
                            setText("");
                            setStyle("");
                        }
                    }
                });
            }

            // Bind table to data
            if (tblBillHistory != null) {
                tblBillHistory.setItems(billHistoryList);

                // Double-click to view bill details
                tblBillHistory.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        Bill selectedBill = tblBillHistory.getSelectionModel().getSelectedItem();
                        if (selectedBill != null) {
                            viewBillDetails(selectedBill);
                        }
                    }
                });
            }

            // Setup search buttons
            if (btnSearchBills != null) {
                btnSearchBills.setOnAction(e -> searchBills());
            }
            if (btnClearSearch != null) {
                btnClearSearch.setOnAction(e -> clearBillSearch());
            }
            if (btnRefreshBills != null) {
                btnRefreshBills.setOnAction(e -> loadTodaysBills());
            }

            // Set today's date in DatePicker
            if (dpSearchDate != null) {
                dpSearchDate.setValue(java.time.LocalDate.now());
            }

            // Setup AutoComplete for bill history customer search
            setupBillHistoryCustomerAutoComplete();

            // Load today's bills
            loadTodaysBills();

            LOG.info("Bill history setup completed");

        } catch (Exception e) {
            LOG.error("Error setting up bill history", e);
        }
    }

    /**
     * Load today's bills (PAID and CREDIT) ordered by bill number ascending
     */
    private void loadTodaysBills() {
        try {
            String today = java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            // Get all bills for today with PAID or CREDIT status ordered by bill number ascending
            List<Bill> paidBills = billService.getBillsByStatusAndDateOrderByBillNoAsc("PAID", today);
            List<Bill> creditBills = billService.getBillsByStatusAndDateOrderByBillNoAsc("CREDIT", today);

            billHistoryList.clear();
            billHistoryList.addAll(paidBills);
            billHistoryList.addAll(creditBills);

            // Sort combined list by bill number ascending
            billHistoryList.sort((b1, b2) -> b1.getBillNo().compareTo(b2.getBillNo()));

            // Update summary totals
            updateBillSummary(paidBills, creditBills);

            LOG.info("Loaded {} PAID and {} CREDIT bills for today", paidBills.size(), creditBills.size());

        } catch (Exception e) {
            LOG.error("Error loading today's bills", e);
        }
    }

    /**
     * Search bills by date, bill no, or customer name
     */
    private void searchBills() {
        try {
            // Get date from DatePicker
            String searchDate = "";
            if (dpSearchDate != null && dpSearchDate.getValue() != null) {
                searchDate = dpSearchDate.getValue().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
            String searchBillNo = txtSearchBillNo != null ? txtSearchBillNo.getText().trim() : "";
            // Get customer from AutoComplete if available, otherwise from TextField
            final String searchCustomer;
            if (billHistoryCustomerAutoComplete != null) {
                searchCustomer = billHistoryCustomerAutoComplete.getTextField().getText().trim();
            } else if (txtSearchCustomer != null) {
                searchCustomer = txtSearchCustomer.getText().trim();
            } else {
                searchCustomer = "";
            }

            List<Bill> results = new ArrayList<>();

            // Search by bill number (highest priority)
            if (!searchBillNo.isEmpty()) {
                try {
                    Integer billNo = Integer.parseInt(searchBillNo);
                    billService.getBillByNo(billNo).ifPresent(results::add);
                } catch (NumberFormatException e) {
                    alert.showError("Invalid bill number");
                    return;
                }
            }
            // Search by date
            else if (!searchDate.isEmpty()) {
                List<Bill> paidBills = billService.getBillsByStatusAndDate("PAID", searchDate);
                List<Bill> creditBills = billService.getBillsByStatusAndDate("CREDIT", searchDate);
                results.addAll(paidBills);
                results.addAll(creditBills);

                // Filter by customer name if provided
                if (!searchCustomer.isEmpty()) {
                    final String customerSearch = searchCustomer.toLowerCase();
                    results = results.stream()
                            .filter(bill -> {
                                if (bill.getCustomerId() != null) {
                                    Customer customer = customerService.getCustomerById(bill.getCustomerId());
                                    return customer != null &&
                                            customer.getFullName().toLowerCase().contains(customerSearch);
                                }
                                return false;
                            })
                            .collect(java.util.stream.Collectors.toList());
                }
            }
            // Search by customer name only
            else if (!searchCustomer.isEmpty()) {
                // Get all customers matching the search
                List<Customer> matchingCustomers = customerService.getAllCustomers().stream()
                        .filter(c -> c.getFullName().toLowerCase().contains(searchCustomer.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());

                for (Customer customer : matchingCustomers) {
                    results.addAll(billService.getBillsByCustomerId(customer.getId()));
                }

                // Filter to only PAID and CREDIT
                results = results.stream()
                        .filter(b -> "PAID".equalsIgnoreCase(b.getStatus()) || "CREDIT".equalsIgnoreCase(b.getStatus()))
                        .collect(java.util.stream.Collectors.toList());
            }
            // No search criteria - load today's bills
            else {
                loadTodaysBills();
                return;
            }

            billHistoryList.clear();
            billHistoryList.addAll(results);

            // Sort by bill number ascending
            billHistoryList.sort((b1, b2) -> b1.getBillNo().compareTo(b2.getBillNo()));

            // Update summary
            List<Bill> paidBills = results.stream()
                    .filter(b -> "PAID".equalsIgnoreCase(b.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            List<Bill> creditBills = results.stream()
                    .filter(b -> "CREDIT".equalsIgnoreCase(b.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            updateBillSummary(paidBills, creditBills);

            LOG.info("Search found {} bills", results.size());

        } catch (Exception e) {
            LOG.error("Error searching bills", e);
            alert.showError("Error searching bills: " + e.getMessage());
        }
    }

    /**
     * Clear bill search fields and reload today's bills
     */
    private void clearBillSearch() {
        if (dpSearchDate != null) {
            dpSearchDate.setValue(java.time.LocalDate.now());
        }
        if (txtSearchBillNo != null) {
            txtSearchBillNo.clear();
        }
        if (billHistoryCustomerAutoComplete != null) {
            billHistoryCustomerAutoComplete.getTextField().clear();
        } else if (txtSearchCustomer != null) {
            txtSearchCustomer.clear();
        }
        loadTodaysBills();
    }

    /**
     * Setup AutoComplete for customer search in bill history panel
     */
    private void setupBillHistoryCustomerAutoComplete() {
        try {
            if (billHistoryCustomerSearchContainer == null) {
                LOG.warn("Bill history customer search container is null");
                return;
            }

            // Get all customers for autocomplete
            List<Customer> allCustomers = customerService.getAllCustomers();
            List<String> customerNames = new ArrayList<>();
            for (Customer customer : allCustomers) {
                String fullName = customer.getFullName();
                if (fullName != null && !fullName.trim().isEmpty()) {
                    customerNames.add(fullName);
                }
            }

            // Create a TextField for AutoComplete
            TextField customerTextField = new TextField();
            customerTextField.setPromptText("Customer Name");
            customerTextField.setPrefHeight(32);
            HBox.setHgrow(customerTextField, javafx.scene.layout.Priority.ALWAYS);

            // Get custom font from SessionService
            Font customFont = SessionService.getCustomFont(18.0);
            if (customFont != null) {
                customerTextField.setFont(customFont);
                String fontFamily = customFont.getFamily();
                customerTextField.setStyle(String.format(
                        "-fx-font-family: '%s'; -fx-font-size: 18px; -fx-background-color: transparent; -fx-border-color: transparent;",
                        fontFamily));

                // Create AutoComplete wrapper with custom font
                billHistoryCustomerAutoComplete = new AutoCompleteTextField(customerTextField, customerNames, customFont);
            } else {
                customerTextField.setStyle("-fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: transparent;");
                // Create AutoComplete wrapper without custom font
                billHistoryCustomerAutoComplete = new AutoCompleteTextField(customerTextField, customerNames);
            }

            // Replace the existing TextField with the new one
            billHistoryCustomerSearchContainer.getChildren().clear();
            billHistoryCustomerSearchContainer.getChildren().add(customerTextField);

            LOG.info("Bill history customer AutoComplete setup with {} customers", customerNames.size());

        } catch (Exception e) {
            LOG.error("Error setting up bill history customer AutoComplete", e);
        }
    }

    /**
     * Update bill summary totals
     */
    private void updateBillSummary(List<Bill> paidBills, List<Bill> creditBills) {
        float totalCash = 0f;
        float totalCredit = 0f;

        for (Bill bill : paidBills) {
            totalCash += bill.getBillAmt();
        }
        for (Bill bill : creditBills) {
            totalCredit += bill.getBillAmt();
        }

        if (lblTotalCash != null) {
            lblTotalCash.setText(String.format("₹%.0f", totalCash));
        }
        if (lblTotalCredit != null) {
            lblTotalCredit.setText(String.format("₹%.0f", totalCredit));
        }
        if (lblTotalBills != null) {
            lblTotalBills.setText(String.format("₹%.0f", totalCash + totalCredit));
        }
    }

    /**
     * View bill details (load into transaction table)
     */
    private void viewBillDetails(Bill bill) {
        try {
            // Always fetch transactions from database to avoid LazyInitializationException
            List<Transaction> transactions = billService.getTransactionsForBill(bill.getBillNo());

            // Convert to TempTransaction for display
            tempTransactionList.clear();
            int idCounter = -1; // Negative IDs to indicate it's from a saved bill
            for (Transaction trans : transactions) {
                TempTransaction temp = new TempTransaction();
                temp.setId(idCounter--);
                temp.setItemName(trans.getItemName());
                temp.setQty(trans.getQty());
                temp.setRate(trans.getRate());
                temp.setAmt(trans.getAmt());
                temp.setTableNo(bill.getTableNo());
                tempTransactionList.add(temp);
            }

            // Update totals
            updateTotals();

            // Fetch bill with transactions for printing
            currentClosedBill = billService.getBillWithTransactions(bill.getBillNo());

            LOG.info("Viewing bill #{} with {} items", bill.getBillNo(), transactions.size());

        } catch (Exception e) {
            LOG.error("Error viewing bill details", e);
            alert.showError("Error loading bill details: " + e.getMessage());
        }
    }

    /**
     * Calculate payment values based on bill amount, cash received, and return to customer
     *
     * Payment Logic:
     * - Net Received = Cash Received - Return to Customer (what we actually keep)
     * - For PAID bills (no customer): Discount = Bill Amount - Net Received (shortfall is discount)
     * - For CREDIT bills (with customer): Discount = 0 (shortfall is credit balance, not discount)
     * - Change = Cash Received - Bill Amount (if positive, what we should return)
     *
     * Examples for PAID bills:
     * 1. Bill=30, Cash=20, Return=0 → NetReceived=20, Discount=10, Change=0, Net=20
     * 2. Bill=30, Cash=50, Return=20 → NetReceived=30, Discount=0, Change=20, Net=30
     *
     * Examples for CREDIT bills (customer selected):
     * 1. Bill=100, Cash=0, Return=0 → Discount=0, Net=100 (customer owes 100)
     * 2. Bill=100, Cash=50, Return=0 → Discount=0, Net=100 (customer owes 50)
     */
    private void calculatePayment() {
        try {
            // Get bill amount from total
            float billAmount = 0;
            for (TempTransaction t : tempTransactionList) {
                billAmount += t.getAmt();
            }

            // Check if cash received field has a value
            boolean cashEntered = txtCashReceived != null && !txtCashReceived.getText().trim().isEmpty();
            boolean returnEntered = txtReturnToCustomer != null && !txtReturnToCustomer.getText().trim().isEmpty();

            // Get cash received
            float cashReceived = 0;
            if (cashEntered) {
                cashReceived = Float.parseFloat(txtCashReceived.getText().trim());
            }

            // Get return to customer (manual entry)
            float returnToCustomer = 0;
            if (returnEntered) {
                returnToCustomer = Float.parseFloat(txtReturnToCustomer.getText().trim());
            }

            // Default values when no payment info entered
            float change = 0;
            float discount = 0;
            float balance = 0;
            float netAmount = billAmount;

            // Check if this is a CREDIT bill (customer selected)
            boolean isCreditBill = (selectedCustomer != null);

            // Only calculate payment values if cash is entered
            if (cashEntered) {
                // Calculate net received (what we actually keep)
                float netReceived = cashReceived - returnToCustomer;
                if (netReceived < 0) {
                    netReceived = 0;
                }

                // Calculate change (what we SHOULD return based on cash received)
                change = Math.max(0, cashReceived - billAmount);

                // For PAID bills: shortfall is discount
                // For CREDIT bills: shortfall is credit balance (NOT discount)
                if (!isCreditBill && netReceived < billAmount) {
                    discount = billAmount - netReceived;
                }

                // Net amount calculation:
                // - For PAID bills: what we're actually keeping (capped at billAmount)
                // - For CREDIT bills: full bill amount (customer owes the full amount)
                if (isCreditBill) {
                    netAmount = billAmount;  // Full amount owed
                } else {
                    netAmount = Math.min(netReceived, billAmount);
                }
            }

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
                lblBalance.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
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
        // Reset payment mode to cash bank
        if (cmbPaymentMode != null && !cmbPaymentMode.getItems().isEmpty()) {
            // Find and select the cash bank (IFSC="cash")
            PaymentOption cashOption = cmbPaymentMode.getItems().stream()
                    .filter(PaymentOption::isCash)
                    .findFirst()
                    .orElse(null);
            if (cashOption != null) {
                cmbPaymentMode.getSelectionModel().select(cashOption);
            } else {
                cmbPaymentMode.getSelectionModel().selectFirst();
            }
        }
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
        tempTransaction.setWaitorId(employeesService.searchByFirstName(selectedWaitor).get(0).getEmployeeId());

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
            // Don't allow adding negative quantity as first item
            if (tempTransaction.getQty() < 0) {
                LOG.warn("Cannot add negative quantity as first item");
                return;
            }
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

                    float newQty = oldTransaction.getQty() + tempTransaction.getQty();

                    if (newQty <= 0) {
                        // Remove item from list if quantity becomes 0 or negative
                        tempTransactionList.remove(i);
                        LOG.info("Item removed from table view (qty became {}): {}", newQty, oldTransaction.getItemName());
                    } else {
                        // Update quantity and amount
                        oldTransaction.setQty(newQty);
                        oldTransaction.setAmt(newQty * oldTransaction.getRate());

                        // Update printQty - reduce by the same amount (negative qty)
                        if (tempTransaction.getQty() < 0 && oldTransaction.getPrintQty() != null) {
                            float newPrintQty = oldTransaction.getPrintQty() + tempTransaction.getPrintQty();
                            oldTransaction.setPrintQty(Math.max(0f, newPrintQty)); // Don't go below 0
                        } else if (tempTransaction.getPrintQty() != null && tempTransaction.getPrintQty() > 0) {
                            float existingPrintQty = oldTransaction.getPrintQty() != null ? oldTransaction.getPrintQty() : 0f;
                            oldTransaction.setPrintQty(existingPrintQty + tempTransaction.getPrintQty());
                        }

                        LOG.info("Updated item in table view: {} qty={}, printQty={}",
                                oldTransaction.getItemName(), newQty, oldTransaction.getPrintQty());
                    }

                    // Trigger TableView update (if using properties) or refresh
                    tblTransaction.refresh();
                    foundMatch = true;
                    break; // Use break instead of return to stay in method
                }
            }

            if (!foundMatch) {
                // Don't allow adding negative quantity for non-existing item
                if (tempTransaction.getQty() < 0) {
                    LOG.warn("Cannot add negative quantity for non-existing item: {}", tempTransaction.getItemName());
                    return;
                }
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
        String qtyText = txtQuantity.getText().trim();
        if (qtyText.isEmpty() || qtyText.equals("-") || qtyText.equals(".") || qtyText.equals("-.")) {
            alert.showError("Enter Valid Quantity");
            txtQuantity.requestFocus();
            return false;
        }
        if (txtPrice.getText().isEmpty()) {
            alert.showError("Enter Rate");
            txtPrice.requestFocus();
            return false;
        }
        if (txtAmount.getText().isEmpty()) {
            alert.showError("Amount not calculated");
            return false;
        }

        // Validate quantity and rate values
        try {
            float qty = Float.parseFloat(txtQuantity.getText().trim());
            float rate = Float.parseFloat(txtPrice.getText().trim());
            String itemName = txtItemName.getText().trim();

            // Validate quantity is not zero
            if (qty == 0) {
                alert.showError("Quantity cannot be zero");
                txtQuantity.requestFocus();
                return false;
            }

            // Validate rate is greater than zero
            if (rate <= 0) {
                alert.showError("Rate must be greater than zero");
                txtPrice.requestFocus();
                return false;
            }

            // Validate negative quantity - item must exist and resulting qty must be > 0
            if (qty < 0) {
                // Check if item exists in table view with same name and rate
                TempTransaction existingItem = findExistingItemInTableView(itemName, rate);
                if (existingItem == null) {
                    alert.showError("Cannot reduce quantity. Item '" + itemName + "' not found in the order.");
                    txtQuantity.requestFocus();
                    return false;
                }

                // Check if resulting quantity would be valid (> 0)
                float resultingQty = existingItem.getQty() + qty; // qty is negative, so this subtracts
                if (resultingQty < 0) {
                    alert.showError("Cannot reduce by " + Math.abs(qty) + ". Current quantity is only " + existingItem.getQty());
                    txtQuantity.requestFocus();
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            alert.showError("Invalid quantity or price");
            return false;
        }

        return true;
    }

    /**
     * Find existing item in table view by item name and rate
     */
    private TempTransaction findExistingItemInTableView(String itemName, float rate) {
        for (TempTransaction t : tempTransactionList) {
            if (t.getItemName().equals(itemName) && Float.compare(t.getRate(), rate) == 0) {
                return t;
            }
        }
        return null;
    }

    // ============= Bill Action Button Methods =============

    /**
     * Close the current table order
     * - If closed bill exists and new temp_transactions: add new items to existing bill
     * - If no closed bill: create new bill with CLOSE status
     * - Table stays selected after closing, showing the closed bill items
     * - No confirmation alerts - just save silently
     */
    private void closeTable() {
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        if (tempTransactionList.isEmpty()) {
            alert.showWarning("No items to close");
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
                alert.showWarning("Table already closed. No new items to add.");
                return;
            }

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
                    List<Employees> waiters = employeesService.searchByFirstName(selectedWaitor);
                    if (!waiters.isEmpty()) {
                        waitorId = waiters.get(0).getEmployeeId();
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

            // Update table button status to Closed (red)
            updateTableButtonStatus(tableId);

            // Reload transactions for this table to show closed bill items
            // Table stays selected, user can now pay the bill
            loadTransactionsForTable(tableId);

            // Clear item form only (keep table selected)
            clearItemForm();

            // Set focus to cash received field so user can enter payment
            if (txtCashReceived != null) {
                txtCashReceived.requestFocus();
            }

            LOG.info("Table {} closed. Bill #{} saved as CLOSED with amount ₹{}", tableName, savedBill.getBillNo(), savedBill.getBillAmt());

            // Print the bill as PDF
            final Bill billToPrint = savedBill;
            final String tableNameForPrint = tableName;

            // Run printing in background thread to avoid blocking UI
            new Thread(() -> {
                try {
                    billPrint.printBillWithDialog(billToPrint, tableNameForPrint);
                } catch (Exception e) {
                    LOG.error("Error printing bill #{}: {}", billToPrint.getBillNo(), e.getMessage(), e);
                }
            }).start();

        } catch (Exception e) {
            LOG.error("Error closing table", e);
            alert.showError("Error closing table: " + e.getMessage());
        }
    }

    /**
     * Start shift table mode
     * Validates that a table is selected and has items (ongoing or closed)
     */
    private void startShiftTableMode() {
        // Validate table is selected
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        String tableName = txtTableNumber.getText();
        Integer tableId = tableMasterService.getTableByName(tableName).getId();

        // Check if table has temp transactions or closed bill
        boolean hasTempTransactions = tempTransactionService.hasTransactions(tableId);
        boolean hasClosedBill = billService.hasClosedBill(tableId);

        if (!hasTempTransactions && !hasClosedBill) {
            alert.showError("Selected table has no items to shift");
            return;
        }

        // Enable shift mode
        isShiftTableMode = true;
        shiftSourceTableId = tableId;
        shiftSourceTableName = tableName;

        // Update button appearance to indicate shift mode is active
        btnShiftTable.setStyle("-fx-background-color: #E65100; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        btnShiftTable.setText("CANCEL");

        alert.showInfo("Shift mode enabled. Select the target table to shift items from " + tableName);
        LOG.info("Shift table mode started for table: {} (ID: {})", tableName, tableId);
    }

    /**
     * Handle target table selection in shift mode
     */
    private void handleShiftTableTarget(Integer targetTableId, String targetTableName) {
        // Check if same table is selected
        if (targetTableId.equals(shiftSourceTableId)) {
            alert.showWarning("Cannot shift to the same table. Select a different table.");
            return;
        }

        // Show confirmation dialog
        boolean confirmed = alert.showConfirmation("Shift Table",
                "Are you sure you want to shift all items from " + shiftSourceTableName + " to " + targetTableName + "?");

        if (confirmed) {
            performTableShift(targetTableId, targetTableName);
        } else {
            // User cancelled, exit shift mode
            cancelShiftTableMode();
        }
    }

    /**
     * Perform the actual table shift operation
     */
    private void performTableShift(Integer targetTableId, String targetTableName) {
        try {
            LOG.info("Shifting items from table {} to table {}", shiftSourceTableName, targetTableName);

            // 1. Shift temp_transactions
            int tempShifted = tempTransactionService.shiftTransactionsToTable(shiftSourceTableId, targetTableId);
            LOG.info("Shifted {} temp transactions", tempShifted);

            // 2. Shift closed bill transactions (if exists)
            Bill closedBill = billService.getClosedBillForTable(shiftSourceTableId);
            if (closedBill != null) {
                billService.shiftBillToTable(closedBill.getBillNo(), targetTableId);
                LOG.info("Shifted closed bill #{} to table {}", closedBill.getBillNo(), targetTableName);
            }

            // 3. Update source table button status
            updateTableButtonStatus(shiftSourceTableId);

            // 4. Update target table button status
            updateTableButtonStatus(targetTableId);

            // 5. Select and load the target table
            txtTableNumber.setText(targetTableName);
            loadTransactionsForTable(targetTableId);

            alert.showInfo("Successfully shifted items from " + shiftSourceTableName + " to " + targetTableName);
            LOG.info("Table shift completed successfully");

        } catch (Exception e) {
            LOG.error("Error shifting table", e);
            alert.showError("Error shifting table: " + e.getMessage());
        } finally {
            // Exit shift mode
            cancelShiftTableMode();
        }
    }

    /**
     * Cancel shift table mode and reset UI
     */
    private void cancelShiftTableMode() {
        isShiftTableMode = false;
        shiftSourceTableId = null;
        shiftSourceTableName = null;

        // Reset button appearance
        btnShiftTable.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        btnShiftTable.setText("SHIFT");

        LOG.info("Shift table mode cancelled");
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
     * Mark the current order as PAID or CREDIT
     * - If customer is selected → CREDIT bill (customer pays later)
     * - If no customer selected → CASH bill (must enter cash received)
     */
    private void markAsPaid() {
        // Validation 1: Table must be selected
        if (txtTableNumber.getText().isEmpty()) {
            alert.showError("Please select a table first");
            return;
        }

        // Validation 2: Must have items to bill
        if (tempTransactionList.isEmpty()) {
            alert.showError("No items to bill");
            return;
        }

        try {
            String tableName = txtTableNumber.getText();
            Integer tableId = tableMasterService.getTableByName(tableName).getId();

            // Check if there are new temp transactions for this table
            List<TempTransaction> newTempTransactions = tempTransactionService.getTransactionsByTableNo(tableId);
            boolean hasNewTempTransactions = !newTempTransactions.isEmpty();

            // Validation 3: Check if this is a closed bill
            if (currentClosedBill == null) {
                if (hasNewTempTransactions) {
                    alert.showWarning("Please close the table first");
                    return;
                } else {
                    alert.showError("No bill found. Close the table first.");
                    return;
                }
            }

            // Validation 4: If there are new temp transactions, table should be closed first
            if (hasNewTempTransactions) {
                alert.showWarning("Close the table first to save new items");
                return;
            }

            // Validation 5: Check if bill is already PAID or CREDIT
            if ("PAID".equalsIgnoreCase(currentClosedBill.getStatus())) {
                alert.showInfo("Bill already PAID");
                return;
            }
            if ("CREDIT".equalsIgnoreCase(currentClosedBill.getStatus())) {
                alert.showInfo("Bill already marked as CREDIT");
                return;
            }

            // Validation 6: Bill must be in CLOSE status
            if (!"CLOSE".equalsIgnoreCase(currentClosedBill.getStatus())) {
                alert.showError("Only closed bills can be processed");
                return;
            }

            // Get payment values from UI
            float billAmount = currentClosedBill.getBillAmt();
            float cashReceived = 0f;
            float returnToCustomer = 0f;
            float discount = 0f;
            Bill processedBill;

            // Check if cash received is empty
            boolean cashEmpty = (txtCashReceived == null || txtCashReceived.getText().trim().isEmpty());
            boolean returnEmpty = (txtReturnToCustomer == null || txtReturnToCustomer.getText().trim().isEmpty());

            // If no cash entered and no customer selected, show prompt
            if (cashEmpty && selectedCustomer == null) {
                alert.showWarning("Please enter Cash Received amount for PAID bill,\nor select a Customer for CREDIT bill.");
                if (txtCashReceived != null) {
                    txtCashReceived.requestFocus();
                }
                return;
            }

            // Check if customer is selected → CREDIT bill
            if (selectedCustomer != null) {
                // CREDIT BILL - Customer selected
                // Cash received is optional for credit bills (partial payment allowed)
                if (!cashEmpty) {
                    try {
                        cashReceived = Float.parseFloat(txtCashReceived.getText().trim());
                    } catch (NumberFormatException e) {
                        alert.showError("Invalid cash received amount");
                        txtCashReceived.requestFocus();
                        return;
                    }
                }

                // Parse return to customer (optional)
                if (!returnEmpty) {
                    try {
                        returnToCustomer = Float.parseFloat(txtReturnToCustomer.getText().trim());
                    } catch (NumberFormatException e) {
                        alert.showError("Invalid return amount");
                        txtReturnToCustomer.requestFocus();
                        return;
                    }
                }

                // For CREDIT bills, the unpaid amount is NOT a discount
                // It's the credit balance that customer owes
                // discount remains 0 unless an actual discount was explicitly given

                // Mark as CREDIT bill (discount = 0, full bill amount is owed)
                processedBill = billService.markBillAsCredit(
                        currentClosedBill.getBillNo(),
                        selectedCustomer.getId(),
                        cashReceived,
                        returnToCustomer,
                        0f  // No discount for credit bills - unpaid amount is credit, not discount
                );

                LOG.info("Bill #{} marked as CREDIT for customer {}. Amount: ₹{}",
                        processedBill.getBillNo(), selectedCustomer.getFullName(), processedBill.getNetAmount());

            } else {
                // PAID BILL - No customer selected, amount is required
                // At this point, amount is not empty (validated above)
                try {
                    cashReceived = Float.parseFloat(txtCashReceived.getText().trim());
                } catch (NumberFormatException e) {
                    alert.showError("Invalid amount received");
                    txtCashReceived.requestFocus();
                    return;
                }

                // Parse return to customer (optional)
                if (!returnEmpty) {
                    try {
                        returnToCustomer = Float.parseFloat(txtReturnToCustomer.getText().trim());
                    } catch (NumberFormatException e) {
                        alert.showError("Invalid return amount");
                        txtReturnToCustomer.requestFocus();
                        return;
                    }
                }

                // Get payment mode and bank ID from dropdown
                PaymentOption selectedPayment = getSelectedPaymentOption();
                if (selectedPayment == null || selectedPayment.getBank() == null) {
                    alert.showError("Please select a payment mode");
                    return;
                }
                // All payments go through bank (including cash bank with IFSC="cash")
                Integer bankId = selectedPayment.getBank().getId();
                String paymode = selectedPayment.isCash() ? "CASH" : "BANK";

                // Calculate net received and discount
                float netReceived = cashReceived - returnToCustomer;
                if (netReceived < 0) netReceived = 0;
                if (netReceived < billAmount) {
                    discount = billAmount - netReceived;
                }

                // Confirm discount if applicable
                if (discount > 0) {
                    if (!alert.showConfirmation("Confirm Discount",
                            "Accepting ₹" + String.format("%.0f", netReceived) + " for bill ₹" + String.format("%.0f", billAmount) + "\n" +
                            "Discount: ₹" + String.format("%.0f", discount) + "\n\nProceed?")) {
                        return;
                    }
                }

                // Mark as PAID bill (CASH or BANK)
                processedBill = billService.markBillAsPaid(
                        currentClosedBill.getBillNo(),
                        cashReceived,
                        returnToCustomer,
                        discount,
                        paymode,
                        bankId
                );

                // Record bank transaction if bank payment
                if (bankId != null && netReceived > 0) {
                    try {
                        // Record bank transaction (this also updates bank balance)
                        bankTransactionService.recordBillPayment(
                                bankId,
                                processedBill.getBillNo(),
                                (double) netReceived,
                                tableName
                        );
                        LOG.info("Bank transaction recorded: Bill #{}, Bank ID {}, Amount ₹{}",
                                processedBill.getBillNo(), bankId, netReceived);
                    } catch (Exception e) {
                        LOG.error("Error recording bank transaction: ", e);
                        alert.showWarning("Bill saved but bank transaction recording failed: " + e.getMessage());
                    }
                }

                LOG.info("Bill #{} marked as PAID ({}). Net: ₹{}, Discount: ₹{}",
                        processedBill.getBillNo(), paymode, processedBill.getNetAmount(), discount);
            }

            // Ask user if they want to print the bill
            boolean wantToPrint = alert.showConfirmation("Print Bill", "Print the bill?");

            if (wantToPrint) {
                final Bill billToPrint = processedBill;
                final String tableNameForPrint = tableName;

                // Run printing in background thread
                new Thread(() -> {
                    try {
                        billPrint.printBillWithDialog(billToPrint, tableNameForPrint);
                    } catch (Exception e) {
                        LOG.error("Error printing bill #{}: {}", billToPrint.getBillNo(), e.getMessage(), e);
                    }
                }).start();
            }

            // Clear UI and reset state
            tempTransactionList.clear();
            currentClosedBill = null;
            updateTotals();

            // Update table button status to Available
            updateTableButtonStatus(tableId);

            // Clear form fields
            clearItemForm();
            clearPaymentFields();
            clearSelectedCustomer();
            cmbWaitorName.getSelectionModel().clearSelection();
            txtTableNumber.clear();

            // Refresh bill history to show the new bill
            loadTodaysBills();

        } catch (Exception e) {
            LOG.error("Error processing payment", e);
            alert.showError("Error: " + e.getMessage());
        }
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
     * Print old/previous bill
     * If a bill is selected in history table, print that bill
     * Otherwise, print the last paid bill
     */
    private void showOldBills() {
        LOG.info("OLD BILL button clicked");

        try {
            Bill billToPrint = null;

            // Check if a bill is selected in the history table
            if (tblBillHistory != null) {
                billToPrint = tblBillHistory.getSelectionModel().getSelectedItem();
            }

            // If no bill selected, get the last paid bill
            if (billToPrint == null) {
                billToPrint = billService.getLastPaidBill();
                if (billToPrint == null) {
                    alert.showError("No paid bills found to print");
                    return;
                }
                LOG.info("No bill selected, printing last paid bill #{}", billToPrint.getBillNo());
            } else {
                // Reload bill with transactions
                billToPrint = billService.getBillWithTransactions(billToPrint.getBillNo());
                LOG.info("Printing selected bill #{}", billToPrint.getBillNo());

                // Clear the selection after getting the bill
                tblBillHistory.getSelectionModel().clearSelection();
            }

            // Get table name for the bill
            String tableName = "Table";
            if (billToPrint.getTableNo() != null) {
                try {
                    TableMaster table = tableMasterService.getTableById(billToPrint.getTableNo());
                    tableName = table.getTableName();
                } catch (Exception e) {
                    LOG.warn("Could not get table name for tableNo: {}", billToPrint.getTableNo());
                }
            }

            // Print the bill
            final Bill finalBill = billToPrint;
            final String finalTableName = tableName;

            new Thread(() -> {
                try {
                    billPrint.printBillWithDialog(finalBill, finalTableName);
                } catch (Exception e) {
                    LOG.error("Error printing bill #{}: {}", finalBill.getBillNo(), e.getMessage(), e);
                }
            }).start();

        } catch (Exception e) {
            LOG.error("Error in showOldBills", e);
            alert.showError("Error: " + e.getMessage());
        }
    }

    /**
     * Edit an existing bill
     * Loads selected bill from history table for editing
     */
    private void editBill() {
        LOG.info("EDIT BILL button clicked");

        try {
            // Check if a bill is selected in the history table
            if (tblBillHistory == null) {
                alert.showError("Bill history table not available");
                return;
            }

            Bill selectedBill = tblBillHistory.getSelectionModel().getSelectedItem();
            if (selectedBill == null) {
                alert.showError("Please select a bill from the history table to edit");
                return;
            }

            // Load bill with transactions
            billBeingEdited = billService.getBillWithTransactions(selectedBill.getBillNo());
            if (billBeingEdited == null) {
                alert.showError("Could not load bill data");
                return;
            }

            // Check if bill can be edited (only PAID or CREDIT bills)
            String status = billBeingEdited.getStatus();
            if (!"PAID".equalsIgnoreCase(status) && !"CREDIT".equalsIgnoreCase(status)) {
                alert.showError("Only PAID or CREDIT bills can be edited");
                billBeingEdited = null;
                return;
            }

            LOG.info("Loading bill #{} for editing. Status: {}, Items: {}",
                    billBeingEdited.getBillNo(), status, billBeingEdited.getTransactions().size());

            // Enable edit bill mode
            isEditBillMode = true;

            // Clear current transaction list
            tempTransactionList.clear();
            currentClosedBill = null;

            // Load transactions into the table view (using TempTransaction for display)
            for (Transaction trans : billBeingEdited.getTransactions()) {
                TempTransaction displayTrans = new TempTransaction();
                displayTrans.setId(trans.getId()); // Use actual transaction ID
                displayTrans.setItemName(trans.getItemName());
                displayTrans.setQty(trans.getQty());
                displayTrans.setRate(trans.getRate());
                displayTrans.setAmt(trans.getAmt());
                displayTrans.setTableNo(billBeingEdited.getTableNo());
                displayTrans.setPrintQty(0f);

                tempTransactionList.add(displayTrans);
            }

            // Load table number
            if (billBeingEdited.getTableNo() != null) {
                try {
                    TableMaster table = tableMasterService.getTableById(billBeingEdited.getTableNo());
                    if (table != null) {
                        txtTableNumber.setText(table.getTableName());
                    }
                } catch (Exception e) {
                    LOG.warn("Could not load table for bill: {}", e.getMessage());
                }
            }

            // Load waiter
            if (billBeingEdited.getWaitorId() != null) {
                try {
                    Employees waiter = employeesService.getEmployeeById(billBeingEdited.getWaitorId());
                    if (waiter != null) {
                        cmbWaitorName.getSelectionModel().select(waiter.getFirstName());
                    }
                } catch (Exception e) {
                    LOG.warn("Could not load waiter for bill: {}", e.getMessage());
                }
            }

            // Load customer if available
            if (billBeingEdited.getCustomerId() != null) {
                try {
                    for (Customer customer : allCustomers) {
                        if (customer.getId().equals(billBeingEdited.getCustomerId())) {
                            selectedCustomer = customer;
                            displaySelectedCustomer(customer);
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Could not load customer for bill: {}", e.getMessage());
                }
            } else {
                selectedCustomer = null;
                if (selectedCustomerDisplay != null) {
                    selectedCustomerDisplay.setVisible(false);
                }
            }

            // Load payment details
            if (txtCashReceived != null && billBeingEdited.getCashReceived() != null) {
                txtCashReceived.setText(String.format("%.0f", billBeingEdited.getCashReceived()));
            }
            if (txtReturnToCustomer != null && billBeingEdited.getReturnAmount() != null) {
                txtReturnToCustomer.setText(String.format("%.0f", billBeingEdited.getReturnAmount()));
            }

            // Load payment mode from saved bill
            if (cmbPaymentMode != null && !cmbPaymentMode.getItems().isEmpty()) {
                PaymentOption matchedOption = null;
                Integer savedBankId = billBeingEdited.getBankId();

                // First try to match by bankId if available
                if (savedBankId != null) {
                    for (PaymentOption opt : cmbPaymentMode.getItems()) {
                        if (opt.getBank() != null && opt.getBank().getId().equals(savedBankId)) {
                            matchedOption = opt;
                            break;
                        }
                    }
                }

                // If no bankId match and paymode is "CASH" (backwards compatibility), find cash bank
                if (matchedOption == null && "CASH".equalsIgnoreCase(billBeingEdited.getPaymode())) {
                    matchedOption = cmbPaymentMode.getItems().stream()
                            .filter(PaymentOption::isCash)
                            .findFirst()
                            .orElse(null);
                }

                // Set the matched payment mode or default to cash bank
                if (matchedOption != null) {
                    cmbPaymentMode.setValue(matchedOption);
                } else {
                    // Default to cash bank
                    PaymentOption cashOption = cmbPaymentMode.getItems().stream()
                            .filter(PaymentOption::isCash)
                            .findFirst()
                            .orElse(cmbPaymentMode.getItems().get(0));
                    cmbPaymentMode.setValue(cashOption);
                }
            }

            // Update UI to show edit mode
            // Change CLOSE button to SAVE and enable it
            btnClose.setDisable(false);
            btnClose.setText("SAVE");
            btnClose.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");

            // Change EDIT button to CANCEL
            btnEditBill.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
            btnEditBill.setText("CANCEL");

            // Disable other buttons during edit
            btnPaid.setDisable(true);
            btnShiftTable.setDisable(true);
            btnOldBill.setDisable(true);

            // Clear history table selection
            tblBillHistory.getSelectionModel().clearSelection();

            // Refresh table and totals
            tblTransaction.refresh();
            updateTotals();

            alert.showInfo("Editing Bill #" + billBeingEdited.getBillNo() + ". Make changes and click SAVE.");
            LOG.info("Bill #{} loaded for editing", billBeingEdited.getBillNo());

        } catch (Exception e) {
            LOG.error("Error loading bill for editing", e);
            alert.showError("Error loading bill: " + e.getMessage());
            cancelEditBillMode();
        }
    }

    /**
     * Cancel edit bill mode and reset UI
     */
    private void cancelEditBillMode() {
        isEditBillMode = false;
        billBeingEdited = null;

        // Reset CLOSE button
        btnClose.setDisable(false);
        btnClose.setText("CLOSE");
        btnClose.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");

        // Reset EDIT button
        btnEditBill.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-min-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        btnEditBill.setText("EDIT");

        // Re-enable buttons
        btnPaid.setDisable(false);
        btnShiftTable.setDisable(false);
        btnOldBill.setDisable(false);

        // Clear form
        tempTransactionList.clear();
        txtTableNumber.clear();
        cmbWaitorName.getSelectionModel().clearSelection();
        selectedCustomer = null;
        if (selectedCustomerDisplay != null) {
            selectedCustomerDisplay.setVisible(false);
        }
        currentClosedBill = null;

        if (txtCashReceived != null) txtCashReceived.clear();
        if (txtReturnToCustomer != null) txtReturnToCustomer.clear();

        clearItemForm();
        updateTotals();
        tblTransaction.refresh();

        LOG.info("Edit bill mode cancelled");
    }

    /**
     * Save the edited bill with modified transactions
     */
    private void saveEditedBill() {
        if (!isEditBillMode || billBeingEdited == null) {
            alert.showError("No bill is being edited");
            return;
        }

        if (tempTransactionList.isEmpty()) {
            alert.showError("Cannot save bill with no items");
            return;
        }

        try {
            LOG.info("Saving edited bill #{}", billBeingEdited.getBillNo());

            // Get waiter ID
            Integer waitorId = null;
            String selectedWaitor = cmbWaitorName.getSelectionModel().getSelectedItem();
            if (selectedWaitor != null && !selectedWaitor.isEmpty()) {
                List<Employees> waiters = employeesService.searchByFirstName(selectedWaitor);
                if (!waiters.isEmpty()) {
                    waitorId = waiters.get(0).getEmployeeId();
                }
            }

            // Get customer ID (for credit bills)
            Integer customerId = selectedCustomer != null ? selectedCustomer.getId() : null;

            // Get payment values
            float cashReceived = 0f;
            float returnAmount = 0f;

            if (txtCashReceived != null && !txtCashReceived.getText().trim().isEmpty()) {
                try {
                    cashReceived = Float.parseFloat(txtCashReceived.getText().trim());
                } catch (NumberFormatException e) {
                    alert.showError("Invalid cash received amount");
                    return;
                }
            }

            if (txtReturnToCustomer != null && !txtReturnToCustomer.getText().trim().isEmpty()) {
                try {
                    returnAmount = Float.parseFloat(txtReturnToCustomer.getText().trim());
                } catch (NumberFormatException e) {
                    alert.showError("Invalid return amount");
                    return;
                }
            }

            // Calculate totals from current items
            float totalAmt = 0f;
            float totalQty = 0f;
            for (TempTransaction temp : tempTransactionList) {
                totalAmt += temp.getAmt();
                totalQty += temp.getQty();
            }

            // Determine status based on customer selection
            String newStatus = (selectedCustomer != null) ? "CREDIT" : "PAID";

            // Update bill in database
            Bill updatedBill = billService.updateBillWithTransactions(
                    billBeingEdited.getBillNo(),
                    tempTransactionList,
                    waitorId,
                    customerId,
                    totalAmt,
                    totalQty,
                    cashReceived,
                    returnAmount,
                    newStatus
            );

            alert.showInfo("Bill #" + updatedBill.getBillNo() + " updated successfully!");
            LOG.info("Bill #{} updated. New total: ₹{}, Status: {}", updatedBill.getBillNo(), totalAmt, newStatus);

            // Reload today's bills to reflect changes
            loadTodaysBills();

            // Exit edit mode
            cancelEditBillMode();

        } catch (Exception e) {
            LOG.error("Error saving bill", e);
            alert.showError("Error saving bill: " + e.getMessage());
        }
    }

}
