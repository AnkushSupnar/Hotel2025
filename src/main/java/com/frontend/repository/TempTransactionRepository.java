package com.frontend.repository;

import com.frontend.entity.TempTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TempTransaction entity
 */
@Repository
public interface TempTransactionRepository extends JpaRepository<TempTransaction, Integer> {

    /**
     * Find all transactions for a specific table number
     */
    List<TempTransaction> findByTableNo(Integer tableNo);

    /**
     * Find transaction by table number and item name
     */
    Optional<TempTransaction> findByTableNoAndItemName(Integer tableNo, String itemName);

    /**
     * Find transaction by table number, item name, and rate
     */
    Optional<TempTransaction> findByTableNoAndItemNameAndRate(Integer tableNo, String itemName, Float rate);

    /**
     * Delete all transactions for a specific table number
     */
    @Modifying
    @Query("DELETE FROM TempTransaction t WHERE t.tableNo = :tableNo")
    void deleteByTableNo(@Param("tableNo") Integer tableNo);

    /**
     * Check if any transactions exist for a table
     */
    boolean existsByTableNo(Integer tableNo);

    /**
     * Get total amount for a table
     */
    @Query("SELECT SUM(t.amt) FROM TempTransaction t WHERE t.tableNo = :tableNo")
    Float getTotalAmountByTableNo(@Param("tableNo") Integer tableNo);

    /**
     * Get total quantity for a table
     */
    @Query("SELECT SUM(t.qty) FROM TempTransaction t WHERE t.tableNo = :tableNo")
    Float getTotalQuantityByTableNo(@Param("tableNo") Integer tableNo);

    /**
     * Count items for a table
     */
    long countByTableNo(Integer tableNo);

    /**
     * Find items with printQty greater than 0 for a table (items that need kitchen printing)
     */
    @Query("SELECT t FROM TempTransaction t WHERE t.tableNo = :tableNo AND t.printQty > 0")
    List<TempTransaction> findPrintableItemsByTableNo(@Param("tableNo") Integer tableNo);
}
