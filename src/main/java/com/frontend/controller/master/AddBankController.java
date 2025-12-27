package com.frontend.controller.master;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Bank;
import com.frontend.service.BankService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.SimpleDoubleProperty;
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
public class AddBankController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddBankController.class);

    @Autowired
    private BankService bankService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML
    private TextField txtBankName;

    @FXML
    private TextField txtAccountNo;

    @FXML
    private ComboBox<String> cmbAccountType;

    @FXML
    private TextField txtIfsc;

    @FXML
    private TextField txtBranchName;

    @FXML
    private TextField txtPersonName;

    @FXML
    private TextField txtContactNo;

    @FXML
    private TextField txtBankAddress;

    @FXML
    private TextField txtBankBalance;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnUpdate;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnRefresh;

    @FXML
    private Button btnBack;

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cmbFilterStatus;

    @FXML
    private TableView<BankTableData> tblBanks;

    @FXML
    private TableColumn<BankTableData, Integer> colId;

    @FXML
    private TableColumn<BankTableData, String> colBankName;

    @FXML
    private TableColumn<BankTableData, String> colAccountNo;

    @FXML
    private TableColumn<BankTableData, String> colAccountType;

    @FXML
    private TableColumn<BankTableData, String> colBranch;

    @FXML
    private TableColumn<BankTableData, Double> colBalance;

    @FXML
    private TableColumn<BankTableData, String> colStatus;

    private ObservableList<BankTableData> bankData = FXCollections.observableArrayList();
    private FilteredList<BankTableData> filteredData;
    private BankTableData selectedBank = null;
    private AutoCompleteTextField autoCompleteSearch;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupTableColumns();
        setupEventHandlers();
        setupBackButton();
        setupSearchAndFilter();
        applyCustomFont();
        loadBanks();
    }

    private void setupUI() {
        // Setup Account Type ComboBox
        cmbAccountType.setItems(FXCollections.observableArrayList(
                "Savings", "Current", "Fixed Deposit", "Recurring Deposit", "Other"
        ));

        // Setup filter ComboBox
        cmbFilterStatus.setItems(FXCollections.observableArrayList("All Status", "ACTIVE", "INACTIVE"));
        cmbFilterStatus.setValue("All Status");

        // Setup filtered list
        filteredData = new FilteredList<>(bankData, p -> true);

        // Setup table with filtered data
        tblBanks.setItems(filteredData);

        // Set default balance
        txtBankBalance.setText("0.00");
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colBankName.setCellValueFactory(cellData -> cellData.getValue().bankNameProperty());
        colAccountNo.setCellValueFactory(cellData -> cellData.getValue().accountNoProperty());
        colAccountType.setCellValueFactory(cellData -> cellData.getValue().accountTypeProperty());
        colBranch.setCellValueFactory(cellData -> cellData.getValue().branchNameProperty());
        colBalance.setCellValueFactory(cellData -> cellData.getValue().balanceProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Apply custom font to table columns (25px for Bank Name and Branch)
        applyBankNameColumnFont();
        applyBranchColumnFont();

        // Apply 18px font to other columns
        applyAccountNoColumnFont();
        applyAccountTypeColumnFont();
        applyBalanceColumnFont();
        applyStatusColumnFont();

        // Add row selection listener
        tblBanks.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                BankTableData selectedItem = tblBanks.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    editBank(selectedItem);
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveBank());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> loadBanks());
        btnUpdate.setOnAction(e -> saveBank());
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
            return (BorderPane) txtBankName.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupSearchAndFilter() {
        // Setup AutoCompleteTextField for search
        List<String> bankNames = new ArrayList<>();

        // Get custom font from session
        Font customFont = SessionService.getCustomFont(25.0);

        // Create AutoCompleteTextField with custom font
        if (customFont != null) {
            autoCompleteSearch = new AutoCompleteTextField(txtSearch, bankNames, customFont);
            // Apply custom font styling to the text field
            txtSearch.setStyle("-fx-font-family: '" + customFont.getFamily() + "'; -fx-font-size: 25px;");
        } else {
            autoCompleteSearch = new AutoCompleteTextField(txtSearch, bankNames);
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

        // Setup filter by status
        cmbFilterStatus.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    /**
     * Update the autocomplete suggestions with current bank names only
     */
    private void updateAutoCompleteSuggestions() {
        if (autoCompleteSearch != null) {
            List<String> bankNames = bankData.stream()
                    .map(BankTableData::getBankName)
                    .filter(name -> name != null && !name.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            autoCompleteSearch.setSuggestions(bankNames);
            LOG.info("Updated autocomplete suggestions with {} bank names", bankNames.size());
        }
    }

    private void applyFilters() {
        filteredData.setPredicate(bank -> {
            String searchText = txtSearch.getText();
            String statusFilter = cmbFilterStatus.getValue();

            // Search by bank name only
            boolean matchesSearch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                matchesSearch = bank.getBankName().toLowerCase().contains(searchText.toLowerCase());
            }

            boolean matchesStatus = true;
            if (statusFilter != null && !"All Status".equals(statusFilter)) {
                matchesStatus = bank.getStatus().equals(statusFilter);
            }

            return matchesSearch && matchesStatus;
        });
    }

    /**
     * Apply custom font to Bank Name column cells only (not header)
     */
    private void applyBankNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colBankName.setCellFactory(column -> {
                    TableCell<BankTableData, String> cell = new TableCell<BankTableData, String>() {
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

                LOG.info("Custom font '{}' applied to Bank Name column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Bank Name column: ", e);
        }
    }

    /**
     * Apply custom font to Branch column cells only (not header)
     */
    private void applyBranchColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();

            if (customFont != null) {
                String fontFamily = customFont.getFamily();

                // Create a cell factory to apply custom font only to cells (not header)
                colBranch.setCellFactory(column -> {
                    TableCell<BankTableData, String> cell = new TableCell<BankTableData, String>() {
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

                LOG.info("Custom font '{}' applied to Branch column cells only", fontFamily);
            } else {
                LOG.debug("No custom font configured for table");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to Branch column: ", e);
        }
    }

    /**
     * Apply 18px font to Account No column cells
     */
    private void applyAccountNoColumnFont() {
        colAccountNo.setCellFactory(column -> {
            TableCell<BankTableData, String> cell = new TableCell<BankTableData, String>() {
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
            cell.setStyle("-fx-font-size: 18px;");
            return cell;
        });
    }

    /**
     * Apply 18px font to Account Type column cells
     */
    private void applyAccountTypeColumnFont() {
        colAccountType.setCellFactory(column -> {
            TableCell<BankTableData, String> cell = new TableCell<BankTableData, String>() {
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
            cell.setStyle("-fx-font-size: 18px;");
            return cell;
        });
    }

    /**
     * Apply 18px font to Balance column cells
     */
    private void applyBalanceColumnFont() {
        colBalance.setCellFactory(column -> {
            TableCell<BankTableData, Double> cell = new TableCell<BankTableData, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            };
            cell.setStyle("-fx-font-size: 18px;");
            return cell;
        });
    }

    /**
     * Apply 18px font to Status column cells
     */
    private void applyStatusColumnFont() {
        colStatus.setCellFactory(column -> {
            TableCell<BankTableData, String> cell = new TableCell<BankTableData, String>() {
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
            cell.setStyle("-fx-font-size: 18px;");
            return cell;
        });
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

                // Apply to Bank Name field
                applyFontToTextField(txtBankName, inputFont, 25);

                // Apply to Account Holder Name field
                applyFontToTextField(txtPersonName, inputFont, 25);

                // Apply to Bank Address field
                applyFontToTextField(txtBankAddress, inputFont, 25);

                // Apply to Branch Name field
                applyFontToTextField(txtBranchName, inputFont, 25);

                LOG.info("Custom font '{}' applied to Bank Name, Account Holder Name, Bank Address, and Branch Name input fields", customFont.getFamily());
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

    private void saveBank() {
        if (!validateInput()) {
            return;
        }

        try {
            Bank bank;
            if (selectedBank == null) {
                bank = new Bank();
            } else {
                bank = bankService.getBankById(selectedBank.getId()).orElse(new Bank());
            }

            bank.setBankName(txtBankName.getText().trim());
            bank.setAccountNo(txtAccountNo.getText().trim());
            bank.setAccountType(cmbAccountType.getValue());
            bank.setIfsc(txtIfsc.getText().trim());
            bank.setBranchName(txtBranchName.getText().trim());
            bank.setPersonName(txtPersonName.getText().trim());
            bank.setContactNo(txtContactNo.getText().trim());
            bank.setBankAddress(txtBankAddress.getText().trim());

            try {
                String balanceText = txtBankBalance.getText().trim();
                if (!balanceText.isEmpty()) {
                    bank.setBankBalance(Double.parseDouble(balanceText));
                } else {
                    bank.setBankBalance(0.0);
                }
            } catch (NumberFormatException e) {
                bank.setBankBalance(0.0);
            }

            if (selectedBank == null) {
                bank.setStatus("ACTIVE");
                bankService.saveBank(bank);
                alertNotification.showSuccess("Bank created successfully!");
            } else {
                bankService.updateBank(bank);
                alertNotification.showSuccess("Bank updated successfully!");
                selectedBank = null;
            }

            clearForm();
            loadBanks();

        } catch (Exception e) {
            LOG.error("Error saving bank: ", e);
            alertNotification.showError("Error saving bank: " + e.getMessage());
        }
    }

    private void editBank(BankTableData bankData) {
        selectedBank = bankData;

        // Load full bank details
        try {
            Bank bank = bankService.getBankById(bankData.getId()).orElse(null);
            if (bank != null) {
                txtBankName.setText(bank.getBankName());
                txtAccountNo.setText(bank.getAccountNo());
                cmbAccountType.setValue(bank.getAccountType());
                txtIfsc.setText(bank.getIfsc());
                txtBranchName.setText(bank.getBranchName());
                txtPersonName.setText(bank.getPersonName());
                txtContactNo.setText(bank.getContactNo());
                txtBankAddress.setText(bank.getBankAddress());
                txtBankBalance.setText(bank.getBankBalance() != null ? String.format("%.2f", bank.getBankBalance()) : "0.00");

                // Show Update button, hide Save button
                btnSave.setVisible(false);
                btnSave.setManaged(false);
                btnUpdate.setVisible(true);
                btnUpdate.setManaged(true);
            }
        } catch (Exception e) {
            LOG.error("Error loading bank details: ", e);
        }
    }

    private void clearForm() {
        txtBankName.clear();
        txtAccountNo.clear();
        cmbAccountType.setValue(null);
        txtIfsc.clear();
        txtBranchName.clear();
        txtPersonName.clear();
        txtContactNo.clear();
        txtBankAddress.clear();
        txtBankBalance.setText("0.00");
        selectedBank = null;

        // Show Save button, hide Update button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
    }

    private boolean validateInput() {
        if (txtBankName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter bank name");
            txtBankName.requestFocus();
            return false;
        }

        if (txtAccountNo.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter account number");
            txtAccountNo.requestFocus();
            return false;
        }

        return true;
    }

    private void loadBanks() {
        try {
            List<Bank> banks = bankService.getAllBanks();
            bankData.clear();

            for (Bank bank : banks) {
                bankData.add(new BankTableData(
                        bank.getId(),
                        bank.getBankName(),
                        bank.getAccountNo(),
                        bank.getAccountType(),
                        bank.getBranchName(),
                        bank.getBankBalance(),
                        bank.getStatus()
                ));
            }

            tblBanks.refresh();

            // Update autocomplete suggestions
            updateAutoCompleteSuggestions();

            LOG.info("Loaded {} banks", banks.size());

        } catch (Exception e) {
            LOG.error("Error loading banks: ", e);
            alertNotification.showError("Error loading banks: " + e.getMessage());
        }
    }

    // Inner class for table data
    public static class BankTableData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty bankName;
        private final SimpleStringProperty accountNo;
        private final SimpleStringProperty accountType;
        private final SimpleStringProperty branchName;
        private final SimpleDoubleProperty balance;
        private final SimpleStringProperty status;

        public BankTableData(Integer id, String bankName, String accountNo, String accountType,
                             String branchName, Double balance, String status) {
            this.id = new SimpleIntegerProperty(id != null ? id : 0);
            this.bankName = new SimpleStringProperty(bankName != null ? bankName : "");
            this.accountNo = new SimpleStringProperty(accountNo != null ? accountNo : "");
            this.accountType = new SimpleStringProperty(accountType != null ? accountType : "");
            this.branchName = new SimpleStringProperty(branchName != null ? branchName : "");
            this.balance = new SimpleDoubleProperty(balance != null ? balance : 0.0);
            this.status = new SimpleStringProperty(status != null ? status : "ACTIVE");
        }

        public Integer getId() {
            return id.get();
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public String getBankName() {
            return bankName.get();
        }

        public SimpleStringProperty bankNameProperty() {
            return bankName;
        }

        public String getAccountNo() {
            return accountNo.get();
        }

        public SimpleStringProperty accountNoProperty() {
            return accountNo;
        }

        public String getAccountType() {
            return accountType.get();
        }

        public SimpleStringProperty accountTypeProperty() {
            return accountType;
        }

        public String getBranchName() {
            return branchName.get();
        }

        public SimpleStringProperty branchNameProperty() {
            return branchName;
        }

        public Double getBalance() {
            return balance.get();
        }

        public SimpleDoubleProperty balanceProperty() {
            return balance;
        }

        public String getStatus() {
            return status.get();
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}
