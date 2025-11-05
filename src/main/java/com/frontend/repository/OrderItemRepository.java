package com.frontend.repository;

import com.frontend.entity.OrderItem;
import com.frontend.entity.Order;
import com.frontend.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find order items by order
     */
    List<OrderItem> findByOrder(Order order);

    /**
     * Find order items by menu item
     */
    List<OrderItem> findByMenuItem(MenuItem menuItem);

    /**
     * Count order items by order
     */
    long countByOrder(Order order);
}
