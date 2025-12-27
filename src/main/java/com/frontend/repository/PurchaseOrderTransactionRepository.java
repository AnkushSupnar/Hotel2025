package com.frontend.repository;

import com.frontend.entity.PurchaseOrderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PurchaseOrderTransaction entity
 */
@Repository
public interface PurchaseOrderTransactionRepository extends JpaRepository<PurchaseOrderTransaction, Integer> {

    /**
     * Find all transactions by order number
     */
    @Query("SELECT t FROM PurchaseOrderTransaction t WHERE t.purchaseOrder.orderNo = :orderNo")
    List<PurchaseOrderTransaction> findByOrderNo(@Param("orderNo") Integer orderNo);

    /**
     * Find transactions by item code
     */
    List<PurchaseOrderTransaction> findByItemCode(Integer itemCode);

    /**
     * Find transactions by category ID
     */
    List<PurchaseOrderTransaction> findByCategoryId(Integer categoryId);

    /**
     * Delete all transactions by order number
     */
    @Query("DELETE FROM PurchaseOrderTransaction t WHERE t.purchaseOrder.orderNo = :orderNo")
    void deleteByOrderNo(@Param("orderNo") Integer orderNo);

    /**
     * Get total quantity by item code
     */
    @Query("SELECT SUM(t.qty) FROM PurchaseOrderTransaction t WHERE t.itemCode = :itemCode")
    Double getTotalQtyByItemCode(@Param("itemCode") Integer itemCode);

    /**
     * Get total quantity by category
     */
    @Query("SELECT SUM(t.qty) FROM PurchaseOrderTransaction t WHERE t.categoryId = :categoryId")
    Double getTotalQtyByCategoryId(@Param("categoryId") Integer categoryId);

    /**
     * Count transactions by order number
     */
    @Query("SELECT COUNT(t) FROM PurchaseOrderTransaction t WHERE t.purchaseOrder.orderNo = :orderNo")
    long countByOrderNo(@Param("orderNo") Integer orderNo);
}
