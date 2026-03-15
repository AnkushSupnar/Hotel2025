/**
 * BillingFrame.js - Web replica of desktop BillingController.java
 * Handles all billing page logic: tables, items, orders, payments, bill history
 */

document.addEventListener('DOMContentLoaded', function () {
    if (!requireAuth()) return;
    initBilling();
});

// ==================== STATE ====================

let allCategories = [];
let allItems = [];
let currentCategoryId = null;
let selectedCustomer = null;
let selectedTableId = null;
let selectedTableName = '';
let tempTransactionList = [];
let selectedTransactionIndex = -1;
let isEditMode = false;
let isEditBillMode = false;
let editBillNo = null;
let isShiftTableMode = false;
let shiftSourceTableId = null;
let shiftSourceTableName = '';
let currentClosedBill = null;
let tableButtonMap = {};
let allBanks = [];
let cashBankId = null;
let focusedNumericField = null;
let isNumberPadUpdate = false; // Flag to suppress form population during number pad updates (matches desktop)
let allCustomersList = []; // Store full customer list for reload
let isAddingItem = false; // Guard flag to prevent duplicate addItem calls

// Autocomplete instances
let categoryAC = null;
let itemAC = null;
let customerAC = null;
let billHistoryCustomerAC = null;

// ==================== INITIALIZATION ====================

async function initBilling() {
    setupNumberPad();
    setupEnterKeyHandlers();
    setupCashCounter();
    setupActionButtons();
    setupBillActionButtons();
    setupBillHistorySearch();
    setupNumericFieldTracking();
    setupWaiterFocusHandler();
    setupAllItemsCheckbox();
    setupTransactionRowSelection();

    // Set today's date for bill history search
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('dpSearchDate').value = today;

    // Load data in parallel
    await Promise.all([
        loadTablesAndSections(),
        loadWaiters(),
        loadCategories(),
        loadAllItems(),
        loadCustomers(),
        loadPaymentModes(),
        loadTodaysBills()
    ]);
}

// ==================== TABLE LOADING ====================
// Desktop layout: sections ordered by sequence, merged groups display side-by-side.
// API: GET /billing/tables/sections returns List<List<{section, color, tables}>>
// Each inner list is a "row group" — merged sections share a horizontal row.

async function loadTablesAndSections() {
    try {
        // Try new sections API first (has sequence + merge info)
        const resp = await apiGet('/billing/tables/sections');
        const sectionGroups = resp.data || resp || [];
        renderSectionGroups(sectionGroups);
    } catch (e) {
        console.error('Sections API failed, falling back to flat tables:', e);
        try {
            const resp = await apiGet('/billing/tables');
            const tables = resp.data || resp || [];
            renderFlatTableSections(tables);
        } catch (e2) {
            console.error('Failed to load tables:', e2);
        }
    }
}

/**
 * Render section row groups matching desktop BillingController.loadSections()
 * sectionGroups = [ [{section, color, tables}, ...], ... ]
 * Each top-level array is a row group. Groups with >1 section display side-by-side.
 */
function renderSectionGroups(sectionGroups) {
    const container = document.getElementById('sectionsContainer');
    container.innerHTML = '';
    tableButtonMap = {};

    sectionGroups.forEach(group => {
        if (!group || group.length === 0) return;

        const isMerged = group.length > 1;

        if (isMerged) {
            // Merged group: sections display side-by-side in a horizontal row
            const rowContainer = document.createElement('div');
            rowContainer.style.cssText = 'display:flex; gap:4px; margin-bottom:6px;';

            group.forEach(sectionData => {
                const box = createSectionBox(sectionData, isMerged, group.length);
                if (box) {
                    box.style.flex = '1';
                    box.style.minWidth = '0';
                    rowContainer.appendChild(box);
                }
            });

            if (rowContainer.childNodes.length > 0) {
                container.appendChild(rowContainer);
            }
        } else {
            // Single section: full-width
            const sectionData = group[0];
            const box = createSectionBox(sectionData, false, 1);
            if (box) container.appendChild(box);
        }
    });
}

/**
 * Create a section box: bordered card with table buttons inside.
 * Desktop: VBox with border-color from section, TilePane with hgap=3 vgap=3
 * prefColumns = isMerged ? max(2, 7/groupSize) : 7
 */
function createSectionBox(sectionData, isMerged, groupSize) {
    const section = sectionData.section;
    const color = sectionData.color || '#616161';
    const tables = sectionData.tables || [];
    if (tables.length === 0) return null;

    // Desktop: prefColumns for TilePane
    const prefColumns = isMerged ? Math.max(2, Math.floor(7 / groupSize)) : 7;

    // Bordered box matching desktop: border-color=sectionColor, bg=#FAFAFA, radius 4
    const box = document.createElement('div');
    box.style.cssText = `
        border: 1.5px solid ${color};
        border-radius: 4px;
        background: #FAFAFA;
        padding: 4px;
        margin-bottom: 6px;
        overflow: hidden;
    `;

    // Table buttons in a grid (TilePane equivalent)
    const grid = document.createElement('div');
    grid.style.cssText = `
        display: grid;
        grid-template-columns: repeat(${prefColumns}, minmax(0, 1fr));
        gap: 3px;
    `;

    tables.forEach(table => {
        const id = table.tableId || table.id;
        const name = table.tableName || table.name;
        const status = table.status || 'Available';
        const btn = document.createElement('button');
        btn.className = 'table-btn';
        btn.textContent = name;
        btn.dataset.tableId = id;
        btn.dataset.tableName = name;
        applyTableButtonStatus(btn, status);
        btn.addEventListener('click', () => handleTableClick(id, name, btn));
        grid.appendChild(btn);
        tableButtonMap[id] = btn;
        // Debug: log non-available tables so we can verify status is coming from API
        if (status.toLowerCase() !== 'available') {
            console.log('Table', name, '(id:' + id + ') status:', status);
        }
    });

    box.appendChild(grid);
    return box;
}

/**
 * Fallback: render flat table list grouped by section (no merge/sequence info)
 */
function renderFlatTableSections(tables) {
    const container = document.getElementById('sectionsContainer');
    container.innerHTML = '';
    tableButtonMap = {};

    const sectionMap = new Map();
    tables.forEach(t => {
        const section = t.section || t.description || 'Default';
        if (!sectionMap.has(section)) sectionMap.set(section, []);
        sectionMap.get(section).push(t);
    });

    // Fallback color map
    const defaultColors = {
        'A': '#1976D2', 'B': '#7B1FA2', 'C': '#C2185B', 'D': '#D32F2F',
        'E': '#F57C00', 'G': '#388E3C', 'V': '#0097A7', 'P': '#5D4037',
        'HP': '#455A64', 'W': '#00796B'
    };

    sectionMap.forEach((sectionTables, sectionName) => {
        const color = defaultColors[sectionName.toUpperCase()] || '#616161';
        const fakeGroup = [{ section: sectionName, color: color, tables: sectionTables }];
        renderSectionGroups([fakeGroup]);
    });
}

function applyTableButtonStatus(btn, status) {
    const s = (status || '').toLowerCase().trim();
    btn.dataset.status = s;
    // Remove all status classes first, then apply the correct one
    btn.classList.remove('table-btn-available', 'table-btn-ongoing', 'table-btn-closed');
    if (s === 'ongoing' || s === 'running') {
        btn.classList.add('table-btn-ongoing');
    } else if (s === 'closed' || s === 'close' || s === 'billed') {
        btn.classList.add('table-btn-closed');
    } else {
        btn.classList.add('table-btn-available');
    }
}

async function handleTableClick(tableId, tableName, btn) {
    // Block table selection during edit bill mode (matches desktop)
    if (isEditBillMode) {
        showWarning('Cannot change table while editing a bill. Save or cancel first.');
        return;
    }

    if (isShiftTableMode) {
        handleShiftTableTarget(tableId, tableName);
        return;
    }

    selectedTableId = tableId;
    selectedTableName = tableName;
    document.getElementById('txtTableNumber').value = tableName;

    // Highlight selected table using CSS class
    Object.values(tableButtonMap).forEach(b => b.classList.remove('table-btn-selected'));
    btn.classList.add('table-btn-selected');

    // Clear customer selection when switching tables
    clearSelectedCustomer();

    await loadTransactionsForTable(tableId);
}

// ==================== LOAD TRANSACTIONS ====================

