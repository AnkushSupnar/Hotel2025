package com.frontend.controller.dashboard;

import com.frontend.config.SpringFXMLLoader;
import com.frontend.enums.ScreenPermission;
import com.frontend.service.DashboardService;
import com.frontend.service.SessionService;
import com.frontend.util.NavigationGuard;
import com.frontend.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the comprehensive Dashboard view.
 * Loads and displays real-time KPIs, charts, and operational metrics.
 */
@Component
public class DashboardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0");

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private NavigationGuard navigationGuard;

    @Autowired
    private AlertNotification alertNotification;

    // Primary KPI Labels
    @FXML private Label lblTodaysSales;
    @FXML private Label lblTodaysOrders;
    @FXML private Label lblTodaysPurchase;
    @FXML private Label lblPendingCredit;
    @FXML private Label lblPendingCreditCount;

    // Financial Summary Labels
    @FXML private Label lblGrossProfit;
    @FXML private Label lblCashInHand;
    @FXML private Label lblPaymentsDue;
    @FXML private Label lblAvgOrderValue;

    // Charts
    @FXML private LineChart<String, Number> salesTrendChart;
    @FXML private CategoryAxis salesChartXAxis;
    @FXML private NumberAxis salesChartYAxis;
    @FXML private BarChart<String, Number> salesVsPurchaseChart;
    @FXML private CategoryAxis comparisonChartXAxis;
    @FXML private NumberAxis comparisonChartYAxis;

    // Table Status
    @FXML private VBox tableStatusContainer;
    @FXML private Label lblTotalTables;
    @FXML private Label lblActiveTables;
    @FXML private Label lblAvailableTables;

    // Order Status
    @FXML private Label lblPaidCount;
    @FXML private Label lblCreditCount;
    @FXML private Label lblPendingCount;
    @FXML private ProgressBar paidProgress;
    @FXML private ProgressBar creditProgress;
    @FXML private ProgressBar pendingProgress;

    // Top Selling Items
    @FXML private VBox topItemsContainer;

    // Quick Action Buttons
    @FXML private Button btnRefresh;
    @FXML private Button btnNewOrder;
    @FXML private Button btnReceivePayment;
    @FXML private Button btnNewPurchase;
    @FXML private Button btnViewReports;

    // Recent Transactions
    @FXML private VBox recentTransactionsContainer;

    // Footer Stats
    @FXML private Label lblTotalCustomers;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalEmployees;
    @FXML private Label lblTotalTablesFooter;
    @FXML private FontAwesomeIcon statusIcon;
    @FXML private Label lblSystemStatus;

    // NEW: Greeting and DateTime
    @FXML private Label lblGreeting;
    @FXML private Label lblCurrentDate;
    @FXML private Label lblCurrentTime;

    // NEW: Notification
    @FXML private Button btnNotification;
    @FXML private Label lblNotificationBadge;

    // NEW: Trend Indicators
    @FXML private HBox salesTrendBox;
    @FXML private FontAwesomeIcon salesTrendIcon;
    @FXML private Label lblSalesTrend;
    @FXML private HBox ordersTrendBox;
    @FXML private FontAwesomeIcon ordersTrendIcon;
    @FXML private Label lblOrdersTrend;
    @FXML private HBox purchaseTrendBox;
    @FXML private FontAwesomeIcon purchaseTrendIcon;
    @FXML private Label lblPurchaseTrend;

    // NEW: Low Stock Alerts
    @FXML private Label lblLowStockCount;
    @FXML private VBox lowStockContainer;

    // NEW: Payment Breakdown (Cash, Bank, Credit)
    @FXML private VBox paymentBreakdownContainer;
    @FXML private ProgressBar cashPaymentProgress;
    @FXML private ProgressBar bankPaymentProgress;
    @FXML private ProgressBar creditPaymentProgress;
    @FXML private Label lblCashPayment;
    @FXML private Label lblBankPayment;
    @FXML private Label lblCreditPayment;

    // NEW: Monthly Target
    @FXML private ProgressBar monthlyTargetProgress;
    @FXML private Label lblTargetPercentage;
    @FXML private Label lblCurrentSales;
    @FXML private Label lblTargetAmount;
    @FXML private Label lblDaysRemaining;

    // Timer for real-time updates
    private Timer clockTimer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing DashboardController");

        setupGreetingAndDateTime();
        setupEventHandlers();
        loadDashboardData();
    }

    /**
     * Setup greeting message and real-time clock
     */
    private void setupGreetingAndDateTime() {
        // Set initial greeting
        updateGreeting();

        // Set initial date/time
        updateDateTime();

        // Start clock timer for real-time updates
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    updateDateTime();
                    // Update greeting every minute (in case time crosses morning/afternoon/evening)
                    updateGreeting();
                });
            }
        }, 1000, 60000); // Update every minute
    }

    private void updateGreeting() {
        if (lblGreeting == null) return;

        int hour = LocalTime.now().getHour();
        String greeting;
        String userName = SessionService.getCurrentUsername();
        String displayName = userName != null ? userName : "User";

        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning, " + displayName + "!";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon, " + displayName + "!";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening, " + displayName + "!";
        } else {
            greeting = "Good Night, " + displayName + "!";
        }

        lblGreeting.setText(greeting);
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();

        if (lblCurrentDate != null) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            lblCurrentDate.setText(now.format(dateFormatter));
        }

        if (lblCurrentTime != null) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
            lblCurrentTime.setText(now.format(timeFormatter));
        }
    }

    private void setupEventHandlers() {
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> loadDashboardData());
        }

        if (btnNewOrder != null) {
            btnNewOrder.setOnAction(e -> openBillingWindow());
        }

        if (btnReceivePayment != null) {
            btnReceivePayment.setOnAction(e -> navigateToReceivePayment());
        }

        if (btnNewPurchase != null) {
            btnNewPurchase.setOnAction(e -> navigateToPurchase());
        }

        if (btnViewReports != null) {
            btnViewReports.setOnAction(e -> navigateToReports());
        }

        if (btnNotification != null) {
            btnNotification.setOnAction(e -> showNotifications());
        }
    }

    private void showNotifications() {
        // TODO: Show notification popup or navigate to notifications screen
        LOG.info("Notification button clicked");
        alertNotification.showInfo("Notifications feature coming soon!");
    }

    /**
     * Load all dashboard data asynchronously
     */
    private void loadDashboardData() {
        LOG.info("Loading dashboard data...");

        // Show loading state
        setLoadingState(true);

        // Load data in background thread
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> data = dashboardService.getAllDashboardData();

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    updateDashboard(data);
                    setLoadingState(false);
                    LOG.info("Dashboard data loaded successfully");
                });

            } catch (Exception e) {
                LOG.error("Error loading dashboard data: ", e);
                Platform.runLater(() -> {
                    setLoadingState(false);
                    setErrorState();
                });
            }
        });
    }

    /**
     * Update all dashboard components with loaded data
     */
    @SuppressWarnings("unchecked")
    private void updateDashboard(Map<String, Object> data) {
        // Primary KPIs
        updateLabel(lblTodaysSales, formatCurrency((Float) data.get("todaysSales")));
        updateLabel(lblTodaysOrders, formatNumber((Long) data.get("todaysOrders")));
        updateLabel(lblTodaysPurchase, formatCurrency((Float) data.get("todaysPurchase")));
        updateLabel(lblPendingCredit, formatCurrency((Float) data.get("pendingCredit")));
        updateLabel(lblPendingCreditCount, formatNumber((Long) data.get("pendingCreditCount")) + " pending");

        // Financial Summary
        Float grossProfit = (Float) data.get("grossProfit");
        updateLabel(lblGrossProfit, formatCurrency(grossProfit));
        if (lblGrossProfit != null && grossProfit != null) {
            lblGrossProfit.getStyleClass().removeAll("profit-positive", "profit-negative");
            lblGrossProfit.getStyleClass().add(grossProfit >= 0 ? "profit-positive" : "profit-negative");
        }
        updateLabel(lblCashInHand, formatCurrency((Float) data.get("cashInHand")));
        updateLabel(lblPaymentsDue, formatCurrency((Float) data.get("paymentsDue")));
        updateLabel(lblAvgOrderValue, formatCurrency((Float) data.get("avgOrderValue")));

        // Charts
        updateSalesTrendChart((Map<String, Float>) data.get("last7DaysSales"));
        updateSalesVsPurchaseChart(
                (Map<String, Float>) data.get("last7DaysSales"),
                (Map<String, Float>) data.get("last7DaysPurchase")
        );

        // Table Status
        Map<String, Long> tableStatus = (Map<String, Long>) data.get("tableStatus");
        updateLabel(lblTotalTables, formatNumber(tableStatus.get("total")));
        updateLabel(lblActiveTables, formatNumber(tableStatus.get("active")));
        updateLabel(lblAvailableTables, formatNumber(tableStatus.get("available")));

        // Order Status
        updateOrderStatus((Map<String, Long>) data.get("orderStatus"));

        // Top Selling Items
        updateTopSellingItems((List<Map<String, Object>>) data.get("topSellingItems"));

        // Recent Transactions
        updateRecentTransactions((List<Map<String, Object>>) data.get("recentTransactions"));

        // Footer Stats
        Map<String, Long> footerStats = (Map<String, Long>) data.get("footerStats");
        updateLabel(lblTotalCustomers, formatNumber(footerStats.get("customers")));
        updateLabel(lblTotalItems, formatNumber(footerStats.get("items")));
        updateLabel(lblTotalEmployees, formatNumber(footerStats.get("employees")));
        updateLabel(lblTotalTablesFooter, formatNumber(footerStats.get("tables")));

        // NEW: Trend Indicators
        updateTrendIndicators(data);

        // NEW: Low Stock Alerts
        updateLowStockAlerts(data);

        // NEW: Payment Breakdown
        updatePaymentBreakdown(data);

        // NEW: Monthly Target
        updateMonthlyTarget(data);

        // NEW: Notification Badge
        updateNotificationBadge(data);

        // System Status
        setSystemOnline();
    }

    /**
     * Update sales trend line chart
     */
    private void updateSalesTrendChart(Map<String, Float> salesData) {
        if (salesTrendChart == null || salesData == null) return;

        salesTrendChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");

        for (Map.Entry<String, Float> entry : salesData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        salesTrendChart.getData().add(series);
    }

    /**
     * Update sales vs purchase bar chart
     */
    private void updateSalesVsPurchaseChart(Map<String, Float> salesData, Map<String, Float> purchaseData) {
        if (salesVsPurchaseChart == null) return;

        salesVsPurchaseChart.getData().clear();

        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        salesSeries.setName("Sales");

        XYChart.Series<String, Number> purchaseSeries = new XYChart.Series<>();
        purchaseSeries.setName("Purchase");

        if (salesData != null) {
            for (Map.Entry<String, Float> entry : salesData.entrySet()) {
                salesSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }

        if (purchaseData != null) {
            for (Map.Entry<String, Float> entry : purchaseData.entrySet()) {
                purchaseSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }

        salesVsPurchaseChart.getData().addAll(salesSeries, purchaseSeries);
    }

    /**
     * Update order status with progress bars
     */
    private void updateOrderStatus(Map<String, Long> orderStatus) {
        if (orderStatus == null) return;

        Long paid = orderStatus.getOrDefault("PAID", 0L);
        Long credit = orderStatus.getOrDefault("CREDIT", 0L);
        Long pending = orderStatus.getOrDefault("PENDING", 0L);

        long total = paid + credit + pending;

        updateLabel(lblPaidCount, formatNumber(paid));
        updateLabel(lblCreditCount, formatNumber(credit));
        updateLabel(lblPendingCount, formatNumber(pending));

        if (total > 0) {
            if (paidProgress != null) paidProgress.setProgress((double) paid / total);
            if (creditProgress != null) creditProgress.setProgress((double) credit / total);
            if (pendingProgress != null) pendingProgress.setProgress((double) pending / total);
        } else {
            if (paidProgress != null) paidProgress.setProgress(0);
            if (creditProgress != null) creditProgress.setProgress(0);
            if (pendingProgress != null) pendingProgress.setProgress(0);
        }
    }

    /**
     * Update top selling items list
     */
    private void updateTopSellingItems(List<Map<String, Object>> topItems) {
        if (topItemsContainer == null) return;

        topItemsContainer.getChildren().clear();

        if (topItems == null || topItems.isEmpty()) {
            Label emptyLabel = new Label("No sales data for today");
            emptyLabel.getStyleClass().add("empty-list-label");
            topItemsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Map<String, Object> item : topItems) {
            HBox itemRow = createTopItemRow(
                    (Integer) item.get("rank"),
                    (String) item.get("name"),
                    (Integer) item.get("quantity")
            );
            topItemsContainer.getChildren().add(itemRow);
        }
    }

    /**
     * Create a row for top selling item
     */
    private HBox createTopItemRow(int rank, String name, int quantity) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("top-item-row");
        row.setPadding(new Insets(8, 12, 8, 12));

        // Rank badge
        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("rank-badge");
        if (rank == 1) rankLabel.getStyleClass().add("rank-gold");
        else if (rank == 2) rankLabel.getStyleClass().add("rank-silver");
        else if (rank == 3) rankLabel.getStyleClass().add("rank-bronze");

        // Item name with custom font (16px)
        Label nameLabel = new Label(name != null ? name : "Unknown Item");
        nameLabel.getStyleClass().add("item-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Apply custom font with 16px if available (use inline style to override CSS)
        if (SessionService.isCustomFontLoaded()) {
            String fontFamily = SessionService.getCustomFontFamily();
            if (fontFamily != null) {
                nameLabel.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 16px;");
            }
        }

        // Quantity
        Label qtyLabel = new Label(quantity + " sold");
        qtyLabel.getStyleClass().add("item-quantity");

        row.getChildren().addAll(rankLabel, nameLabel, qtyLabel);
        return row;
    }

    /**
     * Update recent transactions list
     */
    private void updateRecentTransactions(List<Map<String, Object>> transactions) {
        if (recentTransactionsContainer == null) return;

        recentTransactionsContainer.getChildren().clear();

        if (transactions == null || transactions.isEmpty()) {
            Label emptyLabel = new Label("No recent transactions");
            emptyLabel.getStyleClass().add("empty-list-label");
            recentTransactionsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Map<String, Object> tx : transactions) {
            HBox txRow = createTransactionRow(tx);
            recentTransactionsContainer.getChildren().add(txRow);
        }
    }

    /**
     * Create a row for recent transaction
     */
    private HBox createTransactionRow(Map<String, Object> tx) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("transaction-row");
        row.setPadding(new Insets(10, 12, 10, 12));

        // Bill icon using FontAwesomeIcon
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName("FILE_TEXT_ALT");
        icon.setSize("1.3em");
        icon.setFill(Color.web("#1976D2"));

        // Bill info
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label billNoLabel = new Label("Bill #" + tx.get("billNo"));
        billNoLabel.getStyleClass().add("tx-bill-no");

        // Customer name with custom font (19px) if customer exists, else English "Walk-in"
        String customerName = (String) tx.get("customer");
        Label customerLabel = new Label(customerName != null ? customerName : "Walk-in");
        customerLabel.getStyleClass().add("tx-customer");

        // Apply custom font with 19px only if customer name is not "Walk-in" and custom font is loaded
        // Use inline style to override CSS
        if (customerName != null && !customerName.equalsIgnoreCase("Walk-in") && SessionService.isCustomFontLoaded()) {
            String fontFamily = SessionService.getCustomFontFamily();
            if (fontFamily != null) {
                customerLabel.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: 19px;");
            }
        }

        infoBox.getChildren().addAll(billNoLabel, customerLabel);

        // Amount and status
        VBox amountBox = new VBox(2);
        amountBox.setAlignment(Pos.CENTER_RIGHT);

        Object amountObj = tx.get("amount");
        Float amount = amountObj instanceof Float ? (Float) amountObj : 0f;
        Label amountLabel = new Label(formatCurrency(amount));
        amountLabel.getStyleClass().add("tx-amount");

        String status = (String) tx.get("status");
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().addAll("tx-status", "status-" + status.toLowerCase());

        amountBox.getChildren().addAll(amountLabel, statusLabel);

        // Time
        Label timeLabel = new Label((String) tx.get("time"));
        timeLabel.getStyleClass().add("tx-time");

        row.getChildren().addAll(icon, infoBox, amountBox, timeLabel);
        return row;
    }

    // ==================== NEW: Trend Indicators ====================

    /**
     * Update trend indicators for KPI cards
     */
    private void updateTrendIndicators(Map<String, Object> data) {
        // Sales trend
        Float todaySales = (Float) data.get("todaysSales");
        Float yesterdaySales = (Float) data.get("yesterdaySales");
        updateTrendIndicator(salesTrendBox, salesTrendIcon, lblSalesTrend, todaySales, yesterdaySales);

        // Orders trend
        Long todayOrders = (Long) data.get("todaysOrders");
        Long yesterdayOrders = (Long) data.get("yesterdayOrders");
        updateTrendIndicator(ordersTrendBox, ordersTrendIcon, lblOrdersTrend,
                todayOrders != null ? todayOrders.floatValue() : 0f,
                yesterdayOrders != null ? yesterdayOrders.floatValue() : 0f);

        // Purchase trend (down is good for purchase)
        Float todayPurchase = (Float) data.get("todaysPurchase");
        Float yesterdayPurchase = (Float) data.get("yesterdayPurchase");
        updateTrendIndicator(purchaseTrendBox, purchaseTrendIcon, lblPurchaseTrend, todayPurchase, yesterdayPurchase);
    }

    private void updateTrendIndicator(HBox trendBox, FontAwesomeIcon icon, Label label, Float current, Float previous) {
        if (trendBox == null || icon == null || label == null) return;

        float currentVal = current != null ? current : 0f;
        float previousVal = previous != null ? previous : 0f;

        if (previousVal == 0) {
            // No previous data - show neutral
            trendBox.getStyleClass().removeAll("trend-up", "trend-down", "trend-neutral");
            trendBox.getStyleClass().add("trend-neutral");
            icon.setGlyphName("MINUS");
            icon.setFill(Color.web("#9E9E9E"));
            label.setText("N/A");
            return;
        }

        float percentChange = ((currentVal - previousVal) / previousVal) * 100;
        String percentText = String.format("%+.0f%%", percentChange);

        trendBox.getStyleClass().removeAll("trend-up", "trend-down", "trend-neutral");

        if (percentChange > 0) {
            trendBox.getStyleClass().add("trend-up");
            icon.setGlyphName("ARROW_UP");
            icon.setFill(Color.web("#4CAF50"));
        } else if (percentChange < 0) {
            trendBox.getStyleClass().add("trend-down");
            icon.setGlyphName("ARROW_DOWN");
            icon.setFill(Color.web("#F44336"));
        } else {
            trendBox.getStyleClass().add("trend-neutral");
            icon.setGlyphName("MINUS");
            icon.setFill(Color.web("#9E9E9E"));
        }

        label.setText(percentText);
    }

    // ==================== NEW: Low Stock Alerts ====================

    /**
     * Update low stock alerts widget
     */
    @SuppressWarnings("unchecked")
    private void updateLowStockAlerts(Map<String, Object> data) {
        if (lowStockContainer == null) return;

        lowStockContainer.getChildren().clear();

        List<Map<String, Object>> lowStockItems = (List<Map<String, Object>>) data.get("lowStockItems");

        if (lowStockItems == null || lowStockItems.isEmpty()) {
            if (lblLowStockCount != null) {
                lblLowStockCount.setText("0");
                lblLowStockCount.setVisible(false);
            }
            Label emptyLabel = new Label("All items are well stocked!");
            emptyLabel.getStyleClass().add("empty-list-label");
            emptyLabel.setStyle("-fx-text-fill: #4CAF50;");
            lowStockContainer.getChildren().add(emptyLabel);
            return;
        }

        if (lblLowStockCount != null) {
            lblLowStockCount.setText(String.valueOf(lowStockItems.size()));
            lblLowStockCount.setVisible(true);
        }

        int displayCount = Math.min(lowStockItems.size(), 5); // Show max 5 items
        for (int i = 0; i < displayCount; i++) {
            Map<String, Object> item = lowStockItems.get(i);
            HBox alertRow = createLowStockRow(item);
            lowStockContainer.getChildren().add(alertRow);
        }

        if (lowStockItems.size() > 5) {
            Label moreLabel = new Label("+" + (lowStockItems.size() - 5) + " more items...");
            moreLabel.getStyleClass().add("empty-list-label");
            lowStockContainer.getChildren().add(moreLabel);
        }
    }

    private HBox createLowStockRow(Map<String, Object> item) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("alert-item");
        row.setPadding(new Insets(8, 12, 8, 12));

        // Warning icon
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName("WARNING");
        icon.setSize("1em");

        // Item name
        Label nameLabel = new Label((String) item.get("name"));
        nameLabel.getStyleClass().add("alert-item-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Stock quantity
        Object stockObj = item.get("stock");
        int stock = stockObj instanceof Integer ? (Integer) stockObj : 0;
        Label stockLabel = new Label(stock + " left");
        stockLabel.getStyleClass().add("alert-item-stock");

        // Set color based on severity
        if (stock <= 5) {
            icon.setFill(Color.web("#D32F2F"));
            stockLabel.getStyleClass().add("stock-critical");
        } else {
            icon.setFill(Color.web("#FF9800"));
            stockLabel.getStyleClass().add("stock-low");
        }

        row.getChildren().addAll(icon, nameLabel, stockLabel);
        return row;
    }

    // ==================== NEW: Payment Breakdown ====================

    /**
     * Update payment method breakdown widget (Cash, Bank, Credit)
     */
    @SuppressWarnings("unchecked")
    private void updatePaymentBreakdown(Map<String, Object> data) {
        Map<String, Float> paymentMethods = (Map<String, Float>) data.get("paymentMethods");

        if (paymentMethods == null) {
            // Set default values if no data
            updateLabel(lblCashPayment, "₹0");
            updateLabel(lblBankPayment, "₹0");
            updateLabel(lblCreditPayment, "₹0");
            if (cashPaymentProgress != null) cashPaymentProgress.setProgress(0);
            if (bankPaymentProgress != null) bankPaymentProgress.setProgress(0);
            if (creditPaymentProgress != null) creditPaymentProgress.setProgress(0);
            return;
        }

        Float cashAmount = paymentMethods.getOrDefault("CASH", 0f);
        Float bankAmount = paymentMethods.getOrDefault("BANK", 0f);
        Float creditAmount = paymentMethods.getOrDefault("CREDIT", 0f);

        float totalPayments = cashAmount + bankAmount + creditAmount;

        // Update labels
        updateLabel(lblCashPayment, formatCurrency(cashAmount));
        updateLabel(lblBankPayment, formatCurrency(bankAmount));
        updateLabel(lblCreditPayment, formatCurrency(creditAmount));

        // Update progress bars
        if (totalPayments > 0) {
            if (cashPaymentProgress != null) cashPaymentProgress.setProgress(cashAmount / totalPayments);
            if (bankPaymentProgress != null) bankPaymentProgress.setProgress(bankAmount / totalPayments);
            if (creditPaymentProgress != null) creditPaymentProgress.setProgress(creditAmount / totalPayments);
        } else {
            if (cashPaymentProgress != null) cashPaymentProgress.setProgress(0);
            if (bankPaymentProgress != null) bankPaymentProgress.setProgress(0);
            if (creditPaymentProgress != null) creditPaymentProgress.setProgress(0);
        }
    }

    // ==================== NEW: Monthly Target ====================

    /**
     * Update monthly sales target widget
     */
    @SuppressWarnings("unchecked")
    private void updateMonthlyTarget(Map<String, Object> data) {
        Map<String, Object> targetData = (Map<String, Object>) data.get("monthlyTarget");

        if (targetData == null) {
            // Set default values if no target data
            setDefaultTargetValues();
            return;
        }

        Float currentSales = (Float) targetData.get("currentSales");
        Float targetAmount = (Float) targetData.get("targetAmount");
        Integer daysRemaining = (Integer) targetData.get("daysRemaining");

        if (currentSales == null) currentSales = 0f;
        if (targetAmount == null || targetAmount == 0) {
            setDefaultTargetValues();
            return;
        }

        float percentage = (currentSales / targetAmount) * 100;

        // Update labels
        updateLabel(lblCurrentSales, formatCurrency(currentSales));
        updateLabel(lblTargetAmount, formatCurrency(targetAmount));

        if (lblTargetPercentage != null) {
            lblTargetPercentage.setText(String.format("%.0f%%", percentage));
            // Change color based on progress
            if (percentage >= 100) {
                lblTargetPercentage.setStyle("-fx-text-fill: #4CAF50;"); // Green - target achieved
            } else if (percentage >= 70) {
                lblTargetPercentage.setStyle("-fx-text-fill: #8BC34A;"); // Light green - on track
            } else if (percentage >= 40) {
                lblTargetPercentage.setStyle("-fx-text-fill: #FF9800;"); // Orange - needs attention
            } else {
                lblTargetPercentage.setStyle("-fx-text-fill: #F44336;"); // Red - behind
            }
        }

        if (monthlyTargetProgress != null) {
            monthlyTargetProgress.setProgress(Math.min(percentage / 100, 1.0));
        }

        if (lblDaysRemaining != null && daysRemaining != null) {
            lblDaysRemaining.setText(daysRemaining + " days remaining");
        }
    }

    private void setDefaultTargetValues() {
        // Calculate days remaining in current month
        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        long daysRemaining = ChronoUnit.DAYS.between(today, endOfMonth);

        updateLabel(lblCurrentSales, "₹0");
        updateLabel(lblTargetAmount, "₹1,00,000");
        if (lblTargetPercentage != null) lblTargetPercentage.setText("0%");
        if (monthlyTargetProgress != null) monthlyTargetProgress.setProgress(0);
        if (lblDaysRemaining != null) lblDaysRemaining.setText(daysRemaining + " days remaining");
    }

    // ==================== NEW: Notification Badge ====================

    /**
     * Update notification badge count
     */
    private void updateNotificationBadge(Map<String, Object> data) {
        if (lblNotificationBadge == null) return;

        Integer notificationCount = (Integer) data.get("notificationCount");
        if (notificationCount == null || notificationCount == 0) {
            lblNotificationBadge.setVisible(false);
        } else {
            lblNotificationBadge.setVisible(true);
            lblNotificationBadge.setText(notificationCount > 99 ? "99+" : String.valueOf(notificationCount));
        }
    }

    // ==================== Navigation Methods ====================

    private void openBillingWindow() {
        if (!navigationGuard.canAccess(ScreenPermission.BILLING)) {
            alertNotification.showError("Access Denied: You don't have permission to access Billing");
            return;
        }

        try {
            Parent billingRoot = (Parent) loader.load("/fxml/transaction/BillingFrame.fxml");
            Stage billingStage = new Stage();
            billingStage.setTitle("Billing - " + SessionService.getCurrentRestaurantName());
            Scene billingScene = new Scene(billingRoot);
            billingStage.setScene(billingScene);

            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            billingStage.setX(bounds.getMinX());
            billingStage.setY(bounds.getMinY());
            billingStage.setWidth(bounds.getWidth());
            billingStage.setHeight(bounds.getHeight());
            billingStage.setMaximized(true);
            billingStage.show();

            LOG.info("Billing window opened from dashboard");
        } catch (Exception e) {
            LOG.error("Error opening billing window: ", e);
            alertNotification.showError("Error opening billing: " + e.getMessage());
        }
    }

    private void navigateToReceivePayment() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/transaction/ReceivePaymentFrame.fxml");
        }
    }

    private void navigateToPurchase() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            navigationGuard.navigateWithPermissionCheck(mainPane, "/fxml/transaction/PurchaseBillFrame.fxml");
        }
    }

    private void navigateToReports() {
        BorderPane mainPane = getMainPane();
        if (mainPane != null) {
            try {
                Pane pane = loader.getPage("/fxml/report/ReportMenu.fxml");
                mainPane.setCenter(pane);
            } catch (Exception e) {
                LOG.error("Error navigating to reports: ", e);
            }
        }
    }

    private BorderPane getMainPane() {
        try {
            if (btnRefresh != null && btnRefresh.getScene() != null) {
                return (BorderPane) btnRefresh.getScene().lookup("#mainPane");
            }
        } catch (Exception e) {
            LOG.warn("Could not find main pane");
        }
        return null;
    }

    // ==================== Helper Methods ====================

    private void updateLabel(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private String formatCurrency(Float value) {
        if (value == null) return "₹0";
        return "₹" + CURRENCY_FORMAT.format(value);
    }

    private String formatNumber(Long value) {
        if (value == null) return "0";
        return NUMBER_FORMAT.format(value);
    }

    private void setLoadingState(boolean loading) {
        if (btnRefresh != null) {
            btnRefresh.setDisable(loading);
        }
    }

    private void setSystemOnline() {
        if (statusIcon != null) {
            statusIcon.setFill(Color.web("#4CAF50"));
            statusIcon.setGlyphName("CHECK_CIRCLE");
        }
        if (lblSystemStatus != null) {
            lblSystemStatus.setText("System Online");
            lblSystemStatus.getStyleClass().removeAll("status-offline");
            lblSystemStatus.getStyleClass().add("status-online");
        }
    }

    private void setErrorState() {
        if (statusIcon != null) {
            statusIcon.setFill(Color.web("#F44336"));
            statusIcon.setGlyphName("EXCLAMATION_CIRCLE");
        }
        if (lblSystemStatus != null) {
            lblSystemStatus.setText("Error Loading Data");
            lblSystemStatus.getStyleClass().removeAll("status-online");
            lblSystemStatus.getStyleClass().add("status-offline");
        }
    }
}
