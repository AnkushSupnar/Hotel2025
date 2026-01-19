package com.frontend.controller;

import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.Shop;
import com.frontend.entity.User;
import com.frontend.repository.UserRepository;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.AuthApiService;
import com.frontend.service.SessionService;
import com.frontend.service.ShopService;
import com.frontend.view.AlertNotification;
import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@Component
public class LoginController {
    @FXML
    private Button btnCancel;

    @FXML
    private Button btnLogin;

    @FXML
    private ComboBox<Shop> cmbShop;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtUserName;

    @FXML
    private TextField txtServerUrl;

    @FXML
    private javafx.scene.control.Label lblSupport;

    @FXML
    private javafx.scene.control.Hyperlink linkRegister;

    @Autowired
    @Lazy
    StageManager stageManager;

    @Autowired
    AlertNotification alertNotification;

    @Autowired
    AuthApiService authApiService;

    @Autowired
    SessionService sessionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ShopService shopService;

    @Autowired
    ApplicationSettingService applicationSettingService;

    // Custom font for username field
    private Font customFont;
    private AutoCompleteTextField autoCompleteTextField;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(event -> login());
        btnCancel.setOnAction(event -> closeApplication());

        // Add Enter key action on password field to trigger login
        txtPassword.setOnAction(event -> login());

        // Note: Username field Enter key is handled by AutoCompleteTextField
        // which moves focus to password field after selection

        // Hide server URL field since we're using database now
        if (txtServerUrl != null) {
            txtServerUrl.setVisible(false);
            txtServerUrl.setManaged(false);
        }

        // Load custom font first
        loadCustomFont();

        // Initialize shop dropdown
        initializeShopDropdown();

        // Initialize autocomplete for username field and check for first-time setup
        initializeUsernameAutocomplete();