async function loadTransactionsForTable(tableId) {
    try {
        tempTransactionList = [];
        currentClosedBill = null;

        // Load closed bill if exists
        try {
            const closedResp = await apiGet('/billing/tables/' + tableId + '/closed-bill');
            const closedBill = closedResp.data || closedResp;
            if (closedBill && closedBill.billNo) {
                currentClosedBill = closedBill;
                const items = closedBill.items || [];
                items.forEach(item => {
                    tempTransactionList.push({
                        id: -(item.id || Math.random() * 10000),
                        itemName: item.itemName,
                        qty: item.quantity || item.qty,
                        rate: item.rate,
                        amt: item.amount || item.amt || (item.quantity || item.qty) * item.rate,
                        tableNo: tableId,
                        waitorId: closedBill.waitorId,
                        isClosed: true
                    });
                });
                // Set customer from closed bill
                if (closedBill.customerId && closedBill.customerName) {
                    displaySelectedCustomer({
                        id: closedBill.customerId,
                        firstName: closedBill.customerName,
                        mobileNo: closedBill.customerMobile || ''
                    });
                }
                // Set waiter from closed bill
                if (closedBill.waitorName) {
                    const cmb = document.getElementById('cmbWaitorName');
                    for (let i = 0; i < cmb.options.length; i++) {
                        if (cmb.options[i].text === closedBill.waitorName || cmb.options[i].value == closedBill.waitorId) {
                            cmb.selectedIndex = i;
                            break;
                        }
                    }
                }
            }
        } catch (e) { console.log('No closed bill for table', tableId, e.message); }

        // Load temp transactions
        try {
            const txResp = await apiGet('/billing/tables/' + tableId + '/transactions');
            const txList = txResp.data || txResp || [];

            // Build dedup tracker from closed bill items to prevent duplication
            // (backend may not clear temp transactions after close)
            const closedQtyMap = {};
            if (currentClosedBill) {
                tempTransactionList.forEach(tx => {
                    if (tx.isClosed) {
                        const key = tx.itemName + '|' + tx.rate;
                        closedQtyMap[key] = (closedQtyMap[key] || 0) + parseFloat(tx.qty);
                    }
                });
            }

            txList.forEach(tx => {
                const txQty = parseFloat(tx.quantity || tx.qty);
                const txRate = parseFloat(tx.rate);

                // Deduplicate: skip temp transactions already represented in closed bill
                if (currentClosedBill) {
                    const key = tx.itemName + '|' + txRate;
                    if (closedQtyMap[key] && closedQtyMap[key] >= txQty) {
                        // This temp transaction is already in the closed bill - skip it
                        closedQtyMap[key] -= txQty;
                        return;
                    } else if (closedQtyMap[key] && closedQtyMap[key] > 0) {
                        // Partial overlap - only keep the extra quantity as new item
                        const extraQty = txQty - closedQtyMap[key];
                        closedQtyMap[key] = 0;
                        if (extraQty > 0) {
                            tempTransactionList.push({
                                id: tx.id,
                                itemName: tx.itemName,
                                qty: extraQty,
                                rate: txRate,
                                amt: extraQty * txRate,
                                tableNo: tableId,
                                waitorId: tx.waitorId,
                                isClosed: false
                            });
                        }
                        return;
                    }
                }

                // No closed bill or item not in closed bill - add as new
                tempTransactionList.push({
                    id: tx.id,
                    itemName: tx.itemName,
                    qty: txQty,
                    rate: txRate,
                    amt: tx.amount || tx.amt || txQty * txRate,
                    tableNo: tableId,
                    waitorId: tx.waitorId,
                    isClosed: false
                });
            });
            // Set waiter from first temp transaction
            if (txList.length > 0 && txList[0].waitorId) {
                const cmb = document.getElementById('cmbWaitorName');
                for (let i = 0; i < cmb.options.length; i++) {
                    if (cmb.options[i].value == txList[0].waitorId) {
                        cmb.selectedIndex = i;
                        break;
                    }
                }
            }
        } catch (e) { console.log('No temp transactions for table', tableId, e.message); }

        renderTransactionTable();
        updateTotals();
        updateCloseButtonState();

        // Focus appropriate field (matches desktop: fresh table → waiter dropdown, existing → category)
        if (tempTransactionList.length === 0) {
            // Fresh table: focus waiter dropdown and try to open it
            // (matches desktop cmbWaitorName.requestFocus() + cmbWaitorName.show())
            const cmb2 = document.getElementById('cmbWaitorName');
            cmb2.focus();
            // Use showPicker() to programmatically open the <select> dropdown (Chrome 99+, Firefox 119+)
            // Falls back gracefully: field stays focused, user clicks/taps to open
            setTimeout(() => {
                try { cmb2.showPicker(); } catch (e) { /* Browser doesn't support showPicker */ }
            }, 150);
        } else {
            document.getElementById('txtCategoryName').focus();
        }
    } catch (e) {
        console.error('Failed to load transactions:', e);
    }
}

function renderTransactionTable() {
    const tbody = document.getElementById('transactionBody');
    tbody.innerHTML = '';
    selectedTransactionIndex = -1;

    if (tempTransactionList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="table-empty-msg">Select a table to start billing</td></tr>';
        return;
    }

    tempTransactionList.forEach((tx, index) => {
        const tr = document.createElement('tr');
        tr.dataset.index = index;
        if (tx.isClosed) tr.style.opacity = '0.7';

        tr.innerHTML = `
            <td class="col-srno">${index + 1}</td>
            <td class="col-item-name">${tx.itemName}</td>
            <td class="col-qty">${tx.qty}</td>
            <td class="col-rate">${parseFloat(tx.rate).toFixed(2)}</td>
            <td class="col-amount">${parseFloat(tx.amt).toFixed(2)}</td>
        `;

        tr.addEventListener('click', () => selectTransactionRow(index));
        tbody.appendChild(tr);
    });

    // Auto scroll to bottom
    const wrapper = tbody.closest('.transaction-table-wrapper');
    if (wrapper) wrapper.scrollTop = wrapper.scrollHeight;
}

function selectTransactionRow(index) {
    // Suppress form population during number pad updates (matches desktop isNumberPadUpdate flag)
    if (isNumberPadUpdate) return;

    selectedTransactionIndex = index;
    const rows = document.querySelectorAll('#transactionBody tr');
    rows.forEach((r, i) => {
        r.classList.toggle('selected', i === index);
    });

    // Populate form from selection (matches desktop populateFormFromSelection)
    if (index >= 0 && index < tempTransactionList.length) {
        populateFormFromSelection(tempTransactionList[index]);
    }
}

/**
 * Populate form fields from selected transaction row
 * Matches desktop: sets item name, price, amount, code. Only sets quantity if in edit mode.
 */
function populateFormFromSelection(tx) {
    if (!tx) return;

    // Set item name using autocomplete setText (prevents popup)
    if (itemAC) itemAC.setText(tx.itemName);
    else document.getElementById('txtItemName').value = tx.itemName;

    // Only load quantity when in edit mode (matches desktop behavior)
    if (isEditMode) {
        const qty = tx.qty;
        document.getElementById('txtQuantity').value = (qty === Math.floor(qty)) ? Math.floor(qty) : qty;
    } else {
        document.getElementById('txtQuantity').value = '';
    }

    document.getElementById('txtPrice').value = tx.rate;
    document.getElementById('txtAmount').value = parseFloat(tx.amt).toFixed(2);

    // Look up item code from item list
    const item = allItems.find(it => (it.itemName || it.name) === tx.itemName);
    if (item) {
        document.getElementById('txtCode').value = item.itemCode || item.code || '';
    } else {
        document.getElementById('txtCode').value = '';
    }
}

function updateTotals() {
    let totalQty = 0;
    let totalAmt = 0;
    tempTransactionList.forEach(tx => {
        totalQty += parseFloat(tx.qty) || 0;
        totalAmt += parseFloat(tx.amt) || 0;
    });

    document.getElementById('lblTotalQuantity').textContent = totalQty;
    document.getElementById('lblBillAmount').innerHTML = '&#x20B9;' + totalAmt.toFixed(2);

    calculatePayment();
}

function updateCloseButtonState() {
    const btnClose = document.getElementById('btnClose');
    // In edit bill mode, CLOSE becomes SAVE and should always be enabled
    if (isEditBillMode) {
        btnClose.disabled = false;
        btnClose.style.opacity = '';
        btnClose.style.cursor = '';
        return;
    }
    if (currentClosedBill) {
        // Has closed bill - check if new items exist
        const hasNewItems = tempTransactionList.some(tx => !tx.isClosed);
        if (hasNewItems) {
            btnClose.disabled = false;
            btnClose.style.opacity = '';
            btnClose.style.cursor = '';
        } else {
            // Table already closed with no new items - disable Close button (matches desktop)
            btnClose.disabled = true;
            btnClose.style.opacity = '0.5';
            btnClose.style.cursor = 'default';
        }
    } else {
        btnClose.disabled = false;
        btnClose.style.opacity = '';
        btnClose.style.cursor = '';
    }
}

/**
 * Waiter dropdown: after selection, auto-focus category field (matches desktop cmbWaitorName.setOnHidden)
 */
function setupWaiterFocusHandler() {
    const cmb = document.getElementById('cmbWaitorName');
    cmb.addEventListener('change', function () {
        if (this.value) {
            document.getElementById('txtCategoryName').focus();
        }
    });
}

/**
 * "All Items" checkbox: toggle between category-wise and all items mode
 * Matches desktop setupAllItemsCheckbox() exactly:
 * - Checked: disable category field (grey bg), load all items into autocomplete
 * - Unchecked: enable category field, clear items, clear item name
 */
function setupAllItemsCheckbox() {
    const chk = document.getElementById('chkAllItems');
    if (!chk) return;

    chk.addEventListener('change', function () {
        const txtCat = document.getElementById('txtCategoryName');
        if (this.checked) {
            // Disable category, grey out (matches desktop: -fx-background-color: #E0E0E0)
            txtCat.disabled = true;
            txtCat.value = '';
            txtCat.style.backgroundColor = '#E0E0E0';
            txtCat.style.borderColor = '#BDBDBD';
            currentCategoryId = null;

            // Load all items into autocomplete
            const allItemNames = allItems.map(it => it.itemName || it.name);
            if (itemAC) itemAC.setSuggestions(allItemNames);
        } else {
            // Enable category, restore styling (matches desktop: -fx-background-color: #FAFAFA)
            txtCat.disabled = false;
            txtCat.style.backgroundColor = '';
            txtCat.style.borderColor = '';

            // Clear items - user must select category first
            if (itemAC) itemAC.setSuggestions([]);
            document.getElementById('txtItemName').value = '';

            // If category already typed, load its items
            const catName = txtCat.value;
            if (catName) loadItemsByCategory(catName);
        }
    });
}

