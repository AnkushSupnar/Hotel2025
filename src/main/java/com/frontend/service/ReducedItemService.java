package com.frontend.service;

import com.frontend.entity.ReducedItem;
import com.frontend.repository.ReducedItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for ReducedItem operations.
 * Handles tracking of reduced/cancelled kitchen items.
 */
@Service
public class ReducedItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ReducedItemService.class);

    @Autowired
    private ReducedItemRepository reducedItemRepository;

    /**
     * Track a reduced kitchen item.
     * Called when user reduces quantity or removes an item that was already sent to kitchen.
     *
     * @param itemName Item name
     * @param reducedQty Quantity being reduced (items already sent to kitchen)
     * @param rate Rate per unit
     * @param tableNo Table number
     * @param waitorId Waiter ID
     * @param reducedByUserId User ID who reduced the item
     * @param reducedByUserName Username who reduced the item
     * @param reason Optional reason for reduction
     * @return Saved ReducedItem
     */
    @Transactional
    public ReducedItem trackReducedItem(String itemName, Float reducedQty, Float rate,
                                         Integer tableNo, Integer waitorId,
                                         Integer reducedByUserId, String reducedByUserName,
                                         String reason) {
        try {
            if (reducedQty <= 0) {
                LOG.warn("Reduced quantity must be positive: {}", reducedQty);
                return null;
            }

            ReducedItem reducedItem = new ReducedItem();
            reducedItem.setItemName(itemName);
            reducedItem.setReducedQty(reducedQty);
            reducedItem.setRate(rate);
            reducedItem.setAmt(reducedQty * rate);
            reducedItem.setTableNo(tableNo);
            reducedItem.setWaitorId(waitorId);
            reducedItem.setReducedByUserId(reducedByUserId);
            reducedItem.setReducedByUserName(reducedByUserName);
            reducedItem.setReason(reason);

            ReducedItem saved = reducedItemRepository.save(reducedItem);
            LOG.info("Tracked reduced item: {} x {} @ {} for table {} by user {}",
                    reducedQty, itemName, rate, tableNo, reducedByUserName);
            return saved;

        } catch (Exception e) {
            LOG.error("Error tracking reduced item: {}", itemName, e);
            throw new RuntimeException("Error tracking reduced item: " + e.getMessage(), e);
        }
    }

    /**
     * Get all reduced items for a table
     */
    public List<ReducedItem> getReducedItemsByTable(Integer tableNo) {
        return reducedItemRepository.findByTableNo(tableNo);
    }

    /**
     * Get all reduced items by a user
     */
    public List<ReducedItem> getReducedItemsByUser(Integer userId) {
        return reducedItemRepository.findByReducedByUserId(userId);
    }

    /**
     * Get reduced items for today
     */
    public List<ReducedItem> getReducedItemsForToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return reducedItemRepository.findByDateRange(startOfDay, endOfDay);
    }

    /**
     * Get reduced items within a date range
     */
    public List<ReducedItem> getReducedItemsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return reducedItemRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get total reduced amount for today
     */
    public Float getTotalReducedAmountForToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        Float total = reducedItemRepository.getTotalReducedAmountByDateRange(startOfDay, endOfDay);
        return total != null ? total : 0f;
    }

    /**
     * Get total reduced amount for a date range
     */
    public Float getTotalReducedAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Float total = reducedItemRepository.getTotalReducedAmountByDateRange(startDate, endDate);
        return total != null ? total : 0f;
    }

    /**
     * Get all reduced items
     */
    public List<ReducedItem> getAllReducedItems() {
        return reducedItemRepository.findAll();
    }
}
