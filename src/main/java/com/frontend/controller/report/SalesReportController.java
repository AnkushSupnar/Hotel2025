package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.TableMaster;
import com.frontend.service.BillService;
import com.frontend.service.CustomerService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
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
import javafx.scene.text.Font;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class SalesReportController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SalesReportController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private BillService billService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TableMasterService tableMasterService;

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

    // Customer search
    @FXML private TextField txtCustomerSearch;
    @FXML private Button btnClearCustomer;
    private AutoCompleteTextField customerAutoComplete;

    // Summary labels
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalBills;
    @FXML private Label lblAvgBill;
    @FXML private Label lblTotalDiscount;
    @FXML private Label lblPaidBills;
    @FXML private Label lblCreditBills;

    // Table and columns
    @FXML private TableView<Bill> tblBills;
    @FXML private TableColumn<Bill, Integer> colBillNo;
    @FXML private TableColumn<Bill, String> colDate;
    @FXML private TableColumn<Bill, String> colTime;
    @FXML private TableColumn<Bill, String> colCustomer;
    @FXML private TableColumn<Bill, String> colTable;
    @FXML private TableColumn<Bill, String> colAmount;
    @FXML private TableColumn<Bill, String> colDiscount;
    @FXML private TableColumn<Bill, String> colNetAmount;
    @FXML private TableColumn<Bill, String> colPaymode;
    @FXML private TableColumn<Bill, String> colStatus;

    // Footer labels
    @FXML private Label lblRecordCount;
    @FXML private Label lblDateRange;

    // Data
    private ObservableList<Bill> billList = FXCollections.observableArrayList();
    private List<Customer> allCustomers;
    private List<TableMaster> allTables;
    private ToggleGroup periodToggleGroup;
    private Integer selectedCustomerId = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing Sales Report Controller");

        setupButtons();
        setupToggleGroup();
        setupDatePickers();
        setupCustomerSearch();
        setupTable();
        loadCustomers();
        loadTables();

        // Load customer suggestions for autocomplete
        loadCustomerSuggestions();

        // Default to Today's report
        Platform.runLater(() -> {
            btnToday.setSelected(true);
            loadTodayReport();
        });
    }

    private void setupButtons() {
        btnBack.setOnAction(e -> goBackToReportMenu());
        btnRefresh.setOnAction(e -> refreshReport());
        btnClearCustomer.setOnAction(e -> clearCustomerFilter());
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

    private void setupCustomerSearch() {
        // Get custom font for customer search field (20px)
        Font customerSearchFont = SessionService.getCustomFont(20.0);

        // Apply custom font to txtCustomerSearch via inline style (overrides CSS)
        if (customerSearchFont != null) {
            String fontFamily = customerSearchFont.getFamily();
            double fontSize = customerSearchFont.getSize();
            txtCustomerSearch.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px;");
        }

        // Create AutoCompleteTextField with custom font
        customerAutoComplete = new AutoCompleteTextField(txtCustomerSearch, new ArrayList<>(), customerSearchFont);
        customerAutoComplete.setUseContainsFilter(true);
        customerAutoComplete.setPromptText("grahakacao naava");

        // Set callback when customer is selected
        customerAutoComplete.setOnSelectionCallback(selectedCustomer -> {
            searchCustomer(selectedCustomer);
        });

        // Also filter on text change for partial matching
        txtCustomerSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                searchCustomer(newVal.trim());
            } else {
                selectedCustomerId = null;
                refreshReport();
            }
        });
    }

    private void loadCustomerSuggestions() {
        try {
            if (allCustomers == null) {
                loadCustomers();
            }

            List<String> customerNames = new ArrayList<>();
            for (Customer customer : allCustomers) {
                String fullName = customer.getFirstName() + " " + customer.getLastName();
                if (!fullName.trim().isEmpty()) {
                    customerNames.add(fullName.trim());
                }
            }

            // Update suggestions in autocomplete
            customerNames.sort(String.CASE_INSENSITIVE_ORDER);
            customerAutoComplete.setSuggestions(customerNames);
            LOG.info("Loaded {} customers for autocomplete", customerNames.size());
        } catch (Exception e) {
            LOG.error("Error loading customers for autocomplete: ", e);
        }
    }

    private void setupTable() {
        // Setup column cell value factories
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNo"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("billDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("billTime"));
        colPaymode.setCellValueFactory(new PropertyValueFactory<>("paymode"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Table column - lookup table name from TableMaster
        colTable.setCellValueFactory(cellData -> {
            Integer tableNo = cellData.getValue().getTableNo();
            return new SimpleStringProperty(getTableName(tableNo));
        });

        // Customer column - lookup customer name with custom font
        colCustomer.setCellValueFactory(cellData -> {
            Integer customerId = cellData.getValue().getCustomerId();
            if (customerId != null && allCustomers != null) {
                for (Customer c : allCustomers) {
                    if (c.getId().equals(customerId)) {
                        return new SimpleStringProperty(c.getFirstName() + " " + c.getLastName());
                    }
                }
            }
            return new SimpleStringProperty("");
        });

        // Apply custom font to customer column
        applyCustomFontToCustomerColumn();

        // Amount columns with currency formatting
        colAmount.setCellValueFactory(cellData -> {
            Float amt = cellData.getValue().getBillAmt();
            return new SimpleStringProperty(amt != null ? String.format("₹%.2f", amt) : "₹0.00");
        });

        colDiscount.setCellValueFactory(cellData -> {
            Float disc = cellData.getValue().getDiscount();
            return new SimpleStringProperty(disc != null ? String.format("₹%.2f", disc) : "₹0.00");
        });

        colNetAmount.setCellValueFactory(cellData -> {
            Float net = cellData.getValue().getNetAmount();
            return new SimpleStringProperty(net != null ? String.format("₹%.2f", net) : "₹0.00");
        });

        // Style status column
        colStatus.setCellFactory(column -> new TableCell<Bill, String>() {
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
                    } else if ("CREDIT".equals(item)) {
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

    private void loadCustomers() {
        try {
            allCustomers = customerService.getAllCustomers();
            LOG.info("Loaded {} customers", allCustomers.size());
        } catch (Exception e) {
            LOG.error("Error loading customers", e);
        }
    }

    private void loadTables() {
        try {
            allTables = tableMasterService.getAllTables();
            LOG.info("Loaded {} tables", allTables.size());
        } catch (Exception e) {
            LOG.error("Error loading tables", e);
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
            LOG.info("Loading report from {} to {}, customer: {}", startDate, endDate, selectedCustomerId);

            List<Bill> bills;
            if (selectedCustomerId != null) {
                bills = billService.getSalesBillsByDateRangeAndCustomer(startDate, endDate, selectedCustomerId);
            } else {
                bills = billService.getSalesBillsByDateRange(startDate, endDate);
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

    private void updateSummary(List<Bill> bills) {
        Map<String, Object> summary = billService.calculateBillSummary(bills);

        float totalNet = (float) summary.get("totalNet");
        float totalDiscount = (float) summary.get("totalDiscount");
        float avgAmount = (float) summary.get("averageAmount");
        int totalBills = (int) summary.get("totalBills");
        int paidCount = (int) summary.get("paidCount");
        int creditCount = (int) summary.get("creditCount");

        lblTotalSales.setText(String.format("₹%.2f", totalNet));
        lblTotalBills.setText(String.valueOf(totalBills));
        lblAvgBill.setText(String.format("₹%.2f", avgAmount));
        lblTotalDiscount.setText(String.format("₹%.2f", totalDiscount));
        lblPaidBills.setText(String.valueOf(paidCount));
        lblCreditBills.setText(String.valueOf(creditCount));
    }

    private void updateDateRangeLabel(String text) {
        lblDateRange.setText(text);
    }

    // ============= Customer Search =============

    private void searchCustomer(String searchText) {
        if (allCustomers == null) return;

        // Find matching customer
        List<Customer> matches = allCustomers.stream()
                .filter(c -> {
                    String fullName = (c.getFirstName() + " " + c.getLastName()).toLowerCase();
                    return fullName.contains(searchText.toLowerCase());
                })
                .collect(Collectors.toList());

        if (!matches.isEmpty()) {
            selectedCustomerId = matches.get(0).getId();
            LOG.info("Customer filter applied: {}", selectedCustomerId);
            refreshReport();
        }
    }

    private void clearCustomerFilter() {
        if (customerAutoComplete != null) {
            customerAutoComplete.clear();
        } else {
            txtCustomerSearch.clear();
        }
        selectedCustomerId = null;
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
     * Apply custom font (Kiran/Marathi) to the customer column
     */
    private void applyCustomFontToCustomerColumn() {
        try {
            Font customFont = SessionService.getCustomFont(20.0);
            final String fontFamily = customFont != null ? customFont.getFamily() : null;

            colCustomer.setCellFactory(column -> new TableCell<Bill, String>() {
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

            LOG.info("Custom font applied to customer column: {}", fontFamily);
        } catch (Exception e) {
            LOG.error("Error applying custom font to customer column", e);
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
        fileChooser.setInitialFileName("SalesReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".xlsx");
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
        fileChooser.setInitialFileName("SalesReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf");
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
            Sheet sheet = workbook.createSheet("Sales Report");

            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
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

            // Custom font style for customer column (20px font size)
            CellStyle customerStyle = workbook.createCellStyle();
            customerStyle.cloneStyleFrom(dataStyle);
            String customFontFamily = SessionService.getCustomFontFamily();
            org.apache.poi.ss.usermodel.Font customerFont = workbook.createFont();
            customerFont.setFontHeightInPoints((short) 20);
            if (customFontFamily != null) {
                customerFont.setFontName(customFontFamily);
            }
            customerStyle.setFont(customerFont);

            CellStyle summaryLabelStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryLabelStyle.setFont(summaryFont);

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SALES REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            // Row 1: Date Range
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Period: " + lblDateRange.getText());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            // Row 2: Empty
            sheet.createRow(2);

            // Row 3: Summary
            Row summaryRow = sheet.createRow(3);
            Map<String, Object> summary = billService.calculateBillSummary(billList);
            summaryRow.createCell(0).setCellValue("Total Sales:");
            summaryRow.getCell(0).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(1).setCellValue(String.format("₹%.2f", (float) summary.get("totalNet")));
            summaryRow.createCell(3).setCellValue("Total Bills:");
            summaryRow.getCell(3).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(4).setCellValue((int) summary.get("totalBills"));
            summaryRow.createCell(6).setCellValue("Avg. Bill:");
            summaryRow.getCell(6).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(7).setCellValue(String.format("₹%.2f", (float) summary.get("averageAmount")));

            // Row 4: Empty
            sheet.createRow(4);

            // Row 5: Headers
            Row headerRow = sheet.createRow(5);
            String[] headers = {"Bill No", "Date", "Time", "Customer", "Table", "Amount", "Discount", "Net Amount", "Pay Mode", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 6;
            for (Bill bill : billList) {
                Row row = sheet.createRow(rowNum++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(bill.getBillNo() != null ? bill.getBillNo() : 0);
                c0.setCellStyle(dataStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(bill.getBillDate() != null ? bill.getBillDate() : "");
                c1.setCellStyle(dataStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(bill.getBillTime() != null ? bill.getBillTime() : "");
                c2.setCellStyle(dataStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(getCustomerName(bill.getCustomerId()));
                c3.setCellStyle(customerStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(getTableName(bill.getTableNo()));
                c4.setCellStyle(dataStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(bill.getBillAmt() != null ? String.format("₹%.2f", bill.getBillAmt()) : "₹0.00");
                c5.setCellStyle(currencyStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(bill.getDiscount() != null ? String.format("₹%.2f", bill.getDiscount()) : "₹0.00");
                c6.setCellStyle(currencyStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(bill.getNetAmount() != null ? String.format("₹%.2f", bill.getNetAmount()) : "₹0.00");
                c7.setCellStyle(currencyStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(bill.getPaymode() != null ? bill.getPaymode() : "");
                c8.setCellStyle(dataStyle);

                Cell c9 = row.createCell(9);
                c9.setCellValue(bill.getStatus() != null ? bill.getStatus() : "");
                c9.setCellStyle(dataStyle);
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
        Paragraph title = new Paragraph("SALES REPORT", titleFont);
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
        Map<String, Object> summary = billService.calculateBillSummary(billList);
        com.itextpdf.text.Font summaryFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD);

        PdfPTable summaryTable = new PdfPTable(6);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(20);

        addSummaryCell(summaryTable, "Total Sales:", String.format("₹%.2f", (float) summary.get("totalNet")), summaryFont);
        addSummaryCell(summaryTable, "Total Bills:", String.valueOf((int) summary.get("totalBills")), summaryFont);
        addSummaryCell(summaryTable, "Avg. Bill:", String.format("₹%.2f", (float) summary.get("averageAmount")), summaryFont);

        document.add(summaryTable);

        // Table
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1.2f, 1f, 2f, 0.8f, 1.2f, 1f, 1.2f, 1f, 1f});

        // Header style
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        BaseColor headerBg = new BaseColor(102, 126, 234);

        String[] headers = {"Bill No", "Date", "Time", "Customer", "Table", "Amount", "Discount", "Net Amt", "Pay Mode", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Data rows
        com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.Font paidFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, new BaseColor(76, 175, 80));
        com.itextpdf.text.Font creditFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, new BaseColor(156, 39, 176));

        // Custom font for customer column (20px font size)
        com.itextpdf.text.Font customerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.NORMAL);
        String customFontPath = SessionService.getCustomFontFilePath();
        if (customFontPath != null) {
            try {
                BaseFont customBaseFont = BaseFont.createFont(customFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                customerFont = new com.itextpdf.text.Font(customBaseFont, 20, com.itextpdf.text.Font.NORMAL);
                LOG.info("Custom font loaded for PDF: {}", customFontPath);
            } catch (Exception e) {
                LOG.warn("Failed to load custom font for PDF, using default: {}", e.getMessage());
            }
        }

        boolean alternate = false;
        BaseColor altColor = new BaseColor(248, 249, 250);

        for (Bill bill : billList) {
            BaseColor rowColor = alternate ? altColor : BaseColor.WHITE;
            alternate = !alternate;

            addDataCell(table, String.valueOf(bill.getBillNo() != null ? bill.getBillNo() : 0), dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, bill.getBillDate() != null ? bill.getBillDate() : "", dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, bill.getBillTime() != null ? bill.getBillTime() : "", dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, getCustomerName(bill.getCustomerId()), customerFont, rowColor, Element.ALIGN_LEFT);
            addDataCell(table, getTableName(bill.getTableNo()), dataFont, rowColor, Element.ALIGN_CENTER);
            addDataCell(table, bill.getBillAmt() != null ? String.format("₹%.2f", bill.getBillAmt()) : "₹0.00", dataFont, rowColor, Element.ALIGN_RIGHT);
            addDataCell(table, bill.getDiscount() != null ? String.format("₹%.2f", bill.getDiscount()) : "₹0.00", dataFont, rowColor, Element.ALIGN_RIGHT);
            addDataCell(table, bill.getNetAmount() != null ? String.format("₹%.2f", bill.getNetAmount()) : "₹0.00", dataFont, rowColor, Element.ALIGN_RIGHT);
            addDataCell(table, bill.getPaymode() != null ? bill.getPaymode() : "", dataFont, rowColor, Element.ALIGN_CENTER);

            // Status with color
            String status = bill.getStatus() != null ? bill.getStatus() : "";
            com.itextpdf.text.Font statusFont = "PAID".equals(status) ? paidFont : ("CREDIT".equals(status) ? creditFont : dataFont);
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
     * Get customer name by ID
     */
    private String getCustomerName(Integer customerId) {
        if (customerId == null || allCustomers == null) return "";
        for (Customer c : allCustomers) {
            if (c.getId().equals(customerId)) {
                return c.getFirstName() + " " + c.getLastName();
            }
        }
        return "";
    }

    /**
     * Get table name by ID
     */
    private String getTableName(Integer tableId) {
        if (tableId == null || allTables == null) return "";
        for (TableMaster t : allTables) {
            if (t.getId().equals(tableId)) {
                return t.getTableName();
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