/**
 * Setup transaction table row click selection
 * Separate from action buttons - handles click → populateFormFromSelection
 */
function setupTransactionRowSelection() {
    // Row selection is handled in renderTransactionTable() via click event on each <tr>
    // This function sets up additional behavior like category field focus after blur
    const txtCat = document.getElementById('txtCategoryName');
    txtCat.addEventListener('blur', function () {
        // On focus lost, load items if category is selected and "All Items" not checked
        const chk = document.getElementById('chkAllItems');
        if (!chk.checked && this.value) {
            loadItemsByCategory(this.value);
        }
    });

    // Item name focus/blur - auto-lookup item when entering/leaving field (matches desktop)
    const txtItem = document.getElementById('txtItemName');
    txtItem.addEventListener('blur', function () {
        if (this.value.trim()) {
            setItemFromName(this.value.trim());
        }
    });
}

// ==================== WAITER LOADING ====================

async function loadWaiters() {
    try {
        const resp = await apiGet('/employees/waiters');
        const waiters = resp.data || resp || [];
        const cmb = document.getElementById('cmbWaitorName');
        cmb.innerHTML = '<option value="">vaoTrcao naava</option>';
        waiters.forEach(w => {
            const opt = document.createElement('option');
            opt.value = w.id || w.employeeId;
            opt.textContent = w.name || w.fullName || w.firstName;
            opt.style.fontFamily = "'Kiran', sans-serif";
            opt.style.fontSize = '18px';
            cmb.appendChild(opt);
        });
    } catch (e) {
        console.error('Failed to load waiters:', e);
    }
}

// ==================== CATEGORY & ITEM LOADING ====================

async function loadCategories() {
    try {
        const resp = await apiGet('/categories');
        allCategories = resp.data || resp || [];
        const categoryNames = allCategories.map(c => c.category || c.categoryName || c.name);

        categoryAC = new AutoCompleteTextField(
            document.getElementById('txtCategoryName'),
            categoryNames,
            {
                customFont: 'Kiran',
                fontSize: 20,
                nextFocusField: document.getElementById('txtCode'),
                useContainsFilter: true,
                onSelection: function (value) {
                    loadItemsByCategory(value);
                }
            }
        );

        // Focus lost handler - load items for selected category
        document.getElementById('txtCategoryName').addEventListener('blur', function () {
            if (this.value) loadItemsByCategory(this.value);
        });
    } catch (e) {
        console.error('Failed to load categories:', e);
    }
}

function loadItemsByCategory(categoryName) {
    const cat = allCategories.find(c =>
        (c.category || c.categoryName || c.name || '').toLowerCase() === categoryName.toLowerCase()
    );
    if (cat) {
        currentCategoryId = cat.id || cat.categoryId;
        const items = allItems.filter(it => (it.categoryId || it.category_id) === currentCategoryId);
        const itemNames = items.map(it => it.itemName || it.name);
        if (itemAC) itemAC.setSuggestions(itemNames);
    }
}

async function loadAllItems() {
    try {
        const resp = await apiGet('/items');
        allItems = resp.data || resp || [];
        const allItemNames = allItems.map(it => it.itemName || it.name);

        itemAC = new AutoCompleteTextField(
            document.getElementById('txtItemName'),
            allItemNames,
            {
                customFont: 'Kiran',
                fontSize: 20,
                nextFocusField: document.getElementById('txtQuantity'),
                useContainsFilter: true,
                onSelection: function (value) {
                    setItemFromName(value);
                }
            }
        );
    } catch (e) {
        console.error('Failed to load items:', e);
    }
}

function setItemFromName(itemName) {
    const item = allItems.find(it =>
        (it.itemName || it.name || '').toLowerCase() === itemName.toLowerCase()
    );
    if (item) {
        document.getElementById('txtCode').value = item.itemCode || item.code || '';
        document.getElementById('txtPrice').value = item.rate || item.price || '';
        if (document.getElementById('txtQuantity').value) {
            calculateAndSetAmount();
        }
    }
}

// "All Items" checkbox handler is set up via setupAllItemsCheckbox() in initBilling()

// ==================== CUSTOMER SEARCH ====================

async function loadCustomers() {
    try {
        const resp = await apiGet('/customers');
        const customers = resp.data || resp || [];
        const suggestions = customers.map(c => {
            const name = [c.firstName, c.middleName, c.lastName].filter(Boolean).join(' ');
            return name + ' ' + (c.mobileNo || '');
        });

        customerAC = new AutoCompleteTextField(
            document.getElementById('txtCustomerSearch'),
            suggestions,
            {
                customFont: 'Kiran',
                fontSize: 14,
                useContainsFilter: true,
                onSelection: function (value) {
                    const parts = value.trim().split(/\s+/);
                    const mobile = parts[parts.length - 1];
                    const name = parts.slice(0, -1).join(' ');
                    const cust = customers.find(c => {
                        const fullName = [c.firstName, c.middleName, c.lastName].filter(Boolean).join(' ');
                        return fullName === name && (c.mobileNo || '') === mobile;
                    });
                    if (cust) displaySelectedCustomer(cust);
                }
            }
        );

        // Bill history customer autocomplete
        billHistoryCustomerAC = new AutoCompleteTextField(
            document.getElementById('txtSearchCustomer'),
            suggestions,
            {
                customFont: 'Kiran',
                fontSize: 16,
                useContainsFilter: true
            }
        );

        // Add new customer button
        document.getElementById('btnAddNewCustomer').addEventListener('click', () => {
            showAddCustomerDialog(customers);
        });

        // Clear customer button
        document.getElementById('btnClearCustomer').addEventListener('click', () => {
            clearSelectedCustomer();
        });
    } catch (e) {
        console.error('Failed to load customers:', e);
    }
}

function displaySelectedCustomer(customer) {
    selectedCustomer = customer;
    const name = customer.firstName || customer.name || '';
    const mobile = customer.mobileNo || customer.mobile || '';
    document.getElementById('lblCustomerName').textContent = name;
    document.getElementById('lblCustomerMobile').textContent = mobile;
    // Show chip, hide search row (matches desktop visible/managed toggle)
    document.getElementById('selectedCustomerDisplay').classList.add('visible');
    document.getElementById('customerSearchRow').style.display = 'none';
    // Recalculate payment (CREDIT vs PAID logic changes with customer)
    calculatePayment();
}

function clearSelectedCustomer() {
    selectedCustomer = null;
    document.getElementById('lblCustomerName').textContent = '-';
    document.getElementById('lblCustomerMobile').textContent = '-';
    // Hide chip, show search row
    document.getElementById('selectedCustomerDisplay').classList.remove('visible');
    document.getElementById('customerSearchRow').style.display = '';
    document.getElementById('txtCustomerSearch').value = '';
    calculatePayment();
}

async function showAddCustomerDialog(customersList) {
    const { value: formValues } = await Swal.fire({
        title: 'Add New Customer',
        html: `
            <input id="swal-cust-name" class="swal2-input" placeholder="Customer Name" style="font-family:'Kiran',sans-serif; font-size:18px;">
            <input id="swal-cust-mobile" class="swal2-input" placeholder="Mobile Number">
        `,
        showCancelButton: true,
        confirmButtonText: 'Save',
        preConfirm: () => {
            return {
                name: document.getElementById('swal-cust-name').value,
                mobile: document.getElementById('swal-cust-mobile').value
            };
        }
    });
    if (formValues && formValues.name) {
        try {
            const resp = await apiPost('/customers', {
                firstName: formValues.name,
                mobileNo: formValues.mobile
            });
            const newCust = resp.data || resp;
            if (newCust) {
                displaySelectedCustomer(newCust);
                // Reload customers to update autocomplete
                loadCustomers();
            }
        } catch (e) {
            showError('Failed to add customer: ' + e.message);
        }
    }
}

// ==================== PAYMENT MODE ====================

async function loadPaymentModes() {
    try {
        const resp = await apiGet('/banks');
        allBanks = resp.data || resp || [];
        const cmb = document.getElementById('cmbPaymentMode');
        cmb.innerHTML = '';

        allBanks.forEach(bank => {
            const opt = document.createElement('option');
            opt.value = bank.id || bank.bankId;
            opt.textContent = bank.bankName || bank.name;
            opt.dataset.upiId = bank.upiId || '';
            opt.style.fontFamily = "'Kiran', sans-serif";
            opt.style.fontSize = '16px';
            if ((bank.ifsc || '').toLowerCase() === 'cash') {
                cashBankId = bank.id || bank.bankId;
                opt.style.color = '#4CAF50';
            } else {
                opt.style.color = '#1976D2';
            }
            cmb.appendChild(opt);
        });

        // Default to cash
        if (cashBankId) cmb.value = cashBankId;

        // QR print visibility
        cmb.addEventListener('change', updatePrintQRVisibility);
        updatePrintQRVisibility();
    } catch (e) {
        console.error('Failed to load payment modes:', e);
        // Fallback: just CASH
        const cmb = document.getElementById('cmbPaymentMode');
        cmb.innerHTML = '<option value="CASH">CASH</option>';
    }
}

