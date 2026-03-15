package com.frontend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store mobile application settings
 * Settings like JWT expiration, mobile access enabled, etc.
 */
@Entity
@Table(name = "mobile_app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileAppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_type", length = 50)
    private String settingType; // BOOLEAN, INTEGER, STRING, etc.

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public Boolean getBooleanValue() {
        return "true".equalsIgnoreCase(settingValue);
    }

    public Integer getIntegerValue() {
        try {
            return Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Long getLongValue() {
        try {
            return Long.parseLong(settingValue);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
