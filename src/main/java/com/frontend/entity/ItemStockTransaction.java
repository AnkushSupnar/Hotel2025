package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing Item Stock Transaction
 * Tracks all stock movements (additions and deductions)
 */
@Entity
@Table(name = "item_stock_transaction")
public class ItemStockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_code")
    private Integer itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // "PURCHASE" for add, "SALE" for reduce, "ADJUSTMENT" for manual

    @Column(name = "quantity", nullable = false)
    private Float quantity;

    @Column(name = "rate")
    private Float rate;

    @Column(name = "amount")
    private Float amount;

    @Column(name = "previous_stock")
    private Float previousStock;

    @Column(name = "new_stock")
    private Float newStock;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // "PURCHASE_BILL", "SALES_BILL", "ADJUSTMENT"

    @Column(name = "reference_no")
    private Integer referenceNo; // Bill number

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }

    // Constructors
    public ItemStockTransaction() {
    }

    public ItemStockTransaction(String itemName, String transactionType, Float quantity) {
        this.itemName = itemName;
        this.transactionType = transactionType;
        this.quantity = quantity;
    }

    public ItemStockTransaction(Integer itemCode, String itemName, Integer categoryId,
                                 String transactionType, Float quantity, Float rate,
                                 Float previousStock, Float newStock,
                                 String referenceType, Integer referenceNo) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.rate = rate;
        this.amount = quantity * rate;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.referenceType = referenceType;
        this.referenceNo = referenceNo;
        this.transactionDate = LocalDate.now();
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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Float getQuantity() {
        return quantity;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
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

    public Float getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(Float previousStock) {
        this.previousStock = previousStock;
    }

    public Float getNewStock() {
        return newStock;
    }

    public void setNewStock(Float newStock) {
        this.newStock = newStock;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(Integer referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ItemStockTransaction{" +
                "id=" + id +
                ", itemCode=" + itemCode +
                ", itemName='" + itemName + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", quantity=" + quantity +
                ", previousStock=" + previousStock +
                ", newStock=" + newStock +
                ", referenceType='" + referenceType + '\'' +
                ", referenceNo=" + referenceNo +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
