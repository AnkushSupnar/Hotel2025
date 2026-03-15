package com.frontend.controller.setting;

import com.frontend.service.MobileAppSettingService;
import com.frontend.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
    // Category select-all checkboxes per role
    private Map<String, Map<String, CheckBox>> roleCategorySelectAll = new HashMap<>();
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
        Map<String, CheckBox> categorySelectAlls = roleCategorySelectAll.computeIfAbsent(role, k -> new HashMap<>());

        // Get features grouped by category
        Map<String, List<Map.Entry<String, String>>> featuresByCategory = MobileAppSettingService.getFeaturesByCategory();

        int totalFeatures = 0;

        for (String category : MobileAppSettingService.CATEGORY_ORDER) {
            List<Map.Entry<String, String>> categoryFeatures = featuresByCategory.get(category);
            if (categoryFeatures == null || categoryFeatures.isEmpty()) continue;

            // Category section container
            VBox categorySection = new VBox(8);
            categorySection.setStyle("-fx-padding: 10 0 5 0;");

            // Category header with icon and select-all
            HBox categoryHeader = new HBox(12);
            categoryHeader.setAlignment(Pos.CENTER_LEFT);
            categoryHeader.setStyle("-fx-padding: 8 15; -fx-background-color: linear-gradient(to right, #667eea22, #764ba222); -fx-background-radius: 8;");

            FontAwesomeIcon categoryIcon = new FontAwesomeIcon();
            categoryIcon.setGlyphName(getCategoryIconName(category));
            categoryIcon.setSize("1.3em");
            categoryIcon.setFill(javafx.scene.paint.Color.web(getCategoryColor(category)));

            Label categoryLabel = new Label(category);
            categoryLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #444;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Select All checkbox for this category
            CheckBox selectAllCb = categorySelectAlls.computeIfAbsent(category, k -> new CheckBox("Select All"));
            selectAllCb.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            // Handle Select All click
            final String cat = category;
            selectAllCb.setOnAction(e -> {
                boolean selected = selectAllCb.isSelected();
                selectAllCb.setIndeterminate(false);
                List<Map.Entry<String, String>> catFeatures = featuresByCategory.get(cat);
                if (catFeatures != null) {
                    for (Map.Entry<String, String> f : catFeatures) {
                        CheckBox cb = checkboxes.get(f.getKey());
                        if (cb != null) cb.setSelected(selected);
                    }
                }
            });

            categoryHeader.getChildren().addAll(categoryIcon, categoryLabel, spacer, selectAllCb);
            categorySection.getChildren().add(categoryHeader);

            // Separator
            Separator separator = new Separator();
            separator.setStyle("-fx-padding: 0 0 5 0;");
            categorySection.getChildren().add(separator);

            // Feature rows for this category
            for (Map.Entry<String, String> feature : categoryFeatures) {
                String featureCode = feature.getKey();
                String featureName = feature.getValue();

                // Get or create checkbox
                CheckBox checkBox;
                if (checkboxes.containsKey(featureCode)) {
                    checkBox = checkboxes.get(featureCode);
                } else {
                    checkBox = new CheckBox();
                    checkBox.setStyle("-fx-font-size: 14px;");

                    boolean isEnabled = mobileAppSettingService.isFeatureEnabledForRole(role, featureCode);
                    checkBox.setSelected(isEnabled);

                    // Listen for changes to update select-all state
                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        updateCategorySelectAllState(cat, checkboxes, categorySelectAlls.get(cat), featuresByCategory.get(cat));
                    });

                    checkboxes.put(featureCode, checkBox);
                }

                // Create feature row
                HBox featureRow = new HBox(15);
                featureRow.setAlignment(Pos.CENTER_LEFT);
                featureRow.setStyle("-fx-padding: 8 15; -fx-background-color: #f9f9f9; -fx-background-radius: 5;");

                String iconColor = getFeatureIconColor(featureCode);
                Label iconLabel = new Label(getFeatureIcon(featureCode));
                iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + iconColor + ";");

                VBox featureInfo = new VBox(2);
                Label nameLabel = new Label(featureName);
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
                Label descLabel = new Label(getFeatureDescription(featureCode));
                descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
                featureInfo.getChildren().addAll(nameLabel, descLabel);

                Region rowSpacer = new Region();
                HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                featureRow.getChildren().addAll(iconLabel, featureInfo, rowSpacer, checkBox);
                categorySection.getChildren().add(featureRow);
                totalFeatures++;
            }

            // Update select-all state for this category
            updateCategorySelectAllState(category, checkboxes, selectAllCb, categoryFeatures);

            vboxFeatures.getChildren().add(categorySection);
        }

        LOG.info("Loaded {} features in {} categories for role: {}", totalFeatures, featuresByCategory.size(), role);
    }

    /**
     * Update category select-all checkbox state based on individual feature checkboxes
     */
    private void updateCategorySelectAllState(String category, Map<String, CheckBox> checkboxes,
                                               CheckBox selectAllCb, List<Map.Entry<String, String>> categoryFeatures) {
        if (selectAllCb == null || categoryFeatures == null) return;

        long selectedCount = categoryFeatures.stream()
                .map(f -> checkboxes.get(f.getKey()))
                .filter(Objects::nonNull)
                .filter(CheckBox::isSelected)
                .count();

        long totalCount = categoryFeatures.size();

        if (selectedCount == 0) {
            selectAllCb.setSelected(false);
            selectAllCb.setIndeterminate(false);
        } else if (selectedCount == totalCount) {
            selectAllCb.setSelected(true);
            selectAllCb.setIndeterminate(false);
        } else {
            selectAllCb.setIndeterminate(true);
        }
    }

    private String getFeatureIcon(String featureCode) {
        switch (featureCode) {
            case "dashboard": return "\uf0e4"; // dashboard
            case "tables_overview": return "\uf0ce"; // table
            case "orders_overview": return "\uf03a"; // list
            case "menu_items": return "\uf0f5"; // cutlery
            case "kitchen_overview": return "\uf2dc"; // kitchen/fire
            case "sync_data": return "\uf021"; // refresh/sync
            case "reports": return "\uf080"; // chart
            case "favorite_tables": return "\uf005"; // star
            case "favorite_items": return "\uf004"; // heart
            case "table_selection": return "\uf245"; // mouse pointer/select
            case "order_page": return "\uf07a"; // shopping cart
            case "kitchen_page": return "\uf0f5"; // cutlery
            case "bill_preview": return "\uf0d6"; // money/bill
            default: return "\uf111"; // circle
        }
    }

    private String getFeatureIconColor(String featureCode) {
        switch (featureCode) {
            case "dashboard": return "#2196F3";
            case "tables_overview": return "#4CAF50";
            case "orders_overview": return "#9C27B0";
            case "menu_items": return "#FF9800";
            case "kitchen_overview": return "#f44336";
            case "sync_data": return "#009688";
            case "reports": return "#607D8B";
            case "favorite_tables": return "#FFC107";
            case "favorite_items": return "#E91E63";
            case "table_selection": return "#3F51B5";
            case "order_page": return "#795548";
            case "kitchen_page": return "#FF5722";
            case "bill_preview": return "#00BCD4";
            default: return "#666";
        }
    }

    private String getFeatureDescription(String featureCode) {
        switch (featureCode) {
            case "dashboard": return "Main dashboard with overview and stats";
            case "tables_overview": return "View all tables and their status";
            case "orders_overview": return "View all active and past orders";
            case "menu_items": return "Browse menu items and categories";
            case "kitchen_overview": return "Kitchen display with order queue";
            case "sync_data": return "Sync data with server";
            case "reports": return "Access reports and analytics";
            case "favorite_tables": return "Manage favorite tables list";
            case "favorite_items": return "Manage favorite menu items";
            case "table_selection": return "Select table for new order";
            case "order_page": return "Create and manage order details";
            case "kitchen_page": return "Kitchen order preparation view";
            case "bill_preview": return "Preview and print bill";
            default: return "";
        }
    }

    private String getCategoryIconName(String category) {
        switch (category) {
            case "Main Menu": return "TH_LARGE";
            case "Settings": return "COGS";
            case "Sub-Screen": return "WINDOW_RESTORE";
            default: return "FOLDER";
        }
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "Main Menu": return "#667eea";
            case "Settings": return "#764ba2";
            case "Sub-Screen": return "#f093fb";
            default: return "#666";
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
            roleCategorySelectAll.clear();

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
