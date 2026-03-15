package com.frontend.repository;

import com.frontend.entity.ReducedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ReducedItem entity.
 * Handles data access for reduced/cancelled kitchen items.
 */
@Repository
public interface ReducedItemRepository extends JpaRepository<ReducedItem, Integer> {

    /**
     * Find all reduced items for a specific table
     */
    List<ReducedItem> findByTableNo(Integer tableNo);

    /**
     * Find all reduced items by a specific user
     */
    List<ReducedItem> findByReducedByUserId(Integer userId);

    /**
     * Find reduced items within a date range
     */
    @Query("SELECT r FROM ReducedItem r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<ReducedItem> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find reduced items for a table within a date range
     */
    @Query("SELECT r FROM ReducedItem r WHERE r.tableNo = :tableNo AND r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<ReducedItem> findByTableNoAndDateRange(@Param("tableNo") Integer tableNo,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Get total reduced amount for a date range
     */
    @Query("SELECT SUM(r.amt) FROM ReducedItem r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Float getTotalReducedAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get total reduced quantity for a specific item
     */
    @Query("SELECT SUM(r.reducedQty) FROM ReducedItem r WHERE r.itemName = :itemName")
    Float getTotalReducedQtyByItemName(@Param("itemName") String itemName);
}
