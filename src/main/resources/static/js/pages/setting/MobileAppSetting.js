/**
 * MobileAppSetting.js - Web replica of desktop MobileAppSettingController.java
 * Manages mobile app configuration: JWT, feature access, and general settings.
 */

let mobileSettingsInitialized = false;
let jwtSecretVisible = false;
let jwtSecretValue = '';
let currentMobileRole = '';

const MOBILE_SETTING_KEYS = {
    MOBILE_ACCESS_ENABLED: 'MOBILE_ACCESS_ENABLED',
    JWT_TOKEN_EXPIRY_DAYS: 'JWT_TOKEN_EXPIRY_DAYS',
    JWT_TOKEN_EXPIRY_HOURS: 'JWT_TOKEN_EXPIRY_HOURS',
    JWT_SECRET_KEY: 'JWT_SECRET_KEY',
    MOBILE_APP_VERSION: 'MOBILE_APP_VERSION',
    FORCE_UPDATE_ENABLED: 'FORCE_UPDATE_ENABLED'
};

const MOBILE_ROLES = ['ADMIN', 'MANAGER', 'CASHIER', 'CAPTAIN', 'WAITER'];

async function initMobileSettings() {
    if (!mobileSettingsInitialized) {
        setupMobileHandlers();
        populateMobileRoles();
        mobileSettingsInitialized = true;
    }
    await loadMobileSettings();
}

function setupMobileHandlers() {
    document.getElementById('btnSaveMobile').addEventListener('click', saveMobileSettings);
    document.getElementById('btnResetMobile').addEventListener('click', resetMobileForm);
    document.getElementById('btnRegenerateSecret').addEventListener('click', regenerateSecret);
    document.getElementById('btnToggleSecret').addEventListener('click', toggleSecretVisibility);
    document.getElementById('cmbMobileRole').addEventListener('change', onMobileRoleChange);
}

function populateMobileRoles() {
    const cmb = document.getElementById('cmbMobileRole');
    cmb.innerHTML = '<option value="">-- Select Role --</option>';
    MOBILE_ROLES.forEach(role => {
        const opt = document.createElement('option');
        opt.value = role;
        opt.textContent = role;
        cmb.appendChild(opt);
    });
}

// ==================== LOAD SETTINGS ====================

async function loadMobileSettings() {
    try {
        const resp = await apiGet('/mobile-settings');
        const settings = resp.data || [];

        const settingsMap = {};
        settings.forEach(s => { settingsMap[s.settingKey] = s.settingValue; });

        // Populate form
        document.getElementById('chkMobileAccessEnabled').checked =
            (settingsMap[MOBILE_SETTING_KEYS.MOBILE_ACCESS_ENABLED] === 'true');

        document.getElementById('inputExpiryDays').value =
            settingsMap[MOBILE_SETTING_KEYS.JWT_TOKEN_EXPIRY_DAYS] || '7';

        document.getElementById('inputExpiryHours').value =
            settingsMap[MOBILE_SETTING_KEYS.JWT_TOKEN_EXPIRY_HOURS] || '0';

        document.getElementById('txtMinAppVersion').value =
            settingsMap[MOBILE_SETTING_KEYS.MOBILE_APP_VERSION] || '';

        document.getElementById('chkForceUpdate').checked =
            (settingsMap[MOBILE_SETTING_KEYS.FORCE_UPDATE_ENABLED] === 'true');

        // JWT Secret
        jwtSecretValue = settingsMap[MOBILE_SETTING_KEYS.JWT_SECRET_KEY] || '';
        jwtSecretVisible = false;
        updateSecretDisplay();

    } catch (e) {
        console.error('Failed to load mobile settings:', e);
        showError('Failed to load mobile settings: ' + e.message);
    }
}

// ==================== JWT SECRET ====================

function updateSecretDisplay() {
    const lbl = document.getElementById('lblJwtSecret');
    const btn = document.getElementById('btnToggleSecret');
    if (jwtSecretVisible && jwtSecretValue) {
        lbl.textContent = jwtSecretValue;
        btn.innerHTML = '<i class="bi bi-eye-slash"></i>';
    } else {
        lbl.textContent = '\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022';
        btn.innerHTML = '<i class="bi bi-eye"></i>';
    }
}

function toggleSecretVisibility() {
    jwtSecretVisible = !jwtSecretVisible;
    updateSecretDisplay();
}

