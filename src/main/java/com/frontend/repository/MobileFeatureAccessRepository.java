package com.frontend.repository;

import com.frontend.entity.MobileFeatureAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MobileFeatureAccessRepository extends JpaRepository<MobileFeatureAccess, Long> {

    List<MobileFeatureAccess> findByRole(String role);

    List<MobileFeatureAccess> findByRoleAndIsEnabledTrue(String role);

    Optional<MobileFeatureAccess> findByRoleAndFeatureCode(String role, String featureCode);

    boolean existsByRoleAndFeatureCode(String role, String featureCode);

    void deleteByRole(String role);

    void deleteByRoleAndFeatureCode(String role, String featureCode);

    @Query("SELECT DISTINCT m.role FROM MobileFeatureAccess m")
    List<String> findAllRoles();

    @Query("SELECT DISTINCT m.featureCode FROM MobileFeatureAccess m")
    List<String> findAllFeatureCodes();
}
