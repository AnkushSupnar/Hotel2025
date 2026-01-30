package com.frontend.repository;

import com.frontend.entity.KitchenOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KitchenOrderItemRepository extends JpaRepository<KitchenOrderItem, Integer> {
}
