package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Bank;
import com.frontend.entity.BillPayment;
import com.frontend.entity.PurchaseBill;
import com.frontend.entity.Supplier;
import com.frontend.print.PayReceiptPrint;
import com.frontend.service.*;
import com.frontend.view.AlertNotification;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import java.util.stream.Collectors;

/**
 * Controller for Pay Receipt Frame
 * Handles payment of pending purchase bills with partial payment support
 */
@Component
public class PayReceiptController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PayReceiptController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ============= Services =============
    @Autowired private BillPaymentService billPaymentService;
    @Autowired private PurchaseBillService purchaseBillService;
    @Autowired private SupplierService supplierService;
    @Autowired private BankService bankService;
    @Autowired private AlertNotification alertNotification;
    @Autowired private SpringFXMLLoader loader;
    @Autowired private PayReceiptPrint payReceiptPrint;

    // ============= Header =============
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;

    // ============= Supplier Section =============
    @FXML private Label lblSupplierLabel;
    @FXML private TextField txtSupplier;
    @FXML private Label lblTotalPending;

    // ============= Pending Bills Table =============
    @FXML private TableView<PendingBillRow> tblPendingBills;
    @FXML private TableColumn<PendingBillRow, Integer> colBillNo;
    @FXML private TableColumn<PendingBillRow, String> colBillDate;
    @FXML private TableColumn<PendingBillRow, String> colReffNo;
    @FXML private TableColumn<PendingBillRow, Double> colNetAmount;
    @FXML private TableColumn<PendingBillRow, Double> colPaidAmount;
    @FXML private TableColumn<PendingBillRow, Double> colBalanceAmount;
    @FXML private TableColumn<PendingBillRow, String> colStatus;

    // ============= Payment Section =============
    @FXML private HBox hboxSelectedBill;
    @FXML private Label lblSelectedBillNo;
    @FXML private Label lblSelectedBillAmount;
    @FXML private Label lblSelectedBalance;
    @FXML private TextField txtPaymentAmount;
    @FXML private ComboBox<PaymentOption> cmbPaymentMode;
    @FXML private TextField txtChequeNo;
    @FXML private TextField txtReferenceNo;
    @FXML private TextField txtRemarks;
    @FXML private Button btnPay;
    @FXML private Button btnClear;
    @FXML private Button btnNew;

    // ============= History Section =============
    @FXML private Button btnRefreshHistory;
    @FXML private DatePicker dpHistoryFrom;
    @FXML private DatePicker dpHistoryTo;
    @FXML private TextField txtHistorySearch;
    @FXML private Button btnSearchHistory;
    @FXML private Button btnClearSearch;
    @FXML private Button btnPrint;
    @FXML private TableView<PaymentHistoryRow> tblPaymentHistory;
    @FXML private TableColumn<PaymentHistoryRow, Integer> colHistId;
    @FXML private TableColumn<PaymentHistoryRow, String> colHistDate;
    @FXML private TableColumn<PaymentHistoryRow, Integer> colHistBillNo;
    @FXML private TableColumn<PaymentHistoryRow, String> colHistSupplier;
    @FXML private TableColumn<PaymentHistoryRow, Double> colHistAmount;
    @FXML private TableColumn<PaymentHistoryRow, String> colHistMode;
    @FXML private Label lblTodayPayments;
    @FXML private Label lblMonthPayments;

    // ============= Data =============
    private List<Supplier> allSuppliers;
    private List<Bank> allBanks;
    private Supplier selectedSupplier;
    private List<PendingBillRow> selectedBills = new ArrayList<>();  // Changed to List for multi-select
    private AutoCompleteTextField supplierAutoComplete;
    private AutoCompleteTextField historySearchAutoComplete;
    private List<BillPayment> lastPayments = new ArrayList<>();  // Store multiple payment records
    private Font customFont20;  // Custom font 20px - stored as class member
    private final ObservableList<PendingBillRow> pendingBillsList = FXCollections.observableArrayList();
    private final ObservableList<PaymentHistoryRow> paymentHistoryList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LOG.info("Initializing PayReceiptController");

        Platform.runLater(() -> {
            // Load custom font first - 20px size
            customFont20 = SessionService.getCustomFont(20.0);

            loadMasterData();
            setupSupplierAutoComplete();
            setupPaymentModeComboBox();
            setupHistorySearchAutoComplete();
            setupPendingBillsTable();
            setupPaymentHistoryTable();
            setupEventHandlers();
            setupHistoryFilters();
            loadPaymentHistory();
            updateSummaryLabels();
        });
    }

    // ============= Data Loading =============

    private void loadMasterData() {
        try {
            allSuppliers = supplierService.getAllSuppliers();
            allBanks = bankService.getActiveBanks();
            LOG.info("Loaded {} suppliers and {} banks", allSuppliers.size(), allBanks.size());
        } catch (Exception e) {
            LOG.error("Error loading master data", e);
            alertNotification.showError("Error loading data: " + e.getMessage());
        }
    }

    private void setupSupplierAutoComplete() {
        // Apply custom font 20px to label and text field
        if (customFont20 != null) {
            lblSupplierLabel.setFont(customFont20);
            txtSupplier.setFont(customFont20);
        }

        // Create autocomplete with supplier names
        List<String> supplierNames = allSuppliers.stream()
                .map(Supplier::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());

        supplierAutoComplete = new AutoCompleteTextField(txtSupplier, supplierNames, customFont20);
        supplierAutoComplete.setUseContainsFilter(true);

        // Handle supplier selection
        supplierAutoComplete.setOnSelectionCallback(this::onSupplierSelected);
    }

    private void setupPaymentModeComboBox() {
        List<PaymentOption> options = new ArrayList<>();

        // Add banks as payment options
        for (Bank bank : allBanks) {
            options.add(new PaymentOption(bank));
        }

        cmbPaymentMode.setItems(FXCollections.observableArrayList(options));

        // Custom cell factory to display bank name with custom font 20px
        cmbPaymentMode.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PaymentOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
                if (customFont20 != null) {
                    setFont(customFont20);
                }
            }
        });

        cmbPaymentMode.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PaymentOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
                if (customFont20 != null) {
                    setFont(customFont20);
                }
            }
        });

        // Select first option if available
        if (!options.isEmpty()) {
            cmbPaymentMode.getSelectionModel().selectFirst();
        }
    }

    private void setupHistorySearchAutoComplete() {
        // Apply custom font 20px to history search field
        if (customFont20 != null) {
            txtHistorySearch.setFont(customFont20);
        }

        // Create autocomplete with supplier names for history search
        List<String> supplierNames = allSuppliers.stream()
                .map(Supplier::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());

        historySearchAutoComplete = new AutoCompleteTextField(txtHistorySearch, supplierNames, customFont20);
        historySearchAutoComplete.setUseContainsFilter(true);

        // Handle supplier selection in history search
        historySearchAutoComplete.setOnSelectionCallback(this::onHistorySupplierSelected);
    }

    private void onHistorySupplierSelected(String supplierName) {
        // Filter payment history by supplier name when selected from autocomplete
        if (supplierName != null && !supplierName.trim().isEmpty()) {
            searchHistory();
        }
    }

    private void setupPendingBillsTable() {
        colBillNo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBillNo()).asObject());
        colBillDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillDate()));
        colReffNo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReffNo()));
        colNetAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getNetAmount()).asObject());
        colPaidAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPaidAmount()).asObject());
        colBalanceAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getBalanceAmount()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        // Format currency columns
        formatCurrencyColumn(colNetAmount);
        formatCurrencyColumn(colPaidAmount);
        formatCurrencyColumn(colBalanceAmount);

        // Status column styling
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("PENDING".equals(status)) {
                        setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;");
                    } else if ("PARTIALLY_PAID".equals(status)) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblPendingBills.setItems(pendingBillsList);

        // Enable multiple selection (Ctrl+Click)
        tblPendingBills.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Handle multiple row selection
        tblPendingBills.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<PendingBillRow>) change -> {
                    onBillsSelected(tblPendingBills.getSelectionModel().getSelectedItems());
                });
    }

    private void setupPaymentHistoryTable() {
        colHistId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colHistDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentDate()));
        colHistBillNo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBillNo()).asObject());
        colHistSupplier.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplierName()));
        colHistAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        colHistMode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMode()));

        formatCurrencyColumn(colHistAmount);

        // Apply custom font 20px to Supplier column
        colHistSupplier.setCellFactory(col -> new TableCell<PaymentHistoryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (customFont20 != null) {
                        setFont(customFont20);
                    }
                }
            }
        });

        // Apply custom font 20px to Mode column
        colHistMode.setCellFactory(col -> new TableCell<PaymentHistoryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (customFont20 != null) {
                        setFont(customFont20);
                    }
                }
            }
        });

        tblPaymentHistory.setItems(paymentHistoryList);
    }

    private void setupHistoryFilters() {
        // Set default date range (current month)
        LocalDate today = LocalDate.now();
        dpHistoryFrom.setValue(today.withDayOfMonth(1));
        dpHistoryTo.setValue(today);
    }

    private void setupEventHandlers() {
        btnBack.setOnAction(e -> goBackToMenu());
        btnRefresh.setOnAction(e -> refreshAll());
        btnPay.setOnAction(e -> processPayment());
        btnClear.setOnAction(e -> clearPaymentFields());
        btnNew.setOnAction(e -> clearAll());

        btnRefreshHistory.setOnAction(e -> loadPaymentHistory());
        btnSearchHistory.setOnAction(e -> searchHistory());
        btnClearSearch.setOnAction(e -> clearHistorySearch());

        // Print button handler - prints selected history item or last payment
        if (btnPrint != null) {
            btnPrint.setOnAction(e -> printSelectedOrLastReceipt());
        }
    }

    // ============= Event Handlers =============

    private void onSupplierSelected(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            selectedSupplier = null;
            pendingBillsList.clear();
            lblTotalPending.setText("Rs. 0.00");
            return;
        }

        // Find supplier by name
        selectedSupplier = allSuppliers.stream()
                .filter(s -> s.getName().equals(supplierName))
                .findFirst()
                .orElse(null);

        if (selectedSupplier != null) {
            LOG.info("Supplier selected: {} (ID: {})", supplierName, selectedSupplier.getId());
            loadPendingBillsForSupplier();
        }
    }

    private void loadPendingBillsForSupplier() {
        if (selectedSupplier == null) return;

        try {
            List<PurchaseBill> bills = purchaseBillService.getPayableBillsBySupplier(selectedSupplier.getId());
            pendingBillsList.clear();

            double totalPending = 0.0;
            for (PurchaseBill bill : bills) {
                double balance = bill.getBalanceAmount();
                if (balance > 0) {
                    pendingBillsList.add(new PendingBillRow(bill));
                    totalPending += balance;
                }
            }

            lblTotalPending.setText(String.format("Rs. %.2f", totalPending));
            LOG.info("Loaded {} pending bills for supplier {}", pendingBillsList.size(), selectedSupplier.getName());

            // Clear selection
            clearPaymentFields();

        } catch (Exception e) {
            LOG.error("Error loading pending bills", e);
            alertNotification.showError("Error loading bills: " + e.getMessage());
        }
    }

    private void onBillsSelected(ObservableList<PendingBillRow> bills) {
        selectedBills.clear();

        if (bills == null || bills.isEmpty()) {
            hboxSelectedBill.setVisible(false);
            lblSelectedBillNo.setText("0 bills");
            lblSelectedBillAmount.setText("Rs. 0.00");
            lblSelectedBalance.setText("Rs. 0.00");
            txtPaymentAmount.clear();
            return;
        }

        // Copy selected bills (sorted by bill number for consistent payment order)
        selectedBills.addAll(bills);
        selectedBills.sort((a, b) -> Integer.compare(a.getBillNo(), b.getBillNo()));

        hboxSelectedBill.setVisible(true);

        // Calculate totals for all selected bills
        double totalNetAmount = 0.0;
        double totalBalance = 0.0;
        for (PendingBillRow bill : selectedBills) {
            totalNetAmount += bill.getNetAmount();
            totalBalance += bill.getBalanceAmount();
        }

        // Update UI labels
        if (selectedBills.size() == 1) {
            lblSelectedBillNo.setText("#" + selectedBills.get(0).getBillNo());
        } else {
            lblSelectedBillNo.setText(selectedBills.size() + " bills");
        }
        lblSelectedBillAmount.setText(String.format("Rs. %.2f", totalNetAmount));
        lblSelectedBalance.setText(String.format("Rs. %.2f", totalBalance));

        // Set default payment amount to total balance
        txtPaymentAmount.setText(String.format("%.2f", totalBalance));
    }

    private void processPayment() {
        // Validate supplier
        if (selectedSupplier == null) {
            alertNotification.showError("Please select a supplier first");
            txtSupplier.requestFocus();
            return;
        }

        // Validate bill selection
        if (selectedBills.isEmpty()) {
            alertNotification.showError("Please select one or more bills to pay");
            return;
        }

        // Validate payment amount
        Double paymentAmount;
        try {
            paymentAmount = Double.parseDouble(txtPaymentAmount.getText().trim());
        } catch (NumberFormatException e) {
            alertNotification.showError("Please enter a valid payment amount");
            txtPaymentAmount.requestFocus();
            return;
        }

        if (paymentAmount <= 0) {
            alertNotification.showError("Payment amount must be greater than 0");
            txtPaymentAmount.requestFocus();
            return;
        }

        // Calculate total balance for all selected bills
        double totalBalance = 0.0;
        for (PendingBillRow bill : selectedBills) {
            totalBalance += bill.getBalanceAmount();
        }

        if (paymentAmount > totalBalance) {
            alertNotification.showError("Payment amount (Rs. " + String.format("%.2f", paymentAmount) +
                    ") exceeds total balance (Rs. " + String.format("%.2f", totalBalance) + ")");
            txtPaymentAmount.requestFocus();
            return;
        }

        // Validate payment mode
        PaymentOption paymentMode = cmbPaymentMode.getValue();
        if (paymentMode == null || paymentMode.getBank() == null) {
            alertNotification.showError("Please select a valid payment mode");
            cmbPaymentMode.requestFocus();
            return;
        }

        try {
            // Clear last payments list
            lastPayments.clear();

            // Distribute payment across selected bills (in order of bill number - oldest first)
            double remainingAmount = paymentAmount;
            int paidBillsCount = 0;

            for (PendingBillRow bill : selectedBills) {
                if (remainingAmount <= 0) break;

                double billBalance = bill.getBalanceAmount();
                double amountToPay = Math.min(remainingAmount, billBalance);

                // Record payment for this bill
                BillPayment payment = billPaymentService.recordPayment(
                        bill.getBillNo(),
                        amountToPay,
                        paymentMode.getBank().getId(),
                        paymentMode.getDisplayName(),
                        txtChequeNo.getText().trim(),
                        txtReferenceNo.getText().trim(),
                        txtRemarks.getText().trim()
                );

                lastPayments.add(payment);
                paidBillsCount++;

                LOG.info("Payment recorded: ID={}, Bill={}, Amount={} (Remaining: {})",
                        payment.getId(), bill.getBillNo(), amountToPay, remainingAmount - amountToPay);

                remainingAmount -= amountToPay;
            }

            LOG.info("Multi-bill payment completed: {} bills, Total paid: Rs. {}",
                    paidBillsCount, paymentAmount);

            // Refresh data
            loadPendingBillsForSupplier();
            loadPaymentHistory();
            updateSummaryLabels();
            clearPaymentFields();

            // Enable print button after successful payment
            if (btnPrint != null) {
                btnPrint.setDisable(false);
            }

            // Show summary and ask to print
            askToPrintMultiPaymentReceipt(paidBillsCount, paymentAmount);

        } catch (Exception e) {
            LOG.error("Error processing payment", e);
            alertNotification.showError("Payment failed: " + e.getMessage());
        }
    }

    private void loadPaymentHistory() {
        try {
            LocalDate fromDate = dpHistoryFrom.getValue();
            LocalDate toDate = dpHistoryTo.getValue();

            if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
            if (toDate == null) toDate = LocalDate.now();

            List<BillPayment> payments = billPaymentService.getPaymentsByDateRange(fromDate, toDate);
            paymentHistoryList.clear();

            for (BillPayment payment : payments) {
                paymentHistoryList.add(new PaymentHistoryRow(payment));
            }

            LOG.info("Loaded {} payment history records", paymentHistoryList.size());

        } catch (Exception e) {
            LOG.error("Error loading payment history", e);
        }
    }

    private void searchHistory() {
        String searchText = txtHistorySearch.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            loadPaymentHistory();
            return;
        }

        // Filter by supplier name
        List<PaymentHistoryRow> filtered = paymentHistoryList.stream()
                .filter(row -> row.getSupplierName().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        paymentHistoryList.setAll(filtered);
    }

    private void clearHistorySearch() {
        txtHistorySearch.clear();
        dpHistoryFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpHistoryTo.setValue(LocalDate.now());
        loadPaymentHistory();
    }

    private void updateSummaryLabels() {
        try {
            // Today's payments
            Double todayTotal = billPaymentService.getTodayTotalPayments();
            lblTodayPayments.setText(String.format("Rs. %.2f", todayTotal));

            // This month's payments
            LocalDate today = LocalDate.now();
            Double monthTotal = billPaymentService.getTotalPaymentsByDateRange(
                    today.withDayOfMonth(1), today);
            lblMonthPayments.setText(String.format("Rs. %.2f", monthTotal));

        } catch (Exception e) {
            LOG.error("Error updating summary labels", e);
        }
    }

    // ============= Helper Methods =============

    private void askToPrintMultiPaymentReceipt(int billCount, double totalAmount) {
        // Build summary message
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Payment of Rs. %.2f distributed across %d bill(s)\n\n", totalAmount, billCount));

        for (BillPayment payment : lastPayments) {
            summary.append(String.format("Bill #%d: Rs. %.2f\n", payment.getBillNo(), payment.getPaymentAmount()));
        }

        // Show confirmation dialog
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Payment Successful");
        alert.setHeaderText("Payments recorded successfully!");
        alert.setContentText(summary.toString() + "\nDo you want to print the receipt?");

        javafx.scene.control.ButtonType btnYes = new javafx.scene.control.ButtonType("Yes, Print");
        javafx.scene.control.ButtonType btnNo = new javafx.scene.control.ButtonType("No, Later");
        alert.getButtonTypes().setAll(btnYes, btnNo);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                // Print combined receipt for all payments
                printMultiPaymentReceipt();
            }
        });
    }

    private void printMultiPaymentReceipt() {
        try {
            if (lastPayments.isEmpty()) {
                alertNotification.showWarning("No payments to print.");
                return;
            }

            LOG.info("Printing combined receipt for {} payments", lastPayments.size());
            boolean success = payReceiptPrint.printMultiPaymentReceiptWithDialog(
                    lastPayments, selectedSupplier, btnPrint.getScene().getWindow());

            if (success) {
                alertNotification.showSuccess("Receipt sent to printer!");
            }
        } catch (Exception e) {
            LOG.error("Error printing multi-bill receipt", e);
            alertNotification.showError("Error printing receipt: " + e.getMessage());
        }
    }

    private void askToPrintReceipt(BillPayment payment) {
        // Show confirmation dialog to ask user if they want to print
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Payment Successful");
        alert.setHeaderText("Payment ID: " + payment.getId() + " recorded successfully!");
        alert.setContentText("Do you want to print the receipt?");

        javafx.scene.control.ButtonType btnYes = new javafx.scene.control.ButtonType("Yes, Print");
        javafx.scene.control.ButtonType btnNo = new javafx.scene.control.ButtonType("No, Later");
        alert.getButtonTypes().setAll(btnYes, btnNo);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                printPaymentWithDialog(payment);
            }
        });
    }

    private void printPaymentWithDialog(BillPayment payment) {
        try {
            LOG.info("Printing receipt with print dialog for payment ID: {}", payment.getId());
            boolean success = payReceiptPrint.printPaymentReceiptWithDialog(payment, selectedSupplier, btnPrint.getScene().getWindow());

            if (success) {
                alertNotification.showSuccess("Receipt sent to printer!");
            }
        } catch (Exception e) {
            LOG.error("Error printing receipt", e);
            alertNotification.showError("Error printing receipt: " + e.getMessage());
        }
    }

    private void printSelectedOrLastReceipt() {
        // First check if a history item is selected
        PaymentHistoryRow selectedRow = tblPaymentHistory.getSelectionModel().getSelectedItem();

        if (selectedRow != null) {
            // Print selected history item
            printHistoryReceipt(selectedRow);
        } else if (!lastPayments.isEmpty()) {
            // Print combined receipt for last payments
            printMultiPaymentReceipt();
        } else {
            alertNotification.showWarning("Please select a payment from history or make a new payment first.");
        }
    }

    private void printHistoryReceipt(PaymentHistoryRow selectedRow) {
        try {
            // Get the full payment record
            billPaymentService.getPaymentById(selectedRow.getId()).ifPresent(payment -> {
                Supplier supplier = allSuppliers.stream()
                        .filter(s -> s.getName().equals(selectedRow.getSupplierName()))
                        .findFirst()
                        .orElse(null);

                LOG.info("Printing receipt for history payment ID: {}", payment.getId());
                boolean success = payReceiptPrint.printPaymentReceiptWithDialog(payment, supplier, btnPrint.getScene().getWindow());
                if (success) {
                    alertNotification.showSuccess("Receipt opened for printing!");
                } else {
                    alertNotification.showError("Failed to generate receipt");
                }
            });
        } catch (Exception e) {
            LOG.error("Error printing receipt", e);
            alertNotification.showError("Error printing receipt: " + e.getMessage());
        }
    }

    private void clearPaymentFields() {
        selectedBills.clear();
        hboxSelectedBill.setVisible(false);
        lblSelectedBillNo.setText("0 bills");
        lblSelectedBillAmount.setText("Rs. 0.00");
        lblSelectedBalance.setText("Rs. 0.00");
        txtPaymentAmount.clear();
        txtChequeNo.clear();
        txtReferenceNo.clear();
        txtRemarks.clear();
        tblPendingBills.getSelectionModel().clearSelection();
    }

    private void clearAll() {
        selectedSupplier = null;
        supplierAutoComplete.clear();
        pendingBillsList.clear();
        lblTotalPending.setText("Rs. 0.00");
        clearPaymentFields();
        txtSupplier.requestFocus();
    }

    private void refreshAll() {
        loadMasterData();
        setupSupplierAutoComplete();
        setupPaymentModeComboBox();
        if (selectedSupplier != null) {
            loadPendingBillsForSupplier();
        }
        loadPaymentHistory();
        updateSummaryLabels();
    }

    private void goBackToMenu() {
        try {
            BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
            if (mainPane != null) {
                Pane menuPane = loader.getPage("/fxml/transaction/TransactionMenu.fxml");
                mainPane.setCenter(menuPane);
            }
        } catch (Exception e) {
            LOG.error("Error navigating back to menu", e);
        }
    }

    private <S> void formatCurrencyColumn(TableColumn<S, Double> column) {
        column.setCellFactory(col -> new TableCell<S, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", value));
                }
            }
        });
    }

    // ============= Inner Classes =============

    /**
     * Row data for pending bills table
     */
    public static class PendingBillRow {
        private final int billNo;
        private final String billDate;
        private final String reffNo;
        private final double netAmount;
        private final double paidAmount;
        private final double balanceAmount;
        private final String status;

        public PendingBillRow(PurchaseBill bill) {
            this.billNo = bill.getBillNo();
            this.billDate = bill.getBillDate() != null ? bill.getBillDate().format(DATE_FORMAT) : "";
            this.reffNo = bill.getReffNo() != null ? bill.getReffNo() : "";
            this.netAmount = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;
            this.paidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
            this.balanceAmount = bill.getBalanceAmount();
            this.status = bill.getStatus();
        }

        public int getBillNo() { return billNo; }
        public String getBillDate() { return billDate; }
        public String getReffNo() { return reffNo; }
        public double getNetAmount() { return netAmount; }
        public double getPaidAmount() { return paidAmount; }
        public double getBalanceAmount() { return balanceAmount; }
        public String getStatus() { return status; }
    }

    /**
     * Row data for payment history table
     */
    public static class PaymentHistoryRow {
        private final int id;
        private final String paymentDate;
        private final int billNo;
        private final String supplierName;
        private final double amount;
        private final String paymentMode;

        public PaymentHistoryRow(BillPayment payment) {
            this.id = payment.getId();
            this.paymentDate = payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FORMAT) : "";
            this.billNo = payment.getBillNo();
            this.supplierName = payment.getSupplierName();
            this.amount = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : 0.0;
            this.paymentMode = payment.getPaymentMode() != null ? payment.getPaymentMode() : "";
        }

        public int getId() { return id; }
        public String getPaymentDate() { return paymentDate; }
        public int getBillNo() { return billNo; }
        public String getSupplierName() { return supplierName; }
        public double getAmount() { return amount; }
        public String getPaymentMode() { return paymentMode; }
    }

    /**
     * Payment option wrapper for ComboBox
     */
    public static class PaymentOption {
        private final Bank bank;
        private final String displayName;

        public PaymentOption(Bank bank) {
            this.bank = bank;
            this.displayName = bank.getBankName();
        }

        public Bank getBank() { return bank; }
        public String getDisplayName() { return displayName; }

        public boolean isCash() {
            return bank != null && "cash".equalsIgnoreCase(bank.getIfsc());
        }

        @Override
        public String toString() { return displayName; }
    }
}
