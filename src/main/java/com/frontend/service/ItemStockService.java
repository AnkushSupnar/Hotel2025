package com.frontend.service;

import com.frontend.entity.CategoryMaster;
import com.frontend.entity.Item;
import com.frontend.entity.ItemStock;
import com.frontend.entity.ItemStockTransaction;
import com.frontend.repository.CategoryMasterRepository;
import com.frontend.repository.ItemStockRepository;
import com.frontend.repository.ItemStockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Item Stock and Stock Transactions
 * Handles stock additions (purchase) and reductions (sales)
 */
@Service
public class ItemStockService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemStockService.class);

    // Transaction Types
    public static final String TRANSACTION_PURCHASE = "PURCHASE";
    public static final String TRANSACTION_SALE = "SALE";
    public static final String TRANSACTION_SALE_REVERSAL = "SALE_REVERSAL";
    public static final String TRANSACTION_ADJUSTMENT = "ADJUSTMENT";

    // Reference Types
    public static final String REF_PURCHASE_BILL = "PURCHASE_BILL";
    public static final String REF_SALES_BILL = "SALES_BILL";
    public static final String REF_ADJUSTMENT = "ADJUSTMENT";

    @Autowired
    private ItemStockRepository itemStockRepository;

    @Autowired
    private ItemStockTransactionRepository transactionRepository;

    @Autowired
    private CategoryMasterRepository categoryRepository;

    @Autowired
    private ItemService itemService;

    /**
     * Add stock for an item (called when purchase bill is saved)
     * Only adds stock if the item's category has stock='Y'
     */
    @Transactional
    public ItemStock addStock(Integer itemCode, String itemName, Integer categoryId,
                               Float quantity, Float rate, Integer purchaseBillNo) {
        try {
            // Check if category has stock='Y'
            if (!isCategoryStockEnabled(categoryId)) {
                LOG.debug("Category {} does not have stock enabled, skipping stock update for item: {}",
                        categoryId, itemName);
                return null;
            }

            LOG.info("Adding stock for item: {}, quantity: {}, purchase bill: {}",
                    itemName, quantity, purchaseBillNo);

            // Get or create item stock record
            ItemStock itemStock = getOrCreateItemStock(itemCode, itemName, categoryId);

            Float previousStock = itemStock.getStock();
            Float newStock = previousStock + quantity;

            // Update stock
            itemStock.setStock(newStock);
            itemStock = itemStockRepository.save(itemStock);

            // Create transaction record
            ItemStockTransaction transaction = new ItemStockTransaction();
            transaction.setItemCode(itemCode);
            transaction.setItemName(itemName);
            transaction.setCategoryId(categoryId);
            transaction.setTransactionType(TRANSACTION_PURCHASE);
            transaction.setQuantity(quantity);
            transaction.setRate(rate);
            transaction.setAmount(quantity * rate);
            transaction.setPreviousStock(previousStock);
            transaction.setNewStock(newStock);
            transaction.setReferenceType(REF_PURCHASE_BILL);
            transaction.setReferenceNo(purchaseBillNo);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setRemarks("Stock added via Purchase Bill #" + purchaseBillNo);

            transactionRepository.save(transaction);

            LOG.info("Stock updated for item: {} - Previous: {}, Added: {}, New: {}",
                    itemName, previousStock, quantity, newStock);

            return itemStock;

        } catch (Exception e) {
            LOG.error("Error adding stock for item: {} - {}", itemName, e.getMessage(), e);
            throw new RuntimeException("Error adding stock: " + e.getMessage(), e);
        }
    }

    /**
     * Reduce stock for an item (called when sales bill is saved)
     * Only reduces stock if the item's category has stock='Y'
     */
    @Transactional
    public ItemStock reduceStock(Integer itemCode, String itemName, Integer categoryId,
                                  Float quantity, Float rate, Integer salesBillNo) {
        try {
            // Check if category has stock='Y'
            if (!isCategoryStockEnabled(categoryId)) {
                LOG.debug("Category {} does not have stock enabled, skipping stock update for item: {}",
                        categoryId, itemName);
                return null;
            }

            LOG.info("Reducing stock for item: {}, quantity: {}, sales bill: {}",
                    itemName, quantity, salesBillNo);

            // Get item stock record
            Optional<ItemStock> optStock = getItemStock(itemCode, itemName);
            if (optStock.isEmpty()) {
                LOG.warn("No stock record found for item: {}, creating with zero stock", itemName);
                // Create stock record with zero and allow negative (for tracking)
                ItemStock newStock = getOrCreateItemStock(itemCode, itemName, categoryId);
                optStock = Optional.of(newStock);
            }

            ItemStock itemStock = optStock.get();
            Float previousStock = itemStock.getStock();
            Float newStock = previousStock - quantity;

            // Update stock (allow negative for tracking purposes)
            itemStock.setStock(newStock);
            itemStock = itemStockRepository.save(itemStock);

            // Create transaction record
            ItemStockTransaction transaction = new ItemStockTransaction();
            transaction.setItemCode(itemCode);
            transaction.setItemName(itemName);
            transaction.setCategoryId(categoryId);
            transaction.setTransactionType(TRANSACTION_SALE);
            transaction.setQuantity(quantity);
            transaction.setRate(rate);
            transaction.setAmount(quantity * rate);
            transaction.setPreviousStock(previousStock);
            transaction.setNewStock(newStock);
            transaction.setReferenceType(REF_SALES_BILL);
            transaction.setReferenceNo(salesBillNo);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setRemarks("Stock reduced via Sales Bill #" + salesBillNo);

            transactionRepository.save(transaction);

            LOG.info("Stock reduced for item: {} - Previous: {}, Reduced: {}, New: {}",
                    itemName, previousStock, quantity, newStock);

            // Warn if stock is negative
            if (newStock < 0) {
                LOG.warn("Stock for item {} is now negative: {}", itemName, newStock);
            }

            return itemStock;

        } catch (Exception e) {
            LOG.error("Error reducing stock for item: {} - {}", itemName, e.getMessage(), e);
            throw new RuntimeException("Error reducing stock: " + e.getMessage(), e);
        }
    }

    /**
     * Reverse stock reduction for an item (called when editing a sales bill)
     * Adds back the stock that was previously reduced
     * Only reverses stock if the item's category has stock='Y'
     */
    @Transactional
    public ItemStock reverseStockForSale(Integer itemCode, String itemName, Integer categoryId,
                                          Float quantity, Float rate, Integer salesBillNo) {
        try {
            // Check if category has stock='Y'
            if (!isCategoryStockEnabled(categoryId)) {
                LOG.debug("Category {} does not have stock enabled, skipping stock reversal for item: {}",
                        categoryId, itemName);
                return null;
            }

            LOG.info("Reversing stock for item: {}, quantity: {}, sales bill: {} (bill edit)",
                    itemName, quantity, salesBillNo);

            // Get item stock record
            Optional<ItemStock> optStock = getItemStock(itemCode, itemName);
            if (optStock.isEmpty()) {
                LOG.warn("No stock record found for item: {}, creating with zero stock", itemName);
                ItemStock newStock = getOrCreateItemStock(itemCode, itemName, categoryId);
                optStock = Optional.of(newStock);
            }

            ItemStock itemStock = optStock.get();
            Float previousStock = itemStock.getStock();
            Float newStock = previousStock + quantity; // Add back the quantity

            // Update stock
            itemStock.setStock(newStock);
            itemStock = itemStockRepository.save(itemStock);

            // Create transaction record for reversal
            ItemStockTransaction transaction = new ItemStockTransaction();
            transaction.setItemCode(itemCode);
            transaction.setItemName(itemName);
            transaction.setCategoryId(categoryId);
            transaction.setTransactionType(TRANSACTION_SALE_REVERSAL);
            transaction.setQuantity(quantity);
            transaction.setRate(rate);
            transaction.setAmount(quantity * rate);
            transaction.setPreviousStock(previousStock);
            transaction.setNewStock(newStock);
            transaction.setReferenceType(REF_SALES_BILL);
            transaction.setReferenceNo(salesBillNo);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setRemarks("Stock reversed for Bill #" + salesBillNo + " edit");

            transactionRepository.save(transaction);

            LOG.info("Stock reversed for item: {} - Previous: {}, Added back: {}, New: {}",
                    itemName, previousStock, quantity, newStock);

            return itemStock;

        } catch (Exception e) {
            LOG.error("Error reversing stock for item: {} - {}", itemName, e.getMessage(), e);
            throw new RuntimeException("Error reversing stock: " + e.getMessage(), e);
        }
    }

    /**
     * Adjust stock manually
     */
    @Transactional
    public ItemStock adjustStock(Integer itemCode, String itemName, Integer categoryId,
                                  Float newStockValue, String remarks) {
        try {
            LOG.info("Adjusting stock for item: {} to: {}", itemName, newStockValue);

            ItemStock itemStock = getOrCreateItemStock(itemCode, itemName, categoryId);
            Float previousStock = itemStock.getStock();
            Float adjustmentQty = newStockValue - previousStock;

            // Update stock
            itemStock.setStock(newStockValue);
            itemStock = itemStockRepository.save(itemStock);

            // Create transaction record
            ItemStockTransaction transaction = new ItemStockTransaction();
            transaction.setItemCode(itemCode);
            transaction.setItemName(itemName);
            transaction.setCategoryId(categoryId);
            transaction.setTransactionType(TRANSACTION_ADJUSTMENT);
            transaction.setQuantity(Math.abs(adjustmentQty));
            transaction.setPreviousStock(previousStock);
            transaction.setNewStock(newStockValue);
            transaction.setReferenceType(REF_ADJUSTMENT);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setRemarks(remarks != null ? remarks : "Manual stock adjustment");

            transactionRepository.save(transaction);

            LOG.info("Stock adjusted for item: {} - Previous: {}, New: {}", itemName, previousStock, newStockValue);

            return itemStock;

        } catch (Exception e) {
            LOG.error("Error adjusting stock for item: {} - {}", itemName, e.getMessage(), e);
            throw new RuntimeException("Error adjusting stock: " + e.getMessage(), e);
        }
    }

    /**
     * Check if category has stock management enabled
     */
    public boolean isCategoryStockEnabled(Integer categoryId) {
        if (categoryId == null) {
            return false;
        }
        Optional<CategoryMaster> category = categoryRepository.findById(categoryId);
        return category.map(c -> "Y".equalsIgnoreCase(c.getStock())).orElse(false);
    }

    /**
     * Get or create item stock record
     */
    private ItemStock getOrCreateItemStock(Integer itemCode, String itemName, Integer categoryId) {
        Optional<ItemStock> optStock = getItemStock(itemCode, itemName);

        if (optStock.isPresent()) {
            return optStock.get();
        }

        // Create new stock record
        ItemStock itemStock = new ItemStock();
        itemStock.setItemCode(itemCode);
        itemStock.setItemName(itemName);
        itemStock.setCategoryId(categoryId);
        itemStock.setStock(0.0f);

        // Get category name
        if (categoryId != null) {
            Optional<CategoryMaster> category = categoryRepository.findById(categoryId);
            category.ifPresent(c -> itemStock.setCategoryName(c.getCategory()));
        }

        return itemStockRepository.save(itemStock);
    }

    /**
     * Get item stock by code or name
     */
    private Optional<ItemStock> getItemStock(Integer itemCode, String itemName) {
        if (itemCode != null) {
            Optional<ItemStock> byCode = itemStockRepository.findByItemCode(itemCode);
            if (byCode.isPresent()) {
                return byCode;
            }
        }
        if (itemName != null && !itemName.trim().isEmpty()) {
            return itemStockRepository.findByItemNameIgnoreCase(itemName);
        }
        return Optional.empty();
    }

    /**
     * Get current stock for an item
     */
    public Float getCurrentStock(Integer itemCode, String itemName) {
        Optional<ItemStock> optStock = getItemStock(itemCode, itemName);
        return optStock.map(ItemStock::getStock).orElse(0.0f);
    }

    /**
     * Get all stock items
     */
    public List<ItemStock> getAllStockItems() {
        return itemStockRepository.findAll();
    }

    /**
     * Get low stock items
     */
    public List<ItemStock> getLowStockItems() {
        return itemStockRepository.findLowStockItems();
    }

    /**
     * Get items with zero stock
     */
    public List<ItemStock> getOutOfStockItems() {
        return itemStockRepository.findByStock(0.0f);
    }

    /**
     * Get stock transactions for an item
     */
    public List<ItemStockTransaction> getItemTransactions(Integer itemCode) {
        return transactionRepository.findByItemCode(itemCode);
    }

    /**
     * Get stock transactions by date range
     */
    public List<ItemStockTransaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }

    /**
     * Get purchase transactions
     */
    public List<ItemStockTransaction> getPurchaseTransactions() {
        return transactionRepository.findPurchaseTransactions();
    }

    /**
     * Get sale transactions
     */
    public List<ItemStockTransaction> getSaleTransactions() {
        return transactionRepository.findSaleTransactions();
    }

    /**
     * Get stock by item name
     */
    public Optional<ItemStock> getStockByItemName(String itemName) {
        return itemStockRepository.findByItemNameIgnoreCase(itemName);
    }

    /**
     * Get stock by category
     */
    public List<ItemStock> getStockByCategory(Integer categoryId) {
        return itemStockRepository.findByCategoryId(categoryId);
    }
}