function updatePrintQRVisibility() {
    const cmb = document.getElementById('cmbPaymentMode');
    const qrBox = document.getElementById('hboxPrintQR');
    const selectedOpt = cmb.options[cmb.selectedIndex];
    const upiId = selectedOpt?.dataset?.upiId;
    const isCash = selectedOpt?.value == cashBankId;

    if (qrBox) {
        const showQR = !isCash && upiId;
        qrBox.classList.toggle('visible', !!showQR);
        if (showQR) {
            document.getElementById('chkPrintQR').checked = true;
        }
    }
}

// ==================== ENTER KEY HANDLERS ====================

function setupEnterKeyHandlers() {
    // Code field ENTER
    document.getElementById('txtCode').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleCodeEnter();
        }
    });

    // Item Name ENTER - handled by autocomplete, but add fallback
    document.getElementById('txtItemName').addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !itemAC?.isPopupShowing()) {
            e.preventDefault();
            handleItemNameEnter();
        }
    });

    // Quantity ENTER
    document.getElementById('txtQuantity').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleQuantityEnter();
        }
    });

    // Price ENTER
    document.getElementById('txtPrice').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handlePriceEnter();
            addItem();
        }
    });

    // Cash Received ENTER -> trigger PAID
    document.getElementById('txtCashReceived').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            document.getElementById('btnPaid').click();
        }
    });

    // Numeric validation for code field
    document.getElementById('txtCode').addEventListener('input', function () {
        this.value = this.value.replace(/[^\d]/g, '');
    });

    // Numeric validation for quantity (allow negative and decimal)
    document.getElementById('txtQuantity').addEventListener('input', function () {
        this.value = this.value.replace(/[^-\d.]/g, '');
    });

    // Numeric validation for price
    document.getElementById('txtPrice').addEventListener('input', function () {
        this.value = this.value.replace(/[^\d.]/g, '');
    });

    // Auto-calculate amount on quantity blur
    document.getElementById('txtQuantity').addEventListener('blur', function () {
        if (this.value && document.getElementById('txtPrice').value) {
            calculateAndSetAmount();
        }
    });
}

function handleCodeEnter() {
    const code = document.getElementById('txtCode').value.trim();
    if (!code) {
        document.getElementById('txtItemName').focus();
        return;
    }

    const catName = document.getElementById('txtCategoryName').value.trim();

    // Search by code (optionally within category)
    let item = null;
    if (catName && currentCategoryId) {
        item = allItems.find(it =>
            (String(it.itemCode || it.code) === code) &&
            ((it.categoryId || it.category_id) === currentCategoryId)
        );
    }
    if (!item) {
        item = allItems.find(it => String(it.itemCode || it.code) === code);
    }

    if (item) {
        if (itemAC) itemAC.setText(item.itemName || item.name);
        document.getElementById('txtPrice').value = item.rate || item.price || '';

        // Set category if not set
        if (!catName && item.categoryId) {
            const cat = allCategories.find(c => (c.id || c.categoryId) === (item.categoryId || item.category_id));
            if (cat && categoryAC) categoryAC.setText(cat.category || cat.categoryName || cat.name);
        }

        document.getElementById('txtQuantity').focus();
    } else {
        showWarning('Item not found for code: ' + code);
        document.getElementById('txtItemName').focus();
    }
}

function handleItemNameEnter() {
    const name = document.getElementById('txtItemName').value.trim();
    if (!name) return;

    const item = allItems.find(it =>
        (it.itemName || it.name || '').toLowerCase() === name.toLowerCase()
    );
    if (item) {
        document.getElementById('txtCode').value = item.itemCode || item.code || '';
        document.getElementById('txtPrice').value = item.rate || item.price || '';

        if (!document.getElementById('txtCategoryName').value && item.categoryId) {
            const cat = allCategories.find(c => (c.id || c.categoryId) === (item.categoryId || item.category_id));
            if (cat && categoryAC) categoryAC.setText(cat.category || cat.categoryName || cat.name);
        }

        document.getElementById('txtQuantity').focus();
    } else {
        showWarning('Item not found: ' + name);
    }
}

function handleQuantityEnter() {
    const qty = document.getElementById('txtQuantity').value.trim();
    const itemName = document.getElementById('txtItemName').value.trim();
    if (!itemName || !qty) return;

    calculateAndSetAmount();
    document.getElementById('txtPrice').focus();
}

function handlePriceEnter() {
    calculateAndSetAmount();
}

function calculateAndSetAmount() {
    const qty = parseFloat(document.getElementById('txtQuantity').value) || 0;
    const price = parseFloat(document.getElementById('txtPrice').value) || 0;
    if (qty !== 0 && price > 0) {
        const amount = qty * price;
        document.getElementById('txtAmount').value = amount.toFixed(2);
    }
}

// ==================== ACTION BUTTONS ====================

function setupActionButtons() {
    document.getElementById('btnAdd').addEventListener('click', addItem);
    document.getElementById('btnOrder').addEventListener('click', processOrder);
    document.getElementById('btnEditItem').addEventListener('click', editSelectedItem);
    document.getElementById('btnRemove').addEventListener('click', removeSelectedItem);
    document.getElementById('btnClear').addEventListener('click', clearItemForm);
    document.getElementById('btnKitchenStatus').addEventListener('click', showKitchenStatusDialog);
}

function validateItemForm() {
    if (!selectedTableId) { showWarning('Please select a table first'); return false; }
    if (!document.getElementById('cmbWaitorName').value) { showWarning('Please select a waiter'); document.getElementById('cmbWaitorName').focus(); return false; }
    if (!document.getElementById('txtItemName').value.trim()) { showWarning('Please enter item name'); document.getElementById('txtItemName').focus(); return false; }

    const qtyText = document.getElementById('txtQuantity').value.trim();
    if (!qtyText || qtyText === '-' || qtyText === '.' || qtyText === '-.') {
        showWarning('Please enter valid quantity');
        document.getElementById('txtQuantity').focus();
        return false;
    }
    if (!document.getElementById('txtPrice').value.trim()) { showWarning('Please enter price'); document.getElementById('txtPrice').focus(); return false; }

    const qty = parseFloat(qtyText);
    const rate = parseFloat(document.getElementById('txtPrice').value);

    if (qty === 0) { showWarning('Quantity cannot be zero'); document.getElementById('txtQuantity').focus(); return false; }
    if (rate <= 0) { showWarning('Rate must be greater than zero'); document.getElementById('txtPrice').focus(); return false; }

    // Validate negative quantity - item must exist in order
    if (qty < 0) {
        const itemName = document.getElementById('txtItemName').value.trim();
        const existing = tempTransactionList.find(t => t.itemName === itemName && parseFloat(t.rate) === rate);
        if (!existing) {
            showWarning('Cannot reduce quantity. Item "' + itemName + '" not found in the order.');
            document.getElementById('txtQuantity').focus();
            return false;
        }
        if (existing.qty + qty < 0) {
            showWarning('Cannot reduce by ' + Math.abs(qty) + '. Current quantity is only ' + existing.qty);
            document.getElementById('txtQuantity').focus();
            return false;
        }
    }

    return true;
}

async function addItem() {
    // Guard: prevent duplicate calls while a previous addItem is still in progress
    if (isAddingItem) return;

    calculateAndSetAmount();
    if (!validateItemForm()) return;
    if (!document.getElementById('txtAmount').value) { showWarning('Amount not calculated'); return; }

    const itemName = document.getElementById('txtItemName').value.trim();
    const qty = parseFloat(document.getElementById('txtQuantity').value);
    const rate = parseFloat(document.getElementById('txtPrice').value);
    const amt = qty * rate;
    const waitorId = parseInt(document.getElementById('cmbWaitorName').value);

    // Lock: disable button and set guard flag to prevent duplicate submissions
    isAddingItem = true;
    const btnAdd = document.getElementById('btnAdd');
    btnAdd.disabled = true;

    try {
        if (isEditBillMode) {
            if (isEditMode && selectedTransactionIndex >= 0) {
                // Update in list only (edit bill mode)
                tempTransactionList[selectedTransactionIndex].itemName = itemName;
                tempTransactionList[selectedTransactionIndex].qty = qty;
                tempTransactionList[selectedTransactionIndex].rate = rate;
                tempTransactionList[selectedTransactionIndex].amt = amt;
            } else {
                // Check if item with same name and rate exists (matches desktop addItemToListOnly merge logic)
                const existingIdx = tempTransactionList.findIndex(t => t.itemName === itemName && parseFloat(t.rate) === rate);
                if (existingIdx >= 0) {
                    const existing = tempTransactionList[existingIdx];
                    const newQty = parseFloat(existing.qty) + qty;
                    if (newQty <= 0) {
                        // Remove item if quantity becomes zero or negative
                        tempTransactionList.splice(existingIdx, 1);
                    } else {
                        existing.qty = newQty;
                        existing.amt = newQty * existing.rate;
                    }
                } else {
                    if (qty < 0) {
                        showWarning('Cannot reduce quantity. Item not found in the order.');
                        return;
                    }
                    tempTransactionList.push({
                        id: 0,
                        itemName: itemName,
                        qty: qty,
                        rate: rate,
                        amt: amt,
                        tableNo: selectedTableId,
                        waitorId: waitorId,
                        isClosed: false
                    });
                }
            }
            renderTransactionTable();
            updateTotals();
        } else if (isEditMode && selectedTransactionIndex >= 0) {
            // Update existing temp transaction in DB
            const tx = tempTransactionList[selectedTransactionIndex];
            try {
                await apiPut('/billing/transactions/' + tx.id, {
                    itemName: itemName,
                    quantity: qty,
                    rate: rate,
                    amount: amt
                });
                await loadTransactionsForTable(selectedTableId);
            } catch (e) {
                showError('Failed to update item: ' + e.message);
            }
        } else {
            // Add new temp transaction via API
            try {
                await apiPost('/billing/tables/' + selectedTableId + '/transactions', {
                    waitorId: waitorId,
                    items: [{
                        itemName: itemName,
                        quantity: qty,
                        rate: rate
                    }]
                });
                await loadTransactionsForTable(selectedTableId);
                updateTableButtonStatus(selectedTableId);
            } catch (e) {
                showError('Failed to add item: ' + e.message);
            }
        }

        clearItemForm();
        resetEditMode();
        document.getElementById('txtCategoryName').focus();
    } finally {
        // Unlock: re-enable button and clear guard flag
        isAddingItem = false;
        btnAdd.disabled = false;
    }
}

