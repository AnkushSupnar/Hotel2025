package com.frontend.service;

import com.frontend.entity.Bill;
import com.frontend.entity.Item;
import com.frontend.entity.TempTransaction;
import com.frontend.entity.Transaction;
import com.frontend.repository.BillRepository;
import com.frontend.repository.TransactionRepository;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Bill operations
 * Handles bill creation, saving, and management
 */
@Service
public class BillService {

    private static final Logger LOG = LoggerFactory.getLogger(BillService.class);

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TempTransactionService tempTransactionService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemStockService itemStockService;

    @Autowired
    private KitchenOrderService kitchenOrderService;

    /**
     * Create and save a new bill from temp transactions with CLOSE status
     *
     * @param tableNo    Table number
     * @param customerId Customer ID (can be null for walk-in)
     * @param waitorId   Waiter ID
     * @param userId     Current logged-in user ID
     * @param tempTransactions List of temp transactions to convert to bill
     * @return The saved bill
     */
    @Transactional
    public Bill createClosedBill(Integer tableNo, Integer customerId, Integer waitorId,
                                  Integer userId, List<TempTransaction> tempTransactions) {
        try {
            LOG.info("Creating CLOSE bill for table {}", tableNo);

            // Consolidate temp transactions with same itemName and rate
            List<TempTransaction> consolidatedTransactions = consolidateTempTransactions(tempTransactions);
            LOG.info("Consolidated {} temp transactions to {} unique items",
                    tempTransactions.size(), consolidatedTransactions.size());

            // Calculate totals from consolidated transactions
            float totalAmt = 0f;
            float totalQty = 0f;
            for (TempTransaction temp : consolidatedTransactions) {
                totalAmt += temp.getAmt();
                totalQty += temp.getQty();
            }

            // Create bill
            Bill bill = new Bill();
            bill.setBillAmt(totalAmt);
            bill.setDiscount(0f);
            bill.setCustomerId(customerId);
            bill.setWaitorId(waitorId);
            bill.setTableNo(tableNo);
            bill.setUserId(userId);
            bill.setBillDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            bill.setBillTime(null); // Will be set when bill is PAID/CREDIT
            bill.setCloseAt(java.time.LocalDateTime.now()); // Set close time
            bill.setPaymode("PENDING");
            bill.setStatus("CLOSE");
            bill.setCashReceived(0f);
            bill.setReturnAmount(0f);
            bill.setNetAmount(totalAmt);
            bill.setTotalQty(totalQty);

            // Save bill first to get the bill number
            Bill savedBill = billRepository.save(bill);
            LOG.info("Bill created with number: {}", savedBill.getBillNo());

            // Convert consolidated temp transactions to transactions and add to bill
            for (TempTransaction temp : consolidatedTransactions) {
                Transaction transaction = new Transaction();
                transaction.setItemName(temp.getItemName());
                transaction.setQty(temp.getQty());
                transaction.setRate(temp.getRate());
                transaction.setAmt(temp.getAmt());
                transaction.setBill(savedBill);

                // Get item_code from Item entity by item name
                Optional<Item> itemOpt = itemService.getItemByName(temp.getItemName());
                if (itemOpt.isPresent()) {
                    transaction.setItemCode(itemOpt.get().getItemCode());
                }

                savedBill.addTransaction(transaction);
            }

            // Save bill with transactions
            savedBill = billRepository.save(savedBill);
            LOG.info("Bill {} saved with {} transactions", savedBill.getBillNo(), savedBill.getTransactions().size());

            // Clear temp transactions for this table
            tempTransactionService.clearTransactionsForTable(tableNo);
            LOG.info("Temp transactions cleared for table {}", tableNo);

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error creating CLOSE bill for table {}", tableNo, e);
            throw new RuntimeException("Error creating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Create and save a new bill with PAID status
     */
    @Transactional
    public Bill createPaidBill(Integer tableNo, Integer customerId, Integer waitorId,
                                Integer userId, List<TempTransaction> tempTransactions,
                                Float cashReceived, Float returnAmount, Float discount,
                                String paymode) {
        try {
            LOG.info("Creating PAID bill for table {}", tableNo);

            // Consolidate temp transactions with same itemName and rate
            List<TempTransaction> consolidatedTransactions = consolidateTempTransactions(tempTransactions);
            LOG.info("Consolidated {} temp transactions to {} unique items",
                    tempTransactions.size(), consolidatedTransactions.size());

            // Calculate totals from consolidated transactions
            float totalAmt = 0f;
            float totalQty = 0f;
            for (TempTransaction temp : consolidatedTransactions) {
                totalAmt += temp.getAmt();
                totalQty += temp.getQty();
            }

            // Calculate net amount
            float netAmount = totalAmt - (discount != null ? discount : 0f);

            // Create bill
            Bill bill = new Bill();
            bill.setBillAmt(totalAmt);
            bill.setDiscount(discount != null ? discount : 0f);
            bill.setCustomerId(customerId);
            bill.setWaitorId(waitorId);
            bill.setTableNo(tableNo);
            bill.setUserId(userId);
            bill.setBillDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            bill.setBillTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            bill.setPaymode(paymode != null ? paymode : "CASH");
            bill.setStatus("PAID");
            bill.setCashReceived(cashReceived != null ? cashReceived : netAmount);
            bill.setReturnAmount(returnAmount != null ? returnAmount : 0f);
            bill.setNetAmount(netAmount);
            bill.setTotalQty(totalQty);

            // Save bill first to get the bill number
            Bill savedBill = billRepository.save(bill);
            LOG.info("PAID Bill created with number: {}", savedBill.getBillNo());

            // Convert consolidated temp transactions to transactions
            for (TempTransaction temp : consolidatedTransactions) {
                Transaction transaction = new Transaction();
                transaction.setItemName(temp.getItemName());
                transaction.setQty(temp.getQty());
                transaction.setRate(temp.getRate());
                transaction.setAmt(temp.getAmt());
                transaction.setBill(savedBill);

                // Get item_code from Item entity by item name
                Optional<Item> itemOpt = itemService.getItemByName(temp.getItemName());
                if (itemOpt.isPresent()) {
                    transaction.setItemCode(itemOpt.get().getItemCode());
                }

                savedBill.addTransaction(transaction);
            }

            // Save bill with transactions
            savedBill = billRepository.save(savedBill);
            LOG.info("PAID Bill {} saved with {} transactions", savedBill.getBillNo(), savedBill.getTransactions().size());

            // Reduce stock for items with stock-enabled categories
            reduceStockForSale(savedBill);

            // Clear temp transactions for this table
            tempTransactionService.clearTransactionsForTable(tableNo);

            // Clear kitchen orders for this table
            try {
                kitchenOrderService.clearKitchenOrdersForTable(tableNo);
            } catch (Exception kotEx) {
                LOG.warn("Failed to clear KitchenOrders for table {}: {}", tableNo, kotEx.getMessage());
            }

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error creating PAID bill for table {}", tableNo, e);
            throw new RuntimeException("Error creating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Create and save a new bill with CREDIT status
     */
    @Transactional
    public Bill createCreditBill(Integer tableNo, Integer customerId, Integer waitorId,
                                  Integer userId, List<TempTransaction> tempTransactions) {
        try {
            if (customerId == null) {
                throw new RuntimeException("Customer is required for credit billing");
            }

            LOG.info("Creating CREDIT bill for table {}, customer {}", tableNo, customerId);

            // Consolidate temp transactions with same itemName and rate
            List<TempTransaction> consolidatedTransactions = consolidateTempTransactions(tempTransactions);
            LOG.info("Consolidated {} temp transactions to {} unique items",
                    tempTransactions.size(), consolidatedTransactions.size());

            // Calculate totals from consolidated transactions
            float totalAmt = 0f;
            float totalQty = 0f;
            for (TempTransaction temp : consolidatedTransactions) {
                totalAmt += temp.getAmt();
                totalQty += temp.getQty();
            }

            // Create bill
            Bill bill = new Bill();
            bill.setBillAmt(totalAmt);
            bill.setDiscount(0f);
            bill.setCustomerId(customerId);
            bill.setWaitorId(waitorId);
            bill.setTableNo(tableNo);
            bill.setUserId(userId);
            bill.setBillDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            bill.setBillTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            bill.setPaymode("CREDIT");
            bill.setStatus("CREDIT");
            bill.setCashReceived(0f);
            bill.setReturnAmount(0f);
            bill.setNetAmount(totalAmt);
            bill.setTotalQty(totalQty);

            // Save bill first
            Bill savedBill = billRepository.save(bill);
            LOG.info("CREDIT Bill created with number: {}", savedBill.getBillNo());

            // Convert consolidated temp transactions to transactions
            for (TempTransaction temp : consolidatedTransactions) {
                Transaction transaction = new Transaction();
                transaction.setItemName(temp.getItemName());
                transaction.setQty(temp.getQty());
                transaction.setRate(temp.getRate());
                transaction.setAmt(temp.getAmt());
                transaction.setBill(savedBill);

                // Get item_code from Item entity by item name
                Optional<Item> itemOpt = itemService.getItemByName(temp.getItemName());
                if (itemOpt.isPresent()) {
                    transaction.setItemCode(itemOpt.get().getItemCode());
                }

                savedBill.addTransaction(transaction);
            }

            // Save bill with transactions
            savedBill = billRepository.save(savedBill);
            LOG.info("CREDIT Bill {} saved with {} transactions", savedBill.getBillNo(), savedBill.getTransactions().size());

            // Reduce stock for items with stock-enabled categories
            reduceStockForSale(savedBill);

            // Clear temp transactions for this table
            tempTransactionService.clearTransactionsForTable(tableNo);

            // Clear kitchen orders for this table
            try {
                kitchenOrderService.clearKitchenOrdersForTable(tableNo);
            } catch (Exception kotEx) {
                LOG.warn("Failed to clear KitchenOrders for table {}: {}", tableNo, kotEx.getMessage());
            }

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error creating CREDIT bill for table {}", tableNo, e);
            throw new RuntimeException("Error creating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Update bill status to PAID (Cash payment)
     * Note: Eagerly fetches transactions to avoid LazyInitializationException when printing
     */
    @Transactional
    public Bill markBillAsPaid(Integer billNo, Float cashReceived, Float returnAmount,
                                Float discount, String paymode) {
        return markBillAsPaid(billNo, cashReceived, returnAmount, discount, paymode, null);
    }

    /**
     * Update bill status to PAID with optional bank payment
     * Note: Eagerly fetches transactions to avoid LazyInitializationException when printing
     */
    @Transactional
    public Bill markBillAsPaid(Integer billNo, Float cashReceived, Float returnAmount,
                                Float discount, String paymode, Integer bankId) {
        try {
            Optional<Bill> optBill = billRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Bill not found: " + billNo);
            }

            Bill bill = optBill.get();
            bill.setStatus("PAID");
            bill.setPaymode(paymode != null ? paymode : "CASH");
            bill.setCashReceived(cashReceived);
            bill.setReturnAmount(returnAmount != null ? returnAmount : 0f);
            bill.setDiscount(discount != null ? discount : 0f);
            bill.setNetAmount(bill.getBillAmt() - (discount != null ? discount : 0f));
            bill.setBankId(bankId);
            // Set payment date and time
            bill.setBillDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            bill.setBillTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            Bill updatedBill = billRepository.save(bill);

            // Eagerly fetch transactions to avoid LazyInitializationException
            // when printing the bill in a background thread
            updatedBill.getTransactions().size();

            // Reduce stock for items with stock-enabled categories
            reduceStockForSale(updatedBill);

            LOG.info("Bill {} marked as PAID ({}) at {} with {} transactions", billNo, paymode, bill.getBillTime(), updatedBill.getTransactions().size());

            return updatedBill;

        } catch (Exception e) {
            LOG.error("Error marking bill {} as PAID", billNo, e);
            throw new RuntimeException("Error updating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Update bill status to CREDIT for a customer
     * Credit bill means customer will pay later
     *
     * For CREDIT bills:
     * - netAmount = billAmt (full amount owed by customer)
     * - Credit balance = netAmount - (cashReceived - returnAmount)
     * - discount should typically be 0 (unpaid amount is NOT a discount)
     *
     * Note: Eagerly fetches transactions to avoid LazyInitializationException when printing
     */
    @Transactional
    public Bill markBillAsCredit(Integer billNo, Integer customerId, Float cashReceived,
                                  Float returnAmount, Float discount) {
        try {
            if (customerId == null) {
                throw new RuntimeException("Customer is required for credit billing");
            }

            Optional<Bill> optBill = billRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Bill not found: " + billNo);
            }

            Bill bill = optBill.get();
            bill.setStatus("CREDIT");
            bill.setPaymode("CREDIT");
            bill.setCustomerId(customerId);
            bill.setCashReceived(cashReceived != null ? cashReceived : 0f);
            bill.setReturnAmount(returnAmount != null ? returnAmount : 0f);
            bill.setDiscount(discount != null ? discount : 0f);
            // For credit bills, netAmount = billAmt (what customer owes)
            // The credit balance is: netAmount - (cashReceived - returnAmount)
            bill.setNetAmount(bill.getBillAmt() - (discount != null ? discount : 0f));
            // Set payment date and time
            bill.setBillDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            bill.setBillTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            Bill updatedBill = billRepository.save(bill);

            // Eagerly fetch transactions to avoid LazyInitializationException
            // when printing the bill in a background thread
            updatedBill.getTransactions().size();

            // Reduce stock for items with stock-enabled categories
            reduceStockForSale(updatedBill);

            // Calculate credit balance for logging
            float creditBalance = bill.getNetAmount() - (bill.getCashReceived() - bill.getReturnAmount());
            LOG.info("Bill {} marked as CREDIT at {} for customer {}. Amount: ₹{}, Paid: ₹{}, Credit Balance: ₹{}",
                    billNo, bill.getBillTime(), customerId, bill.getNetAmount(),
                    (bill.getCashReceived() - bill.getReturnAmount()), creditBalance);

            return updatedBill;

        } catch (Exception e) {
            LOG.error("Error marking bill {} as CREDIT", billNo, e);
            throw new RuntimeException("Error updating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Get bill by bill number
     */
    public Optional<Bill> getBillByNo(Integer billNo) {
        return billRepository.findById(billNo);
    }

    /**
     * Get bill by bill number (returns Bill or null)
     */
    public Bill getBillByBillNo(Integer billNo) {
        return billRepository.findById(billNo).orElse(null);
    }

    /**
     * Get bill with transactions eagerly loaded (for printing)
     */
    @Transactional
    public Bill getBillWithTransactions(Integer billNo) {
        Optional<Bill> optBill = billRepository.findById(billNo);
        if (optBill.isPresent()) {
            Bill bill = optBill.get();
            // Eagerly load transactions
            bill.getTransactions().size();
            return bill;
        }
        return null;
    }

    /**
     * Get all bills by status
     */
    public List<Bill> getBillsByStatus(String status) {
        return billRepository.findByStatus(status);
    }

    /**
     * Get all bills by date
     */
    public List<Bill> getBillsByDate(String billDate) {
        return billRepository.findByBillDate(billDate);
    }

    /**
     * Get all bills by status and date
     */
    public List<Bill> getBillsByStatusAndDate(String status, String billDate) {
        return billRepository.findByStatusAndBillDate(status, billDate);
    }

    /**
     * Get all bills by status and date ordered by bill number ascending
     */
    public List<Bill> getBillsByStatusAndDateOrderByBillNoAsc(String status, String billDate) {
        return billRepository.findByStatusAndBillDateOrderByBillNoAsc(status, billDate);
    }

    /**
     * Get all bills by customer
     */
    public List<Bill> getBillsByCustomer(Integer customerId) {
        return billRepository.findByCustomerId(customerId);
    }

    /**
     * Get all bills by customer ID (alias)
     */
    public List<Bill> getBillsByCustomerId(Integer customerId) {
        return billRepository.findByCustomerId(customerId);
    }

    /**
     * Get closed bills for a customer
     */
    public List<Bill> getClosedBillsByCustomer(Integer customerId) {
        return billRepository.findClosedBillsByCustomerId(customerId);
    }

    /**
     * Get total closed (pending) amount for a customer
     */
    public Float getTotalClosedAmount(Integer customerId) {
        Float amount = billRepository.getTotalClosedAmountByCustomerId(customerId);
        return amount != null ? amount : 0f;
    }

    /**
     * Get transactions for a bill
     */
    public List<Transaction> getTransactionsForBill(Integer billNo) {
        return transactionRepository.findByBillNo(billNo);
    }

    /**
     * Get all bills
     */
    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    /**
     * Get today's bills
     */
    public List<Bill> getTodaysBills() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return billRepository.findByBillDate(today);
    }

    /**
     * Get today's total sales
     */
    public Float getTodaysTotalSales() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Float total = billRepository.getTotalBillAmountByDate(today);
        return total != null ? total : 0f;
    }

    /**
     * Get bill count for today
     */
    public long getTodaysBillCount() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return billRepository.countByBillDate(today);
    }

    /**
     * Save bill
     */
    @Transactional
    public Bill saveBill(Bill bill) {
        return billRepository.save(bill);
    }

    /**
     * Delete bill
     */
    @Transactional
    public void deleteBill(Integer billNo) {
        billRepository.deleteById(billNo);
        LOG.info("Bill {} deleted", billNo);
    }

    /**
     * Get closed bill for a specific table
     * Returns the most recent closed bill for the table, or null if none exists
     */
    public Bill getClosedBillForTable(Integer tableNo) {
        Optional<Bill> billOpt = billRepository.findFirstByTableNoAndStatusOrderByBillNoDesc(tableNo, "CLOSE");
        return billOpt.orElse(null);
    }

    /**
     * Get transactions for closed bill of a table
     * Returns empty list if no closed bill exists
     */
    public List<Transaction> getClosedBillTransactionsForTable(Integer tableNo) {
        Bill closedBill = getClosedBillForTable(tableNo);
        if (closedBill != null) {
            LOG.info("Found closed bill #{} for table {}", closedBill.getBillNo(), tableNo);
            return transactionRepository.findByBillNo(closedBill.getBillNo());
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Check if table has a closed bill
     */
    public boolean hasClosedBill(Integer tableNo) {
        return getClosedBillForTable(tableNo) != null;
    }

    /**
     * Shift a bill to a different table
     * Updates the bill's tableNo to the target table
     * @param billNo the bill number to shift
     * @param targetTableNo the target table ID
     */
    @Transactional
    public void shiftBillToTable(Integer billNo, Integer targetTableNo) {
        try {
            LOG.info("Shifting bill #{} to table {}", billNo, targetTableNo);

            Optional<Bill> billOpt = billRepository.findById(billNo);
            if (billOpt.isEmpty()) {
                throw new RuntimeException("Bill not found: " + billNo);
            }

            Bill bill = billOpt.get();
            Integer sourceTableNo = bill.getTableNo();

            // Update bill's table number
            bill.setTableNo(targetTableNo);
            billRepository.save(bill);

            LOG.info("Bill #{} shifted from table {} to table {}", billNo, sourceTableNo, targetTableNo);

        } catch (Exception e) {
            LOG.error("Error shifting bill #{} to table {}", billNo, targetTableNo, e);
            throw new RuntimeException("Error shifting bill: " + e.getMessage(), e);
        }
    }

    /**
     * Add new transactions to an existing closed bill
     * Used when closing a table that has both closed bill items and new temp transactions
     */
    @Transactional
    public Bill addTransactionsToClosedBill(Integer tableNo, List<TempTransaction> newTransactions) {
        try {
            Bill closedBill = getClosedBillForTable(tableNo);
            if (closedBill == null) {
                throw new RuntimeException("No closed bill found for table: " + tableNo);
            }

            LOG.info("Adding {} new transactions to closed bill #{}", newTransactions.size(), closedBill.getBillNo());

            // First consolidate temp transactions with same item and rate
            List<TempTransaction> consolidatedTransactions = consolidateTempTransactions(newTransactions);
            LOG.info("Consolidated to {} unique items", consolidatedTransactions.size());

            float additionalAmt = 0f;
            float additionalQty = 0f;
            int updatedCount = 0;
            int newCount = 0;

            // Add new transactions to the bill (or update existing)
            for (TempTransaction temp : consolidatedTransactions) {
                // Check if transaction with same itemName and rate already exists
                Transaction existingTransaction = findExistingTransaction(closedBill, temp.getItemName(), temp.getRate());

                if (existingTransaction != null) {
                    // Update existing transaction - add quantity and amount
                    existingTransaction.setQty(existingTransaction.getQty() + temp.getQty());
                    existingTransaction.setAmt(existingTransaction.getAmt() + temp.getAmt());
                    LOG.info("Updated existing transaction: {} qty={}, amt={}",
                            temp.getItemName(), existingTransaction.getQty(), existingTransaction.getAmt());
                    updatedCount++;
                } else {
                    // Create new transaction
                    Transaction transaction = new Transaction();
                    transaction.setItemName(temp.getItemName());
                    transaction.setQty(temp.getQty());
                    transaction.setRate(temp.getRate());
                    transaction.setAmt(temp.getAmt());
                    transaction.setBill(closedBill);

                    // Get item_code from Item entity by item name
                    Optional<Item> itemOpt = itemService.getItemByName(temp.getItemName());
                    if (itemOpt.isPresent()) {
                        transaction.setItemCode(itemOpt.get().getItemCode());
                    }

                    closedBill.addTransaction(transaction);
                    newCount++;
                }

                additionalAmt += temp.getAmt();
                additionalQty += temp.getQty();
            }

            // Update bill totals
            closedBill.setBillAmt(closedBill.getBillAmt() + additionalAmt);
            closedBill.setNetAmount(closedBill.getNetAmount() + additionalAmt);
            closedBill.setTotalQty(closedBill.getTotalQty() + additionalQty);

            // Save updated bill
            Bill savedBill = billRepository.save(closedBill);
            LOG.info("Closed bill #{} updated: {} items updated, {} new items added. New total: {}",
                    savedBill.getBillNo(), updatedCount, newCount, savedBill.getBillAmt());

            // Clear temp transactions for this table
            tempTransactionService.clearTransactionsForTable(tableNo);

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error adding transactions to closed bill for table {}", tableNo, e);
            throw new RuntimeException("Error updating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Find existing transaction in bill by itemName and rate
     * Returns null if not found
     */
    private Transaction findExistingTransaction(Bill bill, String itemName, Float rate) {
        for (Transaction t : bill.getTransactions()) {
            if (t.getItemName().equals(itemName) && t.getRate().equals(rate)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Consolidate temp transactions with same itemName and rate
     * Merges quantities and amounts for duplicate items
     */
    private List<TempTransaction> consolidateTempTransactions(List<TempTransaction> tempTransactions) {
        Map<String, TempTransaction> consolidated = new HashMap<>();

        for (TempTransaction temp : tempTransactions) {
            // Create a key based on itemName and rate
            String key = temp.getItemName() + "_" + temp.getRate();

            if (consolidated.containsKey(key)) {
                // Update existing entry - add quantity and amount
                TempTransaction existing = consolidated.get(key);
                existing.setQty(existing.getQty() + temp.getQty());
                existing.setAmt(existing.getAmt() + temp.getAmt());
                LOG.debug("Consolidated item '{}' at rate {}: new qty={}, new amt={}",
                        temp.getItemName(), temp.getRate(), existing.getQty(), existing.getAmt());
            } else {
                // Create a copy to avoid modifying original
                TempTransaction copy = new TempTransaction();
                copy.setItemName(temp.getItemName());
                copy.setQty(temp.getQty());
                copy.setRate(temp.getRate());
                copy.setAmt(temp.getAmt());
                copy.setTableNo(temp.getTableNo());
                consolidated.put(key, copy);
            }
        }

        return new ArrayList<>(consolidated.values());
    }

    /**
     * Get the last paid or credit bill (most recent)
     */
    @Transactional(readOnly = true)
    public Bill getLastPaidBill() {
        List<Bill> bills = billRepository.findLastPaidOrCreditBills();
        if (!bills.isEmpty()) {
            Bill bill = bills.get(0);
            // Eagerly load transactions
            bill.getTransactions().size();
            return bill;
        }
        return null;
    }

    // ============= Sales Report Methods =============

    /**
     * Get all paid and credit bills (for reports)
     */
    public List<Bill> getAllSalesBills() {
        return billRepository.findAllPaidAndCreditBills();
    }

    /**
     * Get paid and credit bills by date
     */
    public List<Bill> getSalesBillsByDate(String billDate) {
        return billRepository.findPaidAndCreditBillsByDate(billDate);
    }

    /**
     * Get paid and credit bills by customer
     */
    public List<Bill> getSalesBillsByCustomer(Integer customerId) {
        return billRepository.findPaidAndCreditBillsByCustomerId(customerId);
    }

    /**
     * Get total sales amount
     */
    public Float getTotalSalesAmount() {
        Float amount = billRepository.getTotalSalesAmount();
        return amount != null ? amount : 0f;
    }

    /**
     * Get total sales amount by date
     */
    public Float getTotalSalesAmountByDate(String billDate) {
        Float amount = billRepository.getTotalSalesAmountByDate(billDate);
        return amount != null ? amount : 0f;
    }

    /**
     * Get total discount amount
     */
    public Float getTotalDiscountAmount() {
        Float amount = billRepository.getTotalDiscount();
        return amount != null ? amount : 0f;
    }

    /**
     * Count all sales bills
     */
    public Long countAllSalesBills() {
        Long count = billRepository.countPaidAndCreditBills();
        return count != null ? count : 0L;
    }

    /**
     * Count sales bills by date
     */
    public Long countSalesBillsByDate(String billDate) {
        Long count = billRepository.countPaidAndCreditBillsByDate(billDate);
        return count != null ? count : 0L;
    }

    /**
     * Get sales bills filtered by date range
     * Since billDate is stored as "dd-MM-yyyy" string, we filter in memory
     */
    public List<Bill> getSalesBillsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Bill> allBills = billRepository.findAllPaidAndCreditBills();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        return allBills.stream()
                .filter(bill -> {
                    try {
                        if (bill.getBillDate() == null) return false;
                        LocalDate billDate = LocalDate.parse(bill.getBillDate(), formatter);
                        return !billDate.isBefore(startDate) && !billDate.isAfter(endDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get sales bills filtered by date range and customer
     */
    public List<Bill> getSalesBillsByDateRangeAndCustomer(LocalDate startDate, LocalDate endDate, Integer customerId) {
        List<Bill> allBills;
        if (customerId != null) {
            allBills = billRepository.findPaidAndCreditBillsByCustomerId(customerId);
        } else {
            allBills = billRepository.findAllPaidAndCreditBills();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        return allBills.stream()
                .filter(bill -> {
                    try {
                        if (bill.getBillDate() == null) return false;
                        LocalDate billDate = LocalDate.parse(bill.getBillDate(), formatter);
                        return !billDate.isBefore(startDate) && !billDate.isAfter(endDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate summary statistics for a list of bills
     */
    public Map<String, Object> calculateBillSummary(List<Bill> bills) {
        Map<String, Object> summary = new HashMap<>();

        float totalAmount = 0f;
        float totalDiscount = 0f;
        float totalNet = 0f;
        int paidCount = 0;
        int creditCount = 0;

        for (Bill bill : bills) {
            totalAmount += bill.getBillAmt() != null ? bill.getBillAmt() : 0f;
            totalDiscount += bill.getDiscount() != null ? bill.getDiscount() : 0f;
            totalNet += bill.getNetAmount() != null ? bill.getNetAmount() : 0f;

            if ("PAID".equals(bill.getStatus())) {
                paidCount++;
            } else if ("CREDIT".equals(bill.getStatus())) {
                creditCount++;
            }
        }

        summary.put("totalBills", bills.size());
        summary.put("totalAmount", totalAmount);
        summary.put("totalDiscount", totalDiscount);
        summary.put("totalNet", totalNet);
        summary.put("averageAmount", bills.isEmpty() ? 0f : totalNet / bills.size());
        summary.put("paidCount", paidCount);
        summary.put("creditCount", creditCount);

        return summary;
    }

    /**
     * Update an existing bill with new transactions
     * Used when editing a paid/credit bill
     *
     * @param billNo the bill number to update
     * @param tempTransactions the new list of transactions (as TempTransaction objects)
     * @param waitorId the waiter ID
     * @param customerId the customer ID (required for CREDIT status)
     * @param totalAmt the total bill amount
     * @param totalQty the total quantity
     * @param cashReceived cash received from customer
     * @param returnAmount return/change amount
     * @param newStatus the new status (PAID or CREDIT)
     * @return the updated bill
     */
    @Transactional
    public Bill updateBillWithTransactions(Integer billNo, List<TempTransaction> tempTransactions,
                                            Integer waitorId, Integer customerId,
                                            Float totalAmt, Float totalQty,
                                            Float cashReceived, Float returnAmount,
                                            String newStatus) {
        try {
            LOG.info("Updating bill #{} with {} transactions, status={}",
                    billNo, tempTransactions.size(), newStatus);

            // Validate CREDIT status requires customer
            if ("CREDIT".equals(newStatus) && customerId == null) {
                throw new RuntimeException("Customer is required for credit billing");
            }

            // Find the existing bill
            Optional<Bill> optBill = billRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Bill not found: " + billNo);
            }

            Bill bill = optBill.get();

            // Copy old transaction DATA (not entity references) before deleting
            // This avoids Hibernate transaction conflicts
            List<Map<String, Object>> oldTransactionData = new ArrayList<>();
            for (Transaction trans : bill.getTransactions()) {
                Map<String, Object> data = new HashMap<>();
                data.put("itemCode", trans.getItemCode());
                data.put("itemName", trans.getItemName());
                data.put("qty", trans.getQty());
                data.put("rate", trans.getRate());
                oldTransactionData.add(data);
            }
            LOG.info("Copied {} old transaction data for stock reversal in bill #{}", oldTransactionData.size(), billNo);

            // Delete existing transactions for this bill FIRST
            // Note: deleteByBillNo has clearAutomatically=true which clears the persistence context
            transactionRepository.deleteByBillNo(billNo);
            LOG.info("Deleted existing transactions for bill #{}", billNo);

            // Reload the bill after persistence context was cleared
            optBill = billRepository.findById(billNo);
            if (optBill.isEmpty()) {
                throw new RuntimeException("Bill not found after reload: " + billNo);
            }
            bill = optBill.get();

            // Reverse stock for old transactions using copied data (add back the old quantities)
            reverseStockFromData(billNo, oldTransactionData);

            // Consolidate temp transactions with same itemName and rate
            List<TempTransaction> consolidatedTransactions = consolidateTempTransactions(tempTransactions);
            LOG.info("Consolidated {} temp transactions to {} unique items",
                    tempTransactions.size(), consolidatedTransactions.size());

            // Create new transactions from temp transactions
            for (TempTransaction temp : consolidatedTransactions) {
                Transaction transaction = new Transaction();
                transaction.setItemName(temp.getItemName());
                transaction.setQty(temp.getQty());
                transaction.setRate(temp.getRate());
                transaction.setAmt(temp.getAmt());
                transaction.setBill(bill);

                // Get item_code from Item entity by item name
                Optional<Item> itemOpt = itemService.getItemByName(temp.getItemName());
                if (itemOpt.isPresent()) {
                    transaction.setItemCode(itemOpt.get().getItemCode());
                }

                bill.addTransaction(transaction);
            }

            // Update bill properties
            bill.setBillAmt(totalAmt);
            bill.setTotalQty(totalQty);
            bill.setWaitorId(waitorId);
            bill.setCustomerId(customerId);
            bill.setCashReceived(cashReceived != null ? cashReceived : 0f);
            bill.setReturnAmount(returnAmount != null ? returnAmount : 0f);
            bill.setNetAmount(totalAmt - (bill.getDiscount() != null ? bill.getDiscount() : 0f));
            bill.setStatus(newStatus);
            bill.setPaymode("CREDIT".equals(newStatus) ? "CREDIT" : "CASH");

            // Save updated bill
            Bill savedBill = billRepository.save(bill);

            // Eagerly fetch transactions for printing
            savedBill.getTransactions().size();

            // Reduce stock for new transactions
            reduceStockForSale(savedBill);

            LOG.info("Bill #{} updated successfully with {} transactions, status={}",
                    savedBill.getBillNo(), savedBill.getTransactions().size(), newStatus);

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error updating bill #{}", billNo, e);
            throw new RuntimeException("Error updating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Reduce stock for bill items
     * Only reduces stock for items whose category has stock='Y'
     */
    private void reduceStockForSale(Bill bill) {
        try {
            LOG.info("Reducing stock for sales bill: {}", bill.getBillNo());
            int stockUpdatedCount = 0;

            for (Transaction trans : bill.getTransactions()) {
                try {
                    // Get item details to find category
                    if (trans.getItemCode() != null) {
                        Item item = itemService.getItemByCode(trans.getItemCode());
                        if (item != null && item.getCategoryId() != null) {
                            itemStockService.reduceStock(
                                    trans.getItemCode(),
                                    trans.getItemName(),
                                    item.getCategoryId(),
                                    trans.getQty(),
                                    trans.getRate(),
                                    bill.getBillNo()
                            );
                            stockUpdatedCount++;
                        }
                    } else if (trans.getItemName() != null) {
                        // Try to find item by name
                        Optional<Item> itemOpt = itemService.getItemByName(trans.getItemName());
                        if (itemOpt.isPresent()) {
                            Item item = itemOpt.get();
                            if (item.getCategoryId() != null) {
                                itemStockService.reduceStock(
                                        item.getItemCode(),
                                        trans.getItemName(),
                                        item.getCategoryId(),
                                        trans.getQty(),
                                        trans.getRate(),
                                        bill.getBillNo()
                                );
                                stockUpdatedCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to reduce stock for item: {} - {}", trans.getItemName(), e.getMessage());
                    // Continue with other items even if one fails
                }
            }

            LOG.info("Stock reduced for {} items in sales bill {}", stockUpdatedCount, bill.getBillNo());

        } catch (Exception e) {
            LOG.error("Error reducing stock for sales bill: {} - {}", bill.getBillNo(), e.getMessage());
            // Don't throw - stock update failure shouldn't fail the bill save
        }
    }

    // ============= Credit Bill Payment Methods =============

    /**
     * Get credit bills with pending balance for a customer
     * Returns bills where netAmount - paidAmount > 0
     */
    public List<Bill> getCreditBillsWithPendingBalanceByCustomerId(Integer customerId) {
        return billRepository.findCreditBillsWithPendingBalanceByCustomerId(customerId);
    }

    /**
     * Get total pending amount for a customer (credit bills)
     */
    public Double getTotalPendingAmountByCustomerId(Integer customerId) {
        Double amount = billRepository.getTotalPendingAmountByCustomerId(customerId);
        return amount != null ? amount : 0.0;
    }

    /**
     * Get all customer IDs with pending credit bills
     */
    public List<Integer> getCustomerIdsWithPendingBills() {
        return billRepository.findCustomerIdsWithPendingBills();
    }

    /**
     * Get all credit bills with pending balance (across all customers)
     */
    public List<Bill> getAllCreditBillsWithPendingBalance() {
        return billRepository.findAllCreditBillsWithPendingBalance();
    }

    /**
     * Get total credit balance across all customers
     */
    public Double getTotalCreditBalance() {
        Double amount = billRepository.getTotalCreditBalance();
        return amount != null ? amount : 0.0;
    }

    /**
     * Reverse stock using copied transaction data when editing a bill
     * This method uses Map data instead of entity references to avoid Hibernate conflicts
     */
    private void reverseStockFromData(Integer billNo, List<Map<String, Object>> oldTransactionData) {
        try {
            LOG.info("Reversing stock for {} old transactions in bill: {}", oldTransactionData.size(), billNo);
            int stockReversedCount = 0;

            for (Map<String, Object> data : oldTransactionData) {
                try {
                    Integer itemCode = (Integer) data.get("itemCode");
                    String itemName = (String) data.get("itemName");
                    Float qty = (Float) data.get("qty");
                    Float rate = (Float) data.get("rate");

                    // Get item details to find category
                    if (itemCode != null) {
                        Item item = itemService.getItemByCode(itemCode);
                        if (item != null && item.getCategoryId() != null) {
                            itemStockService.reverseStockForSale(
                                    itemCode,
                                    itemName,
                                    item.getCategoryId(),
                                    qty,
                                    rate,
                                    billNo
                            );
                            stockReversedCount++;
                        }
                    } else if (itemName != null) {
                        // Try to find item by name
                        Optional<Item> itemOpt = itemService.getItemByName(itemName);
                        if (itemOpt.isPresent()) {
                            Item item = itemOpt.get();
                            if (item.getCategoryId() != null) {
                                itemStockService.reverseStockForSale(
                                        item.getItemCode(),
                                        itemName,
                                        item.getCategoryId(),
                                        qty,
                                        rate,
                                        billNo
                                );
                                stockReversedCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to reverse stock for item: {} - {}", data.get("itemName"), e.getMessage());
                    // Continue with other items even if one fails
                }
            }

            LOG.info("Stock reversed for {} items in bill {} (edit operation)", stockReversedCount, billNo);

        } catch (Exception e) {
            LOG.error("Error reversing stock for bill: {} - {}", billNo, e.getMessage());
            // Don't throw - stock reversal failure shouldn't fail the bill update
        }
    }
}
