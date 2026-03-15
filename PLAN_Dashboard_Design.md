# Dashboard Design Plan - Hotel/Restaurant Management System

## Overview
Comprehensive dashboard redesign for the Hotel2025 management system with real-time KPIs, charts, and operational metrics.

---

## Dashboard Layout Structure

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  DASHBOARD                                            👤 Admin │ 🔄 Refresh  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ TODAY'S     │  │ TODAY'S     │  │ TODAY'S     │  │ PENDING     │         │
│  │ SALES       │  │ ORDERS      │  │ PURCHASE    │  │ CREDIT      │         │
│  │ ₹45,230     │  │ 47          │  │ ₹12,500     │  │ ₹8,750      │         │
│  │ ↑12%        │  │ ↑8%         │  │ ↓5%         │  │ 3 pending   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ GROSS       │  │ CASH IN     │  │ PAYMENT     │  │ AVG ORDER   │         │
│  │ PROFIT      │  │ HAND        │  │ DUE         │  │ VALUE       │         │
│  │ ₹32,730     │  │ ₹1,25,000   │  │ ₹15,200     │  │ ₹962        │         │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                                              │
│  ┌──────────────────────────────┐  ┌──────────────────────────────┐         │
│  │ SALES TREND (7 Days)         │  │ SALES vs PURCHASE            │         │
│  │ [Line Chart]                 │  │ [Bar Chart]                  │         │
│  └──────────────────────────────┘  └──────────────────────────────┘         │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────────┐          │
│  │ TABLE STATUS │  │ ORDER STATUS │  │ TOP SELLING ITEMS         │          │
│  │ [Donut]      │  │ [Progress]   │  │ [List with counts]        │          │
│  └──────────────┘  └──────────────┘  └───────────────────────────┘          │
│                                                                              │
│  ┌───────────────────────┐  ┌────────────────────────────────────┐          │
│  │ QUICK ACTIONS         │  │ RECENT TRANSACTIONS                │          │
│  │ [Action Buttons]      │  │ [Transaction List]                 │          │
│  └───────────────────────┘  └────────────────────────────────────┘          │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  👥 Customers │ 📦 Items │ 👨‍💼 Staff │ 🟢 System Online                      │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Section 1: Primary KPI Cards (Row 1)

| Card | Icon | Color | Data Source | Method |
|------|------|-------|-------------|--------|
| Today's Sales | ₹ | Green #4CAF50 | BillService | `getTodaysTotalSales()` |
| Today's Orders | Cart | Blue #2196F3 | BillService | `getTodaysBillCount()` |
| Today's Purchase | Truck | Orange #FF9800 | PurchaseBillService | `getTodaysTotalPurchase()` |
| Pending Credit | Card | Red #F44336 | BillRepository | `getTotalCreditBalance()` |

---

## Section 2: Financial Summary Cards (Row 2)

| Card | Description | Calculation |
|------|-------------|-------------|
| Gross Profit | Sales - Purchase (Today) | `todaysSales - todaysPurchase` |
| Cash in Hand | Total bank balance | `BankService.getTotalActiveBalance()` |
| Payments Due | Supplier pending payments | `PurchaseBillService.getTotalPendingPayments()` |
| Avg Order Value | Average bill amount | `totalSales / orderCount` |

---

## Section 3: Charts

### 3.1 Sales Trend Chart (Line Chart)
- **Data**: Last 7 days sales
- **X-Axis**: Days (Mon-Sun)
- **Y-Axis**: Sales Amount (₹)
- **Color**: Gradient blue to purple

### 3.2 Sales vs Purchase Chart (Bar Chart)
- **Data**: Last 7 days comparison
- **Bars**: Green (Sales), Orange (Purchase)
- **Shows**: Profit margin visual

---

## Section 4: Operational Widgets

### 4.1 Table Status (Donut Chart)
- **Segments**: Occupied (Blue), Available (Green), Reserved (Orange)
- **Center**: "8/12 Active"

### 4.2 Order Status (Progress Bars)
- **PAID**: Green bar with count
- **CREDIT**: Yellow bar with count
- **PENDING**: Red bar with count

### 4.3 Top Selling Items (List)
- Top 5 items by quantity sold today
- Shows: Item name + quantity
- Icon indicator for food category

---

## Section 5: Quick Actions & Activity

### 5.1 Quick Actions
| Button | Action | Icon |
|--------|--------|------|
| New Order | Opens Billing | Cart |
| Receive Payment | Opens Payment Receipt | Money |
| New Purchase | Opens Purchase Order | Package |
| View Reports | Opens Report Menu | Chart |

