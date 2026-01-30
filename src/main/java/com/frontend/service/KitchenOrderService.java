package com.frontend.service;

import com.frontend.dto.ItemDto;
import com.frontend.entity.Item;
import com.frontend.entity.KitchenOrder;
import com.frontend.entity.KitchenOrderItem;
import com.frontend.entity.TempTransaction;
import com.frontend.repository.KitchenOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KitchenOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(KitchenOrderService.class);

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_SERVE = "SERVE";

    @Autowired
    private KitchenOrderRepository kitchenOrderRepository;

    @Autowired
    private ItemService itemService;

    /**
     * Create a KitchenOrder record from a list of printable TempTransactions.
     * Looks up item ID by name and stores the ID.
     */
    @Transactional
    public KitchenOrder createKitchenOrder(Integer tableNo, String tableName, Integer waitorId,
                                           List<TempTransaction> printableItems) {
        KitchenOrder ko = new KitchenOrder();
        ko.setTableNo(tableNo);
        ko.setTableName(tableName);
        ko.setWaitorId(waitorId);
        ko.setStatus(STATUS_SENT);
        ko.setItemCount(printableItems.size());

        float totalQty = 0f;
        for (TempTransaction item : printableItems) {
            // Look up item ID by name (nullable — save always succeeds)
            Integer itemId = null;
            try {
                Optional<Item> itemOpt = itemService.getItemByName(item.getItemName());
                if (itemOpt.isPresent()) {
                    itemId = itemOpt.get().getId();
                }
            } catch (Exception e) {
                LOG.warn("Could not resolve item ID for '{}': {}", item.getItemName(), e.getMessage());
            }

            Float qty = item.getPrintQty() != null ? item.getPrintQty() : item.getQty();
            KitchenOrderItem koItem = new KitchenOrderItem(item.getItemName(), itemId, qty, item.getRate());
            ko.addItem(koItem);
            totalQty += qty;
        }
        ko.setTotalQty(totalQty);

        KitchenOrder saved = kitchenOrderRepository.save(ko);
        LOG.info("Created KitchenOrder #{} for table {} ({}) with {} items",
                saved.getId(), tableNo, tableName, printableItems.size());
        return saved;
    }

    /**
     * Mark a single KOT as READY. Returns entity with items eagerly loaded.
     */
    @Transactional
    public KitchenOrder markAsReady(Integer kotId) {
        KitchenOrder ko = kitchenOrderRepository.findById(kotId)
                .orElseThrow(() -> new RuntimeException("KitchenOrder not found: " + kotId));
        ko.setStatus(STATUS_READY);
        ko.setReadyAt(LocalDateTime.now());
        kitchenOrderRepository.save(ko);
        LOG.info("KitchenOrder #{} marked as READY", kotId);
        return kitchenOrderRepository.findByIdWithItems(kotId).orElse(ko);
    }

    /**
     * Mark a single KOT as SERVE. Returns entity with items eagerly loaded.
     */
    @Transactional
    public KitchenOrder markAsServed(Integer kotId) {
        KitchenOrder ko = kitchenOrderRepository.findById(kotId)
                .orElseThrow(() -> new RuntimeException("KitchenOrder not found: " + kotId));
        ko.setStatus(STATUS_SERVE);
        kitchenOrderRepository.save(ko);
        LOG.info("KitchenOrder #{} marked as SERVE", kotId);
        return kitchenOrderRepository.findByIdWithItems(kotId).orElse(ko);
    }

    /**
     * Mark ALL SENT KOTs for a table as READY.
     */
    @Transactional
    public void markAllAsReadyForTable(Integer tableNo) {
        List<KitchenOrder> sentOrders = kitchenOrderRepository
                .findByTableNoAndStatusOrderBySentAtAsc(tableNo, STATUS_SENT);
        LocalDateTime now = LocalDateTime.now();
        for (KitchenOrder ko : sentOrders) {
            ko.setStatus(STATUS_READY);
            ko.setReadyAt(now);
        }
        kitchenOrderRepository.saveAll(sentOrders);
        LOG.info("Marked {} KitchenOrders as READY for table {}", sentOrders.size(), tableNo);
    }

    /**
     * Mark ALL READY KOTs for a table as SERVE.
     */
    @Transactional
    public void markAllAsServedForTable(Integer tableNo) {
        List<KitchenOrder> readyOrders = kitchenOrderRepository
                .findByTableNoAndStatusOrderBySentAtAsc(tableNo, STATUS_READY);
        for (KitchenOrder ko : readyOrders) {
            ko.setStatus(STATUS_SERVE);
        }
        kitchenOrderRepository.saveAll(readyOrders);
        LOG.info("Marked {} KitchenOrders as SERVE for table {}", readyOrders.size(), tableNo);
    }

    /**
     * Get all KitchenOrders for a table (eagerly fetches items).
     */
    public List<KitchenOrder> getKitchenOrdersForTable(Integer tableNo) {
        return kitchenOrderRepository.findByTableNoWithItems(tableNo);
    }

    /**
     * Get all SENT KitchenOrders across all tables (eagerly fetches items).
     */
    public List<KitchenOrder> getAllPendingKitchenOrders() {
        return kitchenOrderRepository.findByStatusWithItems(STATUS_SENT);
    }

    /**
     * Get all READY KitchenOrders across all tables (eagerly fetches items).
     */
    public List<KitchenOrder> getAllReadyKitchenOrders() {
        return kitchenOrderRepository.findByStatusWithItems(STATUS_READY);
    }

    /**
     * Get all KitchenOrders across all tables (eagerly fetches items).
     */
    public List<KitchenOrder> getAllKitchenOrders() {
        return kitchenOrderRepository.findAllWithItems();
    }

    /**
     * Delete all KitchenOrders for a table (cleanup on bill finalize).
     * Loads entities first so JPA cascade deletes child items.
     */
    @Transactional
    public void clearKitchenOrdersForTable(Integer tableNo) {
        List<KitchenOrder> orders = kitchenOrderRepository.findByTableNoOrderBySentAtAsc(tableNo);
        if (!orders.isEmpty()) {
            kitchenOrderRepository.deleteAll(orders);
            LOG.info("Cleared {} KitchenOrders for table {}", orders.size(), tableNo);
        }
    }

    /**
     * Shift KitchenOrders from source table to target table.
     */
    @Transactional
    public void shiftKitchenOrders(Integer sourceTableNo, Integer targetTableNo, String targetTableName) {
        kitchenOrderRepository.shiftKitchenOrdersToTable(sourceTableNo, targetTableNo, targetTableName);
        LOG.info("Shifted KitchenOrders from table {} to table {} ({})",
                sourceTableNo, targetTableNo, targetTableName);
    }

    /**
     * Resolve item name from item ID using ItemService.
     */
    public String resolveItemName(Integer itemId) {
        if (itemId == null) return "Unknown";
        try {
            ItemDto item = itemService.getItemById(itemId);
            return item.getItemName();
        } catch (Exception e) {
            LOG.warn("Could not resolve item name for ID {}: {}", itemId, e.getMessage());
            return "Item #" + itemId;
        }
    }
}
