package com.frontend.repository;

import com.frontend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Find users by employee ID
     */
    @Query("SELECT u FROM User u WHERE u.employee.id = :employeeId")
    List<User> findByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Find users by employee
     */
    @Query("SELECT u FROM User u WHERE u.employee = :employee")
    List<User> findByEmployee(@Param("employee") com.frontend.entity.Employee employee);

    /**
     * Count users for an employee
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.employee.id = :employeeId")
    long countByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Get all usernames
     */
    @Query("SELECT u.username FROM User u")
    List<String> findAllUsernames();
}
