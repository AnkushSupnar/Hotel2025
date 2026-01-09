package com.frontend.service;

import com.frontend.entity.Bill;
import com.frontend.entity.Transaction;
import com.frontend.repository.BillRepository;
import com.frontend.repository.CustomerRepository;
import com.frontend.repository.EmployeesRepository;
import com.frontend.repository.ItemRepository;
import com.frontend.repository.PurchaseBillRepository;
import com.frontend.repository.TableMasterRepository;
import com.frontend.repository.TempTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for providing dashboard data and statistics.
 * Consolidates data from multiple sources for the dashboard view.
 */
@Service
public class DashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PurchaseBillRepository purchaseBillRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EmployeesRepository employeesRepository;

    @Autowired
    private TableMasterRepository tableMasterRepository;

    @Autowired
    private TempTransactionRepository tempTransactionRepository;

    @Autowired
    private BillService billService;

    @Autowired
    private PurchaseBillService purchaseBillService;

    @Autowired
    private BankService bankService;

    // =====================================================
    // PRIMARY KPI METHODS
    // =====================================================

    /**
     * Get today's total sales amount (PAID + CREDIT bills)
     */
    public Float getTodaysSales() {
        try {
            Float sales = billService.getTodaysTotalSales();
            return sales != null ? sales : 0f;
        } catch (Exception e) {
            LOG.error("Error getting today's sales: ", e);
            return 0f;
        }
    }

    /**
     * Get today's total order count
     */
    public Long getTodaysOrderCount() {
        try {
            return billService.getTodaysBillCount();
        } catch (Exception e) {
            LOG.error("Error getting today's order count: ", e);
            return 0L;
        }
    }

    /**
     * Get today's total purchase amount
     */
    public Float getTodaysPurchase() {
        try {
            Double purchase = purchaseBillService.getTodaysTotalPurchase();
            return purchase != null ? purchase.floatValue() : 0f;
        } catch (Exception e) {
            LOG.error("Error getting today's purchase: ", e);
            return 0f;
        }
    }

    /**
     * Get total pending credit amount from customers
     */
    public Float getPendingCreditAmount() {
        try {
            Double credit = billService.getTotalCreditBalance();
            return credit != null ? credit.floatValue() : 0f;
        } catch (Exception e) {
            LOG.error("Error getting pending credit: ", e);
            return 0f;
        }
    }

    /**
     * Get count of bills with pending credit
     */
    public Long getPendingCreditCount() {
        try {
            return billRepository.countByStatus("CREDIT");
        } catch (Exception e) {
            LOG.error("Error getting pending credit count: ", e);
            return 0L;
        }
    }

    // =====================================================
    // FINANCIAL SUMMARY METHODS
    // =====================================================

    /**
     * Get today's gross profit (Sales - Purchase)
     */
    public Float getGrossProfit() {
        Float sales = getTodaysSales();
        Float purchase = getTodaysPurchase();
        return sales - purchase;
    }

    /**
     * Get total cash in hand (bank balance)
     */
    public Float getCashInHand() {
        try {
            Double balance = bankService.getTotalActiveBalance();
            return balance != null ? balance.floatValue() : 0f;
        } catch (Exception e) {
            LOG.error("Error getting cash in hand: ", e);
            return 0f;
        }
    }

    /**
     * Get total payments due to suppliers
     */
    public Float getPaymentsDue() {
        try {
            Double payable = purchaseBillRepository.getTotalPendingPayable();
            return payable != null ? payable.floatValue() : 0f;
        } catch (Exception e) {
            LOG.error("Error getting payments due: ", e);
            return 0f;
        }
    }

    /**
     * Get average order value for today
     */
    public Float getAverageOrderValue() {
        Float sales = getTodaysSales();
        Long count = getTodaysOrderCount();
        if (count == null || count == 0) {
            return 0f;
        }
        return sales / count;
    }

    // =====================================================
    // CHART DATA METHODS
    // =====================================================

    /**
     * Get last 7 days sales data for chart
     * @return Map with date as key and sales amount as value
     */
    public Map<String, Float> getLast7DaysSales() {
        Map<String, Float> salesData = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            try {
                Float amount = billRepository.getTotalSalesAmountByDate(dateStr);
                salesData.put(date.getDayOfWeek().toString().substring(0, 3), amount != null ? amount : 0f);
            } catch (Exception e) {
                salesData.put(date.getDayOfWeek().toString().substring(0, 3), 0f);
            }
        }

        return salesData;
    }

    /**
     * Get last 7 days purchase data for chart
     * @return Map with date as key and purchase amount as value
     */
    public Map<String, Float> getLast7DaysPurchase() {
        Map<String, Float> purchaseData = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            try {
                Double amount = purchaseBillService.getTotalPurchaseByDate(date);
                purchaseData.put(date.getDayOfWeek().toString().substring(0, 3),
                        amount != null ? amount.floatValue() : 0f);
            } catch (Exception e) {
                purchaseData.put(date.getDayOfWeek().toString().substring(0, 3), 0f);
            }
        }

        return purchaseData;
    }

    // =====================================================
    // OPERATIONAL METRICS
    // =====================================================

    /**
     * Get table status - total and active count
     * Active tables = tables with temp_transaction entries OR bills with CLOSE status
     * @return Map with "total" and "active" keys
     */
    public Map<String, Long> getTableStatus() {
        Map<String, Long> status = new HashMap<>();
        try {
            long total = tableMasterRepository.count();

            // Get tables with temp transactions (orders in progress, bill not yet created)
            List<Integer> tempActiveTables = tempTransactionRepository.findDistinctActiveTableNumbers();
            Set<Integer> activeTableSet = new HashSet<>(tempActiveTables != null ? tempActiveTables : Collections.emptyList());

            // Also get tables with open bills (status = CLOSE means bill created but not paid)
            Long billActiveTables = billRepository.countDistinctActiveTablesByStatus("CLOSE");

            // For bill tables, we need to get the actual table numbers to merge with temp tables
            // But since we just need count, let's use a combined approach
            // Actually, let's get distinct count properly by fetching both sets

            // Get table numbers from bills with CLOSE status
            List<Bill> closeBills = billRepository.findByStatus("CLOSE");
            for (Bill bill : closeBills) {
                if (bill.getTableNo() != null) {
                    activeTableSet.add(bill.getTableNo());
                }
            }

            long active = activeTableSet.size();

            status.put("total", total);
            status.put("active", active);
            status.put("available", Math.max(0, total - active));
        } catch (Exception e) {
            LOG.error("Error getting table status: ", e);
            status.put("total", 0L);
            status.put("active", 0L);
            status.put("available", 0L);
        }
        return status;
    }

    /**
     * Get order status distribution for today
     * @return Map with status as key and count as value
     */
    public Map<String, Long> getOrderStatus() {
        Map<String, Long> status = new LinkedHashMap<>();
        String today = LocalDate.now().format(DATE_FORMATTER);

        try {
            // Get counts by status for today
            Long paidCount = billRepository.countByBillDateAndStatus(today, "PAID");
            Long creditCount = billRepository.countByBillDateAndStatus(today, "CREDIT");
            Long closeCount = billRepository.countByBillDateAndStatus(today, "CLOSE");

            status.put("PAID", paidCount != null ? paidCount : 0L);
            status.put("CREDIT", creditCount != null ? creditCount : 0L);
            status.put("PENDING", closeCount != null ? closeCount : 0L);
        } catch (Exception e) {
            LOG.error("Error getting order status: ", e);
            status.put("PAID", 0L);
            status.put("CREDIT", 0L);
            status.put("PENDING", 0L);
        }

        return status;
    }

    /**
     * Get top selling items for today
     * @param limit number of items to return
     * @return List of maps with item name and quantity
     */
    public List<Map<String, Object>> getTopSellingItems(int limit) {
        List<Map<String, Object>> topItems = new ArrayList<>();
        String today = LocalDate.now().format(DATE_FORMATTER);

        try {
            // Get all bills for today with transactions eagerly loaded
            List<Bill> todaysBills = billRepository.findByBillDateWithTransactions(today);

            // Aggregate items across all bills using itemName as key
            Map<String, Integer> itemCounts = new HashMap<>();

            for (Bill bill : todaysBills) {
                if (bill.getTransactions() != null) {
                    for (Transaction tx : bill.getTransactions()) {
                        String itemName = tx.getItemName();
                        if (itemName != null && !itemName.isEmpty()) {
                            int qty = tx.getQty() != null ? tx.getQty().intValue() : 0;
                            itemCounts.merge(itemName, qty, Integer::sum);
                        }
                    }
                }
            }

            // Sort by quantity and take top items
            List<Map.Entry<String, Integer>> sorted = itemCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            int rank = 1;
            for (Map.Entry<String, Integer> entry : sorted) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("rank", rank++);
                item.put("name", entry.getKey());
                item.put("quantity", entry.getValue());
                topItems.add(item);
            }

        } catch (Exception e) {
            LOG.error("Error getting top selling items: ", e);
        }

        return topItems;
    }

    // =====================================================
    // RECENT ACTIVITY
    // =====================================================

    /**
     * Get recent transactions
     * @param limit number of transactions to return
     * @return List of recent bill summaries
     */
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        List<Map<String, Object>> transactions = new ArrayList<>();

        try {
            // Get recent bills ordered by creation time
            List<Bill> recentBills = billRepository.findTop10ByOrderByCreatedAtDesc();

            for (Bill bill : recentBills) {
                if (transactions.size() >= limit) break;

                // Get customer full name if customerId exists
                String customerName = "Walk-in";
                if (bill.getCustomerId() != null) {
                    try {
                        customerName = customerRepository.findById(bill.getCustomerId())
                                .map(c -> {
                                    // Build full name, handling null/empty parts
                                    StringBuilder fullName = new StringBuilder();
                                    if (c.getFirstName() != null && !c.getFirstName().trim().isEmpty()) {
                                        fullName.append(c.getFirstName().trim());
                                    }
                                    if (c.getMiddleName() != null && !c.getMiddleName().trim().isEmpty()) {
                                        if (fullName.length() > 0) fullName.append(" ");
                                        fullName.append(c.getMiddleName().trim());
                                    }
                                    if (c.getLastName() != null && !c.getLastName().trim().isEmpty()) {
                                        if (fullName.length() > 0) fullName.append(" ");
                                        fullName.append(c.getLastName().trim());
                                    }
                                    return fullName.length() > 0 ? fullName.toString() : "Customer";
                                })
                                .orElse("Walk-in");
                    } catch (Exception e) {
                        // Ignore lookup errors
                    }
                }

                Map<String, Object> tx = new LinkedHashMap<>();
                tx.put("billNo", bill.getBillNo());
                tx.put("amount", bill.getNetAmount() != null ? bill.getNetAmount() : 0f);
                tx.put("customer", customerName);
                tx.put("status", bill.getStatus());
                tx.put("time", getTimeAgo(bill.getCreatedAt()));
                tx.put("createdAt", bill.getCreatedAt());
                transactions.add(tx);
            }

        } catch (Exception e) {
            LOG.error("Error getting recent transactions: ", e);
        }

        return transactions;
    }

    /**
     * Convert timestamp to "X min ago" format
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        if (minutes < 1440) return (minutes / 60) + " hr ago";
        return (minutes / 1440) + " days ago";
    }

    // =====================================================
    // MONTHLY TARGET DATA
    // =====================================================

    /**
     * Get monthly sales target data
     * @return Map with currentSales, targetAmount, daysRemaining
     */
    public Map<String, Object> getMonthlyTargetData() {
        Map<String, Object> targetData = new LinkedHashMap<>();

        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

            // Get current month's total sales
            Float currentMonthSales = 0f;
            for (LocalDate date = startOfMonth; !date.isAfter(today); date = date.plusDays(1)) {
                String dateStr = date.format(DATE_FORMATTER);
                try {
                    Float daySales = billRepository.getTotalSalesAmountByDate(dateStr);
                    if (daySales != null) {
                        currentMonthSales += daySales;
                    }
                } catch (Exception e) {
                    // Skip this date
                }
            }

            // Calculate days remaining in month
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, endOfMonth);

            // Get monthly target from settings or use default
            // For now, using a calculated target based on average daily sales * days in month
            Float avgDailySales = getAverageDailySalesLast30Days();
            Float calculatedTarget = avgDailySales * today.lengthOfMonth();

            // Use a minimum target of ₹1,00,000 if calculated is too low
            Float targetAmount = Math.max(calculatedTarget, 100000f);

            targetData.put("currentSales", currentMonthSales);
            targetData.put("targetAmount", targetAmount);
            targetData.put("daysRemaining", (int) daysRemaining);

        } catch (Exception e) {
            LOG.error("Error getting monthly target data: ", e);
            targetData.put("currentSales", 0f);
            targetData.put("targetAmount", 100000f);
            targetData.put("daysRemaining", 30);
        }

        return targetData;
    }

    /**
     * Get average daily sales for last 30 days
     */
    private Float getAverageDailySalesLast30Days() {
        Float totalSales = 0f;
        int daysWithSales = 0;
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 30; i++) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            try {
                Float daySales = billRepository.getTotalSalesAmountByDate(dateStr);
                if (daySales != null && daySales > 0) {
                    totalSales += daySales;
                    daysWithSales++;
                }
            } catch (Exception e) {
                // Skip this date
            }
        }

        return daysWithSales > 0 ? totalSales / daysWithSales : 5000f; // Default ₹5000/day
    }

    // =====================================================
    // PAYMENT METHODS BREAKDOWN
    // =====================================================

    /**
     * Get today's payment breakdown by method (CASH, BANK, CREDIT)
     * @return Map with payment method as key and amount as value
     */
    public Map<String, Float> getPaymentMethodsBreakdown() {
        Map<String, Float> paymentMethods = new LinkedHashMap<>();
        String today = LocalDate.now().format(DATE_FORMATTER);

        try {
            // Get PAID bills (CASH payments)
            Float cashAmount = billRepository.getTotalBillAmountByDateAndStatus(today, "PAID");
            paymentMethods.put("CASH", cashAmount != null ? cashAmount : 0f);

            // Get BANK payments - from bills paid via bank
            // This would need a payment mode field in bills, for now use 0
            Float bankAmount = 0f;
            // If you have bank payments tracked separately, add that logic here
            paymentMethods.put("BANK", bankAmount);

            // Get CREDIT bills (pending payment)
            Float creditAmount = billRepository.getTotalBillAmountByDateAndStatus(today, "CREDIT");
            paymentMethods.put("CREDIT", creditAmount != null ? creditAmount : 0f);

        } catch (Exception e) {
            LOG.error("Error getting payment methods breakdown: ", e);
            paymentMethods.put("CASH", 0f);
            paymentMethods.put("BANK", 0f);
            paymentMethods.put("CREDIT", 0f);
        }

        return paymentMethods;
    }

    // =====================================================
    // YESTERDAY'S DATA FOR TREND COMPARISON
    // =====================================================

    /**
     * Get yesterday's sales for trend comparison
     */
    public Float getYesterdaySales() {
        try {
            String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
            Float sales = billRepository.getTotalSalesAmountByDate(yesterday);
            return sales != null ? sales : 0f;
        } catch (Exception e) {
            LOG.error("Error getting yesterday's sales: ", e);
            return 0f;
        }
    }

    /**
     * Get yesterday's order count for trend comparison
     */
    public Long getYesterdayOrderCount() {
        try {
            String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
            Long count = billRepository.countByBillDate(yesterday);
            return count != null ? count : 0L;
        } catch (Exception e) {
            LOG.error("Error getting yesterday's order count: ", e);
            return 0L;
        }
    }

    /**
     * Get yesterday's purchase for trend comparison
     */
    public Float getYesterdayPurchase() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Double purchase = purchaseBillService.getTotalPurchaseByDate(yesterday);
            return purchase != null ? purchase.floatValue() : 0f;
        } catch (Exception e) {
            LOG.error("Error getting yesterday's purchase: ", e);
            return 0f;
        }
    }

    // =====================================================
    // LOW STOCK ALERTS
    // =====================================================

    /**
     * Get items with low stock (below minimum threshold)
     * Note: Currently Item entity represents menu items without stock tracking.
     * This method returns empty list. When raw materials/inventory tracking
     * is implemented, this can be updated to query that repository.
     * @return List of items with low stock (currently empty)
     */
    public List<Map<String, Object>> getLowStockItems() {
        List<Map<String, Object>> lowStockItems = new ArrayList<>();

        // Note: The Item entity is for menu items (dishes) which don't have stock tracking.
        // If you need stock alerts for raw materials/ingredients, create a separate
        // RawMaterial or Inventory entity with currentStock and minStock fields.
        // For now, returning empty list to avoid errors.

        return lowStockItems;
    }

    // =====================================================
    // NOTIFICATION COUNT
    // =====================================================

    /**
     * Get notification count for badge
     * Includes: low stock items, pending credits, pending bills
     */
    public Integer getNotificationCount() {
        int count = 0;

        try {
            // Count low stock items
            count += getLowStockItems().size();

            // Count pending credit bills
            Long creditCount = getPendingCreditCount();
            if (creditCount != null && creditCount > 0) {
                count += creditCount.intValue();
            }

        } catch (Exception e) {
            LOG.error("Error getting notification count: ", e);
        }

        return count;
    }

    // =====================================================
    // FOOTER STATISTICS
    // =====================================================

    /**
     * Get dashboard footer statistics
     * @return Map with various counts
     */
    public Map<String, Long> getFooterStats() {
        Map<String, Long> stats = new LinkedHashMap<>();

        try {
            stats.put("customers", customerRepository.count());
            stats.put("items", itemRepository.count());
            stats.put("employees", employeesRepository.count());
            stats.put("tables", tableMasterRepository.count());
        } catch (Exception e) {
            LOG.error("Error getting footer stats: ", e);
            stats.put("customers", 0L);
            stats.put("items", 0L);
            stats.put("employees", 0L);
            stats.put("tables", 0L);
        }

        return stats;
    }

    // =====================================================
    // COMPREHENSIVE DASHBOARD DATA
    // =====================================================

    /**
     * Get all dashboard data in one call
     * @return Map containing all dashboard metrics
     */
    public Map<String, Object> getAllDashboardData() {
        Map<String, Object> data = new LinkedHashMap<>();

        LOG.info("Loading all dashboard data...");

        // Primary KPIs
        data.put("todaysSales", getTodaysSales());
        data.put("todaysOrders", getTodaysOrderCount());
        data.put("todaysPurchase", getTodaysPurchase());
        data.put("pendingCredit", getPendingCreditAmount());
        data.put("pendingCreditCount", getPendingCreditCount());

        // Yesterday's data for trend comparison
        data.put("yesterdaySales", getYesterdaySales());
        data.put("yesterdayOrders", getYesterdayOrderCount());
        data.put("yesterdayPurchase", getYesterdayPurchase());

        // Financial Summary
        data.put("grossProfit", getGrossProfit());
        data.put("cashInHand", getCashInHand());
        data.put("paymentsDue", getPaymentsDue());
        data.put("avgOrderValue", getAverageOrderValue());

        // Chart Data
        data.put("last7DaysSales", getLast7DaysSales());
        data.put("last7DaysPurchase", getLast7DaysPurchase());

        // Operational Metrics
        data.put("tableStatus", getTableStatus());
        data.put("orderStatus", getOrderStatus());
        data.put("topSellingItems", getTopSellingItems(5));

        // Recent Activity
        data.put("recentTransactions", getRecentTransactions(5));

        // Footer Stats
        data.put("footerStats", getFooterStats());

        // NEW: Monthly Target Data
        data.put("monthlyTarget", getMonthlyTargetData());

        // NEW: Payment Methods Breakdown (Cash, Bank, Credit)
        data.put("paymentMethods", getPaymentMethodsBreakdown());

        // NEW: Low Stock Alerts
        data.put("lowStockItems", getLowStockItems());

        // NEW: Notification Count
        data.put("notificationCount", getNotificationCount());

        LOG.info("Dashboard data loaded successfully");

        return data;
    }
}
