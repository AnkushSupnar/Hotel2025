package com.frontend.repository;

import com.frontend.entity.ItemStockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemStockTransactionRepository extends JpaRepository<ItemStockTransaction, Integer> {

    /**
     * Find transactions by item code
     */
    List<ItemStockTransaction> findByItemCode(Integer itemCode);

    /**
     * Find transactions by item name
     */
    List<ItemStockTransaction> findByItemName(String itemName);

    /**
     * Find transactions by item name (case-insensitive)
     */
    List<ItemStockTransaction> findByItemNameIgnoreCase(String itemName);

    /**
     * Find transactions by type
     */
    List<ItemStockTransaction> findByTransactionType(String transactionType);

    /**
     * Find transactions by date
     */
    List<ItemStockTransaction> findByTransactionDate(LocalDate date);

    /**
     * Find transactions by date range
     */
    List<ItemStockTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by reference type and number
     */
    List<ItemStockTransaction> findByReferenceTypeAndReferenceNo(String referenceType, Integer referenceNo);

    /**
     * Find transactions by category
     */
    List<ItemStockTransaction> findByCategoryId(Integer categoryId);

    /**
     * Find purchase transactions
     */
    @Query("SELECT t FROM ItemStockTransaction t WHERE t.transactionType = 'PURCHASE' ORDER BY t.createdAt DESC")
    List<ItemStockTransaction> findPurchaseTransactions();

    /**
     * Find sale transactions
     */
    @Query("SELECT t FROM ItemStockTransaction t WHERE t.transactionType = 'SALE' ORDER BY t.createdAt DESC")
    List<ItemStockTransaction> findSaleTransactions();

    /**
     * Get total purchase quantity for an item
     */
    @Query("SELECT SUM(t.quantity) FROM ItemStockTransaction t WHERE t.itemCode = :itemCode AND t.transactionType = 'PURCHASE'")
    Float getTotalPurchaseQuantity(@Param("itemCode") Integer itemCode);

    /**
     * Get total sale quantity for an item
     */
    @Query("SELECT SUM(t.quantity) FROM ItemStockTransaction t WHERE t.itemCode = :itemCode AND t.transactionType = 'SALE'")
    Float getTotalSaleQuantity(@Param("itemCode") Integer itemCode);

    /**
     * Get transactions for item by date range
     */
    @Query("SELECT t FROM ItemStockTransaction t WHERE t.itemCode = :itemCode AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<ItemStockTransaction> findByItemCodeAndDateRange(
            @Param("itemCode") Integer itemCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get recent transactions
     */
    @Query("SELECT t FROM ItemStockTransaction t ORDER BY t.createdAt DESC")
    List<ItemStockTransaction> findRecentTransactions();

    /**
     * Delete transactions by reference
     */
    void deleteByReferenceTypeAndReferenceNo(String referenceType, Integer referenceNo);
}
