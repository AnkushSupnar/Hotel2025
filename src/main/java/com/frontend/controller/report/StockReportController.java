package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.CategoryMaster;
import com.frontend.entity.ItemStock;
import com.frontend.repository.CategoryMasterRepository;
import com.frontend.repository.ItemStockRepository;
import com.frontend.repository.ItemStockTransactionRepository;
import com.frontend.service.ItemStockService;
import com.frontend.service.SessionService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class StockReportController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(StockReportController.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private ItemStockService itemStockService;

    @Autowired
    private ItemStockRepository itemStockRepository;

    @Autowired
    private ItemStockTransactionRepository transactionRepository;

    @Autowired
    private CategoryMasterRepository categoryRepository;

    // Header buttons
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;
    @FXML private Button btnExportExcel;

    // Filters
    @FXML private TextField txtCategorySearch;
    @FXML private ComboBox<String> cmbStockStatus;
    @FXML private TextField txtItemSearch;
    @FXML private Button btnClearSearch;

    // Autocomplete
    private AutoCompleteTextField categoryAutoComplete;
    private AutoCompleteTextField itemAutoComplete;

    // Summary labels
    @FXML private Label lblTotalItems;
    @FXML private Label lblInStock;
    @FXML private Label lblLowStock;
    @FXML private Label lblOutOfStock;
    @FXML private Label lblTotalStockQty;

    // Stock Table
    @FXML private TableView<ItemStock> tblStock;
    @FXML private TableColumn<ItemStock, Integer> colItemCode;
    @FXML private TableColumn<ItemStock, String> colItemName;
    @FXML private TableColumn<ItemStock, String> colCategory;
    @FXML private TableColumn<ItemStock, String> colUnit;
    @FXML private TableColumn<ItemStock, String> colCurrentStock;
    @FXML private TableColumn<ItemStock, String> colMinLevel;
    @FXML private TableColumn<ItemStock, String> colStatus;
    @FXML private TableColumn<ItemStock, String> colLastUpdated;

    // Footer
    @FXML private Label lblRecordCount;
    @FXML private Label lblFilterInfo;

    // Data
    private ObservableList<ItemStock> stockList = FXCollections.observableArrayList();
    private List<ItemStock> allStockItems = new ArrayList<>();
    private List<CategoryMaster> allCategories = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing Stock Report Controller");

        setupButtons();
        setupFilters();
        setupTable();

        Platform.runLater(this::loadStockData);
    }

    private void setupButtons() {
        btnBack.setOnAction(e -> goBackToReportMenu());
        btnRefresh.setOnAction(e -> loadStockData());
        btnClearSearch.setOnAction(e -> clearSearch());
        btnExportExcel.setOnAction(e -> exportToExcel());
    }

    private void setupFilters() {
        // Get custom font for search fields
        Font customFont = SessionService.getCustomFont(20.0);
        String fontFamily = customFont != null ? customFont.getFamily() : null;

        // Apply custom font to category search field
        if (fontFamily != null) {
            txtCategorySearch.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
            txtItemSearch.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
        }

        // Setup Category AutoComplete
        categoryAutoComplete = new AutoCompleteTextField(txtCategorySearch, new ArrayList<>(), customFont);
        categoryAutoComplete.setUseContainsFilter(true);

        categoryAutoComplete.setOnSelectionCallback(selectedCategory -> {
            applyFilters();
        });

        txtCategorySearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Setup Item Search AutoComplete
        itemAutoComplete = new AutoCompleteTextField(txtItemSearch, new ArrayList<>(), customFont);
        itemAutoComplete.setUseContainsFilter(true);

        itemAutoComplete.setOnSelectionCallback(selectedItem -> {
            applyFilters();
        });

        txtItemSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Load categories
        try {
            allCategories = categoryRepository.findByStock("Y");
            List<String> categoryNames = new ArrayList<>();
            for (CategoryMaster cat : allCategories) {
                categoryNames.add(cat.getCategory());
            }
            categoryNames.sort(String.CASE_INSENSITIVE_ORDER);
            categoryAutoComplete.setSuggestions(categoryNames);
        } catch (Exception e) {
            LOG.error("Error loading categories", e);
        }

        // Stock status options
        cmbStockStatus.setItems(FXCollections.observableArrayList(
                "All", "In Stock", "Low Stock", "Out of Stock"
        ));
        cmbStockStatus.setValue("All");

        // Filter listener for stock status combo
        cmbStockStatus.setOnAction(e -> applyFilters());
    }

    private void setupTable() {
        // Get custom font family for table columns
        Font customFont = SessionService.getCustomFont(20.0);
        final String fontFamily = customFont != null ? customFont.getFamily() : null;

        colItemCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        colCategory.setCellValueFactory(cellData -> {
            String catName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(catName != null ? catName : "");
        });

        colUnit.setCellValueFactory(cellData -> {
            String unit = cellData.getValue().getUnit();
            return new SimpleStringProperty(unit != null ? unit : "-");
        });

        colCurrentStock.setCellValueFactory(cellData -> {
            Float stock = cellData.getValue().getStock();
            return new SimpleStringProperty(stock != null ? String.format("%.1f", stock) : "0.0");
        });

        colMinLevel.setCellValueFactory(cellData -> {
            Float minLevel = cellData.getValue().getMinStockLevel();
            return new SimpleStringProperty(minLevel != null ? String.format("%.1f", minLevel) : "0.0");
        });

        colStatus.setCellValueFactory(cellData -> {
            ItemStock item = cellData.getValue();
            return new SimpleStringProperty(getStockStatus(item));
        });

        colLastUpdated.setCellValueFactory(cellData -> {
            LocalDateTime updatedAt = cellData.getValue().getUpdatedAt();
            if (updatedAt != null) {
                return new SimpleStringProperty(updatedAt.format(DATE_TIME_FORMAT));
            }
            return new SimpleStringProperty("-");
        });

        // Apply custom font to Item Name column
        colItemName.setCellFactory(column -> new TableCell<ItemStock, String>() {
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

        // Apply custom font to Category column
        colCategory.setCellFactory(column -> new TableCell<ItemStock, String>() {
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

        // Style status column with colors
        colStatus.setCellFactory(column -> new TableCell<ItemStock, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "IN STOCK":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "LOW STOCK":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            break;
                        case "OUT OF STOCK":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });

        // Style current stock column - highlight negative/zero in red
        colCurrentStock.setCellFactory(column -> new TableCell<ItemStock, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        float val = Float.parseFloat(item);
                        if (val <= 0) {
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-font-weight: bold;");
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });

        tblStock.setItems(stockList);
    }

    private void loadStockData() {
        try {
            LOG.info("Loading stock data");
            allStockItems = itemStockService.getAllStockItems();

            // Update item name autocomplete suggestions
            List<String> itemNames = new ArrayList<>();
            for (ItemStock stock : allStockItems) {
                if (stock.getItemName() != null && !stock.getItemName().trim().isEmpty()) {
                    if (!itemNames.contains(stock.getItemName())) {
                        itemNames.add(stock.getItemName());
                    }
                }
            }
            itemNames.sort(String.CASE_INSENSITIVE_ORDER);
            itemAutoComplete.setSuggestions(itemNames);

            applyFilters();
            LOG.info("Loaded {} stock items", allStockItems.size());
        } catch (Exception e) {
            LOG.error("Error loading stock data", e);
        }
    }

    private void applyFilters() {
        List<ItemStock> filtered = new ArrayList<>(allStockItems);

        // Category filter
        String categorySearch = txtCategorySearch.getText();
        if (categorySearch != null && !categorySearch.trim().isEmpty()) {
            String catSearch = categorySearch.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(s -> s.getCategoryName() != null && s.getCategoryName().toLowerCase().contains(catSearch))
                    .collect(Collectors.toList());
        }

        // Stock status filter
        String selectedStatus = cmbStockStatus.getValue();
        if (selectedStatus != null && !"All".equals(selectedStatus)) {
            filtered = filtered.stream()
                    .filter(s -> {
                        String status = getStockStatus(s);
                        switch (selectedStatus) {
                            case "In Stock": return "IN STOCK".equals(status);
                            case "Low Stock": return "LOW STOCK".equals(status);
                            case "Out of Stock": return "OUT OF STOCK".equals(status);
                            default: return true;
                        }
                    })
                    .collect(Collectors.toList());
        }

        // Item search filter
        String searchText = txtItemSearch.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(s -> s.getItemName() != null && s.getItemName().toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }

        // Update table
        stockList.clear();
        stockList.addAll(filtered);

        // Update summary
        updateSummary(filtered);

        // Update footer
        lblRecordCount.setText("Showing " + filtered.size() + " of " + allStockItems.size() + " records");

        // Update filter info
        List<String> activeFilters = new ArrayList<>();
        if (categorySearch != null && !categorySearch.trim().isEmpty()) {
            activeFilters.add("Category: " + categorySearch.trim());
        }
        if (selectedStatus != null && !"All".equals(selectedStatus)) {
            activeFilters.add("Status: " + selectedStatus);
        }
        if (searchText != null && !searchText.trim().isEmpty()) {
            activeFilters.add("Search: " + searchText.trim());
        }
        lblFilterInfo.setText(activeFilters.isEmpty() ? "" : String.join(" | ", activeFilters));
    }

    private void updateSummary(List<ItemStock> items) {
        int totalItems = items.size();
        int inStock = 0;
        int lowStock = 0;
        int outOfStock = 0;
        float totalQty = 0;

        for (ItemStock item : items) {
            Float stock = item.getStock() != null ? item.getStock() : 0f;
            totalQty += stock;

            String status = getStockStatus(item);
            switch (status) {
                case "IN STOCK": inStock++; break;
                case "LOW STOCK": lowStock++; break;
                case "OUT OF STOCK": outOfStock++; break;
            }
        }

        lblTotalItems.setText(String.valueOf(totalItems));
        lblInStock.setText(String.valueOf(inStock));
        lblLowStock.setText(String.valueOf(lowStock));
        lblOutOfStock.setText(String.valueOf(outOfStock));
        lblTotalStockQty.setText(String.format("%.1f", totalQty));
    }

    private String getStockStatus(ItemStock item) {
        Float stock = item.getStock() != null ? item.getStock() : 0f;
        Float minLevel = item.getMinStockLevel() != null ? item.getMinStockLevel() : 0f;

        if (stock <= 0) {
            return "OUT OF STOCK";
        } else if (stock <= minLevel) {
            return "LOW STOCK";
        } else {
            return "IN STOCK";
        }
    }

    private void clearSearch() {
        txtCategorySearch.clear();
        txtItemSearch.clear();
        cmbStockStatus.setValue("All");
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

    private BorderPane getMainPane() {
        try {
            return (BorderPane) btnBack.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    // ============= Export =============

    private void exportToExcel() {
        if (stockList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No data to export. Please load a report first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Stock Report");
        fileChooser.setInitialFileName("StockReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        File file = fileChooser.showSaveDialog(btnExportExcel.getScene().getWindow());
        if (file != null) {
            try {
                createExcelReport(file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Report exported successfully to:\n" + file.getAbsolutePath());
                LOG.info("Stock report exported to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                LOG.error("Error exporting stock report to Excel", e);
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Failed to export report: " + e.getMessage());
            }
        }
    }

    private void createExcelReport(File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stock Report");

            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
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

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle summaryLabelStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryLabelStyle.setFont(summaryFont);

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("STOCK REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Row 1: Date
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            // Row 2: Empty
            sheet.createRow(2);

            // Row 3: Summary
            Row summaryRow = sheet.createRow(3);
            summaryRow.createCell(0).setCellValue("Total Items:");
            summaryRow.getCell(0).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(1).setCellValue(lblTotalItems.getText());
            summaryRow.createCell(2).setCellValue("In Stock:");
            summaryRow.getCell(2).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(3).setCellValue(lblInStock.getText());
            summaryRow.createCell(4).setCellValue("Low Stock:");
            summaryRow.getCell(4).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(5).setCellValue(lblLowStock.getText());
            summaryRow.createCell(6).setCellValue("Out of Stock:");
            summaryRow.getCell(6).setCellStyle(summaryLabelStyle);
            summaryRow.createCell(7).setCellValue(lblOutOfStock.getText());

            // Row 4: Empty
            sheet.createRow(4);

            // Row 5: Headers
            Row headerRow = sheet.createRow(5);
            String[] headers = {"Code", "Item Name", "Category", "Unit", "Current Stock", "Min Level", "Status", "Last Updated"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 6;
            for (ItemStock item : stockList) {
                Row row = sheet.createRow(rowNum++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(item.getItemCode() != null ? item.getItemCode() : 0);
                c0.setCellStyle(dataStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(item.getItemName() != null ? item.getItemName() : "");
                c1.setCellStyle(dataStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(item.getCategoryName() != null ? item.getCategoryName() : "");
                c2.setCellStyle(dataStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(item.getUnit() != null ? item.getUnit() : "-");
                c3.setCellStyle(dataStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(item.getStock() != null ? String.format("%.1f", item.getStock()) : "0.0");
                c4.setCellStyle(numberStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(item.getMinStockLevel() != null ? String.format("%.1f", item.getMinStockLevel()) : "0.0");
                c5.setCellStyle(numberStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(getStockStatus(item));
                c6.setCellStyle(dataStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(item.getUpdatedAt() != null ? item.getUpdatedAt().format(DATE_TIME_FORMAT) : "-");
                c7.setCellStyle(dataStyle);
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
