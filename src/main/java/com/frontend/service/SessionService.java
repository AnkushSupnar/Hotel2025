package com.frontend.service;

import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.Employee;
import com.frontend.entity.Shop;
import com.frontend.entity.User;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    // Cached font family name for creating fonts with different sizes
    private static String cachedFontFamily;

    // Multiplier to match Swing font size appearance in JavaFX
    private static final double JAVAFX_FONT_SIZE_MULTIPLIER = 1.33;

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
    public boolean isLoggedIn() {
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
        customFont = null;
        cachedFontFamily = null;
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

            if (fontPath == null || fontPath.trim().isEmpty()) {
                LOG.debug("No custom font configured");
                customFont = null;
                cachedFontFamily = null;
                return;
            }

            File fontFile = new File(fontPath);

            if (!fontFile.exists()) {
                LOG.warn("Custom font file does not exist: {}", fontPath);
                customFont = null;
                cachedFontFamily = null;
                return;
            }

            // Try loading font using file URI first (sometimes works better)
            String fontUri = fontFile.toURI().toString();
            customFont = Font.loadFont(fontUri, 25);

            // If URI method fails, try FileInputStream with try-with-resources
            if (customFont == null) {
                try (FileInputStream fontStream = new FileInputStream(fontFile)) {
                    customFont = Font.loadFont(fontStream, 25);
                }
            }

            if (customFont != null) {
                cachedFontFamily = customFont.getFamily();
                LOG.info("Custom font loaded successfully:");
                LOG.info("  Path: {}", fontPath);
                LOG.info("  Family: {}", customFont.getFamily());
                LOG.info("  Name: {}", customFont.getName());
                LOG.info("  Style: {}", customFont.getStyle());
                LOG.info("  Size: {}", customFont.getSize());

                // Debug: Check if font family is registered in JavaFX
                boolean familyExists = Font.getFamilies().contains(customFont.getFamily());
                LOG.info("  Family registered in JavaFX: {}", familyExists);
            } else {
                LOG.warn("Failed to load custom font from: {}", fontPath);
                cachedFontFamily = null;
            }

        } catch (Exception e) {
            LOG.error("Error loading custom font: ", e);
            customFont = null;
            cachedFontFamily = null;
        }
    }

    /**
     * Get application setting value by name
     * 
     * @param settingName the setting name
     * @return setting value or null if not found
     */
    public static String getApplicationSetting(String settingName) {
        return applicationSettings != null ? applicationSettings.get(settingName) : null;
    }

    /**
     * Get font path setting
     * 
     * @return font file path or null
     */
    public static String getFontPath() {
        return getApplicationSetting("input_font_path");
    }

    /**
     * Get document directory setting
     * 
     * @return document directory path or null
     */
    public static String getDocumentDirectory() {
        return getApplicationSetting("document_directory");
    }

    /**
     * Get custom font object (loaded once for entire application)
     * 
     * @return Font object or null if not configured
     */
    public static Font getCustomFont() {
        return customFont;
    }

    /**
     * Get custom font with specific size
     * 
     * @param size the font size
     * @return Font object with specified size or null
     */
    public static Font getCustomFont(double size) {
        if (customFont == null || cachedFontFamily == null) {
            return null;
        }

        // Try to use system-installed font with BOLD weight
        Font boldFont = Font.font(cachedFontFamily, FontWeight.BOLD, size);

        if (boldFont != null && boldFont.getFamily().equalsIgnoreCase(cachedFontFamily)) {
            LOG.debug("Using font '{}' with BOLD weight, size: {}", cachedFontFamily, size);
            return boldFont;
        }

        // Fall back to regular font
        LOG.debug("Using font '{}' with regular weight, size: {}", cachedFontFamily, size);
        return Font.font(cachedFontFamily, size);
    }

    /**
     * Get custom font with size adjusted to match Swing rendering
     * Use this method when migrating from Swing to match visual appearance
     * 
     * @param swingSize the font size as used in Swing
     * @return Font with adjusted size for JavaFX or null if not configured
     */
    public static Font getCustomFontForSwingSize(double swingSize) {
        if (customFont == null || cachedFontFamily == null) {
            return null;
        }

        double adjustedSize = swingSize * JAVAFX_FONT_SIZE_MULTIPLIER;
        LOG.debug("Converting Swing size {} to JavaFX size {} for font '{}'",
                swingSize, adjustedSize, cachedFontFamily);
        return Font.font(cachedFontFamily, adjustedSize);
    }

    /**
     * Get custom font with specific size and weight
     * 
     * @param size   the font size
     * @param weight the font weight (e.g., FontWeight.BOLD, FontWeight.NORMAL)
     * @return Font object with specified size and weight or null
     */
    public static Font getCustomFont(double size, FontWeight weight) {
        if (customFont == null || cachedFontFamily == null) {
            return null;
        }
        return Font.font(cachedFontFamily, weight, size);
    }

    /**
     * Get the font family name of loaded custom font
     * 
     * @return font family name or null if not loaded
     */
    public static String getCustomFontFamily() {
        return cachedFontFamily;
    }

    /**
     * Check if custom font is loaded and available
     * 
     * @return true if custom font is loaded
     */
    public static boolean isCustomFontLoaded() {
        return customFont != null && cachedFontFamily != null;
    }

    /**
     * Debug method to print font information
     * Call this to troubleshoot font loading issues
     */
    public static void debugFontInfo() {
        LOG.info("=== Font Debug Info ===");
        if (customFont != null) {
            LOG.info("Custom Font Loaded: YES");
            LOG.info("  Family: {}", customFont.getFamily());
            LOG.info("  Name: {}", customFont.getName());
            LOG.info("  Style: {}", customFont.getStyle());
            LOG.info("  Cached Family: {}", cachedFontFamily);

            // Check if font family exists in system
            boolean familyExists = Font.getFamilies().contains(cachedFontFamily);
            LOG.info("  Registered in JavaFX: {}", familyExists);

            // List similar font families
            List<String> similarFonts = Font.getFamilies().stream()
                    .filter(f -> f.toLowerCase().contains("kiran"))
                    .toList();
            if (!similarFonts.isEmpty()) {
                LOG.info("  Similar fonts found: {}", similarFonts);
            }
        } else {
            LOG.info("Custom Font Loaded: NO");
            LOG.info("  Font Path Setting: {}", getFontPath());
        }
        LOG.info("=======================");
    }

    /**
     * Reload application settings (useful after settings are updated)
     */
    public void reloadApplicationSettings() {
        LOG.info("Reloading application settings");
        loadApplicationSettings();
    }
}
