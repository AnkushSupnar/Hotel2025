package com.frontend.controller.demo;

import com.frontend.service.SessionService;
import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import io.github.palexdev.materialfx.controls.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Component
public class MaterialFXDemoController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MaterialFXDemoController.class);

    @Autowired
    @Lazy
    private StageManager stageManager;

    @FXML
    private MFXButton btnPrimary;

    @FXML
    private MFXButton btnSecondary;

    @FXML
    private MFXButton btnSuccess;

    @FXML
    private MFXButton btnDanger;

    @FXML
    private MFXButton btnBackToHome;

    @FXML
    private MFXTextField txtName;

    @FXML
    private MFXTextField txtEmail;

    @FXML
    private MFXPasswordField txtPassword;

    @FXML
    private MFXComboBox<String> cmbCategory;

    @FXML
    private MFXFilterComboBox<String> cmbFilterCategory;

    @FXML
    private MFXCheckbox chkAgree;

    @FXML
    private MFXRadioButton radioMale;

    @FXML
    private MFXRadioButton radioFemale;

    @FXML
    private MFXToggleButton toggleSwitch;

    @FXML
    private MFXDatePicker datePicker;

    @FXML
    private MFXProgressSpinner progressSpinner;

    @FXML
    private MFXSlider slider;

    @FXML
    private Label lblSliderValue;

    @FXML
    private Label lblStatusMessage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("MaterialFX Demo screen initialized");

        setupCustomFont();
        setupButtons();
        setupTextFields();
        setupComboBoxes();
        setupCheckAndRadio();
        setupDatePicker();
        setupSlider();
        setupProgressSpinner();
    }

    /**
     * Setup custom Kiran font for Marathi text support
     */
    private void setupCustomFont() {
        try {
            // Get Kiran font at size 20 for text fields
            Font kiranFont20 = SessionService.getCustomFont(20.0);
            Font kiranFont16 = SessionService.getCustomFont(16.0);

            if (kiranFont20 != null && kiranFont16 != null) {
                String fontFamily = kiranFont20.getFamily();

                // Apply Kiran font to text fields with inline style for persistence
                String textFieldStyle = String.format(
                        "-fx-font-family: '%s'; -fx-font-size: 20px;",
                        fontFamily);

                txtName.setStyle(textFieldStyle);
                txtEmail.setStyle(textFieldStyle);
                txtPassword.setStyle(textFieldStyle);

                // Apply to ComboBoxes
                String comboBoxStyle = String.format(
                        "-fx-font-family: '%s'; -fx-font-size: 18px;",
                        fontFamily);

                cmbCategory.setStyle(comboBoxStyle);
                cmbFilterCategory.setStyle(comboBoxStyle);

                // Update status message label with custom font
                lblStatusMessage.setFont(kiranFont16);

                // Update floating text prompts to Marathi
                txtName.setFloatingText("तुमचे नाव"); // Your name
                txtEmail.setFloatingText("ईमेल"); // Email
                txtPassword.setFloatingText("पासवर्ड"); // Password
                cmbCategory.setFloatingText("श्रेणी निवडा"); // Select category
                cmbFilterCategory.setFloatingText("वस्तू शोधा..."); // Search items

                // Update checkbox and radio button text
                chkAgree.setText("मी अटी व शर्तींशी सहमत आहे"); // I agree to terms and conditions
                radioMale.setText("पुरुष"); // Male
                radioFemale.setText("स्त्री"); // Female

                // Update labels
                lblSliderValue.setFont(kiranFont16);

                LOG.info("Custom Kiran font applied to MaterialFX components successfully");
            } else {
                LOG.warn("Kiran font not available, using default fonts");
            }
        } catch (Exception e) {
            LOG.error("Error applying custom font to MaterialFX demo: ", e);
        }
    }

    /**
     * Setup button actions
     */
    private void setupButtons() {
        btnPrimary.setOnAction(e -> {
            lblStatusMessage.setText("Primary button clicked!");
            lblStatusMessage.setStyle("-fx-text-fill: #1976D2;");
            LOG.info("Primary button clicked");
        });

        btnSecondary.setOnAction(e -> {
            lblStatusMessage.setText("Secondary button clicked!");
            lblStatusMessage.setStyle("-fx-text-fill: #757575;");
            LOG.info("Secondary button clicked");
        });

        btnSuccess.setOnAction(e -> {
            lblStatusMessage.setText("Success! Operation completed.");
            lblStatusMessage.setStyle("-fx-text-fill: #4CAF50;");
            LOG.info("Success button clicked");
        });

        btnDanger.setOnAction(e -> {
            lblStatusMessage.setText("Danger! This action is destructive.");
            lblStatusMessage.setStyle("-fx-text-fill: #F44336;");
            LOG.info("Danger button clicked");
        });

        btnBackToHome.setOnAction(e -> {
            LOG.info("Navigating back to Home screen");
            stageManager.switchScene(FxmlView.HOME);
        });
    }

    /**
     * Setup text fields with floating labels
     */
    private void setupTextFields() {
        // Text fields are configured in FXML with floating text
        txtName.textProperty().addListener((obs, oldVal, newVal) -> {
            LOG.debug("Name changed: {}", newVal);
        });

        txtEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            // Simple email validation
            if (newVal.contains("@")) {
                txtEmail.setStyle("-fx-border-color: #4CAF50;");
            } else if (!newVal.isEmpty()) {
                txtEmail.setStyle("-fx-border-color: #F44336;");
            }
        });
    }

    /**
     * Setup combo boxes with sample data (Marathi + English)
     */
    private void setupComboBoxes() {
        // Regular ComboBox with Marathi categories
        cmbCategory.getItems().addAll(
                "स्टार्टर (Starter)",
                "मुख्य पदार्थ (Main Course)",
                "मिठाई (Dessert)",
                "पेय पदार्थ (Beverages)",
                "स्नॅक्स (Snacks)");
        cmbCategory.setOnAction(e -> {
            String selected = cmbCategory.getSelectedItem();
            if (selected != null) {
                lblStatusMessage.setText("निवडलेली श्रेणी: " + selected); // Selected category
                lblStatusMessage.setStyle("-fx-text-fill: #1976D2;");
            }
        });

        // FilterComboBox with search functionality - Marathi dish names
        cmbFilterCategory.getItems().addAll(
                "पनीर टिक्का (Paneer Tikka)",
                "बटर चिकन (Butter Chicken)",
                "दाल मखनी (Dal Makhani)",
                "नान (Naan)",
                "तांदूळ (Rice)",
                "बिर्याणी (Biryani)",
                "तंदूरी चिकन (Tandoori Chicken)",
                "गुलाब जामुन (Gulab Jamun)",
                "रसमलाई (Rasmalai)",
                "लस्सी (Lassi)",
                "मसाला चहा (Masala Chai)",
                "कॉफी (Coffee)",
                "कोल्ड ड्रिंक (Cold Drink)",
                "पाण्याची बाटली (Water Bottle)",
                "सॅलड (Salad)");
        cmbFilterCategory.setOnAction(e -> {
            String selected = cmbFilterCategory.getSelectedItem();
            if (selected != null) {
                lblStatusMessage.setText("निवडलेली वस्तू: " + selected); // Selected item
                lblStatusMessage.setStyle("-fx-text-fill: #1976D2;");
            }
        });
    }

    /**
     * Setup checkboxes and radio buttons
     */
    private void setupCheckAndRadio() {
        chkAgree.selectedProperty().addListener((obs, oldVal, newVal) -> {
            lblStatusMessage.setText(newVal ? "Terms accepted" : "Terms not accepted");
            lblStatusMessage.setStyle("-fx-text-fill: " + (newVal ? "#4CAF50" : "#F44336") + ";");
        });

        radioMale.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                lblStatusMessage.setText("Gender: Male");
                lblStatusMessage.setStyle("-fx-text-fill: #1976D2;");
            }
        });

        radioFemale.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                lblStatusMessage.setText("Gender: Female");
                lblStatusMessage.setStyle("-fx-text-fill: #1976D2;");
            }
        });

        toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
            lblStatusMessage.setText("Switch is " + (newVal ? "ON" : "OFF"));
            lblStatusMessage.setStyle("-fx-text-fill: " + (newVal ? "#4CAF50" : "#757575") + ";");
        });
    }

    /**
     * Setup date picker
     */
    private void setupDatePicker() {
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblStatusMessage.setText("Selected date: " + newVal);
                lblStatusMessage.setStyle("-fx-text-fill: #1976D2;");
            }
        });
    }

    /**
     * Setup slider with value display
     */
    private void setupSlider() {
        slider.setValue(50);
        lblSliderValue.setText("50");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblSliderValue.setText(String.format("%.0f", newVal.doubleValue()));
        });
    }

    /**
     * Setup progress spinner
     */
    private void setupProgressSpinner() {
        // Progress spinner is configured in FXML
        // It will show a continuous loading animation
    }

    /**
     * Handle form submission
     */
    @FXML
    private void handleSubmit() {
        String name = txtName.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String category = cmbCategory.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            lblStatusMessage.setText("Please fill all required fields!");
            lblStatusMessage.setStyle("-fx-text-fill: #F44336;");
            return;
        }

        lblStatusMessage.setText("Form submitted successfully! Name: " + name);
        lblStatusMessage.setStyle("-fx-text-fill: #4CAF50;");
        LOG.info("Form submitted - Name: {}, Email: {}, Category: {}", name, email, category);
    }

    /**
     * Clear form fields
     */
    @FXML
    private void handleClear() {
        txtName.clear();
        txtEmail.clear();
        txtPassword.clear();
        cmbCategory.clearSelection();
        cmbFilterCategory.clearSelection();
        chkAgree.setSelected(false);
        radioMale.setSelected(false);
        radioFemale.setSelected(false);
        toggleSwitch.setSelected(false);
        slider.setValue(50);
        datePicker.setValue(LocalDate.now());

        lblStatusMessage.setText("Form cleared");
        lblStatusMessage.setStyle("-fx-text-fill: #757575;");
        LOG.info("Form cleared");
    }
}
