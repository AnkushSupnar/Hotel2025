package com.frontend.controller;

import com.frontend.dto.CategoryMasterDto;
import com.frontend.service.CategoryApiService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    
    @FXML
    private TextField txtCategoryName;
    
    @FXML
    private ComboBox<String> cmbStock;
    
    @FXML
    private Button btnSave;
    
    @FXML
    private Button btnClear;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private TableView<CategoryTableData> tblCategories;
    
    @FXML
    private TableColumn<CategoryTableData, Integer> colId;
    
    @FXML
    private TableColumn<CategoryTableData, String> colCategory;
    
    @FXML
    private TableColumn<CategoryTableData, String> colStock;
    
    @FXML
    private TableColumn<CategoryTableData, String> colActions;
    
    private ObservableList<CategoryTableData> categoryData = FXCollections.observableArrayList();
    private CategoryTableData selectedCategory = null;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        loadCategories();
    }
    
    private void setupUI() {
        // Setup ComboBox items
        cmbStock.setItems(FXCollections.observableArrayList("YES", "NO"));
        cmbStock.setValue("YES");
        
        // Setup table
        tblCategories.setItems(categoryData);
    }
    
    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        
        // Setup actions column with Edit/Delete buttons
        colActions.setCellFactory(col -> new TableCell<CategoryTableData, String>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            
            {
                editBtn.getStyleClass().add("table-edit-button");
                deleteBtn.getStyleClass().add("table-delete-button");
                
                editBtn.setOnAction(e -> {
                    CategoryTableData category = getTableView().getItems().get(getIndex());
                    editCategory(category);
                });
                
                deleteBtn.setOnAction(e -> {
                    CategoryTableData category = getTableView().getItems().get(getIndex());
                    deleteCategory(category);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new javafx.scene.layout.HBox(5, editBtn, deleteBtn));
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveCategory());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadCategories());
    }
    
    private void saveCategory() {
        if (!validateInput()) {
            return;
        }
        
        try {
            CategoryMasterDto categoryDto = new CategoryMasterDto();
            categoryDto.setCategory(txtCategoryName.getText().trim());
            categoryDto.setStock(cmbStock.getValue());
            
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
    
    private void editCategory(CategoryTableData category) {
        selectedCategory = category;
        txtCategoryName.setText(category.getCategory());
        cmbStock.setValue(category.getStock());
        btnSave.setText("Update Category");
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
        cmbStock.setValue("YES");
        selectedCategory = null;
        btnSave.setText("Save Category");
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
        
        if (cmbStock.getValue() == null) {
            alertNotification.showError("Please select stock status");
            cmbStock.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void loadCategories() {
        try {
            List<CategoryMasterDto> categories = categoryApiService.getAllCategories();
            categoryData.clear();
            
            for (CategoryMasterDto dto : categories) {
                categoryData.add(new CategoryTableData(dto.getId(), dto.getCategory(), dto.getStock()));
            }
            
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