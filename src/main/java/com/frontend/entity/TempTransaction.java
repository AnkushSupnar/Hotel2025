package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing temporary transaction records for billing
 */
@Entity
@Table(name = "temp_transaction")
public class TempTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "itemName", nullable = false, length = 100)
    private String itemName;

    @Column(name = "qty", nullable = false)
    private Float qty;

    @Column(name = "rate", nullable = false)
    private Float rate;

    @Column(name = "amt", nullable = false)
    private Float amt;

    @Column(name = "tableNo", nullable = false)
    private Integer tableNo;

    @Column(name = "waitorId")
    private Integer waitorId;

    @Column(name = "printQty")
    private Float printQty;

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
    public TempTransaction() {
    }

    public TempTransaction(String itemName, Float qty, Float rate, Float amt, Integer tableNo) {
        this.itemName = itemName;
        this.qty = qty;
        this.rate = rate;
        this.amt = amt;
        this.tableNo = tableNo;
    }

    public TempTransaction(String itemName, Float qty, Float rate, Float amt, Integer tableNo, Integer waitorId,
            Float printQty) {
        this.itemName = itemName;
        this.qty = qty;
        this.rate = rate;
        this.amt = amt;
        this.tableNo = tableNo;
        this.waitorId = waitorId;
        this.printQty = printQty;
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

    public Integer getTableNo() {
        return tableNo;
    }

    public void setTableNo(Integer tableNo) {
        this.tableNo = tableNo;
    }

    public Integer getWaitorId() {
        return waitorId;
    }

    public void setWaitorId(Integer waitorId) {
        this.waitorId = waitorId;
    }

    public Float getPrintQty() {
        return printQty;
    }

    public void setPrintQty(Float printQty) {
        this.printQty = printQty;
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
        return "TempTransaction{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", qty=" + qty +
                ", rate=" + rate +
                ", amt=" + amt +
                ", tableNo=" + tableNo +
                ", waitorId=" + waitorId +
                ", printQty=" + printQty +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
