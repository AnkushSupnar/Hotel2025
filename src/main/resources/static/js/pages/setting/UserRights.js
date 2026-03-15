/**
 * UserRights.js - Web replica of desktop UserRightsController.java
 * Role-based screen permission management with categorized checkboxes.
 */

let userRightsInitialized = false;
let allPermissionsByCategory = {};
let currentRolePermissions = [];

const CATEGORY_ICONS = {
    DASHBOARD: 'bi-speedometer2',
    SALES: 'bi-cart3',
    PURCHASE: 'bi-truck',
    MASTER: 'bi-database',
    REPORTS: 'bi-bar-chart',
    SETTINGS: 'bi-gear',
    EMPLOYEE_SERVICE: 'bi-briefcase'
};

const CATEGORY_COLORS = {
    DASHBOARD: '#1976D2',
    SALES: '#2E7D32',
    PURCHASE: '#E65100',
    MASTER: '#6A1B9A',
    REPORTS: '#00838F',
    SETTINGS: '#5C6BC0',
    EMPLOYEE_SERVICE: '#AD1457'
};

const CATEGORY_DISPLAY = {
    DASHBOARD: 'Dashboard',
    SALES: 'Sales',
    PURCHASE: 'Purchase',
    MASTER: 'Master Data',
    REPORTS: 'Reports',
    SETTINGS: 'Settings',
    EMPLOYEE_SERVICE: 'Employee Service'
};

async function initUserRights() {
    if (!userRightsInitialized) {
        setupUserRightsHandlers();
        userRightsInitialized = true;
    }
    await loadScreenPermissions();
    await loadRoles();
}

function setupUserRightsHandlers() {
    document.getElementById('cmbRole').addEventListener('change', onRoleSelected);
    document.getElementById('btnSavePermissions').addEventListener('click', savePermissions);
    document.getElementById('btnResetPermissions').addEventListener('click', resetPermissions);
    document.getElementById('txtSearchPermission').addEventListener('input', function() {
        searchPermissions(this.value);
    });
}

// ==================== LOAD ROLES ====================

async function loadRoles() {
    try {
        const resp = await apiGet('/roles');
        const roles = resp.data || [];
        const cmb = document.getElementById('cmbRole');
        cmb.innerHTML = '<option value="">Select a role...</option>';
        roles.forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.roleName;
            opt.textContent = r.roleName;
            cmb.appendChild(opt);
        });
    } catch (e) {
        console.error('Failed to load roles:', e);
        showError('Failed to load roles: ' + e.message);
    }
}

// ==================== LOAD SCREEN PERMISSIONS ====================

async function loadScreenPermissions() {
    try {
        const resp = await apiGet('/screen-permissions');
        allPermissionsByCategory = resp.data || {};
        renderPermissionsGrid(allPermissionsByCategory);
    } catch (e) {
        console.error('Failed to load permissions:', e);
        showError('Failed to load permissions: ' + e.message);
    }
}

function renderPermissionsGrid(permsByCategory) {
    const container = document.getElementById('permissionsContainer');
    container.innerHTML = '';

    const categories = Object.keys(permsByCategory);
    if (categories.length === 0) {
        container.innerHTML = '<p class="text-muted py-3">No permissions available</p>';
        return;
    }

    categories.forEach(cat => {
        const perms = permsByCategory[cat];
        if (!perms || perms.length === 0) return;

        const catDiv = document.createElement('div');
        catDiv.className = 'perm-category';
        catDiv.setAttribute('data-category', cat);

        const icon = CATEGORY_ICONS[cat] || 'bi-folder';
        const color = CATEGORY_COLORS[cat] || '#555';
        const displayName = CATEGORY_DISPLAY[cat] || cat;

        // Category header with select-all checkbox
        catDiv.innerHTML = `
            <div class="perm-category-header" onclick="toggleCategorySelectAll('${cat}')">
                <i class="bi ${icon} perm-category-icon" style="color:${color};"></i>
                <span class="perm-category-name">${displayName}</span>
                <span class="perm-category-badge">${perms.length}</span>
                <input type="checkbox" class="cat-select-all" data-category="${cat}"
                       onclick="event.stopPropagation(); onCategorySelectAllChange('${cat}')" />
            </div>
        `;

        // Individual permission items
        perms.forEach(p => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'perm-item';
            itemDiv.setAttribute('data-perm', p.name);
            itemDiv.setAttribute('data-category', cat);
            itemDiv.innerHTML = `
                <input type="checkbox" class="perm-checkbox" data-perm="${p.name}" data-category="${cat}"
                       onchange="onPermissionCheckboxChange('${cat}')" />
                <span class="perm-item-name">${p.displayName}</span>
                <span class="perm-item-desc">${p.description || ''}</span>
            `;
            catDiv.appendChild(itemDiv);
        });

        container.appendChild(catDiv);
    });

    updatePermissionCount();
}

// ==================== ROLE SELECTION ====================

