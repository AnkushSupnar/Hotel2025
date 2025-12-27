package com.frontend.repository;

import com.frontend.entity.Employees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Employees entity - uses 'employees' table
 */
@Repository
public interface EmployeesRepository extends JpaRepository<Employees, Integer> {

    /**
     * Find employee by mobile number
     */
    Optional<Employees> findByMobileNo(String mobileNo);

    /**
     * Find employees by first name (case-insensitive)
     */
    List<Employees> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Find employees by last name (case-insensitive)
     */
    List<Employees> findByLastNameContainingIgnoreCase(String lastName);

    /**
     * Find employees by mobile number containing
     */
    List<Employees> findByMobileNoContaining(String mobileNo);

    /**
     * Find employees by designation
     */
    List<Employees> findByDesignation(String designation);

    /**
     * Find employees by designation (case-insensitive)
     */
    List<Employees> findByDesignationIgnoreCase(String designation);

    /**
     * Find active employees by designation
     */
    List<Employees> findByDesignationAndActiveStatusTrue(String designation);

    /**
     * Find active employees
     */
    List<Employees> findByActiveStatusTrue();

    /**
     * Find all employees ordered by first name
     */
    List<Employees> findAllByOrderByFirstNameAsc();

    /**
     * Find active employees ordered by first name
     */
    List<Employees> findByActiveStatusTrueOrderByFirstNameAsc();

    /**
     * Check if mobile number exists
     */
    boolean existsByMobileNo(String mobileNo);

    /**
     * Check if aadhar number exists
     */
    boolean existsByAadharNo(String aadharNo);

    /**
     * Find employees by city
     */
    List<Employees> findByCity(String city);

    /**
     * Find employees by district
     */
    List<Employees> findByDistrict(String district);

    /**
     * Get waiter names (employees with designation 'Waitor' or 'Waiter')
     */
    @Query("SELECT e FROM Employees e WHERE LOWER(e.designation) IN ('waitor', 'waiter') AND e.activeStatus = true ORDER BY e.firstName")
    List<Employees> findActiveWaiters();

    /**
     * Search employees by name
     */
    @Query("SELECT e FROM Employees e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Employees> searchByName(String name);
}
