package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.Employees;
import com.frontend.service.EmployeesService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
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
import java.util.List;
import java.util.ResourceBundle;

@Component
public class AddEmployeeController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddEmployeeController.class);

    @Autowired
    private EmployeesService employeesService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    // Name fields
    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtMiddleName;

    @FXML
    private TextField txtLastName;

    // Contact fields
    @FXML
    private TextField txtMobileNo;

    @FXML
    private TextField txtAlternateMobile;

    @FXML
    private TextField txtEmail;

    // Address fields
    @FXML
    private TextField txtAddressLine;

    @FXML
    private TextField txtCity;

    @FXML
    private TextField txtTaluka;

    @FXML
    private TextField txtDistrict;

    @FXML
    private TextField txtState;

    @FXML
    private TextField txtPincode;

    // Identity fields
    @FXML
    private TextField txtAadharNo;

    // Employment fields
    @FXML
    private ComboBox<String> cmbDesignation;

    @FXML
    private TextField txtSalary;

    @FXML
    private DatePicker dpDateJoin;

    // Emergency contact fields
    @FXML
    private TextField txtEmergencyName;

    @FXML
    private TextField txtEmergencyNo;

    // Other fields
    @FXML
    private TextField txtRemarks;

    @FXML
    private CheckBox chkActiveStatus;

    // Buttons
    @FXML
    private Button btnSave;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnRefresh;

    @FXML
    private Button btnBack;

    @FXML
    private Button btnUpdate;

    // Search and Table
    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<EmployeeData> tblEmployees;

    @FXML
    private TableColumn<EmployeeData, Integer> colId;

    @FXML
    private TableColumn<EmployeeData, String> colFullName;

    @FXML
    private TableColumn<EmployeeData, String> colMobile;

    @FXML
    private TableColumn<EmployeeData, String> colDesignation;

    @FXML
    private TableColumn<EmployeeData, String> colCity;

    @FXML
    private TableColumn<EmployeeData, Float> colSalary;

    @FXML
    private TableColumn<EmployeeData, String> colStatus;

    private ObservableList<EmployeeData> employeeData = FXCollections.observableArrayList();
    private FilteredList<EmployeeData> filteredData;
    private EmployeeData selectedEmployee = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadEmployees();
    }

    private void setupUI() {
        // Setup filtered list
        filteredData = new FilteredList<>(employeeData, p -> true);

        // Setup table with filtered data
        tblEmployees.setItems(filteredData);

        // Setup designation combo box with common designations
        cmbDesignation.setItems(FXCollections.observableArrayList(
                "Waiter", "Chef", "Manager","Captain", "Receptionist", "Cleaner", "Cashier",
                "Supervisor", "Helper", "Cook", "Delivery Boy", "Security"));

        // Set default date to today
        dpDateJoin.setValue(LocalDate.now());

        // Set default active status
        chkActiveStatus.setSelected(true);
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colFullName.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        colMobile.setCellValueFactory(cellData -> cellData.getValue().mobileProperty());
        colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        colCity.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        colSalary.setCellValueFactory(cellData -> cellData.getValue().salaryProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Apply custom font to Full Name and Designation columns
        applyFullNameColumnFont();
      //  applyDesignationColumnFont();
        applyCityColumnFont();

        // Add row selection listener to open employee in edit mode
        tblEmployees.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                EmployeeData selectedEmployeeData = tblEmployees.getSelectionModel().getSelectedItem();
                if (selectedEmployeeData != null) {
                    editEmployee(selectedEmployeeData);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveEmployee());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadEmployees());
        btnUpdate.setOnAction(e -> saveEmployee());
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to Master Menu");
                navigateToMasterMenu();
            } catch (Exception ex) {
                LOG.error("Error returning to Master Menu: ", ex);
            }
        });
    }

    private void navigateToMasterMenu() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/master/MasterMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Successfully navigated to Master Menu");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to Master Menu: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) txtFirstName.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(employee -> {
            String searchText = txtSearch.getText();

            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                return employee.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                        employee.getMobile().toLowerCase().contains(lowerCaseFilter) ||
                        employee.getDesignation().toLowerCase().contains(lowerCaseFilter) ||
                        (employee.getCity() != null && employee.getCity().toLowerCase().contains(lowerCaseFilter));
            }

            return true;
        });
    }

    private void applyFullNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colFullName.setCellFactory(column -> {
                    TableCell<EmployeeData, String> cell = new TableCell<EmployeeData, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);

                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                setText(item);
                                setGraphic(null);
                            }
                        }
                    };

                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to Full Name column cells only", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Full Name column: ", e);
        }
    }

    
    private void applyCityColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colCity.setCellFactory(column -> {
                    TableCell<EmployeeData, String> cell = new TableCell<EmployeeData, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);

                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                setText(item);
                                setGraphic(null);
                            }
                        }
                    };

                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to City column cells only", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to City column: ", e);
        }
    }

    private void applyCustomFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                Font inputFont = Font.font(customFont.getFamily(), 25);

                // Apply to name fields
                applyFontToTextField(txtFirstName, inputFont, 25);
                applyFontToTextField(txtMiddleName, inputFont, 25);
                applyFontToTextField(txtLastName, inputFont, 25);

                // Apply to address fields
                applyFontToTextField(txtAddressLine, inputFont, 25);
                applyFontToTextField(txtCity, inputFont, 25);
                applyFontToTextField(txtTaluka, inputFont, 25);
                applyFontToTextField(txtDistrict, inputFont, 25);
                applyFontToTextField(txtState, inputFont, 25);

                // Apply to emergency contact name
                applyFontToTextField(txtEmergencyName, inputFont, 25);

                // Apply to remarks
                applyFontToTextField(txtRemarks, inputFont, 25);

                // Apply to search field
                applyFontToTextField(txtSearch, inputFont, 25);

                LOG.info("Custom font '{}' applied to input fields", customFont.getFamily());
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font: ", e);
        }
    }

    private void applyFontToTextField(TextField textField, Font font, int fontSize) {
        if (textField == null || font == null) {
            return;
        }

        textField.setFont(font);
        textField.setStyle(
                "-fx-font-family: '" + font.getFamily() + "';" +
                        "-fx-font-size: " + fontSize + "px;");

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            textField.setFont(font);
        });
    }

    /**
     * Helper to convert empty strings to null (for unique constraint fields)
     */
    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void saveEmployee() {
        if (!validateInput()) {
            return;
        }

        try {
            Employees employee = new Employees();
            employee.setFirstName(txtFirstName.getText().trim());
            employee.setMiddleName(emptyToNull(txtMiddleName.getText()));
            employee.setLastName(emptyToNull(txtLastName.getText()));
            employee.setMobileNo(txtMobileNo.getText().trim());
            employee.setAlternateMobileNo(emptyToNull(txtAlternateMobile.getText()));
            employee.setEmailId(emptyToNull(txtEmail.getText()));
            employee.setAddressLine(emptyToNull(txtAddressLine.getText()));
            employee.setCity(emptyToNull(txtCity.getText()));
            employee.setTaluka(emptyToNull(txtTaluka.getText()));
            employee.setDistrict(emptyToNull(txtDistrict.getText()));
            employee.setState(emptyToNull(txtState.getText()));
            employee.setPincode(emptyToNull(txtPincode.getText()));
            employee.setAadharNo(emptyToNull(txtAadharNo.getText()));

            // Get designation from combo box (could be selected or typed)
            String designation = cmbDesignation.getValue();
            if (designation == null || designation.isEmpty()) {
                designation = cmbDesignation.getEditor().getText();
            }
            employee.setDesignation(designation != null ? designation.trim() : "");

            // Parse salary if provided
            if (!txtSalary.getText().trim().isEmpty()) {
                employee.setCurrentSalary(Float.parseFloat(txtSalary.getText().trim()));
            }

            employee.setDateJoin(dpDateJoin.getValue());
            employee.setEmergencyContactName(emptyToNull(txtEmergencyName.getText()));
            employee.setEmergencyContactNo(emptyToNull(txtEmergencyNo.getText()));
            employee.setRemarks(emptyToNull(txtRemarks.getText()));
            employee.setActiveStatus(chkActiveStatus.isSelected());

            if (selectedEmployee == null) {
                // Create new employee
                employeesService.createEmployee(employee);
                alertNotification.showSuccess("Employee created successfully!");
            } else {
                // Update existing employee
                employee.setEmployeeId(selectedEmployee.getId());
                employeesService.updateEmployee(selectedEmployee.getId(), employee);
                alertNotification.showSuccess("Employee updated successfully!");
                selectedEmployee = null;
            }

            clearForm();
            loadEmployees();

        } catch (NumberFormatException e) {
            LOG.error("Invalid number format", e);
            alertNotification.showError("Please enter valid number for Salary");
        } catch (Exception e) {
            LOG.error("Error saving employee: ", e);
            alertNotification.showError("Error saving employee: " + e.getMessage());
        }
    }

    private void editEmployee(EmployeeData employee) {
        selectedEmployee = employee;

        // Load full employee data from service
        try {
            Employees fullEmployee = employeesService.getEmployeeById(employee.getId());

            txtFirstName.setText(fullEmployee.getFirstName() != null ? fullEmployee.getFirstName() : "");
            txtMiddleName.setText(fullEmployee.getMiddleName() != null ? fullEmployee.getMiddleName() : "");
            txtLastName.setText(fullEmployee.getLastName() != null ? fullEmployee.getLastName() : "");
            txtMobileNo.setText(fullEmployee.getMobileNo() != null ? fullEmployee.getMobileNo() : "");
            txtAlternateMobile.setText(fullEmployee.getAlternateMobileNo() != null ? fullEmployee.getAlternateMobileNo() : "");
            txtEmail.setText(fullEmployee.getEmailId() != null ? fullEmployee.getEmailId() : "");
            txtAddressLine.setText(fullEmployee.getAddressLine() != null ? fullEmployee.getAddressLine() : "");
            txtCity.setText(fullEmployee.getCity() != null ? fullEmployee.getCity() : "");
            txtTaluka.setText(fullEmployee.getTaluka() != null ? fullEmployee.getTaluka() : "");
            txtDistrict.setText(fullEmployee.getDistrict() != null ? fullEmployee.getDistrict() : "");
            txtState.setText(fullEmployee.getState() != null ? fullEmployee.getState() : "");
            txtPincode.setText(fullEmployee.getPincode() != null ? fullEmployee.getPincode() : "");
            txtAadharNo.setText(fullEmployee.getAadharNo() != null ? fullEmployee.getAadharNo() : "");
            cmbDesignation.setValue(fullEmployee.getDesignation());
            txtSalary.setText(fullEmployee.getCurrentSalary() != null ? String.valueOf(fullEmployee.getCurrentSalary()) : "");
            dpDateJoin.setValue(fullEmployee.getDateJoin());
            txtEmergencyName.setText(fullEmployee.getEmergencyContactName() != null ? fullEmployee.getEmergencyContactName() : "");
            txtEmergencyNo.setText(fullEmployee.getEmergencyContactNo() != null ? fullEmployee.getEmergencyContactNo() : "");
            txtRemarks.setText(fullEmployee.getRemarks() != null ? fullEmployee.getRemarks() : "");
            chkActiveStatus.setSelected(fullEmployee.getActiveStatus() != null ? fullEmployee.getActiveStatus() : true);

            // Show Update button, hide Save button
            btnSave.setVisible(false);
            btnSave.setManaged(false);
            btnUpdate.setVisible(true);
            btnUpdate.setManaged(true);

        } catch (Exception e) {
            LOG.error("Error loading employee for edit: ", e);
            alertNotification.showError("Error loading employee data: " + e.getMessage());
        }
    }

    private void clearForm() {
        txtFirstName.clear();
        txtMiddleName.clear();
        txtLastName.clear();
        txtMobileNo.clear();
        txtAlternateMobile.clear();
        txtEmail.clear();
        txtAddressLine.clear();
        txtCity.clear();
        txtTaluka.clear();
        txtDistrict.clear();
        txtState.clear();
        txtPincode.clear();
        txtAadharNo.clear();
        cmbDesignation.setValue(null);
        cmbDesignation.getEditor().clear();
        txtSalary.clear();
        dpDateJoin.setValue(LocalDate.now());
        txtEmergencyName.clear();
        txtEmergencyNo.clear();
        txtRemarks.clear();
        chkActiveStatus.setSelected(true);
        selectedEmployee = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtFirstName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter first name");
            txtFirstName.requestFocus();
            return false;
        }

        if (txtMobileNo.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter mobile number");
            txtMobileNo.requestFocus();
            return false;
        }

        // Validate mobile number format (10 digits)
        if (!txtMobileNo.getText().trim().matches("\\d{10}")) {
            alertNotification.showError("Mobile number must be 10 digits");
            txtMobileNo.requestFocus();
            return false;
        }

        // Check for duplicate mobile (except for current employee being edited)
        if (selectedEmployee == null) {
            if (employeesService.existsByMobile(txtMobileNo.getText().trim())) {
                alertNotification.showError("Employee with this mobile number already exists");
                txtMobileNo.requestFocus();
                return false;
            }
        }

        // Validate designation
        String designation = cmbDesignation.getValue();
        if (designation == null || designation.isEmpty()) {
            designation = cmbDesignation.getEditor().getText();
        }
        if (designation == null || designation.trim().isEmpty()) {
            alertNotification.showError("Please select or enter designation");
            cmbDesignation.requestFocus();
            return false;
        }

        // Validate salary if provided
        if (!txtSalary.getText().trim().isEmpty()) {
            try {
                Float.parseFloat(txtSalary.getText().trim());
            } catch (NumberFormatException e) {
                alertNotification.showError("Salary must be a valid number");
                txtSalary.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void loadEmployees() {
        try {
            List<Employees> employees = employeesService.getAllEmployees();
            employeeData.clear();

            for (Employees employee : employees) {
                employeeData.add(new EmployeeData(
                        employee.getEmployeeId(),
                        employee.getFirstName(),
                        employee.getMiddleName(),
                        employee.getLastName(),
                        employee.getMobileNo(),
                        employee.getDesignation(),
                        employee.getCity(),
                        employee.getCurrentSalary(),
                        employee.getActiveStatus()));
            }

            tblEmployees.refresh();
            LOG.info("Loaded {} employees", employees.size());

        } catch (Exception e) {
            LOG.error("Error loading employees: ", e);
            alertNotification.showError("Error loading employees: " + e.getMessage());
        }
    }

    // Inner class for table data
    public static class EmployeeData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty firstName;
        private final SimpleStringProperty middleName;
        private final SimpleStringProperty lastName;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty mobile;
        private final SimpleStringProperty designation;
        private final SimpleStringProperty city;
        private final SimpleFloatProperty salary;
        private final SimpleBooleanProperty active;
        private final SimpleStringProperty status;

        public EmployeeData(Integer id, String firstName, String middleName, String lastName,
                String mobile, String designation, String city, Float salary, Boolean active) {
            this.id = new SimpleIntegerProperty(id != null ? id : 0);
            this.firstName = new SimpleStringProperty(firstName != null ? firstName : "");
            this.middleName = new SimpleStringProperty(middleName != null ? middleName : "");
            this.lastName = new SimpleStringProperty(lastName != null ? lastName : "");

            // Build full name
            StringBuilder fullNameBuilder = new StringBuilder();
            if (firstName != null && !firstName.isEmpty()) {
                fullNameBuilder.append(firstName);
            }
            if (middleName != null && !middleName.isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(middleName);
            }
            if (lastName != null && !lastName.isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(lastName);
            }
            this.fullName = new SimpleStringProperty(fullNameBuilder.toString());

            this.mobile = new SimpleStringProperty(mobile != null ? mobile : "");
            this.designation = new SimpleStringProperty(designation != null ? designation : "");
            this.city = new SimpleStringProperty(city != null ? city : "");
            this.salary = new SimpleFloatProperty(salary != null ? salary : 0f);
            this.active = new SimpleBooleanProperty(active != null ? active : true);
            this.status = new SimpleStringProperty(active != null && active ? "Active" : "Inactive");
        }

        public int getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getFirstName() {
            return firstName.get();
        }

        public SimpleStringProperty firstNameProperty() {
            return firstName;
        }

        public String getMiddleName() {
            return middleName.get();
        }

        public SimpleStringProperty middleNameProperty() {
            return middleName;
        }

        public String getLastName() {
            return lastName.get();
        }

        public SimpleStringProperty lastNameProperty() {
            return lastName;
        }

        public String getFullName() {
            return fullName.get();
        }

        public SimpleStringProperty fullNameProperty() {
            return fullName;
        }

        public String getMobile() {
            return mobile.get();
        }

        public SimpleStringProperty mobileProperty() {
            return mobile;
        }

        public String getDesignation() {
            return designation.get();
        }

        public SimpleStringProperty designationProperty() {
            return designation;
        }

        public String getCity() {
            return city.get();
        }

        public SimpleStringProperty cityProperty() {
            return city;
        }

        public Float getSalary() {
            return salary.get();
        }

        public SimpleFloatProperty salaryProperty() {
            return salary;
        }

        public Boolean isActive() {
            return active.get();
        }

        public SimpleBooleanProperty activeProperty() {
            return active;
        }

        public String getStatus() {
            return status.get();
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}
