/**
 * Hotel Management - Comprehensive Dashboard Page Logic
 * Exact replica of desktop DashboardController.java behavior.
 * Uses DashboardService.getAllDashboardData() via /api/v1/web/dashboard/full
 */

// Chart instances (so we can destroy/recreate)
let salesTrendChartInstance = null;
let salesVsPurchaseChartInstance = null;

// Clock timer
let clockTimerId = null;

document.addEventListener('DOMContentLoaded', function () {
    if (!requireAuth()) return;

    initializeHeader();
    setupGreetingAndDateTime();
    setupEventHandlers();
    loadFullDashboard();
});

/**
 * Initialize header with session data
 * (same as desktop HomeController: lblShopeeName, lblSidebarShopName, txtUserName, txtDesignation)
 */
function initializeHeader() {
    const user = getUser();
    if (!user) return;

    const restaurantName = user.restaurantName || 'Hotel Management System';
    document.getElementById('lblShopeeName').textContent = restaurantName;
    document.getElementById('lblSidebarShopName').textContent = restaurantName;
    document.getElementById('txtUserName').textContent = user.employeeName || user.username || 'Guest User';
    document.getElementById('txtDesignation').textContent = user.role || '';
}

/**
 * Setup greeting and real-time clock
 * (like desktop DashboardController.setupGreetingAndDateTime)
 */
function setupGreetingAndDateTime() {
    updateGreeting();
    updateDateTime();

    // Update every minute (like desktop: timer 1000ms initial, 60000ms period)
    clockTimerId = setInterval(function () {
        updateDateTime();
        updateGreeting();
    }, 60000);
}

/**
 * Update greeting based on time of day
 * (like desktop DashboardController.updateGreeting)
 */
function updateGreeting() {
    const hour = new Date().getHours();
    const user = getUser();
    const displayName = user ? (user.employeeName || user.username || 'User') : 'User';

    let greeting;
    if (hour >= 5 && hour < 12) {
        greeting = 'Good Morning, ' + displayName + '!';
    } else if (hour >= 12 && hour < 17) {
        greeting = 'Good Afternoon, ' + displayName + '!';
    } else if (hour >= 17 && hour < 21) {
        greeting = 'Good Evening, ' + displayName + '!';
    } else {
        greeting = 'Good Night, ' + displayName + '!';
    }

    const el = document.getElementById('lblGreeting');
    if (el) el.textContent = greeting;
}

/**
 * Update date and time display
 * (like desktop DashboardController.updateDateTime)
 */
function updateDateTime() {
    const now = new Date();

    const dateEl = document.getElementById('lblCurrentDate');
    if (dateEl) {
        dateEl.textContent = now.toLocaleDateString('en-US', {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
        });
    }

    const timeEl = document.getElementById('lblCurrentTime');
    if (timeEl) {
        timeEl.textContent = now.toLocaleTimeString('en-US', {
            hour: '2-digit', minute: '2-digit', hour12: true
        });
    }
}

/**
 * Setup event handlers for quick action buttons
 * (like desktop DashboardController.setupEventHandlers)
 */
function setupEventHandlers() {
    const btnNewOrder = document.getElementById('btnNewOrder');
    if (btnNewOrder) btnNewOrder.addEventListener('click', function () {
        window.location.href = '/pages/transaction/billing.html';
    });

    const btnReceivePayment = document.getElementById('btnReceivePayment');
    if (btnReceivePayment) btnReceivePayment.addEventListener('click', function () {
        window.location.href = '/pages/transaction/receive-payment.html';
    });

    const btnNewPurchase = document.getElementById('btnNewPurchase');
    if (btnNewPurchase) btnNewPurchase.addEventListener('click', function () {
        window.location.href = '/pages/transaction/purchase-bill.html';
    });

    const btnViewReports = document.getElementById('btnViewReports');
    if (btnViewReports) btnViewReports.addEventListener('click', function () {
        window.location.href = '/pages/report/sales.html';
    });

    const btnNotification = document.getElementById('btnNotification');
    if (btnNotification) btnNotification.addEventListener('click', function () {
        showSuccess('Notifications feature coming soon!');
    });
}

/**
 * Load full dashboard data from server
 * (like desktop DashboardController.loadDashboardData using getAllDashboardData)
 */
async function loadFullDashboard() {
    setLoadingState(true);

    try {
        const result = await apiGet('/web/dashboard/full');

        if (result && result.success) {
            updateDashboard(result.data);
            setSystemOnline();
        } else {
            setErrorState();
        }
    } catch (error) {
        console.error('Dashboard load error:', error);
        setErrorState();
    } finally {
        setLoadingState(false);
    }
}

