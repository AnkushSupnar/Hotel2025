package com.frontend.controller.report;

import com.frontend.config.SpringFXMLLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class PurchaseReportController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseReportController.class);

    @Autowired
    SpringFXMLLoader loader;

    @FXML
    private Button btnBack;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
    }

    /**
     * Setup back button to return to report menu
     */
    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to report menu");
                goBackToReportMenu();
            } catch (Exception ex) {
                LOG.error("Error returning to report menu: ", ex);
            }
        });
    }

    /**
     * Navigate back to Report Menu
     */
    private void goBackToReportMenu() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/report/ReportMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Returned to report menu");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to report menu: ", e);
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
}
