package com.frontend.repository;

import com.frontend.entity.Order;
import com.frontend.entity.OrderStatus;
import com.frontend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders by table number
     */
    List<Order> findByTableNumber(Integer tableNumber);

    /**
     * Find orders by customer name
     */
    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Find orders by created by user
     */
    List<Order> findByCreatedBy(User user);

    /**
     * Find orders created between dates
     */
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find recent orders (latest first)
     */
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders();

    /**
     * Find orders by status and created date
     */
    List<Order> findByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get today's orders
     */
    @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE ORDER BY o.createdAt DESC")
    List<Order> findTodaysOrders();

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);
}
