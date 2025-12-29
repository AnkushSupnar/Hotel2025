package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.dto.ItemDto;
import com.frontend.entity.*;
import com.frontend.print.PurchaseOrderPrint;
import com.frontend.service.*;
import com.frontend.view.AlertNotification;
import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for Purchase Order Entry with AutoCompleteTextField
 * Two-part layout: Left side for order entry, Right side for existing orders
 */
@Component
public class PurchaseOrderController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderController.class);

    @Autowired
    private PurchaseOrderService purchaseOrderService;

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
    private PurchaseOrderPrint purchaseOrderPrint;

    @Lazy
    @Autowired
    private StageManager stageManager;

    // ==================== LEFT SIDE: Order Entry Form ====================

    // Labels with custom font
    @FXML private Label lblSupplier;
    @FXML private Label lblCategory;
    @FXML private Label lblItemName;
    @FXML private Label lblUnit;
    @FXML private Label lblQty;
    @FXML private Label lblRemarks;

    // Section 1: Supplier & Order Info
    @FXML private TextField txtSupplier;
    @FXML private DatePicker dpOrderDate;
    @FXML private Label lblItemCount;

    // Section 2: Item Entry
    @FXML private TextField txtCategory;
    @FXML private TextField txtItemName;
    @FXML private ComboBox<String> cmbUnit;
    @FXML private TextField txtQty;
    @FXML private Button btnAddItem;
    @FXML private Button btnEditItem;
    @FXML private Button btnClearFields;
    @FXML private Button btnRemoveItem;

    // Items Table
    @FXML private TableView<OrderItemData> tblOrderItems;
    @FXML private TableColumn<OrderItemData, Integer> colSrNo;
    @FXML private TableColumn<OrderItemData, String> colItemName;
    @FXML private TableColumn<OrderItemData, String> colUnit;
    @FXML private TableColumn<OrderItemData, Float> colQty;

    // Section 3: Order Summary
    @FXML private TextField txtRemarks;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalQty;

    // Action Buttons
    @FXML private Button btnSaveOrder;
    @FXML private Button btnNewOrder;
    @FXML private Button btnClearAll;
    @FXML private Button btnBack;

    // ==================== RIGHT SIDE: Existing Orders ====================

    @FXML private Button btnRefreshOrders;
    @FXML private DatePicker dpSearchFromDate;
    @FXML private DatePicker dpSearchToDate;
    @FXML private Label lblToDate;
    @FXML private TextField txtSearchOrderNo;
    @FXML private TextField txtSearchSupplier;
    @FXML private Button btnSearchOrders;
    @FXML private Button btnPrintOrder;
    @FXML private Button btnCopyOrder;
    @FXML private Button btnCreateInvoice;
    @FXML private Button btnClearSearch;

    @FXML private TableView<ExistingOrderData> tblExistingOrders;
    @FXML private TableColumn<ExistingOrderData, Integer> colOrderNo;
    @FXML private TableColumn<ExistingOrderData, String> colOrderDate;
    @FXML private TableColumn<ExistingOrderData, String> colSupplierName;
    @FXML private TableColumn<ExistingOrderData, Float> colOrderQty;
    @FXML private TableColumn<ExistingOrderData, String> colOrderStatus;

    @FXML private Label lblTotalPending;
    @FXML private Label lblTotalCompleted;
    @FXML private Label lblTotalOrders;

    // AutoComplete fields
    private AutoCompleteTextField supplierAutoComplete;
    private AutoCompleteTextField categoryAutoComplete;
    private AutoCompleteTextField itemAutoComplete;
    private AutoCompleteTextField searchSupplierAutoComplete;

    // Data
    private ObservableList<OrderItemData> orderItems = FXCollections.observableArrayList();
    private ObservableList<ExistingOrderData> existingOrders = FXCollections.observableArrayList();
    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<CategoryMasterDto> stockCategories = new ArrayList<>();
    private List<ItemDto> currentCategoryItems = new ArrayList<>();

    private Supplier selectedSupplier = null;
    private CategoryMasterDto selectedCategory = null;
    private ItemDto selectedItem = null;
    private OrderItemData editingItem = null;
    private PurchaseOrder editingOrder = null;
    private int serialNumber = 1;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String[] UNIT_OPTIONS = {"KG", "NOS", "LTR", "PCS", "BOX", "PKT", "DOZ", "GM", "ML"};

    // Static variable to pass selected order to Invoice screen
    private static Integer selectedOrderNoForInvoice = null;

    public static Integer getSelectedOrderNoForInvoice() {
        return selectedOrderNoForInvoice;
    }

    public static void clearSelectedOrderNoForInvoice() {
        selectedOrderNoForInvoice = null;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing PurchaseOrderController");
        loadData();
        setupAutoCompleteFields();
        setupItemsTable();
        setupExistingOrdersTable();
        setupEventHandlers();
        applyCustomFonts();

        dpOrderDate.setValue(LocalDate.now());
        dpSearchFromDate.setValue(LocalDate.now().minusDays(30));
        dpSearchToDate.setValue(LocalDate.now());

        // Initialize unit ComboBox
        cmbUnit.getItems().addAll(UNIT_OPTIONS);
        cmbUnit.setValue("KG"); // Default value

        loadExistingOrders();
    }

    private void loadData() {
        try {
            allSuppliers = supplierService.getActiveSuppliers();
            LOG.info("Loaded {} suppliers", allSuppliers.size());

            List<CategoryMasterDto> allCategories = categoryApiService.getAllCategories();
            stockCategories.clear();
            for (CategoryMasterDto cat : allCategories) {
                if ("Y".equalsIgnoreCase(cat.getStock())) {
                    stockCategories.add(cat);
                }
            }
            LOG.info("Loaded {} stock categories", stockCategories.size());

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

        // Search Supplier AutoComplete (same font as txtSupplier)
        searchSupplierAutoComplete = new AutoCompleteTextField(txtSearchSupplier, supplierNames, font20);
        searchSupplierAutoComplete.setUseContainsFilter(true);
    }

    private void onSupplierSelected(String selection) {
        if (selection == null || selection.isEmpty()) {
            selectedSupplier = null;
            return;
        }
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
                txtQty.requestFocus();
                break;
            }
        }
    }

    private void setupItemsTable() {
        colSrNo.setCellValueFactory(cellData -> cellData.getValue().srNoProperty().asObject());
        colItemName.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());
        colUnit.setCellValueFactory(cellData -> cellData.getValue().unitProperty());
        colQty.setCellValueFactory(cellData -> cellData.getValue().qtyProperty().asObject());

        applyTableColumnFonts();

        // Row selection for editing
        tblOrderItems.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadItemForEdit(newVal);
            }
        });

        tblOrderItems.setItems(orderItems);
    }

    private void setupExistingOrdersTable() {
        colOrderNo.setCellValueFactory(cellData -> cellData.getValue().orderNoProperty().asObject());
        colOrderDate.setCellValueFactory(cellData -> cellData.getValue().orderDateProperty());
        colSupplierName.setCellValueFactory(cellData -> cellData.getValue().supplierNameProperty());
        colOrderQty.setCellValueFactory(cellData -> cellData.getValue().totalQtyProperty().asObject());
        colOrderStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Apply custom font to supplier name column
        applySupplierNameColumnFont();

        // Status column styling
        colOrderStatus.setCellFactory(column -> new TableCell<ExistingOrderData, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("COMPLETED".equals(status)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if ("CANCELLED".equals(status)) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Double-click to edit order
        tblExistingOrders.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ExistingOrderData selected = tblExistingOrders.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadOrderForEditing(selected.getOrderNo());
                }
            }
        });

        tblExistingOrders.setItems(existingOrders);
    }

    private void loadItemForEdit(OrderItemData item) {
        editingItem = item;
        txtCategory.setText(item.getCategoryName());
        txtItemName.setText(item.getItemName());
        cmbUnit.setValue(item.getUnit() != null ? item.getUnit() : "KG");
        txtQty.setText(String.valueOf(item.getQty()));
    }

    private void setupEventHandlers() {
        btnAddItem.setOnAction(e -> addItem());
        btnEditItem.setOnAction(e -> editItem());
        btnClearFields.setOnAction(e -> clearItemFields());
        btnRemoveItem.setOnAction(e -> removeSelectedItem());
        btnClearAll.setOnAction(e -> clearAllItems());
        btnSaveOrder.setOnAction(e -> saveOrder());
        btnNewOrder.setOnAction(e -> newOrder());
        btnBack.setOnAction(e -> navigateBack());

        // Right side event handlers
        btnRefreshOrders.setOnAction(e -> loadExistingOrders());
        btnSearchOrders.setOnAction(e -> searchOrders());
        btnPrintOrder.setOnAction(e -> printSelectedOrder());
        btnCopyOrder.setOnAction(e -> copySelectedOrder());
        btnCreateInvoice.setOnAction(e -> createInvoiceFromOrder());
        btnClearSearch.setOnAction(e -> clearSearch());

        txtQty.setOnAction(e -> addItem());
    }

    private void addItem() {
        String itemName = txtItemName.getText().trim();
        if (itemName.isEmpty()) {
            alertNotification.showWarning("Please enter item name");
            return;
        }

        float qty;
        try {
            qty = Float.parseFloat(txtQty.getText());
        } catch (NumberFormatException e) {
            alertNotification.showWarning("Please enter valid quantity");
            return;
        }

        String categoryName = txtCategory.getText().trim();

        // Check for existing item
        for (OrderItemData existing : orderItems) {
            if (existing.getItemName().equals(itemName)) {
                // Allow negative quantity only for existing items
                if (qty < 0) {
                    float absQty = Math.abs(qty);
                    if (existing.getQty() < absQty) {
                        alertNotification.showWarning("Cannot reduce more than existing quantity (" + existing.getQty() + ")");
                        return;
                    }
                    float newQty = existing.getQty() + qty; // qty is negative, so this subtracts
                    if (newQty <= 0) {
                        // Remove item if quantity becomes zero or negative
                        orderItems.remove(existing);
                        renumberItems();
                    } else {
                        existing.setQty(newQty);
                    }
                } else {
                    // Positive quantity - add to existing
                    existing.setQty(existing.getQty() + qty);
                }
                tblOrderItems.refresh();
                updateTotals();
                clearItemFieldsKeepCategory();
                txtItemName.requestFocus();
                return;
            }
        }

        // New item - quantity must be positive
        if (qty <= 0) {
            alertNotification.showWarning("Quantity must be greater than 0 for new items");
            return;
        }

        String unit = cmbUnit.getValue() != null ? cmbUnit.getValue() : "KG";
        Integer itemCode = selectedItem != null ? selectedItem.getItemCode() : null;
        Integer categoryId = selectedItem != null ? selectedItem.getCategoryId() :
                            (selectedCategory != null ? selectedCategory.getId() : null);

        OrderItemData data = new OrderItemData(serialNumber++, categoryName, itemName, unit, qty, itemCode, categoryId);
        orderItems.add(data);

        tblOrderItems.refresh();
        updateTotals();
        clearItemFieldsKeepCategory();
        txtItemName.requestFocus();
    }

    private void editItem() {
        if (editingItem == null) {
            alertNotification.showWarning("Please select an item to edit");
            return;
        }

        float qty;
        try {
            qty = Float.parseFloat(txtQty.getText());
        } catch (NumberFormatException e) {
            alertNotification.showWarning("Please enter valid quantity");
            return;
        }

        editingItem.setCategoryName(txtCategory.getText().trim());
        editingItem.setItemName(txtItemName.getText().trim());
        editingItem.setUnit(cmbUnit.getValue() != null ? cmbUnit.getValue() : "KG");
        editingItem.setQty(qty);

        tblOrderItems.refresh();
        updateTotals();
        clearItemFieldsKeepCategory();
        txtItemName.requestFocus();
    }

    private void clearItemFields() {
        clearItemFieldsKeepCategory();
        // Also clear category
        categoryAutoComplete.clear();
        selectedCategory = null;
        currentCategoryItems.clear();
        itemAutoComplete.setSuggestions(new ArrayList<>());
    }

    private void clearItemFieldsKeepCategory() {
        // Clear only item, unit, qty - keep category
        itemAutoComplete.clear();
        cmbUnit.setValue("KG");
        txtQty.clear();
        selectedItem = null;
        editingItem = null;
        tblOrderItems.getSelectionModel().clearSelection();
    }

    private void removeSelectedItem() {
        OrderItemData selected = tblOrderItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showWarning("Please select an item to remove");
            return;
        }
        orderItems.remove(selected);
        renumberItems();
        updateTotals();
        clearItemFields();
    }

    private void renumberItems() {
        serialNumber = 1;
        for (OrderItemData item : orderItems) {
            item.setSrNo(serialNumber++);
        }
        tblOrderItems.refresh();
    }

    private void updateTotals() {
        double totalQty = 0.0;
        for (OrderItemData item : orderItems) {
            totalQty += item.getQty();
        }
        lblTotalItems.setText(String.valueOf(orderItems.size()));
        lblTotalQty.setText(String.format("%.0f", totalQty));
        lblItemCount.setText(String.valueOf(orderItems.size()));
    }

    private void clearAllItems() {
        orderItems.clear();
        serialNumber = 1;
        updateTotals();
        clearItemFields();
    }

    private void saveOrder() {
        if (selectedSupplier == null) {
            alertNotification.showError("Please select a supplier");
            return;
        }
        if (orderItems.isEmpty()) {
            alertNotification.showError("Please add at least one item");
            return;
        }
        if (dpOrderDate.getValue() == null) {
            alertNotification.showError("Please select an order date");
            return;
        }

        try {
            PurchaseOrder order = new PurchaseOrder();
            order.setPartyId(selectedSupplier.getId());
            order.setOrderDate(dpOrderDate.getValue());
            order.setRemarks(txtRemarks.getText().trim());
            order.setStatus("PENDING");

            List<PurchaseOrderTransaction> transactions = new ArrayList<>();
            for (OrderItemData itemData : orderItems) {
                PurchaseOrderTransaction trans = new PurchaseOrderTransaction();
                trans.setItemName(itemData.getItemName());
                trans.setUnit(itemData.getUnit());
                trans.setQty(itemData.getQty());
                trans.setItemCode(itemData.getItemCode());
                trans.setCategoryId(itemData.getCategoryId());
                trans.setCategoryName(itemData.getCategoryName());
                transactions.add(trans);
            }

            PurchaseOrder savedOrder;
            if (editingOrder != null) {
                savedOrder = purchaseOrderService.updatePurchaseOrder(editingOrder.getOrderNo(), order, transactions);
                alertNotification.showSuccess("Purchase order updated!");
            } else {
                savedOrder = purchaseOrderService.createPurchaseOrder(order, transactions);
                alertNotification.showSuccess("Order #" + savedOrder.getOrderNo() + " saved!");
            }

            // Generate and print PDF
            if (savedOrder != null) {
                // Reload order with transactions for printing
                PurchaseOrder orderToPrint = purchaseOrderService.getOrderWithTransactions(savedOrder.getOrderNo());
                if (orderToPrint != null) {
                    purchaseOrderPrint.printPurchaseOrder(orderToPrint);
                }
            }

            newOrder();
            loadExistingOrders();

        } catch (Exception e) {
            LOG.error("Error saving order: ", e);
            alertNotification.showError("Error: " + e.getMessage());
        }
    }

    private void newOrder() {
        editingOrder = null;
        supplierAutoComplete.clear();
        selectedSupplier = null;
        dpOrderDate.setValue(LocalDate.now());
        txtRemarks.clear();
        clearAllItems();
    }

    private void navigateBack() {
        try {
            BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
            if (mainPane != null) {
                javafx.scene.Node dashboard = (javafx.scene.Node) mainPane.getProperties().get("initialDashboard");
                if (dashboard != null) mainPane.setCenter(dashboard);
            }
        } catch (Exception e) {
            LOG.error("Error navigating back: ", e);
        }
    }

    // ==================== Existing Orders Methods ====================

    private void loadExistingOrders() {
        try {
            existingOrders.clear();
            List<PurchaseOrder> orders = purchaseOrderService.getAllOrders();

            int totalPending = 0;
            int totalCompleted = 0;

            for (PurchaseOrder order : orders) {
                String supplierName = getSupplierName(order.getPartyId());
                String dateStr = order.getOrderDate() != null ? order.getOrderDate().format(DATE_FORMATTER) : "";
                double qty = order.getTotalQty() != null ? order.getTotalQty() : 0.0;

                existingOrders.add(new ExistingOrderData(
                        order.getOrderNo(),
                        dateStr,
                        supplierName,
                        qty,
                        order.getStatus()
                ));

                if ("PENDING".equals(order.getStatus())) {
                    totalPending++;
                } else if ("COMPLETED".equals(order.getStatus())) {
                    totalCompleted++;
                }
            }

            lblTotalPending.setText(String.valueOf(totalPending));
            lblTotalCompleted.setText(String.valueOf(totalCompleted));
            lblTotalOrders.setText(String.valueOf(existingOrders.size()));

            LOG.info("Loaded {} existing orders", existingOrders.size());

        } catch (Exception e) {
            LOG.error("Error loading existing orders: ", e);
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

    private void searchOrders() {
        try {
            existingOrders.clear();
            LocalDate fromDate = dpSearchFromDate.getValue();
            LocalDate toDate = dpSearchToDate.getValue();
            String orderNoStr = txtSearchOrderNo.getText().trim();
            String supplierSearch = txtSearchSupplier.getText().trim().toLowerCase();

            List<PurchaseOrder> orders;
            if (fromDate != null && toDate != null) {
                orders = purchaseOrderService.getOrdersByDateRange(fromDate, toDate);
            } else {
                orders = purchaseOrderService.getAllOrders();
            }

            int totalPending = 0;
            int totalCompleted = 0;

            for (PurchaseOrder order : orders) {
                // Filter by order number
                if (!orderNoStr.isEmpty()) {
                    try {
                        int searchOrderNo = Integer.parseInt(orderNoStr);
                        if (!order.getOrderNo().equals(searchOrderNo)) continue;
                    } catch (NumberFormatException e) {
                        // Skip if invalid number
                    }
                }

                // Filter by supplier
                String supplierName = getSupplierName(order.getPartyId());
                if (!supplierSearch.isEmpty() && !supplierName.toLowerCase().contains(supplierSearch)) {
                    continue;
                }

                String dateStr = order.getOrderDate() != null ? order.getOrderDate().format(DATE_FORMATTER) : "";
                double qty = order.getTotalQty() != null ? order.getTotalQty() : 0.0;

                existingOrders.add(new ExistingOrderData(
                        order.getOrderNo(),
                        dateStr,
                        supplierName,
                        qty,
                        order.getStatus()
                ));

                if ("PENDING".equals(order.getStatus())) {
                    totalPending++;
                } else if ("COMPLETED".equals(order.getStatus())) {
                    totalCompleted++;
                }
            }

            lblTotalPending.setText(String.valueOf(totalPending));
            lblTotalCompleted.setText(String.valueOf(totalCompleted));
            lblTotalOrders.setText(String.valueOf(existingOrders.size()));

        } catch (Exception e) {
            LOG.error("Error searching orders: ", e);
        }
    }

    private void clearSearch() {
        dpSearchFromDate.setValue(LocalDate.now().minusDays(30));
        dpSearchToDate.setValue(LocalDate.now());
        txtSearchOrderNo.clear();
        searchSupplierAutoComplete.clear();
        loadExistingOrders();
    }

    private void printSelectedOrder() {
        ExistingOrderData selected = tblExistingOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showWarning("Please select an order to print");
            return;
        }

        try {
            // Load the complete order with transactions
            PurchaseOrder order = purchaseOrderService.getOrderWithTransactions(selected.getOrderNo());
            if (order == null) {
                alertNotification.showError("Order not found");
                return;
            }

            // Print the order as A4 PDF
            boolean success = purchaseOrderPrint.printPurchaseOrder(order);
            if (success) {
                alertNotification.showSuccess("Purchase Order #" + order.getOrderNo() + " printed successfully!");
            } else {
                alertNotification.showError("Failed to print Purchase Order");
            }

        } catch (Exception e) {
            LOG.error("Error printing order: ", e);
            alertNotification.showError("Error printing order: " + e.getMessage());
        }
    }

    private void copySelectedOrder() {
        ExistingOrderData selected = tblExistingOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showWarning("Please select an order to copy");
            return;
        }

        try {
            // Load the complete order with transactions
            PurchaseOrder order = purchaseOrderService.getOrderWithTransactions(selected.getOrderNo());
            if (order == null) {
                alertNotification.showError("Order not found");
                return;
            }

            // Clear current form and set as NEW order (not editing)
            editingOrder = null;

            // Set supplier from copied order
            selectedSupplier = null;
            for (Supplier supplier : allSuppliers) {
                if (supplier.getId().equals(order.getPartyId())) {
                    selectedSupplier = supplier;
                    supplierAutoComplete.setText(supplier.getName());
                    break;
                }
            }

            // Set today's date for new order
            dpOrderDate.setValue(LocalDate.now());

            // Clear remarks (user can add new remarks)
            txtRemarks.setText("");

            // Copy all items from the selected order
            orderItems.clear();
            serialNumber = 1;
            for (PurchaseOrderTransaction trans : order.getTransactions()) {
                orderItems.add(new OrderItemData(
                        serialNumber++,
                        trans.getCategoryName() != null ? trans.getCategoryName() : "",
                        trans.getItemName(),
                        trans.getUnit() != null ? trans.getUnit() : "KG",
                        trans.getQty(),
                        trans.getItemCode(),
                        trans.getCategoryId()
                ));
            }

            tblOrderItems.refresh();
            updateTotals();

            // Clear item entry fields
            clearItemFields();

            alertNotification.showSuccess("Order #" + selected.getOrderNo() + " copied! Modify items and save as new order.");

        } catch (Exception e) {
            LOG.error("Error copying order: ", e);
            alertNotification.showError("Error copying order: " + e.getMessage());
        }
    }

    private void createInvoiceFromOrder() {
        ExistingOrderData selected = tblExistingOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showWarning("Please select an order to create invoice");
            return;
        }

        try {
            // Store the selected order number for the Invoice screen
            selectedOrderNoForInvoice = selected.getOrderNo();

            // Navigate to Purchase Invoice from PO screen in center pane
            BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/transaction/PurchaseInvoiceFromPO.fxml");
                mainPane.setCenter(pane);
                LOG.info("Navigated to Purchase Invoice from PO screen for order #{}", selected.getOrderNo());
            }

        } catch (Exception e) {
            LOG.error("Error navigating to invoice screen: ", e);
            alertNotification.showError("Error: " + e.getMessage());
        }
    }

    private void loadOrderForEditing(Integer orderNo) {
        try {
            PurchaseOrder order = purchaseOrderService.getOrderWithTransactions(orderNo);
            if (order == null) {
                alertNotification.showError("Order not found");
                return;
            }

            editingOrder = order;

            // Set supplier
            selectedSupplier = null;
            for (Supplier supplier : allSuppliers) {
                if (supplier.getId().equals(order.getPartyId())) {
                    selectedSupplier = supplier;
                    supplierAutoComplete.setText(supplier.getName());
                    break;
                }
            }

            // Set other fields
            dpOrderDate.setValue(order.getOrderDate());
            txtRemarks.setText(order.getRemarks() != null ? order.getRemarks() : "");

            // Load items
            orderItems.clear();
            serialNumber = 1;
            for (PurchaseOrderTransaction trans : order.getTransactions()) {
                orderItems.add(new OrderItemData(
                        serialNumber++,
                        trans.getCategoryName() != null ? trans.getCategoryName() : "",
                        trans.getItemName(),
                        trans.getUnit() != null ? trans.getUnit() : "KG",
                        trans.getQty(),
                        trans.getItemCode(),
                        trans.getCategoryId()
                ));
            }

            tblOrderItems.refresh();
            updateTotals();

            alertNotification.showInfo("Order #" + orderNo + " loaded for editing");

        } catch (Exception e) {
            LOG.error("Error loading order for editing: ", e);
            alertNotification.showError("Error loading order: " + e.getMessage());
        }
    }

    // ==================== Custom Font Methods ====================

    private void applyCustomFonts() {
        Font customFont = SessionService.getCustomFont();
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            Font font25 = Font.font(fontFamily, 25);
            Font font20 = Font.font(fontFamily, 20);

            // Apply to TextFields (20px) - set both font and style
            txtSupplier.setFont(font20);
            txtSupplier.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");

            txtCategory.setFont(font20);
            txtCategory.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");

            txtItemName.setFont(font20);
            txtItemName.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");

            txtRemarks.setFont(font20);
            txtRemarks.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");

            // Apply English font to Qty field (20px) - not custom font
            if (txtQty != null) {
                txtQty.setFont(Font.font("Segoe UI", 20));
                txtQty.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");
            }

            // Apply to Labels (25px)
            if (lblSupplier != null) {
                lblSupplier.setFont(font25);
                lblSupplier.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-text-fill: #00897B; -fx-font-weight: bold;");
            }
            if (lblCategory != null) {
                lblCategory.setFont(font25);
                lblCategory.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-text-fill: #00897B; -fx-font-weight: bold;");
            }
            if (lblItemName != null) {
                lblItemName.setFont(font25);
                lblItemName.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-text-fill: #00897B; -fx-font-weight: bold;");
            }
            if (lblUnit != null) {
                lblUnit.setFont(font25);
                lblUnit.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-text-fill: #616161;");
            }
            if (lblQty != null) {
                lblQty.setFont(font25);
                lblQty.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-text-fill: #616161;");
            }
            if (lblRemarks != null) {
                lblRemarks.setFont(font25);
                lblRemarks.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px; -fx-text-fill: #616161;");
            }
        }
    }

    private void applyTableColumnFonts() {
        Font customFont = SessionService.getCustomFont();
        String fontFamily = customFont != null ? customFont.getFamily() : "Segoe UI";

        // Sr. No column - English font
        colSrNo.setCellFactory(column -> {
            TableCell<OrderItemData, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-alignment: CENTER;");
            return cell;
        });

        // Item name column - Custom font
        colItemName.setCellFactory(column -> {
            TableCell<OrderItemData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 22px;");
            return cell;
        });

        // Unit column - English font
        colUnit.setCellFactory(column -> {
            TableCell<OrderItemData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: CENTER;");
            return cell;
        });

        // Quantity column - English font
        colQty.setCellFactory(column -> {
            TableCell<OrderItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%.0f", item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: CENTER;");
            return cell;
        });
    }

    private void applySupplierNameColumnFont() {
        Font customFont = SessionService.getCustomFont();
        String fontFamily = customFont != null ? customFont.getFamily() : "Segoe UI";

        // Order No column
        colOrderNo.setCellFactory(column -> {
            TableCell<ExistingOrderData, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: CENTER;");
            return cell;
        });

        // Order Date column
        colOrderDate.setCellFactory(column -> {
            TableCell<ExistingOrderData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-alignment: CENTER;");
            return cell;
        });

        // Supplier Name column - Custom font
        colSupplierName.setCellFactory(column -> {
            TableCell<ExistingOrderData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 16px;");
            return cell;
        });

        // Order Qty column
        colOrderQty.setCellFactory(column -> {
            TableCell<ExistingOrderData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%.0f", item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: CENTER;");
            return cell;
        });

        // Apply styles to search UI elements
        applySearchUIStyles();
    }

    private void applySearchUIStyles() {
        Font customFont = SessionService.getCustomFont();
        String fontFamily = customFont != null ? customFont.getFamily() : "Segoe UI";

        // Search text fields
        if (txtSearchOrderNo != null) {
            txtSearchOrderNo.setFont(Font.font("Segoe UI", 18));
            txtSearchOrderNo.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");
        }
        // txtSearchSupplier uses AutoCompleteTextField with font20 (same as txtSupplier)

        // Date pickers - 14px font
        if (dpSearchFromDate != null) {
            dpSearchFromDate.setStyle("-fx-font-size: 14px; -fx-background-radius: 4;");
        }
        if (dpSearchToDate != null) {
            dpSearchToDate.setStyle("-fx-font-size: 14px; -fx-background-radius: 4;");
        }
        // "To" label - 14px font
        if (lblToDate != null) {
            lblToDate.setStyle("-fx-text-fill: #757575; -fx-font-size: 14px;");
        }
    }

    // ==================== Inner Classes ====================

    public static class OrderItemData {
        private final SimpleIntegerProperty srNo;
        private final SimpleStringProperty categoryName;
        private final SimpleStringProperty itemName;
        private final SimpleStringProperty unit;
        private final SimpleFloatProperty qty;
        private Integer itemCode;
        private Integer categoryId;

        public OrderItemData(int srNo, String categoryName, String itemName, String unit, float qty,
                             Integer itemCode, Integer categoryId) {
            this.srNo = new SimpleIntegerProperty(srNo);
            this.categoryName = new SimpleStringProperty(categoryName);
            this.itemName = new SimpleStringProperty(itemName);
            this.unit = new SimpleStringProperty(unit);
            this.qty = new SimpleFloatProperty(qty);
            this.itemCode = itemCode;
            this.categoryId = categoryId;
        }

        public int getSrNo() { return srNo.get(); }
        public SimpleIntegerProperty srNoProperty() { return srNo; }
        public void setSrNo(int value) { srNo.set(value); }

        public String getCategoryName() { return categoryName.get(); }
        public SimpleStringProperty categoryNameProperty() { return categoryName; }
        public void setCategoryName(String value) { categoryName.set(value); }

        public String getItemName() { return itemName.get(); }
        public SimpleStringProperty itemNameProperty() { return itemName; }
        public void setItemName(String value) { itemName.set(value); }

        public String getUnit() { return unit.get(); }
        public SimpleStringProperty unitProperty() { return unit; }
        public void setUnit(String value) { unit.set(value); }

        public float getQty() { return qty.get(); }
        public SimpleFloatProperty qtyProperty() { return qty; }
        public void setQty(float value) { qty.set(value); }

        public Integer getItemCode() { return itemCode; }
        public void setItemCode(Integer itemCode) { this.itemCode = itemCode; }

        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    }

    public static class ExistingOrderData {
        private final SimpleIntegerProperty orderNo;
        private final SimpleStringProperty orderDate;
        private final SimpleStringProperty supplierName;
        private final SimpleFloatProperty totalQty;
        private final SimpleStringProperty status;

        public ExistingOrderData(int orderNo, String orderDate, String supplierName, double totalQty, String status) {
            this.orderNo = new SimpleIntegerProperty(orderNo);
            this.orderDate = new SimpleStringProperty(orderDate);
            this.supplierName = new SimpleStringProperty(supplierName);
            this.totalQty = new SimpleFloatProperty((float) totalQty);
            this.status = new SimpleStringProperty(status);
        }

        public int getOrderNo() { return orderNo.get(); }
        public SimpleIntegerProperty orderNoProperty() { return orderNo; }

        public String getOrderDate() { return orderDate.get(); }
        public SimpleStringProperty orderDateProperty() { return orderDate; }

        public String getSupplierName() { return supplierName.get(); }
        public SimpleStringProperty supplierNameProperty() { return supplierName; }

        public double getTotalQty() { return totalQty.get(); }
        public SimpleFloatProperty totalQtyProperty() { return totalQty; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