async function onRoleSelected() {
    const roleName = document.getElementById('cmbRole').value;
    if (!roleName) {
        uncheckAllPermissions();
        setPermissionsDisabled(false);
        document.getElementById('lblRoleInfo').textContent = 'Select a role to view its permissions';
        updatePermissionCount();
        return;
    }

    try {
        const resp = await apiGet('/roles/' + roleName + '/permissions');
        currentRolePermissions = resp.data || [];

        // Uncheck all first
        uncheckAllPermissions();

        // Check the ones this role has
        currentRolePermissions.forEach(permName => {
            const cb = document.querySelector(`.perm-checkbox[data-perm="${permName}"]`);
            if (cb) cb.checked = true;
        });

        // Update all category select-all states
        Object.keys(allPermissionsByCategory).forEach(updateCategorySelectAll);

        // ADMIN role: all checked, disabled
        if (roleName === 'ADMIN') {
            checkAllPermissions();
            setPermissionsDisabled(true);
            document.getElementById('lblRoleInfo').textContent =
                'ADMIN has full access to all screens. Permissions cannot be modified.';
        } else {
            setPermissionsDisabled(false);
            const count = currentRolePermissions.length;
            const total = getTotalPermissionCount();
            document.getElementById('lblRoleInfo').textContent =
                `${roleName} role has ${count} of ${total} permissions enabled.`;
        }

        updatePermissionCount();
    } catch (e) {
        console.error('Failed to load role permissions:', e);
        showError('Failed to load permissions: ' + e.message);
    }
}

// ==================== CHECKBOX LOGIC ====================

function onCategorySelectAllChange(category) {
    const selectAll = document.querySelector(`.cat-select-all[data-category="${category}"]`);
    const checkboxes = document.querySelectorAll(`.perm-checkbox[data-category="${category}"]`);
    checkboxes.forEach(cb => { cb.checked = selectAll.checked; });
    updatePermissionCount();
}

function toggleCategorySelectAll(category) {
    const selectAll = document.querySelector(`.cat-select-all[data-category="${category}"]`);
    if (selectAll.disabled) return;
    selectAll.checked = !selectAll.checked;
    onCategorySelectAllChange(category);
}

function onPermissionCheckboxChange(category) {
    updateCategorySelectAll(category);
    updatePermissionCount();
}

function updateCategorySelectAll(category) {
    const selectAll = document.querySelector(`.cat-select-all[data-category="${category}"]`);
    const checkboxes = document.querySelectorAll(`.perm-checkbox[data-category="${category}"]`);
    if (!selectAll || checkboxes.length === 0) return;

    const checkedCount = Array.from(checkboxes).filter(cb => cb.checked).length;
    if (checkedCount === 0) {
        selectAll.checked = false;
        selectAll.indeterminate = false;
    } else if (checkedCount === checkboxes.length) {
        selectAll.checked = true;
        selectAll.indeterminate = false;
    } else {
        selectAll.checked = false;
        selectAll.indeterminate = true;
    }
}

function updatePermissionCount() {
    const allCbs = document.querySelectorAll('.perm-checkbox');
    const checked = Array.from(allCbs).filter(cb => cb.checked).length;
    document.getElementById('lblPermissionCount').textContent = `${checked} of ${allCbs.length} selected`;
}

function uncheckAllPermissions() {
    document.querySelectorAll('.perm-checkbox').forEach(cb => { cb.checked = false; });
    document.querySelectorAll('.cat-select-all').forEach(cb => { cb.checked = false; cb.indeterminate = false; });
}

function checkAllPermissions() {
    document.querySelectorAll('.perm-checkbox').forEach(cb => { cb.checked = true; });
    document.querySelectorAll('.cat-select-all').forEach(cb => { cb.checked = true; cb.indeterminate = false; });
}

function setPermissionsDisabled(disabled) {
    document.querySelectorAll('.perm-checkbox').forEach(cb => { cb.disabled = disabled; });
    document.querySelectorAll('.cat-select-all').forEach(cb => { cb.disabled = disabled; });
}

function getTotalPermissionCount() {
    return document.querySelectorAll('.perm-checkbox').length;
}

// ==================== SEARCH ====================

function searchPermissions(query) {
    const q = (query || '').toLowerCase().trim();
    const noResults = document.getElementById('noResultsContainer');
    let anyVisible = false;

    document.querySelectorAll('.perm-category').forEach(catDiv => {
        let catVisible = false;
        catDiv.querySelectorAll('.perm-item').forEach(item => {
            const name = item.querySelector('.perm-item-name').textContent.toLowerCase();
            const desc = (item.querySelector('.perm-item-desc')?.textContent || '').toLowerCase();
            const match = !q || name.includes(q) || desc.includes(q);
            item.style.display = match ? '' : 'none';
            if (match) catVisible = true;
        });
        catDiv.style.display = catVisible ? '' : 'none';
        if (catVisible) anyVisible = true;
    });

    noResults.style.display = anyVisible ? 'none' : '';
}

// ==================== SAVE / RESET ====================

async function savePermissions() {
    const roleName = document.getElementById('cmbRole').value;
    if (!roleName) {
        showWarning('Please select a role first');
        return;
    }
    if (roleName === 'ADMIN') {
        showWarning('ADMIN permissions cannot be modified');
        return;
    }

    const checkedPerms = Array.from(document.querySelectorAll('.perm-checkbox:checked'))
        .map(cb => cb.getAttribute('data-perm'));

    const btn = document.getElementById('btnSavePermissions');
    btn.classList.add('loading');

    try {
        await apiPut('/roles/' + roleName + '/permissions', checkedPerms);
        showSuccess('Permissions saved successfully for ' + roleName);
    } catch (e) {
        showError('Failed to save permissions: ' + e.message);
    } finally {
        btn.classList.remove('loading');
    }
}

async function resetPermissions() {
    const roleName = document.getElementById('cmbRole').value;
    if (!roleName) {
        showWarning('Please select a role first');
        return;
    }

    const confirmed = await showConfirm('Reset permissions for ' + roleName + ' to defaults?');
    if (!confirmed) return;

    await onRoleSelected();
    showSuccess('Permissions reloaded from server');
}
