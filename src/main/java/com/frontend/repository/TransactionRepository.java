package com.frontend.repository;

import com.frontend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Transaction entity
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * Find all transactions for a specific bill
     */
    @Query("SELECT t FROM Transaction t WHERE t.bill.billNo = :billNo")
    List<Transaction> findByBillNo(@Param("billNo") Integer billNo);

    /**
     * Find transactions by item name
     */
    List<Transaction> findByItemName(String itemName);

    /**
     * Find transactions by item name containing (search)
     */
    List<Transaction> findByItemNameContainingIgnoreCase(String itemName);

    /**
     * Get total amount for a bill
     */
    @Query("SELECT SUM(t.amt) FROM Transaction t WHERE t.bill.billNo = :billNo")
    Float getTotalAmountByBillNo(@Param("billNo") Integer billNo);

    /**
     * Get total quantity for a bill
     */
    @Query("SELECT SUM(t.qty) FROM Transaction t WHERE t.bill.billNo = :billNo")
    Float getTotalQuantityByBillNo(@Param("billNo") Integer billNo);

    /**
     * Count items in a bill
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.bill.billNo = :billNo")
    long countByBillNo(@Param("billNo") Integer billNo);

    /**
     * Get item-wise sales summary for a date range
     */
    @Query("SELECT t.itemName, SUM(t.qty), SUM(t.amt) FROM Transaction t " +
           "WHERE t.bill.billDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.itemName ORDER BY SUM(t.amt) DESC")
    List<Object[]> getItemWiseSalesSummary(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    /**
     * Find transactions by item code
     */
    List<Transaction> findByItemCode(Integer itemCode);

    /**
     * Delete all transactions for a specific bill
     * clearAutomatically ensures stale entities are removed from persistence context
     * flushAutomatically ensures pending changes are flushed before delete
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Transaction t WHERE t.bill.billNo = :billNo")
    void deleteByBillNo(@Param("billNo") Integer billNo);
}
