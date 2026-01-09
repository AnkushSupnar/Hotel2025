package com.frontend.service;

import com.frontend.entity.Role;
import com.frontend.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontend.enums.ScreenPermission;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing roles and their rights
 */
@Service
public class RoleService {

    private static final Logger LOG = LoggerFactory.getLogger(RoleService.class);

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Create a new role
     */
    @Transactional
    public Role createRole(String roleName, String rights) {
        LOG.info("Creating new role: {}", roleName);

        if (roleRepository.existsByRoleName(roleName)) {
            LOG.error("Role already exists: {}", roleName);
            throw new IllegalArgumentException("Role with name '" + roleName + "' already exists");
        }

        Role role = new Role(roleName, rights);
        Role savedRole = roleRepository.save(role);
        LOG.info("Role created successfully: {}", savedRole.getRoleId());

        return savedRole;
    }

    /**
     * Get all roles
     */
    public List<Role> getAllRoles() {
        LOG.debug("Fetching all roles");
        return roleRepository.findAll();
    }

    /**
     * Get role by ID
     */
    public Optional<Role> getRoleById(Long roleId) {
        LOG.debug("Fetching role by ID: {}", roleId);
        return roleRepository.findById(roleId);
    }

    /**
     * Get role by name
     */
    public Optional<Role> getRoleByName(String roleName) {
        LOG.debug("Fetching role by name: {}", roleName);
        return roleRepository.findByRoleName(roleName);
    }

    /**
     * Update role
     */
    @Transactional
    public Role updateRole(Long roleId, String roleName, String rights) {
        LOG.info("Updating role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        // Check if new role name is already taken by another role
        if (!role.getRoleName().equals(roleName) && roleRepository.existsByRoleName(roleName)) {
            LOG.error("Role name already exists: {}", roleName);
            throw new IllegalArgumentException("Role with name '" + roleName + "' already exists");
        }

        role.setRoleName(roleName);
        role.setRights(rights);

        Role updatedRole = roleRepository.save(role);
        LOG.info("Role updated successfully: {}", roleId);

        return updatedRole;
    }

    /**
     * Update only rights for a role
     */
    @Transactional
    public Role updateRoleRights(Long roleId, String rights) {
        LOG.info("Updating rights for role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        role.setRights(rights);
        Role updatedRole = roleRepository.save(role);
        LOG.info("Role rights updated successfully: {}", roleId);

        return updatedRole;
    }

    /**
     * Delete role by ID
     */
    @Transactional
    public void deleteRole(Long roleId) {
        LOG.info("Deleting role: {}", roleId);

        if (!roleRepository.existsById(roleId)) {
            LOG.error("Role not found: {}", roleId);
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }

        roleRepository.deleteById(roleId);
        LOG.info("Role deleted successfully: {}", roleId);
    }

    /**
     * Delete role by name
     */
    @Transactional
    public void deleteRoleByName(String roleName) {
        LOG.info("Deleting role by name: {}", roleName);

        if (!roleRepository.existsByRoleName(roleName)) {
            LOG.error("Role not found: {}", roleName);
            throw new IllegalArgumentException("Role not found with name: " + roleName);
        }

        roleRepository.deleteByRoleName(roleName);
        LOG.info("Role deleted successfully: {}", roleName);
    }

    /**
     * Check if role exists by name
     */
    public boolean roleExists(String roleName) {
        return roleRepository.existsByRoleName(roleName);
    }

    /**
     * Check if user has specific right
     * Rights are stored as comma-separated values like "create,view,edit"
     */
    public boolean hasRight(String roleName, String right) {
        LOG.debug("Checking if role {} has right {}", roleName, right);

        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            return false;
        }

        String rights = roleOpt.get().getRights();
        return rights != null && rights.toLowerCase().contains(right.toLowerCase());
    }

    // =====================================================
    // Screen Permission Methods
    // =====================================================

    /**
     * Check if a role has access to a specific screen
     * @param roleName the role to check
     * @param screen the screen permission to check
     * @return true if the role has access, false otherwise
     */
    public boolean hasScreenAccess(String roleName, ScreenPermission screen) {
        LOG.debug("Checking if role {} has access to screen {}", roleName, screen);

        // ADMIN always has full access
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return true;
        }

        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            return false;
        }

