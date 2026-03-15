/**
 * ShopDetails.js - Web replica of desktop ShopDetailsController.java
 * Manages restaurant/shop CRUD with split layout: form (left) + table (right)
 */

let shopDetailsInitialized = false;
let currentEditingShopId = null;
let allShops = [];

async function initShopDetails() {
    if (!shopDetailsInitialized) {
        setupShopEventHandlers();
        shopDetailsInitialized = true;
    }
    await loadShops();
}

function setupShopEventHandlers() {
    document.getElementById('btnSaveShop').addEventListener('click', saveShop);
    document.getElementById('btnUpdateShop').addEventListener('click', updateShop);
    document.getElementById('btnClearShop').addEventListener('click', clearShopForm);
    document.getElementById('btnRefreshShops').addEventListener('click', loadShops);

    document.getElementById('txtShopSearch').addEventListener('input', function() {
        searchShops(this.value);
    });
}

// ==================== LOAD SHOPS ====================

async function loadShops() {
    try {
        const resp = await apiGet('/shops');
        allShops = resp.data || [];
        renderShopTable(allShops);
    } catch (e) {
        console.error('Failed to load shops:', e);
        showError('Failed to load restaurants: ' + e.message);
    }
}

function searchShops(query) {
    if (!query || !query.trim()) {
        renderShopTable(allShops);
        return;
    }
    const q = query.toLowerCase().trim();
    const filtered = allShops.filter(s =>
        (s.restaurantName && s.restaurantName.toLowerCase().includes(q)) ||
        (s.contactNumber && s.contactNumber.includes(q)) ||
        (s.ownerName && s.ownerName.toLowerCase().includes(q))
    );
    renderShopTable(filtered);
}

function renderShopTable(shops) {
    const tbody = document.getElementById('shopTableBody');
    if (!shops || shops.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No restaurants found</td></tr>';
        return;
    }

    tbody.innerHTML = shops.map(s => `
        <tr onclick="editShopFromRow(${s.shopId})" title="Click to edit">
            <td>${s.shopId}</td>
            <td><strong>${escapeHtml(s.restaurantName || '')}</strong></td>
            <td>${escapeHtml(s.ownerName || '')}</td>
            <td>${escapeHtml(s.contactNumber || '')}</td>
            <td>${escapeHtml(s.address || '')}</td>
            <td>${escapeHtml(s.gstinNumber || '')}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-edit" onclick="event.stopPropagation(); editShopFromRow(${s.shopId})" title="Edit">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn-delete" onclick="event.stopPropagation(); deleteShop(${s.shopId})" title="Delete">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// ==================== SAVE / UPDATE ====================

async function saveShop() {
    if (!validateShopForm()) return;

    const shopData = getShopFormData();
    const btn = document.getElementById('btnSaveShop');
    btn.classList.add('loading');

    try {
        await apiPost('/shops', shopData);
        showSuccess('Restaurant saved successfully');
        clearShopForm();
        await loadShops();
    } catch (e) {
        showError('Failed to save: ' + e.message);
    } finally {
        btn.classList.remove('loading');
    }
}

async function updateShop() {
    if (!currentEditingShopId) return;
    if (!validateShopForm()) return;

    const shopData = getShopFormData();
    const btn = document.getElementById('btnUpdateShop');
    btn.classList.add('loading');

    try {
        await apiPut('/shops/' + currentEditingShopId, shopData);
        showSuccess('Restaurant updated successfully');
        clearShopForm();
        await loadShops();
    } catch (e) {
        showError('Failed to update: ' + e.message);
    } finally {
        btn.classList.remove('loading');
    }
}

// ==================== DELETE ====================

async function deleteShop(shopId) {
    const confirmed = await showConfirm('Are you sure you want to delete this restaurant?');
    if (!confirmed) return;

    try {
        await apiDelete('/shops/' + shopId);
        showSuccess('Restaurant deleted successfully');
        if (currentEditingShopId === shopId) {
            clearShopForm();
        }
        await loadShops();
    } catch (e) {
        showError('Failed to delete: ' + e.message);
    }
}

// ==================== EDIT ====================

function editShopFromRow(shopId) {
    const shop = allShops.find(s => s.shopId === shopId);
    if (!shop) return;

    currentEditingShopId = shopId;

    document.getElementById('txtRestaurantName').value = shop.restaurantName || '';
    document.getElementById('txtSubTitle').value = shop.subTitle || '';
    document.getElementById('txtOwnerName').value = shop.ownerName || '';
    document.getElementById('txtAddress').value = shop.address || '';
    document.getElementById('txtContactNumber').value = shop.contactNumber || '';
    document.getElementById('txtContactNumber2').value = shop.contactNumber2 || '';
    document.getElementById('txtGstinNumber').value = shop.gstinNumber || '';
    document.getElementById('txtLicenseKey').value = shop.licenseKey || '';

    document.getElementById('btnSaveShop').style.display = 'none';
    document.getElementById('btnUpdateShop').style.display = '';
    document.getElementById('shopFormTitle').textContent = 'Edit Restaurant';
}

// ==================== FORM HELPERS ====================

function clearShopForm() {
    currentEditingShopId = null;
    document.getElementById('txtRestaurantName').value = '';
    document.getElementById('txtSubTitle').value = '';
    document.getElementById('txtOwnerName').value = '';
    document.getElementById('txtAddress').value = '';
    document.getElementById('txtContactNumber').value = '';
    document.getElementById('txtContactNumber2').value = '';
    document.getElementById('txtGstinNumber').value = '';
    document.getElementById('txtLicenseKey').value = '';

    document.getElementById('btnSaveShop').style.display = '';
    document.getElementById('btnUpdateShop').style.display = 'none';
    document.getElementById('shopFormTitle').textContent = 'Add New Restaurant';
}

function getShopFormData() {
    return {
        restaurantName: document.getElementById('txtRestaurantName').value.trim(),
        subTitle: document.getElementById('txtSubTitle').value.trim(),
        ownerName: document.getElementById('txtOwnerName').value.trim(),
        address: document.getElementById('txtAddress').value.trim(),
        contactNumber: document.getElementById('txtContactNumber').value.trim(),
        contactNumber2: document.getElementById('txtContactNumber2').value.trim(),
        gstinNumber: document.getElementById('txtGstinNumber').value.trim(),
        licenseKey: document.getElementById('txtLicenseKey').value.trim()
    };
}

function validateShopForm() {
    const name = document.getElementById('txtRestaurantName').value.trim();
    const owner = document.getElementById('txtOwnerName').value.trim();
    const address = document.getElementById('txtAddress').value.trim();
    const contact = document.getElementById('txtContactNumber').value.trim();

    if (!name) { showWarning('Restaurant Name is required'); return false; }
    if (!owner) { showWarning('Owner Name is required'); return false; }
    if (!address) { showWarning('Address is required'); return false; }
    if (!contact) { showWarning('Contact Number is required'); return false; }
    return true;
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
