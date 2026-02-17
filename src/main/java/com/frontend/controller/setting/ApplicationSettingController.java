package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.Bank;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.BankService;
import com.frontend.service.SessionService;
import com.frontend.util.ApplicationSettingProperties;
import com.frontend.view.AlertNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class ApplicationSettingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationSettingController.class);

    // Setting keys
    private static final String FONT_PATH_SETTING = "input_font_path";
    private static final String DOCUMENT_PATH_SETTING = "document_directory";
    private static final String BILLING_PRINTER_SETTING = "billing_printer";
    private static final String KOT_PRINTER_SETTING = "kot_printer";
    private static final String DEFAULT_BANK_SETTING = "default_billing_bank";
    private static final String BILL_LOGO_SETTING = "bill_logo_image";
    private static final String USE_BILL_LOGO_SETTING = "use_bill_logo";

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private ApplicationSettingService applicationSettingService;

    @Autowired
    private BankService bankService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SessionService sessionService;

    @FXML
    private Button btnBack;

    @FXML
    private TextField txtDocumentPath;

    @FXML
    private Button btnBrowseDocument;

    @FXML
    private TextField txtFontPath;

    @FXML
    private Button btnBrowseFont;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnClear;

    @FXML
    private Label lblCurrentDocument;

    @FXML
    private Label lblCurrentFont;

    // Printer components
    @FXML
    private ComboBox<String> cmbBillingPrinter;

    @FXML
    private ComboBox<String> cmbKotPrinter;

    @FXML
    private Button btnRefreshBillingPrinter;

    @FXML
    private Button btnRefreshKotPrinter;

    @FXML
    private Label lblCurrentBillingPrinter;

    @FXML
    private Label lblCurrentKotPrinter;

    // Default Bank components
    @FXML
    private ComboBox<String> cmbDefaultBank;

    @FXML
    private Button btnRefreshBanks;

    @FXML
    private Label lblCurrentDefaultBank;

    // Bill Logo components
    @FXML
    private TextField txtBillLogoPath;

    @FXML
    private Button btnBrowseBillLogo;

    @FXML
    private Button btnUploadBillLogo;

    @FXML
    private Label lblCurrentBillLogo;

    @FXML
    private CheckBox chkUseBillLogo;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupEventHandlers();
        loadAvailablePrinters();
        loadAvailableBanks();
        loadCurrentSettings();
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to Settings Menu");
                navigateToSettingMenu();
            } catch (Exception ex) {
                LOG.error("Error returning to Settings Menu: ", ex);
            }
        });
    }

    private void navigateToSettingMenu() {
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
            return (BorderPane) txtFontPath.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupEventHandlers() {
        btnBrowseDocument.setOnAction(e -> browseDocumentDirectory());
        btnBrowseFont.setOnAction(e -> browseFontFile());
        btnSave.setOnAction(e -> saveSettings());
        btnClear.setOnAction(e -> clearForm());

        // Printer refresh buttons
        btnRefreshBillingPrinter.setOnAction(e -> refreshPrinters(cmbBillingPrinter));
        btnRefreshKotPrinter.setOnAction(e -> refreshPrinters(cmbKotPrinter));

        // Bank refresh button
        btnRefreshBanks.setOnAction(e -> refreshBanks());

        // Bill Logo buttons
        btnBrowseBillLogo.setOnAction(e -> browseBillLogo());
        btnUploadBillLogo.setOnAction(e -> uploadBillLogo());

        // Use Bill Logo checkbox
        chkUseBillLogo.selectedProperty().addListener((obs, oldVal, newVal) -> saveUseBillLogoSetting(newVal));
    }

    /**
     * Load all available printers from the system
     */
    private void loadAvailablePrinters() {
        try {
            ObservableList<String> printerNames = getAvailablePrinterNames();

            // Populate both combo boxes
            cmbBillingPrinter.setItems(FXCollections.observableArrayList(printerNames));
            cmbKotPrinter.setItems(FXCollections.observableArrayList(printerNames));

            LOG.info("Loaded {} available printers", printerNames.size());
        } catch (Exception e) {
            LOG.error("Error loading available printers: ", e);
            alertNotification.showError("Error loading printers: " + e.getMessage());
        }
    }

    /**
     * Get list of all available printer names
     */
    private ObservableList<String> getAvailablePrinterNames() {
        ObservableList<String> printerNames = FXCollections.observableArrayList();

        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);

            if (printServices != null && printServices.length > 0) {
                Arrays.stream(printServices)
                        .map(PrintService::getName)
                        .sorted()
                        .forEach(printerNames::add);
            }

            // Add a "None" option at the beginning
            printerNames.add(0, "-- None --");

        } catch (Exception e) {
            LOG.error("Error getting printer list: ", e);
        }

        return printerNames;
    }

    /**
     * Refresh printer list for a specific combo box
     */
    private void refreshPrinters(ComboBox<String> comboBox) {
        try {
            String currentSelection = comboBox.getValue();
            ObservableList<String> printerNames = getAvailablePrinterNames();
            comboBox.setItems(printerNames);

            // Restore previous selection if still available
            if (currentSelection != null && printerNames.contains(currentSelection)) {
                comboBox.setValue(currentSelection);
            }

            alertNotification.showSuccess("Printer list refreshed");
            LOG.info("Printer list refreshed, found {} printers", printerNames.size() - 1);
        } catch (Exception e) {
            LOG.error("Error refreshing printer list: ", e);
            alertNotification.showError("Error refreshing printers: " + e.getMessage());
        }
    }

    /**
     * Load all available active banks for the default bank dropdown with custom font
     */
    private void loadAvailableBanks() {
        try {
            ObservableList<String> bankNames = FXCollections.observableArrayList();

            // Add a "None" option at the beginning
            bankNames.add("-- None --");

            // Get all active banks only
            java.util.List<Bank> banks = bankService.getActiveBanks();
            for (Bank bank : banks) {
                bankNames.add(bank.getBankName());
            }

            cmbDefaultBank.setItems(bankNames);

            // Get custom font for bank names (20px size)
            javafx.scene.text.Font customFont = SessionService.getCustomFont(20.0);
            final String fontFamily = customFont != null ? customFont.getFamily() : null;

            // Apply custom cell factory for dropdown list with custom font
            cmbDefaultBank.setCellFactory(param -> {
                javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            if (item.equals("-- None --")) {
                                setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
                            } else if (fontFamily != null) {
                                setStyle("-fx-font-family: '" + fontFamily + "'; -fx-text-fill: #424242; -fx-font-size: 20px;");
                            } else {
                                setStyle("-fx-text-fill: #424242; -fx-font-size: 20px;");
                            }
                        }
                    }
                };

                // Add click handler to hide popup after selection
                cell.setOnMouseClicked(event -> {
                    if (!cell.isEmpty()) {
                        cmbDefaultBank.setValue(cell.getItem());
                        cmbDefaultBank.hide();
                    }
                });

                return cell;
            });

            // Apply custom font to button cell (selected value display)
            cmbDefaultBank.setButtonCell(new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equals("-- None --")) {
                            setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
                        } else if (fontFamily != null) {
                            setStyle("-fx-font-family: '" + fontFamily + "'; -fx-text-fill: #424242; -fx-font-size: 20px;");
                        } else {
                            setStyle("-fx-text-fill: #424242; -fx-font-size: 20px;");
                        }
                    }
                }
            });

            LOG.info("Loaded {} active banks for default bank dropdown with custom font: {}", banks.size(), fontFamily);
        } catch (Exception e) {
            LOG.error("Error loading available banks: ", e);
            alertNotification.showError("Error loading banks: " + e.getMessage());
        }
    }

    /**
     * Refresh bank list
     */
    private void refreshBanks() {
        try {
            String currentSelection = cmbDefaultBank.getValue();
            loadAvailableBanks();

            // Restore previous selection if still available
            if (currentSelection != null && cmbDefaultBank.getItems().contains(currentSelection)) {
                cmbDefaultBank.setValue(currentSelection);
            }

            alertNotification.showSuccess("Bank list refreshed");
            LOG.info("Bank list refreshed");
        } catch (Exception e) {
            LOG.error("Error refreshing bank list: ", e);
            alertNotification.showError("Error refreshing banks: " + e.getMessage());
        }
    }

    private void browseDocumentDirectory() {
        try {
            javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
            directoryChooser.setTitle("Select Document Directory");

            // Set initial directory to user's home
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

            // Show directory chooser dialog
            File selectedDirectory = directoryChooser.showDialog(btnBrowseDocument.getScene().getWindow());

            if (selectedDirectory != null) {
                String absolutePath = selectedDirectory.getAbsolutePath();
                txtDocumentPath.setText(absolutePath);
                LOG.info("Document directory selected: {}", absolutePath);
            }
        } catch (Exception e) {
            LOG.error("Error browsing document directory: ", e);
            alertNotification.showError("Error selecting directory: " + e.getMessage());
        }
    }

    private void browseFontFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Font File");

            // Set extension filters
            FileChooser.ExtensionFilter fontFilter = new FileChooser.ExtensionFilter(
                    "Font Files", "*.ttf", "*.otf", "*.TTF", "*.OTF");
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(
                    "All Files", "*.*");
            fileChooser.getExtensionFilters().addAll(fontFilter, allFilter);

            // Set initial directory to user's home
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

            // Show open file dialog
            File selectedFile = fileChooser.showOpenDialog(btnBrowseFont.getScene().getWindow());

            if (selectedFile != null) {
                String absolutePath = selectedFile.getAbsolutePath();
                txtFontPath.setText(absolutePath);
                LOG.info("Font file selected: {}", absolutePath);
            }
        } catch (Exception e) {
            LOG.error("Error browsing font file: ", e);
            alertNotification.showError("Error selecting font file: " + e.getMessage());
        }
    }

    private void browseBillLogo() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Bill Logo Image");

            // Set extension filters for image files
            FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                    "Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.PNG", "*.JPG", "*.JPEG", "*.BMP", "*.GIF");
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(
                    "All Files", "*.*");
            fileChooser.getExtensionFilters().addAll(imageFilter, allFilter);

            // Set initial directory to user's home
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

            // Show open file dialog
            File selectedFile = fileChooser.showOpenDialog(btnBrowseBillLogo.getScene().getWindow());

            if (selectedFile != null) {
                String absolutePath = selectedFile.getAbsolutePath();
                txtBillLogoPath.setText(absolutePath);
                LOG.info("Bill logo image selected: {}", absolutePath);
            }
        } catch (Exception e) {
            LOG.error("Error browsing bill logo image: ", e);
            alertNotification.showError("Error selecting bill logo image: " + e.getMessage());
        }
    }

    private void uploadBillLogo() {
        try {
            String selectedFilePath = txtBillLogoPath.getText();
            if (selectedFilePath == null || selectedFilePath.trim().isEmpty()) {
                alertNotification.showError("Please browse and select a bill logo image first.");
                return;
            }

            File sourceFile = new File(selectedFilePath);
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                alertNotification.showError("Selected bill logo image file does not exist.");
                return;
            }

            // Validate file is an image
            String fileName = sourceFile.getName().toLowerCase();
            if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")
                    && !fileName.endsWith(".bmp") && !fileName.endsWith(".gif")) {
                alertNotification.showError("Please select a valid image file (.png, .jpg, .jpeg, .bmp, .gif)");
                return;
            }

            // Get document directory from settings
            Optional<ApplicationSetting> documentSetting = applicationSettingService.getSettingByName(DOCUMENT_PATH_SETTING);
            if (!documentSetting.isPresent() || documentSetting.get().getSettingValue() == null
                    || documentSetting.get().getSettingValue().trim().isEmpty()) {
                alertNotification.showError("Document Directory is not configured. Please set the Document Directory first and save settings.");
                return;
            }

            String documentDirectory = documentSetting.get().getSettingValue();
            File docDir = new File(documentDirectory);
            if (!docDir.exists() || !docDir.isDirectory()) {
                alertNotification.showError("Document Directory does not exist: " + documentDirectory);
                return;
            }

            // Copy file to document directory, renamed to "billlogo" with original extension
            String extension = fileName.substring(fileName.lastIndexOf("."));
            Path sourcePath = sourceFile.toPath();
            Path destinationPath = new File(docDir, "billlogo" + extension).toPath();
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // Save the setting with the destination file path
            String destinationFilePath = destinationPath.toString();
            applicationSettingService.saveSetting(BILL_LOGO_SETTING, destinationFilePath);

            // Reload settings in session
            sessionService.reloadApplicationSettings();

            // Update the UI
            lblCurrentBillLogo.setText(destinationFilePath);
            txtBillLogoPath.clear();

            alertNotification.showSuccess("Bill logo image uploaded successfully to: " + destinationFilePath);
            LOG.info("Bill logo image uploaded to: {}", destinationFilePath);

        } catch (Exception e) {
            LOG.error("Error uploading bill logo image: ", e);
            alertNotification.showError("Error uploading bill logo image: " + e.getMessage());
        }
    }

    private void saveUseBillLogoSetting(boolean useLogo) {
        try {
            applicationSettingService.saveSetting(USE_BILL_LOGO_SETTING, String.valueOf(useLogo));
            SessionService.setUseBillLogo(useLogo);
            sessionService.reloadApplicationSettings();
            LOG.info("Use bill logo setting saved: {}", useLogo);
        } catch (Exception e) {
            LOG.error("Error saving use bill logo setting: ", e);
            alertNotification.showError("Error saving use bill logo setting: " + e.getMessage());
        }
    }

    private void saveSettings() {
        try {
            boolean hasChanges = false;
            StringBuilder successMessage = new StringBuilder();

            // Save Document Directory
            String documentPath = txtDocumentPath.getText();
            if (documentPath != null && !documentPath.trim().isEmpty()) {
                File documentDir = new File(documentPath);
                if (!documentDir.exists() || !documentDir.isDirectory()) {
                    alertNotification.showError("Selected document directory does not exist or is not a directory");
                    return;
                }
                applicationSettingService.saveSetting(DOCUMENT_PATH_SETTING, documentPath);
                successMessage.append("Document directory saved. ");
                hasChanges = true;
                LOG.info("Document directory saved: {}", documentPath);
            }

            // Save Font File
            String fontPath = txtFontPath.getText();
            if (fontPath != null && !fontPath.trim().isEmpty()) {
                File fontFile = new File(fontPath);
                if (!fontFile.exists()) {
                    alertNotification.showError("Selected font file does not exist");
                    return;
                }

                String fileName = fontFile.getName().toLowerCase();
                if (!fileName.endsWith(".ttf") && !fileName.endsWith(".otf")) {
                    alertNotification.showError("Please select a valid font file (.ttf or .otf)");
                    return;
                }

                applicationSettingService.saveSetting(FONT_PATH_SETTING, fontPath);
                successMessage.append("Font setting saved. ");
                hasChanges = true;
                LOG.info("Font setting saved: {}", fontPath);
            }

            // Determine document directory for saving machine-specific settings
            String docDir = (documentPath != null && !documentPath.trim().isEmpty())
                    ? documentPath : SessionService.getDocumentDirectory();

            // Save Billing Printer (to local properties file)
            String billingPrinter = cmbBillingPrinter.getValue();
            if (billingPrinter != null && !billingPrinter.equals("-- None --")) {
                if (docDir == null || docDir.trim().isEmpty()) {
                    alertNotification.showError("Please configure document directory first to save printer/bank settings");
                    return;
                }
                ApplicationSettingProperties.saveSetting(docDir, BILLING_PRINTER_SETTING, billingPrinter);
                successMessage.append("Billing printer saved. ");
                hasChanges = true;
                LOG.info("Billing printer saved to properties file: {}", billingPrinter);
            } else if (billingPrinter != null && billingPrinter.equals("-- None --")) {
                if (docDir != null && !docDir.trim().isEmpty()) {
                    ApplicationSettingProperties.removeSetting(docDir, BILLING_PRINTER_SETTING);
                }
                successMessage.append("Billing printer cleared. ");
                hasChanges = true;
                LOG.info("Billing printer setting cleared from properties file");
            }

            // Save KOT Printer (to local properties file)
            String kotPrinter = cmbKotPrinter.getValue();
            if (kotPrinter != null && !kotPrinter.equals("-- None --")) {
                if (docDir == null || docDir.trim().isEmpty()) {
                    alertNotification.showError("Please configure document directory first to save printer/bank settings");
                    return;
                }
                ApplicationSettingProperties.saveSetting(docDir, KOT_PRINTER_SETTING, kotPrinter);
                successMessage.append("KOT printer saved. ");
                hasChanges = true;
                LOG.info("KOT printer saved to properties file: {}", kotPrinter);
            } else if (kotPrinter != null && kotPrinter.equals("-- None --")) {
                if (docDir != null && !docDir.trim().isEmpty()) {
                    ApplicationSettingProperties.removeSetting(docDir, KOT_PRINTER_SETTING);
                }
                successMessage.append("KOT printer cleared. ");
                hasChanges = true;
                LOG.info("KOT printer setting cleared from properties file");
            }

            // Save Default Bank for Billing (to local properties file)
            String defaultBank = cmbDefaultBank.getValue();
            if (defaultBank != null && !defaultBank.equals("-- None --")) {
                if (docDir == null || docDir.trim().isEmpty()) {
                    alertNotification.showError("Please configure document directory first to save printer/bank settings");
                    return;
                }
                ApplicationSettingProperties.saveSetting(docDir, DEFAULT_BANK_SETTING, defaultBank);
                successMessage.append("Default bank saved. ");
                hasChanges = true;
                LOG.info("Default bank saved to properties file: {}", defaultBank);
            } else if (defaultBank != null && defaultBank.equals("-- None --")) {
                if (docDir != null && !docDir.trim().isEmpty()) {
                    ApplicationSettingProperties.removeSetting(docDir, DEFAULT_BANK_SETTING);
                }
                successMessage.append("Default bank cleared. ");
                hasChanges = true;
                LOG.info("Default bank setting cleared from properties file");
            }

            if (!hasChanges) {
                alertNotification.showError("No changes to save. Please configure at least one setting.");
                return;
            }

            alertNotification.showSuccess(successMessage.toString().trim());

            // Reload settings in session
            sessionService.reloadApplicationSettings();
            LOG.info("Application settings reloaded in session");

            loadCurrentSettings();

        } catch (Exception e) {
            LOG.error("Error saving settings: ", e);
            alertNotification.showError("Error saving settings: " + e.getMessage());
        }
    }

    private void clearForm() {
        txtDocumentPath.clear();
        txtFontPath.clear();
        cmbBillingPrinter.setValue(null);
        cmbKotPrinter.setValue(null);
        cmbDefaultBank.setValue(null);
        txtBillLogoPath.clear();
        chkUseBillLogo.setSelected(false);
    }

    private void loadCurrentSettings() {
        try {
            // Load Document Directory Setting
            Optional<ApplicationSetting> documentSetting = applicationSettingService.getSettingByName(DOCUMENT_PATH_SETTING);
            if (documentSetting.isPresent()) {
                String documentPath = documentSetting.get().getSettingValue();
                lblCurrentDocument.setText(documentPath);
                LOG.info("Loaded current document directory: {}", documentPath);
            } else {
                lblCurrentDocument.setText("Not configured");
                LOG.info("No document directory setting found");
            }

            // Load Font Setting
            Optional<ApplicationSetting> fontSetting = applicationSettingService.getSettingByName(FONT_PATH_SETTING);
            if (fontSetting.isPresent()) {
                String fontPath = fontSetting.get().getSettingValue();
                lblCurrentFont.setText(fontPath);
                LOG.info("Loaded current font setting: {}", fontPath);
            } else {
                lblCurrentFont.setText("Not configured");
                LOG.info("No font setting found");
            }

            // Load Billing Printer Setting (from session, which includes properties file values)
            String billingPrinter = SessionService.getApplicationSetting(BILLING_PRINTER_SETTING);
            if (billingPrinter != null && !billingPrinter.trim().isEmpty()) {
                lblCurrentBillingPrinter.setText(billingPrinter);
                if (cmbBillingPrinter.getItems().contains(billingPrinter)) {
                    cmbBillingPrinter.setValue(billingPrinter);
                }
                LOG.info("Loaded current billing printer: {}", billingPrinter);
            } else {
                lblCurrentBillingPrinter.setText("Not configured");
                LOG.info("No billing printer setting found");
            }

            // Load KOT Printer Setting (from session, which includes properties file values)
            String kotPrinter = SessionService.getApplicationSetting(KOT_PRINTER_SETTING);
            if (kotPrinter != null && !kotPrinter.trim().isEmpty()) {
                lblCurrentKotPrinter.setText(kotPrinter);
                if (cmbKotPrinter.getItems().contains(kotPrinter)) {
                    cmbKotPrinter.setValue(kotPrinter);
                }
                LOG.info("Loaded current KOT printer: {}", kotPrinter);
            } else {
                lblCurrentKotPrinter.setText("Not configured");
                LOG.info("No KOT printer setting found");
            }

            // Load Default Bank Setting (from session, which includes properties file values)
            String defaultBank = SessionService.getApplicationSetting(DEFAULT_BANK_SETTING);
            if (defaultBank != null && !defaultBank.trim().isEmpty()) {
                lblCurrentDefaultBank.setText(defaultBank);
                if (cmbDefaultBank.getItems().contains(defaultBank)) {
                    cmbDefaultBank.setValue(defaultBank);
                }
                LOG.info("Loaded current default bank: {}", defaultBank);
            } else {
                lblCurrentDefaultBank.setText("Not configured");
                LOG.info("No default bank setting found");
            }

            // Load Bill Logo Setting
            Optional<ApplicationSetting> billLogoSetting = applicationSettingService.getSettingByName(BILL_LOGO_SETTING);
            if (billLogoSetting.isPresent()) {
                String billLogoPath = billLogoSetting.get().getSettingValue();
                lblCurrentBillLogo.setText(billLogoPath);
                LOG.info("Loaded current bill logo: {}", billLogoPath);
            } else {
                lblCurrentBillLogo.setText("Not configured");
                LOG.info("No bill logo setting found");
            }

            // Load Use Bill Logo checkbox
            Optional<ApplicationSetting> useBillLogoSetting = applicationSettingService.getSettingByName(USE_BILL_LOGO_SETTING);
            if (useBillLogoSetting.isPresent()) {
                boolean useLogo = "true".equalsIgnoreCase(useBillLogoSetting.get().getSettingValue());
                chkUseBillLogo.setSelected(useLogo);
                LOG.info("Loaded use bill logo setting: {}", useLogo);
            } else {
                chkUseBillLogo.setSelected(false);
                LOG.info("No use bill logo setting found, defaulting to false");
            }

        } catch (Exception e) {
            LOG.error("Error loading current settings: ", e);
            lblCurrentDocument.setText("Error loading settings");
            lblCurrentFont.setText("Error loading settings");
            lblCurrentBillingPrinter.setText("Error loading settings");
            lblCurrentKotPrinter.setText("Error loading settings");
            lblCurrentDefaultBank.setText("Error loading settings");
            lblCurrentBillLogo.setText("Error loading settings");
        }
    }
}
