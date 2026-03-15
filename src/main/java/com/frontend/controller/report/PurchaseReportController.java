package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.PurchaseBill;
import com.frontend.entity.Supplier;
import com.frontend.service.PurchaseBillService;
import com.frontend.service.SessionService;
import com.frontend.service.SupplierService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class PurchaseReportController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseReportController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private PurchaseBillService purchaseBillService;

    @Autowired
    private SupplierService supplierService;

    // Header buttons
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;
    @FXML private Button btnExportExcel;
    @FXML private Button btnExportPdf;

    // Period toggle buttons
    @FXML private ToggleButton btnToday;
    @FXML private ToggleButton btnWeek;
    @FXML private ToggleButton btnMonth;
    @FXML private ToggleButton btnYear;
    @FXML private ToggleButton btnCustom;

    // Date pickers
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;

    // Supplier search
    @FXML private Label lblSupplierSearch;
    @FXML private TextField txtSupplierSearch;
    @FXML private Button btnClearSupplier;

    // AutoComplete wrapper for supplier search
    private AutoCompleteTextField autoCompleteSupplier;

    // Summary labels
    @FXML private Label lblTotalPurchase;
    @FXML private Label lblTotalBills;
    @FXML private Label lblAvgBill;
    @FXML private Label lblTotalGst;
    @FXML private Label lblPaidBills;
    @FXML private Label lblPendingBills;

    // Table and columns
    @FXML private TableView<PurchaseBill> tblBills;
    @FXML private TableColumn<PurchaseBill, Integer> colBillNo;
    @FXML private TableColumn<PurchaseBill, String> colDate;
    @FXML private TableColumn<PurchaseBill, String> colSupplier;
    @FXML private TableColumn<PurchaseBill, String> colReffNo;
    @FXML private TableColumn<PurchaseBill, String> colAmount;
    @FXML private TableColumn<PurchaseBill, String> colGst;
    @FXML private TableColumn<PurchaseBill, String> colNetAmount;
    @FXML private TableColumn<PurchaseBill, String> colPayMode;
    @FXML private TableColumn<PurchaseBill, String> colStatus;

    // Footer labels
    @FXML private Label lblRecordCount;
    @FXML private Label lblDateRange;

    // Data
    private ObservableList<PurchaseBill> billList = FXCollections.observableArrayList();
    private List<Supplier> allSuppliers;
    private ToggleGroup periodToggleGroup;
    private Integer selectedSupplierId = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing Purchase Report Controller");

        setupButtons();
        setupToggleGroup();
        setupDatePickers();
        setupSupplierSearch();
        setupTable();
        loadSuppliers();

        // Default to Today's report
        Platform.runLater(() -> {
            btnToday.setSelected(true);
            loadTodayReport();
        });
    }

    private void setupButtons() {
        btnBack.setOnAction(e -> goBackToReportMenu());
        btnRefresh.setOnAction(e -> refreshReport());
        btnClearSupplier.setOnAction(e -> clearSupplierFilter());
        btnExportExcel.setOnAction(e -> exportToExcel());
        btnExportPdf.setOnAction(e -> exportToPdf());
    }

    private void setupToggleGroup() {
        periodToggleGroup = new ToggleGroup();
        btnToday.setToggleGroup(periodToggleGroup);
        btnWeek.setToggleGroup(periodToggleGroup);
        btnMonth.setToggleGroup(periodToggleGroup);
        btnYear.setToggleGroup(periodToggleGroup);
        btnCustom.setToggleGroup(periodToggleGroup);

        // Add listeners for period buttons
        btnToday.setOnAction(e -> loadTodayReport());
        btnWeek.setOnAction(e -> loadWeekReport());
        btnMonth.setOnAction(e -> loadMonthReport());
        btnYear.setOnAction(e -> loadYearReport());
        btnCustom.setOnAction(e -> enableCustomDateRange());
    }

    private void setupDatePickers() {
        // Set default values
        dpFromDate.setValue(LocalDate.now());
        dpToDate.setValue(LocalDate.now());

        // Initially disable date pickers (enabled only for custom period)
        dpFromDate.setDisable(true);
        dpToDate.setDisable(true);

        // Add change listeners
        dpFromDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (btnCustom.isSelected() && newVal != null) {
                loadCustomReport();
            }
        });

        dpToDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (btnCustom.isSelected() && newVal != null) {
                loadCustomReport();
            }
        });
    }

    private void setupSupplierSearch() {
        // Apply custom font from SessionService to label and textfield (20px)
        javafx.scene.text.Font customFont = SessionService.getCustomFont(20.0);
        if (customFont != null) {
            lblSupplierSearch.setFont(customFont);
            txtSupplierSearch.setFont(customFont);
            LOG.info("Applied custom font to supplier search: {}", customFont.getFamily());
        }

        // Initialize AutoCompleteTextField with empty list (will be populated after loadSuppliers)
        autoCompleteSupplier = new AutoCompleteTextField(txtSupplierSearch, new java.util.ArrayList<>(), customFont);
        autoCompleteSupplier.setUseContainsFilter(true); // Use contains filter for better search

        // Set callback when supplier is selected from autocomplete
        autoCompleteSupplier.setOnSelectionCallback(selectedName -> {
            if (selectedName != null && !selectedName.trim().isEmpty()) {
                // Find supplier by name and set the ID
                if (allSuppliers != null) {
                    for (Supplier s : allSuppliers) {
                        if (s.getName().equals(selectedName)) {
                            selectedSupplierId = s.getId();
                            LOG.info("Supplier selected from autocomplete: {} (ID: {})", selectedName, selectedSupplierId);
                            refreshReport();
                            break;
                        }
                    }
                }
            }
        });

        // Add listener for manual text changes (when user types without selecting)
        txtSupplierSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                selectedSupplierId = null;
                refreshReport();
            }
        });
    }

    private void setupTable() {
        // Setup column cell value factories
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNo"));
        colReffNo.setCellValueFactory(new PropertyValueFactory<>("reffNo"));
        colPayMode.setCellValueFactory(new PropertyValueFactory<>("pay"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Date column - format LocalDate
        colDate.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getBillDate();
            if (date != null) {
                return new SimpleStringProperty(date.format(DATE_FORMAT));
            }
            return new SimpleStringProperty("");
        });

        // Supplier column - lookup supplier name
        colSupplier.setCellValueFactory(cellData -> {
            Integer supplierId = cellData.getValue().getPartyId();
            return new SimpleStringProperty(getSupplierName(supplierId));
        });

        // Apply custom font to supplier column
        applyCustomFontToSupplierColumn();

        // Amount columns with currency formatting
        colAmount.setCellValueFactory(cellData -> {
            Double amt = cellData.getValue().getAmount();
            return new SimpleStringProperty(amt != null ? String.format("₹%.2f", amt) : "₹0.00");
        });

        colGst.setCellValueFactory(cellData -> {
            Double gst = cellData.getValue().getGst();
            return new SimpleStringProperty(gst != null ? String.format("₹%.2f", gst) : "₹0.00");
        });

        colNetAmount.setCellValueFactory(cellData -> {
            Double net = cellData.getValue().getNetAmount();
            return new SimpleStringProperty(net != null ? String.format("₹%.2f", net) : "₹0.00");
        });

        // Style status column
        colStatus.setCellFactory(column -> new TableCell<PurchaseBill, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PAID".equals(item)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if ("PENDING".equals(item)) {
                        setStyle("-fx-text-fill: #9C27B0; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Bind list to table
        tblBills.setItems(billList);
    }

    private void loadSuppliers() {
        try {
            allSuppliers = supplierService.getAllSuppliers();
            LOG.info("Loaded {} suppliers", allSuppliers.size());

            // Populate autocomplete suggestions with supplier names
            if (autoCompleteSupplier != null && allSuppliers != null) {
                List<String> supplierNames = allSuppliers.stream()
                        .map(Supplier::getName)
                        .filter(name -> name != null && !name.trim().isEmpty())
                        .collect(Collectors.toList());
                autoCompleteSupplier.setSuggestions(supplierNames);
                LOG.info("Populated autocomplete with {} supplier names", supplierNames.size());
            }
        } catch (Exception e) {
            LOG.error("Error loading suppliers", e);
        }
    }

    // ============= Report Loading Methods =============

    private void loadTodayReport() {
        LOG.info("Loading Today's report");
        LocalDate today = LocalDate.now();
        dpFromDate.setValue(today);
        dpToDate.setValue(today);
        dpFromDate.setDisable(true);
        dpToDate.setDisable(true);
        loadReport(today, today);
        updateDateRangeLabel("Today: " + today.format(DATE_FORMAT));
    }

    private void loadWeekReport() {
        LOG.info("Loading This Week's report");
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        dpFromDate.setValue(startOfWeek);
        dpToDate.setValue(endOfWeek);
        dpFromDate.setDisable(true);
        dpToDate.setDisable(true);
        loadReport(startOfWeek, endOfWeek);
        updateDateRangeLabel("This Week: " + startOfWeek.format(DATE_FORMAT) + " to " + endOfWeek.format(DATE_FORMAT));
    }

    private void loadMonthReport() {
        LOG.info("Loading This Month's report");
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        dpFromDate.setValue(startOfMonth);
        dpToDate.setValue(endOfMonth);
        dpFromDate.setDisable(true);
        dpToDate.setDisable(true);
        loadReport(startOfMonth, endOfMonth);
        updateDateRangeLabel("This Month: " + startOfMonth.format(DATE_FORMAT) + " to " + endOfMonth.format(DATE_FORMAT));
    }

    private void loadYearReport() {
        LOG.info("Loading This Year's report");
        LocalDate today = LocalDate.now();
        LocalDate startOfYear = today.with(TemporalAdjusters.firstDayOfYear());
        LocalDate endOfYear = today.with(TemporalAdjusters.lastDayOfYear());

        dpFromDate.setValue(startOfYear);
        dpToDate.setValue(endOfYear);
        dpFromDate.setDisable(true);
        dpToDate.setDisable(true);
        loadReport(startOfYear, endOfYear);
        updateDateRangeLabel("This Year: " + startOfYear.format(DATE_FORMAT) + " to " + endOfYear.format(DATE_FORMAT));
    }

    private void enableCustomDateRange() {
        LOG.info("Enabling custom date range");
        dpFromDate.setDisable(false);
        dpToDate.setDisable(false);
        loadCustomReport();
    }

    private void loadCustomReport() {
        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                // Swap dates if from is after to
                LocalDate temp = fromDate;
                fromDate = toDate;
                toDate = temp;
                dpFromDate.setValue(fromDate);
                dpToDate.setValue(toDate);
            }
            loadReport(fromDate, toDate);
            updateDateRangeLabel("Custom: " + fromDate.format(DATE_FORMAT) + " to " + toDate.format(DATE_FORMAT));
        }
    }

    private void loadReport(LocalDate startDate, LocalDate endDate) {
        try {
            LOG.info("Loading report from {} to {}, supplier: {}", startDate, endDate, selectedSupplierId);

            List<PurchaseBill> bills = purchaseBillService.getBillsByDateRange(startDate, endDate);

            // Filter by supplier if selected
            if (selectedSupplierId != null) {
                bills = bills.stream()
                        .filter(bill -> selectedSupplierId.equals(bill.getPartyId()))
                        .collect(Collectors.toList());
            }

            // Update table
            billList.clear();
            billList.addAll(bills);

            // Update summary
            updateSummary(bills);

            // Update record count
            lblRecordCount.setText("Showing " + bills.size() + " records");

            LOG.info("Loaded {} bills", bills.size());

        } catch (Exception e) {
            LOG.error("Error loading report", e);
        }
    }

    private void updateSummary(List<PurchaseBill> bills) {
        Map<String, Object> summary = calculatePurchaseSummary(bills);

        double totalNet = (double) summary.get("totalNet");
        double totalGst = (double) summary.get("totalGst");
        double avgAmount = (double) summary.get("averageAmount");
        int totalBills = (int) summary.get("totalBills");
        int paidCount = (int) summary.get("paidCount");
        int pendingCount = (int) summary.get("pendingCount");

        lblTotalPurchase.setText(String.format("₹%.2f", totalNet));
        lblTotalBills.setText(String.valueOf(totalBills));
        lblAvgBill.setText(String.format("₹%.2f", avgAmount));
        lblTotalGst.setText(String.format("₹%.2f", totalGst));
        lblPaidBills.setText(String.valueOf(paidCount));
        lblPendingBills.setText(String.valueOf(pendingCount));
    }

    /**
     * Calculate summary statistics for purchase bills
     */
    private Map<String, Object> calculatePurchaseSummary(List<PurchaseBill> bills) {
        Map<String, Object> summary = new HashMap<>();

        double totalAmount = 0.0;
        double totalGst = 0.0;
        double totalNet = 0.0;
        int paidCount = 0;
        int pendingCount = 0;

        for (PurchaseBill bill : bills) {
            totalAmount += bill.getAmount() != null ? bill.getAmount() : 0.0;
            totalGst += bill.getGst() != null ? bill.getGst() : 0.0;
            totalNet += bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;

            if ("PAID".equals(bill.getStatus())) {
                paidCount++;
            } else if ("PENDING".equals(bill.getStatus())) {
                pendingCount++;
            }
        }

        double avgAmount = bills.isEmpty() ? 0.0 : totalNet / bills.size();

        summary.put("totalAmount", totalAmount);
        summary.put("totalGst", totalGst);
        summary.put("totalNet", totalNet);
        summary.put("averageAmount", avgAmount);
        summary.put("totalBills", bills.size());
        summary.put("paidCount", paidCount);
        summary.put("pendingCount", pendingCount);

        return summary;
    }

    private void updateDateRangeLabel(String text) {
        lblDateRange.setText(text);
    }

    // ============= Supplier Search =============

    private void clearSupplierFilter() {
        if (autoCompleteSupplier != null) {
            autoCompleteSupplier.clear();
        } else {
            txtSupplierSearch.clear();
        }
        selectedSupplierId = null;
        refreshReport();
    }

    // ============= Navigation =============

    private void goBackToReportMenu() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/report/ReportMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Returned to report menu");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to report menu: ", e);
        }
    }

    private void refreshReport() {
        Toggle selected = periodToggleGroup.getSelectedToggle();
        if (selected == btnToday) {
            loadTodayReport();
        } else if (selected == btnWeek) {
            loadWeekReport();
        } else if (selected == btnMonth) {
            loadMonthReport();
        } else if (selected == btnYear) {
            loadYearReport();
        } else if (selected == btnCustom) {
            loadCustomReport();
        } else {
            loadTodayReport();
        }
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) btnBack.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    /**
     * Apply custom font (Kiran/Marathi) to the supplier column
     */
    private void applyCustomFontToSupplierColumn() {
        try {
            javafx.scene.text.Font customFont = SessionService.getCustomFont(20.0);
            final String fontFamily = customFont != null ? customFont.getFamily() : null;

            colSupplier.setCellFactory(column -> new TableCell<PurchaseBill, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isEmpty()) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (fontFamily != null) {
                            setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                        }
                    }
                }
            });

            LOG.info("Custom font applied to supplier column: {}", fontFamily);
        } catch (Exception e) {
            LOG.error("Error applying custom font to supplier column", e);
        }
    }

    // ============= Export Methods =============

    /**
     * Export current report data to Excel
     */
    private void exportToExcel() {
        if (billList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No data to export. Please load a report first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.setInitialFileName("PurchaseReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        File file = fileChooser.showSaveDialog(btnExportExcel.getScene().getWindow());
        if (file != null) {
            try {
                createExcelReport(file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Report exported successfully to:\n" + file.getAbsolutePath());
                LOG.info("Excel report exported to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                LOG.error("Error exporting to Excel", e);
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Failed to export report: " + e.getMessage());
            }
        }
    }

    /**
     * Export current report data to PDF
     */
    private void exportToPdf() {
        if (billList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No data to export. Please load a report first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("PurchaseReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(btnExportPdf.getScene().getWindow());
        if (file != null) {
            try {
                createPdfReport(file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Report exported successfully to:\n" + file.getAbsolutePath());
                LOG.info("PDF report exported to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                LOG.error("Error exporting to PDF", e);
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Failed to export report: " + e.getMessage());
            }
        }
    }

    /**
     * Create Excel workbook with report data
     */
    private void createExcelReport(File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Purchase Report");

            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Custom font style for supplier column (20px font size)
            CellStyle supplierStyle = workbook.createCellStyle();
            supplierStyle.cloneStyleFrom(dataStyle);
            String customFontFamily = SessionService.getCustomFontFamily();
            org.apache.poi.ss.usermodel.Font supplierFont = workbook.createFont();
            supplierFont.setFontHeightInPoints((short) 20);
            if (customFontFamily != null) {
                supplierFont.setFontName(customFontFamily);
            }
            supplierStyle.setFont(supplierFont);

            CellStyle summaryLabelStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryLabelStyle.setFont(summaryFont);

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("PURCHASE REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            // Row 1: Date Range
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Period: " + lblDateRange.getText());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            // Row 2: Empty
            sheet.createRow(2);

            // Row 3: Summary
            Row summaryRow = sheet.createRow(3);
            Map<String, Object> summary = calculatePurchaseSummary(billList);
            summaryRow.createCell(0).setCellValue("Total Purchase:");
            summaryRow.getCell(0).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(1).setCellValue(String.format("₹%.2f", (double) summary.get("totalNet")));
            summaryRow.createCell(3).setCellValue("Total Bills:");
            summaryRow.getCell(3).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(4).setCellValue((int) summary.get("totalBills"));
            summaryRow.createCell(6).setCellValue("Avg. Bill:");
            summaryRow.getCell(6).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(7).setCellValue(String.format("₹%.2f", (double) summary.get("averageAmount")));

            // Row 4: Empty
            sheet.createRow(4);

            // Row 5: Headers
            Row headerRow = sheet.createRow(5);
            String[] headers = {"Bill No", "Date", "Supplier", "Ref No", "Amount", "GST", "Net Amount", "Pay Mode", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 6;
            for (PurchaseBill bill : billList) {
                Row row = sheet.createRow(rowNum++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(bill.getBillNo() != null ? bill.getBillNo() : 0);
                c0.setCellStyle(dataStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(bill.getBillDate() != null ? bill.getBillDate().format(DATE_FORMAT) : "");
                c1.setCellStyle(dataStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(getSupplierName(bill.getPartyId()));
                c2.setCellStyle(supplierStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(bill.getReffNo() != null ? bill.getReffNo() : "");
                c3.setCellStyle(dataStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(bill.getAmount() != null ? String.format("₹%.2f", bill.getAmount()) : "₹0.00");
                c4.setCellStyle(currencyStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(bill.getGst() != null ? String.format("₹%.2f", bill.getGst()) : "₹0.00");
                c5.setCellStyle(currencyStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(bill.getNetAmount() != null ? String.format("₹%.2f", bill.getNetAmount()) : "₹0.00");
                c6.setCellStyle(currencyStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(bill.getPay() != null ? bill.getPay() : "");
                c7.setCellStyle(dataStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(bill.getStatus() != null ? bill.getStatus() : "");
                c8.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Create PDF document with report data
     */
    private void createPdfReport(File file) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Title
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("PURCHASE REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Date Range
        com.itextpdf.text.Font dateFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.NORMAL, BaseColor.GRAY);
        Paragraph datePara = new Paragraph("Period: " + lblDateRange.getText(), dateFont);
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(15);
        document.add(datePara);

        // Summary
        Map<String, Object> summary = calculatePurchaseSummary(billList);
        com.itextpdf.text.Font summaryFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD);

        PdfPTable summaryTable = new PdfPTable(6);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(20);

        addSummaryCell(summaryTable, "Total Purchase:", String.format("₹%.2f", (double) summary.get("totalNet")), summaryFont);
        addSummaryCell(summaryTable, "Total Bills:", String.valueOf((int) summary.get("totalBills")), summaryFont);
        addSummaryCell(summaryTable, "Avg. Bill:", String.format("₹%.2f", (double) summary.get("averageAmount")), summaryFont);

        document.add(summaryTable);

        // Table
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.8f, 1.2f, 2f, 1f, 1.2f, 1f, 1.2f, 1f, 0.8f});

        // Header style
        com.itextpdf.text.Font headerFontPdf = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        BaseColor headerBg = new BaseColor(33, 150, 243);

        String[] headers = {"Bill No", "Date", "Supplier", "Ref No", "Amount", "GST", "Net Amt", "Pay Mode", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFontPdf));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Data rows
        com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.Font paidFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, new BaseColor(76, 175, 80));
        com.itextpdf.text.Font pendingFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, new BaseColor(156, 39, 176));

        // Custom font for supplier column (20px font size)
        com.itextpdf.text.Font supplierFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.NORMAL);
        String customFontPath = SessionService.getCustomFontFilePath();
        if (customFontPath != null) {
            try {
                BaseFont customBaseFont = BaseFont.createFont(customFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                supplierFont = new com.itextpdf.text.Font(customBaseFont, 20, com.itextpdf.text.Font.NORMAL);
                LOG.info("Custom font loaded for PDF: {}", customFontPath);
            } catch (Exception e) {
                LOG.warn("Failed to load custom font for PDF, using default: {}", e.getMessage());
            }
        }

        boolean alternate = false;
        BaseColor altColor = new BaseColor(248, 249, 250);

        for (PurchaseBill bill : billList) {
            BaseColor rowColor = alternate ? altColor : BaseColor.WHITE;
            alternate = !alternate;

            addDataCell(table, String.valueOf(bill.getBillNo() != null ? bill.getBillNo() : 0), dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, bill.getBillDate() != null ? bill.getBillDate().format(DATE_FORMAT) : "", dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, getSupplierName(bill.getPartyId()), supplierFont, rowColor, Element.ALIGN_LEFT);
            addDataCell(table, bill.getReffNo() != null ? bill.getReffNo() : "", dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, bill.getAmount() != null ? String.format("₹%.2f", bill.getAmount()) : "₹0.00", dataFont, rowColor, Element.ALIGN_RIGHT);
            addDataCell(table, bill.getGst() != null ? String.format("₹%.2f", bill.getGst()) : "₹0.00", dataFont, rowColor, Element.ALIGN_RIGHT);
            addDataCell(table, bill.getNetAmount() != null ? String.format("₹%.2f", bill.getNetAmount()) : "₹0.00", dataFont, rowColor, Element.ALIGN_RIGHT);
            addDataCell(table, bill.getPay() != null ? bill.getPay() : "", dataFont, rowColor, Element.ALIGN_CENTER);

            // Status with color
            String status = bill.getStatus() != null ? bill.getStatus() : "";
            com.itextpdf.text.Font statusFont = "PAID".equals(status) ? paidFont : ("PENDING".equals(status) ? pendingFont : dataFont);
            addDataCell(table, status, statusFont, rowColor, Element.ALIGN_CENTER);
        }

        document.add(table);

        // Footer
        Paragraph footer = new Paragraph("\nGenerated on: " + LocalDate.now().format(DATE_FORMAT), dateFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);

        document.close();
    }

    private void addSummaryCell(PdfPTable table, String label, String value, com.itextpdf.text.Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPaddingRight(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }

    private void addDataCell(PdfPTable table, String text, com.itextpdf.text.Font font, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        table.addCell(cell);
    }

    /**
     * Get supplier name by ID
     */
    private String getSupplierName(Integer supplierId) {
        if (supplierId == null || allSuppliers == null) return "";
        for (Supplier s : allSuppliers) {
            if (s.getId().equals(supplierId)) {
                return s.getName();
            }
        }
        return "";
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
