package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_master")
public class CategoryMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 50)
    private String stock;

    @Column(length = 1)
    private String purchase = "N";

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
    public CategoryMaster() {
    }

    public CategoryMaster(String category, String stock) {
        this.category = category;
        this.stock = stock;
    }

    public CategoryMaster(String category, String stock, String purchase) {
        this.category = category;
        this.stock = stock;
        this.purchase = purchase;
    }

    public CategoryMaster(Integer id, String category, String stock) {
        this.id = id;
        this.category = category;
        this.stock = stock;
    }

    public CategoryMaster(Integer id, String category, String stock, String purchase) {
        this.id = id;
        this.category = category;
        this.stock = stock;
        this.purchase = purchase;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getPurchase() {
        return purchase;
    }

    public void setPurchase(String purchase) {
        this.purchase = purchase;
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

    @Override
    public String toString() {
        return "CategoryMaster{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", stock='" + stock + '\'' +
                ", purchase='" + purchase + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
