package com.frontend.controller.employee;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Bank;
import com.frontend.entity.EmployeeAdvancePayment;
import com.frontend.entity.Employees;
import com.frontend.repository.EmployeesRepository;
import com.frontend.service.BankService;
import com.frontend.service.EmployeeAdvanceService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class EmployeeAdvanceController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeAdvanceController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Autowired
    private EmployeeAdvanceService advanceService;

    @Autowired
    private EmployeesRepository employeesRepository;

    @Autowired
    private BankService bankService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    @FXML private Button btnBack;
    @FXML private Label lblEmployee;
    @FXML private TextField txtEmployee;
    @FXML private TextField txtAmount;
    @FXML private TextField txtReason;
    @FXML private ComboBox<String> cmbPaymentMode;
    @FXML private ComboBox<String> cmbBank;
    @FXML private VBox bankSection;
    @FXML private TextField txtRemarks;
    @FXML private Button btnSave;
    @FXML private Button btnClear;

    @FXML private TableView<AdvanceRow> tblAdvances;
    @FXML private TableColumn<AdvanceRow, Integer> colId;
    @FXML private TableColumn<AdvanceRow, String> colEmployeeName;
    @FXML private TableColumn<AdvanceRow, String> colAmount;
    @FXML private TableColumn<AdvanceRow, String> colReason;
    @FXML private TableColumn<AdvanceRow, String> colPaymentMode;
    @FXML private TableColumn<AdvanceRow, String> colDate;

    @FXML private Label lblTotalAdvances;
    @FXML private Label lblTotalAmount;

    private ObservableList<AdvanceRow> advanceRows = FXCollections.observableArrayList();

    // Maps for lookups
    private Map<String, Integer> employeeMap = new HashMap<>();
    private Map<String, Integer> bankMap = new HashMap<>();

    private AutoCompleteTextField employeeAutoComplete;
    private Font customFont20;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        customFont20 = SessionService.getCustomFont(20.0);
        setupBackButton();
        setupEmployeeAutoComplete();
        setupPaymentModeComboBox();
        setupBankComboBox();
        setupTable();
        setupButtons();
        applyCustomFonts();
        loadAdvances();
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                BorderPane mainPane = getMainPane();
                if (mainPane != null) {
                    Pane pane = loader.getPage("/fxml/employee/EmployeeServiceMenu.fxml");
                    mainPane.setCenter(pane);
                }
            } catch (Exception ex) {
                LOG.error("Error navigating back: ", ex);
            }
        });
    }

    private void setupEmployeeAutoComplete() {
        try {
            List<Employees> employees = employeesRepository.findByActiveStatusTrueOrderByFirstNameAsc();
            List<String> employeeNames = new java.util.ArrayList<>();
            for (Employees emp : employees) {
                String name = emp.getFullName();
                employeeNames.add(name);
                employeeMap.put(name, emp.getEmployeeId());
            }

            employeeAutoComplete = new AutoCompleteTextField(txtEmployee, employeeNames, customFont20);
            employeeAutoComplete.setUseContainsFilter(true);
            employeeAutoComplete.setNextFocusField(txtAmount);
        } catch (Exception e) {
            LOG.error("Error loading employees: ", e);
        }
    }

    private void setupPaymentModeComboBox() {
        cmbPaymentMode.setItems(FXCollections.observableArrayList("CASH", "BANK"));
        cmbPaymentMode.getSelectionModel().select("CASH");

        // Show/hide bank section based on payment mode
        bankSection.setVisible(false);
        bankSection.setManaged(false);

        cmbPaymentMode.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isBank = "BANK".equals(newVal);
            bankSection.setVisible(isBank);
            bankSection.setManaged(isBank);
            if (!isBank) {
                cmbBank.getSelectionModel().clearSelection();
            }
        });
    }

    private void setupBankComboBox() {
        try {
            List<Bank> banks = bankService.getActiveBanks();
            ObservableList<String> bankNames = FXCollections.observableArrayList();
            for (Bank bank : banks) {
                String name = bank.getBankName();
                bankNames.add(name);
                bankMap.put(name, bank.getId());
            }
            cmbBank.setItems(bankNames);

            // Apply custom font 20px to bank dropdown list and button cell
            cmbBank.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    if (customFont20 != null) {
                        setFont(customFont20);
                    }
                }
            });
            cmbBank.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    if (customFont20 != null) {
                        setFont(customFont20);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Error loading banks: ", e);
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colEmployeeName.setCellValueFactory(cellData -> cellData.getValue().employeeNameProperty());
        colAmount.setCellValueFactory(cellData -> cellData.getValue().amountStrProperty());
        colReason.setCellValueFactory(cellData -> cellData.getValue().reasonProperty());
        colPaymentMode.setCellValueFactory(cellData -> cellData.getValue().paymentModeProperty());
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        // Apply custom font to Employee Name column
        applyEmployeeNameColumnFont();

        // Style amount column (bold teal)
        colAmount.setCellFactory(column -> new TableCell<AdvanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #00897B; -fx-alignment: CENTER_RIGHT;");
                }
            }
        });

        // Style payment mode column
        colPaymentMode.setCellFactory(column -> new TableCell<AdvanceRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("BANK".equals(item)) {
                        setStyle("-fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        tblAdvances.setItems(advanceRows);

        // Responsive column widths
        tblAdvances.widthProperty().addListener((obs, oldVal, newVal) -> {
            double totalWidth = newVal.doubleValue() - 20;
            colId.setPrefWidth(totalWidth * 0.06);
            colEmployeeName.setPrefWidth(totalWidth * 0.25);
            colAmount.setPrefWidth(totalWidth * 0.15);
            colReason.setPrefWidth(totalWidth * 0.22);
            colPaymentMode.setPrefWidth(totalWidth * 0.12);
            colDate.setPrefWidth(totalWidth * 0.18);
        });
    }

    private void applyCustomFonts() {
        if (customFont20 != null) {
            lblEmployee.setFont(customFont20);
            txtEmployee.setFont(customFont20);
            txtRemarks.setFont(customFont20);
        }
    }

    private void setupButtons() {
        btnSave.setOnAction(e -> saveAdvance());
        btnClear.setOnAction(e -> clearForm());
    }

    private void saveAdvance() {
        try {
            // Validate
            String rawEmployeeName = txtEmployee.getText();
            if (rawEmployeeName == null || rawEmployeeName.trim().isEmpty()) {
                alertNotification.showWarning("Please select an employee");
                txtEmployee.requestFocus();
                return;
            }
            String employeeName = rawEmployeeName.trim();

            String amountText = txtAmount.getText();
            if (amountText == null || amountText.trim().isEmpty()) {
                alertNotification.showWarning("Please enter an amount");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountText.trim());
                if (amount <= 0) {
                    alertNotification.showWarning("Amount must be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                alertNotification.showWarning("Invalid amount. Please enter a valid number");
                return;
            }

            String reason = txtReason.getText();
            String paymentMode = cmbPaymentMode.getValue();
            String remarks = txtRemarks.getText();

            Integer employeeId = employeeMap.get(employeeName);
            if (employeeId == null) {
                alertNotification.showWarning("Employee not found");
                return;
            }

            Integer bankId = null;
            if ("BANK".equals(paymentMode)) {
                String bankName = cmbBank.getValue();
                if (bankName == null || bankName.isEmpty()) {
                    alertNotification.showWarning("Please select a bank for bank payment");
                    return;
                }
                bankId = bankMap.get(bankName);
                if (bankId == null) {
                    alertNotification.showWarning("Bank not found");
                    return;
                }
            }

            // Confirm
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Advance Payment");
            confirm.setHeaderText(null);

            javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(8);

            Label lblEmployeeName = new Label(employeeName);
            Font customFont = SessionService.getCustomFont(20.0);
            if (customFont != null) {
                lblEmployeeName.setFont(customFont);
                lblEmployeeName.setStyle("-fx-text-fill: #00695C;");
            } else {
                lblEmployeeName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00695C;");
            }
            lblEmployeeName.setWrapText(true);

            Label lblDetails = new Label(String.format(
                    "Amount: ₹%.2f\nMode: %s\nReason: %s\n\nConfirm advance payment?",
                    amount, paymentMode, reason != null ? reason : "-"));
            lblDetails.setStyle("-fx-font-size: 14px; -fx-text-fill: #424242;");
            lblDetails.setWrapText(true);

            contentBox.getChildren().addAll(lblEmployeeName, lblDetails);
            confirm.getDialogPane().setContent(contentBox);

            Integer finalBankId = bankId;
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        advanceService.recordAdvance(employeeId, amount, reason, finalBankId, paymentMode, remarks);
                        clearForm();
                        loadAdvances();
                        alertNotification.showSuccess("Advance paid to " + employeeName + ": ₹" + String.format("%.2f", amount));
                    } catch (Exception ex) {
                        LOG.error("Error saving advance: ", ex);
                        alertNotification.showError("Error: " + ex.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            LOG.error("Error processing advance payment: ", e);
            alertNotification.showError("Error: " + e.getMessage());
        }
    }

    private void clearForm() {
        if (employeeAutoComplete != null) {
            employeeAutoComplete.clear();
        }
        txtEmployee.clear();
        txtAmount.clear();
        txtReason.clear();
        cmbPaymentMode.getSelectionModel().select("CASH");
        cmbBank.getSelectionModel().clearSelection();
        txtRemarks.clear();
    }

    private void loadAdvances() {
        try {
            List<EmployeeAdvancePayment> advances = advanceService.getAllAdvances();

            advanceRows.clear();
            for (EmployeeAdvancePayment adv : advances) {
                advanceRows.add(new AdvanceRow(adv));
            }

            updateSummary();
            LOG.info("Loaded {} advance records", advanceRows.size());

        } catch (Exception e) {
            LOG.error("Error loading advances: ", e);
            alertNotification.showError("Error loading advances: " + e.getMessage());
        }
    }

    private void updateSummary() {
        int total = advanceRows.size();
        double totalAmount = advanceRows.stream()
                .mapToDouble(AdvanceRow::getAmount)
                .sum();

        lblTotalAdvances.setText(String.valueOf(total));
        lblTotalAmount.setText(String.format("₹%.2f", totalAmount));
    }

    private void applyEmployeeNameColumnFont() {
        try {
            Font customFont = SessionService.getCustomFont();
            if (customFont != null) {
                String fontFamily = customFont.getFamily();
                colEmployeeName.setCellFactory(column -> {
                    TableCell<AdvanceRow, String> cell = new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : item);
                        }
                    };
                    cell.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 20px;");
                    return cell;
                });
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) btnBack.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane");
            return null;
        }
    }

    /**
     * Table row data class wrapping EmployeeAdvancePayment with JavaFX properties.
     */
    public static class AdvanceRow {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty employeeName;
        private final SimpleDoubleProperty amount;
        private final SimpleStringProperty amountStr;
        private final SimpleStringProperty reason;
        private final SimpleStringProperty paymentMode;
        private final SimpleStringProperty date;

        public AdvanceRow(EmployeeAdvancePayment advance) {
            this.id = new SimpleIntegerProperty(advance.getId());
            this.employeeName = new SimpleStringProperty(advance.getEmployee().getFullName());
            this.amount = new SimpleDoubleProperty(advance.getAmount());
            this.amountStr = new SimpleStringProperty(String.format("₹%.2f", advance.getAmount()));
            this.reason = new SimpleStringProperty(advance.getReason() != null ? advance.getReason() : "");
            this.paymentMode = new SimpleStringProperty(advance.getPaymentMode());
            this.date = new SimpleStringProperty(
                    advance.getPaidAt() != null ? advance.getPaidAt().format(DATE_FORMAT) : "");
        }

        public int getId() { return id.get(); }
        public SimpleIntegerProperty idProperty() { return id; }

        public String getEmployeeName() { return employeeName.get(); }
        public SimpleStringProperty employeeNameProperty() { return employeeName; }

        public double getAmount() { return amount.get(); }
        public SimpleStringProperty amountStrProperty() { return amountStr; }

        public String getReason() { return reason.get(); }
        public SimpleStringProperty reasonProperty() { return reason; }

        public String getPaymentMode() { return paymentMode.get(); }
        public SimpleStringProperty paymentModeProperty() { return paymentMode; }

        public String getDate() { return date.get(); }
        public SimpleStringProperty dateProperty() { return date; }
    }
}
