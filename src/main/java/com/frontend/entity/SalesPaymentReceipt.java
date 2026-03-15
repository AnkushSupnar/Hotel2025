package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Payment Receipt for customer payments (sales credit bills)
 * One SalesPaymentReceipt groups multiple SalesBillPayment allocations made in a single payment session
 * This creates ONE bank transaction (DEPOSIT) for the total amount received
 */
@Entity
@Table(name = "sales_payment_receipt")
public class SalesPaymentReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_no")
    private Integer receiptNo;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Customer customer;

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

    @OneToMany(mappedBy = "salesPaymentReceipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalesBillPayment> billPayments = new ArrayList<>();

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
    public SalesPaymentReceipt() {
    }

    public SalesPaymentReceipt(Integer customerId, Double totalAmount, Integer bankId, String paymentMode) {
        this.customerId = customerId;
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

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public List<SalesBillPayment> getBillPayments() {
        return billPayments;
    }

    public void setBillPayments(List<SalesBillPayment> billPayments) {
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
    public void addBillPayment(SalesBillPayment billPayment) {
        billPayments.add(billPayment);
        billPayment.setSalesPaymentReceipt(this);
    }

    public void removeBillPayment(SalesBillPayment billPayment) {
        billPayments.remove(billPayment);
        billPayment.setSalesPaymentReceipt(null);
    }

    public String getCustomerName() {
        return customer != null ? customer.getFullName() : "";
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
        return "SalesPaymentReceipt{" +
                "receiptNo=" + receiptNo +
                ", customerId=" + customerId +
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
