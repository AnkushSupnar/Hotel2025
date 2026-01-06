package com.frontend.controller.transaction;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Bank;
import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.SalesBillPayment;
import com.frontend.entity.SalesPaymentReceipt;
import com.frontend.print.ReceivePaymentPrint;
import com.frontend.service.*;
import com.frontend.service.SalesPaymentReceiptService.BillPaymentAllocation;
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
 * Controller for Receive Payment Frame
 * Handles receiving payments from customers for credit sales bills
 */
@Component
public class ReceivePaymentController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ReceivePaymentController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ============= Services =============
    @Autowired private SalesPaymentReceiptService salesPaymentReceiptService;
    @Autowired private BillService billService;
    @Autowired private CustomerService customerService;
    @Autowired private BankService bankService;
    @Autowired private AlertNotification alertNotification;
    @Autowired private SpringFXMLLoader loader;
    @Autowired private ReceivePaymentPrint receivePaymentPrint;

    // ============= Header =============
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;

    // ============= Customer Section =============
    @FXML private Label lblCustomerLabel;
    @FXML private TextField txtCustomer;
    @FXML private Label lblTotalPending;

    // ============= Pending Bills Table =============
    @FXML private TableView<PendingBillRow> tblPendingBills;
    @FXML private TableColumn<PendingBillRow, Integer> colBillNo;
    @FXML private TableColumn<PendingBillRow, String> colBillDate;
    @FXML private TableColumn<PendingBillRow, String> colBillTime;
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
    @FXML private Button btnReceive;
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
    @FXML private TableView<ReceiptHistoryRow> tblPaymentHistory;
    @FXML private TableColumn<ReceiptHistoryRow, Integer> colHistId;
    @FXML private TableColumn<ReceiptHistoryRow, String> colHistDate;
    @FXML private TableColumn<ReceiptHistoryRow, Integer> colHistBillNo;
    @FXML private TableColumn<ReceiptHistoryRow, String> colHistCustomer;
    @FXML private TableColumn<ReceiptHistoryRow, Double> colHistAmount;
    @FXML private TableColumn<ReceiptHistoryRow, String> colHistMode;
    @FXML private Label lblTodayReceipts;
    @FXML private Label lblMonthReceipts;

    // ============= Data =============
    private List<Customer> allCustomers;
    private List<Bank> allBanks;
    private Customer selectedCustomer;
    private List<PendingBillRow> selectedBills = new ArrayList<>();
    private AutoCompleteTextField customerAutoComplete;
    private AutoCompleteTextField historySearchAutoComplete;
    private SalesPaymentReceipt lastReceipt = null;
    private Font customFont20;
    private final ObservableList<PendingBillRow> pendingBillsList = FXCollections.observableArrayList();
    private final ObservableList<ReceiptHistoryRow> receiptHistoryList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LOG.info("Initializing ReceivePaymentController");

        // Reset all state first (important for singleton controllers)
        resetScreenState();

        Platform.runLater(() -> {
            customFont20 = SessionService.getCustomFont(20.0);

            loadMasterData();
            setupCustomerAutoComplete();
            setupPaymentModeComboBox();
            setupHistorySearchAutoComplete();
            setupPendingBillsTable();
            setupPaymentHistoryTable();
            setupEventHandlers();
            setupHistoryFilters();
            loadReceiptHistory();
            updateSummaryLabels();
        });
    }

    // ============= Data Loading =============

    private void loadMasterData() {
        try {
            allCustomers = customerService.getAllCustomers();
            allBanks = bankService.getActiveBanks();
            LOG.info("Loaded {} customers and {} banks", allCustomers.size(), allBanks.size());
        } catch (Exception e) {
            LOG.error("Error loading master data", e);
            alertNotification.showError("Error loading data: " + e.getMessage());
        }
    }

    private void setupCustomerAutoComplete() {
        if (customFont20 != null) {
            lblCustomerLabel.setFont(customFont20);
            txtCustomer.setFont(customFont20);
        }

        List<String> customerNames = allCustomers.stream()
                .map(Customer::getFullName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());

        customerAutoComplete = new AutoCompleteTextField(txtCustomer, customerNames, customFont20);
        customerAutoComplete.setUseContainsFilter(true);
        customerAutoComplete.setOnSelectionCallback(this::onCustomerSelected);
    }

    private void setupPaymentModeComboBox() {
        List<PaymentOption> options = new ArrayList<>();

        for (Bank bank : allBanks) {
            options.add(new PaymentOption(bank));
        }

        cmbPaymentMode.setItems(FXCollections.observableArrayList(options));

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

        if (!options.isEmpty()) {
            cmbPaymentMode.getSelectionModel().selectFirst();
        }
    }

    private void setupHistorySearchAutoComplete() {
        if (customFont20 != null) {
            txtHistorySearch.setFont(customFont20);
        }

        List<String> customerNames = allCustomers.stream()
                .map(Customer::getFullName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());

        historySearchAutoComplete = new AutoCompleteTextField(txtHistorySearch, customerNames, customFont20);
        historySearchAutoComplete.setUseContainsFilter(true);
        historySearchAutoComplete.setOnSelectionCallback(this::onHistoryCustomerSelected);
    }

    private void onHistoryCustomerSelected(String customerName) {
        if (customerName != null && !customerName.trim().isEmpty()) {
            searchHistory();
        }
    }

    private void setupPendingBillsTable() {
        colBillNo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBillNo()).asObject());
        colBillDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillDate()));
        colBillTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillTime()));
        colNetAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getNetAmount()).asObject());
        colPaidAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPaidAmount()).asObject());
        colBalanceAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getBalanceAmount()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        formatCurrencyColumn(colNetAmount);
        formatCurrencyColumn(colPaidAmount);
        formatCurrencyColumn(colBalanceAmount);

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("CREDIT".equals(status)) {
                        setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;");
                    } else if ("PAID".equals(status)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblPendingBills.setItems(pendingBillsList);
        tblPendingBills.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tblPendingBills.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<PendingBillRow>) change -> {
                    onBillsSelected(tblPendingBills.getSelectionModel().getSelectedItems());
                });
    }

    private void setupPaymentHistoryTable() {
        colHistId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getReceiptNo()).asObject());
        colHistDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentDate()));
        colHistBillNo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBillsCount()).asObject());
        colHistCustomer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));
        colHistAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        colHistMode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMode()));

        formatCurrencyColumn(colHistAmount);

        colHistId.setText("Receipt #");
        colHistBillNo.setText("Bills");

        colHistCustomer.setCellFactory(col -> new TableCell<ReceiptHistoryRow, String>() {
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

        colHistMode.setCellFactory(col -> new TableCell<ReceiptHistoryRow, String>() {
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

        tblPaymentHistory.setItems(receiptHistoryList);
    }

    private void setupHistoryFilters() {
        LocalDate today = LocalDate.now();
        dpHistoryFrom.setValue(today.withDayOfMonth(1));
        dpHistoryTo.setValue(today);
    }

    private void setupEventHandlers() {
        btnBack.setOnAction(e -> goBackToMenu());
        btnRefresh.setOnAction(e -> refreshAll());
        btnReceive.setOnAction(e -> processPayment());
        btnClear.setOnAction(e -> clearAll());
        btnNew.setOnAction(e -> clearAll());

        btnRefreshHistory.setOnAction(e -> loadReceiptHistory());
        btnSearchHistory.setOnAction(e -> searchHistory());
        btnClearSearch.setOnAction(e -> clearHistorySearch());

        if (btnPrint != null) {
            btnPrint.setOnAction(e -> printSelectedOrLastReceipt());
        }
    }

    // ============= Event Handlers =============

    private void onCustomerSelected(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            selectedCustomer = null;
            pendingBillsList.clear();
            lblTotalPending.setText("Rs. 0.00");
            return;
        }

        selectedCustomer = allCustomers.stream()
                .filter(c -> c.getFullName().equals(customerName))
                .findFirst()
                .orElse(null);

        if (selectedCustomer != null) {
            LOG.info("Customer selected: {} (ID: {})", customerName, selectedCustomer.getId());
            loadPendingBillsForCustomer();
        }
    }

    private void loadPendingBillsForCustomer() {
        if (selectedCustomer == null) return;

        try {
            List<Bill> bills = billService.getCreditBillsWithPendingBalanceByCustomerId(selectedCustomer.getId());
            pendingBillsList.clear();

            double totalPending = 0.0;
            for (Bill bill : bills) {
                float balance = bill.getBalanceAmount();
                if (balance > 0) {
                    pendingBillsList.add(new PendingBillRow(bill));
                    totalPending += balance;
                }
            }

            lblTotalPending.setText(String.format("Rs. %.2f", totalPending));
            LOG.info("Loaded {} pending credit bills for customer {}", pendingBillsList.size(), selectedCustomer.getFullName());

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

        selectedBills.addAll(bills);
        selectedBills.sort((a, b) -> Integer.compare(a.getBillNo(), b.getBillNo()));

        hboxSelectedBill.setVisible(true);

        double totalNetAmount = 0.0;
        double totalBalance = 0.0;
        for (PendingBillRow bill : selectedBills) {
            totalNetAmount += bill.getNetAmount();
            totalBalance += bill.getBalanceAmount();
        }

        if (selectedBills.size() == 1) {
            lblSelectedBillNo.setText("#" + selectedBills.get(0).getBillNo());
        } else {
            lblSelectedBillNo.setText(selectedBills.size() + " bills");
        }
        lblSelectedBillAmount.setText(String.format("Rs. %.2f", totalNetAmount));
        lblSelectedBalance.setText(String.format("Rs. %.2f", totalBalance));

        txtPaymentAmount.setText(String.format("%.2f", totalBalance));
    }

    private void processPayment() {
        if (selectedCustomer == null) {
            alertNotification.showError("Please select a customer first");
            txtCustomer.requestFocus();
            return;
        }

        if (selectedBills.isEmpty()) {
            alertNotification.showError("Please select one or more bills to receive payment for");
            return;
        }

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

        PaymentOption paymentMode = cmbPaymentMode.getValue();
        if (paymentMode == null || paymentMode.getBank() == null) {
            alertNotification.showError("Please select a valid payment mode");
            cmbPaymentMode.requestFocus();
            return;
        }

        try {
            List<BillPaymentAllocation> allocations = new ArrayList<>();
            double remainingAmount = paymentAmount;

            for (PendingBillRow bill : selectedBills) {
                if (remainingAmount <= 0) break;

                double billBalance = bill.getBalanceAmount();
                double amountToReceive = Math.min(remainingAmount, billBalance);

                allocations.add(new BillPaymentAllocation(bill.getBillNo(), amountToReceive));
                remainingAmount -= amountToReceive;
            }

            SalesPaymentReceipt receipt = salesPaymentReceiptService.recordGroupedPayment(
                    selectedCustomer.getId(),
                    paymentAmount,
                    paymentMode.getBank().getId(),
                    paymentMode.getDisplayName(),
                    txtChequeNo.getText().trim(),
                    txtReferenceNo.getText().trim(),
                    txtRemarks.getText().trim(),
                    allocations
            );

            lastReceipt = receipt;

            LOG.info("Sales Payment Receipt created: ReceiptNo={}, Bills={}, Total=Rs.{}",
                    receipt.getReceiptNo(), receipt.getBillsCount(), paymentAmount);

            loadPendingBillsForCustomer();
            loadReceiptHistory();
            updateSummaryLabels();
            clearPaymentFields();

            if (btnPrint != null) {
                btnPrint.setDisable(false);
            }

            askToPrintPaymentReceipt(receipt);

        } catch (Exception e) {
            LOG.error("Error processing payment", e);
            alertNotification.showError("Payment failed: " + e.getMessage());
        }
    }

    private void loadReceiptHistory() {
        try {
            LocalDate fromDate = dpHistoryFrom.getValue();
            LocalDate toDate = dpHistoryTo.getValue();

            if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
            if (toDate == null) toDate = LocalDate.now();

            List<SalesPaymentReceipt> receipts = salesPaymentReceiptService.getReceiptsByDateRangeWithCustomer(fromDate, toDate);
            receiptHistoryList.clear();

            for (SalesPaymentReceipt receipt : receipts) {
                receiptHistoryList.add(new ReceiptHistoryRow(receipt, allCustomers));
            }

            LOG.info("Loaded {} sales payment receipt records", receiptHistoryList.size());

        } catch (Exception e) {
            LOG.error("Error loading receipt history", e);
        }
    }

    private void searchHistory() {
        String searchText = txtHistorySearch.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            loadReceiptHistory();
            return;
        }

        List<ReceiptHistoryRow> filtered = receiptHistoryList.stream()
                .filter(row -> row.getCustomerName().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        receiptHistoryList.setAll(filtered);
    }

    private void clearHistorySearch() {
        txtHistorySearch.clear();
        dpHistoryFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpHistoryTo.setValue(LocalDate.now());
        loadReceiptHistory();
    }

    private void updateSummaryLabels() {
        try {
            Double todayTotal = salesPaymentReceiptService.getTodayTotalReceipts();
            lblTodayReceipts.setText(String.format("Rs. %.2f", todayTotal));

            LocalDate today = LocalDate.now();
            Double monthTotal = salesPaymentReceiptService.getTotalReceiptsByDateRange(
                    today.withDayOfMonth(1), today);
            lblMonthReceipts.setText(String.format("Rs. %.2f", monthTotal));

        } catch (Exception e) {
            LOG.error("Error updating summary labels", e);
        }
    }

    // ============= Helper Methods =============

    private void askToPrintPaymentReceipt(SalesPaymentReceipt receipt) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Receipt #%d created\n", receipt.getReceiptNo()));
        summary.append(String.format("Total Amount: Rs. %.2f\n", receipt.getTotalAmount()));
        summary.append(String.format("Bills Paid: %d\n\n", receipt.getBillsCount()));

        if (receipt.getBillPayments() != null && !receipt.getBillPayments().isEmpty()) {
            for (SalesBillPayment bp : receipt.getBillPayments()) {
                summary.append(String.format("Bill #%d: Rs. %.2f\n", bp.getBillNo(), bp.getPaymentAmount()));
            }
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Payment Received Successfully");
        alert.setHeaderText("Payment Receipt #" + receipt.getReceiptNo() + " created successfully!");
        alert.setContentText(summary.toString() + "\nDo you want to print the receipt?");

        ButtonType btnYes = new ButtonType("Yes, Print");
        ButtonType btnNo = new ButtonType("No, Later");
        alert.getButtonTypes().setAll(btnYes, btnNo);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                printPaymentReceipt(receipt);
            }
        });
    }

    private void printPaymentReceipt(SalesPaymentReceipt receipt) {
        try {
            if (receipt == null) {
                alertNotification.showWarning("No receipt to print.");
                return;
            }

            SalesPaymentReceipt fullReceipt = salesPaymentReceiptService.getReceiptWithBillPayments(receipt.getReceiptNo())
                    .orElse(receipt);

            Customer customer = selectedCustomer;
            if (customer == null && fullReceipt.getCustomerId() != null) {
                customer = allCustomers.stream()
                        .filter(c -> c.getId().equals(fullReceipt.getCustomerId()))
                        .findFirst()
                        .orElse(null);
            }

            LOG.info("Printing receipt #{} for {} bills", fullReceipt.getReceiptNo(), fullReceipt.getBillsCount());
            boolean success = receivePaymentPrint.printPaymentReceipt(fullReceipt, customer);

            if (success) {
                alertNotification.showSuccess("Receipt sent to printer!");
            }
        } catch (Exception e) {
            LOG.error("Error printing receipt", e);
            alertNotification.showError("Error printing receipt: " + e.getMessage());
        }
    }

    private void printSelectedOrLastReceipt() {
        ReceiptHistoryRow selectedRow = tblPaymentHistory.getSelectionModel().getSelectedItem();

        if (selectedRow != null) {
            printHistoryReceipt(selectedRow);
        } else if (lastReceipt != null) {
            printPaymentReceipt(lastReceipt);
        } else {
            alertNotification.showWarning("Please select a receipt from history or receive a new payment first.");
        }
    }

    private void printHistoryReceipt(ReceiptHistoryRow selectedRow) {
        try {
            salesPaymentReceiptService.getReceiptWithBillPayments(selectedRow.getReceiptNo()).ifPresent(receipt -> {
                Customer customer = allCustomers.stream()
                        .filter(c -> c.getId().equals(receipt.getCustomerId()))
                        .findFirst()
                        .orElse(null);

                LOG.info("Printing receipt #{} from history", receipt.getReceiptNo());
                boolean success = receivePaymentPrint.printPaymentReceipt(receipt, customer);
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
        selectedCustomer = null;
        if (customerAutoComplete != null) {
            customerAutoComplete.clear();
        }
        if (txtCustomer != null) {
            txtCustomer.clear();
        }
        pendingBillsList.clear();
        if (lblTotalPending != null) {
            lblTotalPending.setText("Rs. 0.00");
        }
        clearPaymentFields();
        if (txtCustomer != null) {
            txtCustomer.requestFocus();
        }
    }

    /**
     * Reset all screen state - called on screen initialization
     * Important for singleton controllers to start fresh each time
     */
    private void resetScreenState() {
        selectedCustomer = null;
        selectedBills.clear();
        lastReceipt = null;
        pendingBillsList.clear();
        receiptHistoryList.clear();
    }

    private void refreshAll() {
        loadMasterData();
        setupCustomerAutoComplete();
        setupPaymentModeComboBox();
        if (selectedCustomer != null) {
            loadPendingBillsForCustomer();
        }
        loadReceiptHistory();
        updateSummaryLabels();
    }

    private void goBackToMenu() {
        try {
            BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
            if (mainPane != null) {
                Pane menuPane = loader.getPage("/fxml/transaction/SalesMenu.fxml");
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
        private final String billTime;
        private final double netAmount;
        private final double paidAmount;
        private final double balanceAmount;
        private final String status;

        public PendingBillRow(Bill bill) {
            this.billNo = bill.getBillNo();
            this.billDate = bill.getBillDate() != null ? bill.getBillDate() : "";
            this.billTime = bill.getBillTime() != null ? bill.getBillTime() : "";
            this.netAmount = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;
            this.paidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
            this.balanceAmount = bill.getBalanceAmount();
            this.status = bill.getStatus();
        }

        public int getBillNo() { return billNo; }
        public String getBillDate() { return billDate; }
        public String getBillTime() { return billTime; }
        public double getNetAmount() { return netAmount; }
        public double getPaidAmount() { return paidAmount; }
        public double getBalanceAmount() { return balanceAmount; }
        public String getStatus() { return status; }
    }

    /**
     * Row data for receipt history table
     */
    public static class ReceiptHistoryRow {
        private final int receiptNo;
        private final String paymentDate;
        private final int billsCount;
        private final String customerName;
        private final double amount;
        private final String paymentMode;

        public ReceiptHistoryRow(SalesPaymentReceipt receipt, List<Customer> allCustomers) {
            this.receiptNo = receipt.getReceiptNo();
            this.paymentDate = receipt.getPaymentDate() != null ? receipt.getPaymentDate().format(DATE_FORMAT) : "";
            this.billsCount = receipt.getBillsCount() != null ? receipt.getBillsCount() : 1;

            // Get customer name
            String custName = receipt.getCustomerName();
            if ((custName == null || custName.isEmpty()) && receipt.getCustomerId() != null) {
                custName = allCustomers.stream()
                        .filter(c -> c.getId().equals(receipt.getCustomerId()))
                        .map(Customer::getFullName)
                        .findFirst()
                        .orElse("Customer #" + receipt.getCustomerId());
            }
            this.customerName = custName != null ? custName : "";

            this.amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : 0.0;
            this.paymentMode = receipt.getPaymentMode() != null ? receipt.getPaymentMode() : "";
        }

        public int getReceiptNo() { return receiptNo; }
        public String getPaymentDate() { return paymentDate; }
        public int getBillsCount() { return billsCount; }
        public String getCustomerName() { return customerName; }
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
