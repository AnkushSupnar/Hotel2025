package com.frontend.repository;

import com.frontend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by role name
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * Check if role name exists
     */
    boolean existsByRoleName(String roleName);

    /**
     * Delete role by role name
     */
    void deleteByRoleName(String roleName);
}
