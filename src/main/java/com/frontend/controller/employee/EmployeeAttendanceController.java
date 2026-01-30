package com.frontend.controller.employee;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.EmployeeAttendance;
import com.frontend.entity.Employees;
import com.frontend.service.EmployeeAttendanceService;
import com.frontend.service.EmployeesService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class EmployeeAttendanceController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeAttendanceController.class);

    @Autowired
    private EmployeeAttendanceService attendanceService;

    @Autowired
    private EmployeesService employeesService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML private Button btnBack;
    @FXML private DatePicker dpAttendanceDate;
    @FXML private Button btnLoadDate;
    @FXML private Label lblTotalEmployees;
    @FXML private Label lblPresentCount;
    @FXML private Label lblAbsentCount;
    @FXML private Label lblSearchLabel;
    @FXML private TextField txtSearch;
    @FXML private Button btnClearSearch;
    @FXML private TableView<AttendanceData> tblAttendance;
    @FXML private TableColumn<AttendanceData, Integer> colId;
    @FXML private TableColumn<AttendanceData, String> colEmployeeName;
    @FXML private TableColumn<AttendanceData, String> colDesignation;
    @FXML private TableColumn<AttendanceData, String> colMobile;
    @FXML private TableColumn<AttendanceData, Boolean> colAbsent;
    @FXML private TableColumn<AttendanceData, String> colReason;
    @FXML private Button btnSave;

    private ObservableList<AttendanceData> attendanceDataList = FXCollections.observableArrayList();
    private FilteredList<AttendanceData> filteredData;
    private AutoCompleteTextField searchAutoComplete;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupDatePicker();
        setupTable();
        setupSearch();
        setupSaveButton();

        // Load attendance for today
        loadAttendanceForDate(LocalDate.now());
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to Employee Service Menu");
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

    private void setupDatePicker() {
        dpAttendanceDate.setValue(LocalDate.now());

        btnLoadDate.setOnAction(e -> {
            LocalDate selectedDate = dpAttendanceDate.getValue();
            if (selectedDate != null) {
                loadAttendanceForDate(selectedDate);
            } else {
                alertNotification.showWarning("Please select a date");
            }
        });
    }

    private void setupTable() {
        // Column value factories
        colId.setCellValueFactory(cellData -> cellData.getValue().employeeIdProperty().asObject());
        colEmployeeName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        colMobile.setCellValueFactory(cellData -> cellData.getValue().mobileProperty());
        colAbsent.setCellValueFactory(cellData -> cellData.getValue().absentProperty());
        colReason.setCellValueFactory(cellData -> cellData.getValue().reasonProperty());

        // Employee Name column: custom font cell factory
        applyEmployeeNameColumnFont();

        // Absent column: CheckBox cell factory
        colAbsent.setCellFactory(column -> new TableCell<AttendanceData, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AttendanceData data = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(data.isAbsent());
                    checkBox.setOnAction(e -> {
                        data.setAbsent(checkBox.isSelected());
                        updateSummary();
                    });
                    setGraphic(checkBox);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Reason column: TextField cell factory
        colReason.setCellFactory(column -> new TableCell<AttendanceData, String>() {
            private final TextField textField = new TextField();

            {
                textField.setPromptText("Enter reason...");
                textField.setPrefWidth(200);
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        AttendanceData data = getTableView().getItems().get(getIndex());
                        data.setReason(textField.getText());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AttendanceData data = getTableView().getItems().get(getIndex());
                    textField.setText(data.getReason());
                    setGraphic(textField);
                }
            }
        });

        // Setup filtered list
        filteredData = new FilteredList<>(attendanceDataList, p -> true);
        tblAttendance.setItems(filteredData);

        // Responsive column widths
        tblAttendance.widthProperty().addListener((obs, oldVal, newVal) -> {
            double totalWidth = newVal.doubleValue() - 20;
            colId.setPrefWidth(totalWidth * 0.07);
            colEmployeeName.setPrefWidth(totalWidth * 0.22);
            colDesignation.setPrefWidth(totalWidth * 0.15);
            colMobile.setPrefWidth(totalWidth * 0.13);
            colAbsent.setPrefWidth(totalWidth * 0.10);
            colReason.setPrefWidth(totalWidth * 0.33);
        });
    }

    private void setupSearch() {
        // Apply custom font from SessionService to label and textfield (20px)
        Font customFont = SessionService.getCustomFont(20.0);
        if (customFont != null) {
            lblSearchLabel.setFont(customFont);
            txtSearch.setFont(customFont);
            LOG.info("Applied custom font to employee search: {}", customFont.getFamily());
        }

        // Initialize AutoCompleteTextField (will be populated after loadAttendanceForDate)
        searchAutoComplete = new AutoCompleteTextField(txtSearch, new ArrayList<>(), customFont);
        searchAutoComplete.setUseContainsFilter(true);

        // Clear button
        btnClearSearch.setOnAction(e -> {
            searchAutoComplete.clear();
        });

        // Filter table as user types
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(data -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();
                return data.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

    private void setupSaveButton() {
        btnSave.setOnAction(e -> saveAttendance());
    }

    /**
     * Load attendance data for a given date.
     * Loads all active employees, then checks which have absence records.
     */
    private void loadAttendanceForDate(LocalDate date) {
        try {
            LOG.info("Loading attendance for date: {}", date);
            dpAttendanceDate.setValue(date);

            // Get all active employees
            List<Employees> activeEmployees = employeesService.getActiveEmployees();

            // Get absences for this date
            List<EmployeeAttendance> absences = attendanceService.getAbsencesForDate(date);

            // Build a map of employeeId -> absence record for quick lookup
            Map<Integer, EmployeeAttendance> absenceMap = absences.stream()
                    .collect(Collectors.toMap(
                            a -> a.getEmployee().getEmployeeId(),
                            a -> a
                    ));

            // Build attendance data list
            attendanceDataList.clear();
            for (Employees emp : activeEmployees) {
                EmployeeAttendance absence = absenceMap.get(emp.getEmployeeId());
                boolean isAbsent = absence != null;
                String reason = isAbsent ? absence.getReason() : "";

                AttendanceData data = new AttendanceData(
                        emp.getEmployeeId(),
                        emp.getFullName(),
                        emp.getDesignation() != null ? emp.getDesignation() : "",
                        emp.getMobileNo() != null ? emp.getMobileNo() : "",
                        isAbsent,
                        reason != null ? reason : ""
                );
                attendanceDataList.add(data);
            }

            // Update autocomplete suggestions with employee names
            List<String> employeeNames = attendanceDataList.stream()
                    .map(AttendanceData::getName)
                    .collect(Collectors.toList());
            searchAutoComplete.setSuggestions(employeeNames);

            // Clear search filter
            searchAutoComplete.clear();

            updateSummary();
            LOG.info("Loaded {} employees for attendance on {}", activeEmployees.size(), date);

        } catch (Exception e) {
            LOG.error("Error loading attendance for date: {}", date, e);
            alertNotification.showError("Error loading attendance data: " + e.getMessage());
        }
    }

    /**
     * Save attendance for the selected date
     */
    private void saveAttendance() {
        try {
            LocalDate date = dpAttendanceDate.getValue();
            if (date == null) {
                alertNotification.showWarning("Please select a date");
                return;
            }

            // Collect all entries from the full list (not filtered)
            List<EmployeeAttendanceService.AttendanceEntry> entries = new ArrayList<>();
            for (AttendanceData data : attendanceDataList) {
                entries.add(new EmployeeAttendanceService.AttendanceEntry(
                        data.getEmployeeId(),
                        data.isAbsent(),
                        data.getReason()
                ));
            }

            attendanceService.saveAttendanceForDate(date, entries);
            alertNotification.showSuccess("Attendance saved successfully for " + date);
            LOG.info("Attendance saved for date: {} with {} entries", date, entries.size());

        } catch (Exception e) {
            LOG.error("Error saving attendance: ", e);
            alertNotification.showError("Error saving attendance: " + e.getMessage());
        }
    }

    /**
     * Update the summary labels with current counts
     */
    private void updateSummary() {
        int total = attendanceDataList.size();
        long absent = attendanceDataList.stream().filter(AttendanceData::isAbsent).count();
        long present = total - absent;

        lblTotalEmployees.setText(String.valueOf(total));
        lblPresentCount.setText(String.valueOf(present));
        lblAbsentCount.setText(String.valueOf(absent));
    }

    /**
     * Apply custom font from SessionService to Employee Name column cells (20px)
     */
    private void applyEmployeeNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();
                colEmployeeName.setCellFactory(column -> {
                    TableCell<AttendanceData, String> cell = new TableCell<AttendanceData, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(item);
                            }
                        }
                    };
                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                    return cell;
                });
                LOG.info("Custom font '{}' applied to Employee Name column at 20px", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Employee Name column: ", e);
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
     * Inner static class representing a row in the attendance table
     */
    public static class AttendanceData {
        private final SimpleIntegerProperty employeeId;
        private final SimpleStringProperty name;
        private final SimpleStringProperty designation;
        private final SimpleStringProperty mobile;
        private final SimpleBooleanProperty absent;
        private final SimpleStringProperty reason;

        public AttendanceData(int employeeId, String name, String designation, String mobile, boolean absent, String reason) {
            this.employeeId = new SimpleIntegerProperty(employeeId);
            this.name = new SimpleStringProperty(name);
            this.designation = new SimpleStringProperty(designation);
            this.mobile = new SimpleStringProperty(mobile);
            this.absent = new SimpleBooleanProperty(absent);
            this.reason = new SimpleStringProperty(reason);
        }

        public int getEmployeeId() { return employeeId.get(); }
        public SimpleIntegerProperty employeeIdProperty() { return employeeId; }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getDesignation() { return designation.get(); }
        public SimpleStringProperty designationProperty() { return designation; }

        public String getMobile() { return mobile.get(); }
        public SimpleStringProperty mobileProperty() { return mobile; }

        public boolean isAbsent() { return absent.get(); }
        public void setAbsent(boolean absent) { this.absent.set(absent); }
        public SimpleBooleanProperty absentProperty() { return absent; }

        public String getReason() { return reason.get(); }
        public void setReason(String reason) { this.reason.set(reason); }
        public SimpleStringProperty reasonProperty() { return reason; }
    }
}
