/**
 * receive-payment.js - Web replica of desktop ReceivePaymentController.java
 * Handles receiving payments from customers for credit sales bills.
 */

document.addEventListener('DOMContentLoaded', function () {
    if (!requireAuth()) return;
    initializeHeader();
    initReceivePayment();
});

// ==================== STATE ====================

let allCustomers = [];
let allBanks = [];
let selectedCustomer = null;
let pendingBills = [];
let selectedBillRows = new Set(); // Set of billNo
let lastReceiptNo = null;
let allHistoryData = [];
let selectedHistoryReceiptNo = null;

// ==================== INIT ====================

async function initReceivePayment() {
    setupEventHandlers();
    setupHistoryFilters();
    await loadMasterData();
    loadReceiptHistory();
    loadSummary();
}

function setupEventHandlers() {
    // Customer autocomplete
    document.getElementById('txtCustomer').addEventListener('input', onCustomerInput);
    document.getElementById('txtCustomer').addEventListener('focus', onCustomerInput);
    document.getElementById('txtCustomer').addEventListener('blur', function () {
        setTimeout(() => {
            document.getElementById('customerAutocomplete').style.display = 'none';
        }, 200);
    });

    // Select all checkbox
    document.getElementById('chkSelectAll').addEventListener('change', onSelectAll);

    // Action buttons
    document.getElementById('btnReceive').addEventListener('click', processPayment);
    document.getElementById('btnClear').addEventListener('click', clearPaymentFields);
    document.getElementById('btnNew').addEventListener('click', clearAll);
    document.getElementById('btnRefreshAll').addEventListener('click', refreshAll);

    // History buttons
    document.getElementById('btnRefreshHistory').addEventListener('click', loadReceiptHistory);
    document.getElementById('btnSearchHistory').addEventListener('click', searchHistory);
    document.getElementById('btnClearSearch').addEventListener('click', clearHistorySearch);
    document.getElementById('btnPrintReceipt').addEventListener('click', printSelectedOrLastReceipt);
}

function setupHistoryFilters() {
    const today = new Date();
    const firstOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);

    document.getElementById('dpHistoryFrom').value = formatDateForInput(firstOfMonth);
    document.getElementById('dpHistoryTo').value = formatDateForInput(today);
}

// ==================== LOAD MASTER DATA ====================

async function loadMasterData() {
    try {
        const [custResp, bankResp] = await Promise.all([
            apiGet('/master/customers'),
            apiGet('/master/banks')
        ]);

        allCustomers = custResp.data || custResp || [];
        allBanks = bankResp.data || bankResp || [];

        populatePaymentModes();
    } catch (e) {
        console.error('Failed to load master data:', e);
        showError('Failed to load data: ' + e.message);
    }
}

function populatePaymentModes() {
    const cmb = document.getElementById('cmbPaymentMode');
    cmb.innerHTML = '<option value="">-- Select Bank --</option>';

    allBanks.forEach(bank => {
        const opt = document.createElement('option');
        opt.value = bank.id;
        opt.textContent = bank.bankName || bank.name;
        opt.dataset.bankName = bank.bankName || bank.name;
        cmb.appendChild(opt);
    });

    // Auto-select first if available
    if (allBanks.length > 0) {
        cmb.selectedIndex = 1;
    }
}

// ==================== CUSTOMER AUTOCOMPLETE ====================

function onCustomerInput() {
    const input = document.getElementById('txtCustomer');
    const query = input.value.trim().toLowerCase();
    const listEl = document.getElementById('customerAutocomplete');

    if (query.length < 1) {
        listEl.style.display = 'none';
        return;
    }

    const filtered = allCustomers.filter(c => {
        const name = (c.fullName || c.firstName || '').toLowerCase();
        const mobile = (c.mobileNo || '').toLowerCase();
        return name.includes(query) || mobile.includes(query);
    }).slice(0, 15);

    if (filtered.length === 0) {
        listEl.style.display = 'none';
        return;
    }

    listEl.innerHTML = '';
    filtered.forEach(c => {
        const div = document.createElement('div');
        div.className = 'rp-autocomplete-item';
        div.textContent = c.fullName || (c.firstName + ' ' + (c.lastName || ''));
        div.addEventListener('mousedown', function (e) {
            e.preventDefault();
            selectCustomer(c);
        });
        listEl.appendChild(div);
    });
    listEl.style.display = 'block';
}

function selectCustomer(customer) {
    selectedCustomer = customer;
    const name = customer.fullName || (customer.firstName + ' ' + (customer.lastName || ''));
    document.getElementById('txtCustomer').value = name;
    document.getElementById('customerAutocomplete').style.display = 'none';

    loadPendingBills(customer.id);
}

// ==================== PENDING BILLS ====================

