package com.frontend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store mobile feature access per user role
 * Controls which features are accessible to which roles in mobile app
 */
@Entity
@Table(name = "mobile_feature_access", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"role", "feature_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileFeatureAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role", nullable = false, length = 50)
    private String role; // ADMIN, MANAGER, CASHIER, WAITER, etc.

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode; // e.g., VIEW_TABLES, TAKE_ORDER, VIEW_BILLS, etc.

    @Column(name = "feature_name", length = 100)
    private String featureName; // Human readable name

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isEnabled == null) {
            isEnabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