function editSelectedItem() {
    if (selectedTransactionIndex < 0) {
        showWarning('Please select an item to edit');
        return;
    }
    const tx = tempTransactionList[selectedTransactionIndex];
    if (tx.isClosed && !isEditBillMode) {
        showWarning('Cannot edit closed bill items');
        return;
    }

    isEditMode = true;

    // Populate form
    if (itemAC) itemAC.setText(tx.itemName);
    document.getElementById('txtQuantity').value = tx.qty;
    document.getElementById('txtPrice').value = tx.rate;
    document.getElementById('txtAmount').value = tx.amt;

    // Look up item code
    const item = allItems.find(it => (it.itemName || it.name) === tx.itemName);
    if (item) document.getElementById('txtCode').value = item.itemCode || item.code || '';

    // Change button text
    const btnAdd = document.getElementById('btnAdd');
    btnAdd.innerHTML = '<i class="bi bi-check"></i> UPDATE';
    btnAdd.style.background = '#FF9800';

    document.getElementById('txtQuantity').focus();
}

async function removeSelectedItem() {
    if (selectedTransactionIndex < 0) {
        showWarning('Please select an item to remove');
        return;
    }
    const tx = tempTransactionList[selectedTransactionIndex];
    if (tx.isClosed && !isEditBillMode) {
        showWarning('Cannot remove closed bill items');
        return;
    }

    const confirmed = await showConfirm('Remove "' + tx.itemName + '" from the order?');
    if (!confirmed) return;

    if (isEditBillMode) {
        tempTransactionList.splice(selectedTransactionIndex, 1);
        renderTransactionTable();
        updateTotals();
    } else {
        try {
            await apiDelete('/billing/transactions/' + tx.id);
            await loadTransactionsForTable(selectedTableId);
            updateTableButtonStatus(selectedTableId);
        } catch (e) {
            showError('Failed to remove item: ' + e.message);
        }
    }
}

function clearItemForm() {
    if (categoryAC) categoryAC.clear();
    document.getElementById('txtCode').value = '';
    if (itemAC) itemAC.clear();
    document.getElementById('txtQuantity').value = '';
    document.getElementById('txtPrice').value = '';
    document.getElementById('txtAmount').value = '';
    resetEditMode();
}

function resetEditMode() {
    isEditMode = false;
    selectedTransactionIndex = -1;
    const btnAdd = document.getElementById('btnAdd');
    btnAdd.innerHTML = '<i class="bi bi-plus"></i> ADD';
    btnAdd.style.background = '';

    // Deselect table rows
    document.querySelectorAll('#transactionBody tr').forEach(r => r.classList.remove('selected'));
}

// ==================== PDF DOWNLOAD HELPER ====================

