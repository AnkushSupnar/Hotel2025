package com.frontend.repository;

import com.frontend.entity.SalesBillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for SalesBillPayment entity
 * Provides methods to query payment allocations for sales bills
 */
@Repository
public interface SalesBillPaymentRepository extends JpaRepository<SalesBillPayment, Integer> {

    /**
     * Find all payments for a bill
     */
    List<SalesBillPayment> findByBillNoOrderByPaymentDateDesc(Integer billNo);

    /**
     * Find all payments by customer
     */
    List<SalesBillPayment> findByCustomerIdOrderByPaymentDateDesc(Integer customerId);

    /**
     * Find all payments for a receipt
     */
    List<SalesBillPayment> findByReceiptNoOrderByBillNoAsc(Integer receiptNo);

    /**
     * Find payments by date range
     */
    List<SalesBillPayment> findByPaymentDateBetweenOrderByPaymentDateDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Get total payments for a bill
     */
    @Query("SELECT COALESCE(SUM(sbp.paymentAmount), 0) FROM SalesBillPayment sbp WHERE sbp.billNo = :billNo")
    Double getTotalPaymentsByBillNo(@Param("billNo") Integer billNo);

    /**
     * Get total payments for a customer
     */
    @Query("SELECT COALESCE(SUM(sbp.paymentAmount), 0) FROM SalesBillPayment sbp WHERE sbp.customerId = :customerId")
    Double getTotalPaymentsByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Get total payments by date
     */
    @Query("SELECT COALESCE(SUM(sbp.paymentAmount), 0) FROM SalesBillPayment sbp WHERE sbp.paymentDate = :date")
    Double getTotalPaymentsByDate(@Param("date") LocalDate date);

    /**
     * Get total payments by date range
     */
    @Query("SELECT COALESCE(SUM(sbp.paymentAmount), 0) FROM SalesBillPayment sbp " +
           "WHERE sbp.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalPaymentsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count payments for a bill
     */
    long countByBillNo(Integer billNo);

    /**
     * Count payments for a receipt
     */
    long countByReceiptNo(Integer receiptNo);
}
