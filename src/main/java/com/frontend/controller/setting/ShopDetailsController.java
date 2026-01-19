package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.Shop;
import com.frontend.service.ShopService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.SimpleLongProperty;
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
public class ShopDetailsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ShopDetailsController.class);

    @Autowired
    private ShopService shopService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtRestaurantName;

    @FXML
    private TextField txtSubTitle;

    @FXML
    private TextField txtOwnerName;

    @FXML
    private TextArea txtAddress;

    @FXML
    private TextField txtContactNumber;

    @FXML
    private TextField txtContactNumber2;

    @FXML
    private TextField txtGstinNumber;

    @FXML
    private TextField txtLicenseKey;

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
    private TableView<ShopData> tblShops;

    @FXML
    private TableColumn<ShopData, Long> colId;

    @FXML
    private TableColumn<ShopData, String> colRestaurantName;

    @FXML
    private TableColumn<ShopData, String> colOwnerName;

    @FXML
    private TableColumn<ShopData, String> colContactNumber;

    @FXML
    private TableColumn<ShopData, String> colAddress;

    @FXML
    private TableColumn<ShopData, String> colGstinNumber;

    private ObservableList<ShopData> shopData = FXCollections.observableArrayList();
    private FilteredList<ShopData> filteredData;
    private ShopData selectedShop = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadShops();
    }

    private void setupUI() {
        filteredData = new FilteredList<>(shopData, p -> true);
        tblShops.setItems(filteredData);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().shopIdProperty().asObject());
        colRestaurantName.setCellValueFactory(cellData -> cellData.getValue().restaurantNameProperty());
        colOwnerName.setCellValueFactory(cellData -> cellData.getValue().ownerNameProperty());
        colContactNumber.setCellValueFactory(cellData -> cellData.getValue().contactNumberProperty());
        colAddress.setCellValueFactory(cellData -> cellData.getValue().addressProperty());
        colGstinNumber.setCellValueFactory(cellData -> cellData.getValue().gstinNumberProperty());

        applyRestaurantNameColumnFont();
        applyOwnerNameColumnFont();
        applyAddressColumnFont();

        tblShops.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                ShopData selectedShopData = tblShops.getSelectionModel().getSelectedItem();
                if (selectedShopData != null) {
                    editShop(selectedShopData);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveShop());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadShops());
        btnUpdate.setOnAction(e -> saveShop());
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to Settings Menu");
                navigateToSettingsMenu();
            } catch (Exception ex) {
                LOG.error("Error returning to Settings Menu: ", ex);
            }
        });
    }

    private void navigateToSettingsMenu() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/setting/SettingMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Successfully navigated to Settings Menu");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to Settings Menu: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) txtRestaurantName.getScene().lookup("#mainPane");
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
        filteredData.setPredicate(shop -> {
            String searchText = txtSearch.getText();

            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                return shop.getRestaurantName().toLowerCase().contains(lowerCaseFilter) ||
                       shop.getOwnerName().toLowerCase().contains(lowerCaseFilter) ||
                       shop.getContactNumber().toLowerCase().contains(lowerCaseFilter) ||
                       (shop.getAddress() != null && shop.getAddress().toLowerCase().contains(lowerCaseFilter));
            }

            return true;
        });
    }

    private void applyRestaurantNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colRestaurantName.setCellFactory(column -> {
                    TableCell<ShopData, String> cell = new TableCell<ShopData, String>() {
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

                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to Restaurant Name column cells", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Restaurant Name column: ", e);
        }
    }

    private void applyOwnerNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colOwnerName.setCellFactory(column -> {
                    TableCell<ShopData, String> cell = new TableCell<ShopData, String>() {
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

                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to Owner Name column cells", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Owner Name column: ", e);
        }
    }

    private void applyAddressColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                colAddress.setCellFactory(column -> {
                    TableCell<ShopData, String> cell = new TableCell<ShopData, String>() {
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

                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px;");

                    return cell;
                });

                LOG.info("Custom font '{}' applied to Address column cells", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Address column: ", e);
        }
    }

    private void applyCustomFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                Font inputFont = Font.font(customFont.getFamily(), 18);

                applyFontToTextField(txtRestaurantName, inputFont, 18);
                applyFontToTextField(txtSubTitle, inputFont, 18);
                applyFontToTextField(txtOwnerName, inputFont, 18);
                applyFontToTextArea(txtAddress, inputFont, 18);

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
                        "-fx-font-size: " + fontSize + "px;"
        );

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            textField.setFont(font);
        });
    }

    private void applyFontToTextArea(TextArea textArea, Font font, int fontSize) {
        if (textArea == null || font == null) {
            return;
        }

        textArea.setFont(font);
        textArea.setStyle(
                "-fx-font-family: '" + font.getFamily() + "';" +
                        "-fx-font-size: " + fontSize + "px;"
        );

        textArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            textArea.setFont(font);
        });
    }

    private void saveShop() {
        if (!validateInput()) {
            return;
        }

        try {
            Shop shop = new Shop();
            shop.setRestaurantName(txtRestaurantName.getText().trim());
            shop.setSubTitle(txtSubTitle.getText().trim());
            shop.setOwnerName(txtOwnerName.getText().trim());
            shop.setAddress(txtAddress.getText().trim());
            shop.setContactNumber(txtContactNumber.getText().trim());
            shop.setContactNumber2(txtContactNumber2.getText().trim());
            shop.setGstinNumber(txtGstinNumber.getText().trim());
            shop.setLicenseKey(txtLicenseKey.getText().trim());

            if (selectedShop == null) {
                shopService.createShop(shop);
                alertNotification.showSuccess("Restaurant created successfully!");
            } else {
                shop.setShopId(selectedShop.getShopId());
                shopService.updateShop(selectedShop.getShopId(), shop);
                alertNotification.showSuccess("Restaurant updated successfully!");
                selectedShop = null;
            }

            clearForm();
            loadShops();

        } catch (IllegalArgumentException e) {
            LOG.error("Validation error: ", e);
            alertNotification.showError(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error saving restaurant: ", e);
            alertNotification.showError("Error saving restaurant: " + e.getMessage());
        }
    }

    private void editShop(ShopData shop) {
        selectedShop = shop;
        txtRestaurantName.setText(shop.getRestaurantName());
        txtSubTitle.setText(shop.getSubTitle());
        txtOwnerName.setText(shop.getOwnerName());
        txtAddress.setText(shop.getAddress());
        txtContactNumber.setText(shop.getContactNumber());
        txtContactNumber2.setText(shop.getContactNumber2());
        txtGstinNumber.setText(shop.getGstinNumber());
        txtLicenseKey.setText(shop.getLicenseKey());

        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void clearForm() {
        txtRestaurantName.clear();
        txtSubTitle.clear();
        txtOwnerName.clear();
        txtAddress.clear();
        txtContactNumber.clear();
        txtContactNumber2.clear();
        txtGstinNumber.clear();
        txtLicenseKey.clear();
        selectedShop = null;

        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtRestaurantName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter restaurant name");
            txtRestaurantName.requestFocus();
            return false;
        }

        if (txtOwnerName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter owner name");
            txtOwnerName.requestFocus();
            return false;
        }

        if (txtAddress.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter address");
            txtAddress.requestFocus();
            return false;
        }

        if (txtContactNumber.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter contact number");
            txtContactNumber.requestFocus();
            return false;
        }

        // Validate contact number format (10-15 digits)
        String contactNumber = txtContactNumber.getText().trim();
        if (!contactNumber.matches("\\d{10,15}")) {
            alertNotification.showError("Contact number must be 10-15 digits");
            txtContactNumber.requestFocus();
            return false;
        }

        // Validate alternate contact number if provided
        String contactNumber2 = txtContactNumber2.getText().trim();
        if (!contactNumber2.isEmpty() && !contactNumber2.matches("\\d{10,15}")) {
            alertNotification.showError("Alternate contact number must be 10-15 digits");
            txtContactNumber2.requestFocus();
            return false;
        }

        // Validate GSTIN format if provided (15 alphanumeric characters)
        String gstin = txtGstinNumber.getText().trim();
        if (!gstin.isEmpty() && !gstin.matches("[0-9A-Z]{15}")) {
            alertNotification.showError("GSTIN must be 15 alphanumeric characters");
            txtGstinNumber.requestFocus();
            return false;
        }

        return true;
    }

    private void loadShops() {
        try {
            List<Shop> shops = shopService.getAllShops();
            shopData.clear();

            for (Shop shop : shops) {
                shopData.add(new ShopData(
                        shop.getShopId(),
                        shop.getRestaurantName(),
                        shop.getSubTitle(),
                        shop.getOwnerName(),
                        shop.getAddress(),
                        shop.getContactNumber(),
                        shop.getContactNumber2(),
                        shop.getGstinNumber(),
                        shop.getLicenseKey()
                ));
            }

            tblShops.refresh();

            LOG.info("Loaded {} restaurants", shops.size());

        } catch (Exception e) {
            LOG.error("Error loading restaurants: ", e);
            alertNotification.showError("Error loading restaurants: " + e.getMessage());
        }
    }

    // Inner class for table data
    public static class ShopData {
        private final SimpleLongProperty shopId;
        private final SimpleStringProperty restaurantName;
        private final SimpleStringProperty subTitle;
        private final SimpleStringProperty ownerName;
        private final SimpleStringProperty address;
        private final SimpleStringProperty contactNumber;
        private final SimpleStringProperty contactNumber2;
        private final SimpleStringProperty gstinNumber;
        private final SimpleStringProperty licenseKey;

        public ShopData(Long shopId, String restaurantName, String subTitle, String ownerName,
                       String address, String contactNumber, String contactNumber2,
                       String gstinNumber, String licenseKey) {
            this.shopId = new SimpleLongProperty(shopId != null ? shopId : 0L);
            this.restaurantName = new SimpleStringProperty(restaurantName != null ? restaurantName : "");
            this.subTitle = new SimpleStringProperty(subTitle != null ? subTitle : "");
            this.ownerName = new SimpleStringProperty(ownerName != null ? ownerName : "");
            this.address = new SimpleStringProperty(address != null ? address : "");
            this.contactNumber = new SimpleStringProperty(contactNumber != null ? contactNumber : "");
            this.contactNumber2 = new SimpleStringProperty(contactNumber2 != null ? contactNumber2 : "");
            this.gstinNumber = new SimpleStringProperty(gstinNumber != null ? gstinNumber : "");
            this.licenseKey = new SimpleStringProperty(licenseKey != null ? licenseKey : "");
        }

        public Long getShopId() {
            return shopId.get();
        }

        public SimpleLongProperty shopIdProperty() {
            return shopId;
        }

        public String getRestaurantName() {
            return restaurantName.get();
        }

        public SimpleStringProperty restaurantNameProperty() {
            return restaurantName;
        }

        public String getSubTitle() {
            return subTitle.get();
        }

        public SimpleStringProperty subTitleProperty() {
            return subTitle;
        }

        public String getOwnerName() {
            return ownerName.get();
        }

        public SimpleStringProperty ownerNameProperty() {
            return ownerName;
        }

        public String getAddress() {
            return address.get();
        }

        public SimpleStringProperty addressProperty() {
            return address;
        }

        public String getContactNumber() {
            return contactNumber.get();
        }

        public SimpleStringProperty contactNumberProperty() {
            return contactNumber;
        }

        public String getContactNumber2() {
            return contactNumber2.get();
        }

        public SimpleStringProperty contactNumber2Property() {
            return contactNumber2;
        }

        public String getGstinNumber() {
            return gstinNumber.get();
        }

        public SimpleStringProperty gstinNumberProperty() {
            return gstinNumber;
        }

        public String getLicenseKey() {
            return licenseKey.get();
        }

        public SimpleStringProperty licenseKeyProperty() {
            return licenseKey;
        }
    }
}
