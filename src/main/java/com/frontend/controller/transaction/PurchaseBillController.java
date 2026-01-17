package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.dto.ItemDto;
import com.frontend.entity.*;
import com.frontend.service.*;
import com.frontend.view.AlertNotification;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for Purchase Bill Entry with AutoCompleteTextField
 * Two-part layout: Left side for bill entry, Right side for existing bills
 */
@Component
public class PurchaseBillController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseBillController.class);

    @Autowired
    private PurchaseBillService purchaseBillService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CategoryApiService categoryApiService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private BankService bankService;

    @Autowired
    private BankTransactionService bankTransactionService;

    // ==================== LEFT SIDE: Bill Entry Form ====================

    // Labels with custom font
    @FXML private Label lblSupplier;
    @FXML private Label lblCategory;
    @FXML private Label lblItemName;
    @FXML private Label lblQty;
    @FXML private Label lblRate;
    @FXML private Label lblAmount;
    @FXML private Label lblRemarks;
    @FXML private Label lblSubTotalLabel;
    @FXML private Label lblDiscountLabel;
    @FXML private Label lblGrandTotalLabel;

    // Section 1: Supplier & Bill Info
    @FXML private TextField txtSupplier;
    @FXML private DatePicker dpBillDate;
    @FXML private TextField txtSupplierBillNo;
    @FXML private TextField txtReffNo;
    @FXML private Label lblItemCount;

    // Section 2: Item Entry
    @FXML private TextField txtCategory;
    @FXML private TextField txtItemName;
    @FXML private TextField txtQty;
    @FXML private TextField txtRate;
    @FXML private TextField txtAmount;
    @FXML private Button btnAddItem;
    @FXML private Button btnEditItem;
    @FXML private Button btnClearFields;
    @FXML private Button btnRemoveItem;

    // Items Table
    @FXML private TableView<PurchaseItemData> tblPurchaseItems;
    @FXML private TableColumn<PurchaseItemData, Integer> colSrNo;
    @FXML private TableColumn<PurchaseItemData, String> colItemName;
    @FXML private TableColumn<PurchaseItemData, Float> colQty;
    @FXML private TableColumn<PurchaseItemData, Float> colRate;
    @FXML private TableColumn<PurchaseItemData, Float> colAmount;

    // Section 3: Bill Summary
    @FXML private Label lblSubTotal;
    @FXML private TextField txtDiscount;
    @FXML private TextField txtGst;
    @FXML private TextField txtOtherTax;
    @FXML private Label lblGrandTotal;
    @FXML private ComboBox<PaymentOption> cmbPaymentMode;
    @FXML private TextField txtRemarks;

    // Action Buttons
    @FXML private Button btnSaveBill;
    @FXML private Button btnNewBill;
    @FXML private Button btnClearAll;
    @FXML private Button btnBack;

    // ==================== RIGHT SIDE: Existing Bills ====================

    @FXML private Button btnRefreshBills;
    @FXML private DatePicker dpSearchFromDate;
    @FXML private DatePicker dpSearchToDate;
    @FXML private TextField txtSearchBillNo;
    @FXML private HBox hboxSearchSupplier;
    @FXML private Button btnSearchBills;
    @FXML private Button btnClearSearch;

    @FXML private TableView<ExistingBillData> tblExistingBills;
    @FXML private TableColumn<ExistingBillData, Integer> colBillNo;
    @FXML private TableColumn<ExistingBillData, String> colBillDate;
    @FXML private TableColumn<ExistingBillData, String> colSupplierName;
    @FXML private TableColumn<ExistingBillData, Double> colBillAmount;
    @FXML private TableColumn<ExistingBillData, String> colBillStatus;

    @FXML private Label lblTotalPending;
    @FXML private Label lblTotalPaid;
    @FXML private Label lblTotalAmount;

    // AutoComplete fields
    private AutoCompleteTextField supplierAutoComplete;
    private AutoCompleteTextField categoryAutoComplete;
    private AutoCompleteTextField itemAutoComplete;
    private AutoCompleteTextField searchSupplierAutoComplete;
    private TextField txtSearchSupplier;

    // Data
    private ObservableList<PurchaseItemData> purchaseItems = FXCollections.observableArrayList();
    private ObservableList<ExistingBillData> existingBills = FXCollections.observableArrayList();
    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<CategoryMasterDto> stockCategories = new ArrayList<>();
    private List<ItemDto> currentCategoryItems = new ArrayList<>();
    private List<Bank> allBanks = new ArrayList<>();

    private Supplier selectedSupplier = null;
    private CategoryMasterDto selectedCategory = null;
    private ItemDto selectedItem = null;
    private PurchaseItemData editingItem = null;
    private PurchaseBill editingBill = null;
    private int serialNumber = 1;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing PurchaseBillController");
        loadData();
        setupAutoCompleteFields();
        setupPaymentComboBox();
        setupItemsTable();
        setupExistingBillsTable();
        setupEventHandlers();
        setupCalculationListeners();
        applyCustomFonts();

        dpBillDate.setValue(LocalDate.now());
        dpSearchFromDate.setValue(LocalDate.now().minusDays(30));
        dpSearchToDate.setValue(LocalDate.now());

        loadExistingBills();
    }

    private void loadData() {
        try {
            allSuppliers = supplierService.getActiveSuppliers();
            LOG.info("Loaded {} suppliers", allSuppliers.size());

            List<CategoryMasterDto> purchaseCategories = categoryApiService.getPurchaseCategories();
            stockCategories.clear();
            stockCategories.addAll(purchaseCategories);
            LOG.info("Loaded {} purchase categories", stockCategories.size());

            // Load banks for payment mode (only active banks)
            allBanks = bankService.getActiveBanks();
            LOG.info("Loaded {} active banks", allBanks.size());

        } catch (Exception e) {
            LOG.error("Error loading data: ", e);
        }
    }

    private void setupAutoCompleteFields() {
        Font customFont = SessionService.getCustomFont();
        Font font20 = customFont != null ? Font.font(customFont.getFamily(), 20) : Font.font(20);

        // Supplier AutoComplete
        List<String> supplierNames = allSuppliers.stream()
                .map(s -> s.getName() + (s.getCity() != null ? " (" + s.getCity() + ")" : ""))
                .collect(Collectors.toList());
        supplierAutoComplete = new AutoCompleteTextField(txtSupplier, supplierNames, font20);
        supplierAutoComplete.setUseContainsFilter(true);
        supplierAutoComplete.setOnSelectionCallback(this::onSupplierSelected);
        supplierAutoComplete.setNextFocusField(txtCategory);

        // Category AutoComplete
        List<String> categoryNames = stockCategories.stream()
                .map(CategoryMasterDto::getCategory)
                .collect(Collectors.toList());
        categoryAutoComplete = new AutoCompleteTextField(txtCategory, categoryNames, font20);
        categoryAutoComplete.setUseContainsFilter(true);
        categoryAutoComplete.setOnSelectionCallback(this::onCategorySelected);
        categoryAutoComplete.setNextFocusField(txtItemName);

        // Item AutoComplete (initially empty, filled when category selected)
        itemAutoComplete = new AutoCompleteTextField(txtItemName, new ArrayList<>(), font20);
        itemAutoComplete.setUseContainsFilter(true);
        itemAutoComplete.setOnSelectionCallback(this::onItemSelected);
        itemAutoComplete.setNextFocusField(txtQty);

        // Search Supplier AutoComplete (in right panel)
        setupSearchSupplierAutoComplete();
    }

    private void setupSearchSupplierAutoComplete() {
        Font customFont = SessionService.getCustomFont();
        Font font20 = customFont != null ? Font.font(customFont.getFamily(), 20) : Font.font(20);

        // Create TextField for search supplier
        txtSearchSupplier = new TextField();
        txtSearchSupplier.setPromptText("paurvazadaracao naava.....");
        txtSearchSupplier.setPrefHeight(38);
        txtSearchSupplier.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");
        txtSearchSupplier.setFont(font20);
        HBox.setHgrow(txtSearchSupplier, Priority.ALWAYS);

        // Add to HBox
        hboxSearchSupplier.getChildren().add(txtSearchSupplier);

        // Setup AutoComplete
        List<String> supplierNames = allSuppliers.stream()
                .map(s -> s.getName() + (s.getCity() != null ? " (" + s.getCity() + ")" : ""))
                .collect(Collectors.toList());
        searchSupplierAutoComplete = new AutoCompleteTextField(txtSearchSupplier, supplierNames, font20);
        searchSupplierAutoComplete.setUseContainsFilter(true);
    }

    private void onSupplierSelected(String selection) {
        if (selection == null || selection.isEmpty()) {
            selectedSupplier = null;
            return;
        }
        // Extract supplier name (remove city part if present)
        String supplierName = selection.contains("(") ? selection.substring(0, selection.indexOf("(")).trim() : selection;
        for (Supplier supplier : allSuppliers) {
            if (supplier.getName().equals(supplierName)) {
                selectedSupplier = supplier;
                LOG.info("Selected supplier: {}", supplier.getName());
                break;
            }
        }
    }

    private void onCategorySelected(String selection) {
        if (selection == null || selection.isEmpty()) {
            selectedCategory = null;
            currentCategoryItems.clear();
            itemAutoComplete.setSuggestions(new ArrayList<>());
            return;
        }
        for (CategoryMasterDto cat : stockCategories) {
            if (cat.getCategory().equals(selection)) {
                selectedCategory = cat;
                loadItemsForCategory(cat.getId());
                break;
            }
        }
    }

    private void loadItemsForCategory(Integer categoryId) {
        try {
            currentCategoryItems = itemService.getItemsByCategoryId(categoryId);
            List<String> itemNames = currentCategoryItems.stream()
                    .map(ItemDto::getItemName)
                    .collect(Collectors.toList());
            itemAutoComplete.setSuggestions(itemNames);
            LOG.info("Loaded {} items for category {}", currentCategoryItems.size(), categoryId);
        } catch (Exception e) {
            LOG.error("Error loading items: ", e);
        }
    }

    private void onItemSelected(String selection) {
        if (selection == null || selection.isEmpty()) {
            selectedItem = null;
            return;
        }
        for (ItemDto item : currentCategoryItems) {
            if (item.getItemName().equals(selection)) {
                selectedItem = item;
                txtRate.setText(String.format("%.2f", item.getRate()));
                txtQty.requestFocus();
                calculateAmount();
                break;
            }
        }
    }

    private void setupPaymentComboBox() {
        ObservableList<PaymentOption> paymentOptions = FXCollections.observableArrayList();
        PaymentOption cashBankOption = null;

        // Add all banks (including cash bank with IFSC "cash")
        for (Bank bank : allBanks) {
            PaymentOption option = new PaymentOption(bank.getBankName(), bank);
            paymentOptions.add(option);
            // Track the cash bank option to select it by default
            if ("cash".equalsIgnoreCase(bank.getIfsc())) {
                cashBankOption = option;
            }
        }

        // Add CREDIT option at the end (for credit purchases from suppliers)
        paymentOptions.add(new PaymentOption("CREDIT", null));

        cmbPaymentMode.setItems(paymentOptions);

        // Set custom font for dropdown
        Font customFont = SessionService.getCustomFont();
        String fontFamily = customFont != null ? customFont.getFamily() : "System";

        // Custom cell factory for dropdown items
        cmbPaymentMode.setCellFactory(listView -> new ListCell<PaymentOption>() {
            @Override
            protected void updateItem(PaymentOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    if (item.isCash()) {
                        // Cash bank - green color with custom font
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-background-color: white;");
                    } else if (item.isCredit()) {
                        // Credit - orange color with English font
                        setStyle("-fx-font-size: 14px; -fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-background-color: white;");
                    } else {
                        // Other banks - blue color with custom font
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #1565C0; -fx-background-color: white;");
                    }
                }
            }
        });

        // Custom button cell for selected item display
        cmbPaymentMode.setButtonCell(new ListCell<PaymentOption>() {
            @Override
            protected void updateItem(PaymentOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                    if (item.isCash()) {
                        // Cash bank - custom font
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    } else if (item.isCredit()) {
                        // Credit - English font
                        setStyle("-fx-font-size: 14px; -fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        // Other banks - custom font
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #1565C0;");
                    }
                }
            }
        });

        // String converter for ComboBox
        cmbPaymentMode.setConverter(new StringConverter<PaymentOption>() {
            @Override
            public String toString(PaymentOption option) {
                return option != null ? option.getDisplayName() : "";
            }

            @Override
            public PaymentOption fromString(String string) {
                return paymentOptions.stream()
                        .filter(opt -> opt.getDisplayName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Select cash bank by default, otherwise first option
        if (cashBankOption != null) {
            cmbPaymentMode.setValue(cashBankOption);
            LOG.info("Selected cash bank as default payment mode");
        } else if (!paymentOptions.isEmpty()) {
            cmbPaymentMode.setValue(paymentOptions.get(0));
            LOG.warn("Cash bank (IFSC='cash') not found, selecting first option as default");
        }
    }

    private void setupItemsTable() {
        colSrNo.setCellValueFactory(cellData -> cellData.getValue().srNoProperty().asObject());
        colItemName.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());
        colQty.setCellValueFactory(cellData -> cellData.getValue().qtyProperty().asObject());
        colRate.setCellValueFactory(cellData -> cellData.getValue().rateProperty().asObject());
        colAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty().asObject());

        applyItemNameColumnFont();

        // Row selection for editing
        tblPurchaseItems.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadItemForEdit(newVal);
            }
        });

        tblPurchaseItems.setItems(purchaseItems);
    }

    private void setupExistingBillsTable() {
        colBillNo.setCellValueFactory(cellData -> cellData.getValue().billNoProperty().asObject());
        colBillDate.setCellValueFactory(cellData -> cellData.getValue().billDateProperty());
        colSupplierName.setCellValueFactory(cellData -> cellData.getValue().supplierNameProperty());
        colBillAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty().asObject());
        colBillStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Apply custom font to supplier name column
        applySupplierNameColumnFont();

        // Status column styling
        colBillStatus.setCellFactory(column -> new TableCell<ExistingBillData, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("PAID".equals(status)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Double-click to edit bill
        tblExistingBills.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ExistingBillData selected = tblExistingBills.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadBillForEditing(selected.getBillNo());
                }
            }
        });

        tblExistingBills.setItems(existingBills);
    }

    private void loadItemForEdit(PurchaseItemData item) {
        editingItem = item;
        txtItemName.setText(item.getItemName());
        txtQty.setText(String.valueOf(item.getQty()));
        txtRate.setText(String.format("%.2f", item.getRate()));
        txtAmount.setText(String.format("%.2f", item.getAmount()));
    }

    private void setupEventHandlers() {
        btnAddItem.setOnAction(e -> addItem());
        btnEditItem.setOnAction(e -> editItem());
        btnClearFields.setOnAction(e -> clearItemFields());
        btnRemoveItem.setOnAction(e -> removeSelectedItem());
        btnClearAll.setOnAction(e -> clearAllItems());
        btnSaveBill.setOnAction(e -> saveBill());
        btnNewBill.setOnAction(e -> newBill());
        btnBack.setOnAction(e -> navigateBack());

        // Right side event handlers
        btnRefreshBills.setOnAction(e -> loadExistingBills());
        btnSearchBills.setOnAction(e -> searchBills());
        btnClearSearch.setOnAction(e -> clearSearch());

        txtQty.textProperty().addListener((obs, oldVal, newVal) -> calculateAmount());
        txtRate.textProperty().addListener((obs, oldVal, newVal) -> calculateAmount());

        txtQty.setOnAction(e -> txtRate.requestFocus());
        txtRate.setOnAction(e -> addItem());
    }

    private void setupCalculationListeners() {
        txtDiscount.textProperty().addListener((obs, oldVal, newVal) -> updateTotals());
        txtGst.textProperty().addListener((obs, oldVal, newVal) -> updateTotals());
        txtOtherTax.textProperty().addListener((obs, oldVal, newVal) -> updateTotals());
    }

    private void calculateAmount() {
        try {
            float qty = txtQty.getText().isEmpty() ? 0 : Float.parseFloat(txtQty.getText());
            float rate = txtRate.getText().isEmpty() ? 0 : Float.parseFloat(txtRate.getText());
            txtAmount.setText(String.format("%.2f", qty * rate));
        } catch (NumberFormatException e) {
            txtAmount.setText("0.00");
        }
    }

    private void addItem() {
        String itemName = txtItemName.getText().trim();
        if (itemName.isEmpty()) {
            alertNotification.showWarning("Please enter item name");
            return;
        }

        float qty, rate;
        try {
            qty = Float.parseFloat(txtQty.getText());
            rate = Float.parseFloat(txtRate.getText());
        } catch (NumberFormatException e) {
            alertNotification.showWarning("Please enter valid quantity and rate");
            return;
        }

        if (qty <= 0) {
            alertNotification.showWarning("Quantity must be greater than 0");
            return;
        }

        float amount = qty * rate;

        // Check for duplicate
        for (PurchaseItemData existing : purchaseItems) {
            if (existing.getItemName().equals(itemName) && existing.getRate() == rate) {
                existing.setQty(existing.getQty() + qty);
                existing.setAmount(existing.getQty() * existing.getRate());
                tblPurchaseItems.refresh();
                updateTotals();
                clearItemFields();
                return;
            }
        }

        Integer itemCode = selectedItem != null ? selectedItem.getItemCode() : null;
        Integer categoryId = selectedItem != null ? selectedItem.getCategoryId() :
                            (selectedCategory != null ? selectedCategory.getId() : null);

        PurchaseItemData data = new PurchaseItemData(serialNumber++, itemName, qty, rate, amount, itemCode, categoryId);
        purchaseItems.add(data);

        tblPurchaseItems.refresh();
        updateTotals();
        clearItemFields();
        txtCategory.requestFocus();
    }

    private void editItem() {
        if (editingItem == null) {
            alertNotification.showWarning("Please select an item to edit");
            return;
        }

        float qty, rate;
        try {
            qty = Float.parseFloat(txtQty.getText());
            rate = Float.parseFloat(txtRate.getText());
        } catch (NumberFormatException e) {
            alertNotification.showWarning("Please enter valid quantity and rate");
            return;
        }

        editingItem.setItemName(txtItemName.getText().trim());
        editingItem.setQty(qty);
        editingItem.setRate(rate);
        editingItem.setAmount(qty * rate);

        tblPurchaseItems.refresh();
        updateTotals();
        clearItemFields();
        editingItem = null;
        tblPurchaseItems.getSelectionModel().clearSelection();
    }

    private void clearItemFields() {
        categoryAutoComplete.clear();
        itemAutoComplete.clear();
        txtQty.clear();
        txtRate.clear();
        txtAmount.setText("0.00");
        selectedCategory = null;
        selectedItem = null;
        editingItem = null;
        currentCategoryItems.clear();
        itemAutoComplete.setSuggestions(new ArrayList<>());
        tblPurchaseItems.getSelectionModel().clearSelection();
    }

    private void removeSelectedItem() {
        PurchaseItemData selected = tblPurchaseItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showWarning("Please select an item to remove");
            return;
        }
        purchaseItems.remove(selected);
        renumberItems();
        updateTotals();
        clearItemFields();
    }

    private void renumberItems() {
        serialNumber = 1;
        for (PurchaseItemData item : purchaseItems) {
            item.setSrNo(serialNumber++);
        }
        tblPurchaseItems.refresh();
    }

    private void updateTotals() {
        double subTotal = 0.0;
        for (PurchaseItemData item : purchaseItems) {
            subTotal += item.getAmount();
        }
        lblSubTotal.setText(String.format("%.2f", subTotal));
        lblItemCount.setText(String.valueOf(purchaseItems.size()));

        double discount = parseDouble(txtDiscount.getText());
        double gst = parseDouble(txtGst.getText());
        double otherTax = parseDouble(txtOtherTax.getText());

        double grandTotal = subTotal - discount + gst + otherTax;
        lblGrandTotal.setText(String.format("%.2f", grandTotal));
    }

    private double parseDouble(String text) {
        try {
            return text.isEmpty() ? 0.0 : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void clearAllItems() {
        purchaseItems.clear();
        serialNumber = 1;
        updateTotals();
        clearItemFields();
    }

    private void saveBill() {
        if (selectedSupplier == null) {
            alertNotification.showError("Please select a supplier");
            return;
        }
        if (purchaseItems.isEmpty()) {
            alertNotification.showError("Please add at least one item");
            return;
        }
        if (dpBillDate.getValue() == null) {
            alertNotification.showError("Please select a bill date");
            return;
        }

        try {
            // Get selected payment option
            PaymentOption selectedPayment = cmbPaymentMode.getValue();
            if (selectedPayment == null) {
                alertNotification.showError("Please select a payment mode");
                return;
            }
            // Store the display name (bank name or "CREDIT") in pay field for matching when loading
            String paymentMode = selectedPayment.getDisplayName();
            // All payments except CREDIT have a bankId (including cash bank)
            Integer bankId = selectedPayment.getBank() != null ? selectedPayment.getBank().getId() : null;

            PurchaseBill bill = new PurchaseBill();
            bill.setPartyId(selectedSupplier.getId());
            bill.setBillDate(dpBillDate.getValue());
            bill.setReffNo(txtReffNo.getText().trim());
            bill.setPay(paymentMode);
            bill.setRemarks(txtRemarks.getText().trim());
            bill.setStatus(selectedPayment != null && selectedPayment.isCredit() ? "PENDING" : "PAID");
            bill.setGst(parseDouble(txtGst.getText()));
            bill.setOtherTax(parseDouble(txtOtherTax.getText()));
            bill.setBankId(bankId);

            List<PurchaseTransaction> transactions = new ArrayList<>();
            for (PurchaseItemData itemData : purchaseItems) {
                PurchaseTransaction trans = new PurchaseTransaction();
                trans.setItemName(itemData.getItemName());
                trans.setQty(itemData.getQty());
                trans.setRate(itemData.getRate());
                trans.setAmount(itemData.getAmount());
                trans.setItemCode(itemData.getItemCode());
                trans.setCategoryId(itemData.getCategoryId());
                transactions.add(trans);
            }

            PurchaseBill savedBill;
            if (editingBill != null) {
                purchaseBillService.updatePurchaseBill(editingBill.getBillNo(), bill, transactions);
                savedBill = bill;
                savedBill.setBillNo(editingBill.getBillNo());
                alertNotification.showSuccess("Purchase bill updated!");
            } else {
                savedBill = purchaseBillService.createPurchaseBill(bill, transactions);
                alertNotification.showSuccess("Bill #" + savedBill.getBillNo() + " saved!");
            }

            // Record bank transaction (withdrawal) if bank payment is selected
            if (bankId != null && savedBill.getNetAmount() != null && savedBill.getNetAmount() > 0) {
                try {
                    String particulars = "Purchase Bill #" + savedBill.getBillNo();
                    if (selectedSupplier != null && selectedSupplier.getName() != null) {
                        particulars += " (" + selectedSupplier.getName() + ")";
                    }
                    String remarks = "Purchase-Bill-no-" + savedBill.getBillNo();
                    bankTransactionService.recordWithdrawal(bankId, savedBill.getNetAmount(), particulars,
                            "PURCHASE", savedBill.getBillNo(), remarks);
                    LOG.info("Bank withdrawal recorded for purchase bill #{}: Amount={}", savedBill.getBillNo(), savedBill.getNetAmount());
                } catch (Exception e) {
                    LOG.error("Error recording bank transaction for purchase bill: ", e);
                    alertNotification.showWarning("Bill saved but bank transaction failed: " + e.getMessage());
                }
            }

            newBill();
            loadExistingBills();

        } catch (Exception e) {
            LOG.error("Error saving bill: ", e);
            alertNotification.showError("Error: " + e.getMessage());
        }
    }

    private void newBill() {
        editingBill = null;
        supplierAutoComplete.clear();
        selectedSupplier = null;
        txtSupplierBillNo.clear();
        txtReffNo.clear();
        dpBillDate.setValue(LocalDate.now());
        txtDiscount.clear();
        txtGst.clear();
        txtOtherTax.clear();
        txtRemarks.clear();
        // Reset to cash bank
        if (!cmbPaymentMode.getItems().isEmpty()) {
            // Find and select the cash bank (IFSC="cash")
            PaymentOption cashOption = cmbPaymentMode.getItems().stream()
                    .filter(PaymentOption::isCash)
                    .findFirst()
                    .orElse(cmbPaymentMode.getItems().get(0));
            cmbPaymentMode.setValue(cashOption);
        }
        clearAllItems();
    }

    private void navigateBack() {
        try {
            BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
            if (mainPane != null) {
                Pane menuPane = loader.getPage("/fxml/transaction/PurchaseMenu.fxml");
                mainPane.setCenter(menuPane);
            }
        } catch (Exception e) {
            LOG.error("Error navigating back: ", e);
        }
    }

    // ==================== Existing Bills Methods ====================

    private void loadExistingBills() {
        try {
            existingBills.clear();
            List<PurchaseBill> bills = purchaseBillService.getAllBills();

            double totalPending = 0.0;
            double totalPaid = 0.0;

            for (PurchaseBill bill : bills) {
                String supplierName = getSupplierName(bill.getPartyId());
                String dateStr = bill.getBillDate() != null ? bill.getBillDate().format(DATE_FORMATTER) : "";
                double amount = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;

                existingBills.add(new ExistingBillData(
                        bill.getBillNo(),
                        dateStr,
                        supplierName,
                        amount,
                        bill.getStatus()
                ));

                if ("PENDING".equals(bill.getStatus())) {
                    totalPending += amount;
                } else {
                    totalPaid += amount;
                }
            }

            lblTotalPending.setText("\u20B9" + String.format("%.0f", totalPending));
            lblTotalPaid.setText("\u20B9" + String.format("%.0f", totalPaid));
            lblTotalAmount.setText("\u20B9" + String.format("%.0f", totalPending + totalPaid));

            LOG.info("Loaded {} existing bills", existingBills.size());

        } catch (Exception e) {
            LOG.error("Error loading existing bills: ", e);
        }
    }

    private String getSupplierName(Integer supplierId) {
        if (supplierId == null) return "";
        for (Supplier supplier : allSuppliers) {
            if (supplier.getId().equals(supplierId)) {
                return supplier.getName();
            }
        }
        return "";
    }

    private void searchBills() {
        try {
            existingBills.clear();
            LocalDate fromDate = dpSearchFromDate.getValue();
            LocalDate toDate = dpSearchToDate.getValue();
            String billNoStr = txtSearchBillNo.getText().trim();
            String supplierSearchRaw = txtSearchSupplier.getText().trim();

            // Extract supplier name without city (remove text in parentheses)
            String supplierSearch = supplierSearchRaw;
            if (supplierSearchRaw.contains("(")) {
                supplierSearch = supplierSearchRaw.substring(0, supplierSearchRaw.indexOf("(")).trim();
            }
            supplierSearch = supplierSearch.toLowerCase();

            List<PurchaseBill> bills;
            if (fromDate != null && toDate != null) {
                bills = purchaseBillService.getBillsByDateRange(fromDate, toDate);
            } else {
                bills = purchaseBillService.getAllBills();
            }

            double totalPending = 0.0;
            double totalPaid = 0.0;

            for (PurchaseBill bill : bills) {
                // Filter by bill number
                if (!billNoStr.isEmpty()) {
                    try {
                        int searchBillNo = Integer.parseInt(billNoStr);
                        if (!bill.getBillNo().equals(searchBillNo)) continue;
                    } catch (NumberFormatException e) {
                        // Skip if invalid number
                    }
                }

                // Filter by supplier
                String supplierName = getSupplierName(bill.getPartyId());
                if (!supplierSearch.isEmpty() && !supplierName.toLowerCase().contains(supplierSearch)) {
                    continue;
                }

                String dateStr = bill.getBillDate() != null ? bill.getBillDate().format(DATE_FORMATTER) : "";
                double amount = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;

                existingBills.add(new ExistingBillData(
                        bill.getBillNo(),
                        dateStr,
                        supplierName,
                        amount,
                        bill.getStatus()
                ));

                if ("PENDING".equals(bill.getStatus())) {
                    totalPending += amount;
                } else {
                    totalPaid += amount;
                }
            }

            lblTotalPending.setText("\u20B9" + String.format("%.0f", totalPending));
            lblTotalPaid.setText("\u20B9" + String.format("%.0f", totalPaid));
            lblTotalAmount.setText("\u20B9" + String.format("%.0f", totalPending + totalPaid));

        } catch (Exception e) {
            LOG.error("Error searching bills: ", e);
        }
    }

    private void clearSearch() {
        dpSearchFromDate.setValue(LocalDate.now().minusDays(30));
        dpSearchToDate.setValue(LocalDate.now());
        txtSearchBillNo.clear();
        txtSearchSupplier.clear();
        loadExistingBills();
    }

    private void loadBillForEditing(Integer billNo) {
        try {
            PurchaseBill bill = purchaseBillService.getBillWithTransactions(billNo);
            if (bill == null) {
                alertNotification.showError("Bill not found");
                return;
            }

            editingBill = bill;

            // Set supplier
            selectedSupplier = null;
            for (Supplier supplier : allSuppliers) {
                if (supplier.getId().equals(bill.getPartyId())) {
                    selectedSupplier = supplier;
                    supplierAutoComplete.setText(supplier.getName());
                    break;
                }
            }

            // Set other fields
            dpBillDate.setValue(bill.getBillDate());
            txtReffNo.setText(bill.getReffNo() != null ? bill.getReffNo() : "");
            txtRemarks.setText(bill.getRemarks() != null ? bill.getRemarks() : "");

            // Set payment mode from saved bill
            String savedPayMode = bill.getPay() != null ? bill.getPay() : "";
            PaymentOption matchedOption = null;

            // First try to match by display name
            for (PaymentOption opt : cmbPaymentMode.getItems()) {
                if (opt.getDisplayName().equals(savedPayMode)) {
                    matchedOption = opt;
                    break;
                }
            }

            // Handle backwards compatibility: if saved value was "CASH" (old format), find the cash bank
            if (matchedOption == null && "CASH".equals(savedPayMode)) {
                matchedOption = cmbPaymentMode.getItems().stream()
                        .filter(PaymentOption::isCash)
                        .findFirst()
                        .orElse(null);
            }

            // If still no match and bankId is available, try to find by bankId
            if (matchedOption == null && bill.getBankId() != null) {
                Integer billBankId = bill.getBankId();
                for (PaymentOption opt : cmbPaymentMode.getItems()) {
                    if (opt.getBank() != null && opt.getBank().getId().equals(billBankId)) {
                        matchedOption = opt;
                        break;
                    }
                }
            }

            if (matchedOption != null) {
                cmbPaymentMode.setValue(matchedOption);
            } else {
                // Default to cash bank if nothing matched
                PaymentOption cashOption = cmbPaymentMode.getItems().stream()
                        .filter(PaymentOption::isCash)
                        .findFirst()
                        .orElse(!cmbPaymentMode.getItems().isEmpty() ? cmbPaymentMode.getItems().get(0) : null);
                if (cashOption != null) {
                    cmbPaymentMode.setValue(cashOption);
                }
            }

            txtGst.setText(bill.getGst() != null ? String.format("%.2f", bill.getGst()) : "");
            txtOtherTax.setText(bill.getOtherTax() != null ? String.format("%.2f", bill.getOtherTax()) : "");

            // Load items
            purchaseItems.clear();
            serialNumber = 1;
            for (PurchaseTransaction trans : bill.getTransactions()) {
                purchaseItems.add(new PurchaseItemData(
                        serialNumber++,
                        trans.getItemName(),
                        trans.getQty(),
                        trans.getRate(),
                        trans.getAmount(),
                        trans.getItemCode(),
                        trans.getCategoryId()
                ));
            }

            tblPurchaseItems.refresh();
            updateTotals();

            alertNotification.showInfo("Bill #" + billNo + " loaded for editing");

        } catch (Exception e) {
            LOG.error("Error loading bill for editing: ", e);
            alertNotification.showError("Error loading bill: " + e.getMessage());
        }
    }

    // ==================== Custom Font Methods ====================

    private void applyCustomFonts() {
        Font customFont = SessionService.getCustomFont();
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            Font font25 = Font.font(fontFamily, 25);
            Font font22 = Font.font(fontFamily, 22);
            Font font20 = Font.font(fontFamily, 20);

            // Apply to txtSupplier (22px - highlighted)
            txtSupplier.setFont(font22);
            txtSupplier.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #7B1FA2; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: '" + fontFamily + "'; -fx-font-size: 22px;");

            // Apply to other TextFields (20px)
            txtCategory.setFont(font20);
            txtItemName.setFont(font20);
            txtRemarks.setFont(font20);

            // Apply to Labels (25px)
            if (lblSupplier != null) lblSupplier.setFont(font25);
            if (lblCategory != null) lblCategory.setFont(font25);
            if (lblItemName != null) lblItemName.setFont(font25);
            if (lblQty != null) lblQty.setFont(font25);
            if (lblRate != null) lblRate.setFont(font25);
            if (lblAmount != null) lblAmount.setFont(font25);
            if (lblRemarks != null) lblRemarks.setFont(font25);
            if (lblSubTotalLabel != null) lblSubTotalLabel.setFont(font25);
            if (lblDiscountLabel != null) lblDiscountLabel.setFont(font25);
            if (lblGrandTotalLabel != null) lblGrandTotalLabel.setFont(font25);
        }
    }

    private void applyItemNameColumnFont() {
        Font customFont = SessionService.getCustomFont();
        String fontFamily = customFont != null ? customFont.getFamily() : "System";

        // Item Name column with custom font - 25px
        colItemName.setCellFactory(column -> {
            TableCell<PurchaseItemData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px;");
            return cell;
        });

        // Quantity column - 18px bold, right aligned
        colQty.setCellFactory(column -> {
            TableCell<PurchaseItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.0f", item));
                    }
                }
            };
            cell.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
            return cell;
        });

        // Rate column - 18px bold, right aligned
        colRate.setCellFactory(column -> {
            TableCell<PurchaseItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            };
            cell.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
            return cell;
        });

        // Amount column - 18px bold, right aligned, green color
        colAmount.setCellFactory(column -> {
            TableCell<PurchaseItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            };
            cell.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0; -fx-text-fill: #2E7D32;");
            return cell;
        });

        // Sr. No column - 14px, centered
        colSrNo.setCellFactory(column -> {
            TableCell<PurchaseItemData, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                }
            };
            cell.setStyle("-fx-font-size: 14px; -fx-alignment: CENTER;");
            return cell;
        });
    }

    private void applySupplierNameColumnFont() {
        Font customFont = SessionService.getCustomFont();
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            colSupplierName.setCellFactory(column -> {
                TableCell<ExistingBillData, String> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item);
                    }
                };
                cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                return cell;
            });
        }
    }

    // ==================== Inner Classes ====================

    public static class PurchaseItemData {
        private final SimpleIntegerProperty srNo;
        private final SimpleStringProperty itemName;
        private final SimpleFloatProperty qty;
        private final SimpleFloatProperty rate;
        private final SimpleFloatProperty amount;
        private Integer itemCode;
        private Integer categoryId;

        public PurchaseItemData(int srNo, String itemName, float qty, float rate, float amount,
                                Integer itemCode, Integer categoryId) {
            this.srNo = new SimpleIntegerProperty(srNo);
            this.itemName = new SimpleStringProperty(itemName);
            this.qty = new SimpleFloatProperty(qty);
            this.rate = new SimpleFloatProperty(rate);
            this.amount = new SimpleFloatProperty(amount);
            this.itemCode = itemCode;
            this.categoryId = categoryId;
        }

        public int getSrNo() { return srNo.get(); }
        public SimpleIntegerProperty srNoProperty() { return srNo; }
        public void setSrNo(int value) { srNo.set(value); }

        public String getItemName() { return itemName.get(); }
        public SimpleStringProperty itemNameProperty() { return itemName; }
        public void setItemName(String value) { itemName.set(value); }

        public float getQty() { return qty.get(); }
        public SimpleFloatProperty qtyProperty() { return qty; }
        public void setQty(float value) { qty.set(value); }

        public float getRate() { return rate.get(); }
        public SimpleFloatProperty rateProperty() { return rate; }
        public void setRate(float value) { rate.set(value); }

        public float getAmount() { return amount.get(); }
        public SimpleFloatProperty amountProperty() { return amount; }
        public void setAmount(float value) { amount.set(value); }

        public Integer getItemCode() { return itemCode; }
        public void setItemCode(Integer itemCode) { this.itemCode = itemCode; }

        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    }

    public static class ExistingBillData {
        private final SimpleIntegerProperty billNo;
        private final SimpleStringProperty billDate;
        private final SimpleStringProperty supplierName;
        private final SimpleDoubleProperty amount;
        private final SimpleStringProperty status;

        public ExistingBillData(int billNo, String billDate, String supplierName, double amount, String status) {
            this.billNo = new SimpleIntegerProperty(billNo);
            this.billDate = new SimpleStringProperty(billDate);
            this.supplierName = new SimpleStringProperty(supplierName);
            this.amount = new SimpleDoubleProperty(amount);
            this.status = new SimpleStringProperty(status);
        }

        public int getBillNo() { return billNo.get(); }
        public SimpleIntegerProperty billNoProperty() { return billNo; }

        public String getBillDate() { return billDate.get(); }
        public SimpleStringProperty billDateProperty() { return billDate; }

        public String getSupplierName() { return supplierName.get(); }
        public SimpleStringProperty supplierNameProperty() { return supplierName; }

        public double getAmount() { return amount.get(); }
        public SimpleDoubleProperty amountProperty() { return amount; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }

    /**
     * Payment option for dropdown - can be Cash bank (IFSC="cash"), CREDIT, or other Banks
     */
    public static class PaymentOption {
        private final String displayName;
        private final Bank bank; // null only for CREDIT option
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

        public boolean isCredit() {
            return bank == null && "CREDIT".equals(displayName);
        }

        /**
         * Check if this is a regular bank payment (not cash and not credit)
         */
        public boolean isBank() {
            return bank != null && !CASH_IFSC.equalsIgnoreCase(bank.getIfsc());
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
