package com.frontend.controller.employee;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.EmployeeSalaryService;
import com.frontend.service.EmployeeSalaryService.SalaryData;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.*;
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
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class EmployeeSalaryController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeSalaryController.class);

    @Autowired
    private EmployeeSalaryService salaryService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML private Button btnBack;
    @FXML private ComboBox<String> cmbMonth;
    @FXML private ComboBox<Integer> cmbYear;
    @FXML private Button btnLoad;

    @FXML private Label lblTotalEmployees;
    @FXML private Label lblPaidCount;
    @FXML private Label lblPendingCount;
    @FXML private Label lblTotalPayable;

    @FXML private TableView<SalaryRow> tblSalary;
    @FXML private TableColumn<SalaryRow, Integer> colId;
    @FXML private TableColumn<SalaryRow, String> colEmployeeName;
    @FXML private TableColumn<SalaryRow, String> colDesignation;
    @FXML private TableColumn<SalaryRow, String> colMonthlySalary;
    @FXML private TableColumn<SalaryRow, Integer> colTotalDays;
    @FXML private TableColumn<SalaryRow, Integer> colAbsentDays;
    @FXML private TableColumn<SalaryRow, Integer> colPresentDays;
    @FXML private TableColumn<SalaryRow, String> colCalculatedSalary;
    @FXML private TableColumn<SalaryRow, String> colStatus;
    @FXML private TableColumn<SalaryRow, Void> colAction;

    @FXML private Button btnPayAll;

    private ObservableList<SalaryRow> salaryRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupMonthYearSelectors();
        setupTable();
        setupPayAllButton();

        // Load current month
        loadSalaryData();
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/employee/EmployeeServiceMenu.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error navigating back: ", ex);
            }
        });
    }

    private void setupMonthYearSelectors() {
        // Populate months
        ObservableList<String> months = FXCollections.observableArrayList();
        for (Month m : Month.values()) {
            months.add(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        cmbMonth.setItems(months);

        // Populate years (current year and 2 previous)
        int currentYear = LocalDate.now().getYear();
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int y = currentYear - 2; y <= currentYear; y++) {
            years.add(y);
        }
        cmbYear.setItems(years);

        // Set defaults to current month/year
        cmbMonth.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
        cmbYear.getSelectionModel().select(Integer.valueOf(currentYear));

        // Load button
        btnLoad.setOnAction(e -> loadSalaryData());
    }

    private void setupTable() {
        colId.setCellValueFactory(cellData -> cellData.getValue().employeeIdProperty().asObject());
        colEmployeeName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        colMonthlySalary.setCellValueFactory(cellData -> cellData.getValue().monthlySalaryStrProperty());
        colTotalDays.setCellValueFactory(cellData -> cellData.getValue().totalDaysProperty().asObject());
        colAbsentDays.setCellValueFactory(cellData -> cellData.getValue().absentDaysProperty().asObject());
        colPresentDays.setCellValueFactory(cellData -> cellData.getValue().presentDaysProperty().asObject());
        colCalculatedSalary.setCellValueFactory(cellData -> cellData.getValue().calculatedSalaryStrProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Apply custom font to Employee Name column
        applyEmployeeNameColumnFont();

        // Style status column
        colStatus.setCellFactory(column -> new TableCell<SalaryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PAID".equals(item)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        // Style absent days column (red for > 0)
        colAbsentDays.setCellFactory(column -> new TableCell<SalaryRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    if (item > 0) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        // Style calculated salary column (bold green)
        colCalculatedSalary.setCellFactory(column -> new TableCell<SalaryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-alignment: CENTER_RIGHT;");
                }
            }
        });

        // Action column: Pay button
        colAction.setCellFactory(column -> new TableCell<SalaryRow, Void>() {
            private final Button btnPay = new Button("PAY");

            {
                btnPay.getStyleClass().add("btn-pay");
                btnPay.setOnAction(e -> {
                    SalaryRow row = getTableView().getItems().get(getIndex());
                    paySingleEmployee(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SalaryRow row = getTableView().getItems().get(getIndex());
                    if (row.isPaid()) {
                        Button paidBtn = new Button("PAID");
                        paidBtn.getStyleClass().add("btn-paid");
                        paidBtn.setDisable(true);
                        setGraphic(paidBtn);
                    } else {
                        setGraphic(btnPay);
                    }
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        tblSalary.setItems(salaryRows);

        // Responsive column widths
        tblSalary.widthProperty().addListener((obs, oldVal, newVal) -> {
            double totalWidth = newVal.doubleValue() - 20;
            colId.setPrefWidth(totalWidth * 0.05);
            colEmployeeName.setPrefWidth(totalWidth * 0.18);
            colDesignation.setPrefWidth(totalWidth * 0.11);
            colMonthlySalary.setPrefWidth(totalWidth * 0.11);
            colTotalDays.setPrefWidth(totalWidth * 0.06);
            colAbsentDays.setPrefWidth(totalWidth * 0.07);
            colPresentDays.setPrefWidth(totalWidth * 0.07);
            colCalculatedSalary.setPrefWidth(totalWidth * 0.13);
            colStatus.setPrefWidth(totalWidth * 0.08);
            colAction.setPrefWidth(totalWidth * 0.12);
        });
    }

    private void setupPayAllButton() {
        btnPayAll.setOnAction(e -> payAllPending());
    }

    /**
     * Load salary data for the selected month/year.
     */
    private void loadSalaryData() {
        try {
            int monthIndex = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
            Integer year = cmbYear.getSelectionModel().getSelectedItem();

            if (monthIndex < 1 || year == null) {
                alertNotification.showWarning("Please select month and year");
                return;
            }

            LOG.info("Loading salary data for {}/{}", monthIndex, year);

            List<SalaryData> salaryDataList = salaryService.calculateSalaryForMonth(monthIndex, year);

            salaryRows.clear();
            for (SalaryData data : salaryDataList) {
                salaryRows.add(new SalaryRow(data));
            }

            updateSummary();
            LOG.info("Loaded salary data for {} employees", salaryRows.size());

        } catch (Exception e) {
            LOG.error("Error loading salary data: ", e);
            alertNotification.showError("Error loading salary data: " + e.getMessage());
        }
    }

    /**
     * Pay salary for a single employee.
     */
    private void paySingleEmployee(SalaryRow row) {
        try {
            int monthIndex = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
            Integer year = cmbYear.getSelectionModel().getSelectedItem();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Payment");
            confirm.setHeaderText(null);

            // Build custom content with employee name in custom font
            javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(8);

            Label lblEmployeeName = new Label(row.getName());
            Font customFont = SessionService.getCustomFont(20.0);
            if (customFont != null) {
                lblEmployeeName.setFont(customFont);
                lblEmployeeName.setStyle("-fx-text-fill: #1a237e;");
            } else {
                lblEmployeeName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");
            }
            lblEmployeeName.setWrapText(true);

            Label lblDetails = new Label(String.format(
                    "Month: %s %d\nPresent Days: %d / %d\nPayable: ₹%.2f\n\nConfirm payment?",
                    cmbMonth.getSelectionModel().getSelectedItem(), year,
                    row.getPresentDays(), row.getTotalDays(),
                    row.getCalculatedSalary()));
            lblDetails.setStyle("-fx-font-size: 14px; -fx-text-fill: #424242;");
            lblDetails.setWrapText(true);

            contentBox.getChildren().addAll(lblEmployeeName, lblDetails);
            confirm.getDialogPane().setContent(contentBox);

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        salaryService.paySalary(
                                row.getEmployeeId(), monthIndex, year,
                                row.getTotalDays(), row.getAbsentDays(), row.getPresentDays(),
                                row.getMonthlySalary(), row.getCalculatedSalary(),
                                row.getCalculatedSalary(), null
                        );
                        row.setPaid(true);
                        tblSalary.refresh();
                        updateSummary();
                        alertNotification.showSuccess("Salary paid to " + row.getName() +
                                ": ₹" + String.format("%.2f", row.getCalculatedSalary()));
                    } catch (Exception ex) {
                        LOG.error("Error paying salary: ", ex);
                        alertNotification.showError("Error: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Error processing salary payment: ", e);
            alertNotification.showError("Error: " + e.getMessage());
        }
    }

    /**
     * Pay all pending salaries at once.
     */
    private void payAllPending() {
        try {
            int monthIndex = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
            Integer year = cmbYear.getSelectionModel().getSelectedItem();

            long pendingCount = salaryRows.stream().filter(r -> !r.isPaid()).count();
            if (pendingCount == 0) {
                alertNotification.showWarning("All salaries are already paid for this month");
                return;
            }

            double totalPayable = salaryRows.stream()
                    .filter(r -> !r.isPaid())
                    .mapToDouble(SalaryRow::getCalculatedSalary)
                    .sum();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Pay All");
            confirm.setHeaderText("Pay All Pending Salaries");
            confirm.setContentText(String.format(
                    "Month: %s %d\nPending Employees: %d\nTotal Amount: ₹%.2f\n\nPay all pending salaries?",
                    cmbMonth.getSelectionModel().getSelectedItem(), year,
                    pendingCount, totalPayable));

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    int paidCount = 0;
                    for (SalaryRow row : salaryRows) {
                        if (!row.isPaid()) {
                            try {
                                salaryService.paySalary(
                                        row.getEmployeeId(), monthIndex, year,
                                        row.getTotalDays(), row.getAbsentDays(), row.getPresentDays(),
                                        row.getMonthlySalary(), row.getCalculatedSalary(),
                                        row.getCalculatedSalary(), null
                                );
                                row.setPaid(true);
                                paidCount++;
                            } catch (Exception ex) {
                                LOG.error("Error paying salary for employee {}: ", row.getName(), ex);
                            }
                        }
                    }
                    tblSalary.refresh();
                    updateSummary();
                    alertNotification.showSuccess("Paid salary to " + paidCount + " employees");
                }
            });
        } catch (Exception e) {
            LOG.error("Error paying all salaries: ", e);
            alertNotification.showError("Error: " + e.getMessage());
        }
    }

    /**
     * Update summary labels.
     */
    private void updateSummary() {
        int total = salaryRows.size();
        long paid = salaryRows.stream().filter(SalaryRow::isPaid).count();
        long pending = total - paid;
        double totalPayable = salaryRows.stream()
                .filter(r -> !r.isPaid())
                .mapToDouble(SalaryRow::getCalculatedSalary)
                .sum();

        lblTotalEmployees.setText(String.valueOf(total));
        lblPaidCount.setText(String.valueOf(paid));
        lblPendingCount.setText(String.valueOf(pending));
        lblTotalPayable.setText(String.format("%.2f", totalPayable));
    }

    private void applyEmployeeNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();
                colEmployeeName.setCellFactory(column -> {
                    TableCell<SalaryRow, String> cell = new TableCell<>() {
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
        } catch (Exception e) {
            LOG.error("Error applying custom font: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) btnBack.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane");
            return null;
        }
    }

    /**
     * Table row data class wrapping SalaryData with JavaFX properties.
     */
    public static class SalaryRow {
        private final SimpleIntegerProperty employeeId;
        private final SimpleStringProperty name;
        private final SimpleStringProperty designation;
        private final SimpleFloatProperty monthlySalary;
        private final SimpleStringProperty monthlySalaryStr;
        private final SimpleIntegerProperty totalDays;
        private final SimpleIntegerProperty absentDays;
        private final SimpleIntegerProperty presentDays;
        private final SimpleFloatProperty calculatedSalary;
        private final SimpleStringProperty calculatedSalaryStr;
        private final SimpleBooleanProperty paid;
        private final SimpleStringProperty status;

        public SalaryRow(SalaryData data) {
            this.employeeId = new SimpleIntegerProperty(data.getEmployeeId());
            this.name = new SimpleStringProperty(data.getName());
            this.designation = new SimpleStringProperty(data.getDesignation());
            this.monthlySalary = new SimpleFloatProperty(data.getMonthlySalary());
            this.monthlySalaryStr = new SimpleStringProperty(String.format("₹%.0f", data.getMonthlySalary()));
            this.totalDays = new SimpleIntegerProperty(data.getTotalDays());
            this.absentDays = new SimpleIntegerProperty(data.getAbsentDays());
            this.presentDays = new SimpleIntegerProperty(data.getPresentDays());
            this.calculatedSalary = new SimpleFloatProperty(data.getCalculatedSalary());
            this.calculatedSalaryStr = new SimpleStringProperty(String.format("₹%.2f", data.getCalculatedSalary()));
            this.paid = new SimpleBooleanProperty(data.isPaid());
            this.status = new SimpleStringProperty(data.isPaid() ? "PAID" : "PENDING");
        }

        public int getEmployeeId() { return employeeId.get(); }
        public SimpleIntegerProperty employeeIdProperty() { return employeeId; }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getDesignation() { return designation.get(); }
        public SimpleStringProperty designationProperty() { return designation; }

        public float getMonthlySalary() { return monthlySalary.get(); }
        public SimpleStringProperty monthlySalaryStrProperty() { return monthlySalaryStr; }

        public int getTotalDays() { return totalDays.get(); }
        public SimpleIntegerProperty totalDaysProperty() { return totalDays; }

        public int getAbsentDays() { return absentDays.get(); }
        public SimpleIntegerProperty absentDaysProperty() { return absentDays; }

        public int getPresentDays() { return presentDays.get(); }
        public SimpleIntegerProperty presentDaysProperty() { return presentDays; }

        public float getCalculatedSalary() { return calculatedSalary.get(); }
        public SimpleStringProperty calculatedSalaryStrProperty() { return calculatedSalaryStr; }

        public boolean isPaid() { return paid.get(); }
        public void setPaid(boolean paid) {
            this.paid.set(paid);
            this.status.set(paid ? "PAID" : "PENDING");
        }
        public SimpleBooleanProperty paidProperty() { return paid; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