/**
 * Update all dashboard components with loaded data
 * (like desktop DashboardController.updateDashboard)
 */
function updateDashboard(data) {
    // Primary KPIs
    setText('lblTodaysSales', formatINR(data.todaysSales));
    setText('lblTodaysOrders', formatNum(data.todaysOrders));
    setText('lblTodaysPurchase', formatINR(data.todaysPurchase));
    setText('lblPendingCredit', formatINR(data.pendingCredit));
    setText('lblPendingCreditCount', formatNum(data.pendingCreditCount) + ' pending');

    // Financial Summary
    setText('lblGrossProfit', formatINR(data.grossProfit));
    const gpEl = document.getElementById('lblGrossProfit');
    if (gpEl) {
        gpEl.classList.remove('profit-positive', 'profit-negative');
        gpEl.classList.add((data.grossProfit || 0) >= 0 ? 'profit-positive' : 'profit-negative');
    }
    setText('lblCashInHand', formatINR(data.cashInHand));
    setText('lblPaymentsDue', formatINR(data.paymentsDue));
    setText('lblAvgOrderValue', formatINR(data.avgOrderValue));

    // Trend Indicators
    updateTrendIndicators(data);

    // Charts
    updateSalesTrendChart(data.last7DaysSales);
    updateSalesVsPurchaseChart(data.last7DaysSales, data.last7DaysPurchase);

    // Low Stock Alerts
    updateLowStockAlerts(data.lowStockItems);

    // Payment Breakdown
    updatePaymentBreakdown(data.paymentMethods);

    // Monthly Target
    updateMonthlyTarget(data.monthlyTarget);

    // Table Status
    if (data.tableStatus) {
        setText('lblDashTotalTables', formatNum(data.tableStatus.total));
        setText('lblDashActiveTables', formatNum(data.tableStatus.active));
        setText('lblDashAvailableTables', formatNum(data.tableStatus.available));
    }

    // Order Status
    updateOrderStatus(data.orderStatus);

    // Top Selling Items
    updateTopSellingItems(data.topSellingItems);

    // Recent Transactions
    updateRecentTransactions(data.recentTransactions);

    // Footer Stats
    if (data.footerStats) {
        setText('lblTotalCustomers', formatNum(data.footerStats.customers));
        setText('lblTotalItems', formatNum(data.footerStats.items));
        setText('lblTotalEmployees', formatNum(data.footerStats.employees));
        setText('lblTotalTablesFooter', formatNum(data.footerStats.tables));
    }

    // Notification Badge
    updateNotificationBadge(data.notificationCount);
}

// ==================== Trend Indicators ====================

/**
 * Update trend indicators for KPI cards
 * (like desktop DashboardController.updateTrendIndicators)
 */
function updateTrendIndicators(data) {
    updateTrendIndicator('sales', data.todaysSales, data.yesterdaySales);
    updateTrendIndicator('orders',
        data.todaysOrders != null ? Number(data.todaysOrders) : 0,
        data.yesterdayOrders != null ? Number(data.yesterdayOrders) : 0
    );
    updateTrendIndicator('purchase', data.todaysPurchase, data.yesterdayPurchase);
}

function updateTrendIndicator(prefix, current, previous) {
    const trendBox = document.getElementById(prefix + 'TrendBox');
    const trendIcon = document.getElementById(prefix + 'TrendIcon');
    const trendLabel = document.getElementById('lbl' + capitalize(prefix) + 'Trend');

    if (!trendBox || !trendIcon || !trendLabel) return;

    const currentVal = current || 0;
    const previousVal = previous || 0;

    trendBox.classList.remove('trend-up', 'trend-down', 'trend-neutral');

    if (previousVal === 0) {
        trendBox.classList.add('trend-neutral');
        trendIcon.className = 'bi bi-dash';
        trendLabel.textContent = 'N/A';
        return;
    }

    const percentChange = ((currentVal - previousVal) / previousVal) * 100;
    const percentText = (percentChange > 0 ? '+' : '') + Math.round(percentChange) + '%';

    if (percentChange > 0) {
        trendBox.classList.add('trend-up');
        trendIcon.className = 'bi bi-arrow-up';
    } else if (percentChange < 0) {
        trendBox.classList.add('trend-down');
        trendIcon.className = 'bi bi-arrow-down';
    } else {
        trendBox.classList.add('trend-neutral');
        trendIcon.className = 'bi bi-dash';
    }

    trendLabel.textContent = percentText;
}

