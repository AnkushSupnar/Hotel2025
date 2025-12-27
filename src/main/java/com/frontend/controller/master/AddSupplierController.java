package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Supplier;
import com.frontend.service.SupplierService;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class AddSupplierController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddSupplierController.class);

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtContact;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtAddress;

    @FXML
    private TextField txtCity;

    @FXML
    private TextField txtState;

    @FXML
    private TextField txtPincode;

    @FXML
    private TextField txtGstNo;

    @FXML
    private TextField txtPanNo;

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
    private TableView<SupplierData> tblSuppliers;

    @FXML
    private TableColumn<SupplierData, Integer> colId;

    @FXML
    private TableColumn<SupplierData, String> colName;

    @FXML
    private TableColumn<SupplierData, String> colContact;

    @FXML
    private TableColumn<SupplierData, String> colEmail;

    @FXML
    private TableColumn<SupplierData, String> colAddress;

    @FXML
    private TableColumn<SupplierData, String> colCity;

    @FXML
    private TableColumn<SupplierData, String> colGstNo;

    private ObservableList<SupplierData> supplierData = FXCollections.observableArrayList();
    private FilteredList<SupplierData> filteredData;
    private SupplierData selectedSupplier = null;
    private AutoCompleteTextField autoCompleteSearch;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadSuppliers();
    }

    private void setupUI() {
        // Setup filtered list
        filteredData = new FilteredList<>(supplierData, p -> true);

        // Setup table with filtered data
        tblSuppliers.setItems(filteredData);
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colContact.setCellValueFactory(cellData -> cellData.getValue().contactProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        colAddress.setCellValueFactory(cellData -> cellData.getValue().addressProperty());
        colCity.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        colGstNo.setCellValueFactory(cellData -> cellData.getValue().gstNoProperty());

        // Apply custom font to table columns
        applyNameColumnFont();
        applyAddressColumnFont();
        applyCityColumnFont();

        // Add row selection listener to open supplier in edit mode
        tblSuppliers.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click to select
                SupplierData selectedSupplierData = tblSuppliers.getSelectionModel().getSelectedItem();
                if (selectedSupplierData != null) {
                    editSupplier(selectedSupplierData);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveSupplier());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadSuppliers());
        btnUpdate.setOnAction(e -> saveSupplier());
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
            return (BorderPane) txtName.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        // Setup AutoCompleteTextField for search
        List<String> supplierNames = new ArrayList<>();

        // Get custom font from session
        Font customFont = SessionService.getCustomFont(25.0);

        // Create AutoCompleteTextField with custom font
        if (customFont != null) {
            autoCompleteSearch = new AutoCompleteTextField(txtSearch, supplierNames, customFont);
            // Apply custom font styling to the text field
            txtSearch.setStyle("-fx-font-family: '" + customFont.getFamily() + "'; -fx-font-size: 25px;");
        } else {
            autoCompleteSearch = new AutoCompleteTextField(txtSearch, supplierNames);
        }

        // Use contains filter for better search experience
        autoCompleteSearch.setUseContainsFilter(true);

        // Set selection callback to filter table
        autoCompleteSearch.setOnSelectionCallback(selectedName -> {
            applyFilters();
        });

        // Also filter on text change
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    /**
     * Update the autocomplete suggestions with current supplier names
     */
    private void updateAutoCompleteSuggestions() {
        if (autoCompleteSearch != null) {
            List<String> supplierNames = supplierData.stream()
                    .map(SupplierData::getName)
                    .filter(name -> name != null && !name.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // Also add contact numbers and cities for searching
            List<String> contacts = supplierData.stream()
                    .map(SupplierData::getContact)
                    .filter(c -> c != null && !c.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            List<String> cities = supplierData.stream()
                    .map(SupplierData::getCity)
                    .filter(c -> c != null && !c.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            supplierNames.addAll(contacts);
            supplierNames.addAll(cities);

            autoCompleteSearch.setSuggestions(supplierNames);
            LOG.info("Updated autocomplete suggestions with {} entries", supplierNames.size());
        }
    }

    private void applyFilters() {
        filteredData.setPredicate(supplier -> {
            String searchText = txtSearch.getText();

            // Filter by search text (name, contact, or email)
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                return supplier.getName().toLowerCase().contains(lowerCaseFilter) ||
                       supplier.getContact().toLowerCase().contains(lowerCaseFilter) ||
                       supplier.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                       supplier.getCity().toLowerCase().contains(lowerCaseFilter);
            }

            return true;
        });
    }

    /**
     * Apply custom font to Name column cells only (not header)
     */
    private void applyNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colName.setCellFactory(column -> {
                    TableCell<SupplierData, String> cell = new TableCell<SupplierData, String>() {
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

                LOG.info("Custom font '{}' applied to Name column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Name column: ", e);
        }
    }

    /**
     * Apply custom font to Address column cells only (not header)
     */
    private void applyAddressColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colAddress.setCellFactory(column -> {
                    TableCell<SupplierData, String> cell = new TableCell<SupplierData, String>() {
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

                LOG.info("Custom font '{}' applied to Address column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Address column: ", e);
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
                    TableCell<SupplierData, String> cell = new TableCell<SupplierData, String>() {
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
     * Apply custom font to input fields from session settings
     */
    private void applyCustomFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                // Apply font to input fields with appropriate size
                Font inputFont = Font.font(customFont.getFamily(), 25);

                // Apply to name and address fields (Marathi text)
                applyFontToTextField(txtName, inputFont, 25);
                applyFontToTextField(txtAddress, inputFont, 25);
                applyFontToTextField(txtCity, inputFont, 25);
                applyFontToTextField(txtState, inputFont, 25);

                LOG.info("Custom font '{}' applied to name, address, city, and state input fields", customFont.getFamily());
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

    private void saveSupplier() {
        if (!validateInput()) {
            return;
        }

        try {
            Supplier supplier = new Supplier();
            supplier.setName(txtName.getText().trim());
            supplier.setContact(txtContact.getText().trim());
            supplier.setEmail(txtEmail.getText().trim());
            supplier.setAddress(txtAddress.getText().trim());
            supplier.setCity(txtCity.getText().trim());
            supplier.setState(txtState.getText().trim());
            supplier.setPincode(txtPincode.getText().trim());
            supplier.setGstNo(txtGstNo.getText().trim());
            supplier.setPanNo(txtPanNo.getText().trim());
            supplier.setIsActive(true);

            if (selectedSupplier == null) {
                // Create new supplier
                supplierService.createSupplier(supplier);
                alertNotification.showSuccess("Supplier created successfully!");
            } else {
                // Update existing supplier
                supplier.setId(selectedSupplier.getId());
                supplierService.updateSupplier(selectedSupplier.getId(), supplier);
                alertNotification.showSuccess("Supplier updated successfully!");
                selectedSupplier = null;
            }

            clearForm();
            loadSuppliers();

        } catch (Exception e) {
            LOG.error("Error saving supplier: ", e);
            alertNotification.showError("Error saving supplier: " + e.getMessage());
        }
    }

    private void editSupplier(SupplierData supplier) {
        selectedSupplier = supplier;
        txtName.setText(supplier.getName());
        txtContact.setText(supplier.getContact());
        txtEmail.setText(supplier.getEmail());
        txtAddress.setText(supplier.getAddress());
        txtCity.setText(supplier.getCity());
        txtState.setText(supplier.getState());
        txtPincode.setText(supplier.getPincode());
        txtGstNo.setText(supplier.getGstNo());
        txtPanNo.setText(supplier.getPanNo());

        // Show Update button, hide Save button
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void clearForm() {
        txtName.clear();
        txtContact.clear();
        txtEmail.clear();
        txtAddress.clear();
        txtCity.clear();
        txtState.clear();
        txtPincode.clear();
        txtGstNo.clear();
        txtPanNo.clear();
        selectedSupplier = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter supplier name");
            txtName.requestFocus();
            return false;
        }

        // Validate contact number format if provided
        if (!txtContact.getText().trim().isEmpty()) {
            if (!txtContact.getText().trim().matches("\\d{10}")) {
                alertNotification.showError("Contact number must be 10 digits");
                txtContact.requestFocus();
                return false;
            }
        }

        // Validate email format if provided
        if (!txtEmail.getText().trim().isEmpty()) {
            if (!txtEmail.getText().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                alertNotification.showError("Please enter valid email address");
                txtEmail.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void loadSuppliers() {
        try {
            List<Supplier> suppliers = supplierService.getAllSuppliers();
            supplierData.clear();

            for (Supplier supplier : suppliers) {
                supplierData.add(new SupplierData(
                        supplier.getId(),
                        supplier.getName(),
                        supplier.getContact(),
                        supplier.getEmail(),
                        supplier.getAddress(),
                        supplier.getCity(),
                        supplier.getState(),
                        supplier.getPincode(),
                        supplier.getGstNo(),
                        supplier.getPanNo()
                ));
            }

            // Refresh the table view
            tblSuppliers.refresh();

            // Update autocomplete suggestions
            updateAutoCompleteSuggestions();

            LOG.info("Loaded {} suppliers", suppliers.size());

        } catch (Exception e) {
            LOG.error("Error loading suppliers: ", e);
            alertNotification.showError("Error loading suppliers: " + e.getMessage());
        }
    }

    // Inner class for table data
    public static class SupplierData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty contact;
        private final SimpleStringProperty email;
        private final SimpleStringProperty address;
        private final SimpleStringProperty city;
        private final SimpleStringProperty state;
        private final SimpleStringProperty pincode;
        private final SimpleStringProperty gstNo;
        private final SimpleStringProperty panNo;

        public SupplierData(Integer id, String name, String contact, String email,
                           String address, String city, String state, String pincode,
                           String gstNo, String panNo) {
            this.id = new SimpleIntegerProperty(id != null ? id : 0);
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.contact = new SimpleStringProperty(contact != null ? contact : "");
            this.email = new SimpleStringProperty(email != null ? email : "");
            this.address = new SimpleStringProperty(address != null ? address : "");
            this.city = new SimpleStringProperty(city != null ? city : "");
            this.state = new SimpleStringProperty(state != null ? state : "");
            this.pincode = new SimpleStringProperty(pincode != null ? pincode : "");
            this.gstNo = new SimpleStringProperty(gstNo != null ? gstNo : "");
            this.panNo = new SimpleStringProperty(panNo != null ? panNo : "");
        }

        public Integer getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getContact() {
            return contact.get();
        }

        public SimpleStringProperty contactProperty() {
            return contact;
        }

        public String getEmail() {
            return email.get();
        }

        public SimpleStringProperty emailProperty() {
            return email;
        }

        public String getAddress() {
            return address.get();
        }

        public SimpleStringProperty addressProperty() {
            return address;
        }

        public String getCity() {
            return city.get();
        }

        public SimpleStringProperty cityProperty() {
            return city;
        }

        public String getState() {
            return state.get();
        }

        public SimpleStringProperty stateProperty() {
            return state;
        }

        public String getPincode() {
            return pincode.get();
        }

        public SimpleStringProperty pincodeProperty() {
            return pincode;
        }

        public String getGstNo() {
            return gstNo.get();
        }

        public SimpleStringProperty gstNoProperty() {
            return gstNo;
        }

        public String getPanNo() {
            return panNo.get();
        }

        public SimpleStringProperty panNoProperty() {
            return panNo;
        }
    }
}
