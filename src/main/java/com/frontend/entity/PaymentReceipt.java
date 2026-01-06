package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Payment Receipt (Voucher) for supplier payments
 * One PaymentReceipt groups multiple BillPayment allocations made in a single payment session
 * This creates ONE bank transaction for the total amount
 */
@Entity
@Table(name = "payment_receipt")
public class PaymentReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_no")
    private Integer receiptNo;

    @Column(name = "supplier_id", nullable = false)
    private Integer supplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Supplier supplier;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

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

    @Column(name = "bills_count")
    private Integer billsCount = 1;

    @OneToMany(mappedBy = "paymentReceipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillPayment> billPayments = new ArrayList<>();

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
    public PaymentReceipt() {
    }

    public PaymentReceipt(Integer supplierId, Double totalAmount, Integer bankId, String paymentMode) {
        this.supplierId = supplierId;
        this.totalAmount = totalAmount;
        this.bankId = bankId;
        this.paymentMode = paymentMode;
        this.paymentDate = LocalDate.now();
    }

    // Getters and Setters
    public Integer getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(Integer receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
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

    public Integer getBillsCount() {
        return billsCount;
    }

    public void setBillsCount(Integer billsCount) {
        this.billsCount = billsCount;
    }

    public List<BillPayment> getBillPayments() {
        return billPayments;
    }

    public void setBillPayments(List<BillPayment> billPayments) {
        this.billPayments = billPayments;
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
    public void addBillPayment(BillPayment billPayment) {
        billPayments.add(billPayment);
        billPayment.setPaymentReceipt(this);
    }

    public void removeBillPayment(BillPayment billPayment) {
        billPayments.remove(billPayment);
        billPayment.setPaymentReceipt(null);
    }

    public String getSupplierName() {
        return supplier != null ? supplier.getName() : "";
    }

    public String getBankName() {
        return bank != null ? bank.getBankName() : paymentMode;
    }

    /**
     * Get a summary of bill numbers paid in this receipt
     */
    public String getBillsSummary() {
        if (billPayments == null || billPayments.isEmpty()) {
            return "";
        }
        if (billPayments.size() == 1) {
            return "#" + billPayments.get(0).getBillNo();
        }
        // Return first and last bill numbers
        int firstBill = billPayments.get(0).getBillNo();
        int lastBill = billPayments.get(billPayments.size() - 1).getBillNo();
        return "#" + firstBill + " - #" + lastBill;
    }

    @Override
    public String toString() {
        return "PaymentReceipt{" +
                "receiptNo=" + receiptNo +
                ", supplierId=" + supplierId +
                ", paymentDate=" + paymentDate +
                ", totalAmount=" + totalAmount +
                ", bankId=" + bankId +
                ", paymentMode='" + paymentMode + '\'' +
                ", billsCount=" + billsCount +
                ", bankTransactionId=" + bankTransactionId +
                ", createdAt=" + createdAt +
                '}';
    }
}
