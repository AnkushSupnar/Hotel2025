package com.frontend.repository;

import com.frontend.entity.MenuItem;
import com.frontend.entity.CategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /**
     * Find menu items by category
     */
    List<MenuItem> findByCategory(CategoryMaster category);

    /**
     * Find available menu items
     */
    List<MenuItem> findByAvailableTrue();

    /**
     * Find menu item by name
     */
    Optional<MenuItem> findByName(String name);

    /**
     * Find menu items by name containing (search)
     */
    List<MenuItem> findByNameContainingIgnoreCase(String name);

    /**
     * Find available menu items by category
     */
    List<MenuItem> findByCategoryAndAvailableTrue(CategoryMaster category);

    /**
     * Count menu items by category
     */
    long countByCategory(CategoryMaster category);
}