// ==================== Charts ====================

/**
 * Update sales trend line chart
 * (like desktop DashboardController.updateSalesTrendChart)
 */
function updateSalesTrendChart(salesData) {
    const canvas = document.getElementById('salesTrendChart');
    if (!canvas || !salesData) return;

    if (salesTrendChartInstance) {
        salesTrendChartInstance.destroy();
    }

    const labels = Object.keys(salesData);
    const values = Object.values(salesData);

    salesTrendChartInstance = new Chart(canvas, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Sales',
                data: values,
                borderColor: '#1976D2',
                backgroundColor: 'rgba(25, 118, 210, 0.1)',
                fill: true,
                tension: 0.4,
                borderWidth: 2,
                pointBackgroundColor: '#1976D2',
                pointRadius: 4,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function (ctx) {
                            return formatINR(ctx.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function (val) {
                            return formatINR(val);
                        }
                    }
                }
            }
        }
    });
}

/**
 * Update sales vs purchase area chart
 * (like desktop DashboardController.updateSalesVsPurchaseChart)
 */
function updateSalesVsPurchaseChart(salesData, purchaseData) {
    const canvas = document.getElementById('salesVsPurchaseChart');
    if (!canvas) return;

    if (salesVsPurchaseChartInstance) {
        salesVsPurchaseChartInstance.destroy();
    }

    const salesLabels = salesData ? Object.keys(salesData) : [];
    const purchaseLabels = purchaseData ? Object.keys(purchaseData) : [];
    const labels = salesLabels.length >= purchaseLabels.length ? salesLabels : purchaseLabels;

    salesVsPurchaseChartInstance = new Chart(canvas, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Sales',
                    data: salesData ? Object.values(salesData) : [],
                    borderColor: '#4CAF50',
                    backgroundColor: 'rgba(76, 175, 80, 0.15)',
                    fill: true,
                    tension: 0.4,
                    borderWidth: 2,
                    pointRadius: 3
                },
                {
                    label: 'Purchase',
                    data: purchaseData ? Object.values(purchaseData) : [],
                    borderColor: '#FF9800',
                    backgroundColor: 'rgba(255, 152, 0, 0.15)',
                    fill: true,
                    tension: 0.4,
                    borderWidth: 2,
                    pointRadius: 3
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function (ctx) {
                            return ctx.dataset.label + ': ' + formatINR(ctx.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function (val) {
                            return formatINR(val);
                        }
                    }
                }
            }
        }
    });
}

// ==================== Low Stock Alerts ====================

/**
 * Update low stock alerts widget
 * (like desktop DashboardController.updateLowStockAlerts)
 */
function updateLowStockAlerts(lowStockItems) {
    const container = document.getElementById('lowStockContainer');
    if (!container) return;

    container.innerHTML = '';

    if (!lowStockItems || lowStockItems.length === 0) {
        const badge = document.getElementById('lblLowStockCount');
        if (badge) { badge.textContent = '0'; badge.style.display = 'none'; }
        container.innerHTML = '<p class="empty-list-label" style="color:#4CAF50;">All items are well stocked!</p>';
        return;
    }

    const badge = document.getElementById('lblLowStockCount');
    if (badge) { badge.textContent = lowStockItems.length; badge.style.display = ''; }

    const displayCount = Math.min(lowStockItems.length, 5);
    for (let i = 0; i < displayCount; i++) {
        const item = lowStockItems[i];
        const stock = item.stock || 0;
        const isCritical = stock <= 5;

        const row = document.createElement('div');
        row.className = 'alert-item';
        row.innerHTML =
            '<i class="bi bi-exclamation-triangle" style="color:' + (isCritical ? '#D32F2F' : '#FF9800') + ';"></i>' +
            '<span class="alert-item-name">' + escapeHtml(item.name || 'Unknown') + '</span>' +
            '<span class="alert-item-stock ' + (isCritical ? 'stock-critical' : 'stock-low') + '">' + stock + ' left</span>';
        container.appendChild(row);
    }

    if (lowStockItems.length > 5) {
        const more = document.createElement('p');
        more.className = 'empty-list-label';
        more.textContent = '+' + (lowStockItems.length - 5) + ' more items...';
        container.appendChild(more);
    }
}

// ==================== Payment Breakdown ====================

