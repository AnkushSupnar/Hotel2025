package com.frontend.service;

import com.frontend.entity.*;
import com.frontend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for general hotel management using database
 */
@Service
public class HotelApiService {

    private static final Logger LOG = LoggerFactory.getLogger(HotelApiService.class);

    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BillingRepository billingRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Autowired
    public HotelApiService(MenuItemRepository menuItemRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           BillingRepository billingRepository,
                           UserRepository userRepository,
                           SessionService sessionService) {
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.billingRepository = billingRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }
    
    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats() {
        LOG.debug("Fetching dashboard statistics from database");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("todaysOrders", orderRepository.findTodaysOrders().size());
        stats.put("pendingOrders", orderRepository.countByStatus(OrderStatus.PENDING));
        stats.put("completedOrders", orderRepository.countByStatus(OrderStatus.COMPLETED));
        stats.put("totalMenuItems", menuItemRepository.count());
        stats.put("availableMenuItems", menuItemRepository.findByAvailableTrue().size());

        // Get today's revenue
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        Double todayRevenue = billingRepository.getTotalRevenueByDateRange(startOfDay, endOfDay);
        stats.put("todayRevenue", todayRevenue != null ? todayRevenue : 0.0);

        LOG.info("Successfully fetched dashboard statistics");
        return stats;
    }

