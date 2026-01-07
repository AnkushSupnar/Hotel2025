package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing reduced/cancelled kitchen items.
 * Tracks items that were already sent to kitchen but later reduced or removed.
 */
@Entity
@Table(name = "reduced_item")
public class ReducedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "reduced_qty", nullable = false)
    private Float reducedQty;

    @Column(name = "rate", nullable = false)
    private Float rate;

    @Column(name = "amt", nullable = false)
    private Float amt;

    @Column(name = "table_no", nullable = false)
    private Integer tableNo;

    @Column(name = "waitor_id")
    private Integer waitorId;

    @Column(name = "reduced_by_user_id")
    private Integer reducedByUserId;

    @Column(name = "reduced_by_user_name", length = 100)
    private String reducedByUserName;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ReducedItem() {
    }

    public ReducedItem(String itemName, Float reducedQty, Float rate, Integer tableNo,
                       Integer waitorId, Integer reducedByUserId, String reducedByUserName) {
        this.itemName = itemName;
        this.reducedQty = reducedQty;
        this.rate = rate;
        this.amt = reducedQty * rate;
        this.tableNo = tableNo;
        this.waitorId = waitorId;
        this.reducedByUserId = reducedByUserId;
        this.reducedByUserName = reducedByUserName;
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

    public Float getReducedQty() {
        return reducedQty;
    }

    public void setReducedQty(Float reducedQty) {
        this.reducedQty = reducedQty;
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

    public Integer getReducedByUserId() {
        return reducedByUserId;
    }

    public void setReducedByUserId(Integer reducedByUserId) {
        this.reducedByUserId = reducedByUserId;
    }

    public String getReducedByUserName() {
        return reducedByUserName;
    }

    public void setReducedByUserName(String reducedByUserName) {
        this.reducedByUserName = reducedByUserName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ReducedItem{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", reducedQty=" + reducedQty +
                ", rate=" + rate +
                ", amt=" + amt +
                ", tableNo=" + tableNo +
                ", waitorId=" + waitorId +
                ", reducedByUserId=" + reducedByUserId +
                ", reducedByUserName='" + reducedByUserName + '\'' +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
