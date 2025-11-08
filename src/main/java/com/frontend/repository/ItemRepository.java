package com.frontend.repository;

import com.frontend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Item entity
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    /**
     * Find all items with categories eagerly loaded
     */
    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category")
    List<Item> findAllWithCategory();

    /**
     * Find item by ID with category eagerly loaded
     */
    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category WHERE i.id = :id")
    Optional<Item> findByIdWithCategory(Integer id);

    /**
     * Find item by name
     */
    Optional<Item> findByItemName(String itemName);

    /**
     * Find items by category ID
     */
    List<Item> findByCategoryId(Integer categoryId);

    /**
     * Find items by category ID with category eagerly loaded
     */
    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category WHERE i.categoryId = :categoryId")
    List<Item> findByCategoryIdWithCategory(@Param("categoryId") Integer categoryId);

    /**
     * Find item by item code
     */
    Optional<Item> findByItemCode(Integer itemCode);

    /**
     * Check if item exists by name
     */
    boolean existsByItemName(String itemName);

    /**
     * Check if item exists by item code
     */
    boolean existsByItemCode(Integer itemCode);

    /**
     * Find items by name containing (case-insensitive search)
     */
    List<Item> findByItemNameContainingIgnoreCase(String itemName);

    /**
     * Find items by name containing with category eagerly loaded
     */
    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category WHERE LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))")
    List<Item> findByItemNameContainingIgnoreCaseWithCategory(@Param("itemName") String itemName);

    /**
     * Find items by category ID and name containing
     */
    List<Item> findByCategoryIdAndItemNameContainingIgnoreCase(Integer categoryId, String itemName);

    /**
     * Find items by category ID and name containing with category eagerly loaded
     */
    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category WHERE i.categoryId = :categoryId AND LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))")
    List<Item> findByCategoryIdAndItemNameContainingIgnoreCaseWithCategory(@Param("categoryId") Integer categoryId, @Param("itemName") String itemName);
}