### 5.2 Recent Transactions
- Last 5 transactions
- Shows: Bill#, Amount, Customer, Time ago
- Clickable to view details

---

## Section 6: Footer Statistics

| Metric | Source |
|--------|--------|
| Total Customers | `CustomerService.count()` |
| Total Items | `ItemService.count()` |
| Total Employees | `EmployeesService.count()` |
| System Status | Database connection check |

---

## Color Scheme

| Element | Color Code | Usage |
|---------|------------|-------|
| Primary | #667eea | Headers, primary buttons |
| Success | #4CAF50 | Sales, positive metrics |
| Warning | #FF9800 | Purchase, alerts |
| Danger | #F44336 | Credit, negative metrics |
| Info | #2196F3 | Orders, neutral metrics |
| Background | #f5f5f5 | Page background |
| Card BG | #FFFFFF | Card backgrounds |
| Text Primary | #1a237e | Headings |
| Text Secondary | #616161 | Labels |

---

## Data Methods Required

### DashboardService Methods
```java
// Primary KPIs
Float getTodaysSales()
Long getTodaysOrderCount()
Float getTodaysPurchase()
Float getPendingCreditAmount()

// Financial Summary
Float getGrossProfit()
Float getCashInHand()
Float getPaymentsDue()
Float getAverageOrderValue()

// Chart Data
List<DailySales> getLast7DaysSales()
List<DailyPurchase> getLast7DaysPurchase()

// Operational
TableStatusDTO getTableStatus()
OrderStatusDTO getOrderStatus()
List<TopSellingItem> getTopSellingItems(int limit)

// Recent Activity
List<RecentTransaction> getRecentTransactions(int limit)

// Footer Stats
DashboardStats getStats()
```

---

## Implementation Phases

### Phase 1: Core KPIs
- [ ] Create DashboardService
- [ ] Implement 4 primary KPI methods
- [ ] Create KPI card components
- [ ] Wire data to UI

### Phase 2: Financial Summary
- [ ] Add 4 financial metrics methods
- [ ] Create summary card components
- [ ] Calculate gross profit

### Phase 3: Charts
- [ ] Add chart library (or use JavaFX charts)
- [ ] Implement 7-day data queries
- [ ] Create line chart for sales trend
- [ ] Create bar chart for comparison

### Phase 4: Operational Widgets
- [ ] Table status with donut chart
- [ ] Order status progress bars
- [ ] Top selling items list

### Phase 5: Activity Section
- [ ] Quick action buttons
- [ ] Recent transactions list
- [ ] Footer statistics

### Phase 6: Polish
- [ ] Auto-refresh functionality
- [ ] Loading states
- [ ] Error handling
- [ ] Animations

---

## Files to Create/Modify

### New Files
1. `src/main/java/com/frontend/service/DashboardService.java`
2. `src/main/java/com/frontend/controller/DashboardController.java`
3. `src/main/java/com/frontend/dto/DashboardDTO.java`
4. `src/main/resources/fxml/dashboard/Dashboard.fxml`
5. `src/main/resources/fxml/css/dashboard-material.css`

### Files to Modify
1. `HomeController.java` - Load new dashboard
2. `BillRepository.java` - Add missing queries
3. `PurchaseBillRepository.java` - Add missing queries

---

## UI Components

### KPI Card Structure
```
┌────────────────────────┐
│  [Icon]                │
│                        │
│  ₹45,230              │  <- Large value
│  Today's Sales         │  <- Label
│  ↑12% from yesterday   │  <- Trend indicator
└────────────────────────┘
```

### Design Principles
1. **5-Second Rule**: Key metrics visible immediately
2. **Information Hierarchy**: Most important at top
3. **Consistent Grid**: 4-column layout
4. **Color Coding**: Green=positive, Red=negative
5. **Auto-Refresh**: Every 30 seconds option
6. **Responsive**: Adapts to window size

---

## Dependencies

### Already Available
- MaterialFX (UI components)
- FontAwesomeFX (Icons)
- Spring Data JPA (Data access)

### May Need
- JavaFX Charts (built-in, no additional dependency)
- TilesFX (optional, for fancy gauges)

---

## Testing Checklist

- [ ] All KPI cards show correct data
- [ ] Charts render properly
- [ ] Data refreshes on button click
- [ ] Quick actions navigate correctly
- [ ] Recent transactions are accurate
- [ ] Footer stats are correct
- [ ] No errors on empty data
- [ ] Performance < 2 seconds load time