        // Setup registration link handler
        if (linkRegister != null) {
            linkRegister.setOnAction(event -> openRegistrationScreen());
        }
    }

    /**
     * Load custom font from application settings
     */
    private void loadCustomFont() {
        try {
            // First try to get font path from database settings
            String fontPath = null;
            try {
                ApplicationSetting fontSetting = applicationSettingService.getSettingByName("input_font_path").orElse(null);
                if (fontSetting != null && fontSetting.getSettingValue() != null) {
                    fontPath = fontSetting.getSettingValue();
                }
            } catch (Exception e) {
                System.err.println("Error getting font setting from database: " + e.getMessage());
            }

            // Try loading from external path
            if (fontPath != null && !fontPath.trim().isEmpty()) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    try (FileInputStream fontStream = new FileInputStream(fontFile)) {
                        customFont = Font.loadFont(fontStream, 20);
                        if (customFont != null) {
                            System.out.println("Custom font loaded from external path: " + fontPath);
                            applyCustomFontToUsername();
                            return;
                        }
                    }
                }
            }

            // Try loading from bundled resources
            java.io.InputStream resourceStream = getClass().getResourceAsStream("/fonts/kiran.ttf");
            if (resourceStream != null) {
                customFont = Font.loadFont(resourceStream, 20);
                resourceStream.close();
                if (customFont != null) {
                    System.out.println("Custom font loaded from bundled resources: /fonts/kiran.ttf");
                    applyCustomFontToUsername();
                    return;
                }
            }

            System.out.println("No custom font available, using system default");

        } catch (Exception e) {
            System.err.println("Error loading custom font: " + e.getMessage());
        }
    }

    /**
     * Apply custom font to username and password fields
     * Note: Font is already set in FXML, this only applies if loaded from settings
     */
    private void applyCustomFontToUsername() {
        if (customFont != null) {
            String fontFamily = customFont.getFamily();
            String fontStyle =
                "-fx-font-family: '" + fontFamily + "';" +
                "-fx-font-size: 20px;" +
                "-fx-text-fill: #212121;" +
                "-fx-prompt-text-fill: #9E9E9E;";

            // Apply to username field
            if (txtUserName != null) {
                txtUserName.setFont(customFont);
                txtUserName.setStyle(fontStyle);

                // Maintain font on focus changes
                txtUserName.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    txtUserName.setFont(customFont);
                });
            }

            // Apply to password field as well
            if (txtPassword != null) {
                txtPassword.setFont(customFont);
                txtPassword.setStyle(fontStyle);

                // Maintain font on focus changes
                txtPassword.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    txtPassword.setFont(customFont);
                });
            }

            System.out.println("Custom font '" + fontFamily + "' applied to username and password fields");
        }
    }

    /**
     * Initialize shop dropdown with available restaurants
     */
    private void initializeShopDropdown() {
        try {
            List<Shop> shops = shopService.getAllShops();

            if (shops != null && !shops.isEmpty()) {
                cmbShop.getItems().addAll(shops);

                // Build font style for dropdown cells
                String cellFontStyle = "";
                if (customFont != null) {
                    cellFontStyle = "-fx-font-family: '" + customFont.getFamily() + "';" +
                                   "-fx-font-size: 20px;";
                }
                final String fontStyle = cellFontStyle;

                // Set custom cell factory to display restaurant name with Kiran font
                cmbShop.setCellFactory(param -> new javafx.scene.control.ListCell<Shop>() {
                    @Override
                    protected void updateItem(Shop item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getRestaurantName());
                            // Apply Kiran font if loaded
                            if (customFont != null) {
                                setFont(customFont);
                                setStyle(fontStyle + "-fx-text-fill: #212121;");
                            }
                        }
                    }
                });

                // Set button cell to display selected restaurant name with Kiran font
                cmbShop.setButtonCell(new javafx.scene.control.ListCell<Shop>() {
                    @Override
                    protected void updateItem(Shop item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Select your restaurant");
                        } else {
                            setText(item.getRestaurantName());
                        }
                        // Apply Kiran font if loaded
                        if (customFont != null) {
                            setFont(customFont);
                            setStyle(fontStyle + "-fx-text-fill: #212121;");
                        }
                    }
                });

                // Auto-select if only one shop exists
                if (shops.size() == 1) {
                    cmbShop.getSelectionModel().selectFirst();
                    System.out.println("Auto-selected single restaurant: " + shops.get(0).getRestaurantName());
                }

                System.out.println("Loaded " + shops.size() + " restaurant(s) into dropdown");
            } else {
                System.out.println("No restaurants found in database");
            }
        } catch (Exception e) {
            System.err.println("Error loading shops: " + e.getMessage());
            // Continue without shops if there's an error
        }
    }

    private void initializeUsernameAutocomplete() {
        try {
            // Get all usernames from database
            List<String> usernames = userRepository.findAllUsernames();

            // Check if this is first-time setup (no users exist)
            boolean isFirstTimeSetup = usernames.isEmpty();
           // isFirstTimeSetup = true;
            if (isFirstTimeSetup) {
                // Show registration link, hide support text
                if (linkRegister != null) {
                    linkRegister.setVisible(true);
                    linkRegister.setManaged(true);
                }
                if (lblSupport != null) {
                    lblSupport.setVisible(false);
                    lblSupport.setManaged(false);
                }
                System.out.println("First-time setup detected: No users found in database");
            } else {
                // Show support text, hide registration link
                if (lblSupport != null) {
                    lblSupport.setVisible(true);
                    lblSupport.setManaged(true);
                }
                if (linkRegister != null) {
                    linkRegister.setVisible(false);
                    linkRegister.setManaged(false);
                }

                // Create autocomplete with custom font if available
                if (customFont != null) {
                    autoCompleteTextField = new AutoCompleteTextField(txtUserName, usernames, customFont, txtPassword);
                    System.out.println("Autocomplete initialized with custom font and " + usernames.size() + " usernames");
                } else {
                    autoCompleteTextField = new AutoCompleteTextField(txtUserName, usernames, txtPassword);
                    System.out.println("Autocomplete initialized with " + usernames.size() + " usernames");
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing username autocomplete: " + e.getMessage());
            // Continue without autocomplete if there's an error
        }
    }

    private void openRegistrationScreen() {
        try {
            System.out.println("Opening restaurant registration screen...");
            stageManager.switchScene(FxmlView.CREATE_SHOPE);
        } catch (Exception e) {
            System.err.println("Error opening registration screen: " + e.getMessage());
            alertNotification.showError("Unable to open registration screen: " + e.getMessage());
        }
    }

    public void login() {

        // Validate shop selection
        if (cmbShop.getSelectionModel().getSelectedItem() == null) {
            alertNotification.showError("Please select a restaurant");
            return;
        }

        if (txtUserName.getText().isEmpty()) {
            alertNotification.showError("Please Enter User Name");
            return;
        }
        if (txtPassword.getText().isEmpty()) {
            alertNotification.showError("Please Enter Password");
            return;
        }

        String username = txtUserName.getText().trim();
        String password = txtPassword.getText().trim();
        Shop selectedShop = cmbShop.getSelectionModel().getSelectedItem();

        // Validate inputs
        if (username.isEmpty()) {
            alertNotification.showError("Please enter username");
            return;
        }
        if (password.isEmpty()) {
            alertNotification.showError("Please enter password");
            return;
        }

        try {
            // Test database connection first
            if (!authApiService.testConnection()) {
                alertNotification.showError(
                        "Cannot connect to database.\nPlease check database configuration and ensure MySQL is running");
                return;
            }

            // Attempt login - now returns User entity
            User user = authApiService.login(username, password);

            if (user != null) {
                // Store user session with selected shop
                sessionService.setUserSession(user, selectedShop);

                // Build success message
                StringBuilder successMessage = new StringBuilder();
                successMessage.append("Login Successful!\n\n");
                successMessage.append("Restaurant: ").append(selectedShop.getRestaurantName()).append("\n");
                successMessage.append("User: ").append(user.getUsername());
                successMessage.append(" (").append(user.getRole()).append(")");

                // Add employee info if available
                if (user.getEmployee() != null) {
                    successMessage.append("\n").append(user.getEmployee().getFullName());
                    if (user.getEmployee().getDesignation() != null) {
                        successMessage.append(" - ").append(user.getEmployee().getDesignation());
                    }
                }

                alertNotification.showSuccess(successMessage.toString());

                // Navigate to MaterialFX Demo (temporary for demo purposes)
                // stageManager.switchScene(FxmlView.MATERIALFX_DEMO);
                // Navigate to dashboard
                 stageManager.switchScene(FxmlView.DASHBOARD);
               // stageManager.switchScene(FxmlView.BILLING);
            } else {
                alertNotification.showError("Login failed. Please try again.");
            }

        } catch (RuntimeException e) {
            alertNotification.showError(e.getMessage());
        } catch (Exception e) {
            alertNotification.showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    private void closeApplication() {
        javafx.application.Platform.exit();
        System.exit(0);
    }

}
