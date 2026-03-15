package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class SettingMenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SettingMenuController.class);

    @Autowired
    SpringFXMLLoader loader;

    @Autowired
    AlertNotification alertNotification;

    @FXML
    private Button btnBack;

    @FXML
    private StackPane applicationSettingCard;

    @FXML
    private StackPane userRightsCard;

    @FXML
    private StackPane shopDetailsCard;

    @FXML
    private StackPane sectionSequenceCard;

    @FXML
    private StackPane mobileAppSettingCard;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupMenuActions();
    }

    /**
     * Setup back button to return to initial home dashboard
     */
    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to home dashboard");
                showInitialDashboard();
            } catch (Exception ex) {
                LOG.error("Error returning to home dashboard: ", ex);
            }
        });
    }

    /**
     * Show initial home dashboard with statistics
     */
    private void showInitialDashboard() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                // Get the stored initial dashboard from HomeController
                javafx.scene.Node initialDashboard = (javafx.scene.Node) mainPane.getProperties().get("initialDashboard");

                if (initialDashboard != null) {
                    mainPane.setCenter(initialDashboard);
                    LOG.info("Successfully restored initial dashboard");
                } else {
                    // Fallback: load Home.fxml dashboard page
                    Pane pane = loader.getPage("/fxml/dashboard/Home.fxml");
                    mainPane.setCenter(pane);
                    LOG.info("Loaded dashboard from file");
                }
            }
        } catch (Exception e) {
            LOG.error("Error showing initial dashboard: ", e);
        }
    }

    private void setupMenuActions() {
        // Application Settings
        applicationSettingCard.setOnMouseClicked(e -> {
            try {
                loadApplicationSettings();
            } catch (Exception ex) {
                LOG.error("Error loading application settings: ", ex);
            }
        });

        // User Rights
        userRightsCard.setOnMouseClicked(e -> {
            try {
                // Only ADMIN can access User Rights
                String currentRole = SessionService.getCurrentUserRole();
                if (!"ADMIN".equalsIgnoreCase(currentRole)) {
                    alertNotification.showError("Access Denied: Only Administrators can manage user rights");
                    LOG.warn("User {} with role {} attempted to access User Rights",
                            SessionService.getCurrentUsername(), currentRole);
                    return;
                }
                loadUserRights();
            } catch (Exception ex) {
                LOG.error("Error loading user rights: ", ex);
            }
        });

        // Shop Details
        shopDetailsCard.setOnMouseClicked(e -> {
            try {
                // Only ADMIN can access Shop Details
                String currentRole = SessionService.getCurrentUserRole();
                if (!"ADMIN".equalsIgnoreCase(currentRole)) {
                    alertNotification.showError("Access Denied: Only Administrators can manage shop details");
                    LOG.warn("User {} with role {} attempted to access Shop Details",
                            SessionService.getCurrentUsername(), currentRole);
                    return;
                }
                loadShopDetails();
            } catch (Exception ex) {
                LOG.error("Error loading shop details: ", ex);
            }
        });

        // Section Sequence
        sectionSequenceCard.setOnMouseClicked(e -> {
            try {
                loadSectionSequence();
            } catch (Exception ex) {
                LOG.error("Error loading section sequence: ", ex);
            }
        });

        // Mobile App Settings
        if (mobileAppSettingCard != null) {
            mobileAppSettingCard.setOnMouseClicked(e -> {
                try {
                    // Only ADMIN can access Mobile App Settings
                    String currentRole = SessionService.getCurrentUserRole();
                    if (!"ADMIN".equalsIgnoreCase(currentRole)) {
                        alertNotification.showError("Access Denied: Only Administrators can manage mobile app settings");
                        LOG.warn("User {} with role {} attempted to access Mobile App Settings",
                                SessionService.getCurrentUsername(), currentRole);
                        return;
                    }
                    loadMobileAppSettings();
                } catch (Exception ex) {
                    LOG.error("Error loading mobile app settings: ", ex);
                }
            });
        }
    }

    private void loadApplicationSettings() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/setting/ApplicationSetting.fxml");
            mainPane.setCenter(pane);
            LOG.info("Loaded Application Settings screen");
        }
    }

    private void loadUserRights() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/setting/UserRights.fxml");
            mainPane.setCenter(pane);
            LOG.info("Loaded User Rights screen");
        }
    }

    private void loadShopDetails() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/setting/ShopDetails.fxml");
            mainPane.setCenter(pane);
            LOG.info("Loaded Shop Details screen");
        }
    }

    private void loadSectionSequence() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/setting/SectionSequence.fxml");
            mainPane.setCenter(pane);
            LOG.info("Loaded Table Section Sequence screen");
        }
    }

    private void loadMobileAppSettings() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/setting/MobileAppSetting.fxml");
            mainPane.setCenter(pane);
            LOG.info("Loaded Mobile App Settings screen");
        }
    }

    private BorderPane getMainPane() {
        try {
            // Find the main BorderPane in the parent hierarchy
            return (BorderPane) applicationSettingCard.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }
}
