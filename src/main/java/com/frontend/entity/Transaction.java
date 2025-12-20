package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a Transaction (items consumed in a bill)
 * Many Transactions belong to one Bill
 */
@Entity
@Table(name = "transaction")
public class Transaction {

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

    @Column(name = "amt", nullable = false)
    private Float amt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_no", nullable = false)
    private Bill bill;

    @Column(name = "item_code")
    private Integer itemCode;

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
    public Transaction() {
    }

    public Transaction(String itemName, Float qty, Float rate, Float amt, Bill bill) {
        this.itemName = itemName;
        this.qty = qty;
        this.rate = rate;
        this.amt = amt;
        this.bill = bill;
    }

    public Transaction(String itemName, Float qty, Float rate, Float amt, Bill bill,
                       Integer itemCode) {
        this.itemName = itemName;
        this.qty = qty;
        this.rate = rate;
        this.amt = amt;
        this.bill = bill;
        this.itemCode = itemCode;
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

    public Float getAmt() {
        return amt;
    }

    public void setAmt(Float amt) {
        this.amt = amt;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Integer getItemCode() {
        return itemCode;
    }

    public void setItemCode(Integer itemCode) {
        this.itemCode = itemCode;
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
        return bill != null ? bill.getBillNo() : null;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", qty=" + qty +
                ", rate=" + rate +
                ", amt=" + amt +
                ", billNo=" + getBillNo() +
                ", itemCode=" + itemCode +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
