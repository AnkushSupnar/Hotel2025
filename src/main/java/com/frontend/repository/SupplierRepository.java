package com.frontend.repository;

import com.frontend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Supplier entity
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    /**
     * Find supplier by name
     */
    Optional<Supplier> findByName(String name);

    /**
     * Find supplier by contact
     */
    Optional<Supplier> findByContact(String contact);

    /**
     * Find supplier by email
     */
    Optional<Supplier> findByEmail(String email);

    /**
     * Find supplier by GST number
     */
    Optional<Supplier> findByGstNo(String gstNo);

    /**
     * Find suppliers by name (case-insensitive)
     */
    List<Supplier> findByNameContainingIgnoreCase(String name);

    /**
     * Find suppliers by city
     */
    List<Supplier> findByCity(String city);

    /**
     * Find suppliers by state
     */
    List<Supplier> findByState(String state);

    /**
     * Find active suppliers
     */
    List<Supplier> findByIsActiveTrue();

    /**
     * Find all suppliers ordered by name
     */
    List<Supplier> findAllByOrderByNameAsc();

    /**
     * Find active suppliers ordered by name
     */
    List<Supplier> findByIsActiveTrueOrderByNameAsc();

    /**
     * Check if supplier exists by name
     */
    boolean existsByName(String name);

    /**
     * Check if supplier exists by contact
     */
    boolean existsByContact(String contact);

    /**
     * Check if supplier exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Check if supplier exists by GST number
     */
    boolean existsByGstNo(String gstNo);
}
