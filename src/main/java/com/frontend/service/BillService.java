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
            bill.setBillTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
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

            // Clear temp transactions for this table
            tempTransactionService.clearTransactionsForTable(tableNo);

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

            // Clear temp transactions for this table
            tempTransactionService.clearTransactionsForTable(tableNo);

            return savedBill;

        } catch (Exception e) {
            LOG.error("Error creating CREDIT bill for table {}", tableNo, e);
            throw new RuntimeException("Error creating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Update bill status to PAID
     * Note: Eagerly fetches transactions to avoid LazyInitializationException when printing
     */
    @Transactional
    public Bill markBillAsPaid(Integer billNo, Float cashReceived, Float returnAmount,
                                Float discount, String paymode) {
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

            Bill updatedBill = billRepository.save(bill);

            // Eagerly fetch transactions to avoid LazyInitializationException
            // when printing the bill in a background thread
            updatedBill.getTransactions().size();

            LOG.info("Bill {} marked as PAID with {} transactions", billNo, updatedBill.getTransactions().size());

            return updatedBill;

        } catch (Exception e) {
            LOG.error("Error marking bill {} as PAID", billNo, e);
            throw new RuntimeException("Error updating bill: " + e.getMessage(), e);
        }
    }

    /**
     * Update bill status to CREDIT for a customer
     * Credit bill means customer will pay later
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
            bill.setNetAmount(bill.getBillAmt() - (discount != null ? discount : 0f));

            Bill updatedBill = billRepository.save(bill);

            // Eagerly fetch transactions to avoid LazyInitializationException
            // when printing the bill in a background thread
            updatedBill.getTransactions().size();

            LOG.info("Bill {} marked as CREDIT for customer {} with {} transactions",
                    billNo, customerId, updatedBill.getTransactions().size());

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
}
