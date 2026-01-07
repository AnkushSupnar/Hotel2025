package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.ReducedItem;
import com.frontend.entity.TableMaster;
import com.frontend.service.ReducedItemService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import com.frontend.service.UserService;
import com.frontend.entity.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReducedItemReportController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ReducedItemReportController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QTY_FORMAT = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private ReducedItemService reducedItemService;

    @Autowired
    private TableMasterService tableMasterService;

    @Autowired
    private UserService userService;

    // Table name cache
    private Map<Integer, String> tableNameCache = new HashMap<>();

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

    // User search
    @FXML private TextField txtUserSearch;
    @FXML private Button btnClearUser;
    private AutoCompleteTextField userAutoComplete;

    // Summary labels
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalQty;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblUniqueTables;

    // Table
    @FXML private TableView<ReducedItem> tblReducedItems;
    @FXML private TableColumn<ReducedItem, String> colId;
    @FXML private TableColumn<ReducedItem, String> colDate;
    @FXML private TableColumn<ReducedItem, String> colItemName;
    @FXML private TableColumn<ReducedItem, String> colQty;
    @FXML private TableColumn<ReducedItem, String> colRate;
    @FXML private TableColumn<ReducedItem, String> colAmount;
    @FXML private TableColumn<ReducedItem, String> colTableNo;
    @FXML private TableColumn<ReducedItem, String> colReducedBy;
    @FXML private TableColumn<ReducedItem, String> colReason;

    // Footer
    @FXML private Label lblRecordCount;
    @FXML private Label lblDateRange;

    // Data
    private ObservableList<ReducedItem> itemsList = FXCollections.observableArrayList();
    private ToggleGroup periodToggleGroup;
    private LocalDate currentFromDate;
    private LocalDate currentToDate;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing ReducedItemReportController");

        Platform.runLater(() -> {
            loadTableNames();
            setupToggleGroup();
            setupDatePickers();
            setupTable();
            setupButtons();
            setupSearch();

            // Load all users for autocomplete from database
            loadUserSuggestions();

            // Default to today
            btnToday.setSelected(true);
            setDateRange(LocalDate.now(), LocalDate.now());
            loadData();
        });
    }

    private void loadTableNames() {
        try {
            List<TableMaster> tables = tableMasterService.getAllTables();
            tableNameCache.clear();
            for (TableMaster table : tables) {
                tableNameCache.put(table.getId(), table.getTableName());
            }
            LOG.info("Loaded {} table names", tableNameCache.size());
        } catch (Exception e) {
            LOG.error("Error loading table names: ", e);
        }
    }

    private String getTableName(Integer tableId) {
        if (tableId == null) return "";
        return tableNameCache.getOrDefault(tableId, String.valueOf(tableId));
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
        // Get custom font for Marathi columns
        Font customFont = SessionService.getCustomFont(20.0);
        final String customFontFamily = customFont != null ? customFont.getFamily() : null;

        // Cell value factories
        colId.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        colDate.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCreatedAt() != null ?
                data.getValue().getCreatedAt().format(DATETIME_FORMAT) : ""));

        colItemName.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getItemName()));

        colQty.setCellValueFactory(data ->
            new SimpleStringProperty(QTY_FORMAT.format(
                data.getValue().getReducedQty() != null ? data.getValue().getReducedQty() : 0)));

        colRate.setCellValueFactory(data ->
            new SimpleStringProperty(CURRENCY_FORMAT.format(
                data.getValue().getRate() != null ? data.getValue().getRate() : 0)));

        colAmount.setCellValueFactory(data ->
            new SimpleStringProperty(CURRENCY_FORMAT.format(
                data.getValue().getAmt() != null ? data.getValue().getAmt() : 0)));

        // Show table name instead of table id
        colTableNo.setCellValueFactory(data ->
            new SimpleStringProperty(getTableName(data.getValue().getTableNo())));

        colReducedBy.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getReducedByUserName() != null ?
                data.getValue().getReducedByUserName() : ""));

        colReason.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getReason() != null ?
                data.getValue().getReason() : ""));

        // Apply 14px English font to standard columns
        applyEnglishFontToColumn(colId, "-fx-alignment: CENTER;");
        applyEnglishFontToColumn(colDate, "");
        applyEnglishFontToColumn(colQty, "-fx-alignment: CENTER;");
        applyEnglishFontToColumn(colRate, "-fx-alignment: CENTER-RIGHT;");
        applyEnglishFontToColumn(colAmount, "-fx-alignment: CENTER-RIGHT; -fx-text-fill: #F44336;");
        applyEnglishFontToColumn(colTableNo, "-fx-alignment: CENTER;");
        applyEnglishFontToColumn(colReason, "-fx-text-fill: #666;");

        // Apply 20px custom font to ItemName and ReducedBy columns
        applyCustomFontToColumn(colItemName, customFontFamily);
        applyCustomFontToColumn(colReducedBy, customFontFamily);

        tblReducedItems.setItems(itemsList);
    }

    private void applyEnglishFontToColumn(TableColumn<ReducedItem, String> column, String extraStyle) {
        column.setCellFactory(col -> new TableCell<ReducedItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 14px; " + extraStyle);
                }
            }
        });
    }

    private void applyCustomFontToColumn(TableColumn<ReducedItem, String> column, String fontFamily) {
        column.setCellFactory(col -> new TableCell<ReducedItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (fontFamily != null) {
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                    } else {
                        setStyle("-fx-font-size: 20px;");
                    }
                }
            }
        });
    }

    private void setupButtons() {
        btnBack.setOnAction(e -> navigateBack());
        btnRefresh.setOnAction(e -> loadData());
        btnExportExcel.setOnAction(e -> exportToExcel());
        btnExportPdf.setOnAction(e -> exportToPdf());
    }

    private void setupSearch() {
        // Get custom font for user search field (20px)
        Font userSearchFont = SessionService.getCustomFont(20.0);

        // Create AutoCompleteTextField with custom font
        userAutoComplete = new AutoCompleteTextField(txtUserSearch, new ArrayList<>(), userSearchFont);
        userAutoComplete.setUseContainsFilter(true);
        userAutoComplete.setPromptText("Search by user");

        // Set callback when user is selected
        userAutoComplete.setOnSelectionCallback(selectedUser -> {
            filterData();
        });

        // Also filter on text change for partial matching
        txtUserSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filterData();
        });

        btnClearUser.setOnAction(e -> {
            userAutoComplete.clear();
            filterData();
        });
    }

    private void loadUserSuggestions() {
        try {
            // Get all users from USERS table
            List<User> users = userService.getAllUsers();
            List<String> userNames = new ArrayList<>();

            for (User user : users) {
                if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                    userNames.add(user.getUsername());
                }
            }

            // Update suggestions in autocomplete
            userNames.sort(String.CASE_INSENSITIVE_ORDER);
            userAutoComplete.setSuggestions(userNames);
            LOG.info("Loaded {} users for autocomplete from database", userNames.size());
        } catch (Exception e) {
            LOG.error("Error loading users for autocomplete: ", e);
        }
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

            LOG.info("Loading reduced items from {} to {}", currentFromDate, currentToDate);

            LocalDateTime startDateTime = currentFromDate.atStartOfDay();
            LocalDateTime endDateTime = currentToDate.plusDays(1).atStartOfDay().minusSeconds(1);

            List<ReducedItem> items = reducedItemService.getReducedItemsByDateRange(startDateTime, endDateTime);

            itemsList.clear();
            itemsList.addAll(items);

            filterData();
            updateSummary();

        } catch (Exception e) {
            LOG.error("Error loading reduced items: ", e);
            showError("Error loading data: " + e.getMessage());
        }
    }

    private void filterData() {
        String searchText = userAutoComplete != null ? userAutoComplete.getText() : txtUserSearch.getText();

        if (searchText == null || searchText.trim().isEmpty()) {
            tblReducedItems.setItems(itemsList);
            updateSummary();
            return;
        }

        String searchTrimmed = searchText.trim();
        ObservableList<ReducedItem> filtered = itemsList.stream()
                .filter(r -> {
                    String userName = r.getReducedByUserName();
                    if (userName == null) return false;
                    // Exact match if user selected from dropdown, otherwise contains match
                    return userName.equalsIgnoreCase(searchTrimmed) ||
                           userName.toLowerCase().contains(searchTrimmed.toLowerCase());
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        tblReducedItems.setItems(filtered);
        updateSummaryForList(filtered);
    }

    private void updateSummary() {
        updateSummaryForList(itemsList);
    }

    private void updateSummaryForList(ObservableList<ReducedItem> list) {
        int totalItems = list.size();
        double totalQty = 0;
        double totalAmount = 0;
        Set<Integer> uniqueTables = new HashSet<>();

        for (ReducedItem item : list) {
            if (item.getReducedQty() != null) {
                totalQty += item.getReducedQty();
            }
            if (item.getAmt() != null) {
                totalAmount += item.getAmt();
            }
            if (item.getTableNo() != null) {
                uniqueTables.add(item.getTableNo());
            }
        }

        lblTotalItems.setText(String.valueOf(totalItems));
        lblTotalQty.setText(QTY_FORMAT.format(totalQty));
        lblTotalAmount.setText(CURRENCY_FORMAT.format(totalAmount));
        lblUniqueTables.setText(String.valueOf(uniqueTables.size()));
        lblRecordCount.setText("Showing " + totalItems + " records");
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
