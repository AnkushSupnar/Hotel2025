package com.frontend.service;

import com.frontend.entity.TempTransaction;
import com.frontend.repository.TempTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for TempTransaction operations
 * Handles temporary transaction records for billing
 */
@Service
public class TempTransactionService {

    private static final Logger LOG = LoggerFactory.getLogger(TempTransactionService.class);

    @Autowired
    private TempTransactionRepository tempTransactionRepository;

    /**
     * Add or update a transaction for a table.
     * If item with same name and rate exists for the table, update quantity and amount.
     * Otherwise, create a new transaction.
     *
     * @param transaction the transaction to add or update
     * @return the saved/updated transaction
     */
    @Transactional
    public TempTransaction addOrUpdateTransaction(TempTransaction transaction) {
        try {
            Integer tableNo = transaction.getTableNo();
            String itemName = transaction.getItemName();
            Float rate = transaction.getRate();

            LOG.info("Adding/updating transaction for table {}: {} x {} @ {}",
                    tableNo, transaction.getQty(), itemName, rate);

            // Check if item already exists for this table with same name and rate
            Optional<TempTransaction> existing = tempTransactionRepository
                    .findByTableNoAndItemNameAndRate(tableNo, itemName, rate);

            if (existing.isPresent()) {
                // Update existing transaction - add quantity and recalculate amount
                TempTransaction existingTrans = existing.get();
                Float newQty = existingTrans.getQty() + transaction.getQty();
                Float newAmt = newQty * rate;

                existingTrans.setQty(newQty);
                existingTrans.setAmt(newAmt);

                // Update printQty only if this is a printable item (category stock = 'N')
                // If transaction.printQty > 0, it means category stock = 'N', so add to printQty
                // If transaction.printQty = 0, it means category stock = 'Y', don't update printQty
                if (transaction.getPrintQty() != null && transaction.getPrintQty() > 0) {
                    Float existingPrintQty = existingTrans.getPrintQty() != null ? existingTrans.getPrintQty() : 0f;
                    existingTrans.setPrintQty(existingPrintQty + transaction.getPrintQty());
                }

                TempTransaction updated = tempTransactionRepository.save(existingTrans);
                LOG.info("Updated existing transaction ID {}: qty={}, amt={}, printQty={}",
                        updated.getId(), updated.getQty(), updated.getAmt(), updated.getPrintQty());
                return updated;
            } else {
                // Create new transaction
                TempTransaction saved = tempTransactionRepository.save(transaction);
                LOG.info("Created new transaction ID {}: {} x {} @ {} = {}",
                        saved.getId(), saved.getQty(), saved.getItemName(),
                        saved.getRate(), saved.getAmt());
                return saved;
            }

        } catch (Exception e) {
            LOG.error("Error adding/updating transaction", e);
            throw new RuntimeException("Error adding/updating transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Get all transactions for a specific table
     */
    public List<TempTransaction> getTransactionsByTableNo(Integer tableNo) {
        try {
            LOG.info("Fetching transactions for table {}", tableNo);
            return tempTransactionRepository.findByTableNo(tableNo);
        } catch (Exception e) {
            LOG.error("Error fetching transactions for table {}", tableNo, e);
            throw new RuntimeException("Error fetching transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing transaction
     */
    @Transactional
    public TempTransaction updateTransaction(TempTransaction transaction) {
        try {
            LOG.info("Updating transaction ID {}", transaction.getId());

            if (transaction.getId() == null) {
                throw new RuntimeException("Transaction ID is required for update");
            }

            // Recalculate amount
            transaction.setAmt(transaction.getQty() * transaction.getRate());

            TempTransaction updated = tempTransactionRepository.save(transaction);
            LOG.info("Transaction updated: ID={}, qty={}, amt={}",
                    updated.getId(), updated.getQty(), updated.getAmt());
            return updated;

        } catch (Exception e) {
            LOG.error("Error updating transaction", e);
            throw new RuntimeException("Error updating transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a transaction by ID
     */
    @Transactional
    public void deleteTransaction(Integer id) {
        try {
            LOG.info("Deleting transaction ID {}", id);
            tempTransactionRepository.deleteById(id);
            LOG.info("Transaction deleted: ID={}", id);
        } catch (Exception e) {
            LOG.error("Error deleting transaction ID {}", id, e);
            throw new RuntimeException("Error deleting transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all transactions for a table (used when order is completed or cancelled)
     */
    @Transactional
    public void clearTransactionsForTable(Integer tableNo) {
        try {
            LOG.info("Clearing all transactions for table {}", tableNo);
            tempTransactionRepository.deleteByTableNo(tableNo);
            LOG.info("All transactions cleared for table {}", tableNo);
        } catch (Exception e) {
            LOG.error("Error clearing transactions for table {}", tableNo, e);
            throw new RuntimeException("Error clearing transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Get total amount for a table
     */
    public Float getTotalAmount(Integer tableNo) {
        Float total = tempTransactionRepository.getTotalAmountByTableNo(tableNo);
        return total != null ? total : 0f;
    }

    /**
     * Get total quantity for a table
     */
    public Float getTotalQuantity(Integer tableNo) {
        Float total = tempTransactionRepository.getTotalQuantityByTableNo(tableNo);
        return total != null ? total : 0f;
    }

    /**
     * Get item count for a table
     */
    public long getItemCount(Integer tableNo) {
        return tempTransactionRepository.countByTableNo(tableNo);
    }

    /**
     * Check if table has any transactions
     */
    public boolean hasTransactions(Integer tableNo) {
        return tempTransactionRepository.existsByTableNo(tableNo);
    }

    /**
     * Save a transaction directly (for edit operations)
     */
    @Transactional
    public TempTransaction save(TempTransaction transaction) {
        return tempTransactionRepository.save(transaction);
    }

    /**
     * Get items with printQty > 0 for a table (items that need kitchen printing)
     */
    public List<TempTransaction> getPrintableItemsByTableNo(Integer tableNo) {
        try {
            LOG.info("Fetching printable items for table {}", tableNo);
            List<TempTransaction> items = tempTransactionRepository.findPrintableItemsByTableNo(tableNo);
            LOG.info("Found {} printable items for table {}", items.size(), tableNo);
            return items;
        } catch (Exception e) {
            LOG.error("Error fetching printable items for table {}", tableNo, e);
            throw new RuntimeException("Error fetching printable items: " + e.getMessage(), e);
        }
    }

    /**
     * Reset printQty to 0 for all items of a table (after printing KOT)
     */
    @Transactional
    public void resetPrintQtyForTable(Integer tableNo) {
        try {
            LOG.info("Resetting printQty for table {}", tableNo);
            List<TempTransaction> items = tempTransactionRepository.findByTableNo(tableNo);
            for (TempTransaction item : items) {
                item.setPrintQty(0f);
                tempTransactionRepository.save(item);
            }
            LOG.info("Reset printQty for {} items on table {}", items.size(), tableNo);
        } catch (Exception e) {
            LOG.error("Error resetting printQty for table {}", tableNo, e);
            throw new RuntimeException("Error resetting printQty: " + e.getMessage(), e);
        }
    }
}
