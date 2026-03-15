package com.frontend.service;

import com.frontend.entity.BankTransaction;
import com.frontend.entity.BillPayment;
import com.frontend.entity.PurchaseBill;
import com.frontend.repository.BillPaymentRepository;
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
 * Service for BillPayment operations
 * Handles payments for purchase bills with partial payment support
 */
@Service
public class BillPaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(BillPaymentService.class);

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Autowired
    private PurchaseBillRepository purchaseBillRepository;

    @Autowired
    private BankTransactionService bankTransactionService;

    /**
     * Record a payment for a purchase bill
     * - Creates BillPayment record
     * - Updates PurchaseBill.paidAmount and status
     * - Records bank withdrawal transaction
     *
     * @param billNo        The bill number to pay
     * @param paymentAmount The amount being paid
     * @param bankId        The bank account ID
     * @param paymentMode   Payment mode description (e.g., "CASH", bank name)
     * @param chequeNo      Cheque number (optional)
     * @param referenceNo   Reference number (optional)
     * @param remarks       Additional remarks (optional)
     * @return The created BillPayment record
     */
    @Transactional
    public BillPayment recordPayment(Integer billNo, Double paymentAmount, Integer bankId,
                                      String paymentMode, String chequeNo, String referenceNo,
                                      String remarks) {
        try {
            // 1. Validate bill exists
            Optional<PurchaseBill> optBill = purchaseBillRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Bill not found: " + billNo);
            }

            PurchaseBill bill = optBill.get();

            // 2. Validate payment amount
            if (paymentAmount == null || paymentAmount <= 0) {
                throw new RuntimeException("Payment amount must be greater than 0");
            }

            Double balanceAmount = bill.getBalanceAmount();
            if (paymentAmount > balanceAmount) {
                throw new RuntimeException("Payment amount (" + paymentAmount +
                        ") exceeds balance amount (" + balanceAmount + ")");
            }

            // 3. Record bank withdrawal
            String particulars = "Purchase Bill Payment #" + billNo;
            if (bill.getSupplier() != null) {
                particulars += " - " + bill.getSupplier().getName();
            }

            BankTransaction bankTxn = bankTransactionService.recordWithdrawal(
                    bankId,
                    paymentAmount,
                    particulars,
                    "PURCHASE_PAYMENT",
                    billNo,
                    "Payment for Purchase Bill #" + billNo
            );

            // 4. Create BillPayment record
            BillPayment payment = new BillPayment();
            payment.setBillNo(billNo);
            payment.setPaymentDate(LocalDate.now());
            payment.setPaymentAmount(paymentAmount);
            payment.setBankId(bankId);
            payment.setPaymentMode(paymentMode);
            payment.setChequeNo(chequeNo != null ? chequeNo.trim() : null);
            payment.setReferenceNo(referenceNo != null ? referenceNo.trim() : null);
            payment.setRemarks(remarks != null ? remarks.trim() : null);
            payment.setBankTransactionId(bankTxn.getId());
            payment.setSupplierId(bill.getPartyId());
            payment.setCreatedBy(SessionService.getCurrentEmployeeId());

            BillPayment savedPayment = billPaymentRepository.save(payment);

            // 5. Update PurchaseBill paidAmount and status
            Double currentPaidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
            Double newPaidAmount = currentPaidAmount + paymentAmount;
            bill.setPaidAmount(newPaidAmount);

            // Determine new status
            Double netAmount = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;
            if (newPaidAmount >= netAmount) {
                bill.setStatus("PAID");
                LOG.info("Bill #{} fully paid. Total paid: {}", billNo, newPaidAmount);
            } else if (newPaidAmount > 0) {
                bill.setStatus("PARTIALLY_PAID");
                LOG.info("Bill #{} partially paid. Paid: {}, Balance: {}",
                        billNo, newPaidAmount, netAmount - newPaidAmount);
            }

            purchaseBillRepository.save(bill);

            LOG.info("Payment recorded: ID={}, Bill={}, Amount={}, Bank={}, Mode={}",
                    savedPayment.getId(), billNo, paymentAmount, bankId, paymentMode);

            return savedPayment;

        } catch (Exception e) {
            LOG.error("Error recording payment for bill {}: {}", billNo, e.getMessage(), e);
            throw new RuntimeException("Error recording payment: " + e.getMessage(), e);
        }
    }

    /**
     * Get all payments for a specific bill
     */
    public List<BillPayment> getPaymentsByBillNo(Integer billNo) {
        return billPaymentRepository.findByBillNoOrderByPaymentDateDescIdDesc(billNo);
    }

    /**
     * Get payments by supplier
     */
    public List<BillPayment> getPaymentsBySupplierId(Integer supplierId) {
        return billPaymentRepository.findBySupplierIdOrderByPaymentDateDescIdDesc(supplierId);
    }

    /**
     * Get payments by date range
     */
    public List<BillPayment> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return billPaymentRepository.findByPaymentDateBetweenWithDetails(startDate, endDate);
    }

    /**
     * Get payments by supplier and date range
     */
    public List<BillPayment> getPaymentsBySupplierAndDateRange(Integer supplierId,
                                                                LocalDate startDate, LocalDate endDate) {
        return billPaymentRepository.findBySupplierIdAndPaymentDateBetweenWithDetails(
                supplierId, startDate, endDate);
    }

    /**
     * Get payments by date
     */
    public List<BillPayment> getPaymentsByDate(LocalDate date) {
        return billPaymentRepository.findByPaymentDateOrderByIdDesc(date);
    }

    /**
     * Get all payments ordered by date
     */
    public List<BillPayment> getAllPayments() {
        return billPaymentRepository.findAllWithDetailsOrderByPaymentDateDescIdDesc();
    }

    /**
     * Get payment by ID
     */
    public Optional<BillPayment> getPaymentById(Integer id) {
        return billPaymentRepository.findById(id);
    }

    /**
     * Get total payments for a bill
     */
    public Double getTotalPaymentsForBill(Integer billNo) {
        Double total = billPaymentRepository.getTotalPaymentsByBillNo(billNo);
        return total != null ? total : 0.0;
    }

    /**
     * Get total payments for a supplier
     */
    public Double getTotalPaymentsForSupplier(Integer supplierId) {
        Double total = billPaymentRepository.getTotalPaymentsBySupplierId(supplierId);
        return total != null ? total : 0.0;
    }

    /**
     * Get total payments by date range
     */
    public Double getTotalPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        Double total = billPaymentRepository.getTotalPaymentsByDateRange(startDate, endDate);
        return total != null ? total : 0.0;
    }

    /**
     * Get total payments for supplier in date range
     */
    public Double getTotalPaymentsForSupplierInDateRange(Integer supplierId,
                                                          LocalDate startDate, LocalDate endDate) {
        Double total = billPaymentRepository.getTotalPaymentsBySupplierIdAndDateRange(
                supplierId, startDate, endDate);
        return total != null ? total : 0.0;
    }

    /**
     * Get today's total payments
     */
    public Double getTodayTotalPayments() {
        Double total = billPaymentRepository.getTodayTotalPayments(LocalDate.now());
        return total != null ? total : 0.0;
    }

    /**
     * Get payment count for a bill
     */
    public long getPaymentCountForBill(Integer billNo) {
        return billPaymentRepository.countByBillNo(billNo);
    }

    /**
     * Delete a payment (reverses the transaction)
     * Note: Use with caution - this will also reverse the bank transaction
     */
    @Transactional
    public void deletePayment(Integer paymentId) {
        try {
            Optional<BillPayment> optPayment = billPaymentRepository.findById(paymentId);
            if (optPayment.isEmpty()) {
                throw new RuntimeException("Payment not found: " + paymentId);
            }

            BillPayment payment = optPayment.get();

            // 1. Reverse the bank transaction
            if (payment.getBankTransactionId() != null) {
                bankTransactionService.deleteTransaction(payment.getBankTransactionId());
            }

            // 2. Update the bill's paid amount
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

            // 3. Delete the payment record
            billPaymentRepository.deleteById(paymentId);

            LOG.info("Payment {} deleted and reversed", paymentId);

        } catch (Exception e) {
            LOG.error("Error deleting payment {}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Error deleting payment: " + e.getMessage(), e);
        }
    }
}
