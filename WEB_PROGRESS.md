# Hotel Management - Web Frontend Progress Tracker

## Project: Convert JavaFX Desktop App to Web Application (HTML/CSS/Bootstrap/JS)
## Approach: Option 1 - Static files served from Spring Boot (src/main/resources/static/)
## Backend: Existing Spring Boot REST APIs (port 8081)

---

## COMPLETED

### Day 1 (2026-03-07)
- [x] Removed git-related files (.git, .gitignore, .gitattributes)
- [x] Created static web folder structure under src/main/resources/static/
- [x] Created core JS utility files:
  - js/app.js - API client, auth helpers, common utilities
  - js/auth.js - Login/logout, JWT token management
  - js/dashboard.js - Dashboard data loading
- [x] Created WebAuthController.java (/api/v1/web/*) - web-specific auth API:
  - GET /api/v1/web/usernames - load usernames (no mobile access check)
  - GET /api/v1/web/shops - load restaurants for dropdown (no mobile access check)
  - POST /api/v1/web/login - authenticate (no mobile access check, sets server session)
  - GET /api/v1/web/health - server health check
  - GET /api/v1/web/dashboard - dashboard table status data (requires auth)
- [x] Added web public endpoints to JwtAuthenticationFilter (login, health, shops, usernames)
- [x] Downloaded local libraries (no CDN):
  - lib/bootstrap/css/bootstrap.min.css
  - lib/bootstrap/js/bootstrap.bundle.min.js
  - lib/bootstrap-icons/bootstrap-icons.min.css + woff/woff2 fonts
  - lib/sweetalert2/sweetalert2.all.min.js
- [x] Copied Kiran font to static/fonts/kiran.ttf (for Marathi text)
- [x] Created Login page (index.html) - EXACT replica of desktop login.fxml
- [x] Created css/login.css - pixel-perfect match of desktop Login.css
- [x] Created Dashboard page (pages/dashboard.html) - EXACT replica of desktop Home.fxml
- [x] Created css/style.css - exact match of desktop home-material.css
- [x] Created reusable AutoCompleteTextField component (js/components/autocomplete-textfield.js)

### Day 2 (2026-03-07)
- [x] Created comprehensive Dashboard page (pages/dashboard/Dashboard.html)
- [x] Created js/pages/dashboard/Dashboard.js - all dashboard logic
- [x] Created css/dashboard-material.css
- [x] Created WebDashboardController.java in api/dashboard/ package
- [x] Downloaded Chart.js 4.4.1 locally to lib/chartjs/
- [x] Fixed viewport scroll containment and responsive breakpoints
- [x] Created Sales Menu page (pages/transaction/SalesMenu.html)
- [x] Created Billing/POS page (pages/transaction/BillingFrame.html):
  - 3-panel layout: Left (table sections), Center (order entry + numpad), Right (bill history)
  - Desktop-matching proportions: 28% tables | 52% billing+numpad | 20% bill history
  - Responsive: < 1200px hides bill history, < 900px hides numpad
  - < 768px: all 3 sections stack vertically (tables strip, billing, history)
- [x] Created css/billing-material.css - exact match of desktop billing-material.css:
  - Kiran font 20px with text-shadow bold effect (matches desktop dropshadow)
  - Table buttons: Tahoma 15px, gradient backgrounds matching desktop exactly
  - Number pad: 89x50px buttons, 25px bold font (matches desktop)
  - Section colors matching desktop getMaterialColorForSection()
- [x] Created js/pages/transaction/BillingFrame.js - full billing logic:
  - Loads tables with section sequence + merge groups via API
  - Table section display matching desktop: bordered boxes, merged groups side-by-side
  - TilePane-equivalent grid layout: prefColumns=7 (single) or max(2, 7/groupSize) (merged)
  - Customer search autocomplete, add/clear customer
  - Category/Item autocomplete with "All Items" checkbox
  - Keyboard ENTER handlers: Code→ItemName→Qty→Price→auto-Add, CashReceived→Paid
  - Numeric validation on code/qty/price fields
  - Transaction table management (add, edit, remove items via API)
  - Number pad: appends to focused field or updates selected row quantity
  - Payment calculation: change, discount, due, net amount (PAID vs CREDIT logic)
  - Payment mode: loads banks from API, QR print visibility toggle
  - Bill operations: CLOSE, PAID (with discount confirm), SHIFT table, OLD BILL, EDIT BILL
  - Edit bill mode: load PAID/CREDIT bill, SAVE/CANCEL with UI mode changes
  - Kitchen status dialog: KOT list with READY/SERVE action buttons
  - Bill history: search by date/billNo/customer, today's bills on load
- [x] Created new API endpoint GET /api/v1/billing/tables/sections:
  - Returns tables organized by section row groups (sequence + merge info)
  - Same structure as desktop BillingController.loadSections()
  - Each row group is a list of sections with color and tables
  - Uses TableMasterService.getSectionRowGroups() for sequence/merge config
  - Includes getMaterialColorForSection() matching desktop color palette

### Day 3 - Bug Fixes (2026-03-07)
- [x] Fixed 3-panel layout overflow: table buttons hidden behind center section
  - Changed from fixed `width` + `flex-shrink:0` to `flex: 0 1 28%` / `flex: 1 1 52%` / `flex: 0 1 20%`
  - Panels now shrink proportionally when borders/padding exceed 100vw
  - Updated all responsive breakpoints to use `flex` shorthand consistently
- [x] Fixed table buttons overflowing section borders
  - Changed grid columns from `repeat(N, 1fr)` to `repeat(N, minmax(0, 1fr))`
  - Added `overflow: hidden` on section box containers
  - Removed fixed `min-width: 58px` on table buttons, added `text-overflow: ellipsis`
- [x] Reduced table button size: font 15px, height 32px, padding 3px
- [x] Fixed category names showing "undefined" in autocomplete
  - API returns `category` field (from `CategoryMaster.getCategory()`), not `categoryName` or `name`
  - Fixed all 4 references: suggestion list, category lookup, and 2x setText calls
- [x] Fixed category/item autocomplete font size: 16px → 20px (Kiran font needs min ~20px)
- [x] Added text-shadow for Kiran bold effect on AutoCompleteTextField input and popup rows
- [x] Fixed table selection returning "please select table" on Add button click
  - API `TableStatusDto` returns `tableId`/`tableName`, not `id`/`name`
  - Fixed `createSectionBox()` to use `table.tableId || table.id` and `table.tableName || table.name`

### Day 4 - BillingFrame Parity Fixes (2026-03-07)
- [x] Fixed selectedCustomerDisplay not hiding on clearSelectedCustomer()
  - Added `style.display = 'none'` to match desktop's `setVisible(false)`
- [x] Added table selection block during edit bill mode
  - Desktop prevents table changes while editing; web now shows warning
- [x] Added waiter dropdown → category field auto-focus
  - Matches desktop `cmbWaitorName.setOnHidden` → `txtCategoryName.requestFocus()`
- [x] Fixed Close button disable state when only closed bill items exist
  - Desktop disables Close when table has closed bill but no new items
  - Web now properly disables/enables with opacity feedback
- [x] Fixed number pad row-update: digit ADD (not string append)
  - Desktop: `newQty = currentQty + digit` (arithmetic), web was doing string concat
  - After numpad update, now clears input fields and deselects row (matches desktop)
- [x] Added bill history double-click to view bill details
  - Matches desktop `viewBillDetails()` on table double-click
  - Loads bill items into transaction table for viewing
- [x] Enhanced validateItemForm() to match desktop validate()
  - Added zero-quantity check, rate > 0 check
  - Added negative quantity validation (item must exist, qty must not go below 0)
  - Added field focus on validation failure
  - Added amount-not-calculated check before add
- [x] Fixed edit-bill-mode add: merge items with same name+rate
  - Matches desktop `addItemToListOnly()` which merges duplicate items
  - Removes item from list if merged qty becomes <= 0

### Day 4 (cont.) - Deep Parity Fixes for markAsPaid, editBill, showOldBill (2026-03-07)
- [x] Rewrote markAsPaid() with all 6 desktop validations:
  - Empty items check, bill status check (PAID/CREDIT already done), CLOSE status required
  - "Please enter Cash Received or select Customer" when both empty (matches desktop prompt)
  - Bill amount from currentClosedBill instead of DOM scraping
  - Discount confirmation with desktop format: "Accepting ₹X for bill ₹Y\nDiscount: ₹Z"
  - Sends discount field in pay API call
- [x] Inlined afterPaymentComplete() into markAsPaid() (removed separate function)
  - Adds waiter dropdown clear (selectedIndex = 0) matching desktop cmbWaitorName.clearSelection()
  - Single try/catch around both CREDIT and PAID paths
- [x] Fixed cancelEditBillMode() to clear waiter dropdown (matches desktop)
- [x] Improved showOldBill() to try /billing/bills/last-paid API first (matches desktop getLastPaidBill)
  - Fallback to today's paid bills if API unavailable
  - Clears bill history selection after getting bill (matches desktop clearSelection)
- [x] Fixed editBill() to clear bill history selection after loading
  - Clears customer if bill has no customer (matches desktop else branch)
  - Handles bankId/paymode fallback for payment mode dropdown
  - Enables CLOSE/SAVE button explicitly (overrides disabled state from updateCloseButtonState)
- [x] Added isEditBillMode guard in updateCloseButtonState() to keep SAVE always enabled
- [x] Fixed clearPaymentFields() to reset payment mode dropdown to first option (CASH)
- [x] Fixed bill history row click to clear previous row's inline backgroundColor

### Key Decision: Why new /api/v1/web/* endpoints?
- Existing /api/v1/auth/login was designed for MOBILE app only
- It checks `mobileAppSettingService.isMobileAccessEnabled()` which blocks web login
- Desktop app uses direct database auth (AuthApiService.login) without mobile checks
- Web endpoints work exactly like desktop LoginController: shop selection + direct auth

---

## PENDING

### Phase 1 - Master Data Pages (Priority: HIGH)
- [ ] Master Menu page (pages/master/index.html)
- [ ] Add/Edit Customer (pages/master/customers.html) - API: /api/v1/customers
- [ ] Add/Edit Category (pages/master/categories.html) - API: /api/v1/categories
- [ ] Add/Edit Item (pages/master/items.html) - API: /api/v1/items
- [ ] Add/Edit Employee (pages/master/employees.html) - API: /api/v1/employees
- [ ] Add/Edit Table (pages/master/tables.html) - API: /api/v1/billing/tables
- [ ] Add/Edit Bank (pages/master/banks.html) - API: /api/v1/banks
- [ ] Add/Edit Supplier (pages/master/suppliers.html) - needs new API
- [ ] Add/Edit User (pages/master/users.html) - needs new API

### Phase 2 - Remaining Transaction Pages (Priority: HIGH)
- [ ] Receive Payment (pages/transaction/receive-payment.html) - needs new API
- [ ] Purchase Order (pages/transaction/purchase-order.html) - needs new API
- [ ] Purchase Bill (pages/transaction/purchase-bill.html) - needs new API
- [ ] Pay Receipt (pages/transaction/pay-receipt.html) - needs new API
- [ ] Kitchen Display (pages/transaction/kitchen.html) - API: /api/v1/kitchen-orders/*

### Phase 3 - Report Pages (Priority: MEDIUM)
- [ ] Sales Report (pages/report/sales.html) - needs new API
- [ ] Purchase Report (pages/report/purchase.html) - needs new API
- [ ] Stock Report (pages/report/stock.html) - needs new API
- [ ] Bill Search (pages/report/bill-search.html) - API: /api/v1/billing/bills/search
- [ ] Payment Received Report - needs new API
- [ ] Reduced Item Report - needs new API

### Phase 4 - Employee Pages (Priority: MEDIUM)
- [ ] Employee Attendance (pages/employee/attendance.html) - needs new API
- [ ] Employee Salary (pages/employee/salary.html) - needs new API
- [ ] Employee Advance (pages/employee/advance.html) - needs new API

### Phase 5 - Settings Pages (Priority: LOW)
- [ ] Settings Menu (pages/setting/index.html)
- [ ] Application Settings - needs new API
- [ ] Shop Details - needs new API
- [ ] User Rights - needs new API
- [ ] Section Sequence (drag-drop reorder, merge/split) - needs new API
- [ ] Mobile App Settings

### Phase 6 - Backend API Gaps (Need new REST endpoints)
- [ ] Supplier CRUD API
- [ ] User CRUD API
- [ ] Purchase Order/Bill CRUD APIs
- [ ] Payment Receipt CRUD APIs
- [ ] Reports APIs (Sales, Purchase, Stock, etc.)
- [ ] Employee Attendance/Salary/Advance APIs
- [ ] Application Settings CRUD API
- [ ] Shop Details CRUD API
- [ ] Section Sequence API (save order + merge groups)

### Phase 7 - Enhancements (Priority: LOW)
- [ ] Multi-language support (English/Marathi toggle)
- [ ] Print bill from browser (thermal receipt format)
- [ ] WebSocket real-time updates for kitchen orders
- [ ] Dark mode toggle

---

## FILE STRUCTURE
```
src/main/java/com/frontend/api/
  WebAuthController.java              -- Web auth API (DONE)
  BillingApiController.java           -- Billing API (ENHANCED - added /tables/sections)
  dashboard/
    WebDashboardController.java       -- Web dashboard API (DONE)

src/main/resources/static/
  index.html              -- Login page (DONE)
  css/
    style.css             -- Main layout styles (DONE)
    login.css             -- Login styles (DONE)
    dashboard-material.css -- Dashboard styles (DONE)
    billing-material.css  -- Billing POS styles (DONE)
  js/
    app.js                -- API client & utilities (DONE)
    auth.js               -- Authentication (DONE)
    dashboard.js          -- Home page table overview (DONE)
    pages/
      dashboard/
        Dashboard.js      -- Comprehensive dashboard logic (DONE)
      transaction/
        BillingFrame.js   -- Full billing/POS logic (DONE)
    components/
      autocomplete-textfield.js -- Reusable AutoCompleteTextField (DONE)
  fonts/
    kiran.ttf             -- Kiran font for Marathi text (DONE)
  lib/
    bootstrap/            -- Bootstrap 5 CSS + JS (LOCAL)
    bootstrap-icons/      -- Bootstrap Icons CSS + fonts (LOCAL)
    sweetalert2/          -- SweetAlert2 (LOCAL)
    chartjs/              -- Chart.js 4.4.1 (LOCAL)
  pages/
    dashboard.html        -- Home page / table overview (DONE)
    dashboard/
      Dashboard.html      -- Comprehensive dashboard (DONE)
    transaction/
      SalesMenu.html      -- Sales submenu (DONE)
      BillingFrame.html   -- Billing/POS page (DONE)
    master/               -- Master data pages (PENDING)
    report/               -- Report pages (PENDING)
    setting/              -- Settings pages (PENDING)
    employee/             -- Employee pages (PENDING)
```

---

## API ENDPOINTS USED BY WEB FRONTEND
| Frontend Page | API Endpoint | Status |
|---|---|---|
| Login - shops dropdown | GET /api/v1/web/shops | DONE |
| Login - usernames | GET /api/v1/web/usernames | DONE |
| Login - authenticate | POST /api/v1/web/login | DONE |
| Login - health check | GET /api/v1/web/health | DONE |
| Dashboard - table status | GET /api/v1/web/dashboard | DONE |
| Dashboard - full data | GET /api/v1/web/dashboard/full | DONE |
| Billing - tables with sections | GET /api/v1/billing/tables/sections | DONE (NEW) |
| Billing - flat tables | GET /api/v1/billing/tables | DONE |
| Billing - table status | GET /api/v1/billing/tables/{id}/status | DONE |
| Billing - table transactions | GET /api/v1/billing/tables/{id}/transactions | DONE |
| Billing - add items | POST /api/v1/billing/tables/{id}/transactions | DONE |
| Billing - update item | PUT /api/v1/billing/transactions/{id} | DONE |
| Billing - remove item | DELETE /api/v1/billing/transactions/{id} | DONE |
| Billing - close table | POST /api/v1/billing/tables/{id}/close | DONE |
| Billing - closed bill | GET /api/v1/billing/tables/{id}/closed-bill | DONE |
| Billing - pay bill | POST /api/v1/billing/bills/{no}/pay | DONE |
| Billing - credit bill | POST /api/v1/billing/bills/{no}/credit | DONE |
| Billing - get bill | GET /api/v1/billing/bills/{no} | DONE |
| Billing - update bill | PUT /api/v1/billing/bills/{no} | DONE |
| Billing - print bill | POST /api/v1/billing/bills/{no}/print | DONE |
| Billing - print KOT | POST /api/v1/billing/tables/{id}/print-kot | DONE |
| Billing - shift table | POST /api/v1/billing/tables/shift | DONE |
| Billing - today's bills | GET /api/v1/billing/bills/today | DONE |
| Billing - search bills | POST /api/v1/billing/bills/search | DONE |
| Billing - kitchen orders | GET /api/v1/billing/tables/{id}/kitchen-orders | DONE |
| Billing - KOT ready | PUT /api/v1/billing/kitchen-orders/{id}/ready | DONE |
| Billing - KOT serve | PUT /api/v1/billing/kitchen-orders/{id}/serve | DONE |
| Master - categories | GET /api/v1/categories | DONE |
| Master - items | GET /api/v1/items | DONE |
| Master - customers | GET /api/v1/customers | DONE |
| Master - add customer | POST /api/v1/customers | DONE |
| Master - waiters | GET /api/v1/employees/waiters | DONE |
| Master - banks | GET /api/v1/banks | DONE |

---

## NOTES
- All libraries are LOCAL files (no CDN) in lib/ folder
- Icons: Bootstrap Icons (local)
- Alerts: SweetAlert2 (local)
- Kiran font loaded via @font-face from /fonts/kiran.ttf (same as desktop SessionService)
- Kiran font uses text-shadow to replicate desktop's JavaFX dropshadow bold effect
- Kiran font needs minimum ~20px to render Marathi characters correctly
- Backend runs on port 8081 (server profile)
- JWT token stored in localStorage
- Web uses /api/v1/web/* (no mobile access check) instead of /api/v1/auth/*
- All API responses wrapped in ApiResponse: { message, success, data, errorCode }
- Billing layout uses `flex` shorthand (not fixed `width`) to prevent overflow: `flex: 0 1 28%` | `flex: 1 1 52%` | `flex: 0 1 20%`
- Section sequence & merge groups stored in application_settings table (section_sequence, section_row_groups)
- Table sections API returns row groups: List<List<{section, color, tables}>>
- API field name mapping (always check entity class):
  - CategoryMaster: `category` (not categoryName/name)
  - TableStatusDto: `tableId`, `tableName` (not id/name)
  - Use fallback pattern: `obj.tableId || obj.id` for safety
