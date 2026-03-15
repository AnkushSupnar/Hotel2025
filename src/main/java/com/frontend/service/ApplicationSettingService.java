package com.frontend.service;

import com.frontend.entity.ApplicationSetting;
import com.frontend.repository.ApplicationSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApplicationSettingService {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationSettingService.class);

    @Autowired
    private ApplicationSettingRepository applicationSettingRepository;

    /**
     * Get all settings
     * @return list of all settings
     */
    public List<ApplicationSetting> getAllSettings() {
        LOG.info("Fetching all application settings");
        return applicationSettingRepository.findAll();
    }

    /**
     * Get setting by ID
     * @param id the setting ID
     * @return Optional containing the setting if found
     */
    public Optional<ApplicationSetting> getSettingById(Long id) {
        LOG.info("Fetching setting by id: {}", id);
        return applicationSettingRepository.findById(id);
    }

    /**
     * Get setting by name
     * @param settingName the setting name
     * @return Optional containing the setting if found
     */
    public Optional<ApplicationSetting> getSettingByName(String settingName) {
        LOG.info("Fetching setting by name: {}", settingName);
        return applicationSettingRepository.findBySettingName(settingName);
    }

    /**
     * Get setting value by name, returns null if not found
     * @param settingName the setting name
     * @return setting value or null
     */
    public String getSettingValue(String settingName) {
        return applicationSettingRepository.findBySettingName(settingName)
                .map(ApplicationSetting::getSettingValue)
                .orElse(null);
    }

    /**
     * Save or update a setting
     * If setting with the same name exists, it will be updated
     * @param settingName the setting name
     * @param settingValue the setting value
     * @return saved setting
     */
    public ApplicationSetting saveSetting(String settingName, String settingValue) {
        LOG.info("Saving setting: {} = {}", settingName, settingValue);

        Optional<ApplicationSetting> existingSetting = applicationSettingRepository.findBySettingName(settingName);

        ApplicationSetting setting;
        if (existingSetting.isPresent()) {
            // Update existing setting
            setting = existingSetting.get();
            setting.setSettingValue(settingValue);
            LOG.info("Updating existing setting: {}", settingName);
        } else {
            // Create new setting
            setting = ApplicationSetting.builder()
                    .settingName(settingName)
                    .settingValue(settingValue)
                    .build();
            LOG.info("Creating new setting: {}", settingName);
        }

        return applicationSettingRepository.save(setting);
    }

    /**
     * Save or update a setting entity
     * @param setting the setting to save
     * @return saved setting
     */
    public ApplicationSetting saveSetting(ApplicationSetting setting) {
        LOG.info("Saving setting entity: {}", setting.getSettingName());
        return applicationSettingRepository.save(setting);
    }

    /**
     * Delete setting by ID
     * @param id the setting ID to delete
     */
    public void deleteSetting(Long id) {
        LOG.info("Deleting setting by id: {}", id);
        applicationSettingRepository.deleteById(id);
    }

    /**
     * Delete setting by name
     * @param settingName the setting name to delete
     */
    public void deleteSettingByName(String settingName) {
        LOG.info("Deleting setting by name: {}", settingName);
        applicationSettingRepository.deleteBySettingName(settingName);
    }

    /**
     * Check if setting exists by name
     * @param settingName the setting name
     * @return true if exists, false otherwise
     */
    public boolean existsByName(String settingName) {
        return applicationSettingRepository.existsBySettingName(settingName);
    }
}
