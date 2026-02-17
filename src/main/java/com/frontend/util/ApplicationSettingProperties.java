package com.frontend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Utility class for managing machine-specific application settings
 * (billing_printer, kot_printer, default_billing_bank) in a local properties file.
 * This allows each machine to have its own printer/bank configuration.
 */
public class ApplicationSettingProperties {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationSettingProperties.class);

    public static final String FILE_NAME = "ApplicationSetting.properties";

    public static final Set<String> MANAGED_KEYS = Set.of(
            "billing_printer",
            "kot_printer",
            "default_billing_bank"
    );

    private ApplicationSettingProperties() {
        // Static utility class
    }

    /**
     * Check if a setting key is managed by the properties file
     */
    public static boolean isManagedKey(String key) {
        return key != null && MANAGED_KEYS.contains(key);
    }

    /**
     * Get the properties file object for the given document directory
     */
    public static File getPropertiesFile(String documentDirectory) {
        return new File(documentDirectory, FILE_NAME);
    }

    /**
     * Load settings from the properties file.
     * Returns a map containing only the managed keys that exist in the file.
     */
    public static Map<String, String> loadSettings(String documentDirectory) {
        Map<String, String> settings = new HashMap<>();

        if (documentDirectory == null || documentDirectory.trim().isEmpty()) {
            LOG.debug("Document directory is not configured, skipping properties file load");
            return settings;
        }

        File propsFile = getPropertiesFile(documentDirectory);
        if (!propsFile.exists()) {
            LOG.debug("Properties file does not exist: {}", propsFile.getAbsolutePath());
            return settings;
        }

        try (FileInputStream fis = new FileInputStream(propsFile)) {
            Properties props = new Properties();
            props.load(fis);

            for (String key : MANAGED_KEYS) {
                String value = props.getProperty(key);
                if (value != null) {
                    settings.put(key, value);
                }
            }

            LOG.info("Loaded {} settings from properties file: {}", settings.size(), propsFile.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("Error loading settings from properties file: {}", propsFile.getAbsolutePath(), e);
        }

        return settings;
    }

    /**
     * Save a single setting to the properties file.
     * Loads existing properties, updates the key, and writes back.
     */
    public static void saveSetting(String documentDirectory, String key, String value) {
        if (documentDirectory == null || documentDirectory.trim().isEmpty()) {
            LOG.error("Cannot save setting '{}': document directory is not configured", key);
            return;
        }

        File propsFile = getPropertiesFile(documentDirectory);
        Properties props = new Properties();

        // Load existing properties if file exists
        if (propsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                props.load(fis);
            } catch (Exception e) {
                LOG.error("Error reading existing properties file: {}", propsFile.getAbsolutePath(), e);
            }
        }

        props.setProperty(key, value);

        try (FileOutputStream fos = new FileOutputStream(propsFile)) {
            props.store(fos, "Application Settings - Local Machine Configuration");
            LOG.info("Saved setting '{}' = '{}' to properties file: {}", key, value, propsFile.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("Error saving setting to properties file: {}", propsFile.getAbsolutePath(), e);
        }
    }

    /**
     * Remove a setting from the properties file.
     */
    public static void removeSetting(String documentDirectory, String key) {
        if (documentDirectory == null || documentDirectory.trim().isEmpty()) {
            LOG.error("Cannot remove setting '{}': document directory is not configured", key);
            return;
        }

        File propsFile = getPropertiesFile(documentDirectory);
        if (!propsFile.exists()) {
            return;
        }

        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(propsFile)) {
            props.load(fis);
        } catch (Exception e) {
            LOG.error("Error reading existing properties file: {}", propsFile.getAbsolutePath(), e);
            return;
        }

        props.remove(key);

        try (FileOutputStream fos = new FileOutputStream(propsFile)) {
            props.store(fos, "Application Settings - Local Machine Configuration");
            LOG.info("Removed setting '{}' from properties file: {}", key, propsFile.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("Error saving properties file after removal: {}", propsFile.getAbsolutePath(), e);
        }
    }
}
