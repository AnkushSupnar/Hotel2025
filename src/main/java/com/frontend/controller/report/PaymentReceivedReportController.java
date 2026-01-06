package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.SalesPaymentReceipt;
import com.frontend.service.SalesPaymentReceiptService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class PaymentReceivedReportController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReceivedReportController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private SalesPaymentReceiptService receiptService;

    // Header buttons
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;
    @FXML private Button btnExportExcel;
    @FXML private Button btnExportPdf;

    // Period filters
    @FXML private ToggleButton btnToday;
    @FXML private ToggleButton btnWeek;
    @FXML private ToggleButton btnMonth;
    @FXML private ToggleButton btnYear;
    @FXML private ToggleButton btnCustom;

    // Date range
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;

    // Customer search
    @FXML private TextField txtCustomerSearch;
    @FXML private Button btnClearCustomer;

    // Summary labels
    @FXML private Label lblTotalReceipts;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblCashReceived;
    @FXML private Label lblBankReceived;
    @FXML private Label lblTotalBillsPaid;

    // Table
    @FXML private TableView<SalesPaymentReceipt> tblReceipts;
    @FXML private TableColumn<SalesPaymentReceipt, String> colReceiptNo;
    @FXML private TableColumn<SalesPaymentReceipt, String> colDate;
    @FXML private TableColumn<SalesPaymentReceipt, String> colCustomer;
    @FXML private TableColumn<SalesPaymentReceipt, String> colAmount;
    @FXML private TableColumn<SalesPaymentReceipt, String> colPaymentMode;
    @FXML private TableColumn<SalesPaymentReceipt, String> colBank;
    @FXML private TableColumn<SalesPaymentReceipt, String> colBillsCount;
    @FXML private TableColumn<SalesPaymentReceipt, String> colReference;

    // Footer
    @FXML private Label lblRecordCount;
    @FXML private Label lblDateRange;

    // Data
    private ObservableList<SalesPaymentReceipt> receiptsList = FXCollections.observableArrayList();
    private ToggleGroup periodToggleGroup;
    private LocalDate currentFromDate;
    private LocalDate currentToDate;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing PaymentReceivedReportController");

        Platform.runLater(() -> {
            setupToggleGroup();
            setupDatePickers();
            setupTable();
            setupButtons();
            setupSearch();

            // Default to today
            btnToday.setSelected(true);
            setDateRange(LocalDate.now(), LocalDate.now());
            loadData();
        });
    }

    private void setupToggleGroup() {
        periodToggleGroup = new ToggleGroup();
        btnToday.setToggleGroup(periodToggleGroup);
        btnWeek.setToggleGroup(periodToggleGroup);
        btnMonth.setToggleGroup(periodToggleGroup);
        btnYear.setToggleGroup(periodToggleGroup);
        btnCustom.setToggleGroup(periodToggleGroup);

        periodToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handlePeriodChange((ToggleButton) newVal);
            }
        });
    }

    private void setupDatePickers() {
        dpFromDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (btnCustom.isSelected() && newVal != null) {
                currentFromDate = newVal;
                loadData();
            }
        });

        dpToDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (btnCustom.isSelected() && newVal != null) {
                currentToDate = newVal;
                loadData();
            }
        });
    }

    private void setupTable() {
        colReceiptNo.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getReceiptNo())));

        colDate.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPaymentDate() != null ?
                data.getValue().getPaymentDate().format(DATE_FORMAT) : ""));

        colCustomer.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCustomerName()));

        colAmount.setCellValueFactory(data ->
            new SimpleStringProperty(CURRENCY_FORMAT.format(
                data.getValue().getTotalAmount() != null ? data.getValue().getTotalAmount() : 0)));

        colPaymentMode.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPaymentMode() != null ?
                data.getValue().getPaymentMode() : ""));

        colBank.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getBankName()));

        colBillsCount.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(
                data.getValue().getBillsCount() != null ? data.getValue().getBillsCount() : 1)));

        colReference.setCellValueFactory(data -> {
            String ref = data.getValue().getReferenceNo();
            String cheque = data.getValue().getChequeNo();
            if (ref != null && !ref.isEmpty()) return new SimpleStringProperty(ref);
            if (cheque != null && !cheque.isEmpty()) return new SimpleStringProperty("CHQ: " + cheque);
            return new SimpleStringProperty("");
        });

        tblReceipts.setItems(receiptsList);
    }

    private void setupButtons() {
        btnBack.setOnAction(e -> navigateBack());
        btnRefresh.setOnAction(e -> loadData());
        btnExportExcel.setOnAction(e -> exportToExcel());
        btnExportPdf.setOnAction(e -> exportToPdf());
    }

    private void setupSearch() {
        txtCustomerSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filterData();
        });

        btnClearCustomer.setOnAction(e -> {
            txtCustomerSearch.clear();
        });
    }

    private void handlePeriodChange(ToggleButton selected) {
        LocalDate today = LocalDate.now();

        if (selected == btnToday) {
            setDateRange(today, today);
        } else if (selected == btnWeek) {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            setDateRange(weekStart, today);
        } else if (selected == btnMonth) {
            LocalDate monthStart = today.withDayOfMonth(1);
            setDateRange(monthStart, today);
        } else if (selected == btnYear) {
            LocalDate yearStart = today.withDayOfYear(1);
            setDateRange(yearStart, today);
        } else if (selected == btnCustom) {
            // Enable manual date selection
            dpFromDate.setDisable(false);
            dpToDate.setDisable(false);
            return;
        }

        dpFromDate.setDisable(true);
        dpToDate.setDisable(true);
        loadData();
    }

    private void setDateRange(LocalDate from, LocalDate to) {
        currentFromDate = from;
        currentToDate = to;
        dpFromDate.setValue(from);
        dpToDate.setValue(to);

        // Update date range label
        if (from.equals(to)) {
            lblDateRange.setText("Date: " + from.format(DATE_FORMAT));
        } else {
            lblDateRange.setText("From: " + from.format(DATE_FORMAT) + " To: " + to.format(DATE_FORMAT));
        }
    }

    private void loadData() {
        try {
            if (currentFromDate == null || currentToDate == null) {
                return;
            }

            LOG.info("Loading receipts from {} to {}", currentFromDate, currentToDate);

            List<SalesPaymentReceipt> receipts = receiptService.getReceiptsByDateRangeWithCustomer(
                    currentFromDate, currentToDate);

            receiptsList.clear();
            receiptsList.addAll(receipts);

            filterData();
            updateSummary();

        } catch (Exception e) {
            LOG.error("Error loading receipts: ", e);
            showError("Error loading data: " + e.getMessage());
        }
    }

    private void filterData() {
        String searchText = txtCustomerSearch.getText();

        if (searchText == null || searchText.trim().isEmpty()) {
            tblReceipts.setItems(receiptsList);
            updateSummary();
            return;
        }

        String lowerSearch = searchText.toLowerCase().trim();
        ObservableList<SalesPaymentReceipt> filtered = receiptsList.stream()
                .filter(r -> {
                    String customerName = r.getCustomerName();
                    return customerName != null && customerName.toLowerCase().contains(lowerSearch);
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        tblReceipts.setItems(filtered);
        updateSummaryForList(filtered);
    }

    private void updateSummary() {
        updateSummaryForList(receiptsList);
    }

    private void updateSummaryForList(ObservableList<SalesPaymentReceipt> list) {
        int totalReceipts = list.size();
        double totalAmount = 0;
        double cashAmount = 0;
        double bankAmount = 0;
        int totalBills = 0;

        for (SalesPaymentReceipt r : list) {
            double amt = r.getTotalAmount() != null ? r.getTotalAmount() : 0;
            totalAmount += amt;

            String mode = r.getPaymentMode();
            if (mode != null) {
                if (mode.equalsIgnoreCase("CASH")) {
                    cashAmount += amt;
                } else {
                    bankAmount += amt;
                }
            }

            totalBills += r.getBillsCount() != null ? r.getBillsCount() : 1;
        }

        lblTotalReceipts.setText(String.valueOf(totalReceipts));
        lblTotalAmount.setText(CURRENCY_FORMAT.format(totalAmount));
        lblCashReceived.setText(CURRENCY_FORMAT.format(cashAmount));
        lblBankReceived.setText(CURRENCY_FORMAT.format(bankAmount));
        lblTotalBillsPaid.setText(String.valueOf(totalBills));
        lblRecordCount.setText("Showing " + totalReceipts + " records");
    }

    private void navigateBack() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/report/ReportMenu.fxml");
                mainPane.setCenter(pane);
            }
        } catch (Exception e) {
            LOG.error("Error navigating back: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            if (btnBack != null && btnBack.getScene() != null) {
                return (BorderPane) btnBack.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane");
        }
        return null;
    }

    private void exportToExcel() {
        // TODO: Implement Excel export
        showInfo("Excel export will be implemented soon.");
    }

    private void exportToPdf() {
        // TODO: Implement PDF export
        showInfo("PDF export will be implemented soon.");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