async function loadPendingBills(customerId) {
    try {
        const resp = await apiGet('/receive-payment/pending-bills/' + customerId);
        const data = resp.data || {};
        pendingBills = data.bills || [];
        const totalPending = data.totalPending || 0;

        document.getElementById('lblTotalPending').textContent = 'Rs. ' + totalPending.toFixed(2);

        renderPendingBills();
        clearPaymentFields();
    } catch (e) {
        console.error('Failed to load pending bills:', e);
        showError('Failed to load bills: ' + e.message);
    }
}

function renderPendingBills() {
    const tbody = document.getElementById('pendingBillsBody');

    if (pendingBills.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="rp-empty-state">No pending bills for this customer</td></tr>';
        document.getElementById('chkSelectAll').checked = false;
        return;
    }

    tbody.innerHTML = '';
    pendingBills.forEach(bill => {
        const tr = document.createElement('tr');
        tr.dataset.billNo = bill.billNo;

        const isSelected = selectedBillRows.has(bill.billNo);
        if (isSelected) tr.classList.add('selected');

        const statusClass = bill.status === 'CREDIT' ? 'status-credit' :
                           bill.status === 'PAID' ? 'status-paid' : 'status-partial';

        tr.innerHTML =
            '<td><input type="checkbox" class="bill-checkbox" data-bill-no="' + bill.billNo + '"' +
            (isSelected ? ' checked' : '') + ' /></td>' +
            '<td>' + bill.billNo + '</td>' +
            '<td>' + bill.billDate + '</td>' +
            '<td>' + bill.billTime + '</td>' +
            '<td>Rs. ' + Number(bill.netAmount).toFixed(2) + '</td>' +
            '<td>Rs. ' + Number(bill.paidAmount).toFixed(2) + '</td>' +
            '<td><strong>Rs. ' + Number(bill.balanceAmount).toFixed(2) + '</strong></td>' +
            '<td class="' + statusClass + '">' + bill.status + '</td>';

        // Row click toggles selection
        tr.addEventListener('click', function (e) {
            if (e.target.type === 'checkbox') return;
            const chk = tr.querySelector('.bill-checkbox');
            chk.checked = !chk.checked;
            onBillCheckChanged(bill.billNo, chk.checked);
        });

        // Checkbox change
        tr.querySelector('.bill-checkbox').addEventListener('change', function () {
            onBillCheckChanged(bill.billNo, this.checked);
        });

        tbody.appendChild(tr);
    });

    updateSelectAllCheckbox();
}

function onSelectAll() {
    const checked = document.getElementById('chkSelectAll').checked;
    selectedBillRows.clear();
    if (checked) {
        pendingBills.forEach(b => selectedBillRows.add(b.billNo));
    }
    renderPendingBills();
    updateSelectedBillsInfo();
}

function onBillCheckChanged(billNo, checked) {
    if (checked) {
        selectedBillRows.add(billNo);
    } else {
        selectedBillRows.delete(billNo);
    }

    // Update row highlight
    const rows = document.querySelectorAll('#pendingBillsBody tr');
    rows.forEach(tr => {
        const bn = parseInt(tr.dataset.billNo);
        if (bn === billNo) {
            tr.classList.toggle('selected', checked);
        }
    });

    updateSelectAllCheckbox();
    updateSelectedBillsInfo();
}

function updateSelectAllCheckbox() {
    const chkAll = document.getElementById('chkSelectAll');
    if (pendingBills.length === 0) {
        chkAll.checked = false;
    } else {
        chkAll.checked = selectedBillRows.size === pendingBills.length;
    }
}

function updateSelectedBillsInfo() {
    const infoEl = document.getElementById('selectedBillsInfo');
    const selectedBills = pendingBills.filter(b => selectedBillRows.has(b.billNo));

    if (selectedBills.length === 0) {
        infoEl.classList.remove('visible');
        document.getElementById('lblSelectedBillNo').textContent = '0 bills';
        document.getElementById('lblSelectedBillAmount').textContent = 'Rs. 0.00';
        document.getElementById('lblSelectedBalance').textContent = 'Rs. 0.00';
        document.getElementById('txtPaymentAmount').value = '';
        return;
    }

    infoEl.classList.add('visible');

    let totalNet = 0, totalBalance = 0;
    selectedBills.forEach(b => {
        totalNet += Number(b.netAmount);
        totalBalance += Number(b.balanceAmount);
    });

    if (selectedBills.length === 1) {
        document.getElementById('lblSelectedBillNo').textContent = '#' + selectedBills[0].billNo;
    } else {
        document.getElementById('lblSelectedBillNo').textContent = selectedBills.length + ' bills';
    }
    document.getElementById('lblSelectedBillAmount').textContent = 'Rs. ' + totalNet.toFixed(2);
    document.getElementById('lblSelectedBalance').textContent = 'Rs. ' + totalBalance.toFixed(2);

    // Auto-fill payment amount with total balance
    document.getElementById('txtPaymentAmount').value = totalBalance.toFixed(2);
}

