package com.frontend.controller.setting;

import com.frontend.config.SpringFXMLLoader;
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

    @FXML
    private Button btnBack;

    @FXML
    private StackPane applicationSettingCard;

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
    }

    private void loadApplicationSettings() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            Pane pane = loader.getPage("/fxml/setting/ApplicationSetting.fxml");
            mainPane.setCenter(pane);
            LOG.info("Loaded Application Settings screen");
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