function downloadPdfFromBase64(base64Data, filename) {
    const byteCharacters = atob(base64Data);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: 'application/pdf' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

// ==================== ORDER (KOT PRINT) ====================

async function processOrder() {
    if (!selectedTableId) { showWarning('Please select a table first'); return; }

    const btnOrder = document.getElementById('btnOrder');
    btnOrder.disabled = true;
    btnOrder.classList.add('loading');

    try {
        // Server is the source of truth for printQty — it determines which items need printing.
        // Other clients may also add items to the same table, so no client-side printQty check.
        const resp = await apiPost('/billing/tables/' + selectedTableId + '/print-kot', {});

        if (resp && resp.success) {
            const data = resp.data || {};
            const count = data.itemsPrinted || 0;

            // Download KOT PDF with fixed filename (overwrites previous)
            if (data.pdfBase64) {
                downloadPdfFromBase64(data.pdfBase64, 'KOT.pdf');
            }

            showSuccess('KOT generated successfully! ' + count + ' items sent to kitchen.');
        } else {
            showWarning(resp.message || 'No new items to print.');
        }

        // Reload transactions to reflect server state (printQty reset to 0 after print)
        await loadTransactionsForTable(selectedTableId);
    } catch (e) {
        // Backend returns 400 when no printable items exist - show as warning, not error
        const msg = e.message || '';
        if (msg.includes('No new items to print') || msg.includes('already been sent')) {
            showWarning(msg);
        } else {
            showError('Failed to generate KOT: ' + msg);
        }
        // Still reload to get fresh state from server
        await loadTransactionsForTable(selectedTableId);
    } finally {
        btnOrder.disabled = false;
        btnOrder.classList.remove('loading');
    }
}

// ==================== BILL ACTION BUTTONS ====================

function setupBillActionButtons() {
    document.getElementById('btnClose').addEventListener('click', closeTable);
    document.getElementById('btnPaid').addEventListener('click', markAsPaid);
    document.getElementById('btnShiftTable').addEventListener('click', startShiftTableMode);
    document.getElementById('btnOldBill').addEventListener('click', showOldBill);
    document.getElementById('btnEditBill').addEventListener('click', () => {
        if (isEditBillMode) {
            cancelEditBillMode();
        } else {
            editBill();
        }
    });
}

// ==================== CLOSE TABLE ====================

async function closeTable() {
    if (!selectedTableId) { showWarning('Please select a table first'); return; }

    if (isEditBillMode) {
        await saveEditedBill();
        return;
    }

    if (tempTransactionList.length === 0) {
        showWarning('No items to close');
        return;
    }

    // Check if we have only closed bill items and no new items (matches desktop logic)
    const hasClosedBill = currentClosedBill != null;
    const hasNewItems = tempTransactionList.some(tx => !tx.isClosed);

    if (hasClosedBill && !hasNewItems) {
        showWarning('Table already closed. No new items to add.');
        return;
    }

    const btnClose = document.getElementById('btnClose');
    btnClose.classList.add('loading');

    try {
        const user = getUser();
        const waitorId = parseInt(document.getElementById('cmbWaitorName').value) || 0;
        const customerId = selectedCustomer ? (selectedCustomer.id || selectedCustomer.customerId) : null;

        const resp = await apiPost('/billing/tables/' + selectedTableId + '/close', {
            customerId: customerId,
            waitorId: waitorId,
            userId: user?.userId || user?.id
        });

        const bill = resp.data || resp;
        if (bill) {
            currentClosedBill = bill;

            // Desktop auto-prints bill silently (no confirmation) then focuses cash field
            if (bill.billNo) {
                printBill(bill.billNo);
            }
        }

        // Reload transactions to show closed bill items, clear item form
        await loadTransactionsForTable(selectedTableId);
        clearItemForm();

        // Update table button to closed/red (uses local state: currentClosedBill is set)
        updateTableButtonStatus(selectedTableId);

        // Focus cash received so user can immediately enter payment (matches desktop)
        document.getElementById('txtCashReceived').focus();
    } catch (e) {
        showError('Failed to close table: ' + e.message);
    } finally {
        btnClose.classList.remove('loading');
    }
}

// ==================== MARK AS PAID ====================

async function markAsPaid() {
    // Validation 1: Table must be selected
    if (!selectedTableId) { showWarning('Please select a table first'); return; }

    // Validation 2: Must have items
    if (tempTransactionList.length === 0) { showError('No items to bill'); return; }

    // Validation 3: Must have a closed bill
    if (!currentClosedBill) {
        showWarning('Please close the table first');
        return;
    }

    // Validation 4: Check for new temp items not yet closed
    const hasNew = tempTransactionList.some(tx => !tx.isClosed);
    if (hasNew) {
        showWarning('Close the table first to save new items');
        return;
    }

    // Validation 5: Check if bill is already PAID or CREDIT
    const billStatus = (currentClosedBill.status || '').toUpperCase();
    if (billStatus === 'PAID') { showWarning('Bill already PAID'); return; }
    if (billStatus === 'CREDIT') { showWarning('Bill already marked as CREDIT'); return; }

    // Validation 6: Bill must be in CLOSE status
    if (billStatus !== 'CLOSE') {
        showError('Only closed bills can be processed');
        return;
    }

    const billNo = currentClosedBill.billNo;
    const billAmount = parseFloat(currentClosedBill.billAmt || currentClosedBill.totalAmount ||
        document.getElementById('lblBillAmount').textContent.replace(/[^\d.]/g, '')) || 0;

    const cashText = (document.getElementById('txtCashReceived').value || '').trim();
    const returnText = (document.getElementById('txtReturnToCustomer').value || '').trim();
    const cashEmpty = !cashText;
    const cashReceived = parseFloat(cashText) || 0;
    const returnAmount = parseFloat(returnText) || 0;

    // Validation: If no cash entered AND no customer selected, prompt user (matches desktop)
    if (cashEmpty && !selectedCustomer) {
        showWarning('Please enter Cash Received amount for PAID bill,\nor select a Customer for CREDIT bill.');
        document.getElementById('txtCashReceived').focus();
        return;
    }

    const cmb = document.getElementById('cmbPaymentMode');
    const bankId = parseInt(cmb.value) || 0;
    const paymode = (bankId === cashBankId || !bankId) ? 'CASH' : 'BANK';
    const manualDiscount = parseFloat(document.getElementById('txtDiscount').value) || 0;

    // Set loading state on PAID button
    const btnPaid = document.getElementById('btnPaid');
    btnPaid.classList.add('loading');

    try {
        if (selectedCustomer) {
            // CREDIT BILL - Customer selected, cash is optional (partial payment)
            await apiPost('/billing/bills/' + billNo + '/credit', {
                customerId: selectedCustomer.id || selectedCustomer.customerId,
                cashReceived: cashReceived,
                returnAmount: returnAmount,
                discount: manualDiscount
            });
        } else {
            // PAID BILL - Cash is required (already validated above)
            const netReceived = Math.max(0, cashReceived - returnAmount);
            // Use manual discount if entered, otherwise auto-calculate from underpayment
            const discount = manualDiscount > 0 ? manualDiscount : Math.max(0, billAmount - netReceived);

            // Confirm discount if applicable (matches desktop format)
            if (discount > 0) {
                const netAfterDiscount = billAmount - discount;
                const confirmed = await showConfirm(
                    'Accepting \u20B9' + Math.round(netReceived) + ' for bill \u20B9' + Math.round(billAmount) +
                    '\nDiscount: \u20B9' + Math.round(discount) + '\nNet: \u20B9' + Math.round(netAfterDiscount) +
                    '\n\nProceed?'
                );
                if (!confirmed) { btnPaid.classList.remove('loading'); return; }
            }

            await apiPost('/billing/bills/' + billNo + '/pay', {
                cashReceived: cashReceived,
                returnAmount: returnAmount,
                discount: discount,
                paymode: paymode,
                bankId: bankId
            });
        }

        // Ask user if they want to print the bill (matches desktop)
        const wantToPrint = await showConfirm('Print the bill?');
        if (wantToPrint) printBill(billNo);

        // Clear UI and reset state (matches desktop post-payment cleanup)
        tempTransactionList = [];
        currentClosedBill = null;
        renderTransactionTable();
        updateTotals();

        // Update table button status to Available
        if (selectedTableId) {
            updateTableButtonStatus(selectedTableId);
            Object.values(tableButtonMap).forEach(b => b.classList.remove('table-btn-selected'));
        }

        // Clear all form fields (matches desktop)
        clearItemForm();
        clearPaymentFields();
        clearSelectedCustomer();
        document.getElementById('cmbWaitorName').selectedIndex = 0; // Clear waiter (matches desktop)
        document.getElementById('txtTableNumber').value = '-';

        selectedTableId = null;
        selectedTableName = '';

        // Refresh bill history
        loadTodaysBills();
    } catch (e) {
        showError('Error processing payment: ' + e.message);
    } finally {
        btnPaid.classList.remove('loading');
    }
}

function clearPaymentFields() {
    document.getElementById('txtCashReceived').value = '';
    document.getElementById('txtReturnToCustomer').value = '';
    document.getElementById('txtDiscount').value = '';
    document.getElementById('lblChange').innerHTML = '&#x20B9;0';
    document.getElementById('lblBalance').innerHTML = '&#x20B9;0';
    document.getElementById('lblNetAmount').innerHTML = '&#x20B9;0';
    // Reset payment mode to first option (CASH)
    const payCmb = document.getElementById('cmbPaymentMode');
    if (payCmb) payCmb.selectedIndex = 0;
}

// ==================== PRINT BILL ====================

async function printBill(billNo) {
    try {
        await apiPost('/billing/bills/' + billNo + '/print', {});
    } catch (e) {
        // Try PDF download as fallback
        try {
            const resp = await fetch(API_BASE + '/billing/bills/' + billNo + '/pdf', {
                headers: { 'Authorization': 'Bearer ' + getToken() }
            });
            if (resp.ok) {
                const blob = await resp.blob();
                const url = URL.createObjectURL(blob);
                const w = window.open(url, '_blank');
                if (w) w.print();
            }
        } catch (e2) {
            console.error('Print fallback failed:', e2);
        }
    }
}

// ==================== SHIFT TABLE ====================

function startShiftTableMode() {
    if (isShiftTableMode) {
        cancelShiftTableMode();
        return;
    }
    if (!selectedTableId) { showWarning('Please select a table first'); return; }
    if (tempTransactionList.length === 0) { showWarning('No items to shift'); return; }

    isShiftTableMode = true;
    shiftSourceTableId = selectedTableId;
    shiftSourceTableName = selectedTableName;

    const btn = document.getElementById('btnShiftTable');
    btn.innerHTML = '<i class="bi bi-x-lg"></i> CANCEL';
    btn.style.background = '#FF9800';

    showWarning('Select target table to shift items from ' + shiftSourceTableName);
}

async function handleShiftTableTarget(targetId, targetName) {
    if (targetId === shiftSourceTableId) {
        showWarning('Cannot shift to the same table');
        return;
    }

    const confirmed = await showConfirm('Shift all items from ' + shiftSourceTableName + ' to ' + targetName + '?');
    if (!confirmed) {
        cancelShiftTableMode();
        return;
    }

    try {
        await apiPost('/billing/tables/shift', {
            sourceTableId: shiftSourceTableId,
            targetTableId: targetId
        });

        showSuccess('Items shifted to ' + targetName);
        updateTableButtonStatus(shiftSourceTableId);
        updateTableButtonStatus(targetId);

        // Load target table
        handleTableClick(targetId, targetName, tableButtonMap[targetId]);
    } catch (e) {
        showError('Failed to shift table: ' + e.message);
    }

    cancelShiftTableMode();
}

function cancelShiftTableMode() {
    isShiftTableMode = false;
    shiftSourceTableId = null;
    shiftSourceTableName = '';
    const btn = document.getElementById('btnShiftTable');
    btn.innerHTML = '<i class="bi bi-arrow-left-right"></i> SHIFT';
    btn.style.background = '';
}

// ==================== OLD BILL ====================

async function showOldBill() {
    try {
        let billNo = null;

        // Check if bill selected in history
        const selectedRow = document.querySelector('#billHistoryBody tr.selected');
        if (selectedRow) {
            billNo = parseInt(selectedRow.dataset.billNo);
            // Clear selection after getting the bill (matches desktop)
            selectedRow.classList.remove('selected');
        }

        if (!billNo) {
            // Get last paid bill (matches desktop billService.getLastPaidBill())
            try {
                const resp = await apiGet('/billing/bills/last-paid');
                const bill = resp.data || resp;
                if (bill && bill.billNo) {
                    billNo = bill.billNo;
                }
            } catch (e) {
                // Fallback: get last bill from today's list
                const resp = await apiGet('/billing/bills/today');
                const bills = resp.data?.bills || resp.bills || resp.data || [];
                const paidBills = bills.filter(b => b.status === 'PAID' || b.status === 'CREDIT');
                if (paidBills.length > 0) {
                    billNo = paidBills[paidBills.length - 1].billNo;
                }
            }
        }

        if (!billNo) {
            showError('No paid bills found to print');
            return;
        }

        printBill(billNo);
    } catch (e) {
        showError('Error: ' + e.message);
    }
}

// ==================== EDIT BILL ====================

async function editBill() {
    const selectedRow = document.querySelector('#billHistoryBody tr.selected');
    if (!selectedRow) {
        showWarning('Please select a bill from history to edit');
        return;
    }

    const billNo = parseInt(selectedRow.dataset.billNo);
    try {
        const resp = await apiGet('/billing/bills/' + billNo);
        const bill = resp.data || resp;

        if (!bill) { showWarning('Bill not found'); return; }
        if (bill.status !== 'PAID' && bill.status !== 'CREDIT') {
            showWarning('Only PAID or CREDIT bills can be edited');
            return;
        }

        // Load bill into UI
        isEditBillMode = true;
        editBillNo = billNo;
        tempTransactionList = [];

        (bill.items || []).forEach(item => {
            tempTransactionList.push({
                id: item.id,
                itemName: item.itemName,
                qty: item.quantity || item.qty,
                rate: item.rate,
                amt: item.amount || item.amt || (item.quantity || item.qty) * item.rate,
                tableNo: bill.tableNo,
                waitorId: bill.waitorId,
                isClosed: false
            });
        });

        selectedTableId = bill.tableNo;
        selectedTableName = bill.tableName || '';
        document.getElementById('txtTableNumber').value = bill.tableName || bill.tableNo;

        // Set waiter
        const cmb = document.getElementById('cmbWaitorName');
        for (let i = 0; i < cmb.options.length; i++) {
            if (cmb.options[i].value == bill.waitorId) { cmb.selectedIndex = i; break; }
        }

        // Set customer (matches desktop: clear if no customer)
        if (bill.customerId && bill.customerName) {
            displaySelectedCustomer({ id: bill.customerId, firstName: bill.customerName, mobileNo: bill.customerMobile || '' });
        } else {
            clearSelectedCustomer();
        }

        // Set payment
        document.getElementById('txtCashReceived').value = bill.cashReceived || '';
        document.getElementById('txtReturnToCustomer').value = bill.returnAmount || '';
        document.getElementById('txtDiscount').value = bill.discount || '';

        // Set payment mode from saved bill (matches desktop bankId matching logic)
        const payCmb = document.getElementById('cmbPaymentMode');
        if (bill.bankId) {
            payCmb.value = bill.bankId;
        } else if (bill.paymode === 'CASH' || !bill.paymode) {
            // Default to cash (first option)
            payCmb.selectedIndex = 0;
        }

        renderTransactionTable();
        updateTotals();

        // Clear bill history selection (matches desktop)
        const selectedHistoryRow = document.querySelector('#billHistoryBody tr.selected');
        if (selectedHistoryRow) selectedHistoryRow.classList.remove('selected');

        // Update UI for edit mode
        document.getElementById('btnClose').innerHTML = '<i class="bi bi-check-lg"></i> SAVE';
        document.getElementById('btnClose').style.background = '#4CAF50';
        document.getElementById('btnClose').disabled = false;
        document.getElementById('btnClose').style.opacity = '';
        document.getElementById('btnClose').style.cursor = '';
        document.getElementById('btnEditBill').innerHTML = '<i class="bi bi-x-lg"></i> CANCEL';
        document.getElementById('btnEditBill').style.background = '#F44336';
        document.getElementById('btnPaid').disabled = true;
        document.getElementById('btnShiftTable').disabled = true;
        document.getElementById('btnOldBill').disabled = true;

        showSuccess('Editing Bill #' + billNo + '. Make changes and click SAVE.');
    } catch (e) {
        showError('Failed to load bill: ' + e.message);
    }
}

async function saveEditedBill() {
    if (!editBillNo) return;
    if (tempTransactionList.length === 0) { showWarning('Cannot save empty bill'); return; }

    const waitorId = parseInt(document.getElementById('cmbWaitorName').value) || 0;
    const customerId = selectedCustomer ? (selectedCustomer.id || selectedCustomer.customerId) : null;
    const cashReceived = parseFloat(document.getElementById('txtCashReceived').value) || 0;
    const returnAmount = parseFloat(document.getElementById('txtReturnToCustomer').value) || 0;
    const discount = parseFloat(document.getElementById('txtDiscount').value) || 0;

    let totalAmt = 0, totalQty = 0;
    tempTransactionList.forEach(tx => {
        totalQty += parseFloat(tx.qty);
        totalAmt += parseFloat(tx.amt);
    });

    const status = selectedCustomer ? 'CREDIT' : 'PAID';

    try {
        await apiPut('/billing/bills/' + editBillNo, {
            items: tempTransactionList.map(tx => ({
                itemName: tx.itemName,
                quantity: tx.qty,
                rate: tx.rate,
                amount: tx.amt
            })),
            waitorId: waitorId,
            customerId: customerId,
            totalAmount: totalAmt,
            totalQuantity: totalQty,
            cashReceived: cashReceived,
            returnAmount: returnAmount,
            discount: discount,
            status: status
        });

        showSuccess('Bill #' + editBillNo + ' updated successfully');
        cancelEditBillMode();
        loadTodaysBills();
    } catch (e) {
        showError('Failed to update bill: ' + e.message);
    }
}

function cancelEditBillMode() {
    isEditBillMode = false;
    editBillNo = null;
    tempTransactionList = [];
    currentClosedBill = null;

    // Reset buttons first
    document.getElementById('btnClose').innerHTML = '<i class="bi bi-x-lg"></i> CLOSE';
    document.getElementById('btnClose').style.background = '';
    document.getElementById('btnEditBill').innerHTML = '<i class="bi bi-pencil-square"></i> EDIT';
    document.getElementById('btnEditBill').style.background = '';
    document.getElementById('btnPaid').disabled = false;
    document.getElementById('btnShiftTable').disabled = false;
    document.getElementById('btnOldBill').disabled = false;

    // Clear form (matches desktop cancelEditBillMode)
    clearItemForm();
    clearPaymentFields();
    clearSelectedCustomer();
    document.getElementById('cmbWaitorName').selectedIndex = 0; // Clear waiter (matches desktop)
    document.getElementById('txtTableNumber').value = '-';
    updateTotals();
    renderTransactionTable();

    if (selectedTableId) {
        Object.values(tableButtonMap).forEach(b => b.classList.remove('table-btn-selected'));
    }
    selectedTableId = null;
    selectedTableName = '';
}

// ==================== KITCHEN STATUS ====================

async function showKitchenStatusDialog() {
    if (!selectedTableId) { showWarning('Please select a table'); return; }

    try {
        const resp = await apiGet('/billing/tables/' + selectedTableId + '/kitchen-orders');
        const orders = resp.data || resp || [];

        if (orders.length === 0) {
            showWarning('No kitchen orders for this table');
            return;
        }

        let html = '<div style="max-height:400px; overflow-y:auto; text-align:left;">';
        orders.forEach(order => {
            const statusColor = order.status === 'SENT' ? '#FF9800' :
                order.status === 'READY' ? '#4CAF50' : '#2196F3';
            const statusLabel = order.status || 'SENT';
            const time = order.sentAt ? new Date(order.sentAt).toLocaleTimeString() : '';

            html += `<div style="border:1px solid #E0E0E0; border-radius:8px; padding:10px; margin-bottom:8px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                    <strong>KOT #${order.id}</strong>
                    <span style="font-size:11px; color:#757575;">${time}</span>
                    <span style="background:${statusColor}; color:white; padding:2px 8px; border-radius:10px; font-size:11px;">${statusLabel}</span>
                </div>
                <div style="font-family:'Kiran',sans-serif; font-size:16px;">`;

            (order.items || []).forEach(item => {
                html += `<div>${item.itemName} x ${item.qty}</div>`;
            });

            html += '</div>';

            if (order.status === 'SENT') {
                html += `<button onclick="updateKotStatus(${order.id}, 'READY')" style="margin-top:6px; background:#4CAF50; color:white; border:none; padding:4px 12px; border-radius:4px; cursor:pointer;">READY</button>`;
            } else if (order.status === 'READY') {
                html += `<button onclick="updateKotStatus(${order.id}, 'SERVE')" style="margin-top:6px; background:#2196F3; color:white; border:none; padding:4px 12px; border-radius:4px; cursor:pointer;">SERVE</button>`;
            }

            html += '</div>';
        });
        html += '</div>';

        Swal.fire({
            title: 'Kitchen Orders - ' + selectedTableName,
            html: html,
            width: 500,
            showConfirmButton: true,
            confirmButtonText: 'Close'
        });
    } catch (e) {
        showError('Failed to load kitchen orders: ' + e.message);
    }
}

async function updateKotStatus(kotId, newStatus) {
    try {
        if (newStatus === 'READY') {
            await apiPut('/billing/kitchen-orders/' + kotId + '/ready', {});
        } else if (newStatus === 'SERVE') {
            await apiPut('/billing/kitchen-orders/' + kotId + '/serve', {});
        }
        // Refresh dialog
        showKitchenStatusDialog();
    } catch (e) {
        showError('Failed to update status: ' + e.message);
    }
}

// ==================== CASH COUNTER / PAYMENT CALCULATION ====================

function setupCashCounter() {
    const txtCashReceived = document.getElementById('txtCashReceived');
    const txtReturn = document.getElementById('txtReturnToCustomer');
    const txtDiscount = document.getElementById('txtDiscount');

    // Numeric validation
    txtCashReceived.addEventListener('input', function () {
        this.value = this.value.replace(/[^\d.]/g, '');
        calculatePayment();
    });

    txtReturn.addEventListener('input', function () {
        this.value = this.value.replace(/[^\d.]/g, '');
        calculatePayment();
    });

    // Discount field: numeric validation + recalculate on change
    if (txtDiscount) {
        txtDiscount.addEventListener('input', function () {
            this.value = this.value.replace(/[^\d.]/g, '');
            calculatePayment();
        });
    }
}

function calculatePayment() {
    const billAmount = parseFloat(document.getElementById('lblBillAmount').textContent.replace(/[^\d.]/g, '')) || 0;
    const manualDiscount = parseFloat(document.getElementById('txtDiscount').value) || 0;
    const cashReceived = parseFloat(document.getElementById('txtCashReceived').value) || 0;
    const returnAmount = parseFloat(document.getElementById('txtReturnToCustomer').value) || 0;

    // Net amount = bill total minus manual discount
    const netAmount = Math.max(0, billAmount - manualDiscount);
    const netReceived = cashReceived - returnAmount;

    let change = 0;
    let due = 0;

    if (selectedCustomer) {
        // CREDIT: due = remaining after partial payment
        change = Math.max(0, netReceived - netAmount);
        due = Math.max(0, netAmount - Math.max(0, netReceived));
    } else {
        // PAID: change back if overpaid, due if underpaid
        change = Math.max(0, netReceived - netAmount);
        due = Math.max(0, netAmount - Math.max(0, netReceived));
    }

    const lblChange = document.getElementById('lblChange');
    lblChange.innerHTML = '&#x20B9;' + change.toFixed(2);
    lblChange.style.color = change > 0 ? '#7B1FA2' : '#9E9E9E';

    document.getElementById('lblNetAmount').innerHTML = '&#x20B9;' + netAmount.toFixed(2);

    // Due/Balance
    const lblBalance = document.getElementById('lblBalance');
    lblBalance.innerHTML = '&#x20B9;' + due.toFixed(2);
    lblBalance.style.color = due > 0 ? '#B71C1C' : '#4CAF50';
}

// ==================== NUMBER PAD ====================

function setupNumberPad() {
    document.querySelectorAll('.numpad-btn').forEach(btn => {
        btn.addEventListener('click', () => handleNumberPadClick(btn.dataset.num));
    });
}

function setupNumericFieldTracking() {
    const numericFields = ['txtQuantity', 'txtPrice', 'txtCashReceived', 'txtReturnToCustomer', 'txtCode', 'txtDiscount'];
    numericFields.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('focus', () => { focusedNumericField = el; });
            el.addEventListener('blur', () => {
                setTimeout(() => {
                    if (focusedNumericField === el) focusedNumericField = null;
                }, 200);
            });
        }
    });
}