// ==================== PROCESS PAYMENT ====================

async function processPayment() {
    // Validations
    if (!selectedCustomer) {
        showWarning('Please select a customer first');
        document.getElementById('txtCustomer').focus();
        return;
    }

    const selectedBills = pendingBills.filter(b => selectedBillRows.has(b.billNo));
    if (selectedBills.length === 0) {
        showWarning('Please select one or more bills to receive payment for');
        return;
    }

    const amountStr = document.getElementById('txtPaymentAmount').value.trim();
    const paymentAmount = parseFloat(amountStr);
    if (isNaN(paymentAmount) || paymentAmount <= 0) {
        showWarning('Please enter a valid payment amount');
        document.getElementById('txtPaymentAmount').focus();
        return;
    }

    let totalBalance = 0;
    selectedBills.forEach(b => { totalBalance += Number(b.balanceAmount); });

    if (paymentAmount > totalBalance + 0.01) {
        showError('Payment amount (Rs. ' + paymentAmount.toFixed(2) +
                  ') exceeds total balance (Rs. ' + totalBalance.toFixed(2) + ')');
        document.getElementById('txtPaymentAmount').focus();
        return;
    }

    const cmbMode = document.getElementById('cmbPaymentMode');
    const bankId = parseInt(cmbMode.value);
    if (!bankId) {
        showWarning('Please select a payment mode');
        cmbMode.focus();
        return;
    }
    const paymentMode = cmbMode.options[cmbMode.selectedIndex].dataset.bankName || '';

    // Build allocations (allocate amount across bills in order)
    const allocations = [];
    let remaining = paymentAmount;
    for (const bill of selectedBills) {
        if (remaining <= 0) break;
        const billBalance = Number(bill.balanceAmount);
        const amountToReceive = Math.min(remaining, billBalance);
        allocations.push({ billNo: bill.billNo, amount: Math.round(amountToReceive * 100) / 100 });
        remaining -= amountToReceive;
    }

    const btnReceive = document.getElementById('btnReceive');
    btnReceive.disabled = true;
    btnReceive.classList.add('loading');

    try {
        const resp = await apiPost('/receive-payment/receive', {
            customerId: selectedCustomer.id,
            totalAmount: paymentAmount,
            bankId: bankId,
            paymentMode: paymentMode,
            chequeNo: document.getElementById('txtChequeNo').value.trim(),
            referenceNo: document.getElementById('txtReferenceNo').value.trim(),
            remarks: document.getElementById('txtRemarks').value.trim(),
            allocations: allocations
        });

        const result = resp.data || {};
        lastReceiptNo = result.receiptNo;

        // Refresh data
        await loadPendingBills(selectedCustomer.id);
        loadReceiptHistory();
        loadSummary();
        clearPaymentFields();

        // Show success and ask to print
        askToPrintReceipt(result);

    } catch (e) {
        showError('Payment failed: ' + e.message);
    } finally {
        btnReceive.disabled = false;
        btnReceive.classList.remove('loading');
    }
}

function askToPrintReceipt(result) {
    let billDetails = '';
    if (result.billPayments && result.billPayments.length > 0) {
        result.billPayments.forEach(bp => {
            billDetails += 'Bill #' + bp.billNo + ': Rs. ' + Number(bp.amount).toFixed(2) + '\n';
        });
    }

    Swal.fire({
        icon: 'success',
        title: 'Payment Receipt #' + result.receiptNo,
        html: '<div style="text-align:left;font-size:14px;">' +
              '<p><strong>Total Amount:</strong> Rs. ' + Number(result.totalAmount).toFixed(2) + '</p>' +
              '<p><strong>Bills Paid:</strong> ' + result.billsCount + '</p>' +
              '<p><strong>Payment Mode:</strong> ' + (result.paymentMode || '') + '</p>' +
              '<hr/><p>Do you want to download the receipt?</p></div>',
        showCancelButton: true,
        confirmButtonText: 'Yes, Download',
        cancelButtonText: 'No, Later',
        confirmButtonColor: '#009688'
    }).then((res) => {
        if (res.isConfirmed) {
            downloadReceiptPdf(result.receiptNo);
        }
    });
}

// ==================== RECEIPT PDF ====================

async function downloadReceiptPdf(receiptNo) {
    try {
        const resp = await apiGet('/receive-payment/receipt/' + receiptNo + '/pdf');
        const data = resp.data || {};
        if (data.pdfBase64) {
            downloadPdfFromBase64(data.pdfBase64, 'ReceivePayment.pdf');
            showSuccess('Receipt downloaded');
        } else {
            showError('Failed to generate receipt PDF');
        }
    } catch (e) {
        showError('Failed to download receipt: ' + e.message);
    }
}

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

