package com.frontend.repository;

import com.frontend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Find users by role
     */
    List<User> findByRole(String role);

    /**
     * Get all usernames
     */
    @Query("SELECT u.username FROM User u")
    List<String> findAllUsernames();
}
