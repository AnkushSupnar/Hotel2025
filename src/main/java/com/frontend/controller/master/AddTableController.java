package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.TableMaster;
import com.frontend.service.TableMasterService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class AddTableController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddTableController.class);

    @Autowired
    private TableMasterService tableMasterService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtTableName;

    @FXML
    private TextField txtDescription;

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
    private TableView<TableData> tblTables;

    @FXML
    private TableColumn<TableData, Integer> colId;

    @FXML
    private TableColumn<TableData, String> colTableName;

    @FXML
    private TableColumn<TableData, String> colDescription;

    private ObservableList<TableData> tableData = FXCollections.observableArrayList();
    private FilteredList<TableData> filteredData;
    private TableData selectedTable = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        loadTables();
    }

    private void setupUI() {
        // Setup filtered list
        filteredData = new FilteredList<>(tableData, p -> true);

        // Setup table with filtered data
        tblTables.setItems(filteredData);
    }

    private void setupTableColumns() {
        // Setup table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colTableName.setCellValueFactory(cellData -> cellData.getValue().tableNameProperty());
        colDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());

        // Add row selection listener to open table in edit mode
        tblTables.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click to select
                TableData selectedTableData = tblTables.getSelectionModel().getSelectedItem();
                if (selectedTableData != null) {
                    editTable(selectedTableData);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveTable());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadTables());
        btnUpdate.setOnAction(e -> saveTable());
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
            return (BorderPane) txtTableName.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        // Setup search by table name
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(table -> {
            String searchText = txtSearch.getText();

            // Filter by search text (table name or description)
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                return table.getTableName().toLowerCase().contains(lowerCaseFilter) ||
                       table.getDescription().toLowerCase().contains(lowerCaseFilter);
            }

            return true;
        });
    }


    private void saveTable() {
        if (!validateInput()) {
            return;
        }

        try {
            TableMaster tableMaster = new TableMaster();
            tableMaster.setTableName(txtTableName.getText().trim());
            tableMaster.setDescription(txtDescription.getText().trim());

            if (selectedTable == null) {
                // Create new table
                tableMasterService.createTable(tableMaster);
                alertNotification.showSuccess("Table created successfully!");
            } else {
                // Update existing table
                tableMaster.setId(selectedTable.getId());
                tableMasterService.updateTable(selectedTable.getId(), tableMaster);
                alertNotification.showSuccess("Table updated successfully!");
                selectedTable = null;
            }

            clearForm();
            loadTables();

        } catch (Exception e) {
            LOG.error("Error saving table: ", e);
            alertNotification.showError("Error saving table: " + e.getMessage());
        }
    }

    private void editTable(TableData table) {
        selectedTable = table;
        txtTableName.setText(table.getTableName());
        txtDescription.setText(table.getDescription());

        // Show Update button, hide Save button
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnUpdate.setVisible(true);
        btnUpdate.setManaged(true);
    }

    private void clearForm() {
        txtTableName.clear();
        txtDescription.clear();
        selectedTable = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtTableName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter table name");
            txtTableName.requestFocus();
            return false;
        }

        if (txtDescription.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter description");
            txtDescription.requestFocus();
            return false;
        }

        return true;
    }

    private void loadTables() {
        try {
            List<TableMaster> tables = tableMasterService.getAllTables();
            tableData.clear();

            for (TableMaster table : tables) {
                tableData.add(new TableData(
                        table.getId(),
                        table.getTableName(),
                        table.getDescription()
                ));
            }

            // Refresh the table view
            tblTables.refresh();

            LOG.info("Loaded {} tables", tables.size());

        } catch (Exception e) {
            LOG.error("Error loading tables: ", e);
            alertNotification.showError("Error loading tables: " + e.getMessage());
        }
    }

    // Inner class for table data
    public static class TableData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty tableName;
        private final SimpleStringProperty description;

        public TableData(Integer id, String tableName, String description) {
            this.id = new SimpleIntegerProperty(id);
            this.tableName = new SimpleStringProperty(tableName);
            this.description = new SimpleStringProperty(description);
        }

        public Integer getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getTableName() {
            return tableName.get();
        }

        public SimpleStringProperty tableNameProperty() {
            return tableName;
        }

        public String getDescription() {
            return description.get();
        }

        public SimpleStringProperty descriptionProperty() {
            return description;
        }
    }
}
