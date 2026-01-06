package com.frontend.service;

import com.frontend.entity.BankTransaction;
import com.frontend.entity.BillPayment;
import com.frontend.entity.PaymentReceipt;
import com.frontend.entity.PurchaseBill;
import com.frontend.repository.BillPaymentRepository;
import com.frontend.repository.PaymentReceiptRepository;
import com.frontend.repository.PurchaseBillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for PaymentReceipt operations
 * Handles grouped payment recording - one receipt for multiple bill payments
 * Creates single bank transaction for total amount
 */
@Service
public class PaymentReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReceiptService.class);

    @Autowired
    private PaymentReceiptRepository paymentReceiptRepository;

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Autowired
    private PurchaseBillRepository purchaseBillRepository;

    @Autowired
    private BankTransactionService bankTransactionService;

    /**
     * DTO for bill payment allocation
     */
    public static class BillPaymentAllocation {
        private final Integer billNo;
        private final Double amount;

        public BillPaymentAllocation(Integer billNo, Double amount) {
            this.billNo = billNo;
            this.amount = amount;
        }

        public Integer getBillNo() { return billNo; }
        public Double getAmount() { return amount; }
    }

    /**
     * Record a grouped payment for one or more bills
     * Creates ONE PaymentReceipt + ONE BankTransaction
     * Allocates payment across bills in BillPayment records
     *
     * @param supplierId   The supplier being paid
     * @param totalAmount  Total payment amount
     * @param bankId       Bank account for payment
     * @param paymentMode  Payment mode description
     * @param chequeNo     Cheque number (optional)
     * @param referenceNo  Reference number (optional)
     * @param remarks      Additional remarks (optional)
     * @param allocations  List of bill allocations (billNo, amount)
     * @return The created PaymentReceipt with all details
     */
    @Transactional
    public PaymentReceipt recordGroupedPayment(
            Integer supplierId,
            Double totalAmount,
            Integer bankId,
            String paymentMode,
            String chequeNo,
            String referenceNo,
            String remarks,
            List<BillPaymentAllocation> allocations) {

        try {
            LOG.info("Recording grouped payment: Supplier={}, Amount={}, Bills={}",
                    supplierId, totalAmount, allocations.size());

            // Validate allocations
            if (allocations == null || allocations.isEmpty()) {
                throw new RuntimeException("At least one bill allocation is required");
            }

            // Validate total matches allocations
            double allocatedTotal = allocations.stream()
                    .mapToDouble(BillPaymentAllocation::getAmount)
                    .sum();

            if (Math.abs(allocatedTotal - totalAmount) > 0.01) {
                throw new RuntimeException("Total amount (" + totalAmount +
                        ") does not match sum of allocations (" + allocatedTotal + ")");
            }

            // 1. Create single bank transaction for total amount
            String supplierName = getSupplierName(supplierId);
            String particulars = "Supplier Payment - " + supplierName;
            if (allocations.size() > 1) {
                particulars += " (" + allocations.size() + " bills)";
            } else {
                particulars += " (Bill #" + allocations.get(0).getBillNo() + ")";
            }

            BankTransaction bankTxn = bankTransactionService.recordWithdrawal(
                    bankId,
                    totalAmount,
                    particulars,
                    "SUPPLIER_PAYMENT",
                    null,  // No single bill reference - it's grouped
                    remarks != null ? remarks : "Supplier Payment Receipt"
            );

            LOG.info("Bank transaction created: ID={}, Amount={}", bankTxn.getId(), totalAmount);

            // 2. Create PaymentReceipt (master record)
            PaymentReceipt receipt = new PaymentReceipt();
            receipt.setSupplierId(supplierId);
            receipt.setPaymentDate(LocalDate.now());
            receipt.setTotalAmount(totalAmount);
            receipt.setBankId(bankId);
            receipt.setPaymentMode(paymentMode);
            receipt.setChequeNo(chequeNo != null && !chequeNo.trim().isEmpty() ? chequeNo.trim() : null);
            receipt.setReferenceNo(referenceNo != null && !referenceNo.trim().isEmpty() ? referenceNo.trim() : null);
            receipt.setRemarks(remarks != null && !remarks.trim().isEmpty() ? remarks.trim() : null);
            receipt.setBankTransactionId(bankTxn.getId());
            receipt.setBillsCount(allocations.size());
            receipt.setCreatedBy(SessionService.getCurrentEmployeeId());

            PaymentReceipt savedReceipt = paymentReceiptRepository.save(receipt);
            LOG.info("PaymentReceipt created: ReceiptNo={}", savedReceipt.getReceiptNo());

            // 3. Create BillPayment allocation records and update bills
            for (BillPaymentAllocation alloc : allocations) {
                // Validate bill exists and has sufficient balance
                Optional<PurchaseBill> optBill = purchaseBillRepository.findById(alloc.getBillNo());
                if (optBill.isEmpty()) {
                    throw new RuntimeException("Bill not found: " + alloc.getBillNo());
                }

                PurchaseBill bill = optBill.get();
                Double balanceAmount = bill.getBalanceAmount();

                if (alloc.getAmount() > balanceAmount + 0.01) {
                    throw new RuntimeException("Payment amount (" + alloc.getAmount() +
                            ") exceeds balance (" + balanceAmount + ") for Bill #" + alloc.getBillNo());
                }

                // Create BillPayment allocation record
                BillPayment payment = new BillPayment();
                payment.setReceiptNo(savedReceipt.getReceiptNo());
                payment.setBillNo(alloc.getBillNo());
                payment.setPaymentDate(LocalDate.now());
                payment.setPaymentAmount(alloc.getAmount());
                payment.setBankId(bankId);
                payment.setPaymentMode(paymentMode);
                payment.setSupplierId(supplierId);
                // Note: No individual bank_transaction_id - linked via receipt
                payment.setCreatedBy(SessionService.getCurrentEmployeeId());

                billPaymentRepository.save(payment);

                // Update PurchaseBill paid amount and status
                updateBillPaidStatus(bill, alloc.getAmount());

                LOG.info("Bill allocation recorded: Bill={}, Amount={}", alloc.getBillNo(), alloc.getAmount());
            }

            // Reload receipt with bill payments
            savedReceipt = paymentReceiptRepository.findByIdWithBillPayments(savedReceipt.getReceiptNo())
                    .orElse(savedReceipt);

            LOG.info("Grouped payment completed: ReceiptNo={}, Bills={}, TotalAmount={}",
                    savedReceipt.getReceiptNo(), allocations.size(), totalAmount);

            return savedReceipt;

        } catch (Exception e) {
            LOG.error("Error recording grouped payment: {}", e.getMessage(), e);
            throw new RuntimeException("Error recording payment: " + e.getMessage(), e);
        }
    }

    /**
     * Update bill's paid amount and status after a payment
     */
    private void updateBillPaidStatus(PurchaseBill bill, Double paymentAmount) {
        Double currentPaidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
        Double newPaidAmount = currentPaidAmount + paymentAmount;
        bill.setPaidAmount(newPaidAmount);

        // Determine new status
        Double netAmount = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;
        if (newPaidAmount >= netAmount - 0.01) {
            bill.setStatus("PAID");
            LOG.info("Bill #{} fully paid. Total paid: {}", bill.getBillNo(), newPaidAmount);
        } else if (newPaidAmount > 0) {
            bill.setStatus("PARTIALLY_PAID");
            LOG.info("Bill #{} partially paid. Paid: {}, Balance: {}",
                    bill.getBillNo(), newPaidAmount, netAmount - newPaidAmount);
        }

        purchaseBillRepository.save(bill);
    }

    /**
     * Get supplier name by ID
     */
    private String getSupplierName(Integer supplierId) {
        if (supplierId == null) return "";
        try {
            return purchaseBillRepository.findById(supplierId)
                    .map(bill -> bill.getSupplierName())
                    .orElse("Supplier #" + supplierId);
        } catch (Exception e) {
            return "Supplier #" + supplierId;
        }
    }

    // ============= Query Methods =============

    /**
     * Get receipt by ID
     */
    public Optional<PaymentReceipt> getReceiptById(Integer receiptNo) {
        return paymentReceiptRepository.findById(receiptNo);
    }

    /**
     * Get receipt with bill payments eagerly loaded
     */
    @Transactional(readOnly = true)
    public Optional<PaymentReceipt> getReceiptWithBillPayments(Integer receiptNo) {
        return paymentReceiptRepository.findByIdWithBillPayments(receiptNo);
    }

    /**
     * Get all receipts ordered by date
     */
    public List<PaymentReceipt> getAllReceipts() {
        return paymentReceiptRepository.findAllByOrderByPaymentDateDescReceiptNoDesc();
    }

    /**
     * Get receipts by supplier
     */
    public List<PaymentReceipt> getReceiptsBySupplier(Integer supplierId) {
        return paymentReceiptRepository.findBySupplierIdOrderByPaymentDateDescReceiptNoDesc(supplierId);
    }

    /**
     * Get receipts by date range
     */
    public List<PaymentReceipt> getReceiptsByDateRange(LocalDate startDate, LocalDate endDate) {
        return paymentReceiptRepository.findByPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
                startDate, endDate);
    }

    /**
     * Get receipts by date range with supplier details
     */
    public List<PaymentReceipt> getReceiptsByDateRangeWithSupplier(LocalDate startDate, LocalDate endDate) {
        return paymentReceiptRepository.findByDateRangeWithSupplier(startDate, endDate);
    }

    /**
     * Get receipts by supplier and date range
     */
    public List<PaymentReceipt> getReceiptsBySupplierAndDateRange(
            Integer supplierId, LocalDate startDate, LocalDate endDate) {
        return paymentReceiptRepository.findBySupplierIdAndPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
                supplierId, startDate, endDate);
    }

    /**
     * Get receipts by date
     */
    public List<PaymentReceipt> getReceiptsByDate(LocalDate date) {
        return paymentReceiptRepository.findByPaymentDateOrderByReceiptNoDesc(date);
    }

    // ============= Summary Methods =============

    /**
     * Get total payments by date (using receipts)
     */
    public Double getTotalPaymentsByDate(LocalDate date) {
        return paymentReceiptRepository.getTotalPaymentsByDate(date);
    }

    /**
     * Get total payments by date range
     */
    public Double getTotalPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return paymentReceiptRepository.getTotalPaymentsByDateRange(startDate, endDate);
    }

    /**
     * Get total payments for supplier
     */
    public Double getTotalPaymentsForSupplier(Integer supplierId) {
        return paymentReceiptRepository.getTotalPaymentsBySupplierId(supplierId);
    }

    /**
     * Get total payments for supplier in date range
     */
    public Double getTotalPaymentsForSupplierInDateRange(
            Integer supplierId, LocalDate startDate, LocalDate endDate) {
        return paymentReceiptRepository.getTotalPaymentsBySupplierIdAndDateRange(
                supplierId, startDate, endDate);
    }

    /**
     * Get today's total payments
     */
    public Double getTodayTotalPayments() {
        return getTotalPaymentsByDate(LocalDate.now());
    }

    /**
     * Count receipts by date
     */
    public long getReceiptCountByDate(LocalDate date) {
        return paymentReceiptRepository.countByPaymentDate(date);
    }

    /**
     * Delete a receipt (reverses the transaction)
     * Note: Use with caution - this will also reverse the bank transaction and bill payments
     */
    @Transactional
    public void deleteReceipt(Integer receiptNo) {
        try {
            Optional<PaymentReceipt> optReceipt = paymentReceiptRepository.findByIdWithBillPayments(receiptNo);
            if (optReceipt.isEmpty()) {
                throw new RuntimeException("Receipt not found: " + receiptNo);
            }

            PaymentReceipt receipt = optReceipt.get();

            // 1. Reverse the bank transaction
            if (receipt.getBankTransactionId() != null) {
                bankTransactionService.deleteTransaction(receipt.getBankTransactionId());
            }

            // 2. Reverse each bill payment and update bill status
            for (BillPayment payment : receipt.getBillPayments()) {
                Optional<PurchaseBill> optBill = purchaseBillRepository.findById(payment.getBillNo());
                if (optBill.isPresent()) {
                    PurchaseBill bill = optBill.get();
                    Double currentPaidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
                    Double newPaidAmount = currentPaidAmount - payment.getPaymentAmount();
                    if (newPaidAmount < 0) newPaidAmount = 0.0;
                    bill.setPaidAmount(newPaidAmount);

                    // Update status
                    if (newPaidAmount <= 0) {
                        bill.setStatus("PENDING");
                    } else if (newPaidAmount < bill.getNetAmount()) {
                        bill.setStatus("PARTIALLY_PAID");
                    }

                    purchaseBillRepository.save(bill);
                }
            }

            // 3. Delete the receipt (cascades to bill payments)
            paymentReceiptRepository.deleteById(receiptNo);

            LOG.info("Receipt #{} deleted and reversed", receiptNo);

        } catch (Exception e) {
            LOG.error("Error deleting receipt {}: {}", receiptNo, e.getMessage(), e);
            throw new RuntimeException("Error deleting receipt: " + e.getMessage(), e);
        }
    }
}