    /**
     * Get recent orders
     */
    public List<Map<String, Object>> getRecentOrders() {
        LOG.debug("Fetching recent orders from database");

        List<Order> orders = orderRepository.findRecentOrders();
        List<Map<String, Object>> orderMaps = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("tableNumber", order.getTableNumber());
            orderMap.put("customerName", order.getCustomerName());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus().toString());
            orderMap.put("createdAt", order.getCreatedAt());
            orderMaps.add(orderMap);
        }

        LOG.info("Successfully fetched {} recent orders", orderMaps.size());
        return orderMaps;
    }

    /**
     * Get all menu items
     */
    public List<Map<String, Object>> getMenuItems() {
        LOG.debug("Fetching menu items from database");

        List<MenuItem> menuItems = menuItemRepository.findAll();
        List<Map<String, Object>> menuItemMaps = new ArrayList<>();

        for (MenuItem item : menuItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("name", item.getName());
            itemMap.put("description", item.getDescription());
            itemMap.put("price", item.getPrice());
            itemMap.put("available", item.getAvailable());
            if (item.getCategory() != null) {
                itemMap.put("categoryId", item.getCategory().getId());
                itemMap.put("categoryName", item.getCategory().getCategory());
            }
            menuItemMaps.add(itemMap);
        }

        LOG.info("Successfully fetched {} menu items", menuItemMaps.size());
        return menuItemMaps;
    }
    
    /**
     * Create a new menu item
     */
    public boolean createMenuItem(Map<String, Object> menuItem) {
        LOG.debug("Creating new menu item: {}", menuItem.get("name"));

        MenuItem item = new MenuItem();
        item.setName((String) menuItem.get("name"));
        item.setDescription((String) menuItem.get("description"));
        item.setPrice(new BigDecimal(menuItem.get("price").toString()));
        item.setAvailable((Boolean) menuItem.getOrDefault("available", true));

        menuItemRepository.save(item);

        LOG.info("Successfully created menu item: {}", menuItem.get("name"));
        return true;
    }

    /**
     * Update a menu item
     */
    public boolean updateMenuItem(Long itemId, Map<String, Object> menuItem) {
        LOG.debug("Updating menu item with ID: {}", itemId);

        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        item.setName((String) menuItem.get("name"));
        item.setDescription((String) menuItem.get("description"));
        item.setPrice(new BigDecimal(menuItem.get("price").toString()));
        item.setAvailable((Boolean) menuItem.getOrDefault("available", true));

        menuItemRepository.save(item);

        LOG.info("Successfully updated menu item with ID: {}", itemId);
        return true;
    }

    /**
     * Delete a menu item
     */
    public boolean deleteMenuItem(Long itemId) {
        LOG.debug("Deleting menu item with ID: {}", itemId);

        if (!menuItemRepository.existsById(itemId)) {
            throw new RuntimeException("Menu item not found");
        }

        menuItemRepository.deleteById(itemId);

        LOG.info("Successfully deleted menu item with ID: {}", itemId);
        return true;
    }
    
    /**
     * Get all orders
     */
    public List<Map<String, Object>> getAllOrders() {
        LOG.debug("Fetching all orders from database");

        List<Order> orders = orderRepository.findAll();
        List<Map<String, Object>> orderMaps = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("tableNumber", order.getTableNumber());
            orderMap.put("customerName", order.getCustomerName());
            orderMap.put("customerPhone", order.getCustomerPhone());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus().toString());
            orderMap.put("createdAt", order.getCreatedAt());
            orderMaps.add(orderMap);
        }

        LOG.info("Successfully fetched {} orders", orderMaps.size());
        return orderMaps;
    }

    /**
     * Create a new order
     */
    public boolean createOrder(Map<String, Object> order) {
        LOG.debug("Creating new order for table: {}", order.get("tableNumber"));

        Order newOrder = new Order();
        newOrder.setTableNumber((Integer) order.get("tableNumber"));
        newOrder.setCustomerName((String) order.get("customerName"));
        newOrder.setCustomerPhone((String) order.get("customerPhone"));
        newOrder.setStatus(OrderStatus.PENDING);

        // Get current user if available
        String username = sessionService.getCurrentUsername();
        if (username != null) {
            userRepository.findByUsername(username)
                    .ifPresent(newOrder::setCreatedBy);
        }

        orderRepository.save(newOrder);

        LOG.info("Successfully created order for table: {}", order.get("tableNumber"));
        return true;
    }
    
    /**
     * Generate bill for an order
     */
    public Map<String, Object> generateBill(Long orderId) {
        LOG.debug("Generating bill for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Calculate billing amounts
        order.calculateTotalAmount();
        BigDecimal subtotal = order.getTotalAmount();
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.18")); // 18% tax
        BigDecimal totalAmount = subtotal.add(taxAmount);

        // Create billing
        Billing billing = new Billing();
        billing.setOrder(order);
        billing.setSubtotal(subtotal);
        billing.setTaxAmount(taxAmount);
        billing.setDiscountAmount(BigDecimal.ZERO);
        billing.setTotalAmount(totalAmount);
        billing.setPaymentStatus(PaymentStatus.PENDING);

        // Get current user if available
        String username = sessionService.getCurrentUsername();
        if (username != null) {
            userRepository.findByUsername(username)
                    .ifPresent(billing::setCreatedBy);
        }

        Billing savedBilling = billingRepository.save(billing);

        // Update order status
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        // Prepare response
        Map<String, Object> billMap = new HashMap<>();
        billMap.put("id", savedBilling.getId());
        billMap.put("billNumber", savedBilling.getBillNumber());
        billMap.put("orderId", order.getId());
        billMap.put("subtotal", savedBilling.getSubtotal());
        billMap.put("taxAmount", savedBilling.getTaxAmount());
        billMap.put("totalAmount", savedBilling.getTotalAmount());
        billMap.put("paymentStatus", savedBilling.getPaymentStatus().toString());
        billMap.put("createdAt", savedBilling.getCreatedAt());

        LOG.info("Successfully generated bill for order ID: {}", orderId);
        return billMap;
    }

    /**
     * Get daily billing report
     */
    public Map<String, Object> getDailyBillingReport() {
        LOG.debug("Fetching daily billing report from database");

        List<Billing> todaysBillings = billingRepository.findTodaysBillings();

        Map<String, Object> report = new HashMap<>();
        report.put("totalBills", todaysBillings.size());
        report.put("paidBills", todaysBillings.stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                .count());
        report.put("pendingBills", todaysBillings.stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PENDING)
                .count());

        BigDecimal totalRevenue = todaysBillings.stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                .map(Billing::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("totalRevenue", totalRevenue);

        LOG.info("Successfully fetched daily billing report");
        return report;
    }

    /**
     * Test if user has valid session and database connection
     */
    public boolean hasValidSession() {
        try {
            return sessionService.isLoggedIn() && orderRepository.count() >= 0;
        } catch (Exception e) {
            LOG.error("Session validation failed: {}", e.getMessage());
            return false;
        }
    }
}