        String rights = roleOpt.get().getRights();
        if (rights == null || rights.trim().isEmpty()) {
            return false;
        }

        return Arrays.stream(rights.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(screen.name()));
    }

    /**
     * Check if a role has access to a screen by its FXML path
     * @param roleName the role to check
     * @param fxmlPath the FXML path of the screen
     * @return true if the role has access, false otherwise
     */
    public boolean hasScreenAccessByPath(String roleName, String fxmlPath) {
        ScreenPermission screen = ScreenPermission.fromFxmlPath(fxmlPath);
        if (screen == null) {
            // Unknown screens are allowed by default
            return true;
        }
        return hasScreenAccess(roleName, screen);
    }

    /**
     * Get all screen permissions for a role
     * @param roleName the role to get permissions for
     * @return set of ScreenPermission that the role has access to
     */
    public Set<ScreenPermission> getScreenPermissions(String roleName) {
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return EnumSet.allOf(ScreenPermission.class);
        }

        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            return Collections.emptySet();
        }

        String rights = roleOpt.get().getRights();
        if (rights == null || rights.trim().isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(rights.split(","))
                .map(String::trim)
                .map(name -> {
                    try {
                        return ScreenPermission.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Update screen permissions for a role
     * @param roleName the role to update
     * @param permissions the new set of permissions
     * @return the updated Role entity
     */
    @Transactional
    public Role updateScreenPermissions(String roleName, Set<ScreenPermission> permissions) {
        LOG.info("Updating screen permissions for role: {}", roleName);

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        String rightsString = permissions.stream()
                .map(ScreenPermission::name)
                .collect(Collectors.joining(","));

        role.setRights(rightsString);
        return roleRepository.save(role);
    }

    /**
     * Initialize default permissions for predefined roles.
     * Creates roles if they don't exist with sensible default permissions.
     */
    @Transactional
    public void initializeDefaultRolePermissions() {
        LOG.info("Initializing default role permissions");

        // ADMIN - Full access (handled in code, rights can be empty or "ALL")
        createRoleIfNotExists("ADMIN", "ALL");

        // MANAGER - All screens except User Rights
        createRoleIfNotExists("MANAGER",
                "DASHBOARD," +
                "BILLING,RECEIVE_PAYMENT," +
                "PURCHASE_ORDER,PURCHASE_INVOICE,PURCHASE_INVOICE_FROM_PO,PAY_RECEIPT," +
                "CATEGORY,ITEM,TABLE,CUSTOMER,EMPLOYEE,SUPPLIER,USER,BANK," +
                "SALES_REPORT,PURCHASE_REPORT,PAYMENT_RECEIVED_REPORT,PAY_RECEIPT_REPORT,REDUCED_ITEM_REPORT," +
                "APPLICATION_SETTINGS");

        // CASHIER - Billing and payment related screens + Dashboard
        createRoleIfNotExists("CASHIER",
                "DASHBOARD,BILLING,RECEIVE_PAYMENT,SALES_REPORT,PAYMENT_RECEIVED_REPORT");

        // WAITER - Billing only + Dashboard
        createRoleIfNotExists("WAITER", "DASHBOARD,BILLING");

        // USER - Basic access + Dashboard
        createRoleIfNotExists("USER", "DASHBOARD,BILLING,SALES_REPORT");
    }

    /**
     * Create a role if it doesn't exist
     */
    private void createRoleIfNotExists(String roleName, String rights) {
        if (!roleRepository.existsByRoleName(roleName)) {
            Role role = new Role(roleName, rights);
            roleRepository.save(role);
            LOG.info("Created default role: {} with rights: {}", roleName, rights);
        }
    }

    /**
     * Get all predefined role names
     * @return list of predefined role names
     */
    public List<String> getPredefinedRoleNames() {
        return Arrays.asList("ADMIN", "MANAGER", "CASHIER", "WAITER", "USER");
    }
}
