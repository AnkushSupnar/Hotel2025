package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing Bank Transactions
 * Tracks all deposits and withdrawals for each bank account
 */
@Entity
@Table(name = "bank_transaction")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "particulars", length = 255)
    private String particulars;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "cheque_no", length = 50)
    private String chequeNo;

    @Column(name = "bank_id", nullable = false)
    private Integer bankId;

    @Column(name = "deposit")
    private Double deposit = 0.0;

    @Column(name = "withdraw")
    private Double withdraw = 0.0;

    @Column(name = "balance")
    private Double balance = 0.0;

    @Column(name = "transaction_type", length = 20)
    private String transactionType; // DEPOSIT, WITHDRAW

    @Column(name = "reference_type", length = 50)
    private String referenceType; // BILL_PAYMENT, PURCHASE, EXPENSE, MANUAL, etc.

    @Column(name = "reference_id")
    private Integer referenceId; // billNo, purchaseId, etc.

    @Column(name = "remarks", length = 500)
    private String remarks;

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
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public BankTransaction() {
    }

    public BankTransaction(Integer bankId, String particulars, Double deposit, Double withdraw) {
        this.bankId = bankId;
        this.particulars = particulars;
        this.deposit = deposit != null ? deposit : 0.0;
        this.withdraw = withdraw != null ? withdraw : 0.0;
        this.transactionDate = LocalDate.now();
        this.transactionType = deposit != null && deposit > 0 ? "DEPOSIT" : "WITHDRAW";
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getParticulars() {
        return particulars;
    }

    public void setParticulars(String particulars) {
        this.particulars = particulars;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getChequeNo() {
        return chequeNo;
    }

    public void setChequeNo(String chequeNo) {
        this.chequeNo = chequeNo;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public Double getDeposit() {
        return deposit;
    }

    public void setDeposit(Double deposit) {
        this.deposit = deposit;
    }

    public Double getWithdraw() {
        return withdraw;
    }

    public void setWithdraw(Double withdraw) {
        this.withdraw = withdraw;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "BankTransaction{" +
                "id=" + id +
                ", particulars='" + particulars + '\'' +
                ", transactionDate=" + transactionDate +
                ", bankId=" + bankId +
                ", deposit=" + deposit +
                ", withdraw=" + withdraw +
                ", balance=" + balance +
                ", transactionType='" + transactionType + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", referenceId=" + referenceId +
                '}';
    }
}
