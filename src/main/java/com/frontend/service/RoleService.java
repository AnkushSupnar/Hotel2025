package com.frontend.service;

import com.frontend.entity.Role;
import com.frontend.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
}
