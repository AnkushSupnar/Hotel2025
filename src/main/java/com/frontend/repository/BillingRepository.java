package com.frontend.repository;

import com.frontend.entity.Billing;
import com.frontend.entity.Order;
import com.frontend.entity.PaymentStatus;
import com.frontend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    /**
     * Find billing by order
     */
    Optional<Billing> findByOrder(Order order);

    /**
     * Find billing by bill number
     */
    Optional<Billing> findByBillNumber(String billNumber);

    /**
     * Find billings by payment status
     */
    List<Billing> findByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Find billings created by user
     */
    List<Billing> findByCreatedBy(User user);

    /**
     * Find billings created between dates
     */
    List<Billing> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get today's billings
     */
    @Query("SELECT b FROM Billing b WHERE DATE(b.createdAt) = CURRENT_DATE ORDER BY b.createdAt DESC")
    List<Billing> findTodaysBillings();

    /**
     * Get total revenue for a date range
     */
    @Query("SELECT SUM(b.totalAmount) FROM Billing b WHERE b.paymentStatus = 'PAID' AND b.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count billings by payment status
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);
}
