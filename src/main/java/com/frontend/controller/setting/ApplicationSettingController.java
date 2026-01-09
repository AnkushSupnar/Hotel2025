package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.ApplicationSetting;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
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

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private ApplicationSettingService applicationSettingService;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupEventHandlers();
        loadAvailablePrinters();
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

            // Save Billing Printer
            String billingPrinter = cmbBillingPrinter.getValue();
            if (billingPrinter != null && !billingPrinter.equals("-- None --")) {
                applicationSettingService.saveSetting(BILLING_PRINTER_SETTING, billingPrinter);
                successMessage.append("Billing printer saved. ");
                hasChanges = true;
                LOG.info("Billing printer saved: {}", billingPrinter);
            } else if (billingPrinter != null && billingPrinter.equals("-- None --")) {
                // Clear the setting if "None" is selected
                applicationSettingService.deleteSettingByName(BILLING_PRINTER_SETTING);
                successMessage.append("Billing printer cleared. ");
                hasChanges = true;
                LOG.info("Billing printer setting cleared");
            }

            // Save KOT Printer
            String kotPrinter = cmbKotPrinter.getValue();
            if (kotPrinter != null && !kotPrinter.equals("-- None --")) {
                applicationSettingService.saveSetting(KOT_PRINTER_SETTING, kotPrinter);
                successMessage.append("KOT printer saved. ");
                hasChanges = true;
                LOG.info("KOT printer saved: {}", kotPrinter);
            } else if (kotPrinter != null && kotPrinter.equals("-- None --")) {
                // Clear the setting if "None" is selected
                applicationSettingService.deleteSettingByName(KOT_PRINTER_SETTING);
                successMessage.append("KOT printer cleared. ");
                hasChanges = true;
                LOG.info("KOT printer setting cleared");
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

            // Load Billing Printer Setting
            Optional<ApplicationSetting> billingPrinterSetting = applicationSettingService.getSettingByName(BILLING_PRINTER_SETTING);
            if (billingPrinterSetting.isPresent()) {
                String billingPrinter = billingPrinterSetting.get().getSettingValue();
                lblCurrentBillingPrinter.setText(billingPrinter);
                // Also select in combo box if available
                if (cmbBillingPrinter.getItems().contains(billingPrinter)) {
                    cmbBillingPrinter.setValue(billingPrinter);
                }
                LOG.info("Loaded current billing printer: {}", billingPrinter);
            } else {
                lblCurrentBillingPrinter.setText("Not configured");
                LOG.info("No billing printer setting found");
            }

            // Load KOT Printer Setting
            Optional<ApplicationSetting> kotPrinterSetting = applicationSettingService.getSettingByName(KOT_PRINTER_SETTING);
            if (kotPrinterSetting.isPresent()) {
                String kotPrinter = kotPrinterSetting.get().getSettingValue();
                lblCurrentKotPrinter.setText(kotPrinter);
                // Also select in combo box if available
                if (cmbKotPrinter.getItems().contains(kotPrinter)) {
                    cmbKotPrinter.setValue(kotPrinter);
                }
                LOG.info("Loaded current KOT printer: {}", kotPrinter);
            } else {
                lblCurrentKotPrinter.setText("Not configured");
                LOG.info("No KOT printer setting found");
            }

        } catch (Exception e) {
            LOG.error("Error loading current settings: ", e);
            lblCurrentDocument.setText("Error loading settings");
            lblCurrentFont.setText("Error loading settings");
            lblCurrentBillingPrinter.setText("Error loading settings");
            lblCurrentKotPrinter.setText("Error loading settings");
        }
    }
}
