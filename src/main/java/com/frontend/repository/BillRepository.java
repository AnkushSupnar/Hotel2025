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
}