function handleNumberPadClick(digit) {
    if (digit === 'C') {
        // Clear: if row selected, deselect; else clear focused field or quantity
        if (selectedTransactionIndex >= 0) {
            selectedTransactionIndex = -1;
            document.querySelectorAll('#transactionBody tr').forEach(r => r.classList.remove('selected'));
        }
        if (focusedNumericField) {
            focusedNumericField.value = '';
            focusedNumericField.dispatchEvent(new Event('input'));
        } else {
            document.getElementById('txtQuantity').value = '';
        }
        return;
    }

    // Decimal point: only append to focused field (don't add qty to row), prevent double dots
    if (digit === '.') {
        const target = focusedNumericField || document.getElementById('txtQuantity');
        if (!target.value.includes('.')) {
            target.value += '.';
            target.dispatchEvent(new Event('input'));
        }
        return;
    }

    // If a transaction row is selected and not in edit mode: ADD digit to quantity (matches desktop)
    // Desktop: newQty = currentQty + digit (arithmetic addition, not string concatenation)
    if (selectedTransactionIndex >= 0 && !isEditMode && !isEditBillMode) {
        const tx = tempTransactionList[selectedTransactionIndex];
        if (tx.isClosed) return;

        const addQty = parseInt(digit);
        const newQty = parseFloat(tx.qty) + addQty;
        tx.qty = newQty;
        tx.amt = newQty * tx.rate;

        // Set flag to suppress form population during update (matches desktop isNumberPadUpdate)
        isNumberPadUpdate = true;

        // Clear input fields after numpad update (matches desktop)
        if (categoryAC) categoryAC.clear();
        document.getElementById('txtCode').value = '';
        if (itemAC) itemAC.clear();
        document.getElementById('txtQuantity').value = '';
        document.getElementById('txtPrice').value = '';
        document.getElementById('txtAmount').value = '';
        selectedTransactionIndex = -1;

        // Update in database, then reload
        apiPut('/billing/transactions/' + tx.id, {
            quantity: newQty,
            rate: tx.rate,
            amount: tx.amt
        }).then(() => {
            loadTransactionsForTable(selectedTableId);
            isNumberPadUpdate = false;
        }).catch(e => {
            console.error('Numpad update failed:', e);
            isNumberPadUpdate = false;
        });

        renderTransactionTable();
        updateTotals();
        return;
    }

    // Append to focused numeric field or default to quantity
    const target = focusedNumericField || document.getElementById('txtQuantity');
    target.value += digit;
    target.dispatchEvent(new Event('input'));
}

