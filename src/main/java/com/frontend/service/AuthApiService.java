package com.frontend.service;

import com.frontend.entity.Employee;
import com.frontend.entity.User;
import com.frontend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for authentication using database
 * Note: Passwords are stored as plain text for development purposes
 */
@Service
public class AuthApiService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthApiService.class);

    private final UserRepository userRepository;

    @Autowired
    public AuthApiService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticate user with database (plain text password comparison)
     * Returns User entity for desktop application session
     */
    public User login(String username, String password) {
        LOG.debug("Attempting login for user: {}", username);

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            LOG.error("Login failed: User not found - {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        User user = userOptional.get();

        // Verify password (plain text comparison)
        if (!password.equals(user.getPassword())) {
            LOG.error("Login failed: Invalid password for user - {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        LOG.info("Login successful for user: {} (Role: {})", username, user.getRole());

        // Log employee information if linked
        if (user.getEmployee() != null) {
            LOG.info("User linked to employee: {} - {} (ID: {})",
                    user.getEmployee().getFullName(),
                    user.getEmployee().getDesignation(),
                    user.getEmployee().getEmployeeId());
        }

        return user;
    }

    /**
     * Register new user in database (plain text password)
     */
    public boolean register(String username, String password, String role) {
        LOG.debug("Attempting registration for user: {} with role: {}", username, role);

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            LOG.error("Registration failed: Username already exists - {}", username);
            throw new RuntimeException("Username already exists");
        }

        // Create new user with plain text password
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Store password as plain text
        user.setRole(role.toUpperCase());

        userRepository.save(user);

        LOG.info("Registration successful for user: {}", username);
        return true;
    }

    /**
     * Register new user with employee link in database (plain text password)
     */
    public boolean registerWithEmployee(String username, String password, String role, Employee employee) {
        LOG.debug("Attempting registration for user: {} with role: {} and employee: {}",
                username, role, employee != null ? employee.getEmployeeId() : null);

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            LOG.error("Registration failed: Username already exists - {}", username);
            throw new RuntimeException("Username already exists");
        }

        // Validate employee
        if (employee == null) {
            LOG.error("Registration failed: Invalid employee");
            throw new RuntimeException("Valid employee is required for user registration");
        }

        // Create new user with plain text password and employee link
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Store password as plain text
        user.setRole(role.toUpperCase());
        user.setEmployee(employee);

        userRepository.save(user);

        LOG.info("Registration successful for user: {} linked to employee: {}", username, employee.getEmployeeId());
        return true;
    }

    /**
     * Get list of all usernames from database
     */
    public List<String> getAllUserNames() {
        LOG.debug("Fetching all usernames from database");

        List<String> usernames = userRepository.findAllUsernames();
        LOG.info("Successfully fetched {} usernames", usernames.size());
        return usernames;
    }

    /**
     * Test database connection
     */
    public boolean testConnection() {
        try {
            userRepository.count();
            LOG.debug("Database connection test successful");
            return true;
        } catch (Exception e) {
            LOG.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
}