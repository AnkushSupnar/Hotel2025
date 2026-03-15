package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Purchase Bill/Invoice from Supplier
 * One PurchaseBill contains many PurchaseTransactions (items purchased)
 */
@Entity
@Table(name = "purchase_bill")
public class PurchaseBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_no")
    private Integer billNo;

    @Column(name = "party_id")
    private Integer partyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Supplier supplier;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "gst")
    private Double gst = 0.0;

    @Column(name = "other_tax")
    private Double otherTax = 0.0;

    @Column(name = "reff_no", length = 50)
    private String reffNo;

    @Column(name = "pay", length = 100)
    private String pay;

    @Column(name = "pay_id")
    private Integer payId;

    @Column(name = "bank_id")
    private Integer bankId;

    @Column(name = "net_amount")
    private Double netAmount;

    @Column(name = "total_qty")
    private Double totalQty = 0.0;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "paid_amount")
    private Double paidAmount = 0.0;

    @OneToMany(mappedBy = "purchaseBill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseTransaction> transactions = new ArrayList<>();

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
    public PurchaseBill() {
    }

    public PurchaseBill(Integer partyId, Double amount, LocalDate billDate, String reffNo, String pay) {
        this.partyId = partyId;
        this.amount = amount;
        this.billDate = billDate;
        this.reffNo = reffNo;
        this.pay = pay;
    }

    // Getters and Setters
    public Integer getBillNo() {
        return billNo;
    }

    public void setBillNo(Integer billNo) {
        this.billNo = billNo;
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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public Double getGst() {
        return gst;
    }

    public void setGst(Double gst) {
        this.gst = gst;
    }

    public Double getOtherTax() {
        return otherTax;
    }

    public void setOtherTax(Double otherTax) {
        this.otherTax = otherTax;
    }

    public String getReffNo() {
        return reffNo;
    }

    public void setReffNo(String reffNo) {
        this.reffNo = reffNo;
    }

    public String getPay() {
        return pay;
    }

    public void setPay(String pay) {
        this.pay = pay;
    }

    public Integer getPayId() {
        return payId;
    }

    public void setPayId(Integer payId) {
        this.payId = payId;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public Double getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Double netAmount) {
        this.netAmount = netAmount;
    }

    public Double getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Double totalQty) {
        this.totalQty = totalQty;
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

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    /**
     * Calculate the remaining balance amount to be paid
     * @return balance amount (netAmount - paidAmount)
     */
    public Double getBalanceAmount() {
        Double net = netAmount != null ? netAmount : 0.0;
        Double paid = paidAmount != null ? paidAmount : 0.0;
        return net - paid;
    }

    /**
     * Check if the bill is fully paid
     * @return true if balance amount is zero or less
     */
    public boolean isFullyPaid() {
        return getBalanceAmount() <= 0.0;
    }

    public List<PurchaseTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PurchaseTransaction> transactions) {
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
    public void addTransaction(PurchaseTransaction transaction) {
        transactions.add(transaction);
        transaction.setPurchaseBill(this);
    }

    // Helper method to remove transaction
    public void removeTransaction(PurchaseTransaction transaction) {
        transactions.remove(transaction);
        transaction.setPurchaseBill(null);
    }

    // Helper method to get supplier name
    public String getSupplierName() {
        return supplier != null ? supplier.getName() : "";
    }

    @Override
    public String toString() {
        return "PurchaseBill{" +
                "billNo=" + billNo +
                ", partyId=" + partyId +
                ", amount=" + amount +
                ", billDate=" + billDate +
                ", gst=" + gst +
                ", otherTax=" + otherTax +
                ", reffNo='" + reffNo + '\'' +
                ", pay='" + pay + '\'' +
                ", payId=" + payId +
                ", bankId=" + bankId +
                ", netAmount=" + netAmount +
                ", paidAmount=" + paidAmount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
