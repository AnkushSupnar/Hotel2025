package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a payment allocation towards a Sales Bill (credit bill)
 * Supports partial payments - multiple payments can be made for a single bill
 * Links to SalesPaymentReceipt for grouped payment tracking
 */
@Entity
@Table(name = "sales_bill_payment")
public class SalesBillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "bill_no", nullable = false)
    private Integer billNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_no", referencedColumnName = "bill_no", insertable = false, updatable = false)
    private Bill bill;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_amount", nullable = false)
    private Double paymentAmount;

    @Column(name = "bank_id", nullable = false)
    private Integer bankId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Bank bank;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "receipt_no")
    private Integer receiptNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_no", referencedColumnName = "receipt_no", insertable = false, updatable = false)
    private SalesPaymentReceipt salesPaymentReceipt;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public SalesBillPayment() {
    }

    public SalesBillPayment(Integer billNo, Double paymentAmount, Integer bankId, String paymentMode) {
        this.billNo = billNo;
        this.paymentAmount = paymentAmount;
        this.bankId = bankId;
        this.paymentMode = paymentMode;
        this.paymentDate = LocalDate.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBillNo() {
        return billNo;
    }

    public void setBillNo(Integer billNo) {
        this.billNo = billNo;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Integer getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(Integer receiptNo) {
        this.receiptNo = receiptNo;
    }

    public SalesPaymentReceipt getSalesPaymentReceipt() {
        return salesPaymentReceipt;
    }

    public void setSalesPaymentReceipt(SalesPaymentReceipt salesPaymentReceipt) {
        this.salesPaymentReceipt = salesPaymentReceipt;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public String getBankName() {
        return bank != null ? bank.getBankName() : paymentMode;
    }

    public String getCustomerName() {
        return bill != null && bill.getCustomerId() != null
                ? "Customer #" + bill.getCustomerId() : "";
    }

    @Override
    public String toString() {
        return "SalesBillPayment{" +
                "id=" + id +
                ", billNo=" + billNo +
                ", paymentDate=" + paymentDate +
                ", paymentAmount=" + paymentAmount +
                ", bankId=" + bankId +
                ", paymentMode='" + paymentMode + '\'' +
                ", receiptNo=" + receiptNo +
                ", customerId=" + customerId +
                ", createdAt=" + createdAt +
                '}';
    }
}
