package com.frontend.repository;

import com.frontend.entity.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemStockRepository extends JpaRepository<ItemStock, Integer> {

    /**
     * Find stock by item code
     */
    Optional<ItemStock> findByItemCode(Integer itemCode);

    /**
     * Find stock by item name
     */
    Optional<ItemStock> findByItemName(String itemName);

    /**
     * Find stock by item name (case-insensitive)
     */
    Optional<ItemStock> findByItemNameIgnoreCase(String itemName);

    /**
     * Find all stocks by category
     */
    List<ItemStock> findByCategoryId(Integer categoryId);

    /**
     * Find all low stock items
     */
    @Query("SELECT s FROM ItemStock s WHERE s.stock <= s.minStockLevel")
    List<ItemStock> findLowStockItems();

    /**
     * Find items with zero stock
     */
    List<ItemStock> findByStock(Float stock);

    /**
     * Find items with stock greater than zero
     */
    @Query("SELECT s FROM ItemStock s WHERE s.stock > 0")
    List<ItemStock> findInStockItems();

    /**
     * Find items with stock less than or equal to given quantity
     */
    @Query("SELECT s FROM ItemStock s WHERE s.stock <= :quantity")
    List<ItemStock> findByStockLessThanEqual(@Param("quantity") Float quantity);

    /**
     * Check if item exists by item code
     */
    boolean existsByItemCode(Integer itemCode);

    /**
     * Check if item exists by item name
     */
    boolean existsByItemName(String itemName);

    /**
     * Get total stock value
     */
    @Query("SELECT SUM(s.stock) FROM ItemStock s")
    Float getTotalStock();

    /**
     * Get stock count by category
     */
    @Query("SELECT COUNT(s) FROM ItemStock s WHERE s.categoryId = :categoryId")
    Long countByCategoryId(@Param("categoryId") Integer categoryId);
}
