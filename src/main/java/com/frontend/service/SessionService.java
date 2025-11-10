package com.frontend.service;

import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.Employee;
import com.frontend.entity.Shop;
import com.frontend.entity.User;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing user session in desktop application
 * Uses static properties for single-user desktop application
 */
@Service
public class SessionService {

    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private ApplicationSettingService applicationSettingService;

    // Static properties for desktop application (single user session)
    private static User currentUser;
    private static Employee currentEmployee;
    private static Shop currentShop;
    private static Map<String, String> applicationSettings;

    // Static Font object - loaded once for entire application lifecycle
    private static Font customFont;

    /**
     * Set user session after successful login
     */
    public void setUserSession(User user, Shop shop) {
        LOG.info("Setting user session for: {}", user != null ? user.getUsername() : "null");

        currentUser = user;
        currentEmployee = user != null ? user.getEmployee() : null;
        currentShop = shop;

        // Load application settings and custom font
        loadApplicationSettings();

        LOG.info("Session established - User: {}, Employee: {}, Shop: {}",
                 getCurrentUsername(),
                 currentEmployee != null ? currentEmployee.getFullName() : "Not linked",
                 shop != null ? shop.getRestaurantName() : "Not selected");
    }

    /**
     * Get current logged-in user
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Get current employee (if user is linked to an employee)
     */
    public static Employee getCurrentEmployee() {
        return currentEmployee;
    }

    /**
     * Get current shop/restaurant
     */
    public static Shop getCurrentShop() {
        return currentShop;
    }

    /**
     * Check if user is logged in
     */
    public static boolean isLoggedIn() {
        boolean loggedIn = currentUser != null;
        LOG.debug("Checking isLoggedIn: {}", loggedIn);
        return loggedIn;
    }

    /**
     * Get current username
     */
    public static String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    /**
     * Get current user role
     */
    public static String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    /**
     * Get current user ID
     */
    public static Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * Get current employee ID
     */
    public static Long getCurrentEmployeeId() {
        return currentEmployee != null ? currentEmployee.getEmployeeId() : null;
    }

    /**
     * Get current employee name
     */
    public static String getCurrentEmployeeName() {
        return currentEmployee != null ? currentEmployee.getFullName() : null;
    }

    /**
     * Get current employee designation
     */
    public static String getCurrentEmployeeDesignation() {
        return currentEmployee != null && currentEmployee.getDesignation() != null
               ? currentEmployee.getDesignation()
               : null;
    }

    /**
     * Get current shop ID
     */
    public static Long getCurrentShopId() {
        return currentShop != null ? currentShop.getShopId() : null;
    }

    /**
     * Get current restaurant name
     */
    public static String getCurrentRestaurantName() {
        return currentShop != null ? currentShop.getRestaurantName() : null;
    }

    /**
     * Get current restaurant address
     */
    public static String getCurrentRestaurantAddress() {
        return currentShop != null ? currentShop.getAddress() : null;
    }

    /**
     * Get current restaurant contact
     */
    public static String getCurrentRestaurantContact() {
        return currentShop != null ? currentShop.getContactNumber() : null;
    }

    /**
     * Clear user session (logout)
     */
    public void clearSession() {
        LOG.info("Clearing session for user: {} from shop: {}",
                 getCurrentUsername(),
                 getCurrentRestaurantName());
        currentUser = null;
        currentEmployee = null;
        currentShop = null;
        applicationSettings = null;
        customFont = null; // Clear font on logout
    }

    /**
     * Load all application settings from database into session
     */
    public void loadApplicationSettings() {
        try {
            applicationSettings = new HashMap<>();
            List<ApplicationSetting> settings = applicationSettingService.getAllSettings();

            for (ApplicationSetting setting : settings) {
                applicationSettings.put(setting.getSettingName(), setting.getSettingValue());
            }

            LOG.info("Loaded {} application settings into session", applicationSettings.size());

            // Load custom font after loading settings
            loadCustomFont();

        } catch (Exception e) {
            LOG.error("Error loading application settings: ", e);
            applicationSettings = new HashMap<>();
        }
    }

    /**
     * Load custom font from settings (called once during application lifecycle)
     */
    private void loadCustomFont() {
        try {
            String fontPath = getApplicationSetting("input_font_path");

            if (fontPath != null && !fontPath.trim().isEmpty()) {
                File fontFile = new File(fontPath);

                if (fontFile.exists()) {
                    FileInputStream fontStream = new FileInputStream(fontFile);
                    customFont = Font.loadFont(fontStream, 25); // Load with default size 15
                    fontStream.close();

                    if (customFont != null) {
                        LOG.info("Custom font loaded successfully: {}", fontPath);
                    } else {
                        LOG.warn("Failed to load custom font from: {}", fontPath);
                    }
                } else {
                    LOG.warn("Custom font file does not exist: {}", fontPath);
                    customFont = null;
                }
            } else {
                LOG.debug("No custom font configured");
                customFont = null;
            }
        } catch (Exception e) {
            LOG.error("Error loading custom font: ", e);
            customFont = null;
        }
    }

    /**
     * Get application setting value by name
     * @param settingName the setting name
     * @return setting value or null if not found
     */
    public static String getApplicationSetting(String settingName) {
        return applicationSettings != null ? applicationSettings.get(settingName) : null;
    }

    /**
     * Get font path setting
     * @return font file path or null
     */
    public static String getFontPath() {
        return getApplicationSetting("input_font_path");
    }

    /**
     * Get document directory setting
     * @return document directory path or null
     */
    public static String getDocumentDirectory() {
        return getApplicationSetting("document_directory");
    }

    /**
     * Get custom font object (loaded once for entire application)
     * @return Font object or null if not configured
     */
    public static Font getCustomFont() {
        return customFont;
    }

    /**
     * Get custom font with specific size
     * @param size the font size
     * @return Font object with specified size or null
     */
    public static Font getCustomFont(double size) {
        if (customFont != null) {
            return Font.font(customFont.getFamily(), size);
        }
        return null;
    }

    /**
     * Reload application settings (useful after settings are updated)
     */
    public void reloadApplicationSettings() {
        LOG.info("Reloading application settings");
        loadApplicationSettings();
    }
}