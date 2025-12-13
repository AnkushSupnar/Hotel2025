package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.service.CategoryApiService;
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
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
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
public class AddCategoryController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddCategoryController.class);

    @Autowired
    private CategoryApiService categoryApiService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtCategoryName;

    @FXML
    private RadioButton rbYes;

    @FXML
    private RadioButton rbNo;

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
    private ComboBox<String> cmbFilterStock;

    @FXML
    private TableView<CategoryTableData> tblCategories;

    @FXML
    private TableColumn<CategoryTableData, Integer> colId;

    @FXML
    private TableColumn<CategoryTableData, String> colCategory;

    @FXML
    private TableColumn<CategoryTableData, String> colStock;

    private ObservableList<CategoryTableData> categoryData = FXCollections.observableArrayList();
    private FilteredList<CategoryTableData> filteredData;
    private CategoryTableData selectedCategory = null;
    private ToggleGroup stockToggleGroup;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadCategories();
    }

    private void setupUI() {
        // Setup RadioButton ToggleGroup for stock status
        stockToggleGroup = new ToggleGroup();
        rbYes.setToggleGroup(stockToggleGroup);
        rbNo.setToggleGroup(stockToggleGroup);
        rbYes.setSelected(true); // Default to YES

        // Setup filter ComboBox
        cmbFilterStock.setItems(FXCollections.observableArrayList("All Status", "Y", "N"));
        cmbFilterStock.setValue("All Status");

        // Setup filtered list
        filteredData = new FilteredList<>(categoryData, p -> true);

        // Setup table with filtered data
        tblCategories.setItems(filteredData);
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Apply custom font to Category column (if available)
        applyCategoryColumnFont();

        // Add row selection listener to open category in edit mode
        tblCategories.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click to select
                CategoryTableData selectedItem = tblCategories.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    editCategory(selectedItem);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveCategory());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadCategories());
        btnUpdate.setOnAction(e -> saveCategory());
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
            // Find the main BorderPane in the parent hierarchy
            return (BorderPane) txtCategoryName.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        // Setup search by category name
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Setup filter by stock status
        cmbFilterStock.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(category -> {
            String searchText = txtSearch.getText();
            String stockFilter = cmbFilterStock.getValue();

            // Filter by search text (category name)
            boolean matchesSearch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                matchesSearch = category.getCategory().toLowerCase()
                        .contains(searchText.toLowerCase());
            }

            // Filter by stock status
            boolean matchesStock = true;
            if (stockFilter != null && !"All Status".equals(stockFilter)) {
                matchesStock = category.getStock().equals(stockFilter);
            }

            return matchesSearch && matchesStock;
        });
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
                    javafx.scene.control.TableCell<CategoryTableData, String> cell = new javafx.scene.control.TableCell<CategoryTableData, String>() {
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

                LOG.info("Custom font '{}' applied to Category Name column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to table: ", e);
        }
    }

    /**
     * Apply custom font to input fields from session settings
     * Font is loaded once in SessionService, just retrieve and apply here
     */
    private void applyCustomFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                // Apply font to input fields with appropriate size
                Font inputFont = Font.font(customFont.getFamily(), 25);
                Font searchFont = Font.font(customFont.getFamily(), 25);

                // Apply to category name field
                applyFontToTextField(txtCategoryName, inputFont, 25);

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
                        "-fx-font-size: " + fontSize + "px;");

        // Add focus listener to ensure font persists through focus changes
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            // Reapply font on focus change to prevent CSS override
            textField.setFont(font);
        });
    }

    private void saveCategory() {
        if (!validateInput()) {
            return;
        }

        try {
            CategoryMasterDto categoryDto = new CategoryMasterDto();
            categoryDto.setCategory(txtCategoryName.getText().trim());
            categoryDto.setStock(getSelectedStockValue());

            if (selectedCategory == null) {
                // Create new category
                categoryApiService.createCategory(categoryDto);
                alertNotification.showSuccess("Category created successfully!");
            } else {
                // Update existing category
                categoryDto.setId(selectedCategory.getId());
                categoryApiService.updateCategory(selectedCategory.getId(), categoryDto);
                alertNotification.showSuccess("Category updated successfully!");
                selectedCategory = null;
            }

            clearForm();
            loadCategories();

        } catch (Exception e) {
            LOG.error("Error saving category: ", e);
            alertNotification.showError("Error saving category: " + e.getMessage());
        }
    }

    private String getSelectedStockValue() {
        return rbYes.isSelected() ? "Y" : "N";
    }

    private void editCategory(CategoryTableData category) {
        selectedCategory = category;
        txtCategoryName.setText(category.getCategory());

        // Set radio button based on stock value
        if ("Y".equals(category.getStock()) || "YES".equalsIgnoreCase(category.getStock())) {
            rbYes.setSelected(true);
        } else {
            rbNo.setSelected(true);
        }

        // Show Update button, hide Save button
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void deleteCategory(CategoryTableData category) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Category");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("Do you want to delete the category: " + category.getCategory() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    categoryApiService.deleteCategory(category.getId());
                    alertNotification.showSuccess("Category deleted successfully!");
                    loadCategories();
                } catch (Exception e) {
                    LOG.error("Error deleting category: ", e);
                    alertNotification.showError("Error deleting category: " + e.getMessage());
                }
            }
        });
    }

    private void clearForm() {
        txtCategoryName.clear();
        rbYes.setSelected(true); // Default to YES
        selectedCategory = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtCategoryName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter category name");
            txtCategoryName.requestFocus();
            return false;
        }

        if (txtCategoryName.getText().trim().length() > 45) {
            alertNotification.showError("Category name cannot exceed 45 characters");
            txtCategoryName.requestFocus();
            return false;
        }

        // No need to validate radio buttons as one is always selected
        return true;
    }

    private void loadCategories() {
        try {
            List<CategoryMasterDto> categories = categoryApiService.getAllCategories();
            categoryData.clear();

            for (CategoryMasterDto dto : categories) {
                categoryData.add(new CategoryTableData(dto.getId(), dto.getCategory(), dto.getStock()));
            }

            // Refresh the table view to ensure it displays updated data
            tblCategories.refresh();

            LOG.info("Loaded {} categories", categories.size());

        } catch (Exception e) {
            LOG.error("Error loading categories: ", e);
            String errorMessage = e.getMessage();

            if (errorMessage.contains("not logged in") || errorMessage.contains("Authentication failed")) {
                alertNotification.showError("Authentication required. Please login to access categories.");
                // Optionally redirect to login page
            } else {
                alertNotification.showError("Error loading categories: " + errorMessage);
            }
        }
    }

    // Inner class for table data
    public static class CategoryTableData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty category;
        private final SimpleStringProperty stock;

        public CategoryTableData(Integer id, String category, String stock) {
            this.id = new SimpleIntegerProperty(id);
            this.category = new SimpleStringProperty(category);
            this.stock = new SimpleStringProperty(stock);
        }

        public Integer getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getCategory() {
            return category.get();
        }

        public SimpleStringProperty categoryProperty() {
            return category;
        }

        public String getStock() {
            return stock.get();
        }

        public SimpleStringProperty stockProperty() {
            return stock;
        }
    }
}