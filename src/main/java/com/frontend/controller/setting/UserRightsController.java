package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.enums.ScreenPermission;
import com.frontend.service.RoleService;
import com.frontend.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
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

    @FXML
    private TextField txtSearchPermission;

    @FXML
    private VBox noResultsContainer;

    @FXML
    private Label lblNoResults;

    @FXML
    private ScrollPane permissionsScrollPane;

    // Map to track all permission checkboxes
    private Map<ScreenPermission, CheckBox> permissionCheckBoxes = new LinkedHashMap<>();

    // Map to track category "Select All" checkboxes
    private Map<String, CheckBox> categorySelectAllCheckBoxes = new LinkedHashMap<>();

    // Map to track category containers (for show/hide during search)
    private Map<String, VBox> categoryContainers = new LinkedHashMap<>();

    // Map to track checkbox containers for filtering
    private Map<ScreenPermission, HBox> permissionContainers = new LinkedHashMap<>();

    // Map from category to its permissions
    private Map<String, List<ScreenPermission>> categoryPermissions = new LinkedHashMap<>();

    // Current selected role
    private String selectedRole = null;

    // Category order including DASHBOARD
    private static final List<String> CATEGORY_ORDER = Arrays.asList(
            "DASHBOARD", "SALES", "PURCHASE", "MASTER", "REPORTS", "SETTINGS"
    );

    // Category display names
    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = new HashMap<>();
    static {
        CATEGORY_DISPLAY_NAMES.put("DASHBOARD", "Dashboard");
        CATEGORY_DISPLAY_NAMES.put("SALES", "Sales");
        CATEGORY_DISPLAY_NAMES.put("PURCHASE", "Purchase");
        CATEGORY_DISPLAY_NAMES.put("MASTER", "Master Data");
        CATEGORY_DISPLAY_NAMES.put("REPORTS", "Reports");
        CATEGORY_DISPLAY_NAMES.put("SETTINGS", "Settings");
    }

    // Category icons (using glyph names as strings)
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<>();
    static {
        CATEGORY_ICONS.put("DASHBOARD", "DASHBOARD");
        CATEGORY_ICONS.put("SALES", "SHOPPING_CART");
        CATEGORY_ICONS.put("PURCHASE", "TRUCK");
        CATEGORY_ICONS.put("MASTER", "DATABASE");
        CATEGORY_ICONS.put("REPORTS", "BAR_CHART");
        CATEGORY_ICONS.put("SETTINGS", "COGS");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing UserRightsController");
        setupBackButton();
        setupRoleComboBox();
        initializeCategoryPermissions();
        buildPermissionsGrid();
        setupEventHandlers();
        setupSearchFilter();
        updatePermissionCount();
    }

    /**
     * Initialize the category to permissions mapping
     */
    private void initializeCategoryPermissions() {
        for (String category : CATEGORY_ORDER) {
            ScreenPermission[] permissions = ScreenPermission.getByCategory(category);
            categoryPermissions.put(category, Arrays.asList(permissions));
        }
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
        List<String> roles = roleService.getPredefinedRoleNames();
        cmbRole.setItems(FXCollections.observableArrayList(roles));

        cmbRole.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectedRole = newVal;
                        loadRolePermissions(newVal);
                    }
                }
        );

        if (!roles.isEmpty()) {
            cmbRole.getSelectionModel().selectFirst();
        }
    }

    /**
     * Build the permissions grid with categories, select-all checkboxes, and permission checkboxes
     */
    private void buildPermissionsGrid() {
        permissionsContainer.getChildren().clear();
        permissionCheckBoxes.clear();
        categorySelectAllCheckBoxes.clear();
        categoryContainers.clear();
        permissionContainers.clear();

        for (String category : CATEGORY_ORDER) {
            List<ScreenPermission> permissions = categoryPermissions.get(category);
            if (permissions == null || permissions.isEmpty()) continue;

            // Create category container
            VBox categoryBox = createCategorySection(category, permissions);
            categoryContainers.put(category, categoryBox);
            permissionsContainer.getChildren().add(categoryBox);
        }
    }

    /**
     * Create a category section with header (including select-all) and grid of checkboxes
     */
    private VBox createCategorySection(String category, List<ScreenPermission> permissions) {
        VBox categoryBox = new VBox(10);
        categoryBox.getStyleClass().add("category-section");

        // Category Header with Select All
        HBox headerRow = createCategoryHeader(category, permissions);

        // Separator
        Separator separator = new Separator();
        separator.getStyleClass().add("category-separator");

        // FlowPane for checkboxes (responsive grid)
        FlowPane checkboxGrid = new FlowPane();
        checkboxGrid.setHgap(20);
        checkboxGrid.setVgap(12);
        checkboxGrid.setPadding(new Insets(10, 0, 15, 15));
        checkboxGrid.setPrefWrapLength(500); // Allows 2-3 columns depending on width
        checkboxGrid.getStyleClass().add("checkbox-grid");

        for (ScreenPermission perm : permissions) {
            HBox checkboxContainer = createPermissionCheckbox(perm);
            checkboxGrid.getChildren().add(checkboxContainer);
        }

        categoryBox.getChildren().addAll(headerRow, separator, checkboxGrid);
        return categoryBox;
    }

    /**
     * Create category header with icon, name, and select-all checkbox
     */
    private HBox createCategoryHeader(String category, List<ScreenPermission> permissions) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("category-header-row");

        // Category icon
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName(CATEGORY_ICONS.getOrDefault(category, "FOLDER"));
        icon.setSize("1.3em");
        icon.getStyleClass().add("category-icon");

        // Category label
        Label categoryLabel = new Label(CATEGORY_DISPLAY_NAMES.getOrDefault(category, category));
        categoryLabel.getStyleClass().add("category-header");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Select All checkbox
        CheckBox selectAllCb = new CheckBox("Select All");
        selectAllCb.getStyleClass().addAll("material-checkbox", "select-all-checkbox");

        // Handle Select All click
        selectAllCb.setOnAction(e -> {
            boolean selected = selectAllCb.isSelected();
            selectAllCb.setIndeterminate(false);
            List<ScreenPermission> categoryPerms = categoryPermissions.get(category);
            if (categoryPerms != null) {
                for (ScreenPermission perm : categoryPerms) {
                    CheckBox cb = permissionCheckBoxes.get(perm);
                    if (cb != null && !cb.isDisabled()) {
                        cb.setSelected(selected);
                    }
                }
            }
            updatePermissionCount();
        });

        categorySelectAllCheckBoxes.put(category, selectAllCb);

        header.getChildren().addAll(icon, categoryLabel, spacer, selectAllCb);
        return header;
    }

    /**
     * Create a permission checkbox with tooltip
     */
    private HBox createPermissionCheckbox(ScreenPermission perm) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("permission-checkbox-container");
        container.setMinWidth(200);
        container.setPrefWidth(220);

        CheckBox cb = new CheckBox(perm.getDisplayName());
        cb.getStyleClass().add("material-checkbox");

        // Add tooltip with description
        String description = perm.getDescription();
        if (description != null && !description.isEmpty()) {
            Tooltip tooltip = new Tooltip(description);
            tooltip.setShowDelay(Duration.millis(300));
            tooltip.setHideDelay(Duration.millis(100));
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(300);
            cb.setTooltip(tooltip);
        }

        // Add listener to update count and select-all state
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updatePermissionCount();
            updateSelectAllState(perm.getCategory());
        });

        permissionCheckBoxes.put(perm, cb);
        permissionContainers.put(perm, container);
        container.getChildren().add(cb);
        return container;
    }

    /**
     * Update the select-all checkbox state based on individual checkboxes
     */
    private void updateSelectAllState(String category) {
        CheckBox selectAllCb = categorySelectAllCheckBoxes.get(category);
        if (selectAllCb == null || selectAllCb.isDisabled()) return;

        List<ScreenPermission> permissions = categoryPermissions.get(category);
        if (permissions == null || permissions.isEmpty()) return;

        long selectedCount = permissions.stream()
                .map(permissionCheckBoxes::get)
                .filter(Objects::nonNull)
                .filter(cb -> !cb.isDisabled())
                .filter(CheckBox::isSelected)
                .count();

        long enabledCount = permissions.stream()
                .map(permissionCheckBoxes::get)
                .filter(Objects::nonNull)
                .filter(cb -> !cb.isDisabled())
                .count();

        if (selectedCount == 0) {
            selectAllCb.setSelected(false);
            selectAllCb.setIndeterminate(false);
        } else if (selectedCount == enabledCount) {
            selectAllCb.setSelected(true);
            selectAllCb.setIndeterminate(false);
        } else {
            selectAllCb.setIndeterminate(true);
        }
    }

    /**
     * Setup search/filter functionality
     */
    private void setupSearchFilter() {
        if (txtSearchPermission != null) {
            txtSearchPermission.textProperty().addListener((obs, oldVal, newVal) -> {
                filterPermissions(newVal);
            });
        }
    }

    /**
     * Filter permissions based on search text
     */
    private void filterPermissions(String searchText) {
        String lowerSearch = searchText == null ? "" : searchText.toLowerCase().trim();
        int visibleCount = 0;

        for (String category : CATEGORY_ORDER) {
            VBox categoryBox = categoryContainers.get(category);
            if (categoryBox == null) continue;

            List<ScreenPermission> permissions = categoryPermissions.get(category);
            if (permissions == null) continue;

            int categoryVisibleCount = 0;

            for (ScreenPermission perm : permissions) {
                HBox checkboxContainer = permissionContainers.get(perm);
                if (checkboxContainer == null) continue;

                boolean matches = lowerSearch.isEmpty() ||
                        perm.getDisplayName().toLowerCase().contains(lowerSearch) ||
                        (perm.getDescription() != null &&
                                perm.getDescription().toLowerCase().contains(lowerSearch));

                checkboxContainer.setVisible(matches);
                checkboxContainer.setManaged(matches);

                if (matches) {
                    categoryVisibleCount++;
                    visibleCount++;
                }
            }

            // Hide entire category if no permissions match
            boolean categoryVisible = categoryVisibleCount > 0;
            categoryBox.setVisible(categoryVisible);
            categoryBox.setManaged(categoryVisible);
        }

        // Show/hide no results message
        boolean hasResults = visibleCount > 0;
        if (noResultsContainer != null) {
            noResultsContainer.setVisible(!hasResults && !lowerSearch.isEmpty());
            noResultsContainer.setManaged(!hasResults && !lowerSearch.isEmpty());
        }
        if (permissionsScrollPane != null) {
            permissionsScrollPane.setVisible(hasResults || lowerSearch.isEmpty());
            permissionsScrollPane.setManaged(hasResults || lowerSearch.isEmpty());
        }

        if (!hasResults && !lowerSearch.isEmpty() && lblNoResults != null) {
            lblNoResults.setText("No permissions matching \"" + searchText + "\"");
        }
    }

    private void loadRolePermissions(String roleName) {
        LOG.info("Loading permissions for role: {}", roleName);

        // Clear search when loading new role
        if (txtSearchPermission != null) {
            txtSearchPermission.clear();
        }

        updateRoleInfo(roleName);

        // Handle ADMIN specially - all permissions, disabled
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            permissionCheckBoxes.values().forEach(cb -> {
                cb.setSelected(true);
                cb.setDisable(true);
            });
            categorySelectAllCheckBoxes.values().forEach(cb -> {
                cb.setSelected(true);
                cb.setDisable(true);
                cb.setIndeterminate(false);
            });
            btnSave.setDisable(true);
            updatePermissionCount();
            return;
        }

        // Enable all checkboxes and save button for non-admin roles
        permissionCheckBoxes.values().forEach(cb -> cb.setDisable(false));
        categorySelectAllCheckBoxes.values().forEach(cb -> cb.setDisable(false));
        btnSave.setDisable(false);

        // Get current permissions for the role
        Set<ScreenPermission> currentPermissions = roleService.getScreenPermissions(roleName);

        // Update checkbox states
        permissionCheckBoxes.forEach((perm, cb) -> {
            cb.setSelected(currentPermissions.contains(perm));
        });

        // Update all select-all checkbox states
        for (String category : CATEGORY_ORDER) {
            updateSelectAllState(category);
        }

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
            Set<ScreenPermission> selectedPermissions = permissionCheckBoxes.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

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
                roleService.initializeDefaultRolePermissions();
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
