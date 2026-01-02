package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing Item Stock
 * Maintains current stock quantity for each item
 */
@Entity
@Table(name = "item_stock")
public class ItemStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_code")
    private Integer itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "stock", nullable = false)
    private Float stock = 0.0f;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "min_stock_level")
    private Float minStockLevel = 0.0f;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public ItemStock() {
    }

    public ItemStock(String itemName, Float stock) {
        this.itemName = itemName;
        this.stock = stock;
    }

    public ItemStock(Integer itemCode, String itemName, Integer categoryId, String categoryName, Float stock) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.stock = stock;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getItemCode() {
        return itemCode;
    }

    public void setItemCode(Integer itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Float getStock() {
        return stock;
    }

    public void setStock(Float stock) {
        this.stock = stock;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Float getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Float minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public void addStock(Float quantity) {
        this.stock = this.stock + quantity;
    }

    public void reduceStock(Float quantity) {
        this.stock = this.stock - quantity;
    }

    public boolean isLowStock() {
        return this.stock <= this.minStockLevel;
    }

    @Override
    public String toString() {
        return "ItemStock{" +
                "id=" + id +
                ", itemCode=" + itemCode +
                ", itemName='" + itemName + '\'' +
                ", categoryId=" + categoryId +
                ", stock=" + stock +
                ", unit='" + unit + '\'' +
                ", minStockLevel=" + minStockLevel +
                '}';
    }
}
