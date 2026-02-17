package com.frontend.service;

import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.Employees;
import com.frontend.entity.Shop;
import com.frontend.entity.User;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.frontend.util.ApplicationSettingProperties;

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
    private static Employees currentEmployee;
    private static Shop currentShop;
    private static Map<String, String> applicationSettings;

    // Static Font object - loaded once for entire application lifecycle
    private static Font customFont;

    // Cached font family name for creating fonts with different sizes
    private static String cachedFontFamily;

    // Whether to use bill logo in bill printing
    private static boolean useBillLogo = false;

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
    public static Employees getCurrentEmployee() {
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
    public static int getCurrentEmployeeId() {
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
     * Get current restaurant secondary contact
     */
    public static String getCurrentRestaurantContact2() {
        return currentShop != null ? currentShop.getContactNumber2() : null;
    }

    /**
     * Get current restaurant subtitle/tagline
     */
    public static String getCurrentRestaurantSubTitle() {
        return currentShop != null ? currentShop.getSubTitle() : null;
    }

    /**
     * Get current restaurant GSTIN number
     */
    public static String getCurrentRestaurantGstin() {
        return currentShop != null ? currentShop.getGstinNumber() : null;
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
        useBillLogo = false;
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

            // Merge machine-specific settings from local properties file
            String documentDir = applicationSettings.get("document_directory");
            if (documentDir != null && !documentDir.trim().isEmpty()) {
                Map<String, String> localSettings = ApplicationSettingProperties.loadSettings(documentDir);
                applicationSettings.putAll(localSettings);
                LOG.info("Merged {} local machine settings from properties file", localSettings.size());
            }

            LOG.info("Loaded {} application settings into session", applicationSettings.size());

            // Load use bill logo setting
            String useBillLogoValue = applicationSettings.get("use_bill_logo");
            useBillLogo = "true".equalsIgnoreCase(useBillLogoValue);
            LOG.info("Use bill logo: {}", useBillLogo);

            // Load custom font after loading settings
            loadCustomFont();

        } catch (Exception e) {
            LOG.error("Error loading application settings: ", e);
            applicationSettings = new HashMap<>();
        }
    }

    /**
     * Load custom font from settings (called once during application lifecycle)
     * Priority: 1) External path from settings, 2) Bundled resource font
     */
    private void loadCustomFont() {
        try {
            // First, try loading from external path in settings
            String fontPath = getApplicationSetting("input_font_path");
            boolean loadedFromExternal = false;

            if (fontPath != null && !fontPath.trim().isEmpty()) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    loadedFromExternal = loadFontFromFile(fontFile);
                } else {
                    LOG.warn("External font file does not exist: {}", fontPath);
                }
            }

            // If external loading failed, try bundled resource font
            if (!loadedFromExternal) {
                LOG.info("Trying to load bundled Kiran font from resources...");
                loadFontFromResources();
            }

            // Log final status
            if (customFont != null) {
                LOG.info("=== Custom Font Loaded ===");
                LOG.info("  Family: {}", customFont.getFamily());
                LOG.info("  Name: {}", customFont.getName());
                LOG.info("  Style: {}", customFont.getStyle());
                LOG.info("  Registered in JavaFX: {}", Font.getFamilies().contains(cachedFontFamily));
                LOG.info("==========================");
            } else {
                LOG.warn("No custom font available - using system defaults");
            }

        } catch (Exception e) {
            LOG.error("Error loading custom font: ", e);
            customFont = null;
            cachedFontFamily = null;
        }
    }

    /**
     * Load font from external file
     */
    private boolean loadFontFromFile(File fontFile) {
        try {
            // Method 1: Load via FileInputStream (most reliable for custom fonts)
            try (FileInputStream fontStream = new FileInputStream(fontFile)) {
                customFont = Font.loadFont(fontStream, 20);
            }

            // Method 2: If stream method fails, try URI method
            if (customFont == null) {
                String fontUri = fontFile.toURI().toString();
                customFont = Font.loadFont(fontUri, 20);
            }

            if (customFont != null) {
                cachedFontFamily = customFont.getFamily();
                LOG.info("Font loaded from external file: {}", fontFile.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            LOG.error("Failed to load font from file: {}", fontFile.getAbsolutePath(), e);
        }
        return false;
    }

    /**
     * Load font from bundled resources (classpath)
     */
    private void loadFontFromResources() {
        try {
            // Load from classpath resources
            java.io.InputStream fontStream = getClass().getResourceAsStream("/fonts/kiran.ttf");

            if (fontStream != null) {
                customFont = Font.loadFont(fontStream, 20);
                fontStream.close();

                if (customFont != null) {
                    cachedFontFamily = customFont.getFamily();
                    LOG.info("Font loaded from bundled resources: /fonts/kiran.ttf");
                } else {
                    LOG.warn("Failed to parse bundled font file");
                }
            } else {
                LOG.warn("Bundled font not found at /fonts/kiran.ttf");
            }
        } catch (Exception e) {
            LOG.error("Failed to load font from resources", e);
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
     * Check if bill logo should be used in bill printing
     */
    public static boolean isUseBillLogo() {
        return useBillLogo;
    }

    /**
     * Set whether to use bill logo in bill printing
     */
    public static void setUseBillLogo(boolean value) {
        useBillLogo = value;
        LOG.info("Use bill logo set to: {}", value);
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
     * Note: Custom fonts like Kiran typically don't support FontWeight variants,
     * so we use the font as-is without attempting weight modification.
     *
     * @param size the font size
     * @return Font object with specified size or null
     */
    public static Font getCustomFont(double size) {
        if (customFont == null || cachedFontFamily == null) {
            return null;
        }

        // Use the font family directly without weight modification
        // Custom fonts often render poorly when JavaFX tries to synthesize bold
        Font font = Font.font(cachedFontFamily, size);

        if (font != null && font.getFamily().equalsIgnoreCase(cachedFontFamily)) {
            LOG.debug("Using font '{}' at size: {}", cachedFontFamily, size);
            return font;
        }

        // If font family lookup fails, return null
        LOG.warn("Font family '{}' not found in JavaFX, font may not render correctly", cachedFontFamily);
        return null;
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
     * Get custom font file path for external libraries (iText, POI, etc.)
     * Returns the external path if available, otherwise tries to extract bundled font
     *
     * @return font file path or null if not available
     */
    public static String getCustomFontFilePath() {
        // First try external path
        String fontPath = getApplicationSetting("input_font_path");
        if (fontPath != null && !fontPath.trim().isEmpty()) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                return fontFile.getAbsolutePath();
            }
        }

        // Try to get bundled font path
        try {
            java.net.URL fontUrl = SessionService.class.getResource("/fonts/kiran.ttf");
            if (fontUrl != null) {
                // If running from JAR, extract to temp file
                if (fontUrl.getProtocol().equals("jar")) {
                    java.io.InputStream is = SessionService.class.getResourceAsStream("/fonts/kiran.ttf");
                    if (is != null) {
                        File tempFile = File.createTempFile("kiran_font_", ".ttf");
                        tempFile.deleteOnExit();
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }
                        is.close();
                        return tempFile.getAbsolutePath();
                    }
                } else {
                    // Running from IDE/file system
                    return new File(fontUrl.toURI()).getAbsolutePath();
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting custom font file path", e);
        }

        return null;
    }

    /**
     * Get input stream for custom font (useful for libraries that accept streams)
     *
     * @return InputStream for font file or null if not available
     */
    public static java.io.InputStream getCustomFontInputStream() {
        // First try external path
        String fontPath = getApplicationSetting("input_font_path");
        if (fontPath != null && !fontPath.trim().isEmpty()) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    return new FileInputStream(fontFile);
                } catch (Exception e) {
                    LOG.error("Error opening external font file", e);
                }
            }
        }

        // Try bundled font
        java.io.InputStream is = SessionService.class.getResourceAsStream("/fonts/kiran.ttf");
        if (is != null) {
            return is;
        }

        return null;
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
