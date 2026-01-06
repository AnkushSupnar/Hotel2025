package com.frontend.repository;

import com.frontend.entity.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for BillPayment entity
 * Handles payment history for purchase bills
 */
@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, Integer> {

    /**
     * Find all payments for a specific bill
     */
    List<BillPayment> findByBillNoOrderByPaymentDateDescIdDesc(Integer billNo);

    /**
     * Find payments by bank ID
     */
    List<BillPayment> findByBankIdOrderByPaymentDateDescIdDesc(Integer bankId);

    /**
     * Find payments by supplier ID
     */
    List<BillPayment> findBySupplierIdOrderByPaymentDateDescIdDesc(Integer supplierId);

    /**
     * Find payments by date
     */
    List<BillPayment> findByPaymentDateOrderByIdDesc(LocalDate paymentDate);

    /**
     * Find payments by date range
     */
    List<BillPayment> findByPaymentDateBetweenOrderByPaymentDateDescIdDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Find payments by supplier and date range
     */
    List<BillPayment> findBySupplierIdAndPaymentDateBetweenOrderByPaymentDateDescIdDesc(
            Integer supplierId, LocalDate startDate, LocalDate endDate);

    /**
     * Get total payments for a specific bill
     */
    @Query("SELECT COALESCE(SUM(bp.paymentAmount), 0) FROM BillPayment bp WHERE bp.billNo = :billNo")
    Double getTotalPaymentsByBillNo(@Param("billNo") Integer billNo);

    /**
     * Get total payments by date range
     */
    @Query("SELECT COALESCE(SUM(bp.paymentAmount), 0) FROM BillPayment bp " +
           "WHERE bp.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalPaymentsByDateRange(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Get total payments by supplier
     */
    @Query("SELECT COALESCE(SUM(bp.paymentAmount), 0) FROM BillPayment bp WHERE bp.supplierId = :supplierId")
    Double getTotalPaymentsBySupplierId(@Param("supplierId") Integer supplierId);

    /**
     * Get total payments by supplier and date range
     */
    @Query("SELECT COALESCE(SUM(bp.paymentAmount), 0) FROM BillPayment bp " +
           "WHERE bp.supplierId = :supplierId AND bp.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalPaymentsBySupplierIdAndDateRange(@Param("supplierId") Integer supplierId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * Get total payments by bank and date range
     */
    @Query("SELECT COALESCE(SUM(bp.paymentAmount), 0) FROM BillPayment bp " +
           "WHERE bp.bankId = :bankId AND bp.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalPaymentsByBankIdAndDateRange(@Param("bankId") Integer bankId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Count payments for a bill
     */
    long countByBillNo(Integer billNo);

    /**
     * Find all payments ordered by date desc
     */
    List<BillPayment> findAllByOrderByPaymentDateDescIdDesc();

    /**
     * Get today's total payments
     */
    @Query("SELECT COALESCE(SUM(bp.paymentAmount), 0) FROM BillPayment bp WHERE bp.paymentDate = :today")
    Double getTodayTotalPayments(@Param("today") LocalDate today);

    // ============= Methods with Eager Fetch for Lazy Loading Fix =============

    /**
     * Find all payments with eager fetch of purchaseBill and supplier
     */
    @Query("SELECT bp FROM BillPayment bp " +
           "LEFT JOIN FETCH bp.purchaseBill pb " +
           "LEFT JOIN FETCH pb.supplier " +
           "ORDER BY bp.paymentDate DESC, bp.id DESC")
    List<BillPayment> findAllWithDetailsOrderByPaymentDateDescIdDesc();

    /**
     * Find payments by date range with eager fetch
     */
    @Query("SELECT bp FROM BillPayment bp " +
           "LEFT JOIN FETCH bp.purchaseBill pb " +
           "LEFT JOIN FETCH pb.supplier " +
           "WHERE bp.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY bp.paymentDate DESC, bp.id DESC")
    List<BillPayment> findByPaymentDateBetweenWithDetails(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find payments by supplier and date range with eager fetch
     */
    @Query("SELECT bp FROM BillPayment bp " +
           "LEFT JOIN FETCH bp.purchaseBill pb " +
           "LEFT JOIN FETCH pb.supplier " +
           "WHERE bp.supplierId = :supplierId AND bp.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY bp.paymentDate DESC, bp.id DESC")
    List<BillPayment> findBySupplierIdAndPaymentDateBetweenWithDetails(
            @Param("supplierId") Integer supplierId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ============= Receipt-based Queries =============

    /**
     * Find payments by receipt number
     */
    List<BillPayment> findByReceiptNoOrderByBillNoAsc(Integer receiptNo);

    /**
     * Find payments by receipt number with bill details
     */
    @Query("SELECT bp FROM BillPayment bp " +
           "LEFT JOIN FETCH bp.purchaseBill pb " +
           "WHERE bp.receiptNo = :receiptNo " +
           "ORDER BY bp.billNo ASC")
    List<BillPayment> findByReceiptNoWithBillDetails(@Param("receiptNo") Integer receiptNo);
}
