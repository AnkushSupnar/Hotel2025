package com.frontend.repository;

import com.frontend.entity.CategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Find categories by purchase status
     */
    List<CategoryMaster> findByPurchase(String purchase);

    /**
     * Find categories by stock status
     */
    List<CategoryMaster> findByStock(String stock);

    /**
     * Find categories by both stock and purchase status
     */
    List<CategoryMaster> findByStockAndPurchase(String stock, String purchase);
}
