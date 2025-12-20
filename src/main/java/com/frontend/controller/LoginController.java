package com.frontend.controller;

import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.entity.Shop;
import com.frontend.entity.User;
import com.frontend.repository.UserRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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

    @FXML
    private void initialize() {
        btnLogin.setOnAction(event -> login());
        // Hide server URL field since we're using database now
        if (txtServerUrl != null) {
            txtServerUrl.setVisible(false);
            txtServerUrl.setManaged(false);
        }

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
     * Initialize shop dropdown with available restaurants
     */
    private void initializeShopDropdown() {
        try {
            List<Shop> shops = shopService.getAllShops();

            if (shops != null && !shops.isEmpty()) {
                cmbShop.getItems().addAll(shops);

                // Set custom cell factory to display restaurant name
                cmbShop.setCellFactory(param -> new javafx.scene.control.ListCell<Shop>() {
                    @Override
                    protected void updateItem(Shop item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getRestaurantName());
                        }
                    }
                });

                // Set button cell to display selected restaurant name
                cmbShop.setButtonCell(new javafx.scene.control.ListCell<Shop>() {
                    @Override
                    protected void updateItem(Shop item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Select your restaurant");
                        } else {
                            setText(item.getRestaurantName());
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

                new AutoCompleteTextField(txtUserName, usernames, txtPassword);
                System.out.println("Autocomplete initialized with " + usernames.size() + " usernames");
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

}
