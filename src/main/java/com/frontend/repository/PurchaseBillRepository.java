package com.frontend.repository;

import com.frontend.entity.PurchaseBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PurchaseBill entity
 */
@Repository
public interface PurchaseBillRepository extends JpaRepository<PurchaseBill, Integer> {

    /**
     * Find all purchase bills by status
     */
    List<PurchaseBill> findByStatus(String status);

    /**
     * Find all purchase bills by supplier ID
     */
    List<PurchaseBill> findByPartyId(Integer partyId);

    /**
     * Find all purchase bills by bill date
     */
    List<PurchaseBill> findByBillDate(LocalDate billDate);

    /**
     * Find purchase bills by date range
     */
    List<PurchaseBill> findByBillDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find purchase bills by supplier and date range
     */
    List<PurchaseBill> findByPartyIdAndBillDateBetween(Integer partyId, LocalDate startDate, LocalDate endDate);

    /**
     * Find purchase bills by status and date
     */
    List<PurchaseBill> findByStatusAndBillDate(String status, LocalDate billDate);

    /**
     * Find purchase bills by reference number
     */
    Optional<PurchaseBill> findByReffNo(String reffNo);

    /**
     * Get total purchase amount by date
     */
    @Query("SELECT SUM(p.amount) FROM PurchaseBill p WHERE p.billDate = :billDate")
    Double getTotalPurchaseAmountByDate(@Param("billDate") LocalDate billDate);

    /**
     * Get total purchase amount by supplier
     */
    @Query("SELECT SUM(p.amount) FROM PurchaseBill p WHERE p.partyId = :partyId")
    Double getTotalPurchaseAmountBySupplier(@Param("partyId") Integer partyId);

    /**
     * Get total purchase amount by date range
     */
    @Query("SELECT SUM(p.amount) FROM PurchaseBill p WHERE p.billDate BETWEEN :startDate AND :endDate")
    Double getTotalPurchaseAmountByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count purchase bills by date
     */
    long countByBillDate(LocalDate billDate);

    /**
     * Count purchase bills by status
     */
    long countByStatus(String status);

    /**
     * Get the last bill number
     */
    @Query("SELECT MAX(p.billNo) FROM PurchaseBill p")
    Optional<Integer> findMaxBillNo();

    /**
     * Find all purchase bills ordered by bill date descending
     */
    List<PurchaseBill> findAllByOrderByBillDateDesc();

    /**
     * Find all purchase bills ordered by bill number descending
     */
    List<PurchaseBill> findAllByOrderByBillNoDesc();

    /**
     * Find pending/unpaid bills by supplier
     */
    @Query("SELECT p FROM PurchaseBill p WHERE p.partyId = :partyId AND p.status = 'PENDING'")
    List<PurchaseBill> findPendingBillsBySupplier(@Param("partyId") Integer partyId);

    /**
     * Get total pending amount for a supplier
     */
    @Query("SELECT SUM(p.netAmount) FROM PurchaseBill p WHERE p.partyId = :partyId AND p.status = 'PENDING'")
    Double getTotalPendingAmountBySupplier(@Param("partyId") Integer partyId);
}
