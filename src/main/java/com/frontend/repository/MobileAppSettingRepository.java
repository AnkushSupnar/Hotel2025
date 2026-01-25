package com.frontend.repository;

import com.frontend.entity.MobileAppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileAppSettingRepository extends JpaRepository<MobileAppSetting, Long> {

    Optional<MobileAppSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);

    void deleteBySettingKey(String settingKey);
}
