package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.entity.ApplicationSetting;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class ApplicationSettingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationSettingController.class);
    private static final String FONT_PATH_SETTING = "input_font_path";
    private static final String DOCUMENT_PATH_SETTING = "document_directory";

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupEventHandlers();
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
                successMessage.append("Font setting saved.");
                hasChanges = true;
                LOG.info("Font setting saved: {}", fontPath);
            }

            if (!hasChanges) {
                alertNotification.showError("No changes to save. Please configure at least one setting.");
                return;
            }

            alertNotification.showSuccess(successMessage.toString());

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
        } catch (Exception e) {
            LOG.error("Error loading current settings: ", e);
            lblCurrentDocument.setText("Error loading settings");
            lblCurrentFont.setText("Error loading settings");
        }
    }
}
