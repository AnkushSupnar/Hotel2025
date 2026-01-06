package com.frontend.repository;

import com.frontend.entity.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentReceipt entity
 * Provides methods to query payment receipts (grouped payments)
 */
@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Integer> {

    /**
     * Find all receipts ordered by date and receipt number descending
     */
    List<PaymentReceipt> findAllByOrderByPaymentDateDescReceiptNoDesc();

    /**
     * Find receipts by supplier
     */
    List<PaymentReceipt> findBySupplierIdOrderByPaymentDateDescReceiptNoDesc(Integer supplierId);

    /**
     * Find receipts by date range
     */
    List<PaymentReceipt> findByPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Find receipts by supplier and date range
     */
    List<PaymentReceipt> findBySupplierIdAndPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
            Integer supplierId, LocalDate startDate, LocalDate endDate);

    /**
     * Find receipts by payment date
     */
    List<PaymentReceipt> findByPaymentDateOrderByReceiptNoDesc(LocalDate paymentDate);

    /**
     * Find receipt with bill payments eagerly loaded
     */
    @Query("SELECT pr FROM PaymentReceipt pr LEFT JOIN FETCH pr.billPayments WHERE pr.receiptNo = :receiptNo")
    Optional<PaymentReceipt> findByIdWithBillPayments(@Param("receiptNo") Integer receiptNo);

    /**
     * Find receipts by supplier with bill payments eagerly loaded
     */
    @Query("SELECT DISTINCT pr FROM PaymentReceipt pr LEFT JOIN FETCH pr.billPayments " +
           "WHERE pr.supplierId = :supplierId ORDER BY pr.paymentDate DESC, pr.receiptNo DESC")
    List<PaymentReceipt> findBySupplierIdWithBillPayments(@Param("supplierId") Integer supplierId);

    /**
     * Find receipts by date range with supplier eagerly loaded
     */
    @Query("SELECT pr FROM PaymentReceipt pr LEFT JOIN FETCH pr.supplier " +
           "WHERE pr.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pr.paymentDate DESC, pr.receiptNo DESC")
    List<PaymentReceipt> findByDateRangeWithSupplier(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total payments by date
     */
    @Query("SELECT COALESCE(SUM(pr.totalAmount), 0) FROM PaymentReceipt pr WHERE pr.paymentDate = :date")
    Double getTotalPaymentsByDate(@Param("date") LocalDate date);

    /**
     * Get total payments by date range
     */
    @Query("SELECT COALESCE(SUM(pr.totalAmount), 0) FROM PaymentReceipt pr " +
           "WHERE pr.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalPaymentsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total payments for a supplier
     */
    @Query("SELECT COALESCE(SUM(pr.totalAmount), 0) FROM PaymentReceipt pr WHERE pr.supplierId = :supplierId")
    Double getTotalPaymentsBySupplierId(@Param("supplierId") Integer supplierId);

    /**
     * Get total payments for supplier in date range
     */
    @Query("SELECT COALESCE(SUM(pr.totalAmount), 0) FROM PaymentReceipt pr " +
           "WHERE pr.supplierId = :supplierId AND pr.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalPaymentsBySupplierIdAndDateRange(
            @Param("supplierId") Integer supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count receipts by date
     */
    long countByPaymentDate(LocalDate paymentDate);

    /**
     * Count receipts by supplier
     */
    long countBySupplierId(Integer supplierId);

    /**
     * Find receipts by bank transaction ID
     */
    Optional<PaymentReceipt> findByBankTransactionId(Integer bankTransactionId);
}
