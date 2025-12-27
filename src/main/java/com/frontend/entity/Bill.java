package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Bill/Invoice
 * One Bill contains many Transactions (items consumed)
 */
@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_no")
    private Integer billNo;

    @Column(name = "bill_amt", nullable = false)
    private Float billAmt;

    @Column(name = "discount")
    private Float discount = 0.0f;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "waitor_id")
    private Integer waitorId;

    @Column(name = "table_no")
    private Integer tableNo;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "bill_date", length = 20)
    private String billDate;

    @Column(name = "bill_time", length = 20)
    private String billTime;

    @Column(name = "paymode", length = 50)
    private String paymode;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "cash_received")
    private Float cashReceived = 0.0f;

    @Column(name = "return_amount")
    private Float returnAmount = 0.0f;

    @Column(name = "net_amount")
    private Float netAmount = 0.0f;

    @Column(name = "total_qty")
    private Float totalQty = 0.0f;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "bank_id")
    private Integer bankId;

    @Column(name = "close_at")
    private LocalDateTime closeAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

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
    public Bill() {
    }

    public Bill(Float billAmt, Float discount, Integer customerId, Integer waitorId,
                Integer tableNo, Integer userId, String billDate, String billTime,
                String paymode, String status) {
        this.billAmt = billAmt;
        this.discount = discount;
        this.customerId = customerId;
        this.waitorId = waitorId;
        this.tableNo = tableNo;
        this.userId = userId;
        this.billDate = billDate;
        this.billTime = billTime;
        this.paymode = paymode;
        this.status = status;
    }

    // Getters and Setters
    public Integer getBillNo() {
        return billNo;
    }

    public void setBillNo(Integer billNo) {
        this.billNo = billNo;
    }

    public Float getBillAmt() {
        return billAmt;
    }

    public void setBillAmt(Float billAmt) {
        this.billAmt = billAmt;
    }

    public Float getDiscount() {
        return discount;
    }

    public void setDiscount(Float discount) {
        this.discount = discount;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getWaitorId() {
        return waitorId;
    }

    public void setWaitorId(Integer waitorId) {
        this.waitorId = waitorId;
    }

    public Integer getTableNo() {
        return tableNo;
    }

    public void setTableNo(Integer tableNo) {
        this.tableNo = tableNo;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getBillDate() {
        return billDate;
    }

    public void setBillDate(String billDate) {
        this.billDate = billDate;
    }

    public String getBillTime() {
        return billTime;
    }

    public void setBillTime(String billTime) {
        this.billTime = billTime;
    }

    public String getPaymode() {
        return paymode;
    }

    public void setPaymode(String paymode) {
        this.paymode = paymode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Float getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(Float cashReceived) {
        this.cashReceived = cashReceived;
    }

    public Float getReturnAmount() {
        return returnAmount;
    }

    public void setReturnAmount(Float returnAmount) {
        this.returnAmount = returnAmount;
    }

    public Float getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Float netAmount) {
        this.netAmount = netAmount;
    }

    public Float getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Float totalQty) {
        this.totalQty = totalQty;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(LocalDateTime closeAt) {
        this.closeAt = closeAt;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
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
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setBill(this);
    }

    // Helper method to remove transaction
    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setBill(null);
    }

    @Override
    public String toString() {
        return "Bill{" +
                "billNo=" + billNo +
                ", billAmt=" + billAmt +
                ", discount=" + discount +
                ", customerId=" + customerId +
                ", waitorId=" + waitorId +
                ", tableNo=" + tableNo +
                ", userId=" + userId +
                ", billDate='" + billDate + '\'' +
                ", billTime='" + billTime + '\'' +
                ", paymode='" + paymode + '\'' +
                ", status='" + status + '\'' +
                ", cashReceived=" + cashReceived +
                ", netAmount=" + netAmount +
                ", totalQty=" + totalQty +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