async function regenerateSecret() {
    const confirmed = await showConfirm(
        'Regenerating the JWT secret will invalidate ALL existing mobile tokens. Users will need to login again. Continue?'
    );
    if (!confirmed) return;

    try {
        const resp = await apiPost('/mobile-settings/regenerate-secret', {});
        jwtSecretValue = resp.data || '';
        jwtSecretVisible = true;
        updateSecretDisplay();
        showSuccess('JWT secret regenerated successfully');
    } catch (e) {
        showError('Failed to regenerate secret: ' + e.message);
    }
}

// ==================== FEATURE ACCESS ====================

async function onMobileRoleChange() {
    const role = document.getElementById('cmbMobileRole').value;
    currentMobileRole = role;
    if (!role) {
        document.getElementById('mobileFeatureContainer').innerHTML =
            '<p class="text-muted" style="padding:15px 0;">Select a role to view feature access</p>';
        return;
    }
    await loadFeatureAccess(role);
}

async function loadFeatureAccess(role) {
    try {
        const resp = await apiGet('/mobile-settings/features/' + role);
        const features = resp.data || [];
        renderFeatureCategories(features);
    } catch (e) {
        console.error('Failed to load feature access:', e);
        showError('Failed to load feature access: ' + e.message);
    }
}

function renderFeatureCategories(features) {
    const container = document.getElementById('mobileFeatureContainer');
    container.innerHTML = '';

    // Group by category
    const byCategory = {};
    features.forEach(f => {
        const cat = f.category || 'Other';
        if (!byCategory[cat]) byCategory[cat] = [];
        byCategory[cat].push(f);
    });

    const categoryOrder = ['Main Menu', 'Settings', 'Sub-Screen', 'Other'];

    categoryOrder.forEach(cat => {
        const catFeatures = byCategory[cat];
        if (!catFeatures || catFeatures.length === 0) return;

        const catDiv = document.createElement('div');
        catDiv.className = 'mobile-feature-category';

        catDiv.innerHTML = `<p class="mobile-feature-category-title">${escapeHtmlMobile(cat)}</p>`;

        catFeatures.forEach(f => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'mobile-feature-item';
            itemDiv.innerHTML = `
                <input type="checkbox" class="mobile-feature-cb" data-feature="${f.featureCode}"
                       ${f.isEnabled ? 'checked' : ''} />
                <label>${escapeHtmlMobile(f.featureName)}</label>
            `;
            catDiv.appendChild(itemDiv);
        });

        container.appendChild(catDiv);
    });

    if (container.children.length === 0) {
        container.innerHTML = '<p class="text-muted" style="padding:15px 0;">No features defined</p>';
    }
}

// ==================== SAVE ====================

async function saveMobileSettings() {
    const btn = document.getElementById('btnSaveMobile');
    btn.classList.add('loading');

    try {
        // Save general settings
        const generalSettings = {};
        generalSettings[MOBILE_SETTING_KEYS.MOBILE_ACCESS_ENABLED] =
            String(document.getElementById('chkMobileAccessEnabled').checked);
        generalSettings[MOBILE_SETTING_KEYS.JWT_TOKEN_EXPIRY_DAYS] =
            document.getElementById('inputExpiryDays').value || '7';
        generalSettings[MOBILE_SETTING_KEYS.JWT_TOKEN_EXPIRY_HOURS] =
            document.getElementById('inputExpiryHours').value || '0';
        generalSettings[MOBILE_SETTING_KEYS.MOBILE_APP_VERSION] =
            document.getElementById('txtMinAppVersion').value.trim();
        generalSettings[MOBILE_SETTING_KEYS.FORCE_UPDATE_ENABLED] =
            String(document.getElementById('chkForceUpdate').checked);

        await apiPost('/mobile-settings', generalSettings);

        // Save feature access for current role if one is selected
        if (currentMobileRole) {
            const featureMap = {};
            document.querySelectorAll('.mobile-feature-cb').forEach(cb => {
                featureMap[cb.getAttribute('data-feature')] = cb.checked;
            });
            await apiPost('/mobile-settings/features/' + currentMobileRole, featureMap);
        }

        showSuccess('Mobile settings saved successfully');
    } catch (e) {
        showError('Failed to save: ' + e.message);
    } finally {
        btn.classList.remove('loading');
    }
}

async function resetMobileForm() {
    const confirmed = await showConfirm('Reset all fields to server state?');
    if (!confirmed) return;
    await loadMobileSettings();
    if (currentMobileRole) {
        await loadFeatureAccess(currentMobileRole);
    }
    showSuccess('Form reset');
}

function escapeHtmlMobile(str) {
    const div = document.createElement('div');
    div.textContent = str || '';
    return div.innerHTML;
}