/**
 * Update payment method breakdown widget
 * (like desktop DashboardController.updatePaymentBreakdown)
 */
function updatePaymentBreakdown(paymentMethods) {
    if (!paymentMethods) {
        setText('lblCashPayment', formatINR(0));
        setText('lblBankPayment', formatINR(0));
        setText('lblCreditPayment', formatINR(0));
        setProgressWidth('cashPaymentProgress', 0);
        setProgressWidth('bankPaymentProgress', 0);
        setProgressWidth('creditPaymentProgress', 0);
        return;
    }

    const cash = paymentMethods.CASH || 0;
    const bank = paymentMethods.BANK || 0;
    const credit = paymentMethods.CREDIT || 0;
    const total = cash + bank + credit;

    setText('lblCashPayment', formatINR(cash));
    setText('lblBankPayment', formatINR(bank));
    setText('lblCreditPayment', formatINR(credit));

    if (total > 0) {
        setProgressWidth('cashPaymentProgress', (cash / total) * 100);
        setProgressWidth('bankPaymentProgress', (bank / total) * 100);
        setProgressWidth('creditPaymentProgress', (credit / total) * 100);
    } else {
        setProgressWidth('cashPaymentProgress', 0);
        setProgressWidth('bankPaymentProgress', 0);
        setProgressWidth('creditPaymentProgress', 0);
    }
}

// ==================== Monthly Target ====================

/**
 * Update monthly sales target widget
 * (like desktop DashboardController.updateMonthlyTarget)
 */
function updateMonthlyTarget(targetData) {
    if (!targetData) {
        setDefaultTargetValues();
        return;
    }

    const currentSales = targetData.currentSales || 0;
    const targetAmount = targetData.targetAmount || 0;
    const daysRemaining = targetData.daysRemaining;

    if (targetAmount === 0) {
        setDefaultTargetValues();
        return;
    }

    const percentage = (currentSales / targetAmount) * 100;

    setText('lblCurrentSales', formatINR(currentSales));
    setText('lblTargetAmount', formatINR(targetAmount));

    const percEl = document.getElementById('lblTargetPercentage');
    if (percEl) {
        percEl.textContent = Math.round(percentage) + '%';
        if (percentage >= 100) percEl.style.color = '#4CAF50';
        else if (percentage >= 70) percEl.style.color = '#8BC34A';
        else if (percentage >= 40) percEl.style.color = '#FF9800';
        else percEl.style.color = '#F44336';
    }

    // Update SVG ring (circumference = 2 * PI * 50 = 314.16)
    const ring = document.getElementById('targetRingFill');
    if (ring) {
        const circumference = 314.16;
        const offset = circumference - (Math.min(percentage, 100) / 100) * circumference;
        ring.setAttribute('stroke-dashoffset', offset);
    }

    if (daysRemaining != null) {
        setText('lblDaysRemaining', daysRemaining + ' days remaining');
    }
}

function setDefaultTargetValues() {
    // Calculate days remaining in current month
    const today = new Date();
    const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const daysRemaining = Math.ceil((endOfMonth - today) / (1000 * 60 * 60 * 24));

    setText('lblCurrentSales', formatINR(0));
    setText('lblTargetAmount', formatINR(100000));
    setText('lblTargetPercentage', '0%');
    setText('lblDaysRemaining', daysRemaining + ' days remaining');

    const ring = document.getElementById('targetRingFill');
    if (ring) ring.setAttribute('stroke-dashoffset', '314.16');
}

// ==================== Order Status ====================

/**
 * Update order status with progress bars
 * (like desktop DashboardController.updateOrderStatus)
 */
function updateOrderStatus(orderStatus) {
    if (!orderStatus) return;

    const paid = orderStatus.PAID || 0;
    const credit = orderStatus.CREDIT || 0;
    const pending = orderStatus.PENDING || 0;
    const total = paid + credit + pending;

    setText('lblPaidCount', formatNum(paid));
    setText('lblCreditCount', formatNum(credit));
    setText('lblPendingCount', formatNum(pending));

    if (total > 0) {
        setProgressWidth('paidProgress', (paid / total) * 100);
        setProgressWidth('creditOrderProgress', (credit / total) * 100);
        setProgressWidth('pendingProgress', (pending / total) * 100);
    } else {
        setProgressWidth('paidProgress', 0);
        setProgressWidth('creditOrderProgress', 0);
        setProgressWidth('pendingProgress', 0);
    }
}

// ==================== Top Selling Items ====================

