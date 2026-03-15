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

    @Autowired
    private ReducedItemService reducedItemService;

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

                // If resulting quantity is 0 or less, delete the transaction
                if (newQty <= 0) {
                    tempTransactionRepository.delete(existingTrans);
                    LOG.info("Deleted transaction ID {} (qty became {}): {} @ {}",
                            existingTrans.getId(), newQty, itemName, rate);
                    return null; // Return null to indicate deletion
                }

                existingTrans.setQty(newQty);
                existingTrans.setAmt(newAmt);

                // Update printQty - handle both positive and negative quantities
                if (transaction.getPrintQty() != null) {
                    Float existingPrintQty = existingTrans.getPrintQty() != null ? existingTrans.getPrintQty() : 0f;
                    Float newPrintQty = existingPrintQty + transaction.getPrintQty();
                    // Don't allow printQty to go below 0
                    existingTrans.setPrintQty(Math.max(0f, newPrintQty));
                }

                TempTransaction updated = tempTransactionRepository.save(existingTrans);
                LOG.info("Updated existing transaction ID {}: qty={}, amt={}, printQty={}",
                        updated.getId(), updated.getQty(), updated.getAmt(), updated.getPrintQty());
                return updated;
            } else {
                // Don't create new transaction with negative quantity
                if (transaction.getQty() < 0) {
                    LOG.warn("Cannot create transaction with negative quantity: {} x {} @ {}",
                            transaction.getQty(), itemName, rate);
                    return null;
                }
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
     * Add or update a transaction with tracking of reduced kitchen items.
     * Tracks items that were already sent to kitchen when they are reduced/removed.
     *
     * @param transaction the transaction to add or update
     * @param reducedByUserId User ID who is reducing the item
     * @param reducedByUserName Username who is reducing the item
     * @return the saved/updated transaction
     */
    @Transactional
    public TempTransaction addOrUpdateTransactionWithTracking(TempTransaction transaction,
                                                               Integer reducedByUserId,
                                                               String reducedByUserName) {
        try {
            Integer tableNo = transaction.getTableNo();
            String itemName = transaction.getItemName();
            Float rate = transaction.getRate();
            Float incomingQty = transaction.getQty();

            LOG.info("Adding/updating transaction with tracking for table {}: {} x {} @ {}",
                    tableNo, incomingQty, itemName, rate);

            // Check if item already exists for this table with same name and rate
            Optional<TempTransaction> existing = tempTransactionRepository
                    .findByTableNoAndItemNameAndRate(tableNo, itemName, rate);

            if (existing.isPresent() && incomingQty < 0) {
                // Reducing quantity - check if we need to track kitchen items
                TempTransaction existingTrans = existing.get();
                Float existingQty = existingTrans.getQty();
                Float existingPrintQty = existingTrans.getPrintQty() != null ? existingTrans.getPrintQty() : 0f;
                Float reducingAmount = Math.abs(incomingQty);

                // Items already sent to kitchen = qty - printQty
                // (printQty is items waiting to be printed, not yet sent)
                Float itemsSentToKitchen = existingQty - existingPrintQty;

                if (itemsSentToKitchen > 0) {
                    // Some items were already sent to kitchen
                    // Calculate how many of the reduced items were from kitchen
                    // First, reduce from printQty (items not yet sent to kitchen)
                    Float reducedFromPending = Math.min(reducingAmount, existingPrintQty);
                    Float reducedFromKitchen = reducingAmount - reducedFromPending;

                    // If we're reducing more than pending items, track the kitchen items
                    if (reducedFromKitchen > 0) {
                        Float qtyToTrack = Math.min(reducedFromKitchen, itemsSentToKitchen);
                        LOG.info("Tracking {} kitchen items being reduced: {} @ {} for table {}",
                                qtyToTrack, itemName, rate, tableNo);

                        reducedItemService.trackReducedItem(
                                itemName,
                                qtyToTrack,
                                rate,
                                tableNo,
                                existingTrans.getWaitorId(),
                                reducedByUserId,
                                reducedByUserName,
                                null // reason
                        );
                    }
                }

                // Now proceed with the normal update logic
                Float newQty = existingQty + incomingQty;
                Float newAmt = newQty * rate;

                // If resulting quantity is 0 or less, delete the transaction
                if (newQty <= 0) {
                    tempTransactionRepository.delete(existingTrans);
                    LOG.info("Deleted transaction ID {} (qty became {}): {} @ {}",
                            existingTrans.getId(), newQty, itemName, rate);
                    return null;
                }

                existingTrans.setQty(newQty);
                existingTrans.setAmt(newAmt);

                // Update printQty
                if (transaction.getPrintQty() != null) {
                    Float newPrintQty = existingPrintQty + transaction.getPrintQty();
                    existingTrans.setPrintQty(Math.max(0f, newPrintQty));
                }

                TempTransaction updated = tempTransactionRepository.save(existingTrans);
                LOG.info("Updated existing transaction ID {}: qty={}, amt={}, printQty={}",
                        updated.getId(), updated.getQty(), updated.getAmt(), updated.getPrintQty());
                return updated;

            } else {
                // Not reducing or item doesn't exist - use normal logic
                return addOrUpdateTransaction(transaction);
            }

        } catch (Exception e) {
            LOG.error("Error adding/updating transaction with tracking", e);
            throw new RuntimeException("Error adding/updating transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Remove a transaction completely and track if it was a kitchen item.
     *
     * @param transactionId ID of the transaction to remove
     * @param reducedByUserId User ID who is removing the item
     * @param reducedByUserName Username who is removing the item
     */
    @Transactional
    public void removeTransactionWithTracking(Integer transactionId,
                                               Integer reducedByUserId,
                                               String reducedByUserName) {
        try {
            Optional<TempTransaction> transOpt = tempTransactionRepository.findById(transactionId);
            if (transOpt.isEmpty()) {
                LOG.warn("Transaction not found for removal: ID={}", transactionId);
                return;
            }

            TempTransaction trans = transOpt.get();
            Float qty = trans.getQty();
            Float printQty = trans.getPrintQty() != null ? trans.getPrintQty() : 0f;

            // Items already sent to kitchen = qty - printQty
            Float itemsSentToKitchen = qty - printQty;

            if (itemsSentToKitchen > 0) {
                // Track the kitchen items being removed
                LOG.info("Tracking {} kitchen items being removed: {} @ {} for table {}",
                        itemsSentToKitchen, trans.getItemName(), trans.getRate(), trans.getTableNo());

                reducedItemService.trackReducedItem(
                        trans.getItemName(),
                        itemsSentToKitchen,
                        trans.getRate(),
                        trans.getTableNo(),
                        trans.getWaitorId(),
                        reducedByUserId,
                        reducedByUserName,
                        null // reason
                );
            }

            // Delete the transaction
            tempTransactionRepository.delete(trans);
            LOG.info("Removed transaction ID {}: {} x {} @ {}",
                    transactionId, qty, trans.getItemName(), trans.getRate());

        } catch (Exception e) {
            LOG.error("Error removing transaction with tracking: ID={}", transactionId, e);
            throw new RuntimeException("Error removing transaction: " + e.getMessage(), e);
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
     * Get a transaction by ID
     */
    public TempTransaction getTransactionById(Integer id) {
        return tempTransactionRepository.findById(id).orElse(null);
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

    /**
     * Shift all temp transactions from one table to another
     * @param sourceTableNo the table to shift from
     * @param targetTableNo the table to shift to
     * @return the number of transactions shifted
     */
    @Transactional
    public int shiftTransactionsToTable(Integer sourceTableNo, Integer targetTableNo) {
        try {
            LOG.info("Shifting temp transactions from table {} to table {}", sourceTableNo, targetTableNo);

            List<TempTransaction> sourceTransactions = tempTransactionRepository.findByTableNo(sourceTableNo);

            if (sourceTransactions.isEmpty()) {
                LOG.info("No temp transactions to shift from table {}", sourceTableNo);
                return 0;
            }

            int shiftedCount = 0;
            for (TempTransaction trans : sourceTransactions) {
                // Update table number to target table
                trans.setTableNo(targetTableNo);
                tempTransactionRepository.save(trans);
                shiftedCount++;
            }

            LOG.info("Shifted {} temp transactions from table {} to table {}",
                    shiftedCount, sourceTableNo, targetTableNo);
            return shiftedCount;

        } catch (Exception e) {
            LOG.error("Error shifting temp transactions from table {} to table {}",
                    sourceTableNo, targetTableNo, e);
            throw new RuntimeException("Error shifting transactions: " + e.getMessage(), e);
        }
    }
}