// ==================== RECEIPT HISTORY ====================

async function loadReceiptHistory() {
    try {
        const from = document.getElementById('dpHistoryFrom').value;
        const to = document.getElementById('dpHistoryTo').value;

        if (!from || !to) return;

        const resp = await apiGet('/receive-payment/history?from=' + from + '&to=' + to);
        allHistoryData = resp.data || [];
        renderHistoryTable(allHistoryData);
    } catch (e) {
        console.error('Failed to load receipt history:', e);
    }
}

function renderHistoryTable(data) {
    const tbody = document.getElementById('historyBody');

    if (data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="rp-empty-state">No receipts found</td></tr>';
        return;
    }

    tbody.innerHTML = '';
    data.forEach(row => {
        const tr = document.createElement('tr');
        if (row.receiptNo === selectedHistoryReceiptNo) tr.classList.add('selected');

        tr.innerHTML =
            '<td>' + row.receiptNo + '</td>' +
            '<td>' + (row.paymentDate || '') + '</td>' +
            '<td>' + (row.billsCount || 1) + '</td>' +
            '<td class="kiran-cell">' + (row.customerName || '') + '</td>' +
            '<td><strong>Rs. ' + Number(row.totalAmount).toFixed(2) + '</strong></td>' +
            '<td class="kiran-cell">' + (row.paymentMode || '') + '</td>';

        tr.addEventListener('click', function () {
            // Deselect previous
            document.querySelectorAll('#historyBody tr.selected').forEach(r => r.classList.remove('selected'));
            tr.classList.add('selected');
            selectedHistoryReceiptNo = row.receiptNo;
        });

        // Double-click to download receipt
        tr.addEventListener('dblclick', function () {
            downloadReceiptPdf(row.receiptNo);
        });

        tbody.appendChild(tr);
    });
}

function searchHistory() {
    const searchText = document.getElementById('txtHistorySearch').value.trim().toLowerCase();

    if (!searchText) {
        loadReceiptHistory();
        return;
    }

    const filtered = allHistoryData.filter(row =>
        (row.customerName || '').toLowerCase().includes(searchText)
    );
    renderHistoryTable(filtered);
}

function clearHistorySearch() {
    document.getElementById('txtHistorySearch').value = '';
    setupHistoryFilters();
    loadReceiptHistory();
}

// ==================== SUMMARY ====================

async function loadSummary() {
    try {
        const resp = await apiGet('/receive-payment/summary');
        const data = resp.data || {};
        document.getElementById('lblTodayReceipts').textContent =
            'Rs. ' + (data.todayTotal || 0).toFixed(2);
        document.getElementById('lblMonthReceipts').textContent =
            'Rs. ' + (data.monthTotal || 0).toFixed(2);
    } catch (e) {
        console.error('Failed to load summary:', e);
    }
}

// ==================== PRINT ====================

function printSelectedOrLastReceipt() {
    if (selectedHistoryReceiptNo) {
        downloadReceiptPdf(selectedHistoryReceiptNo);
    } else if (lastReceiptNo) {
        downloadReceiptPdf(lastReceiptNo);
    } else {
        showWarning('Please select a receipt from history or receive a new payment first.');
    }
}

// ==================== CLEAR / RESET ====================

function clearPaymentFields() {
    selectedBillRows.clear();
    document.getElementById('selectedBillsInfo').classList.remove('visible');
    document.getElementById('lblSelectedBillNo').textContent = '0 bills';
    document.getElementById('lblSelectedBillAmount').textContent = 'Rs. 0.00';
    document.getElementById('lblSelectedBalance').textContent = 'Rs. 0.00';
    document.getElementById('txtPaymentAmount').value = '';
    document.getElementById('txtChequeNo').value = '';
    document.getElementById('txtReferenceNo').value = '';
    document.getElementById('txtRemarks').value = '';
    document.getElementById('chkSelectAll').checked = false;

    // Re-render to clear checkboxes
    if (pendingBills.length > 0) {
        renderPendingBills();
    }
}

function clearAll() {
    selectedCustomer = null;
    document.getElementById('txtCustomer').value = '';
    pendingBills = [];
    document.getElementById('lblTotalPending').textContent = 'Rs. 0.00';
    document.getElementById('pendingBillsBody').innerHTML =
        '<tr><td colspan="8" class="rp-empty-state">Select a customer to view pending bills</td></tr>';
    clearPaymentFields();
    document.getElementById('txtCustomer').focus();
}

async function refreshAll() {
    await loadMasterData();
    if (selectedCustomer) {
        await loadPendingBills(selectedCustomer.id);
    }
    loadReceiptHistory();
    loadSummary();
    showSuccess('Data refreshed');
}

// ==================== HELPERS ====================

function formatDateForInput(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return y + '-' + m + '-' + d;
}
