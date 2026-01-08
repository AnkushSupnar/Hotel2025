package com.frontend.repository;

import com.frontend.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Bill entity
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Integer> {

    /**
     * Find all bills by status
     */
    List<Bill> findByStatus(String status);

    /**
     * Find all bills by customer ID
     */
    List<Bill> findByCustomerId(Integer customerId);

    /**
     * Find all bills by table number
     */
    List<Bill> findByTableNo(Integer tableNo);

    /**
     * Find all bills by waiter ID
     */
    List<Bill> findByWaitorId(Integer waitorId);

    /**
     * Find all bills by bill date
     */
    List<Bill> findByBillDate(String billDate);

    /**
     * Find all bills by user ID
     */
    List<Bill> findByUserId(Integer userId);

    /**
     * Find bills by status and date
     */
    List<Bill> findByStatusAndBillDate(String status, String billDate);

    /**
     * Find bills by status and date ordered by bill number ascending
     */
    List<Bill> findByStatusAndBillDateOrderByBillNoAsc(String status, String billDate);

    /**
     * Find bills by customer ID and status
     */
    List<Bill> findByCustomerIdAndStatus(Integer customerId, String status);

    /**
     * Get total bill amount for a date
     */
    @Query("SELECT SUM(b.billAmt) FROM Bill b WHERE b.billDate = :billDate")
    Float getTotalBillAmountByDate(@Param("billDate") String billDate);

    /**
     * Get total bill amount for a date and status
     */
    @Query("SELECT SUM(b.billAmt) FROM Bill b WHERE b.billDate = :billDate AND b.status = :status")
    Float getTotalBillAmountByDateAndStatus(@Param("billDate") String billDate, @Param("status") String status);

    /**
     * Get count of bills by date
     */
    long countByBillDate(String billDate);

    /**
     * Get count of bills by status
     */
    long countByStatus(String status);

    /**
     * Get the last bill number (for generating next bill number)
     */
    @Query("SELECT MAX(b.billNo) FROM Bill b")
    Optional<Integer> findMaxBillNo();

    /**
     * Find closed (unpaid) bills for a customer
     */
    @Query("SELECT b FROM Bill b WHERE b.customerId = :customerId AND b.status = 'CLOSE'")
    List<Bill> findClosedBillsByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Get total closed (unpaid) amount for a customer
     */
    @Query("SELECT SUM(b.billAmt - COALESCE(b.cashReceived, 0)) FROM Bill b WHERE b.customerId = :customerId AND b.status = 'CLOSE'")
    Float getTotalClosedAmountByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Find closed bill for a specific table
     * Returns the most recent closed bill for the table
     */
    @Query("SELECT b FROM Bill b WHERE b.tableNo = :tableNo AND b.status = 'CLOSE' ORDER BY b.billNo DESC")
    List<Bill> findClosedBillsByTableNo(@Param("tableNo") Integer tableNo);

    /**
     * Find bill by table number and status
     */
    Optional<Bill> findFirstByTableNoAndStatusOrderByBillNoDesc(Integer tableNo, String status);

    /**
     * Find the last paid or credit bill (most recent by bill number)
     */
    @Query("SELECT b FROM Bill b WHERE b.status IN ('PAID', 'CREDIT') ORDER BY b.billNo DESC")
    List<Bill> findLastPaidOrCreditBills();

    /**
     * Find all paid and credit bills ordered by bill number
     */
    @Query("SELECT b FROM Bill b WHERE b.status IN ('PAID', 'CREDIT') ORDER BY b.billNo ASC")
    List<Bill> findAllPaidAndCreditBills();

    /**
     * Find paid and credit bills by customer ID
     */
    @Query("SELECT b FROM Bill b WHERE b.customerId = :customerId AND b.status IN ('PAID', 'CREDIT') ORDER BY b.billNo DESC")
    List<Bill> findPaidAndCreditBillsByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Get total sales amount for paid and credit bills
     */
    @Query("SELECT COALESCE(SUM(b.netAmount), 0) FROM Bill b WHERE b.status IN ('PAID', 'CREDIT')")
    Float getTotalSalesAmount();

    /**
     * Get total sales amount for paid and credit bills by date
     */
    @Query("SELECT COALESCE(SUM(b.netAmount), 0) FROM Bill b WHERE b.billDate = :billDate AND b.status IN ('PAID', 'CREDIT')")
    Float getTotalSalesAmountByDate(@Param("billDate") String billDate);

    /**
     * Get total discount for paid and credit bills
     */
    @Query("SELECT COALESCE(SUM(b.discount), 0) FROM Bill b WHERE b.status IN ('PAID', 'CREDIT')")
    Float getTotalDiscount();

    /**
     * Count paid and credit bills
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.status IN ('PAID', 'CREDIT')")
    Long countPaidAndCreditBills();

    /**
     * Count paid and credit bills by date
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.billDate = :billDate AND b.status IN ('PAID', 'CREDIT')")
    Long countPaidAndCreditBillsByDate(@Param("billDate") String billDate);

    /**
     * Find paid and credit bills by date ordered by bill number
     */
    @Query("SELECT b FROM Bill b WHERE b.billDate = :billDate AND b.status IN ('PAID', 'CREDIT') ORDER BY b.billNo ASC")
    List<Bill> findPaidAndCreditBillsByDate(@Param("billDate") String billDate);

    // ============= Credit Bill Payment Queries =============

    /**
     * Find credit bills with pending balance for a customer
     * Returns bills where netAmount - paidAmount > 0
     */
    @Query("SELECT b FROM Bill b WHERE b.customerId = :customerId AND b.status = 'CREDIT' " +
           "AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0 ORDER BY b.billNo ASC")
    List<Bill> findCreditBillsWithPendingBalanceByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Get total pending amount for a customer (credit bills)
     */
    @Query("SELECT COALESCE(SUM(b.netAmount - COALESCE(b.paidAmount, 0)), 0) FROM Bill b " +
           "WHERE b.customerId = :customerId AND b.status = 'CREDIT'")
    Double getTotalPendingAmountByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Get all customers with pending credit bills
     */
    @Query("SELECT DISTINCT b.customerId FROM Bill b WHERE b.status = 'CREDIT' " +
           "AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0")
    List<Integer> findCustomerIdsWithPendingBills();

    /**
     * Count credit bills with pending balance for a customer
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.customerId = :customerId AND b.status = 'CREDIT' " +
           "AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0")
    Long countCreditBillsWithPendingBalanceByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Find all credit bills (unpaid or partially paid)
     */
    @Query("SELECT b FROM Bill b WHERE b.status = 'CREDIT' " +
           "AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0 ORDER BY b.billDate DESC, b.billNo DESC")
    List<Bill> findAllCreditBillsWithPendingBalance();

    /**
     * Get total credit balance across all customers
     */
    @Query("SELECT COALESCE(SUM(b.netAmount - COALESCE(b.paidAmount, 0)), 0) FROM Bill b " +
           "WHERE b.status = 'CREDIT' AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0")
    Double getTotalCreditBalance();

    // ============= Dashboard Queries =============

    /**
     * Find recent bills ordered by creation time (for dashboard)
     */
    List<Bill> findTop10ByOrderByCreatedAtDesc();

    /**
     * Count bills by date and status (for dashboard order status)
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.billDate = :billDate AND b.status = :status")
    Long countByBillDateAndStatus(@Param("billDate") String billDate, @Param("status") String status);

    /**
     * Find bills by date with transactions eagerly loaded (for top selling items)
     */
    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.transactions WHERE b.billDate = :billDate")
    List<Bill> findByBillDateWithTransactions(@Param("billDate") String billDate);

    /**
     * Count distinct active tables by bill status (for dashboard table status)
     * Tables with CLOSE status bills are considered active/occupied
     */
    @Query("SELECT COUNT(DISTINCT b.tableNo) FROM Bill b WHERE b.status = :status AND b.tableNo IS NOT NULL")
    Long countDistinctActiveTablesByStatus(@Param("status") String status);
}
