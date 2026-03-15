package com.frontend.repository;

import com.frontend.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PurchaseOrder entity
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    /**
     * Find all purchase orders by status
     */
    List<PurchaseOrder> findByStatus(String status);

    /**
     * Find all purchase orders by supplier ID
     */
    List<PurchaseOrder> findByPartyId(Integer partyId);

    /**
     * Find all purchase orders by order date
     */
    List<PurchaseOrder> findByOrderDate(LocalDate orderDate);

    /**
     * Find purchase orders by date range
     */
    List<PurchaseOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find purchase orders by supplier and date range
     */
    List<PurchaseOrder> findByPartyIdAndOrderDateBetween(Integer partyId, LocalDate startDate, LocalDate endDate);

    /**
     * Find purchase orders by status and date
     */
    List<PurchaseOrder> findByStatusAndOrderDate(String status, LocalDate orderDate);

    /**
     * Count purchase orders by date
     */
    long countByOrderDate(LocalDate orderDate);

    /**
     * Count purchase orders by status
     */
    long countByStatus(String status);

    /**
     * Get the last order number
     */
    @Query("SELECT MAX(p.orderNo) FROM PurchaseOrder p")
    Optional<Integer> findMaxOrderNo();

    /**
     * Find all purchase orders ordered by order date descending
     */
    List<PurchaseOrder> findAllByOrderByOrderDateDesc();

    /**
     * Find all purchase orders ordered by order number descending
     */
    List<PurchaseOrder> findAllByOrderByOrderNoDesc();

    /**
     * Find pending orders by supplier
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.partyId = :partyId AND p.status = 'PENDING'")
    List<PurchaseOrder> findPendingOrdersBySupplier(@Param("partyId") Integer partyId);

    /**
     * Find pending orders
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.status = 'PENDING' ORDER BY p.orderDate DESC")
    List<PurchaseOrder> findAllPendingOrders();

    /**
     * Get total quantity for orders in date range
     */
    @Query("SELECT SUM(p.totalQty) FROM PurchaseOrder p WHERE p.orderDate BETWEEN :startDate AND :endDate")
    Double getTotalQtyByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
