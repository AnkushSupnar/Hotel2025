package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Purchase Order
 * One PurchaseOrder contains many PurchaseOrderTransactions (items to be purchased)
 * This is created before the actual purchase - contains no payment/amount details
 */
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_no")
    private Integer orderNo;

    @Column(name = "party_id")
    private Integer partyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Supplier supplier;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "total_qty")
    private Double totalQty = 0.0;

    @Column(name = "total_items")
    private Integer totalItems = 0;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "status", length = 20)
    private String status = "PENDING"; // PENDING, COMPLETED, CANCELLED

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseOrderTransaction> transactions = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public PurchaseOrder() {
    }

    public PurchaseOrder(Integer partyId, LocalDate orderDate, String remarks) {
        this.partyId = partyId;
        this.orderDate = orderDate;
        this.remarks = remarks;
    }

    // Getters and Setters
    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(Integer partyId) {
        this.partyId = partyId;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Double getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Double totalQty) {
        this.totalQty = totalQty;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PurchaseOrderTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PurchaseOrderTransaction> transactions) {
        this.transactions = transactions;
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

    // Helper method to add transaction
    public void addTransaction(PurchaseOrderTransaction transaction) {
        transactions.add(transaction);
        transaction.setPurchaseOrder(this);
    }

    // Helper method to remove transaction
    public void removeTransaction(PurchaseOrderTransaction transaction) {
        transactions.remove(transaction);
        transaction.setPurchaseOrder(null);
    }

    // Helper method to get supplier name
    public String getSupplierName() {
        return supplier != null ? supplier.getName() : "";
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "orderNo=" + orderNo +
                ", partyId=" + partyId +
                ", orderDate=" + orderDate +
                ", totalQty=" + totalQty +
                ", totalItems=" + totalItems +
                ", status='" + status + '\'' +
                ", remarks='" + remarks + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
