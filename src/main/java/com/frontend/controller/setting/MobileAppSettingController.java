package com.frontend.controller.setting;

import com.frontend.service.MobileAppSettingService;
import com.frontend.view.AlertNotification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;

@Component
public class MobileAppSettingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MobileAppSettingController.class);

    @Autowired
    private MobileAppSettingService mobileAppSettingService;

    @Autowired
    private AlertNotification alertNotification;

    // General Settings
    @FXML
    private CheckBox chkMobileAccessEnabled;

    @FXML
    private Spinner<Integer> spinnerExpiryDays;

    @FXML
    private Spinner<Integer> spinnerExpiryHours;

    @FXML
    private TextField txtMinAppVersion;

    @FXML
    private CheckBox chkForceUpdate;

    @FXML
    private Label lblJwtSecret;

    @FXML
    private Button btnRegenerateSecret;

    // Role and Features
    @FXML
    private ComboBox<String> cmbRole;

    @FXML
    private VBox vboxFeatures;

    // Buttons
    @FXML
    private Button btnSave;

    @FXML
    private Button btnReset;

    // Store feature checkboxes for each role
    private Map<String, Map<String, CheckBox>> roleFeatureCheckboxes = new HashMap<>();
    private String currentSelectedRole = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing Mobile App Settings Controller");

        // Initialize default settings in database
        mobileAppSettingService.initializeDefaultSettings();

        setupSpinners();
        setupRoleComboBox();
        loadSettings();
        setupButtons();
    }

    private void setupSpinners() {
        // Days spinner (0-365)
        SpinnerValueFactory<Integer> daysFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 365, 7);
        spinnerExpiryDays.setValueFactory(daysFactory);
        spinnerExpiryDays.setEditable(true);

        // Hours spinner (0-23)
        SpinnerValueFactory<Integer> hoursFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0);
        spinnerExpiryHours.setValueFactory(hoursFactory);
        spinnerExpiryHours.setEditable(true);
    }

    private void setupRoleComboBox() {
        // Add roles to combo box
        cmbRole.setItems(FXCollections.observableArrayList(MobileAppSettingService.DEFAULT_ROLES));

        // When role is selected, show features for that role
        cmbRole.setOnAction(e -> {
            String selectedRole = cmbRole.getValue();
            if (selectedRole != null) {
                currentSelectedRole = selectedRole;
                loadFeaturesForRole(selectedRole);
            }
        });
    }

    private void loadFeaturesForRole(String role) {
        vboxFeatures.getChildren().clear();

        // Get or create checkboxes for this role
        Map<String, CheckBox> checkboxes = roleFeatureCheckboxes.computeIfAbsent(role, k -> new HashMap<>());

        Map<String, String> features = MobileAppSettingService.FEATURE_DEFINITIONS;

        for (Map.Entry<String, String> feature : features.entrySet()) {
            String featureCode = feature.getKey();
            String featureName = feature.getValue();

            // Check if we already have a checkbox for this feature
            CheckBox checkBox;
            if (checkboxes.containsKey(featureCode)) {
                checkBox = checkboxes.get(featureCode);
            } else {
                // Create new checkbox
                checkBox = new CheckBox();
                checkBox.setStyle("-fx-font-size: 14px;");

                // Load current value from database
                boolean isEnabled = mobileAppSettingService.isFeatureEnabledForRole(role, featureCode);
                checkBox.setSelected(isEnabled);

                checkboxes.put(featureCode, checkBox);
            }

            // Create feature row
            HBox featureRow = new HBox(15);
            featureRow.setAlignment(Pos.CENTER_LEFT);
            featureRow.setStyle("-fx-padding: 8 15; -fx-background-color: #f9f9f9; -fx-background-radius: 5;");

            // Feature icon based on type
            String iconColor = getFeatureIconColor(featureCode);
            Label iconLabel = new Label(getFeatureIcon(featureCode));
            iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + iconColor + ";");

            // Feature name and description
            VBox featureInfo = new VBox(2);
            Label nameLabel = new Label(featureName);
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
            Label descLabel = new Label(getFeatureDescription(featureCode));
            descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            featureInfo.getChildren().addAll(nameLabel, descLabel);

            // Spacer
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            featureRow.getChildren().addAll(iconLabel, featureInfo, spacer, checkBox);
            vboxFeatures.getChildren().add(featureRow);
        }

        LOG.info("Loaded {} features for role: {}", features.size(), role);
    }

    private String getFeatureIcon(String featureCode) {
        switch (featureCode) {
            case "VIEW_TABLES": return "\uf0ce"; // table
            case "TAKE_ORDER": return "\uf067"; // plus
            case "VIEW_ORDERS": return "\uf03a"; // list
            case "EDIT_ORDER": return "\uf044"; // edit
            case "CANCEL_ORDER": return "\uf00d"; // times
            case "VIEW_BILLS": return "\uf0d6"; // money
            case "GENERATE_BILL": return "\uf1c1"; // file
            case "ACCEPT_PAYMENT": return "\uf09d"; // credit card
            case "VIEW_MENU": return "\uf0f5"; // cutlery
            case "VIEW_REPORTS": return "\uf080"; // chart
            case "MANAGE_TABLES": return "\uf013"; // cog
            case "KOT_PRINT": return "\uf02f"; // print
            default: return "\uf111"; // circle
        }
    }

    private String getFeatureIconColor(String featureCode) {
        switch (featureCode) {
            case "VIEW_TABLES": return "#2196F3";
            case "TAKE_ORDER": return "#4CAF50";
            case "VIEW_ORDERS": return "#9C27B0";
            case "EDIT_ORDER": return "#FF9800";
            case "CANCEL_ORDER": return "#f44336";
            case "VIEW_BILLS": return "#009688";
            case "GENERATE_BILL": return "#3F51B5";
            case "ACCEPT_PAYMENT": return "#E91E63";
            case "VIEW_MENU": return "#795548";
            case "VIEW_REPORTS": return "#607D8B";
            case "MANAGE_TABLES": return "#FF5722";
            case "KOT_PRINT": return "#00BCD4";
            default: return "#666";
        }
    }

    private String getFeatureDescription(String featureCode) {
        switch (featureCode) {
            case "VIEW_TABLES": return "View table layout and status";
            case "TAKE_ORDER": return "Add items to table orders";
            case "VIEW_ORDERS": return "View existing orders";
            case "EDIT_ORDER": return "Modify existing orders";
            case "CANCEL_ORDER": return "Cancel orders";
            case "VIEW_BILLS": return "View generated bills";
            case "GENERATE_BILL": return "Create bills for tables";
            case "ACCEPT_PAYMENT": return "Process payments";
            case "VIEW_MENU": return "View menu items and categories";
            case "VIEW_REPORTS": return "Access reports and analytics";
            case "MANAGE_TABLES": return "Add/edit table configuration";
            case "KOT_PRINT": return "Print kitchen order tickets";
            default: return "";
        }
    }

    private void loadSettings() {
        LOG.info("Loading mobile app settings");

        try {
            // Load general settings
            chkMobileAccessEnabled.setSelected(
                    mobileAppSettingService.getSettingBoolean(MobileAppSettingService.MOBILE_ACCESS_ENABLED, false));

            spinnerExpiryDays.getValueFactory().setValue(
                    mobileAppSettingService.getSettingInteger(MobileAppSettingService.JWT_TOKEN_EXPIRY_DAYS, 7));

            spinnerExpiryHours.getValueFactory().setValue(
                    mobileAppSettingService.getSettingInteger(MobileAppSettingService.JWT_TOKEN_EXPIRY_HOURS, 0));

            String appVersion = mobileAppSettingService.getSettingValue(MobileAppSettingService.MOBILE_APP_VERSION);
            txtMinAppVersion.setText(appVersion != null ? appVersion : "1.0.0");

            chkForceUpdate.setSelected(
                    mobileAppSettingService.getSettingBoolean(MobileAppSettingService.FORCE_UPDATE_ENABLED, false));

            // Show masked JWT secret
            String secret = mobileAppSettingService.getJwtSecretKey();
            if (secret != null && secret.length() > 8) {
                lblJwtSecret.setText(secret.substring(0, 8) + "..." + secret.substring(secret.length() - 4));
            }

            // Clear cached checkboxes to reload from DB
            roleFeatureCheckboxes.clear();

            // Select first role by default
            if (!cmbRole.getItems().isEmpty()) {
                cmbRole.getSelectionModel().selectFirst();
                currentSelectedRole = cmbRole.getValue();
                loadFeaturesForRole(currentSelectedRole);
            }

            LOG.info("Mobile app settings loaded successfully");
        } catch (Exception e) {
            LOG.error("Error loading mobile app settings", e);
            alertNotification.showError("Error loading settings: " + e.getMessage());
        }
    }

    private void setupButtons() {
        btnSave.setOnAction(e -> saveSettings());
        btnReset.setOnAction(e -> loadSettings());
        btnRegenerateSecret.setOnAction(e -> regenerateJwtSecret());
    }

    private void saveSettings() {
        LOG.info("Saving mobile app settings");

        try {
            // Save general settings
            mobileAppSettingService.saveSetting(
                    MobileAppSettingService.MOBILE_ACCESS_ENABLED,
                    String.valueOf(chkMobileAccessEnabled.isSelected()),
                    "BOOLEAN",
                    "Enable/disable mobile app access"
            );

            mobileAppSettingService.saveSetting(
                    MobileAppSettingService.JWT_TOKEN_EXPIRY_DAYS,
                    String.valueOf(spinnerExpiryDays.getValue()),
                    "INTEGER",
                    "JWT token expiry in days"
            );

            mobileAppSettingService.saveSetting(
                    MobileAppSettingService.JWT_TOKEN_EXPIRY_HOURS,
                    String.valueOf(spinnerExpiryHours.getValue()),
                    "INTEGER",
                    "JWT token expiry additional hours"
            );

            mobileAppSettingService.saveSetting(
                    MobileAppSettingService.MOBILE_APP_VERSION,
                    txtMinAppVersion.getText().trim(),
                    "STRING",
                    "Minimum mobile app version required"
            );

            mobileAppSettingService.saveSetting(
                    MobileAppSettingService.FORCE_UPDATE_ENABLED,
                    String.valueOf(chkForceUpdate.isSelected()),
                    "BOOLEAN",
                    "Force users to update mobile app"
            );

            // Save feature access for all roles that have been edited
            for (Map.Entry<String, Map<String, CheckBox>> roleEntry : roleFeatureCheckboxes.entrySet()) {
                String role = roleEntry.getKey();
                Map<String, CheckBox> checkboxes = roleEntry.getValue();

                for (Map.Entry<String, CheckBox> featureEntry : checkboxes.entrySet()) {
                    String featureCode = featureEntry.getKey();
                    CheckBox checkBox = featureEntry.getValue();
                    String featureName = MobileAppSettingService.FEATURE_DEFINITIONS.get(featureCode);

                    mobileAppSettingService.saveFeatureAccess(
                            role,
                            featureCode,
                            featureName,
                            checkBox.isSelected()
                    );
                }
            }

            alertNotification.showSuccess("Mobile app settings saved successfully!");
            LOG.info("Mobile app settings saved successfully");

        } catch (Exception e) {
            LOG.error("Error saving mobile app settings", e);
            alertNotification.showError("Error saving settings: " + e.getMessage());
        }
    }

    private void regenerateJwtSecret() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Regenerate JWT Secret");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Regenerating the JWT secret will invalidate all existing mobile app tokens. " +
                "All users will need to login again.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newSecret = UUID.randomUUID().toString() + UUID.randomUUID().toString();
            mobileAppSettingService.saveSetting(
                    MobileAppSettingService.JWT_SECRET_KEY,
                    newSecret,
                    "STRING",
                    "JWT Secret Key for token signing"
            );

            lblJwtSecret.setText(newSecret.substring(0, 8) + "..." + newSecret.substring(newSecret.length() - 4));
            alertNotification.showSuccess("JWT secret regenerated successfully!");
            LOG.info("JWT secret regenerated");
        }
    }
}
