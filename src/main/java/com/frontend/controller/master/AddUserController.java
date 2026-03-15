package com.frontend.controller.master;

import com.frontend.entity.Employees;
import com.frontend.entity.User;
import com.frontend.service.EmployeesService;
import com.frontend.service.SessionService;
import com.frontend.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class AddUserController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddUserController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Autowired
    private UserService userService;

    @Autowired
    private EmployeesService employeesService;

    // Form Fields
    @FXML private ComboBox<Employees> cmbEmployee;
    @FXML private VBox employeeInfoBox;
    @FXML private Label lblEmployeeMobile;
    @FXML private Label lblEmployeeDesignation;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cmbRole;

    // Buttons
    @FXML private Button btnBack;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnRefresh;

    // Table
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, Long> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmployeeName;
    @FXML private TableColumn<User, String> colDesignation;
    @FXML private TableColumn<User, String> colCity;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreatedAt;

    // Search
    @FXML private TextField txtSearch;

    // Data
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private User selectedUser = null;

    // Available roles
    private static final String[] ROLES = {"ADMIN", "MANAGER", "CASHIER","CAPTAIN", "WAITER", "USER"};

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing AddUserController");
        setupBackButton();
        setupEmployeeComboBox();
        setupRoleComboBox();
        setupTable();
        setupSearch();
        setupButtons();
        applyCustomFont();
        loadUsers();
        loadEmployees();
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                BorderPane mainPane = (BorderPane) btnBack.getScene().lookup("#mainPane");
                if (mainPane != null) {
                    javafx.scene.Node initialDashboard = (javafx.scene.Node) mainPane.getProperties().get("initialDashboard");
                    if (initialDashboard != null) {
                        mainPane.setCenter(initialDashboard);
                    }
                }
            } catch (Exception ex) {
                LOG.error("Error navigating back: ", ex);
            }
        });
    }

    private void setupEmployeeComboBox() {
        // Set converter for displaying employee names
        cmbEmployee.setConverter(new StringConverter<Employees>() {
            @Override
            public String toString(Employees employee) {
                if (employee == null) return "";
                String name = employee.getFullName();
                String designation = employee.getDesignation() != null ? " (" + employee.getDesignation() + ")" : "";
                return name + designation;
            }

            @Override
            public Employees fromString(String string) {
                return null;
            }
        });

        // Apply custom font to combo box cells
        applyCustomFontToEmployeeComboBox();

        // Add selection listener using setOnAction for immediate response
        cmbEmployee.setOnAction(event -> {
            Employees selected = cmbEmployee.getValue();
            LOG.info("Employee ComboBox action - selected: {}",
                selected != null ? selected.getFullName() : "null");
            onEmployeeSelected(selected);
        });
    }

    private void onEmployeeSelected(Employees selected) {
        LOG.info("onEmployeeSelected called with: {}", selected != null ? selected.getFullName() : "null");

        if (selected != null) {
            // Show employee info
            employeeInfoBox.setVisible(true);
            employeeInfoBox.setManaged(true);

            // Update mobile label
            String mobile = selected.getMobileNo() != null ? selected.getMobileNo() : "-";
            lblEmployeeMobile.setText(mobile);
            LOG.info("Setting mobile to: {}", mobile);

            // Update designation label
            String designation = selected.getDesignation() != null ? selected.getDesignation() : "-";
            lblEmployeeDesignation.setText(designation);
            LOG.info("Setting designation to: {}", designation);

            // Always update username when employee changes (unless in edit mode)
            if (selectedUser == null) {
                // Use firstName directly without toLowerCase (for Marathi/non-ASCII text)
                String firstName = selected.getFirstName();
                if (firstName != null) {
                    String suggestedUsername = firstName.trim();
                    txtUsername.setText(suggestedUsername);
                    LOG.info("Setting suggested username to: {}", suggestedUsername);
                }
            }

            LOG.info("Employee info updated - Mobile: {}, Designation: {}", mobile, designation);
        } else {
            // Hide employee info
            employeeInfoBox.setVisible(false);
            employeeInfoBox.setManaged(false);
            lblEmployeeMobile.setText("-");
            lblEmployeeDesignation.setText("-");

            // Clear username if not in edit mode
            if (selectedUser == null) {
                txtUsername.clear();
            }
        }
    }

    private void applyCustomFontToEmployeeComboBox() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Apply custom font to combo box button cell (selected item display)
                cmbEmployee.setButtonCell(new ListCell<Employees>() {
                    @Override
                    protected void updateItem(Employees item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            String name = item.getFullName();
                            String designation = item.getDesignation() != null ? " (" + item.getDesignation() + ")" : "";
                            setText(name + designation);
                        }
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                    }
                });

                // Apply custom font to combo box dropdown cells
                cmbEmployee.setCellFactory(listView -> new ListCell<Employees>() {
                    @Override
                    protected void updateItem(Employees item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            String name = item.getFullName();
                            String designation = item.getDesignation() != null ? " (" + item.getDesignation() + ")" : "";
                            setText(name + designation);
                        }
                        setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                    }
                });

                LOG.info("Custom font '{}' applied to employee ComboBox", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to employee ComboBox: ", e);
        }
    }

    private void setupRoleComboBox() {
        cmbRole.setItems(FXCollections.observableArrayList(ROLES));
        cmbRole.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        colId.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleLongProperty(cellData.getValue().getId()).asObject());

        colUsername.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUsername()));

        colEmployeeName.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String employeeName = user.getEmployee() != null ? user.getEmployee().getFullName() : "Not linked";
            return new SimpleStringProperty(employeeName);
        });

        colDesignation.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String designation = user.getEmployee() != null && user.getEmployee().getDesignation() != null
                ? user.getEmployee().getDesignation() : "-";
            return new SimpleStringProperty(designation);
        });

        colCity.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String city = user.getEmployee() != null && user.getEmployee().getCity() != null
                ? user.getEmployee().getCity() : "-";
            return new SimpleStringProperty(city);
        });

        colRole.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getRole()));

        colCreatedAt.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String dateStr = user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : "-";
            return new SimpleStringProperty(dateStr);
        });

        // Apply custom font to username, employee name and city columns
        applyUsernameColumnFont();
        applyEmployeeNameColumnFont();
        applyCityColumnFont();

        // Row selection
        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectUser(newVal);
            }
        });

        // Setup filtered list
        filteredUsers = new FilteredList<>(userList, p -> true);
        tblUsers.setItems(filteredUsers);
    }

    private void applyUsernameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colUsername.setCellFactory(column -> {
                    TableCell<User, String> cell = new TableCell<User, String>() {
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

                LOG.info("Custom font '{}' applied to Username column", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Username column: ", e);
        }
    }

    private void applyEmployeeNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colEmployeeName.setCellFactory(column -> {
                    TableCell<User, String> cell = new TableCell<User, String>() {
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

                LOG.info("Custom font '{}' applied to Employee Name column", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Employee Name column: ", e);
        }
    }

    private void applyCityColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colCity.setCellFactory(column -> {
                    TableCell<User, String> cell = new TableCell<User, String>() {
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

                LOG.info("Custom font '{}' applied to City column", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to City column: ", e);
        }
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredUsers.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();

                // Match username
                if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                // Match employee name
                if (user.getEmployee() != null &&
                    user.getEmployee().getFullName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                // Match role
                if (user.getRole().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
        });
    }

    private void setupButtons() {
        btnSave.setOnAction(e -> saveUser());
        btnUpdate.setOnAction(e -> updateUser());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> {
            loadUsers();
            loadEmployees();
        });
    }

    private void applyCustomFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                Font inputFont = Font.font(customFont.getFamily(), 20);

                // Apply to username field
                applyFontToTextField(txtUsername, inputFont, 20);

                // Apply to search field
                applyFontToTextField(txtSearch, inputFont, 20);

                // Apply to info labels
                String fontFamily = customFont.getFamily();
                lblEmployeeMobile.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px;");
                lblEmployeeDesignation.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px;");

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

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            userList.clear();
            userList.addAll(users);
            LOG.info("Loaded {} users", users.size());
        } catch (Exception e) {
            LOG.error("Error loading users: ", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void loadEmployees() {
        try {
            // Load only active employees who don't have user accounts yet
            List<Employees> allEmployees = employeesService.getActiveEmployees();
            ObservableList<Employees> availableEmployees = FXCollections.observableArrayList();

            for (Employees emp : allEmployees) {
                // Check if employee already has a user account
                if (!userService.existsByEmployee(emp)) {
                    availableEmployees.add(emp);
                }
            }

            cmbEmployee.setItems(availableEmployees);
            LOG.info("Loaded {} available employees", availableEmployees.size());
        } catch (Exception e) {
            LOG.error("Error loading employees: ", e);
        }
    }

    private void selectUser(User user) {
        selectedUser = user;

        // Populate form
        if (user.getEmployee() != null) {
            // Need to add the employee to the combo box if not present
            Employees emp = user.getEmployee();
            if (!cmbEmployee.getItems().contains(emp)) {
                cmbEmployee.getItems().add(emp);
            }
            cmbEmployee.setValue(emp);
        } else {
            cmbEmployee.setValue(null);
        }

        txtUsername.setText(user.getUsername());
        txtPassword.clear();
        txtConfirmPassword.clear();
        cmbRole.setValue(user.getRole());

        // Switch to update mode
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void saveUser() {
        try {
            // Validate
            if (!validateForm()) {
                return;
            }

            // Check password match
            if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Passwords do not match!");
                return;
            }

            // Create user
            User user = new User();
            user.setUsername(txtUsername.getText().trim());
            user.setPassword(txtPassword.getText());
            user.setRole(cmbRole.getValue());
            user.setEmployee(cmbEmployee.getValue());

            userService.createUser(user);

            showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully!");
            clearForm();
            loadUsers();
            loadEmployees(); // Reload to remove used employee from list

        } catch (Exception e) {
            LOG.error("Error saving user: ", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create user: " + e.getMessage());
        }
    }

    private void updateUser() {
        try {
            if (selectedUser == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a user to update!");
                return;
            }

            // Validate
            if (!validateFormForUpdate()) {
                return;
            }

            // Check password match if password is being changed
            if (!txtPassword.getText().isEmpty()) {
                if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", "Passwords do not match!");
                    return;
                }
            }

            // Update user
            selectedUser.setUsername(txtUsername.getText().trim());
            selectedUser.setRole(cmbRole.getValue());
            selectedUser.setEmployee(cmbEmployee.getValue());

            // Only update password if provided
            if (!txtPassword.getText().isEmpty()) {
                selectedUser.setPassword(txtPassword.getText());
            }

            userService.updateUser(selectedUser.getId(), selectedUser);

            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            clearForm();
            loadUsers();
            loadEmployees();

        } catch (Exception e) {
            LOG.error("Error updating user: ", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (cmbEmployee.getValue() == null) {
            errors.append("- Please select an employee\n");
        }

        if (txtUsername.getText().trim().isEmpty()) {
            errors.append("- Username is required\n");
        }

        if (txtPassword.getText().trim().isEmpty()) {
            errors.append("- Password is required\n");
        }

        if (txtConfirmPassword.getText().trim().isEmpty()) {
            errors.append("- Please confirm password\n");
        }

        if (cmbRole.getValue() == null) {
            errors.append("- Please select a role\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", errors.toString());
            return false;
        }

        return true;
    }

    private boolean validateFormForUpdate() {
        StringBuilder errors = new StringBuilder();

        if (txtUsername.getText().trim().isEmpty()) {
            errors.append("- Username is required\n");
        }

        if (cmbRole.getValue() == null) {
            errors.append("- Please select a role\n");
        }

        // For update, password is optional but if provided, confirm is required
        if (!txtPassword.getText().isEmpty() && txtConfirmPassword.getText().isEmpty()) {
            errors.append("- Please confirm password\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", errors.toString());
            return false;
        }

        return true;
    }

    private void clearForm() {
        selectedUser = null;
        cmbEmployee.setValue(null);
        txtUsername.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        cmbRole.getSelectionModel().selectFirst();

        employeeInfoBox.setVisible(false);
        employeeInfoBox.setManaged(false);
        lblEmployeeMobile.setText("-");
        lblEmployeeDesignation.setText("-");

        // Switch back to save mode
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);

        tblUsers.getSelectionModel().clearSelection();

        // Reload employees to get fresh list
        loadEmployees();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
