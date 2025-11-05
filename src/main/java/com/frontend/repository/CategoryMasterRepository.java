package com.frontend.repository;

import com.frontend.entity.CategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryMasterRepository extends JpaRepository<CategoryMaster, Integer> {

    /**
     * Find category by name
     */
    Optional<CategoryMaster> findByCategory(String category);

    /**
     * Check if category exists by name
     */
    boolean existsByCategory(String category);

    /**
     * Find category by name (case-insensitive)
     */
    Optional<CategoryMaster> findByCategoryIgnoreCase(String category);
}
