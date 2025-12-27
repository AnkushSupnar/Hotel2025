package com.frontend.controller;

import com.frontend.entity.Employees;
import com.frontend.entity.Shop;
import com.frontend.service.AuthApiService;
import com.frontend.service.EmployeesService;
import com.frontend.service.ShopService;
import com.frontend.view.AlertNotification;
import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    EmployeesService employeesService;

    @Autowired
    ShopService shopService;

    @Autowired
    AlertNotification alertNotification;

    @Autowired
    @Lazy
    StageManager stageManager;

    @FXML
    private void initialize() {
        registerButton.setOnAction(e -> registerShop());
        cancelButton.setOnAction(e -> backToLogin());
    }

    /**
     * Navigate back to login screen
     */
    private void backToLogin() {
        try {
            System.out.println("Navigating back to login screen...");
            stageManager.switchScene(FxmlView.LOGIN);
        } catch (Exception e) {
            System.err.println("Error navigating to login screen: " + e.getMessage());
            alertNotification.showError("Unable to navigate to login screen: " + e.getMessage());
        }
    }

    private void registerShop() {
        if(!validateInputs()) return;
        try {
            String ownerName = txtWonerName.getText().trim();

            // Step 1: Create Shop/Restaurant record
            Shop shop = Shop.builder()
                .restaurantName(txtShopName.getText().trim())
                .address(txtShopAddress.getText().trim())
                .contactNumber(txtShopContact.getText().trim())
                .licenseKey(txtShoplicense.getText().trim())
                .ownerName(ownerName)
                .build();

            // Save shop
            Shop savedShop = shopService.createShop(shop);

            // Step 2: Create Admin Employee record using Employees entity
            String[] nameParts = parseOwnerName(ownerName);

            Employees adminEmployee = new Employees();
            adminEmployee.setFirstName(nameParts[0]);
            adminEmployee.setMiddleName(nameParts[1]);
            adminEmployee.setLastName(nameParts[2]);
            adminEmployee.setAddressLine(txtShopAddress.getText().trim());
            adminEmployee.setMobileNo(txtWonerContact.getText().trim());
            adminEmployee.setDesignation("ADMIN");
            adminEmployee.setCurrentSalary(0.0f);
            adminEmployee.setActiveStatus(true);

            // Save employee
            Employees savedEmployee = employeesService.createEmployee(adminEmployee);

            // Step 3: Create Admin User linked to the Employee
            boolean success = authApiService.registerWithEmployee(
                ownerName,  // username
                txtPassword.getText(),  // password
                "ADMIN",  // role
                savedEmployee  // linked employee
            );

            if (success) {
                alertNotification.showSuccess(
                    "Restaurant registered successfully!\n\n" +
                    "Restaurant: " + savedShop.getRestaurantName() + "\n" +
                    "Shop ID: " + savedShop.getShopId() + "\n" +
                    "Admin Username: " + ownerName + "\n" +
                    "Employee ID: " + savedEmployee.getEmployeeId() + "\n\n" +
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

    /**
     * Parse owner name into first, middle, and last name
     * Assumes format: "FirstName MiddleName LastName" or "FirstName LastName"
     */
    private String[] parseOwnerName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        String firstName = "";
        String middleName = "";
        String lastName = "";

        if (parts.length == 1) {
            firstName = parts[0];
            lastName = parts[0]; // Use same as first name if only one part
        } else if (parts.length == 2) {
            firstName = parts[0];
            lastName = parts[1];
        } else if (parts.length >= 3) {
            firstName = parts[0];
            // Join all middle parts except the last one
            StringBuilder middle = new StringBuilder();
            for (int i = 1; i < parts.length - 1; i++) {
                if (middle.length() > 0) middle.append(" ");
                middle.append(parts[i]);
            }
            middleName = middle.toString();
            lastName = parts[parts.length - 1];
        }

        return new String[]{firstName, middleName, lastName};
    }
    private boolean validateInputs() {
        if(txtShopName.getText().isEmpty()){
            alertNotification.showError("Enter Restaurant Name");
            return false;
        }
        if(txtShopAddress .getText().isEmpty()){
            alertNotification.showError("Enter Restaurant Address");
            return false;
        }
        if(txtShopContact .getText().isEmpty()){
            alertNotification.showError("Enter Restaurant Contact Number");
            return false;
        }
        // License key is optional - no validation required

        if(txtWonerName .getText().isEmpty()){
            alertNotification.showError("Enter Owner Name");
            return false;
        }
        if(txtWonerContact .getText().isEmpty()){
            alertNotification.showError("Enter Owner Contact Number");
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
