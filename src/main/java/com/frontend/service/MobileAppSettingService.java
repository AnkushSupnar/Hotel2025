package com.frontend.service;

import com.frontend.entity.MobileAppSetting;
import com.frontend.entity.MobileFeatureAccess;
import com.frontend.repository.MobileAppSettingRepository;
import com.frontend.repository.MobileFeatureAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing mobile application settings
 */
@Service
@Transactional
public class MobileAppSettingService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileAppSettingService.class);

    // Setting Keys
    public static final String MOBILE_ACCESS_ENABLED = "MOBILE_ACCESS_ENABLED";
    public static final String JWT_TOKEN_EXPIRY_HOURS = "JWT_TOKEN_EXPIRY_HOURS";
    public static final String JWT_TOKEN_EXPIRY_DAYS = "JWT_TOKEN_EXPIRY_DAYS";
    public static final String JWT_SECRET_KEY = "JWT_SECRET_KEY";
    public static final String MOBILE_APP_VERSION = "MOBILE_APP_VERSION";
    public static final String FORCE_UPDATE_ENABLED = "FORCE_UPDATE_ENABLED";

    // Mobile Features
    public static final String FEATURE_VIEW_TABLES = "VIEW_TABLES";
    public static final String FEATURE_TAKE_ORDER = "TAKE_ORDER";
    public static final String FEATURE_VIEW_ORDERS = "VIEW_ORDERS";
    public static final String FEATURE_EDIT_ORDER = "EDIT_ORDER";
    public static final String FEATURE_CANCEL_ORDER = "CANCEL_ORDER";
    public static final String FEATURE_VIEW_BILLS = "VIEW_BILLS";
    public static final String FEATURE_GENERATE_BILL = "GENERATE_BILL";
    public static final String FEATURE_ACCEPT_PAYMENT = "ACCEPT_PAYMENT";
    public static final String FEATURE_VIEW_MENU = "VIEW_MENU";
    public static final String FEATURE_VIEW_REPORTS = "VIEW_REPORTS";
    public static final String FEATURE_MANAGE_TABLES = "MANAGE_TABLES";
    public static final String FEATURE_KOT_PRINT = "KOT_PRINT";

    // Default roles
    public static final String[] DEFAULT_ROLES = {"ADMIN", "MANAGER", "CASHIER", "CAPTAIN", "WAITER"};

    // Feature definitions with display names
    public static final Map<String, String> FEATURE_DEFINITIONS = new LinkedHashMap<>();
    static {
        FEATURE_DEFINITIONS.put(FEATURE_VIEW_TABLES, "View Tables");
        FEATURE_DEFINITIONS.put(FEATURE_TAKE_ORDER, "Take Order");
        FEATURE_DEFINITIONS.put(FEATURE_VIEW_ORDERS, "View Orders");
        FEATURE_DEFINITIONS.put(FEATURE_EDIT_ORDER, "Edit Order");
        FEATURE_DEFINITIONS.put(FEATURE_CANCEL_ORDER, "Cancel Order");
        FEATURE_DEFINITIONS.put(FEATURE_VIEW_BILLS, "View Bills");
        FEATURE_DEFINITIONS.put(FEATURE_GENERATE_BILL, "Generate Bill");
        FEATURE_DEFINITIONS.put(FEATURE_ACCEPT_PAYMENT, "Accept Payment");
        FEATURE_DEFINITIONS.put(FEATURE_VIEW_MENU, "View Menu");
        FEATURE_DEFINITIONS.put(FEATURE_VIEW_REPORTS, "View Reports");
        FEATURE_DEFINITIONS.put(FEATURE_MANAGE_TABLES, "Manage Tables");
        FEATURE_DEFINITIONS.put(FEATURE_KOT_PRINT, "KOT Print");
    }

    @Autowired
    private MobileAppSettingRepository settingRepository;

    @Autowired
    private MobileFeatureAccessRepository featureAccessRepository;

    // ==================== SETTINGS METHODS ====================

    /**
     * Get all mobile app settings
     */
    public List<MobileAppSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    /**
     * Get setting by key
     */
    public Optional<MobileAppSetting> getSettingByKey(String key) {
        return settingRepository.findBySettingKey(key);
    }

    /**
     * Get setting value by key
     */
    public String getSettingValue(String key) {
        return settingRepository.findBySettingKey(key)
                .map(MobileAppSetting::getSettingValue)
                .orElse(null);
    }

    /**
     * Get setting value as boolean
     */
    public boolean getSettingBoolean(String key, boolean defaultValue) {
        String value = getSettingValue(key);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Get setting value as integer
     */
    public int getSettingInteger(String key, int defaultValue) {
        String value = getSettingValue(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Save or update a setting
     */
    public MobileAppSetting saveSetting(String key, String value, String type, String description) {
        LOG.info("Saving mobile setting: {} = {}", key, value);

        Optional<MobileAppSetting> existing = settingRepository.findBySettingKey(key);

        MobileAppSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setSettingValue(value);
            setting.setSettingType(type);
            setting.setDescription(description);
        } else {
            setting = MobileAppSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .settingType(type)
                    .description(description)
                    .build();
        }

        return settingRepository.save(setting);
    }

    /**
     * Check if mobile access is enabled
     */
    public boolean isMobileAccessEnabled() {
        return getSettingBoolean(MOBILE_ACCESS_ENABLED, false);
    }

    /**
     * Get JWT token expiration in hours
     */
    public int getJwtExpirationHours() {
        int days = getSettingInteger(JWT_TOKEN_EXPIRY_DAYS, 0);
        int hours = getSettingInteger(JWT_TOKEN_EXPIRY_HOURS, 24);
        return (days * 24) + hours;
    }

    /**
     * Get JWT secret key
     */
    public String getJwtSecretKey() {
        String secret = getSettingValue(JWT_SECRET_KEY);
        if (secret == null || secret.isEmpty()) {
            // Generate and save a default secret
            secret = UUID.randomUUID().toString() + UUID.randomUUID().toString();
            saveSetting(JWT_SECRET_KEY, secret, "STRING", "JWT Secret Key for token signing");
        }
        return secret;
    }

    // ==================== FEATURE ACCESS METHODS ====================

    /**
     * Get all feature access records
     */
    public List<MobileFeatureAccess> getAllFeatureAccess() {
        return featureAccessRepository.findAll();
    }

    /**
     * Get feature access for a specific role
     */
    public List<MobileFeatureAccess> getFeatureAccessByRole(String role) {
        return featureAccessRepository.findByRole(role);
    }

    /**
     * Get enabled features for a role
     */
    public List<MobileFeatureAccess> getEnabledFeaturesForRole(String role) {
        return featureAccessRepository.findByRoleAndIsEnabledTrue(role);
    }

    /**
     * Check if a feature is enabled for a role
     */
    public boolean isFeatureEnabledForRole(String role, String featureCode) {
        return featureAccessRepository.findByRoleAndFeatureCode(role, featureCode)
                .map(MobileFeatureAccess::getIsEnabled)
                .orElse(false);
    }

    /**
     * Save or update feature access
     */
    public MobileFeatureAccess saveFeatureAccess(String role, String featureCode, String featureName, boolean isEnabled) {
        LOG.info("Saving feature access: {} - {} = {}", role, featureCode, isEnabled);

        Optional<MobileFeatureAccess> existing = featureAccessRepository.findByRoleAndFeatureCode(role, featureCode);

        MobileFeatureAccess access;
        if (existing.isPresent()) {
            access = existing.get();
            access.setIsEnabled(isEnabled);
            access.setFeatureName(featureName);
        } else {
            access = MobileFeatureAccess.builder()
                    .role(role)
                    .featureCode(featureCode)
                    .featureName(featureName)
                    .isEnabled(isEnabled)
                    .build();
        }

        return featureAccessRepository.save(access);
    }

    /**
     * Initialize default settings if not exist
     */
    public void initializeDefaultSettings() {
        LOG.info("Initializing default mobile app settings...");

        // Default settings
        if (!settingRepository.existsBySettingKey(MOBILE_ACCESS_ENABLED)) {
            saveSetting(MOBILE_ACCESS_ENABLED, "false", "BOOLEAN", "Enable/disable mobile app access");
        }
        if (!settingRepository.existsBySettingKey(JWT_TOKEN_EXPIRY_DAYS)) {
            saveSetting(JWT_TOKEN_EXPIRY_DAYS, "7", "INTEGER", "JWT token expiry in days");
        }
        if (!settingRepository.existsBySettingKey(JWT_TOKEN_EXPIRY_HOURS)) {
            saveSetting(JWT_TOKEN_EXPIRY_HOURS, "0", "INTEGER", "JWT token expiry additional hours");
        }
        if (!settingRepository.existsBySettingKey(MOBILE_APP_VERSION)) {
            saveSetting(MOBILE_APP_VERSION, "1.0.0", "STRING", "Minimum mobile app version required");
        }
        if (!settingRepository.existsBySettingKey(FORCE_UPDATE_ENABLED)) {
            saveSetting(FORCE_UPDATE_ENABLED, "false", "BOOLEAN", "Force users to update mobile app");
        }

        // Initialize default feature access for all roles
        for (String role : DEFAULT_ROLES) {
            for (Map.Entry<String, String> feature : FEATURE_DEFINITIONS.entrySet()) {
                if (!featureAccessRepository.existsByRoleAndFeatureCode(role, feature.getKey())) {
                    // Default: ADMIN has all features, others have basic features
                    boolean defaultEnabled = "ADMIN".equals(role) ||
                            feature.getKey().equals(FEATURE_VIEW_TABLES) ||
                            feature.getKey().equals(FEATURE_VIEW_MENU);
                    saveFeatureAccess(role, feature.getKey(), feature.getValue(), defaultEnabled);
                }
            }
        }

        LOG.info("Default mobile app settings initialized");
    }

    /**
     * Get feature access matrix (all roles x all features)
     */
    public Map<String, Map<String, Boolean>> getFeatureAccessMatrix() {
        Map<String, Map<String, Boolean>> matrix = new LinkedHashMap<>();

        for (String role : DEFAULT_ROLES) {
            Map<String, Boolean> roleFeatures = new LinkedHashMap<>();
            for (String featureCode : FEATURE_DEFINITIONS.keySet()) {
                boolean enabled = isFeatureEnabledForRole(role, featureCode);
                roleFeatures.put(featureCode, enabled);
            }
            matrix.put(role, roleFeatures);
        }

        return matrix;
    }

    /**
     * Get list of enabled feature codes for a role (for JWT claims)
     */
    public List<String> getEnabledFeatureCodesForRole(String role) {
        List<String> features = new ArrayList<>();
        List<MobileFeatureAccess> enabledFeatures = getEnabledFeaturesForRole(role);
        for (MobileFeatureAccess feature : enabledFeatures) {
            features.add(feature.getFeatureCode());
        }
        return features;
    }
}
