package com.frontend.service;

import com.frontend.entity.BankTransaction;
import com.frontend.entity.Bill;
import com.frontend.entity.SalesBillPayment;
import com.frontend.entity.SalesPaymentReceipt;
import com.frontend.repository.BillRepository;
import com.frontend.repository.SalesBillPaymentRepository;
import com.frontend.repository.SalesPaymentReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for SalesPaymentReceipt operations
 * Handles grouped payment recording for customer payments on credit bills
 * Creates single bank transaction (DEPOSIT) for total amount received
 */
@Service
public class SalesPaymentReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(SalesPaymentReceiptService.class);

    @Autowired
    private SalesPaymentReceiptRepository salesPaymentReceiptRepository;

    @Autowired
    private SalesBillPaymentRepository salesBillPaymentRepository;

    @Autowired
    private BillRepository billRepository;

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
     * Record a grouped payment from customer for one or more credit bills
     * Creates ONE SalesPaymentReceipt + ONE BankTransaction (DEPOSIT)
     * Allocates payment across bills in SalesBillPayment records
     *
     * @param customerId   The customer making payment
     * @param totalAmount  Total payment amount received
     * @param bankId       Bank account receiving payment
     * @param paymentMode  Payment mode description
     * @param chequeNo     Cheque number (optional)
     * @param referenceNo  Reference number (optional)
     * @param remarks      Additional remarks (optional)
     * @param allocations  List of bill allocations (billNo, amount)
     * @return The created SalesPaymentReceipt with all details
     */
    @Transactional
    public SalesPaymentReceipt recordGroupedPayment(
            Integer customerId,
            Double totalAmount,
            Integer bankId,
            String paymentMode,
            String chequeNo,
            String referenceNo,
            String remarks,
            List<BillPaymentAllocation> allocations) {

        try {
            LOG.info("Recording customer payment: Customer={}, Amount={}, Bills={}",
                    customerId, totalAmount, allocations.size());

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

            // 1. Create single bank transaction (DEPOSIT for receiving payment)
            String customerName = getCustomerName(customerId);
            String particulars = "Customer Payment - " + customerName;
            if (allocations.size() > 1) {
                particulars += " (" + allocations.size() + " bills)";
            } else {
                particulars += " (Bill #" + allocations.get(0).getBillNo() + ")";
            }

            BankTransaction bankTxn = bankTransactionService.recordDeposit(
                    bankId,
                    totalAmount,
                    particulars,
                    "CUSTOMER_PAYMENT",
                    null,  // No single bill reference - it's grouped
                    remarks != null ? remarks : "Customer Payment Receipt"
            );

            LOG.info("Bank transaction created: ID={}, Amount={}", bankTxn.getId(), totalAmount);

            // 2. Create SalesPaymentReceipt (master record)
            SalesPaymentReceipt receipt = new SalesPaymentReceipt();
            receipt.setCustomerId(customerId);
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

            SalesPaymentReceipt savedReceipt = salesPaymentReceiptRepository.save(receipt);
            LOG.info("SalesPaymentReceipt created: ReceiptNo={}", savedReceipt.getReceiptNo());

            // 3. Create SalesBillPayment allocation records and update bills
            for (BillPaymentAllocation alloc : allocations) {
                // Validate bill exists and has sufficient balance
                Optional<Bill> optBill = billRepository.findById(alloc.getBillNo());
                if (optBill.isEmpty()) {
                    throw new RuntimeException("Bill not found: " + alloc.getBillNo());
                }

                Bill bill = optBill.get();
                Float balanceAmount = bill.getBalanceAmount();

                if (alloc.getAmount() > balanceAmount + 0.01) {
                    throw new RuntimeException("Payment amount (" + alloc.getAmount() +
                            ") exceeds balance (" + balanceAmount + ") for Bill #" + alloc.getBillNo());
                }

                // Create SalesBillPayment allocation record
                SalesBillPayment payment = new SalesBillPayment();
                payment.setReceiptNo(savedReceipt.getReceiptNo());
                payment.setBillNo(alloc.getBillNo());
                payment.setPaymentDate(LocalDate.now());
                payment.setPaymentAmount(alloc.getAmount());
                payment.setBankId(bankId);
                payment.setPaymentMode(paymentMode);
                payment.setCustomerId(customerId);
                payment.setCreatedBy(SessionService.getCurrentEmployeeId());

                salesBillPaymentRepository.save(payment);

                // Update Bill paid amount and status
                updateBillPaidStatus(bill, alloc.getAmount());

                LOG.info("Bill allocation recorded: Bill={}, Amount={}", alloc.getBillNo(), alloc.getAmount());
            }

            // Reload receipt with bill payments
            savedReceipt = salesPaymentReceiptRepository.findByIdWithBillPayments(savedReceipt.getReceiptNo())
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
    private void updateBillPaidStatus(Bill bill, Double paymentAmount) {
        Float currentPaidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0f;
        Float newPaidAmount = currentPaidAmount + paymentAmount.floatValue();
        bill.setPaidAmount(newPaidAmount);

        // Determine new status
        Float netAmount = bill.getNetAmount() != null ? bill.getNetAmount() : 0f;
        if (newPaidAmount >= netAmount - 0.01f) {
            bill.setStatus("PAID");  // Fully paid
            LOG.info("Bill #{} fully paid. Total paid: {}", bill.getBillNo(), newPaidAmount);
        } else if (newPaidAmount > 0) {
            // Keep as CREDIT if partially paid
            LOG.info("Bill #{} partially paid. Paid: {}, Balance: {}",
                    bill.getBillNo(), newPaidAmount, netAmount - newPaidAmount);
        }

        billRepository.save(bill);
    }

    /**
     * Get customer name by ID
     */
    private String getCustomerName(Integer customerId) {
        if (customerId == null) return "";
        // For now, return a simple reference
        // The actual customer name will be retrieved from the Customer entity when needed
        return "Customer #" + customerId;
    }

    // ============= Query Methods =============

    /**
     * Get receipt by ID
     */
    public Optional<SalesPaymentReceipt> getReceiptById(Integer receiptNo) {
        return salesPaymentReceiptRepository.findById(receiptNo);
    }

    /**
     * Get receipt with bill payments eagerly loaded
     */
    @Transactional(readOnly = true)
    public Optional<SalesPaymentReceipt> getReceiptWithBillPayments(Integer receiptNo) {
        return salesPaymentReceiptRepository.findByIdWithBillPayments(receiptNo);
    }

    /**
     * Get all receipts ordered by date
     */
    public List<SalesPaymentReceipt> getAllReceipts() {
        return salesPaymentReceiptRepository.findAllByOrderByPaymentDateDescReceiptNoDesc();
    }

    /**
     * Get receipts by customer
     */
    public List<SalesPaymentReceipt> getReceiptsByCustomer(Integer customerId) {
        return salesPaymentReceiptRepository.findByCustomerIdOrderByPaymentDateDescReceiptNoDesc(customerId);
    }

    /**
     * Get receipts by date range
     */
    public List<SalesPaymentReceipt> getReceiptsByDateRange(LocalDate startDate, LocalDate endDate) {
        return salesPaymentReceiptRepository.findByPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
                startDate, endDate);
    }

    /**
     * Get receipts by date range with customer details
     */
    public List<SalesPaymentReceipt> getReceiptsByDateRangeWithCustomer(LocalDate startDate, LocalDate endDate) {
        return salesPaymentReceiptRepository.findByDateRangeWithCustomer(startDate, endDate);
    }

    /**
     * Get receipts by customer and date range
     */
    public List<SalesPaymentReceipt> getReceiptsByCustomerAndDateRange(
            Integer customerId, LocalDate startDate, LocalDate endDate) {
        return salesPaymentReceiptRepository.findByCustomerIdAndPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(
                customerId, startDate, endDate);
    }

    /**
     * Get receipts by date
     */
    public List<SalesPaymentReceipt> getReceiptsByDate(LocalDate date) {
        return salesPaymentReceiptRepository.findByPaymentDateOrderByReceiptNoDesc(date);
    }

    // ============= Summary Methods =============

    /**
     * Get total receipts by date
     */
    public Double getTotalReceiptsByDate(LocalDate date) {
        return salesPaymentReceiptRepository.getTotalReceiptsByDate(date);
    }

    /**
     * Get total receipts by date range
     */
    public Double getTotalReceiptsByDateRange(LocalDate startDate, LocalDate endDate) {
        return salesPaymentReceiptRepository.getTotalReceiptsByDateRange(startDate, endDate);
    }

    /**
     * Get total receipts for customer
     */
    public Double getTotalReceiptsForCustomer(Integer customerId) {
        return salesPaymentReceiptRepository.getTotalReceiptsByCustomerId(customerId);
    }

    /**
     * Get total receipts for customer in date range
     */
    public Double getTotalReceiptsForCustomerInDateRange(
            Integer customerId, LocalDate startDate, LocalDate endDate) {
        return salesPaymentReceiptRepository.getTotalReceiptsByCustomerIdAndDateRange(
                customerId, startDate, endDate);
    }

    /**
     * Get today's total receipts
     */
    public Double getTodayTotalReceipts() {
        return getTotalReceiptsByDate(LocalDate.now());
    }

    /**
     * Count receipts by date
     */
    public long getReceiptCountByDate(LocalDate date) {
        return salesPaymentReceiptRepository.countByPaymentDate(date);
    }

    /**
     * Delete a receipt (reverses the transaction)
     * Note: Use with caution - this will also reverse the bank transaction and bill payments
     */
    @Transactional
    public void deleteReceipt(Integer receiptNo) {
        try {
            Optional<SalesPaymentReceipt> optReceipt = salesPaymentReceiptRepository.findByIdWithBillPayments(receiptNo);
            if (optReceipt.isEmpty()) {
                throw new RuntimeException("Receipt not found: " + receiptNo);
            }

            SalesPaymentReceipt receipt = optReceipt.get();

            // 1. Reverse the bank transaction
            if (receipt.getBankTransactionId() != null) {
                bankTransactionService.deleteTransaction(receipt.getBankTransactionId());
            }

            // 2. Reverse each bill payment and update bill status
            for (SalesBillPayment payment : receipt.getBillPayments()) {
                Optional<Bill> optBill = billRepository.findById(payment.getBillNo());
                if (optBill.isPresent()) {
                    Bill bill = optBill.get();
                    Float currentPaidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0f;
                    Float newPaidAmount = currentPaidAmount - payment.getPaymentAmount().floatValue();
                    if (newPaidAmount < 0) newPaidAmount = 0f;
                    bill.setPaidAmount(newPaidAmount);

                    // Update status
                    if (newPaidAmount <= 0) {
                        bill.setStatus("CREDIT");  // Back to credit
                    } else if (newPaidAmount < bill.getNetAmount()) {
                        bill.setStatus("CREDIT");  // Partially paid still shows as credit
                    }

                    billRepository.save(bill);
                }
            }

            // 3. Delete the receipt (cascades to bill payments)
            salesPaymentReceiptRepository.deleteById(receiptNo);

            LOG.info("Receipt #{} deleted and reversed", receiptNo);

        } catch (Exception e) {
            LOG.error("Error deleting receipt {}: {}", receiptNo, e.getMessage(), e);
            throw new RuntimeException("Error deleting receipt: " + e.getMessage(), e);
        }
    }
}
