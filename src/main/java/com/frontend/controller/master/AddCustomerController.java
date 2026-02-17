package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.Customer;
import com.frontend.service.CustomerService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
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

import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Component
public class AddCustomerController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddCustomerController.class);

    private Stage dialogStage;
    private Consumer<Customer> onCustomerSaved;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtCustomerKey;

    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtMiddleName;

    @FXML
    private TextField txtLastName;

    @FXML
    private TextField txtMobileNo;

    @FXML
    private TextField txtEmailId;

    @FXML
    private TextField txtFlatNo;

    @FXML
    private TextField txtStreetName;

    @FXML
    private TextField txtCity;

    @FXML
    private TextField txtDistrict;

    @FXML
    private TextField txtTaluka;

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
    private TableView<CustomerData> tblCustomers;

    @FXML
    private TableColumn<CustomerData, Integer> colId;

    @FXML
    private TableColumn<CustomerData, String> colCustomerKey;

    @FXML
    private TableColumn<CustomerData, String> colFullName;

    @FXML
    private TableColumn<CustomerData, String> colMobileNo;

    @FXML
    private TableColumn<CustomerData, String> colEmailId;

    @FXML
    private TableColumn<CustomerData, String> colCity;

    @FXML
    private TableColumn<CustomerData, String> colDistrict;

    private ObservableList<CustomerData> customerData = FXCollections.observableArrayList();
    private FilteredList<CustomerData> filteredData;
    private CustomerData selectedCustomer = null;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnCustomerSaved(Consumer<Customer> onCustomerSaved) {
        this.onCustomerSaved = onCustomerSaved;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadCustomers();
    }

    private void setupUI() {
        // Setup filtered list
        filteredData = new FilteredList<>(customerData, p -> true);

        // Setup table with filtered data
        tblCustomers.setItems(filteredData);
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colCustomerKey.setCellValueFactory(cellData -> cellData.getValue().customerKeyProperty());
        colFullName.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        colMobileNo.setCellValueFactory(cellData -> cellData.getValue().mobileNoProperty());
        colEmailId.setCellValueFactory(cellData -> cellData.getValue().emailIdProperty());
        colCity.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        colDistrict.setCellValueFactory(cellData -> cellData.getValue().districtProperty());

        // Apply custom font to table columns
        applyFullNameColumnFont();
        applyCityColumnFont();
        applyDistrictColumnFont();

        // Add row selection listener to open customer in edit mode
        tblCustomers.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click to select
                CustomerData selectedCustomerData = tblCustomers.getSelectionModel().getSelectedItem();
                if (selectedCustomerData != null) {
                    editCustomer(selectedCustomerData);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveCustomer());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadCustomers());
        btnUpdate.setOnAction(e -> saveCustomer());
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                if (dialogStage != null) {
                    LOG.info("Back button clicked - closing dialog");
                    dialogStage.close();
                } else {
                    LOG.info("Back button clicked - returning to Master Menu");
                    navigateToMasterMenu();
                }
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
            return (BorderPane) txtCustomerKey.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        // Setup search by customer name or mobile
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(customer -> {
            String searchText = txtSearch.getText();

            // Filter by search text (name, mobile, or email)
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                return customer.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                       customer.getMobileNo().toLowerCase().contains(lowerCaseFilter) ||
                       customer.getEmailId().toLowerCase().contains(lowerCaseFilter) ||
                       customer.getCustomerKey().toLowerCase().contains(lowerCaseFilter);
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
                    TableCell<CustomerData, String> cell = new TableCell<CustomerData, String>() {
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
     * Apply custom font to City column cells only (not header)
     */
    private void applyCityColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colCity.setCellFactory(column -> {
                    TableCell<CustomerData, String> cell = new TableCell<CustomerData, String>() {
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

                LOG.info("Custom font '{}' applied to City column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to City column: ", e);
        }
    }

    /**
     * Apply custom font to District column cells only (not header)
     */
    private void applyDistrictColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colDistrict.setCellFactory(column -> {
                    TableCell<CustomerData, String> cell = new TableCell<CustomerData, String>() {
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

                LOG.info("Custom font '{}' applied to District column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to District column: ", e);
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

                // Apply to address fields
                applyFontToTextField(txtStreetName, inputFont, 25);
                applyFontToTextField(txtCity, inputFont, 25);
                applyFontToTextField(txtDistrict, inputFont, 25);
                applyFontToTextField(txtTaluka, inputFont, 25);

                LOG.info("Custom font '{}' applied to name and address input fields", customFont.getFamily());
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
                        "-fx-font-size: " + fontSize + "px;"
        );

        // Add focus listener to ensure font persists through focus changes
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            textField.setFont(font);
        });
    }

    private void saveCustomer() {
        if (!validateInput()) {
            return;
        }

        try {
            Customer customer = new Customer();
            customer.setCustomerKey(txtCustomerKey.getText().trim());
            customer.setFirstName(txtFirstName.getText().trim());
            customer.setMiddleName(txtMiddleName.getText().trim());
            customer.setLastName(txtLastName.getText().trim());
            customer.setMobileNo(txtMobileNo.getText().trim());
            customer.setEmailId(txtEmailId.getText().trim());
            customer.setFlatNo(Integer.parseInt(txtFlatNo.getText().trim()));
            customer.setStreetName(txtStreetName.getText().trim());
            customer.setCity(txtCity.getText().trim());
            customer.setDistrict(txtDistrict.getText().trim());
            customer.setTaluka(txtTaluka.getText().trim());

            Customer savedCustomer;
            if (selectedCustomer == null) {
                // Create new customer
                savedCustomer = customerService.createCustomer(customer);
                alertNotification.showSuccess("Customer created successfully!");
            } else {
                // Update existing customer
                customer.setId(selectedCustomer.getId());
                savedCustomer = customerService.updateCustomer(selectedCustomer.getId(), customer);
                alertNotification.showSuccess("Customer updated successfully!");
                selectedCustomer = null;
            }

            clearForm();
            loadCustomers();

            // If opened as dialog, notify caller and close
            if (onCustomerSaved != null && savedCustomer != null) {
                onCustomerSaved.accept(savedCustomer);
            }
            if (dialogStage != null) {
                dialogStage.close();
            }

        } catch (NumberFormatException e) {
            LOG.error("Invalid number format", e);
            alertNotification.showError("Please enter valid number for Flat No");
        } catch (Exception e) {
            LOG.error("Error saving customer: ", e);
            alertNotification.showError("Error saving customer: " + e.getMessage());
        }
    }

    private void editCustomer(CustomerData customer) {
        selectedCustomer = customer;
        txtCustomerKey.setText(customer.getCustomerKey());
        txtFirstName.setText(customer.getFirstName());
        txtMiddleName.setText(customer.getMiddleName());
        txtLastName.setText(customer.getLastName());
        txtMobileNo.setText(customer.getMobileNo());
        txtEmailId.setText(customer.getEmailId());
        txtFlatNo.setText(String.valueOf(customer.getFlatNo()));
        txtStreetName.setText(customer.getStreetName());
        txtCity.setText(customer.getCity());
        txtDistrict.setText(customer.getDistrict());
        txtTaluka.setText(customer.getTaluka());

        // Show Update button, hide Save button
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void clearForm() {
        txtCustomerKey.clear();
        txtFirstName.clear();
        txtMiddleName.clear();
        txtLastName.clear();
        txtMobileNo.clear();
        txtEmailId.clear();
        txtFlatNo.clear();
        txtStreetName.clear();
        txtCity.clear();
        txtDistrict.clear();
        txtTaluka.clear();
        selectedCustomer = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtCustomerKey.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter customer key");
            txtCustomerKey.requestFocus();
            return false;
        }

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

        if (txtEmailId.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter email address");
            txtEmailId.requestFocus();
            return false;
        }

        // Validate email format
        if (!txtEmailId.getText().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            alertNotification.showError("Please enter valid email address");
            txtEmailId.requestFocus();
            return false;
        }

        if (txtFlatNo.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter flat/house number");
            txtFlatNo.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(txtFlatNo.getText().trim());
        } catch (NumberFormatException e) {
            alertNotification.showError("Flat/House number must be a valid number");
            txtFlatNo.requestFocus();
            return false;
        }

        if (txtStreetName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter street name");
            txtStreetName.requestFocus();
            return false;
        }

        if (txtCity.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter city");
            txtCity.requestFocus();
            return false;
        }

        if (txtDistrict.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter district");
            txtDistrict.requestFocus();
            return false;
        }

        if (txtTaluka.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter taluka");
            txtTaluka.requestFocus();
            return false;
        }

        return true;
    }

    private void loadCustomers() {
        try {
            List<Customer> customers = customerService.getAllCustomers();
            customerData.clear();

            for (Customer customer : customers) {
                customerData.add(new CustomerData(
                        customer.getId(),
                        customer.getCustomerKey(),
                        customer.getFirstName(),
                        customer.getMiddleName(),
                        customer.getLastName(),
                        customer.getMobileNo(),
                        customer.getEmailId(),
                        customer.getFlatNo(),
                        customer.getStreetName(),
                        customer.getCity(),
                        customer.getDistrict(),
                        customer.getTaluka()
                ));
            }

            // Refresh the table view
            tblCustomers.refresh();

            LOG.info("Loaded {} customers", customers.size());

        } catch (Exception e) {
            LOG.error("Error loading customers: ", e);
            alertNotification.showError("Error loading customers: " + e.getMessage());
        }
    }

    // Inner class for table data
    public static class CustomerData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty customerKey;
        private final SimpleStringProperty firstName;
        private final SimpleStringProperty middleName;
        private final SimpleStringProperty lastName;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty mobileNo;
        private final SimpleStringProperty emailId;
        private final SimpleIntegerProperty flatNo;
        private final SimpleStringProperty streetName;
        private final SimpleStringProperty city;
        private final SimpleStringProperty district;
        private final SimpleStringProperty taluka;

        public CustomerData(Integer id, String customerKey, String firstName, String middleName,
                           String lastName, String mobileNo, String emailId, Integer flatNo,
                           String streetName, String city, String district, String taluka) {
            this.id = new SimpleIntegerProperty(id);
            this.customerKey = new SimpleStringProperty(customerKey);
            this.firstName = new SimpleStringProperty(firstName);
            this.middleName = new SimpleStringProperty(middleName);
            this.lastName = new SimpleStringProperty(lastName);
            this.fullName = new SimpleStringProperty(firstName + " " + middleName + " " + lastName);
            this.mobileNo = new SimpleStringProperty(mobileNo);
            this.emailId = new SimpleStringProperty(emailId);
            this.flatNo = new SimpleIntegerProperty(flatNo);
            this.streetName = new SimpleStringProperty(streetName);
            this.city = new SimpleStringProperty(city);
            this.district = new SimpleStringProperty(district);
            this.taluka = new SimpleStringProperty(taluka);
        }

        public Integer getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getCustomerKey() {
            return customerKey.get();
        }

        public SimpleStringProperty customerKeyProperty() {
            return customerKey;
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

        public String getMobileNo() {
            return mobileNo.get();
        }

        public SimpleStringProperty mobileNoProperty() {
            return mobileNo;
        }

        public String getEmailId() {
            return emailId.get();
        }

        public SimpleStringProperty emailIdProperty() {
            return emailId;
        }

        public Integer getFlatNo() {
            return flatNo.get();
        }

        public SimpleIntegerProperty flatNoProperty() {
            return flatNo;
        }

        public String getStreetName() {
            return streetName.get();
        }

        public SimpleStringProperty streetNameProperty() {
            return streetName;
        }

        public String getCity() {
            return city.get();
        }

        public SimpleStringProperty cityProperty() {
            return city;
        }

        public String getDistrict() {
            return district.get();
        }

        public SimpleStringProperty districtProperty() {
            return district;
        }

        public String getTaluka() {
            return taluka.get();
        }

        public SimpleStringProperty talukaProperty() {
            return taluka;
        }
    }
}
