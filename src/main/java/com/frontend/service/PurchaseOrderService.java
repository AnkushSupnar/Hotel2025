package com.frontend.service;

import com.frontend.entity.Item;
import com.frontend.entity.PurchaseOrder;
import com.frontend.entity.PurchaseOrderTransaction;
import com.frontend.repository.PurchaseOrderRepository;
import com.frontend.repository.PurchaseOrderTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for PurchaseOrder operations
 * Handles purchase order creation, saving, and management
 */
@Service
public class PurchaseOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderService.class);

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderTransactionRepository purchaseOrderTransactionRepository;

    @Autowired
    private ItemService itemService;

    /**
     * Create and save a new purchase order
     */
    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrder order, List<PurchaseOrderTransaction> transactions) {
        try {
            LOG.info("Creating purchase order for supplier ID: {}", order.getPartyId());

            // Calculate totals
            double totalQty = 0.0;
            for (PurchaseOrderTransaction trans : transactions) {
                totalQty += trans.getQty();
            }

            order.setTotalQty(totalQty);
            order.setTotalItems(transactions.size());

            // Save order first to get the order number
            PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
            LOG.info("Purchase order created with number: {}", savedOrder.getOrderNo());

            // Add transactions to order
            for (PurchaseOrderTransaction trans : transactions) {
                trans.setPurchaseOrder(savedOrder);

                // Get item_code from Item entity by item name if available
                Optional<Item> itemOpt = itemService.getItemByName(trans.getItemName());
                if (itemOpt.isPresent()) {
                    trans.setItemCode(itemOpt.get().getItemCode());
                    trans.setCategoryId(itemOpt.get().getCategoryId());
                }

                savedOrder.addTransaction(trans);
            }

            // Save order with transactions
            savedOrder = purchaseOrderRepository.save(savedOrder);
            LOG.info("Purchase order {} saved with {} transactions",
                    savedOrder.getOrderNo(), savedOrder.getTransactions().size());

            return savedOrder;

        } catch (Exception e) {
            LOG.error("Error creating purchase order", e);
            throw new RuntimeException("Error creating purchase order: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing purchase order
     */
    @Transactional
    public PurchaseOrder updatePurchaseOrder(Integer orderNo, PurchaseOrder updatedOrder,
                                              List<PurchaseOrderTransaction> transactions) {
        try {
            Optional<PurchaseOrder> optOrder = purchaseOrderRepository.findById(orderNo);
            if (optOrder.isEmpty()) {
                throw new RuntimeException("Purchase order not found: " + orderNo);
            }

            PurchaseOrder order = optOrder.get();

            // Delete existing transactions
            purchaseOrderTransactionRepository.deleteByOrderNo(orderNo);
            order.getTransactions().clear();

            // Update order properties
            order.setPartyId(updatedOrder.getPartyId());
            order.setOrderDate(updatedOrder.getOrderDate());
            order.setRemarks(updatedOrder.getRemarks());
            order.setStatus(updatedOrder.getStatus());

            // Calculate totals
            double totalQty = 0.0;
            for (PurchaseOrderTransaction trans : transactions) {
                totalQty += trans.getQty();
            }

            order.setTotalQty(totalQty);
            order.setTotalItems(transactions.size());

            // Add new transactions
            for (PurchaseOrderTransaction trans : transactions) {
                trans.setPurchaseOrder(order);

                Optional<Item> itemOpt = itemService.getItemByName(trans.getItemName());
                if (itemOpt.isPresent()) {
                    trans.setItemCode(itemOpt.get().getItemCode());
                    trans.setCategoryId(itemOpt.get().getCategoryId());
                }

                order.addTransaction(trans);
            }

            PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
            LOG.info("Purchase order {} updated with {} transactions",
                    savedOrder.getOrderNo(), savedOrder.getTransactions().size());

            return savedOrder;

        } catch (Exception e) {
            LOG.error("Error updating purchase order {}", orderNo, e);
            throw new RuntimeException("Error updating purchase order: " + e.getMessage(), e);
        }
    }

    /**
     * Mark purchase order as completed
     */
    @Transactional
    public PurchaseOrder markAsCompleted(Integer orderNo) {
        try {
            Optional<PurchaseOrder> optOrder = purchaseOrderRepository.findById(orderNo);
            if (optOrder.isEmpty()) {
                throw new RuntimeException("Purchase order not found: " + orderNo);
            }

            PurchaseOrder order = optOrder.get();
            order.setStatus("COMPLETED");

            PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
            LOG.info("Purchase order {} marked as COMPLETED", orderNo);

            return savedOrder;

        } catch (Exception e) {
            LOG.error("Error marking purchase order {} as completed", orderNo, e);
            throw new RuntimeException("Error updating purchase order: " + e.getMessage(), e);
        }
    }

    /**
     * Mark purchase order as cancelled
     */
    @Transactional
    public PurchaseOrder markAsCancelled(Integer orderNo) {
        try {
            Optional<PurchaseOrder> optOrder = purchaseOrderRepository.findById(orderNo);
            if (optOrder.isEmpty()) {
                throw new RuntimeException("Purchase order not found: " + orderNo);
            }

            PurchaseOrder order = optOrder.get();
            order.setStatus("CANCELLED");

            PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
            LOG.info("Purchase order {} marked as CANCELLED", orderNo);

            return savedOrder;

        } catch (Exception e) {
            LOG.error("Error marking purchase order {} as cancelled", orderNo, e);
            throw new RuntimeException("Error updating purchase order: " + e.getMessage(), e);
        }
    }

    /**
     * Get purchase order by order number
     */
    public Optional<PurchaseOrder> getOrderByNo(Integer orderNo) {
        return purchaseOrderRepository.findById(orderNo);
    }

    /**
     * Get purchase order with transactions eagerly loaded
     */
    @Transactional(readOnly = true)
    public PurchaseOrder getOrderWithTransactions(Integer orderNo) {
        Optional<PurchaseOrder> optOrder = purchaseOrderRepository.findById(orderNo);
        if (optOrder.isPresent()) {
            PurchaseOrder order = optOrder.get();
            // Eagerly load transactions
            order.getTransactions().size();
            return order;
        }
        return null;
    }

    /**
     * Get all purchase orders
     */
    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAllByOrderByOrderNoDesc();
    }

    /**
     * Get purchase orders by supplier
     */
    public List<PurchaseOrder> getOrdersBySupplier(Integer supplierId) {
        return purchaseOrderRepository.findByPartyId(supplierId);
    }

    /**
     * Get purchase orders by date
     */
    public List<PurchaseOrder> getOrdersByDate(LocalDate date) {
        return purchaseOrderRepository.findByOrderDate(date);
    }

    /**
     * Get purchase orders by date range
     */
    public List<PurchaseOrder> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        return purchaseOrderRepository.findByOrderDateBetween(startDate, endDate);
    }

    /**
     * Get purchase orders by status
     */
    public List<PurchaseOrder> getOrdersByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    /**
     * Get pending orders
     */
    public List<PurchaseOrder> getPendingOrders() {
        return purchaseOrderRepository.findAllPendingOrders();
    }

    /**
     * Get pending orders for a supplier
     */
    public List<PurchaseOrder> getPendingOrdersBySupplier(Integer supplierId) {
        return purchaseOrderRepository.findPendingOrdersBySupplier(supplierId);
    }

    /**
     * Get transactions for a purchase order
     */
    public List<PurchaseOrderTransaction> getTransactionsForOrder(Integer orderNo) {
        return purchaseOrderTransactionRepository.findByOrderNo(orderNo);
    }

    /**
     * Delete purchase order
     */
    @Transactional
    public void deleteOrder(Integer orderNo) {
        purchaseOrderRepository.deleteById(orderNo);
        LOG.info("Purchase order {} deleted", orderNo);
    }

    /**
     * Save purchase order
     */
    @Transactional
    public PurchaseOrder saveOrder(PurchaseOrder order) {
        return purchaseOrderRepository.save(order);
    }

    /**
     * Get today's purchase orders
     */
    public List<PurchaseOrder> getTodaysOrders() {
        return purchaseOrderRepository.findByOrderDate(LocalDate.now());
    }

    /**
     * Get order count by date
     */
    public long getOrderCountByDate(LocalDate date) {
        return purchaseOrderRepository.countByOrderDate(date);
    }

    /**
     * Get pending order count
     */
    public long getPendingOrderCount() {
        return purchaseOrderRepository.countByStatus("PENDING");
    }
}
