package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.Employee;
import com.frontend.service.EmployeeService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
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
import java.util.List;
import java.util.ResourceBundle;

@Component
public class AddEmployeeController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddEmployeeController.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtMiddleName;

    @FXML
    private TextField txtLastName;

    @FXML
    private TextField txtAddress;

    @FXML
    private TextField txtContact;

    @FXML
    private TextField txtDesignation;

    @FXML
    private TextField txtSalary;

    @FXML
    private ComboBox<String> cmbSalaryType;

    @FXML
    private ComboBox<String> cmbStatus;

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

    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<EmployeeData> tblEmployees;

    @FXML
    private TableColumn<EmployeeData, Integer> colId;

    @FXML
    private TableColumn<EmployeeData, String> colFullName;

    @FXML
    private TableColumn<EmployeeData, String> colContact;

    @FXML
    private TableColumn<EmployeeData, String> colDesignation;

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

        // Setup salary type combo box
        cmbSalaryType.setItems(FXCollections.observableArrayList(
                "Monthly", "Daily", "Hourly", "Fixed"));

        // Setup status combo box
        cmbStatus.setItems(FXCollections.observableArrayList(
                "Active", "Inactive", "On Leave", "Terminated"));
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colFullName.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        colContact.setCellValueFactory(cellData -> cellData.getValue().contactProperty());
        colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        colSalary.setCellValueFactory(cellData -> cellData.getValue().salaryProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Apply custom font to Full Name and Designation columns
        applyFullNameColumnFont();
        applyDesignationColumnFont();

        // Add row selection listener to open employee in edit mode
        tblEmployees.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click to select
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
        // Setup search by employee name or contact
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(employee -> {
            String searchText = txtSearch.getText();

            // Filter by search text (name, contact, or designation)
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                return employee.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                        employee.getContact().toLowerCase().contains(lowerCaseFilter) ||
                        employee.getDesignation().toLowerCase().contains(lowerCaseFilter);
            }

            return true;
        });
    }

    /**
     * Apply custom font to Full Name column cells only (not header)
     */
    private void applyFullNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
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

                    // Apply inline style to cell only (not affecting header)
                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to Full Name column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Full Name column: ", e);
        }
    }

    /**
     * Apply custom font to Designation column cells only (not header)
     */
    private void applyDesignationColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colDesignation.setCellFactory(column -> {
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

                    // Apply inline style to cell only (not affecting header)
                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 25px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to Designation column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Designation column: ", e);
        }
    }

    /**
     * Apply custom font to input fields from session settings
     */
    private void applyCustomFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                // Apply font to input fields with appropriate size
                Font inputFont = Font.font(customFont.getFamily(), 25);

                // Apply to name fields
                applyFontToTextField(txtFirstName, inputFont, 25);
                applyFontToTextField(txtMiddleName, inputFont, 25);
                applyFontToTextField(txtLastName, inputFont, 25);

                // Apply to designation field
                applyFontToTextField(txtDesignation, inputFont, 25);

                LOG.info("Custom font '{}' applied to name and designation input fields", customFont.getFamily());
            } else {
                LOG.debug("No custom font configured");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font: ", e);
        }
    }

    /**
     * Helper method to apply custom font to a text field with persistence
     */
    private void applyFontToTextField(TextField textField, Font font, int fontSize) {
        if (textField == null || font == null) {
            return;
        }

        // Set the font object
        textField.setFont(font);

        // Set inline style to override CSS and ensure font persists
        textField.setStyle(
                "-fx-font-family: '" + font.getFamily() + "';" +
                        "-fx-font-size: " + fontSize + "px;");

        // Add focus listener to ensure font persists through focus changes
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            textField.setFont(font);
        });
    }

    private void saveEmployee() {
        if (!validateInput()) {
            return;
        }

        try {
            Employee employee = new Employee();
            employee.setFirstName(txtFirstName.getText().trim());
            employee.setMiddleName(txtMiddleName.getText().trim());
            employee.setLastName(txtLastName.getText().trim());
            employee.setAddress(txtAddress.getText().trim());
            employee.setContact(txtContact.getText().trim());
            employee.setDesignation(txtDesignation.getText().trim());
            employee.setSalary(Float.parseFloat(txtSalary.getText().trim()));
            employee.setSalaryType(cmbSalaryType.getValue());
            employee.setStatus(cmbStatus.getValue());

            if (selectedEmployee == null) {
                // Create new employee
                employeeService.createEmployee(employee);
                alertNotification.showSuccess("Employee created successfully!");
            } else {
                // Update existing employee
                employee.setId(selectedEmployee.getId());
                employeeService.updateEmployee(selectedEmployee.getId(), employee);
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
        txtFirstName.setText(employee.getFirstName());
        txtMiddleName.setText(employee.getMiddleName());
        txtLastName.setText(employee.getLastName());
        txtAddress.setText(employee.getAddress());
        txtContact.setText(employee.getContact());
        txtDesignation.setText(employee.getDesignation());
        txtSalary.setText(String.valueOf(employee.getSalary()));
        cmbSalaryType.setValue(employee.getSalaryType());
        cmbStatus.setValue(employee.getStatus());

        // Show Update button, hide Save button
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void clearForm() {
        txtFirstName.clear();
        txtMiddleName.clear();
        txtLastName.clear();
        txtAddress.clear();
        txtContact.clear();
        txtDesignation.clear();
        txtSalary.clear();
        cmbSalaryType.setValue(null);
        cmbStatus.setValue(null);
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

        if (txtMiddleName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter middle name");
            txtMiddleName.requestFocus();
            return false;
        }

        if (txtLastName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter last name");
            txtLastName.requestFocus();
            return false;
        }

        if (txtAddress.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter address");
            txtAddress.requestFocus();
            return false;
        }

        if (txtContact.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter contact number");
            txtContact.requestFocus();
            return false;
        }

        // Validate contact number format (10 digits)
        if (!txtContact.getText().trim().matches("\\d{10}")) {
            alertNotification.showError("Contact number must be 10 digits");
            txtContact.requestFocus();
            return false;
        }

        if (txtDesignation.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter designation");
            txtDesignation.requestFocus();
            return false;
        }

        if (txtSalary.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter salary");
            txtSalary.requestFocus();
            return false;
        }

        try {
            Float.parseFloat(txtSalary.getText().trim());
        } catch (NumberFormatException e) {
            alertNotification.showError("Salary must be a valid number");
            txtSalary.requestFocus();
            return false;
        }

        if (cmbSalaryType.getValue() == null || cmbSalaryType.getValue().isEmpty()) {
            alertNotification.showError("Please select salary type");
            cmbSalaryType.requestFocus();
            return false;
        }

        if (cmbStatus.getValue() == null || cmbStatus.getValue().isEmpty()) {
            alertNotification.showError("Please select status");
            cmbStatus.requestFocus();
            return false;
        }

        return true;
    }

    private void loadEmployees() {
        try {
            List<Employee> employees = employeeService.getAllEmployees();
            employeeData.clear();

            for (Employee employee : employees) {
                employeeData.add(new EmployeeData(
                        employee.getId(),
                        employee.getFirstName(),
                        employee.getMiddleName(),
                        employee.getLastName(),
                        employee.getAddress(),
                        employee.getContact(),
                        employee.getDesignation(),
                        employee.getSalary(),
                        employee.getSalaryType(),
                        employee.getStatus()));
            }

            // Refresh the table view
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
        private final SimpleStringProperty address;
        private final SimpleStringProperty contact;
        private final SimpleStringProperty designation;
        private final SimpleFloatProperty salary;
        private final SimpleStringProperty salaryType;
        private final SimpleStringProperty status;

        public EmployeeData(int id, String firstName, String middleName, String lastName,
                String address, String contact, String designation, Float salary,
                String salaryType, String status) {
            this.id = new SimpleIntegerProperty(id);
            this.firstName = new SimpleStringProperty(firstName);
            this.middleName = new SimpleStringProperty(middleName);
            this.lastName = new SimpleStringProperty(lastName);
            this.fullName = new SimpleStringProperty(firstName + " " + middleName + " " + lastName);
            this.address = new SimpleStringProperty(address);
            this.contact = new SimpleStringProperty(contact);
            this.designation = new SimpleStringProperty(designation);
            this.salary = new SimpleFloatProperty(salary);
            this.salaryType = new SimpleStringProperty(salaryType);
            this.status = new SimpleStringProperty(status);
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

        public String getAddress() {
            return address.get();
        }

        public SimpleStringProperty addressProperty() {
            return address;
        }

        public String getContact() {
            return contact.get();
        }

        public SimpleStringProperty contactProperty() {
            return contact;
        }

        public String getDesignation() {
            return designation.get();
        }

        public SimpleStringProperty designationProperty() {
            return designation;
        }

        public Float getSalary() {
            return salary.get();
        }

        public SimpleFloatProperty salaryProperty() {
            return salary;
        }

        public String getSalaryType() {
            return salaryType.get();
        }

        public SimpleStringProperty salaryTypeProperty() {
            return salaryType;
        }

        public String getStatus() {
            return status.get();
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}
