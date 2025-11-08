package com.frontend.repository;

import com.frontend.entity.ApplicationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, Long> {

    /**
     * Find setting by setting name
     * @param settingName the setting name to search for
     * @return Optional containing the setting if found
     */
    Optional<ApplicationSetting> findBySettingName(String settingName);

    /**
     * Check if a setting exists by setting name
     * @param settingName the setting name to check
     * @return true if exists, false otherwise
     */
    boolean existsBySettingName(String settingName);

    /**
     * Delete setting by setting name
     * @param settingName the setting name to delete
     */
    void deleteBySettingName(String settingName);
}
