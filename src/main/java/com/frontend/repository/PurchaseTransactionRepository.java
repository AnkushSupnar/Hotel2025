package com.frontend.repository;

import com.frontend.entity.PurchaseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PurchaseTransaction entity
 */
@Repository
public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, Integer> {

    /**
     * Find all transactions by bill number
     */
    @Query("SELECT pt FROM PurchaseTransaction pt WHERE pt.purchaseBill.billNo = :billNo")
    List<PurchaseTransaction> findByBillNo(@Param("billNo") Integer billNo);

    /**
     * Find all transactions by item name
     */
    List<PurchaseTransaction> findByItemName(String itemName);

    /**
     * Find all transactions by item code
     */
    List<PurchaseTransaction> findByItemCode(Integer itemCode);

    /**
     * Find all transactions by category ID
     */
    List<PurchaseTransaction> findByCategoryId(Integer categoryId);

    /**
     * Delete transactions by bill number
     */
    @Modifying
    @Query("DELETE FROM PurchaseTransaction pt WHERE pt.purchaseBill.billNo = :billNo")
    void deleteByBillNo(@Param("billNo") Integer billNo);

    /**
     * Get total quantity purchased for an item
     */
    @Query("SELECT SUM(pt.qty) FROM PurchaseTransaction pt WHERE pt.itemCode = :itemCode")
    Float getTotalQuantityByItemCode(@Param("itemCode") Integer itemCode);

    /**
     * Get total amount spent on an item
     */
    @Query("SELECT SUM(pt.amount) FROM PurchaseTransaction pt WHERE pt.itemCode = :itemCode")
    Float getTotalAmountByItemCode(@Param("itemCode") Integer itemCode);

    /**
     * Get total quantity purchased for an item by name
     */
    @Query("SELECT SUM(pt.qty) FROM PurchaseTransaction pt WHERE pt.itemName = :itemName")
    Float getTotalQuantityByItemName(@Param("itemName") String itemName);

    /**
     * Count transactions by bill number
     */
    @Query("SELECT COUNT(pt) FROM PurchaseTransaction pt WHERE pt.purchaseBill.billNo = :billNo")
    Long countByBillNo(@Param("billNo") Integer billNo);
}
