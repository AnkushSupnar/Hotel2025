package com.frontend.repository;

import com.frontend.entity.SalesPaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SalesPaymentReceipt entity
 * Provides methods to query sales payment receipts (customer payments for credit bills)
 */
@Repository
public interface SalesPaymentReceiptRepository extends JpaRepository<SalesPaymentReceipt, Integer> {

    /**
     * Find all receipts ordered by date and receipt number descending
     */
    List<SalesPaymentReceipt> findAllByOrderByPaymentDateDescReceiptNoDesc();

    /**
     * Find receipts by customer
     */
    List<SalesPaymentReceipt> findByCustomerIdOrderByPaymentDateDescReceiptNoDesc(Integer customerId);

    /**
     * Find receipts by date range
     */
    List<SalesPaymentReceipt> findByPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Find receipts by customer and date range
     */
    List<SalesPaymentReceipt> findByCustomerIdAndPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
            Integer customerId, LocalDate startDate, LocalDate endDate);

    /**
     * Find receipts by payment date
     */
    List<SalesPaymentReceipt> findByPaymentDateOrderByReceiptNoDesc(LocalDate paymentDate);

    /**
     * Find receipt with bill payments eagerly loaded
     */
    @Query("SELECT spr FROM SalesPaymentReceipt spr LEFT JOIN FETCH spr.billPayments WHERE spr.receiptNo = :receiptNo")
    Optional<SalesPaymentReceipt> findByIdWithBillPayments(@Param("receiptNo") Integer receiptNo);

    /**
     * Find receipts by customer with bill payments eagerly loaded
     */
    @Query("SELECT DISTINCT spr FROM SalesPaymentReceipt spr LEFT JOIN FETCH spr.billPayments " +
           "WHERE spr.customerId = :customerId ORDER BY spr.paymentDate DESC, spr.receiptNo DESC")
    List<SalesPaymentReceipt> findByCustomerIdWithBillPayments(@Param("customerId") Integer customerId);

    /**
     * Find receipts by date range with customer and bank eagerly loaded
     */
    @Query("SELECT spr FROM SalesPaymentReceipt spr LEFT JOIN FETCH spr.customer LEFT JOIN FETCH spr.bank " +
           "WHERE spr.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY spr.paymentDate DESC, spr.receiptNo DESC")
    List<SalesPaymentReceipt> findByDateRangeWithCustomer(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total receipts (payments received) by date
     */
    @Query("SELECT COALESCE(SUM(spr.totalAmount), 0) FROM SalesPaymentReceipt spr WHERE spr.paymentDate = :date")
    Double getTotalReceiptsByDate(@Param("date") LocalDate date);

    /**
     * Get total receipts by date range
     */
    @Query("SELECT COALESCE(SUM(spr.totalAmount), 0) FROM SalesPaymentReceipt spr " +
           "WHERE spr.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalReceiptsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total receipts for a customer
     */
    @Query("SELECT COALESCE(SUM(spr.totalAmount), 0) FROM SalesPaymentReceipt spr WHERE spr.customerId = :customerId")
    Double getTotalReceiptsByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Get total receipts for customer in date range
     */
    @Query("SELECT COALESCE(SUM(spr.totalAmount), 0) FROM SalesPaymentReceipt spr " +
           "WHERE spr.customerId = :customerId AND spr.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalReceiptsByCustomerIdAndDateRange(
            @Param("customerId") Integer customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count receipts by date
     */
    long countByPaymentDate(LocalDate paymentDate);

    /**
     * Count receipts by customer
     */
    long countByCustomerId(Integer customerId);

    /**
     * Find receipts by bank transaction ID
     */
    Optional<SalesPaymentReceipt> findByBankTransactionId(Integer bankTransactionId);
}
