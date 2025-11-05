package com.frontend.controller;

import com.frontend.customUI.AutoCompleteTextField;
import com.frontend.dto.LoginResponse;
import com.frontend.repository.UserRepository;
import com.frontend.service.AuthApiService;
import com.frontend.service.SessionService;
import com.frontend.view.AlertNotification;
import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private PasswordField txtPassword;

    @FXML
    private TextField txtUserName;

    @FXML
    private TextField txtServerUrl;

    @FXML
    private javafx.scene.control.Label lblSupport;

    @FXML
    private javafx.scene.control.Hyperlink linkRegister;

    @Autowired @Lazy
    StageManager stageManager;

    @Autowired
    AlertNotification alertNotification;

    @Autowired
    AuthApiService authApiService;

    @Autowired
    SessionService sessionService;

    @Autowired
    UserRepository userRepository;

    private AutoCompleteTextField autoCompleteUsername;

    @FXML
    private void initialize() {
       btnLogin.setOnAction(event -> login());
       // Hide server URL field since we're using database now
       if(txtServerUrl != null) {
           txtServerUrl.setVisible(false);
           txtServerUrl.setManaged(false);
       }

       // Initialize autocomplete for username field and check for first-time setup
       initializeUsernameAutocomplete();

       // Setup registration link handler
       if(linkRegister != null) {
           linkRegister.setOnAction(event -> openRegistrationScreen());
       }
    }

    private void initializeUsernameAutocomplete() {
        try {
            // Get all usernames from database
            List<String> usernames = userRepository.findAllUsernames();

            // Check if this is first-time setup (no users exist)
            boolean isFirstTimeSetup = usernames.isEmpty();

            if(isFirstTimeSetup) {
                // Show registration link, hide support text
                if(linkRegister != null) {
                    linkRegister.setVisible(true);
                    linkRegister.setManaged(true);
                }
                if(lblSupport != null) {
                    lblSupport.setVisible(false);
                    lblSupport.setManaged(false);
                }
                System.out.println("First-time setup detected: No users found in database");
            } else {
                // Show support text, hide registration link
                if(lblSupport != null) {
                    lblSupport.setVisible(true);
                    lblSupport.setManaged(true);
                }
                if(linkRegister != null) {
                    linkRegister.setVisible(false);
                    linkRegister.setManaged(false);
                }

                // Initialize autocomplete with the username text field
                autoCompleteUsername = new AutoCompleteTextField(txtUserName, usernames);
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
    public void login(){

        if(txtUserName.getText().isEmpty()){
            alertNotification.showError("Please Enter User Name");
            return;
        }
        if(txtPassword.getText().isEmpty()){
            alertNotification.showError("Please Enter Password");
            return;
        }

        String username = txtUserName.getText().trim();
        String password = txtPassword.getText().trim();

        // Validate inputs
        if(username.isEmpty()) {
            alertNotification.showError("Please enter username");
            return;
        }
        if(password.isEmpty()) {
            alertNotification.showError("Please enter password");
            return;
        }

        try {
            // Test database connection first
            if(!authApiService.testConnection()) {
                alertNotification.showError("Cannot connect to database.\nPlease check database configuration and ensure MySQL is running");
                return;
            }

            // Attempt login
            LoginResponse loginResponse = authApiService.login(username, password);

            if(loginResponse != null) {
                // Store user session
                sessionService.setUserSession(loginResponse);

                // Show success message
                alertNotification.showSuccess("Login Successful!\nWelcome " + loginResponse.getUsername() + " (" + loginResponse.getRole() + ")");

                // Navigate to dashboard
                stageManager.switchScene(FxmlView.DASHBOARD);
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
