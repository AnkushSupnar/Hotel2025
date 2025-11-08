package com.frontend.repository;

import com.frontend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Customer entity
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /**
     * Find customer by customer key
     */
    Optional<Customer> findByCustomerKey(String customerKey);

    /**
     * Find customer by mobile number
     */
    Optional<Customer> findByMobileNo(String mobileNo);

    /**
     * Find customer by email
     */
    Optional<Customer> findByEmailId(String emailId);

    /**
     * Find customers by first name (case-insensitive)
     */
    List<Customer> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Find customers by last name (case-insensitive)
     */
    List<Customer> findByLastNameContainingIgnoreCase(String lastName);

    /**
     * Find customers by mobile number containing
     */
    List<Customer> findByMobileNoContaining(String mobileNo);

    /**
     * Find customers by city
     */
    List<Customer> findByCity(String city);

    /**
     * Find customers by district
     */
    List<Customer> findByDistrict(String district);

    /**
     * Check if customer exists by customer key
     */
    boolean existsByCustomerKey(String customerKey);

    /**
     * Check if customer exists by mobile number
     */
    boolean existsByMobileNo(String mobileNo);

    /**
     * Check if customer exists by email
     */
    boolean existsByEmailId(String emailId);

    /**
     * Find all customers ordered by first name
     */
    List<Customer> findAllByOrderByFirstNameAsc();
}
