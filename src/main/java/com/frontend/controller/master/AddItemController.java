package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.dto.ItemDto;
import com.frontend.service.CategoryApiService;
import com.frontend.service.ItemService;
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
public class AddItemController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddItemController.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private CategoryApiService categoryApiService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtItemName;

    @FXML
    private ComboBox<CategoryComboItem> cmbCategory;

    @FXML
    private TextField txtItemCode;

    @FXML
    private TextField txtRate;

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
    private ComboBox<CategoryComboItem> cmbFilterCategory;

    @FXML
    private TableView<ItemTableData> tblItems;

    @FXML
    private TableColumn<ItemTableData, Integer> colId;

    @FXML
    private TableColumn<ItemTableData, String> colItemName;

    @FXML
    private TableColumn<ItemTableData, String> colCategory;

    @FXML
    private TableColumn<ItemTableData, Integer> colItemCode;

    @FXML
    private TableColumn<ItemTableData, Float> colRate;

    private ObservableList<ItemTableData> itemData = FXCollections.observableArrayList();
    private FilteredList<ItemTableData> filteredData;
    private ItemTableData selectedItem = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadCategories();
        loadItems();
    }

    private void setupUI() {
        // Setup filtered list
        filteredData = new FilteredList<>(itemData, p -> true);

        // Setup table with filtered data
        tblItems.setItems(filteredData);
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colItemName.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());
        colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryNameProperty());
        colItemCode.setCellValueFactory(cellData -> cellData.getValue().itemCodeProperty().asObject());
        colRate.setCellValueFactory(cellData -> cellData.getValue().rateProperty().asObject());

        // Setup responsive column widths - bind to table width
        setupResponsiveColumnWidths();

        // Apply custom font to Item Name and Category columns
        applyItemNameColumnFont();
        applyCategoryColumnFont();

        // Add row selection listener to open item in edit mode
        tblItems.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click to select
                ItemTableData selectedTableItem = tblItems.getSelectionModel().getSelectedItem();
                if (selectedTableItem != null) {
                    editItem(selectedTableItem);
                }
            }
        });
    }

    /**
     * Setup responsive column widths that adjust based on table width
     */
    private void setupResponsiveColumnWidths() {
        // Bind column widths to percentage of table width
        // ID: 8%, Item Name: 32%, Category: 25%, Item Code: 15%, Rate: 20%
        colId.prefWidthProperty().bind(tblItems.widthProperty().multiply(0.08));
        colItemName.prefWidthProperty().bind(tblItems.widthProperty().multiply(0.32));
        colCategory.prefWidthProperty().bind(tblItems.widthProperty().multiply(0.25));
        colItemCode.prefWidthProperty().bind(tblItems.widthProperty().multiply(0.15));
        colRate.prefWidthProperty().bind(tblItems.widthProperty().multiply(0.18));

        // Disable manual column resizing to maintain proportions
        colId.setResizable(false);
        colItemName.setResizable(false);
        colCategory.setResizable(false);
        colItemCode.setResizable(false);
        colRate.setResizable(false);
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveItem());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadItems());
        btnUpdate.setOnAction(e -> saveItem());
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
            return (BorderPane) txtItemName.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        // Setup search by item name
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Setup filter by category
        cmbFilterCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(item -> {
            String searchText = txtSearch.getText();
            CategoryComboItem categoryFilter = cmbFilterCategory.getValue();

            // Filter by search text (item name)
            boolean matchesSearch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                matchesSearch = item.getItemName().toLowerCase()
                        .contains(searchText.toLowerCase());
            }

            // Filter by category
            boolean matchesCategory = true;
            if (categoryFilter != null && categoryFilter.getId() != null) {
                matchesCategory = item.getCategoryId().equals(categoryFilter.getId());
            }

            return matchesSearch && matchesCategory;
        });
    }

    /**
     * Apply custom font to Item Name column cells only (not header)
     */
    private void applyItemNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colItemName.setCellFactory(column -> {
                    TableCell<ItemTableData, String> cell = new TableCell<ItemTableData, String>() {
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

                LOG.info("Custom font '{}' applied to Item Name column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to table: ", e);
        }
    }

    /**
     * Apply custom font to Category column cells only (not header)
     */
    private void applyCategoryColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colCategory.setCellFactory(column -> {
                    TableCell<ItemTableData, String> cell = new TableCell<ItemTableData, String>() {
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

                LOG.info("Custom font '{}' applied to Category column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Category column: ", e);
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
                Font searchFont = Font.font(customFont.getFamily(), 25);

                // Apply to item name field
                applyFontToTextField(txtItemName, inputFont, 25);

                // Apply to search field
                applyFontToTextField(txtSearch, searchFont, 25);

                LOG.info("Custom font '{}' applied to input fields", customFont.getFamily());
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

    private void loadCategories() {
        try {
            List<CategoryMasterDto> categories = categoryApiService.getAllCategories();

            // Populate form category dropdown
            ObservableList<CategoryComboItem> categoryItems = FXCollections.observableArrayList();
            for (CategoryMasterDto category : categories) {
                categoryItems.add(new CategoryComboItem(category.getId(), category.getCategory()));
            }
            cmbCategory.setItems(categoryItems);

            // Populate filter category dropdown (with "All Categories" option)
            ObservableList<CategoryComboItem> filterItems = FXCollections.observableArrayList();
            filterItems.add(new CategoryComboItem(null, "sava- k^TogaIrI"));
            filterItems.addAll(categoryItems);
            cmbFilterCategory.setItems(filterItems);
            cmbFilterCategory.setValue(filterItems.get(0));

            // Apply custom font to ComboBox dropdowns
            applyCustomFontToComboBox(cmbCategory);
            applyCustomFontToComboBox(cmbFilterCategory);

            LOG.info("Loaded {} categories", categories.size());

        } catch (Exception e) {
            LOG.error("Error loading categories: ", e);
            alertNotification.showError("Error loading categories: " + e.getMessage());
        }
    }

    /**
     * Apply custom font to ComboBox items
     */
    private void applyCustomFontToComboBox(ComboBox<CategoryComboItem> comboBox) {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();
                int fontSize = 25;

                // Set cell factory for dropdown items
                comboBox.setCellFactory(listView -> new ListCell<CategoryComboItem>() {
                    @Override
                    protected void updateItem(CategoryComboItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.getName());
                            setGraphic(null);
                            // Apply custom font to dropdown items
                            setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px;");
                        }
                    }
                });

                // Set button cell (selected item display)
                comboBox.setButtonCell(new ListCell<CategoryComboItem>() {
                    @Override
                    protected void updateItem(CategoryComboItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.getName());
                            setGraphic(null);
                            // Apply custom font to selected item display
                            setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px;");
                        }
                    }
                });

                LOG.debug("Custom font '{}' applied to ComboBox", fontFamily);
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to ComboBox: ", e);
        }
    }

    private void saveItem() {
        if (!validateInput()) {
            return;
        }

        try {
            ItemDto itemDto = new ItemDto();
            itemDto.setItemName(txtItemName.getText().trim());
            itemDto.setCategoryId(cmbCategory.getValue().getId());
            itemDto.setItemCode(Integer.parseInt(txtItemCode.getText().trim()));
            itemDto.setRate(Float.parseFloat(txtRate.getText().trim()));

            if (selectedItem == null) {
                // Create new item
                itemService.createItem(itemDto);
                alertNotification.showSuccess("Item created successfully!");
            } else {
                // Update existing item
                itemDto.setId(selectedItem.getId());
                itemService.updateItem(selectedItem.getId(), itemDto);
                alertNotification.showSuccess("Item updated successfully!");
                selectedItem = null;
            }

            clearForm();
            loadItems();

        } catch (NumberFormatException e) {
            LOG.error("Invalid number format", e);
            alertNotification.showError("Please enter valid numbers for Item Code and Rate");
        } catch (Exception e) {
            LOG.error("Error saving item: ", e);
            alertNotification.showError("Error saving item: " + e.getMessage());
        }
    }

    private void editItem(ItemTableData item) {
        selectedItem = item;
        txtItemName.setText(item.getItemName());
        txtItemCode.setText(String.valueOf(item.getItemCode()));
        txtRate.setText(String.valueOf(item.getRate()));

        // Set category
        for (CategoryComboItem categoryItem : cmbCategory.getItems()) {
            if (categoryItem.getId().equals(item.getCategoryId())) {
                cmbCategory.setValue(categoryItem);
                break;
            }
        }

        // Show Update button, hide Save button
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void clearForm() {
        txtItemName.clear();
        txtItemCode.clear();
        txtRate.clear();
        cmbCategory.setValue(null);
        selectedItem = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtItemName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter item name");
            txtItemName.requestFocus();
            return false;
        }

        if (cmbCategory.getValue() == null) {
            alertNotification.showError("Please select a category");
            cmbCategory.requestFocus();
            return false;
        }

        if (txtItemCode.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter item code");
            txtItemCode.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(txtItemCode.getText().trim());
        } catch (NumberFormatException e) {
            alertNotification.showError("Item code must be a valid number");
            txtItemCode.requestFocus();
            return false;
        }

        if (txtRate.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter rate");
            txtRate.requestFocus();
            return false;
        }

        try {
            Float rate = Float.parseFloat(txtRate.getText().trim());
            if (rate < 0) {
                alertNotification.showError("Rate must be a positive number");
                txtRate.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            alertNotification.showError("Rate must be a valid number");
            txtRate.requestFocus();
            return false;
        }

        return true;
    }

    private void loadItems() {
        try {
            List<ItemDto> items = itemService.getAllItems();
            itemData.clear();

            for (ItemDto dto : items) {
                itemData.add(new ItemTableData(
                        dto.getId(),
                        dto.getItemName(),
                        dto.getCategoryId(),
                        dto.getCategoryName(),
                        dto.getItemCode(),
                        dto.getRate()
                ));
            }

            // Refresh the table view
            tblItems.refresh();

            LOG.info("Loaded {} items", items.size());

        } catch (Exception e) {
            LOG.error("Error loading items: ", e);
            alertNotification.showError("Error loading items: " + e.getMessage());
        }
    }

    // Inner class for Category ComboBox items
    public static class CategoryComboItem {
        private final Integer id;
        private final String name;

        public CategoryComboItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Inner class for table data
    public static class ItemTableData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty itemName;
        private final SimpleIntegerProperty categoryId;
        private final SimpleStringProperty categoryName;
        private final SimpleIntegerProperty itemCode;
        private final SimpleFloatProperty rate;

        public ItemTableData(Integer id, String itemName, Integer categoryId, String categoryName,
                             Integer itemCode, Float rate) {
            this.id = new SimpleIntegerProperty(id);
            this.itemName = new SimpleStringProperty(itemName);
            this.categoryId = new SimpleIntegerProperty(categoryId);
            this.categoryName = new SimpleStringProperty(categoryName);
            this.itemCode = new SimpleIntegerProperty(itemCode);
            this.rate = new SimpleFloatProperty(rate);
        }

        public Integer getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getItemName() {
            return itemName.get();
        }

        public SimpleStringProperty itemNameProperty() {
            return itemName;
        }

        public Integer getCategoryId() {
            return categoryId.get();
        }

        public SimpleIntegerProperty categoryIdProperty() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName.get();
        }

        public SimpleStringProperty categoryNameProperty() {
            return categoryName;
        }

        public Integer getItemCode() {
            return itemCode.get();
        }

        public SimpleIntegerProperty itemCodeProperty() {
            return itemCode;
        }

        public Float getRate() {
            return rate.get();
        }

        public SimpleFloatProperty rateProperty() {
            return rate;
        }
    }
}
