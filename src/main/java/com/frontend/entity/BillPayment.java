package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a payment made towards a Purchase Bill
 * Supports partial payments - multiple payments can be made for a single bill
 */
@Entity
@Table(name = "bill_payment")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "bill_no", nullable = false)
    private Integer billNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_no", referencedColumnName = "bill_no", insertable = false, updatable = false)
    private PurchaseBill purchaseBill;

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

    @Column(name = "cheque_no", length = 50)
    private String chequeNo;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "bank_transaction_id")
    private Integer bankTransactionId;

    @Column(name = "supplier_id")
    private Integer supplierId;

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
    public BillPayment() {
    }

    public BillPayment(Integer billNo, Double paymentAmount, Integer bankId, String paymentMode) {
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

    public PurchaseBill getPurchaseBill() {
        return purchaseBill;
    }

    public void setPurchaseBill(PurchaseBill purchaseBill) {
        this.purchaseBill = purchaseBill;
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

    public String getChequeNo() {
        return chequeNo;
    }

    public void setChequeNo(String chequeNo) {
        this.chequeNo = chequeNo;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getBankTransactionId() {
        return bankTransactionId;
    }

    public void setBankTransactionId(Integer bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
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

    public String getSupplierName() {
        return purchaseBill != null && purchaseBill.getSupplier() != null
                ? purchaseBill.getSupplier().getName() : "";
    }

    @Override
    public String toString() {
        return "BillPayment{" +
                "id=" + id +
                ", billNo=" + billNo +
                ", paymentDate=" + paymentDate +
                ", paymentAmount=" + paymentAmount +
                ", bankId=" + bankId +
                ", paymentMode='" + paymentMode + '\'' +
                ", chequeNo='" + chequeNo + '\'' +
                ", referenceNo='" + referenceNo + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
