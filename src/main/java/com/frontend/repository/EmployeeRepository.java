package com.frontend.repository;

import com.frontend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Employee entity
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    /**
     * Find employee by contact number
     */
    Optional<Employee> findByContact(String contact);

    /**
     * Find employees by first name (case-insensitive)
     */
    List<Employee> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Find employees by last name (case-insensitive)
     */
    List<Employee> findByLastNameContainingIgnoreCase(String lastName);

    /**
     * Find employees by contact containing
     */
    List<Employee> findByContactContaining(String contact);

    /**
     * Find employees by designation
     */
    List<Employee> findByDesignation(String designation);

    /**
     * Find employees by status
     */
    List<Employee> findByStatus(String status);

    /**
     * Find employees by salary type
     */
    List<Employee> findBySalaryType(String salaryType);

    /**
     * Check if contact exists
     */
    boolean existsByContact(String contact);

    /**
     * Find all employees ordered by first name
     */
    List<Employee> findAllByOrderByFirstNameAsc();

    /**
     * Find employees by designation and status
     */
    List<Employee> findByDesignationAndStatus(String designation, String status);
}
