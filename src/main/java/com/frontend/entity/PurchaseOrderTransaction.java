package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a Purchase Order Transaction (items to be purchased)
 * Many PurchaseOrderTransactions belong to one PurchaseOrder
 * Contains only item details and quantity - no rate/amount
 */
@Entity
@Table(name = "purchase_order_transaction")
public class PurchaseOrderTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "qty", nullable = false)
    private Float qty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_no", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "item_code")
    private Integer itemCode;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

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
    public PurchaseOrderTransaction() {
    }

    public PurchaseOrderTransaction(String itemName, Float qty, PurchaseOrder purchaseOrder) {
        this.itemName = itemName;
        this.qty = qty;
        this.purchaseOrder = purchaseOrder;
    }

    public PurchaseOrderTransaction(String itemName, Float qty, PurchaseOrder purchaseOrder,
                                    Integer itemCode, Integer categoryId, String categoryName) {
        this.itemName = itemName;
        this.qty = qty;
        this.purchaseOrder = purchaseOrder;
        this.itemCode = itemCode;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Float getQty() {
        return qty;
    }

    public void setQty(Float qty) {
        this.qty = qty;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Integer getItemCode() {
        return itemCode;
    }

    public void setItemCode(Integer itemCode) {
        this.itemCode = itemCode;
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

    // Helper method to get order number
    public Integer getOrderNo() {
        return purchaseOrder != null ? purchaseOrder.getOrderNo() : null;
    }

    @Override
    public String toString() {
        return "PurchaseOrderTransaction{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", qty=" + qty +
                ", orderNo=" + getOrderNo() +
                ", itemCode=" + itemCode +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
