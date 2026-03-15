package com.frontend.service;

import com.frontend.entity.Employees;
import com.frontend.entity.User;
import com.frontend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for User operations
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        try {
            LOG.info("Fetching all users");
            return userRepository.findAllByOrderByUsernameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching all users", e);
            throw new RuntimeException("Error fetching users: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by ID
     */
    public User getUserById(Long id) {
        try {
            LOG.info("Fetching user by ID: {}", id);
            return userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        } catch (Exception e) {
            LOG.error("Error fetching user by ID: {}", id, e);
            throw new RuntimeException("Error fetching user: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        try {
            LOG.info("Fetching user by username: {}", username);
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            LOG.error("Error fetching user by username: {}", username, e);
            throw new RuntimeException("Error fetching user: " + e.getMessage(), e);
        }
    }

    /**
     * Search users by username
     */
    public List<User> searchByUsername(String username) {
        try {
            LOG.info("Searching users by username: {}", username);
            return userRepository.findByUsernameContainingIgnoreCase(username);
        } catch (Exception e) {
            LOG.error("Error searching users by username: {}", username, e);
            throw new RuntimeException("Error searching users: " + e.getMessage(), e);
        }
    }

    /**
     * Get users by role
     */
    public List<User> getUsersByRole(String role) {
        try {
            LOG.info("Fetching users by role: {}", role);
            return userRepository.findByRole(role);
        } catch (Exception e) {
            LOG.error("Error fetching users by role: {}", role, e);
            throw new RuntimeException("Error fetching users: " + e.getMessage(), e);
        }
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if employee already has a user account
     */
    public boolean existsByEmployee(Employees employee) {
        return userRepository.existsByEmployee(employee);
    }

    /**
     * Create new user
     */
    @Transactional
    public User createUser(User user) {
        try {
            LOG.info("Creating new user: {}", user.getUsername());

            // Validate username is unique
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new RuntimeException("Username '" + user.getUsername() + "' already exists");
            }

            // Validate employee doesn't already have a user account
            if (user.getEmployee() != null) {
                if (userRepository.existsByEmployee(user.getEmployee())) {
                    throw new RuntimeException("Employee already has a user account");
                }
            }

            // Validate password
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Password is required");
            }

            User savedUser = userRepository.save(user);
            LOG.info("User created successfully with ID: {}", savedUser.getId());
            return savedUser;

        } catch (Exception e) {
            LOG.error("Error creating user", e);
            throw new RuntimeException("Error creating user: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing user
     */
    @Transactional
    public User updateUser(Long id, User user) {
        try {
            LOG.info("Updating user with ID: {}", id);

            User existingUser = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            // Check if username is being changed and if new username already exists
            if (!existingUser.getUsername().equals(user.getUsername())) {
                if (userRepository.existsByUsername(user.getUsername())) {
                    throw new RuntimeException("Username '" + user.getUsername() + "' already exists");
                }
            }

            // Update fields
            existingUser.setUsername(user.getUsername());
            existingUser.setRole(user.getRole());

            // Only update password if provided
            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                existingUser.setPassword(user.getPassword());
            }

            // Update employee link if provided
            if (user.getEmployee() != null) {
                existingUser.setEmployee(user.getEmployee());
            }

            User updatedUser = userRepository.save(existingUser);
            LOG.info("User updated successfully with ID: {}", updatedUser.getId());
            return updatedUser;

        } catch (Exception e) {
            LOG.error("Error updating user with ID: {}", id, e);
            throw new RuntimeException("Error updating user: " + e.getMessage(), e);
        }
    }

    /**
     * Delete user by ID
     */
    @Transactional
    public void deleteUser(Long id) {
        try {
            LOG.info("Deleting user with ID: {}", id);

            if (!userRepository.existsById(id)) {
                throw new RuntimeException("User not found with ID: " + id);
            }

            userRepository.deleteById(id);
            LOG.info("User deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting user with ID: {}", id, e);
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    /**
     * Change user password
     */
    @Transactional
    public User changePassword(Long id, String newPassword) {
        try {
            LOG.info("Changing password for user ID: {}", id);

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new RuntimeException("New password is required");
            }

            user.setPassword(newPassword);
            User updatedUser = userRepository.save(user);
            LOG.info("Password changed successfully for user ID: {}", id);
            return updatedUser;

        } catch (Exception e) {
            LOG.error("Error changing password for user ID: {}", id, e);
            throw new RuntimeException("Error changing password: " + e.getMessage(), e);
        }
    }

    /**
     * Check if user exists by ID
     */
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
}
