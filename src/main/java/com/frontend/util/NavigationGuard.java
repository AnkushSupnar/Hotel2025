package com.frontend.util;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.enums.ScreenPermission;
import com.frontend.service.RoleService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility class to check screen permissions before navigation.
 * Used to enforce role-based access control on screens.
 */
@Component
public class NavigationGuard {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationGuard.class);

    @Autowired
    private RoleService roleService;

    @Autowired
    private AlertNotification alertNotification;

    @Autowired
    private SpringFXMLLoader loader;

    /**
     * Check if current user can access a screen and navigate if allowed.
     * Shows an error notification if access is denied.
     *
     * @param mainPane the BorderPane to set the center content
     * @param fxmlPath the FXML path of the screen to navigate to
     * @return true if navigation was successful, false if blocked
     */
    public boolean navigateWithPermissionCheck(BorderPane mainPane, String fxmlPath) {
        if (!canAccessByPath(fxmlPath)) {
            String screenName = getScreenDisplayName(fxmlPath);
            alertNotification.showError("Access Denied: You don't have permission to access " + screenName);
            LOG.warn("Access denied for user {} (role: {}) to screen {}",
                    SessionService.getCurrentUsername(),
                    SessionService.getCurrentUserRole(),
                    fxmlPath);
            return false;
        }

        try {
            Pane pane = loader.getPage(fxmlPath);
            if (pane != null) {
                mainPane.setCenter(pane);
                LOG.debug("Navigation successful to {}", fxmlPath);
                return true;
            } else {
                alertNotification.showError("Error loading screen");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error loading screen: {}", fxmlPath, e);
            alertNotification.showError("Error loading screen: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user can access a screen by FXML path.
     *
     * @param fxmlPath the FXML path of the screen
     * @return true if the user has access, false otherwise
     */
    public boolean canAccessByPath(String fxmlPath) {
        if (!SessionService.isLoggedIn()) {
            return false;
        }

        String role = SessionService.getCurrentUserRole();

        // ADMIN always has access
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        return roleService.hasScreenAccessByPath(role, fxmlPath);
    }

    /**
     * Check if current user can access a specific screen permission.
     *
     * @param screen the ScreenPermission to check
     * @return true if the user has access, false otherwise
     */
    public boolean canAccess(ScreenPermission screen) {
        if (!SessionService.isLoggedIn()) {
            return false;
        }

        String role = SessionService.getCurrentUserRole();
        return roleService.hasScreenAccess(role, screen);
    }

    /**
     * Get the display name for a screen from its FXML path.
     *
     * @param fxmlPath the FXML path
     * @return the display name or a fallback name
     */
    private String getScreenDisplayName(String fxmlPath) {
        ScreenPermission screen = ScreenPermission.fromFxmlPath(fxmlPath);
        if (screen != null) {
            return screen.getDisplayName();
        }
        // Fallback: extract name from path
        if (fxmlPath != null && fxmlPath.contains("/")) {
            String fileName = fxmlPath.substring(fxmlPath.lastIndexOf("/") + 1);
            return fileName.replace(".fxml", "").replace("Frame", "");
        }
        return "this screen";
    }
}