// ==================== TABLE STATUS UPDATE ====================

/**
 * Determine table button status from local state (matches desktop logic exactly):
 *   hasTempTransactions → "ongoing" (green)
 *   hasClosedBill       → "closed"  (red)
 *   else                → "available" (white)
 */
function applyTableButtonStatusFromState(tableId) {
    const btn = tableButtonMap[tableId];
    if (!btn) return;

    // Check if the currently loaded table matches - use local state
    if (tableId === selectedTableId) {
        const hasTempItems = tempTransactionList.some(t => !t.isClosed);
        if (hasTempItems) {
            applyTableButtonStatus(btn, 'ongoing');
        } else if (currentClosedBill) {
            applyTableButtonStatus(btn, 'closed');
        } else {
            applyTableButtonStatus(btn, 'available');
        }
        return;
    }

    // For non-selected tables, fetch from API
    updateTableButtonStatusFromAPI(tableId);
}

async function updateTableButtonStatusFromAPI(tableId) {
    try {
        const resp = await apiGet('/billing/tables/' + tableId + '/status');
        const status = resp.data || resp || 'Available';
        const resolvedStatus = typeof status === 'string' ? status : status.status || 'Available';
        const btn = tableButtonMap[tableId];
        if (btn) {
            applyTableButtonStatus(btn, resolvedStatus);
            console.log('Table status updated:', btn.dataset.tableName, '→', resolvedStatus);
        }
    } catch (e) {
        console.error('Failed to update table status:', e);
    }
}

/**
 * Update table button status - uses local state for current table, API for others.
 * Matches desktop BillingController.updateTableButtonStatus() which checks
 * hasTempTransactions and hasClosedBill locally.
 */
function updateTableButtonStatus(tableId) {
    applyTableButtonStatusFromState(tableId);
}

// ==================== BILL HISTORY ====================

function setupBillHistorySearch() {
    document.getElementById('btnSearchBills').addEventListener('click', searchBills);
    document.getElementById('btnClearSearch').addEventListener('click', () => {
        document.getElementById('dpSearchDate').value = new Date().toISOString().split('T')[0];
        document.getElementById('txtSearchBillNo').value = '';
        document.getElementById('txtSearchCustomer').value = '';
        loadTodaysBills();
    });
    document.getElementById('btnRefreshBills').addEventListener('click', loadTodaysBills);
    document.getElementById('btnRefreshTables').addEventListener('click', loadTablesAndSections);
}

async function loadTodaysBills() {
    try {
        const resp = await apiGet('/billing/bills/today');
        const bills = resp.data?.bills || resp.bills || resp.data || [];
        renderBillHistory(Array.isArray(bills) ? bills : []);
    } catch (e) {
        console.error('Failed to load today bills:', e);
        renderBillHistory([]);
    }
}

async function searchBills() {
    const billNo = document.getElementById('txtSearchBillNo').value.trim();
    const dateVal = document.getElementById('dpSearchDate').value;
    const customer = document.getElementById('txtSearchCustomer').value.trim();

    const searchParams = {};
    if (billNo) searchParams.billNo = parseInt(billNo);
    if (dateVal) {
        // Convert yyyy-MM-dd to dd-MM-yyyy
        const parts = dateVal.split('-');
        searchParams.date = parts[2] + '-' + parts[1] + '-' + parts[0];
    }

    try {
        const resp = await apiPost('/billing/bills/search', searchParams);
        let bills = resp.data || resp || [];
        if (!Array.isArray(bills)) bills = [];

        // Client-side filter by customer name if provided
        if (customer && bills.length > 0) {
            const q = customer.toLowerCase();
            bills = bills.filter(b => (b.customerName || '').toLowerCase().includes(q));
        }

        renderBillHistory(bills);
    } catch (e) {
        showError('Search failed: ' + e.message);
    }
}

/**
 * View bill details - load bill items into transaction table (matches desktop viewBillDetails)
 * Called on double-click of bill history row
 */
async function viewBillDetails(billNo) {
    try {
        const resp = await apiGet('/billing/bills/' + billNo);
        const bill = resp.data || resp;
        if (!bill) { showWarning('Bill not found'); return; }

        // Load bill items into transaction table for viewing
        tempTransactionList = [];
        (bill.items || []).forEach(item => {
            tempTransactionList.push({
                id: -(item.id || Math.random() * 10000),
                itemName: item.itemName,
                qty: item.quantity || item.qty,
                rate: item.rate,
                amt: item.amount || item.amt || (item.quantity || item.qty) * item.rate,
                tableNo: bill.tableNo,
                waitorId: bill.waitorId,
                isClosed: true
            });
        });

        currentClosedBill = bill;

        // Set table info
        selectedTableId = bill.tableNo;
        selectedTableName = bill.tableName || '';
        document.getElementById('txtTableNumber').value = bill.tableName || bill.tableNo || '-';

        // Set customer if available
        if (bill.customerId && bill.customerName) {
            displaySelectedCustomer({ id: bill.customerId, firstName: bill.customerName, mobileNo: bill.customerMobile || '' });
        }

        renderTransactionTable();
        updateTotals();
        updateCloseButtonState();
    } catch (e) {
        showError('Failed to load bill details: ' + e.message);
    }
}

function renderBillHistory(bills) {
    const tbody = document.getElementById('billHistoryBody');
    tbody.innerHTML = '';

    if (bills.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#9E9E9E; padding:20px;">No bills found</td></tr>';
        return;
    }

    bills.forEach(bill => {
        const tr = document.createElement('tr');
        tr.dataset.billNo = bill.billNo;

        // Status badge class matching CSS: .status-paid, .status-credit, .status-close
        const statusClass = bill.status === 'PAID' ? 'status-paid' :
            bill.status === 'CREDIT' ? 'status-credit' :
                bill.status === 'CLOSE' ? 'status-close' : '';

        tr.innerHTML = `
            <td>${bill.billNo}</td>
            <td>${formatDate(bill.billDate)}</td>
            <td style="font-family:'Kiran',sans-serif;">${bill.customerName || '-'}</td>
            <td>&#x20B9;${parseFloat(bill.billAmount || bill.netAmount || 0).toFixed(2)}</td>
            <td><span class="status-badge ${statusClass}">${bill.status}</span></td>
        `;

        tr.addEventListener('click', function () {
            document.querySelectorAll('#billHistoryBody tr').forEach(r => r.classList.remove('selected'));
            this.classList.add('selected');
        });

        // Double-click to view bill details (matches desktop viewBillDetails)
        tr.addEventListener('dblclick', function () {
            viewBillDetails(bill.billNo);
        });

        tbody.appendChild(tr);
    });
}
