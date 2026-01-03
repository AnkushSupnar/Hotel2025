package com.frontend.service;

import com.frontend.entity.Item;
import com.frontend.entity.PurchaseBill;
import com.frontend.entity.PurchaseTransaction;
import com.frontend.repository.PurchaseBillRepository;
import com.frontend.repository.PurchaseTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for PurchaseBill operations
 * Handles purchase bill creation, saving, and management
 */
@Service
public class PurchaseBillService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseBillService.class);

    @Autowired
    private PurchaseBillRepository purchaseBillRepository;

    @Autowired
    private PurchaseTransactionRepository purchaseTransactionRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemStockService itemStockService;

    /**
     * Create and save a new purchase bill
     */
    @Transactional
    public PurchaseBill createPurchaseBill(PurchaseBill bill, List<PurchaseTransaction> transactions) {
        try {
            LOG.info("Creating purchase bill for supplier ID: {}", bill.getPartyId());

            // Calculate totals
            double totalAmount = 0.0;
            double totalQty = 0.0;
            for (PurchaseTransaction trans : transactions) {
                totalAmount += trans.getAmount();
                totalQty += trans.getQty();
            }

            bill.setAmount(totalAmount);
            bill.setTotalQty(totalQty);

            // Calculate net amount (amount + GST + other tax)
            double netAmount = totalAmount +
                    (bill.getGst() != null ? bill.getGst() : 0.0) +
                    (bill.getOtherTax() != null ? bill.getOtherTax() : 0.0);
            bill.setNetAmount(netAmount);

            // Save bill first to get the bill number
            PurchaseBill savedBill = purchaseBillRepository.save(bill);
            LOG.info("Purchase bill created with number: {}", savedBill.getBillNo());

            // Add transactions to bill
            for (PurchaseTransaction trans : transactions) {
                trans.setPurchaseBill(savedBill);

                // Get item_code from Item entity by category and item name (preferred)
                // Items with same name can exist in different categories
                Optional<Item> itemOpt;
                if (trans.getCategoryId() != null) {
                    // Use category + name lookup (accurate)
                    itemOpt = itemService.getItemByCategoryAndName(trans.getCategoryId(), trans.getItemName());
                } else {
                    // Fallback to name-only lookup
                    itemOpt = itemService.getItemByName(trans.getItemName());
                }

                if (itemOpt.isPresent()) {
                    trans.setItemCode(itemOpt.get().getItemCode());
                    if (trans.getCategoryId() == null) {
                        trans.setCategoryId(itemOpt.get().getCategoryId());
                    }
                }

                savedBill.addTransaction(trans);
            }

            // Save bill with transactions
            savedBill = purchaseBillRepository.save(savedBill);
            LOG.info("Purchase bill {} saved with {} transactions",
                    savedBill.getBillNo(), savedBill.getTransactions().size());

            // Update stock for items with stock-enabled categories
            updateStockForPurchase(savedBill);

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error creating purchase bill", e);
            throw new RuntimeException("Error creating purchase bill: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing purchase bill
     */
    @Transactional
    public PurchaseBill updatePurchaseBill(Integer billNo, PurchaseBill updatedBill,
                                           List<PurchaseTransaction> transactions) {
        try {
            Optional<PurchaseBill> optBill = purchaseBillRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Purchase bill not found: " + billNo);
            }

            PurchaseBill bill = optBill.get();

            // Delete existing transactions
            purchaseTransactionRepository.deleteByBillNo(billNo);
            bill.getTransactions().clear();

            // Update bill properties
            bill.setPartyId(updatedBill.getPartyId());
            bill.setBillDate(updatedBill.getBillDate());
            bill.setReffNo(updatedBill.getReffNo());
            bill.setPay(updatedBill.getPay());
            bill.setPayId(updatedBill.getPayId());
            bill.setGst(updatedBill.getGst());
            bill.setOtherTax(updatedBill.getOtherTax());
            bill.setRemarks(updatedBill.getRemarks());
            bill.setStatus(updatedBill.getStatus());

            // Calculate totals
            double totalAmount = 0.0;
            double totalQty = 0.0;
            for (PurchaseTransaction trans : transactions) {
                totalAmount += trans.getAmount();
                totalQty += trans.getQty();
            }

            bill.setAmount(totalAmount);
            bill.setTotalQty(totalQty);

            // Calculate net amount
            double netAmount = totalAmount +
                    (bill.getGst() != null ? bill.getGst() : 0.0) +
                    (bill.getOtherTax() != null ? bill.getOtherTax() : 0.0);
            bill.setNetAmount(netAmount);

            // Add new transactions
            for (PurchaseTransaction trans : transactions) {
                trans.setPurchaseBill(bill);

                Optional<Item> itemOpt = itemService.getItemByName(trans.getItemName());
                if (itemOpt.isPresent()) {
                    trans.setItemCode(itemOpt.get().getItemCode());
                    trans.setCategoryId(itemOpt.get().getCategoryId());
                }

                bill.addTransaction(trans);
            }

            PurchaseBill savedBill = purchaseBillRepository.save(bill);
            LOG.info("Purchase bill {} updated with {} transactions",
                    savedBill.getBillNo(), savedBill.getTransactions().size());

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error updating purchase bill {}", billNo, e);
            throw new RuntimeException("Error updating purchase bill: " + e.getMessage(), e);
        }
    }

    /**
     * Mark purchase bill as paid
     */
    @Transactional
    public PurchaseBill markAsPaid(Integer billNo, String paymentMode, Integer payId) {
        try {
            Optional<PurchaseBill> optBill = purchaseBillRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Purchase bill not found: " + billNo);
            }

            PurchaseBill bill = optBill.get();
            bill.setStatus("PAID");
            bill.setPay(paymentMode);
            bill.setPayId(payId);

            PurchaseBill savedBill = purchaseBillRepository.save(bill);
            LOG.info("Purchase bill {} marked as PAID", billNo);

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error marking purchase bill {} as paid", billNo, e);
            throw new RuntimeException("Error updating purchase bill: " + e.getMessage(), e);
        }
    }

    /**
     * Get purchase bill by bill number
     */
    public Optional<PurchaseBill> getBillByNo(Integer billNo) {
        return purchaseBillRepository.findById(billNo);
    }

    /**
     * Get purchase bill with transactions eagerly loaded
     */
    @Transactional(readOnly = true)
    public PurchaseBill getBillWithTransactions(Integer billNo) {
        Optional<PurchaseBill> optBill = purchaseBillRepository.findById(billNo);
        if (optBill.isPresent()) {
            PurchaseBill bill = optBill.get();
            // Eagerly load transactions
            bill.getTransactions().size();
            return bill;
        }
        return null;
    }

    /**
     * Get all purchase bills
     */
    public List<PurchaseBill> getAllBills() {
        return purchaseBillRepository.findAllByOrderByBillNoDesc();
    }

    /**
     * Get purchase bills by supplier
     */
    public List<PurchaseBill> getBillsBySupplier(Integer supplierId) {
        return purchaseBillRepository.findByPartyId(supplierId);
    }

    /**
     * Get purchase bills by date
     */
    public List<PurchaseBill> getBillsByDate(LocalDate date) {
        return purchaseBillRepository.findByBillDate(date);
    }

    /**
     * Get purchase bills by date range
     */
    public List<PurchaseBill> getBillsByDateRange(LocalDate startDate, LocalDate endDate) {
        return purchaseBillRepository.findByBillDateBetween(startDate, endDate);
    }

    /**
     * Get purchase bills by status
     */
    public List<PurchaseBill> getBillsByStatus(String status) {
        return purchaseBillRepository.findByStatus(status);
    }

    /**
     * Get pending bills for a supplier
     */
    public List<PurchaseBill> getPendingBillsBySupplier(Integer supplierId) {
        return purchaseBillRepository.findPendingBillsBySupplier(supplierId);
    }

    /**
     * Get total pending amount for a supplier
     */
    public Double getTotalPendingAmount(Integer supplierId) {
        Double amount = purchaseBillRepository.getTotalPendingAmountBySupplier(supplierId);
        return amount != null ? amount : 0.0;
    }

    // ============= Payment Related Methods =============

    /**
     * Get bills with pending balance (PENDING or PARTIALLY_PAID status)
     */
    public List<PurchaseBill> getBillsWithPendingBalance() {
        return purchaseBillRepository.findBillsWithPendingBalance();
    }

    /**
     * Get payable bills for a specific supplier (PENDING or PARTIALLY_PAID)
     */
    public List<PurchaseBill> getPayableBillsBySupplier(Integer supplierId) {
        return purchaseBillRepository.findPayableBillsBySupplier(supplierId);
    }

    /**
     * Get total payable amount for a supplier (netAmount - paidAmount for unpaid bills)
     */
    public Double getTotalPayableAmount(Integer supplierId) {
        Double amount = purchaseBillRepository.getTotalPayableAmountBySupplier(supplierId);
        return amount != null ? amount : 0.0;
    }

    /**
     * Get list of supplier IDs that have pending bills
     */
    public List<Integer> getSuppliersWithPendingBills() {
        return purchaseBillRepository.findSuppliersWithPendingBills();
    }

    /**
     * Count bills with pending balance
     */
    public long countBillsWithPendingBalance() {
        return purchaseBillRepository.countBillsWithPendingBalance();
    }

    /**
     * Count payable bills for a supplier
     */
    public long countPayableBillsBySupplier(Integer supplierId) {
        return purchaseBillRepository.countPayableBillsBySupplier(supplierId);
    }

    /**
     * Get total purchase amount by date
     */
    public Double getTotalPurchaseByDate(LocalDate date) {
        Double amount = purchaseBillRepository.getTotalPurchaseAmountByDate(date);
        return amount != null ? amount : 0.0;
    }

    /**
     * Get total purchase amount by date range
     */
    public Double getTotalPurchaseByDateRange(LocalDate startDate, LocalDate endDate) {
        Double amount = purchaseBillRepository.getTotalPurchaseAmountByDateRange(startDate, endDate);
        return amount != null ? amount : 0.0;
    }

    /**
     * Get transactions for a purchase bill
     */
    public List<PurchaseTransaction> getTransactionsForBill(Integer billNo) {
        return purchaseTransactionRepository.findByBillNo(billNo);
    }

    /**
     * Delete purchase bill
     */
    @Transactional
    public void deleteBill(Integer billNo) {
        purchaseBillRepository.deleteById(billNo);
        LOG.info("Purchase bill {} deleted", billNo);
    }

    /**
     * Save purchase bill
     */
    @Transactional
    public PurchaseBill saveBill(PurchaseBill bill) {
        return purchaseBillRepository.save(bill);
    }

    /**
     * Get today's purchase bills
     */
    public List<PurchaseBill> getTodaysBills() {
        return purchaseBillRepository.findByBillDate(LocalDate.now());
    }

    /**
     * Get today's total purchase amount
     */
    public Double getTodaysTotalPurchase() {
        Double amount = purchaseBillRepository.getTotalPurchaseAmountByDate(LocalDate.now());
        return amount != null ? amount : 0.0;
    }

    /**
     * Get bill count by date
     */
    public long getBillCountByDate(LocalDate date) {
        return purchaseBillRepository.countByBillDate(date);
    }

    /**
     * Update stock for purchase bill items
     * Only updates stock for items whose category has stock='Y'
     */
    private void updateStockForPurchase(PurchaseBill bill) {
        try {
            LOG.info("Updating stock for purchase bill: {}", bill.getBillNo());
            int stockUpdatedCount = 0;

            for (PurchaseTransaction trans : bill.getTransactions()) {
                try {
                    // Only update stock if categoryId and itemCode are available
                    if (trans.getCategoryId() != null) {
                        itemStockService.addStock(
                                trans.getItemCode(),
                                trans.getItemName(),
                                trans.getCategoryId(),
                                trans.getQty(),
                                trans.getRate(),
                                bill.getBillNo()
                        );
                        stockUpdatedCount++;
                    } else {
                        LOG.debug("Skipping stock update for item {} - no category ID", trans.getItemName());
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to update stock for item: {} - {}", trans.getItemName(), e.getMessage());
                    // Continue with other items even if one fails
                }
            }

            LOG.info("Stock updated for {} items in purchase bill {}", stockUpdatedCount, bill.getBillNo());

        } catch (Exception e) {
            LOG.error("Error updating stock for purchase bill: {} - {}", bill.getBillNo(), e.getMessage());
            // Don't throw - stock update failure shouldn't fail the bill save
        }
    }
}
