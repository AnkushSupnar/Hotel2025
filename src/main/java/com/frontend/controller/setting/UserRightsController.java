package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.enums.ScreenPermission;
import com.frontend.service.RoleService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the User Rights Management screen.
 * Allows administrators to configure screen-level permissions per role.
 */
@Component
public class UserRightsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(UserRightsController.class);

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AlertNotification alertNotification;

    @FXML
    private Button btnBack;

    @FXML
    private ComboBox<String> cmbRole;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnReset;

    @FXML
    private Label lblRoleInfo;

    @FXML
    private Label lblPermissionCount;

    @FXML
    private VBox permissionsContainer;

    // Map to track all permission checkboxes
    private Map<ScreenPermission, CheckBox> permissionCheckBoxes = new LinkedHashMap<>();

    // Current selected role
    private String selectedRole = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing UserRightsController");
        setupBackButton();
        setupRoleComboBox();
        buildPermissionsGrid();
        setupEventHandlers();
        updatePermissionCount();
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
            return (BorderPane) btnBack.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupRoleComboBox() {
        // Get predefined roles
        List<String> roles = roleService.getPredefinedRoleNames();
        cmbRole.setItems(FXCollections.observableArrayList(roles));

        // Handle role selection change
        cmbRole.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectedRole = newVal;
                        loadRolePermissions(newVal);
                    }
                }
        );

        // Select first role by default
        if (!roles.isEmpty()) {
            cmbRole.getSelectionModel().selectFirst();
        }
    }

    private void buildPermissionsGrid() {
        permissionsContainer.getChildren().clear();
        permissionCheckBoxes.clear();

        // Define category order
        List<String> categoryOrder = Arrays.asList("SALES", "PURCHASE", "MASTER", "REPORTS", "SETTINGS");

        // Category display names
        Map<String, String> categoryDisplayNames = new HashMap<>();
        categoryDisplayNames.put("SALES", "Sales");
        categoryDisplayNames.put("PURCHASE", "Purchase");
        categoryDisplayNames.put("MASTER", "Master Data");
        categoryDisplayNames.put("REPORTS", "Reports");
        categoryDisplayNames.put("SETTINGS", "Settings");

        for (String category : categoryOrder) {
            ScreenPermission[] permissions = ScreenPermission.getByCategory(category);
            if (permissions.length == 0) continue;

            // Category Header
            Label categoryLabel = new Label(categoryDisplayNames.getOrDefault(category, category));
            categoryLabel.getStyleClass().add("category-header");
            categoryLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #5e35b1; -fx-padding: 5 0 5 0;");

            // Separator
            Separator separator = new Separator();
            separator.setStyle("-fx-background-color: #e0e0e0;");

            // Checkboxes container for this category
            VBox categoryBox = new VBox(10);
            categoryBox.setPadding(new Insets(10, 0, 15, 15));

            for (ScreenPermission perm : permissions) {
                CheckBox cb = new CheckBox(perm.getDisplayName());
                cb.getStyleClass().add("material-checkbox");
                cb.setStyle("-fx-font-size: 14px; -fx-text-fill: #424242;");

                // Add listener to update count
                cb.selectedProperty().addListener((obs, oldVal, newVal) -> updatePermissionCount());

                permissionCheckBoxes.put(perm, cb);
                categoryBox.getChildren().add(cb);
            }

            permissionsContainer.getChildren().addAll(categoryLabel, separator, categoryBox);
        }
    }

    private void loadRolePermissions(String roleName) {
        LOG.info("Loading permissions for role: {}", roleName);

        // Update role info
        updateRoleInfo(roleName);

        // Handle ADMIN specially - all permissions, disabled
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            permissionCheckBoxes.values().forEach(cb -> {
                cb.setSelected(true);
                cb.setDisable(true);
            });
            btnSave.setDisable(true);
            return;
        }

        // Enable all checkboxes and save button for non-admin roles
        permissionCheckBoxes.values().forEach(cb -> cb.setDisable(false));
        btnSave.setDisable(false);

        // Get current permissions for the role
        Set<ScreenPermission> currentPermissions = roleService.getScreenPermissions(roleName);

        // Update checkbox states
        permissionCheckBoxes.forEach((perm, cb) -> {
            cb.setSelected(currentPermissions.contains(perm));
        });

        updatePermissionCount();
    }

    private void updateRoleInfo(String roleName) {
        String info;
        switch (roleName.toUpperCase()) {
            case "ADMIN":
                info = "Administrator has full access to all screens. Permissions cannot be modified.";
                break;
            case "MANAGER":
                info = "Manager role - typically has access to most operations except user rights management.";
                break;
            case "CASHIER":
                info = "Cashier role - focused on billing and payment operations.";
                break;
            case "WAITER":
                info = "Waiter role - primarily for taking orders and basic billing access.";
                break;
            case "USER":
                info = "Standard user role - limited access based on assigned permissions.";
                break;
            default:
                info = "Custom role - permissions can be configured as needed.";
        }
        lblRoleInfo.setText(info);
    }

    private void updatePermissionCount() {
        long selected = permissionCheckBoxes.values().stream()
                .filter(CheckBox::isSelected)
                .count();
        int total = permissionCheckBoxes.size();
        lblPermissionCount.setText(selected + " of " + total + " selected");
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> savePermissions());
        btnReset.setOnAction(e -> resetToDefault());
    }

    private void savePermissions() {
        if (selectedRole == null) {
            alertNotification.showError("Please select a role first");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selectedRole)) {
            alertNotification.showInfo("Administrator permissions cannot be modified");
            return;
        }

        try {
            // Collect selected permissions
            Set<ScreenPermission> selectedPermissions = permissionCheckBoxes.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // Save to database
            roleService.updateScreenPermissions(selectedRole, selectedPermissions);

            alertNotification.showSuccess("Permissions saved successfully for " + selectedRole);
            LOG.info("Saved {} permissions for role {}", selectedPermissions.size(), selectedRole);

        } catch (Exception e) {
            LOG.error("Error saving permissions: ", e);
            alertNotification.showError("Error saving permissions: " + e.getMessage());
        }
    }

    private void resetToDefault() {
        if (selectedRole == null) {
            alertNotification.showError("Please select a role first");
            return;
        }

        boolean confirm = alertNotification.showConfirmation(
                "Reset Permissions",
                "Reset permissions for " + selectedRole + " to default values?\n\nThis will overwrite current settings."
        );

        if (confirm) {
            try {
                // Reinitialize default permissions
                roleService.initializeDefaultRolePermissions();

                // Reload the current role's permissions
                loadRolePermissions(selectedRole);

                alertNotification.showSuccess("Permissions reset to default for " + selectedRole);
                LOG.info("Reset permissions to default for role: {}", selectedRole);

            } catch (Exception e) {
                LOG.error("Error resetting permissions: ", e);
                alertNotification.showError("Error resetting permissions: " + e.getMessage());
            }
        }
    }
}
