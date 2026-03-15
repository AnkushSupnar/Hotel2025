package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.*;
import com.frontend.service.*;
import com.frontend.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for creating Purchase Invoice from Purchase Order
 * Features:
 * - Row selection based editing (no double-click required)
 * - Press Enter to update and auto-select next item
 * - Custom font support from SessionService
 * - Loads in center pane like Purchase Order screen
 */
@Component
public class PurchaseInvoiceFromPOController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseInvoiceFromPOController.class);

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private PurchaseBillService purchaseBillService;

    @Autowired
    private BankService bankService;

    @Autowired
    private BankTransactionService bankTransactionService;

    // Header
    @FXML private Label lblOrderNo;
    @FXML private Label lblOrderDate;
    @FXML private Label lblSupplierLabel;
    @FXML private Label lblSupplierName;
    @FXML private DatePicker dpInvoiceDate;
    @FXML private TextField txtRefNo;

    // Items Table
    @FXML private TableView<InvoiceItemData> tblItems;
    @FXML private TableColumn<InvoiceItemData, Integer> colSrNo;
    @FXML private TableColumn<InvoiceItemData, String> colItemName;
    @FXML private TableColumn<InvoiceItemData, String> colUnit;
    @FXML private TableColumn<InvoiceItemData, Float> colOrderedQty;
    @FXML private TableColumn<InvoiceItemData, Float> colReceivedQty;
    @FXML private TableColumn<InvoiceItemData, Float> colRate;
    @FXML private TableColumn<InvoiceItemData, Float> colAmount;
    @FXML private TableColumn<InvoiceItemData, Boolean> colStatus;
    @FXML private Label lblProgress;

    // Item Entry Section
    @FXML private Label lblSelectedItem;
    @FXML private Label lblSelectedUnit;
    @FXML private Label lblSelectedOrderedQty;
    @FXML private TextField txtReceivedQty;
    @FXML private TextField txtRate;
    @FXML private Label lblItemAmount;
    @FXML private Button btnUpdateItem;

    // Payment Section
    @FXML private ComboBox<String> cmbPaymentStatus;
    @FXML private ComboBox<PaymentOption> cmbPaymentMode;
    // cmbBank removed - using unified PaymentOption dropdown
    @FXML private Label lblBankLabel;
    @FXML private VBox vboxBank;
    @FXML private Label lblRemarks;
    @FXML private TextField txtRemarks;
    @FXML private TextField txtGst;

    // Summary
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalQty;
    @FXML private Label lblSubTotal;
    @FXML private Label lblNetAmount;

    // Buttons
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Button btnBack;

    // Layout containers - SplitPane for adjustable panels
    @FXML private SplitPane splitPaneMain;
    @FXML private VBox vboxLeftPanel;

    // Data
    private ObservableList<InvoiceItemData> invoiceItems = FXCollections.observableArrayList();
    private PurchaseOrder currentOrder;
    private List<Bank> allBanks = new ArrayList<>();
    private InvoiceItemData selectedItem = null;
    private int selectedIndex = -1;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing PurchaseInvoiceFromPOController");

        setupLayoutBinding();
        setupTable();
        setupComboBoxes();
        setupItemEntry();
        setupEventHandlers();
        applyCustomFonts();

        // Load the selected order
        loadSelectedOrder();
    }

    /**
     * Setup SplitPane with default divider position (40:60 ratio)
     * User can drag divider to adjust panel sizes
     */
    private void setupLayoutBinding() {
        if (splitPaneMain != null) {
            // Set default divider position to 40%
            splitPaneMain.setDividerPositions(0.4);
            LOG.info("SplitPane initialized with 40:60 ratio - user can adjust by dragging divider");
        }
    }

    private void setupTable() {
        tblItems.setItems(invoiceItems);

        // Sr. No column
        colSrNo.setCellValueFactory(data -> data.getValue().srNoProperty().asObject());

        // Item Name column
        colItemName.setCellValueFactory(data -> data.getValue().itemNameProperty());

        // Unit column
        colUnit.setCellValueFactory(data -> data.getValue().unitProperty());

        // Ordered Qty column
        colOrderedQty.setCellValueFactory(data -> data.getValue().orderedQtyProperty().asObject());

        // Received Qty column
        colReceivedQty.setCellValueFactory(data -> data.getValue().receivedQtyProperty().asObject());

        // Rate column
        colRate.setCellValueFactory(data -> data.getValue().rateProperty().asObject());

        // Amount column
        colAmount.setCellValueFactory(data -> data.getValue().amountProperty().asObject());

        // Status column - show checkmark if rate is entered
        colStatus.setCellValueFactory(data -> data.getValue().completedProperty().asObject());
        colStatus.setCellFactory(column -> new TableCell<InvoiceItemData, Boolean>() {
            @Override
            protected void updateItem(Boolean completed, boolean empty) {
                super.updateItem(completed, empty);
                if (empty || completed == null) {
                    setGraphic(null);
                } else if (completed) {
                    FontAwesomeIcon icon = new FontAwesomeIcon();
                    icon.setGlyphName("CHECK_CIRCLE");
                    icon.setFill(Color.web("#4CAF50"));
                    icon.setSize("14px");
                    setGraphic(icon);
                } else {
                    FontAwesomeIcon icon = new FontAwesomeIcon();
                    icon.setGlyphName("CIRCLE_THIN");
                    icon.setFill(Color.web("#BDBDBD"));
                    icon.setSize("14px");
                    setGraphic(icon);
                }
            }
        });

        // Apply custom fonts to table columns
        applyTableColumnFonts();

        // Row selection listener
        tblItems.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadItemForEditing(newVal);
            }
        });
    }

    private void setupComboBoxes() {
        // Payment Status
        cmbPaymentStatus.setItems(FXCollections.observableArrayList("PAID", "DUE"));
        cmbPaymentStatus.setValue("PAID"); // Default to PAID since we're creating invoice

        // Setup Payment Mode dropdown with all banks (including cash bank with IFSC "cash")
        setupPaymentModeComboBox();

        // Hide bank selector VBox since we're using unified dropdown
        if (vboxBank != null) {
            vboxBank.setVisible(false);
            vboxBank.setManaged(false);
        }

        // Set default date
        dpInvoiceDate.setValue(LocalDate.now());

        // GST listener
        txtGst.textProperty().addListener((obs, oldVal, newVal) -> updateTotals());
    }

    private void setupPaymentModeComboBox() {
        ObservableList<PaymentOption> paymentOptions = FXCollections.observableArrayList();
        PaymentOption cashBankOption = null;

        // Load all active banks
        try {
            allBanks = bankService.getActiveBanks();
        } catch (Exception e) {
            LOG.error("Error loading banks", e);
            allBanks = new ArrayList<>();
        }

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

        // Get custom font
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
                        // Credit - orange color with English font 14px
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
                        // Credit - English font 14px
                        setStyle("-fx-font-size: 14px; -fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        // Other banks - custom font
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #1565C0;");
                    }
                }
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

    private void setupItemEntry() {
        // Real-time amount calculation as user types
        txtReceivedQty.textProperty().addListener((obs, oldVal, newVal) -> calculateItemAmount());
        txtRate.textProperty().addListener((obs, oldVal, newVal) -> calculateItemAmount());

        // Enter key on rate field triggers update
        txtRate.setOnAction(e -> updateItemAndSelectNext());
        txtReceivedQty.setOnAction(e -> txtRate.requestFocus());

        // Update button
        btnUpdateItem.setOnAction(e -> updateItemAndSelectNext());

        // Initial state
        clearItemEntry();
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveInvoice());
        btnCancel.setOnAction(e -> navigateBack());
        btnBack.setOnAction(e -> navigateBack());
    }

    private void loadSelectedOrder() {
        Integer orderNo = PurchaseOrderController.getSelectedOrderNoForInvoice();
        if (orderNo == null) {
            alertNotification.showError("No order selected");
            navigateBack();
            return;
        }

        try {
            currentOrder = purchaseOrderService.getOrderWithTransactions(orderNo);
            if (currentOrder == null) {
                alertNotification.showError("Order not found");
                navigateBack();
                return;
            }

            // Display order info
            lblOrderNo.setText(String.valueOf(currentOrder.getOrderNo()));
            lblOrderDate.setText(currentOrder.getOrderDate() != null ?
                    currentOrder.getOrderDate().format(DATE_FORMATTER) : "-");
            lblSupplierName.setText(currentOrder.getSupplierName());

            // Load items
            invoiceItems.clear();
            int srNo = 1;
            for (PurchaseOrderTransaction trans : currentOrder.getTransactions()) {
                invoiceItems.add(new InvoiceItemData(
                        srNo++,
                        trans.getItemName(),
                        trans.getUnit() != null ? trans.getUnit() : "KG",
                        trans.getQty().floatValue(),
                        trans.getQty().floatValue(), // Default received = ordered
                        0f, // Rate to be entered
                        trans.getItemCode(),
                        trans.getCategoryId()
                ));
            }

            updateTotals();
            updateProgress();
            LOG.info("Loaded order #{} with {} items", orderNo, invoiceItems.size());

            // Auto-select first item
            if (!invoiceItems.isEmpty()) {
                tblItems.getSelectionModel().selectFirst();
                txtRate.requestFocus();
            }

            // Clear the static variable
            PurchaseOrderController.clearSelectedOrderNoForInvoice();

        } catch (Exception e) {
            LOG.error("Error loading order", e);
            alertNotification.showError("Error loading order: " + e.getMessage());
        }
    }

    private void loadItemForEditing(InvoiceItemData item) {
        selectedItem = item;
        selectedIndex = invoiceItems.indexOf(item);

        // Update labels
        lblSelectedItem.setText(item.getItemName());
        lblSelectedUnit.setText("Unit: " + item.getUnit());
        lblSelectedOrderedQty.setText("Ordered: " + String.format("%.0f", item.getOrderedQty()));

        // Set input fields
        txtReceivedQty.setText(String.format("%.0f", item.getReceivedQty()));
        txtRate.setText(item.getRate() > 0 ? String.format("%.2f", item.getRate()) : "");

        calculateItemAmount();

        // Always focus on rate field when row is clicked
        txtRate.requestFocus();
        txtRate.selectAll();
    }

    private void calculateItemAmount() {
        float qty = 0f;
        float rate = 0f;

        try {
            String qtyText = txtReceivedQty.getText().trim();
            if (!qtyText.isEmpty()) {
                qty = Float.parseFloat(qtyText);
            }
        } catch (NumberFormatException ignored) {}

        try {
            String rateText = txtRate.getText().trim();
            if (!rateText.isEmpty()) {
                rate = Float.parseFloat(rateText);
            }
        } catch (NumberFormatException ignored) {}

        float amount = qty * rate;
        lblItemAmount.setText(String.format("₹ %.2f", amount));
    }

    private void updateItemAndSelectNext() {
        if (selectedItem == null) {
            alertNotification.showWarning("Please select an item first");
            return;
        }

        // Parse values
        float receivedQty = 0f;
        float rate = 0f;

        try {
            String qtyText = txtReceivedQty.getText().trim();
            if (!qtyText.isEmpty()) {
                receivedQty = Float.parseFloat(qtyText);
            }
        } catch (NumberFormatException e) {
            alertNotification.showWarning("Invalid received quantity");
            txtReceivedQty.requestFocus();
            return;
        }

        try {
            String rateText = txtRate.getText().trim();
            if (!rateText.isEmpty()) {
                rate = Float.parseFloat(rateText);
            }
        } catch (NumberFormatException e) {
            alertNotification.showWarning("Invalid rate");
            txtRate.requestFocus();
            return;
        }

        if (rate <= 0) {
            alertNotification.showWarning("Please enter a valid rate");
            txtRate.requestFocus();
            return;
        }

        // Update item
        selectedItem.setReceivedQty(receivedQty);
        selectedItem.setRate(rate);
        selectedItem.calculateAmount();
        selectedItem.setCompleted(true);

        tblItems.refresh();
        updateTotals();
        updateProgress();

        // Select next incomplete item
        selectNextIncompleteItem();
    }

    private void selectNextIncompleteItem() {
        // Find next item without rate (starting from current position)
        for (int i = selectedIndex + 1; i < invoiceItems.size(); i++) {
            if (invoiceItems.get(i).getRate() <= 0) {
                tblItems.getSelectionModel().select(i);
                tblItems.scrollTo(i);
                return;
            }
        }

        // If no incomplete items after current, check from beginning
        for (int i = 0; i < selectedIndex; i++) {
            if (invoiceItems.get(i).getRate() <= 0) {
                tblItems.getSelectionModel().select(i);
                tblItems.scrollTo(i);
                return;
            }
        }

        // All items complete
        clearItemEntry();
        alertNotification.showSuccess("All items have been entered! Ready to save.");
        btnSave.requestFocus();
    }

    private void clearItemEntry() {
        selectedItem = null;
        selectedIndex = -1;
        lblSelectedItem.setText("- Select an item -");
        lblSelectedUnit.setText("Unit: -");
        lblSelectedOrderedQty.setText("Ordered: -");
        txtReceivedQty.clear();
        txtRate.clear();
        lblItemAmount.setText("₹ 0.00");
    }

    private void updateProgress() {
        int completed = 0;
        for (InvoiceItemData item : invoiceItems) {
            if (item.getRate() > 0) {
                completed++;
            }
        }
        lblProgress.setText(completed + " / " + invoiceItems.size() + " completed");
    }

    private void updateTotals() {
        int totalItems = invoiceItems.size();
        float totalQty = 0f;
        float subTotal = 0f;

        for (InvoiceItemData item : invoiceItems) {
            totalQty += item.getReceivedQty();
            subTotal += item.getAmount();
        }

        float gst = 0f;
        try {
            String gstText = txtGst.getText().trim();
            if (!gstText.isEmpty()) {
                gst = Float.parseFloat(gstText);
            }
        } catch (NumberFormatException ignored) {}

        float netAmount = subTotal + gst;

        lblTotalItems.setText(String.valueOf(totalItems));
        lblTotalQty.setText(String.format("%.0f", totalQty));
        lblSubTotal.setText(String.format("₹ %.2f", subTotal));
        lblNetAmount.setText(String.format("₹ %.2f", netAmount));
    }

    private void saveInvoice() {
        // Validate
        if (invoiceItems.isEmpty()) {
            alertNotification.showWarning("No items to save");
            return;
        }

        // Check if all items have rate
        boolean allRatesEntered = true;
        for (InvoiceItemData item : invoiceItems) {
            if (item.getRate() <= 0) {
                allRatesEntered = false;
                break;
            }
        }

        if (!allRatesEntered) {
            alertNotification.showWarning("Please enter rate for all items");
            return;
        }

        try {
            // Get selected payment option
            PaymentOption selectedPayment = cmbPaymentMode.getValue();
            if (selectedPayment == null) {
                alertNotification.showError("Please select a payment mode");
                return;
            }

            // Create PurchaseBill
            PurchaseBill bill = new PurchaseBill();
            bill.setPartyId(currentOrder.getPartyId());
            bill.setBillDate(dpInvoiceDate.getValue());
            bill.setReffNo(txtRefNo.getText().trim());

            // Store the display name (bank name or "CREDIT") in pay field
            String paymentMode = selectedPayment.getDisplayName();
            bill.setPay(paymentMode);

            // All payments except CREDIT have a bankId (including cash bank)
            Integer bankId = selectedPayment.getBank() != null ? selectedPayment.getBank().getId() : null;
            bill.setBankId(bankId);

            // GST
            float gst = 0f;
            try {
                String gstText = txtGst.getText().trim();
                if (!gstText.isEmpty()) {
                    gst = Float.parseFloat(gstText);
                }
            } catch (NumberFormatException ignored) {}
            bill.setGst((double) gst);
            bill.setOtherTax(0.0); // Set otherTax to 0 (consistent with PurchaseBillController)

            bill.setRemarks(txtRemarks.getText().trim());
            // Set status: PENDING for CREDIT, otherwise based on payment status dropdown
            if (selectedPayment.isCredit()) {
                bill.setStatus("PENDING");
            } else {
                bill.setStatus("PAID".equals(cmbPaymentStatus.getValue()) ? "PAID" : "PENDING");
            }

            // Create transactions
            List<PurchaseTransaction> transactions = new ArrayList<>();
            for (InvoiceItemData item : invoiceItems) {
                if (item.getReceivedQty() > 0) {
                    PurchaseTransaction trans = new PurchaseTransaction();
                    trans.setItemName(item.getItemName());
                    trans.setQty(item.getReceivedQty());
                    trans.setRate(item.getRate());
                    trans.setAmount(item.getAmount());
                    trans.setItemCode(item.getItemCode());
                    trans.setCategoryId(item.getCategoryId());
                    transactions.add(trans);
                }
            }

            // Save
            PurchaseBill savedBill = purchaseBillService.createPurchaseBill(bill, transactions);

            // Update purchase order status
            currentOrder.setStatus("COMPLETED");
            purchaseOrderService.saveOrder(currentOrder);

            // Record bank transaction (withdrawal) if bank payment is selected
            if (bill.getBankId() != null && savedBill.getNetAmount() != null && savedBill.getNetAmount() > 0) {
                try {
                    String particulars = "Purchase Invoice #" + savedBill.getBillNo();
                    if (currentOrder.getSupplierName() != null && !currentOrder.getSupplierName().isEmpty()) {
                        particulars += " (" + currentOrder.getSupplierName() + ")";
                    }
                    String remarks = "Purchase-Invoice-Bill-no-" + savedBill.getBillNo();
                    bankTransactionService.recordWithdrawal(
                            bill.getBankId(),
                            savedBill.getNetAmount(),
                            particulars,
                            "PURCHASE",
                            savedBill.getBillNo(),
                            remarks);
                    LOG.info("Bank withdrawal recorded for purchase invoice #{}: Amount={}",
                            savedBill.getBillNo(), savedBill.getNetAmount());
                } catch (Exception e) {
                    LOG.error("Error recording bank transaction for purchase invoice: ", e);
                    alertNotification.showWarning("Invoice saved but bank transaction failed: " + e.getMessage());
                }
            }

            alertNotification.showSuccess("Invoice #" + savedBill.getBillNo() + " created successfully!");
            navigateBack();

        } catch (Exception e) {
            LOG.error("Error saving invoice", e);
            alertNotification.showError("Error saving invoice: " + e.getMessage());
        }
    }

    private void navigateBack() {
        PurchaseOrderController.clearSelectedOrderNoForInvoice();
        try {
            // Navigate back to Purchase Menu
            BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/transaction/PurchaseMenu.fxml");
                mainPane.setCenter(pane);
            }
        } catch (Exception e) {
            LOG.error("Error navigating back", e);
        }
    }

    private void applyCustomFonts() {
        Font customFont = SessionService.getCustomFont();
        if (customFont != null) {
            String fontFamily = customFont.getFamily();

            // Supplier labels
            lblSupplierLabel.setFont(Font.font(fontFamily, 16));
            lblSupplierLabel.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 16px; -fx-text-fill: #1976D2; -fx-font-weight: bold;");

            lblSupplierName.setFont(Font.font(fontFamily, 20));
            lblSupplierName.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #333;");

            // Selected item label - 20px for item name
            lblSelectedItem.setFont(Font.font(fontFamily, 20));
            lblSelectedItem.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px; -fx-text-fill: #333; -fx-font-weight: bold;");

            // Remarks
            lblRemarks.setFont(Font.font(fontFamily, 22));
            lblRemarks.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 22px; -fx-text-fill: #9E9E9E;");

            txtRemarks.setFont(Font.font(fontFamily, 22));
            txtRemarks.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 22px;");
        }
    }

    private void applyTableColumnFonts() {
        Font customFont = SessionService.getCustomFont();
        String fontFamily = customFont != null ? customFont.getFamily() : "Segoe UI";

        // Item Name column with custom font - 20px size
        colItemName.setCellFactory(column -> {
            TableCell<InvoiceItemData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
            return cell;
        });

        // Numeric columns with standard font
        colSrNo.setCellFactory(column -> {
            TableCell<InvoiceItemData, Integer> cell = new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-alignment: CENTER;");
            return cell;
        });

        colUnit.setCellFactory(column -> {
            TableCell<InvoiceItemData, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-alignment: CENTER;");
            return cell;
        });

        colOrderedQty.setCellFactory(column -> {
            TableCell<InvoiceItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%.0f", item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-alignment: CENTER;");
            return cell;
        });

        colReceivedQty.setCellFactory(column -> {
            TableCell<InvoiceItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%.0f", item));
                }
            };
            cell.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-alignment: CENTER;");
            return cell;
        });

        colRate.setCellFactory(column -> {
            TableCell<InvoiceItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item <= 0) {
                        setText("-");
                        setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #BDBDBD; -fx-alignment: CENTER;");
                    } else {
                        setText(String.format("%.2f", item));
                        setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-alignment: CENTER;");
                    }
                }
            };
            return cell;
        });

        colAmount.setCellFactory(column -> {
            TableCell<InvoiceItemData, Float> cell = new TableCell<>() {
                @Override
                protected void updateItem(Float item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item <= 0) {
                        setText("-");
                        setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #BDBDBD; -fx-alignment: CENTER;");
                    } else {
                        setText(String.format("%.2f", item));
                        setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2; -fx-alignment: CENTER;");
                    }
                }
            };
            return cell;
        });
    }

    // ==================== Inner class for PaymentOption ====================

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

    // ==================== Inner class for table data ====================

    public static class InvoiceItemData {
        private final IntegerProperty srNo;
        private final StringProperty itemName;
        private final StringProperty unit;
        private final FloatProperty orderedQty;
        private final FloatProperty receivedQty;
        private final FloatProperty rate;
        private final FloatProperty amount;
        private final BooleanProperty completed;
        private final Integer itemCode;
        private final Integer categoryId;

        public InvoiceItemData(int srNo, String itemName, String unit, float orderedQty,
                               float receivedQty, float rate, Integer itemCode, Integer categoryId) {
            this.srNo = new SimpleIntegerProperty(srNo);
            this.itemName = new SimpleStringProperty(itemName);
            this.unit = new SimpleStringProperty(unit);
            this.orderedQty = new SimpleFloatProperty(orderedQty);
            this.receivedQty = new SimpleFloatProperty(receivedQty);
            this.rate = new SimpleFloatProperty(rate);
            this.amount = new SimpleFloatProperty(receivedQty * rate);
            this.completed = new SimpleBooleanProperty(rate > 0);
            this.itemCode = itemCode;
            this.categoryId = categoryId;
        }

        public void calculateAmount() {
            this.amount.set(this.receivedQty.get() * this.rate.get());
        }

        // Property getters
        public IntegerProperty srNoProperty() { return srNo; }
        public StringProperty itemNameProperty() { return itemName; }
        public StringProperty unitProperty() { return unit; }
        public FloatProperty orderedQtyProperty() { return orderedQty; }
        public FloatProperty receivedQtyProperty() { return receivedQty; }
        public FloatProperty rateProperty() { return rate; }
        public FloatProperty amountProperty() { return amount; }
        public BooleanProperty completedProperty() { return completed; }

        // Value getters
        public int getSrNo() { return srNo.get(); }
        public String getItemName() { return itemName.get(); }
        public String getUnit() { return unit.get(); }
        public float getOrderedQty() { return orderedQty.get(); }
        public float getReceivedQty() { return receivedQty.get(); }
        public float getRate() { return rate.get(); }
        public float getAmount() { return amount.get(); }
        public boolean isCompleted() { return completed.get(); }
        public Integer getItemCode() { return itemCode; }
        public Integer getCategoryId() { return categoryId; }

        // Setters
        public void setReceivedQty(float qty) { this.receivedQty.set(qty); }
        public void setRate(float rate) { this.rate.set(rate); }
        public void setCompleted(boolean completed) { this.completed.set(completed); }
    }
}
