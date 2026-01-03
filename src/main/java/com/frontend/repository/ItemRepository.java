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
     * Find item by name (exact match)
     */
    Optional<Item> findByItemName(String itemName);

    /**
     * Find item by name (case-insensitive exact match)
     * Returns first matching item to handle duplicates
     */
    Optional<Item> findFirstByItemNameIgnoreCase(String itemName);

    /**
     * Find all items by name (case-insensitive exact match)
     * Use this when you need to check for duplicates
     */
    List<Item> findByItemNameIgnoreCase(String itemName);

    /**
     * Find item by category ID and name (case-insensitive)
     * This is the preferred method as items with same name can exist in different categories
     */
    Optional<Item> findFirstByCategoryIdAndItemNameIgnoreCase(Integer categoryId, String itemName);

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
    List<Item> findByCategoryIdAndItemNameContainingIgnoreCaseWithCategory(@Param("categoryId") Integer categoryId,
            @Param("itemName") String itemName);

    /* Get all item.item_name as List<String> */
    @Query("SELECT i.itemName FROM Item i")
    List<String> findAllItemNames();

    /**
     * Find item by category id and item code
     */
    @Query("SELECT i FROM Item i WHERE i.categoryId = :categoryId AND i.itemCode = :itemCode")
    Optional<Item> findByCategoryIdAndItemCode(@Param("categoryId") Integer categoryId,
            @Param("itemCode") Integer itemCode);

}
