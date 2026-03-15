package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a Purchase Transaction (items purchased in a purchase bill)
 * Many PurchaseTransactions belong to one PurchaseBill
 */
@Entity
@Table(name = "purchase_transaction")
public class PurchaseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "qty", nullable = false)
    private Float qty;

    @Column(name = "rate", nullable = false)
    private Float rate;

    @Column(name = "amount", nullable = false)
    private Float amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_no", nullable = false)
    private PurchaseBill purchaseBill;

    @Column(name = "item_code")
    private Integer itemCode;

    @Column(name = "category_id")
    private Integer categoryId;

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
    public PurchaseTransaction() {
    }

    public PurchaseTransaction(String itemName, Float qty, Float rate, Float amount, PurchaseBill purchaseBill) {
        this.itemName = itemName;
        this.qty = qty;
        this.rate = rate;
        this.amount = amount;
        this.purchaseBill = purchaseBill;
    }

    public PurchaseTransaction(String itemName, Float qty, Float rate, Float amount,
                               PurchaseBill purchaseBill, Integer itemCode, Integer categoryId) {
        this.itemName = itemName;
        this.qty = qty;
        this.rate = rate;
        this.amount = amount;
        this.purchaseBill = purchaseBill;
        this.itemCode = itemCode;
        this.categoryId = categoryId;
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

    public Float getRate() {
        return rate;
    }

    public void setRate(Float rate) {
        this.rate = rate;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public PurchaseBill getPurchaseBill() {
        return purchaseBill;
    }

    public void setPurchaseBill(PurchaseBill purchaseBill) {
        this.purchaseBill = purchaseBill;
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

    // Helper method to get bill number
    public Integer getBillNo() {
        return purchaseBill != null ? purchaseBill.getBillNo() : null;
    }

    @Override
    public String toString() {
        return "PurchaseTransaction{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", qty=" + qty +
                ", rate=" + rate +
                ", amount=" + amount +
                ", billNo=" + getBillNo() +
                ", itemCode=" + itemCode +
                ", categoryId=" + categoryId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
