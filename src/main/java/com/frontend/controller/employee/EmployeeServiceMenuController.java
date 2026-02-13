package com.frontend.controller.employee;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.SessionService;
import com.frontend.util.NavigationGuard;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class EmployeeServiceMenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeServiceMenuController.class);

    @Autowired
    SpringFXMLLoader loader;

    @Autowired
    NavigationGuard navigationGuard;

    @FXML
    private Button btnBack;

    @FXML
    private VBox attendanceCard;

    @FXML
    private VBox salaryCard;

    @FXML
    private Label lblAttendanceTitle;

    @FXML
    private Label lblSalaryTitle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupMenuActions();
        setupCardEffects();
        applyCustomFonts();
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
                javafx.scene.Node initialDashboard = (javafx.scene.Node) mainPane.getProperties().get("initialDashboard");

                if (initialDashboard != null) {
                    mainPane.setCenter(initialDashboard);
                    LOG.info("Successfully restored initial dashboard");
                } else {
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
        attendanceCard.setOnMouseClicked(e -> {
            try {
                loadAttendanceManagement();
            } catch (Exception ex) {
                LOG.error("Error loading attendance management: ", ex);
            }
        });

        salaryCard.setOnMouseClicked(e -> {
            try {
                loadSalaryManagement();
            } catch (Exception ex) {
                LOG.error("Error loading salary management: ", ex);
            }
        });
    }

    private void loadAttendanceManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/employee/EmployeeAttendance.fxml");
        }
    }

    private void loadSalaryManagement() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/employee/EmployeeSalary.fxml");
        }
    }

    private void setupCardEffects() {
        addCardHoverEffect(attendanceCard, "rgba(255, 87, 34, 0.4)", "rgba(255, 87, 34, 0.55)");
        addCardHoverEffect(salaryCard, "rgba(46, 125, 50, 0.4)", "rgba(46, 125, 50, 0.55)");
    }

    private void addCardHoverEffect(VBox card, String normalShadowColor, String hoverShadowColor) {
        if (card == null) return;
        String normalEffect = "-fx-effect: dropshadow(three-pass-box, " + normalShadowColor + ", 12, 0, 0, 4);";
        String hoverEffect = "-fx-effect: dropshadow(three-pass-box, " + hoverShadowColor + ", 18, 0, 0, 6);";

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
            String style = card.getStyle().replaceAll("-fx-effect:[^;]+;", hoverEffect);
            card.setStyle(style);
        });

        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            String style = card.getStyle().replaceAll("-fx-effect:[^;]+;", normalEffect);
            card.setStyle(style);
        });

        card.setOnMousePressed(e -> {
            card.setScaleX(0.95);
            card.setScaleY(0.95);
        });

        card.setOnMouseReleased(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) attendanceCard.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    /**
     * Apply custom font from SessionService to card title labels
     */
    private void applyCustomFonts() {
        Font customFont = SessionService.getCustomFont();
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            String fontStyle = "-fx-font-family: '" + fontFamily + "'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;";

            if (lblAttendanceTitle != null) lblAttendanceTitle.setStyle(fontStyle);
            if (lblSalaryTitle != null) lblSalaryTitle.setStyle(fontStyle);

            LOG.info("Applied custom font '{}' to card titles", fontFamily);
        }
    }
}
