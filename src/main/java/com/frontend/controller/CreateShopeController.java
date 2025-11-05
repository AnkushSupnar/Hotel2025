package com.frontend.controller;

import com.frontend.service.AuthApiService;
import com.frontend.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateShopeController {

    @FXML
    private Button cancelButton;

    @FXML
    private Button registerButton;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtShopAddress;

    @FXML
    private TextField txtShopContact;

    @FXML
    private TextField txtShopName;

    @FXML
    private TextField txtShoplicense;

    @FXML
    private TextField txtWonerContact;

    @FXML
    private TextField txtWonerName;
    @Autowired
    AuthApiService authApiService;
    @Autowired
    AlertNotification alertNotification;

    @FXML
    private void initialize() {
        registerButton.setOnAction(e -> registerShop());
    }

    private void registerShop() {
        if(!validateInputs()) return;
        try {
            // Register admin user with backend API
            boolean success = authApiService.register(
                txtWonerName.getText().trim(), 
                txtPassword.getText(), 
                "ADMIN"
            );
            
            if (success) {
                alertNotification.showSuccess(
                    "Admin user registered successfully!\n" +
                    "Username: " + txtWonerName.getText().trim() + "\n" +
                    "You can now login with these credentials."
                );
                clearForm();
            } else {
                alertNotification.showError("Registration failed. Please try again.");
            }
            
        } catch (RuntimeException e) {
            alertNotification.showError(e.getMessage());
        } catch (Exception e) {
            alertNotification.showError("An unexpected error occurred: " + e.getMessage());
        }
    }
    private boolean validateInputs() {
        if(txtShopName.getText().isEmpty()){
            alertNotification.showError("Enter Shop Name");
            return false;
        }
        if(txtShopAddress .getText().isEmpty()){
            alertNotification.showError("Enter Shop Address");
            return false;
        }
        if(txtShopContact .getText().isEmpty()){
            alertNotification.showError("Enter Shop Contact No");
            return false;
        }
        if(txtShoplicense .getText().isEmpty()){
            alertNotification.showError("Enter Shop license");
            return false;
        }
        if(txtWonerName .getText().isEmpty()){
            alertNotification.showError("Enter Shop Woner Name");
            return false;
        }
        if(txtWonerContact .getText().isEmpty()){
            alertNotification.showError("Enter Shop Woner Contact");
            return false;
        }
        if(txtPassword .getText().isEmpty()){
            alertNotification.showError("Enter Admin Password");
            return false;
        }
        if(txtConfirmPassword .getText().isEmpty()){
            alertNotification.showError("Enter Admin Confirm Password");
            return false;
        }
        if(!txtPassword .getText().equals(txtConfirmPassword.getText())){
            alertNotification.showError("Password And Confirm Password Must Be Same");
            return false;
        }
        return true;
    }



    private void clearForm() {
        txtShopName.clear();
        txtShopAddress.clear();
        txtShopContact.clear();
        txtShoplicense.clear();
        txtWonerName.clear();
        txtWonerContact.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
    }
}