/**
 * Update top selling items list
 * (like desktop DashboardController.updateTopSellingItems)
 */
function updateTopSellingItems(topItems) {
    const container = document.getElementById('topItemsContainer');
    if (!container) return;

    container.innerHTML = '';

    if (!topItems || topItems.length === 0) {
        container.innerHTML = '<p class="empty-list-label">No sales data for today</p>';
        return;
    }

    topItems.forEach(function (item) {
        const rank = item.rank || 0;
        let rankClass = '';
        if (rank === 1) rankClass = ' rank-gold';
        else if (rank === 2) rankClass = ' rank-silver';
        else if (rank === 3) rankClass = ' rank-bronze';

        const row = document.createElement('div');
        row.className = 'top-item-row';
        row.innerHTML =
            '<span class="rank-badge' + rankClass + '">' + rank + '</span>' +
            '<span class="item-name" style="font-family:Kiran,sans-serif; font-size:16px;">' + escapeHtml(item.name || 'Unknown Item') + '</span>' +
            '<span class="item-quantity">' + (item.quantity || 0) + ' sold</span>';
        container.appendChild(row);
    });
}

// ==================== Recent Transactions ====================

/**
 * Update recent transactions list
 * (like desktop DashboardController.updateRecentTransactions)
 */
function updateRecentTransactions(transactions) {
    const container = document.getElementById('recentTransactionsContainer');
    if (!container) return;

    container.innerHTML = '';

    if (!transactions || transactions.length === 0) {
        container.innerHTML = '<p class="empty-list-label">No recent transactions</p>';
        return;
    }

    transactions.forEach(function (tx) {
        const status = (tx.status || 'UNKNOWN').toLowerCase();
        const customerName = tx.customer || 'Walk-in';
        const isMarathi = customerName !== 'Walk-in' && customerName !== 'walk-in';

        const row = document.createElement('div');
        row.className = 'transaction-row';
        row.innerHTML =
            '<i class="bi bi-file-earmark-text" style="color:#1976D2; font-size:1.3em;"></i>' +
            '<div class="tx-info">' +
                '<span class="tx-bill-no">Bill #' + escapeHtml(String(tx.billNo || '')) + '</span>' +
                '<span class="tx-customer"' + (isMarathi ? ' style="font-family:Kiran,sans-serif; font-size:19px;"' : '') + '>' + escapeHtml(customerName) + '</span>' +
            '</div>' +
            '<div class="tx-amount-box">' +
                '<span class="tx-amount">' + formatINR(tx.amount) + '</span>' +
                '<span class="tx-status status-' + escapeHtml(status) + '">' + escapeHtml(tx.status || '') + '</span>' +
            '</div>' +
            '<span class="tx-time">' + escapeHtml(tx.time || '') + '</span>';
        container.appendChild(row);
    });
}

// ==================== Notification Badge ====================

function updateNotificationBadge(count) {
    const badge = document.getElementById('lblNotificationBadge');
    if (!badge) return;

    if (!count || count === 0) {
        badge.style.display = 'none';
    } else {
        badge.style.display = '';
        badge.textContent = count > 99 ? '99+' : count;
    }
}

// ==================== Helper Functions ====================

function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

function setProgressWidth(id, percent) {
    const el = document.getElementById(id);
    if (el) el.style.width = Math.max(0, Math.min(100, percent)) + '%';
}

function formatINR(value) {
    if (value == null || isNaN(value)) return '\u20B90';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency', currency: 'INR', minimumFractionDigits: 2
    }).format(value);
}

function formatNum(value) {
    if (value == null || isNaN(value)) return '0';
    return new Intl.NumberFormat('en-IN').format(value);
}

function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

function setLoadingState(loading) {
    const btn = document.getElementById('btnRefresh');
    if (btn) btn.disabled = loading;
}

function setSystemOnline() {
    const icon = document.getElementById('statusIcon');
    if (icon) { icon.className = 'bi bi-check-circle-fill'; icon.style.color = '#4CAF50'; }
    const label = document.getElementById('lblSystemStatus');
    if (label) { label.textContent = 'System Online'; label.className = 'system-status-text status-online'; }
}

function setErrorState() {
    const icon = document.getElementById('statusIcon');
    if (icon) { icon.className = 'bi bi-exclamation-circle-fill'; icon.style.color = '#F44336'; }
    const label = document.getElementById('lblSystemStatus');
    if (label) { label.textContent = 'Error Loading Data'; label.className = 'system-status-text status-offline'; }
}
