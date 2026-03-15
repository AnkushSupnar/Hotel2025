/**
 * ApplicationSetting.js - Web replica of desktop ApplicationSettingController.java
 * Handles loading, displaying, and saving application settings.
 *
 * Storage strategy (matches desktop behavior):
 * - Global settings (document_directory, input_font_path, bill_logo_image, use_bill_logo)
 *   → saved to server (database)
 * - Machine-specific settings (billing_printer, kot_printer, default_billing_bank)
 *   → saved to client-side localStorage, keyed by document_directory path
 *   (browser equivalent of desktop's ApplicationSetting.properties file)
 */

// When loaded as standalone page, auto-init.
// When loaded inside SettingMenu.html, initSettings() is called manually on card click.
if (document.getElementById('settings-standalone-page')) {
    document.addEventListener('DOMContentLoaded', function () {
        if (!requireAuth()) return;
        initSettings();
    });
}

// Setting keys (matches desktop constants)
const SETTING_KEYS = {
    DOCUMENT_DIRECTORY: 'document_directory',
    FONT_PATH: 'input_font_path',
    BILLING_PRINTER: 'billing_printer',
    KOT_PRINTER: 'kot_printer',
    DEFAULT_BANK: 'default_billing_bank',
    BILL_LOGO: 'bill_logo_image',
    USE_BILL_LOGO: 'use_bill_logo'
};

// Keys stored in client-side localStorage (machine-specific)
const LOCAL_SETTING_KEYS = [
    SETTING_KEYS.BILLING_PRINTER,
    SETTING_KEYS.KOT_PRINTER,
    SETTING_KEYS.DEFAULT_BANK
];

// localStorage key prefix
const LOCAL_SETTINGS_PREFIX = 'appSettings_';

let settingsInitialized = false;
let currentDocumentDirectory = null;

async function initSettings() {
    if (!settingsInitialized) {
        setupEventHandlers();
        settingsInitialized = true;
    }
    await Promise.all([loadAvailablePrinters(), loadAvailableBanks()]);
    await loadCurrentSettings();
}

function setupEventHandlers() {
    document.getElementById('btnSave').addEventListener('click', saveSettings);
    document.getElementById('btnClear').addEventListener('click', clearForm);
    document.getElementById('btnRefreshBanks').addEventListener('click', refreshBanks);
    document.getElementById('btnRefreshBillingPrinter').addEventListener('click', refreshPrinters);
    document.getElementById('btnRefreshKotPrinter').addEventListener('click', refreshPrinters);
    document.getElementById('btnUploadBillLogo').addEventListener('click', uploadBillLogo);

    // Use Logo checkbox - save immediately on change (matches desktop)
    document.getElementById('chkUseBillLogo').addEventListener('change', function () {
        saveUseBillLogoSetting(this.checked);
    });
}

// ==================== LOCAL STORAGE (Client-side Properties) ====================

/**
 * Get the localStorage key for the current document directory.
 * This is the browser equivalent of ApplicationSetting.properties file
 * stored inside the document directory on desktop.
 */
function getLocalStorageKey() {
    if (!currentDocumentDirectory) return null;
    return LOCAL_SETTINGS_PREFIX + currentDocumentDirectory;
}

/**
 * Load machine-specific settings from localStorage.
 * Returns a map of key-value pairs (matches desktop ApplicationSettingProperties.loadSettings).
 */
function loadLocalSettings() {
    const storageKey = getLocalStorageKey();
    if (!storageKey) return {};

    try {
        const stored = localStorage.getItem(storageKey);
        if (stored) {
            return JSON.parse(stored);
        }
    } catch (e) {
        console.error('Failed to load local settings:', e);
    }
    return {};
}

/**
 * Save a machine-specific setting to localStorage.
 * Matches desktop ApplicationSettingProperties.saveSetting behavior.
 */
function saveLocalSetting(key, value) {
    const storageKey = getLocalStorageKey();
    if (!storageKey) return;

    try {
        const settings = loadLocalSettings();
        settings[key] = value;
        localStorage.setItem(storageKey, JSON.stringify(settings));
    } catch (e) {
        console.error('Failed to save local setting:', e);
    }
}

/**
 * Save multiple machine-specific settings to localStorage at once.
 */
function saveLocalSettings(settingsMap) {
    const storageKey = getLocalStorageKey();
    if (!storageKey) return;

    try {
        const existing = loadLocalSettings();
        Object.assign(existing, settingsMap);
        localStorage.setItem(storageKey, JSON.stringify(existing));
    } catch (e) {
        console.error('Failed to save local settings:', e);
    }
}

/**
 * Check if a setting key is machine-specific (stored locally).
 */
function isLocalSettingKey(key) {
    return LOCAL_SETTING_KEYS.includes(key);
}

// ==================== LOAD SETTINGS ====================

async function loadCurrentSettings() {
    try {
        // Load global settings from server (DB)
        const resp = await apiGet('/settings/all');
        const serverSettings = resp.data || resp || {};

        // Document Directory (from server)
        currentDocumentDirectory = serverSettings[SETTING_KEYS.DOCUMENT_DIRECTORY] || null;
        document.getElementById('lblCurrentDocument').textContent = currentDocumentDirectory || 'Not configured';

        // Font File (from server)
        const fontPath = serverSettings[SETTING_KEYS.FONT_PATH];
        document.getElementById('lblCurrentFont').textContent = fontPath || 'Not configured';

        // Bill Logo (from server)
        const billLogo = serverSettings[SETTING_KEYS.BILL_LOGO];
        document.getElementById('lblCurrentBillLogo').textContent = billLogo || 'Not configured';

        // Use Bill Logo checkbox (from server)
        const useLogo = serverSettings[SETTING_KEYS.USE_BILL_LOGO];
        document.getElementById('chkUseBillLogo').checked = (useLogo === 'true');

        // Load machine-specific settings from localStorage
        const localSettings = loadLocalSettings();

        // Billing Printer (from localStorage)
        const billingPrinter = localSettings[SETTING_KEYS.BILLING_PRINTER];
        document.getElementById('lblCurrentBillingPrinter').textContent = billingPrinter || 'Not configured';
        if (billingPrinter) {
            selectDropdownByValue('cmbBillingPrinter', billingPrinter);
        }

        // KOT Printer (from localStorage)
        const kotPrinter = localSettings[SETTING_KEYS.KOT_PRINTER];
        document.getElementById('lblCurrentKotPrinter').textContent = kotPrinter || 'Not configured';
        if (kotPrinter) {
            selectDropdownByValue('cmbKotPrinter', kotPrinter);
        }

        // Default Bank (from localStorage)
        const defaultBank = localSettings[SETTING_KEYS.DEFAULT_BANK];
        document.getElementById('lblCurrentDefaultBank').textContent = defaultBank || 'Not configured';
        if (defaultBank) {
            selectDropdownByText('cmbDefaultBank', defaultBank);
        }

    } catch (e) {
        console.error('Failed to load settings:', e);
        showError('Failed to load settings: ' + e.message);
    }
}

// ==================== LOAD PRINTERS ====================

async function loadAvailablePrinters() {
    try {
        const resp = await apiGet('/settings/printers');
        const printers = resp.data || resp || [];

        populatePrinterDropdown('cmbBillingPrinter', printers);
        populatePrinterDropdown('cmbKotPrinter', printers);

    } catch (e) {
        console.error('Failed to load printers:', e);
    }
}

function populatePrinterDropdown(elementId, printers) {
    const cmb = document.getElementById(elementId);
    const currentValue = cmb.value;
    cmb.innerHTML = '<option value="">-- Select Printer --</option>';

    printers.forEach(name => {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        cmb.appendChild(opt);
    });

    // Restore previous selection if still available
    if (currentValue) {
        selectDropdownByValue(elementId, currentValue);
    }
}

async function refreshPrinters() {
    const billingVal = document.getElementById('cmbBillingPrinter').value;
    const kotVal = document.getElementById('cmbKotPrinter').value;

    await loadAvailablePrinters();

    // Restore selections
    if (billingVal) selectDropdownByValue('cmbBillingPrinter', billingVal);
    if (kotVal) selectDropdownByValue('cmbKotPrinter', kotVal);

    showSuccess('Printer list refreshed');
}

// ==================== LOAD BANKS ====================

async function loadAvailableBanks() {
    try {
        const resp = await apiGet('/settings/banks');
        const banks = resp.data || resp || [];

        const cmb = document.getElementById('cmbDefaultBank');
        cmb.innerHTML = '<option value="">-- None --</option>';

        banks.forEach(bank => {
            const opt = document.createElement('option');
            opt.value = bank.name;
            opt.textContent = bank.name;
            cmb.appendChild(opt);
        });

    } catch (e) {
        console.error('Failed to load banks:', e);
    }
}

async function refreshBanks() {
    const currentSelection = document.getElementById('cmbDefaultBank').value;
    await loadAvailableBanks();

    if (currentSelection) {
        selectDropdownByValue('cmbDefaultBank', currentSelection);
    }
    showSuccess('Bank list refreshed');
}

// ==================== SAVE SETTINGS ====================

async function saveSettings() {
    const serverSettings = {};
    const localSettingsToSave = {};
    let hasChanges = false;

    // Document Directory (→ server)
    const docPath = document.getElementById('txtDocumentPath').value.trim();
    if (docPath) {
        serverSettings[SETTING_KEYS.DOCUMENT_DIRECTORY] = docPath;
        hasChanges = true;
    }

    // Font File (→ server)
    const fontPath = document.getElementById('txtFontPath').value.trim();
    if (fontPath) {
        serverSettings[SETTING_KEYS.FONT_PATH] = fontPath;
        hasChanges = true;
    }

    // Billing Printer (→ localStorage)
    const billingPrinter = document.getElementById('cmbBillingPrinter').value;
    if (billingPrinter) {
        localSettingsToSave[SETTING_KEYS.BILLING_PRINTER] = billingPrinter;
        hasChanges = true;
    }

    // KOT Printer (→ localStorage)
    const kotPrinter = document.getElementById('cmbKotPrinter').value;
    if (kotPrinter) {
        localSettingsToSave[SETTING_KEYS.KOT_PRINTER] = kotPrinter;
        hasChanges = true;
    }

    // Default Bank (→ localStorage)
    const defaultBank = document.getElementById('cmbDefaultBank').value;
    if (defaultBank) {
        localSettingsToSave[SETTING_KEYS.DEFAULT_BANK] = defaultBank;
        hasChanges = true;
    }

    if (!hasChanges) {
        showWarning('No changes to save. Please configure at least one setting.');
        return;
    }

    const btnSave = document.getElementById('btnSave');
    btnSave.classList.add('loading');

    try {
        // Save global settings to server (if any)
        if (Object.keys(serverSettings).length > 0) {
            await apiPost('/settings/save', serverSettings);
        }

        // If document directory was just set, update our reference
        if (docPath) {
            currentDocumentDirectory = docPath;
        }

        // Save machine-specific settings to localStorage
        if (Object.keys(localSettingsToSave).length > 0) {
            if (!currentDocumentDirectory) {
                showWarning('Please configure Document Directory first to save printer/bank settings.');
                return;
            }
            saveLocalSettings(localSettingsToSave);
        }

        showSuccess('Settings saved successfully');
        await loadCurrentSettings();
        clearForm();
    } catch (e) {
        showError('Failed to save settings: ' + e.message);
    } finally {
        btnSave.classList.remove('loading');
    }
}

// ==================== USE BILL LOGO ====================

async function saveUseBillLogoSetting(useLogo) {
    try {
        await apiPost('/settings/save', {
            [SETTING_KEYS.USE_BILL_LOGO]: String(useLogo)
        });
    } catch (e) {
        showError('Failed to save use bill logo setting: ' + e.message);
    }
}

// ==================== UPLOAD LOGO ====================

async function uploadBillLogo() {
    const fileInput = document.getElementById('fileBillLogo');
    if (!fileInput.files || fileInput.files.length === 0) {
        showWarning('Please select a bill logo image first.');
        return;
    }

    const file = fileInput.files[0];

    // Validate file type
    if (!file.type.startsWith('image/')) {
        showError('Please select a valid image file.');
        return;
    }

    const btnUpload = document.getElementById('btnUploadBillLogo');
    btnUpload.classList.add('loading');

    try {
        const formData = new FormData();
        formData.append('file', file);

        const token = getToken();
        const resp = await fetch('/api/v1/settings/upload-logo', {
            method: 'POST',
            headers: token ? { 'Authorization': 'Bearer ' + token } : {},
            body: formData
        });

        if (!resp.ok) {
            const errorData = await resp.json().catch(() => ({}));
            throw new Error(errorData.message || 'Upload failed');
        }

        const result = await resp.json();
        const savedPath = result.data || '';

        showSuccess('Bill logo uploaded successfully');
        document.getElementById('lblCurrentBillLogo').textContent = savedPath;
        fileInput.value = '';

    } catch (e) {
        showError('Failed to upload logo: ' + e.message);
    } finally {
        btnUpload.classList.remove('loading');
    }
}

// ==================== CLEAR FORM ====================

function clearForm() {
    document.getElementById('txtDocumentPath').value = '';
    document.getElementById('txtFontPath').value = '';
    document.getElementById('cmbBillingPrinter').selectedIndex = 0;
    document.getElementById('cmbKotPrinter').selectedIndex = 0;
    document.getElementById('cmbDefaultBank').selectedIndex = 0;
    document.getElementById('fileBillLogo').value = '';
}

// ==================== HELPERS ====================

function selectDropdownByValue(elementId, value) {
    const cmb = document.getElementById(elementId);
    for (let i = 0; i < cmb.options.length; i++) {
        if (cmb.options[i].value === value) {
            cmb.selectedIndex = i;
            return;
        }
    }
}

function selectDropdownByText(elementId, text) {
    const cmb = document.getElementById(elementId);
    for (let i = 0; i < cmb.options.length; i++) {
        if (cmb.options[i].text === text) {
            cmb.selectedIndex = i;
            return;
        }
    }
}

/**
 * Get a local setting value by key.
 * Can be called from other pages (e.g., BillingFrame) to read printer/bank config.
 */
function getLocalAppSetting(key) {
    const docDir = currentDocumentDirectory;
    if (!docDir) {
        // Try to get from any stored key
        for (let i = 0; i < localStorage.length; i++) {
            const k = localStorage.key(i);
            if (k && k.startsWith(LOCAL_SETTINGS_PREFIX)) {
                try {
                    const settings = JSON.parse(localStorage.getItem(k));
                    if (settings[key]) return settings[key];
                } catch (e) { /* ignore */ }
            }
        }
        return null;
    }
    const storageKey = LOCAL_SETTINGS_PREFIX + docDir;
    try {
        const stored = localStorage.getItem(storageKey);
        if (stored) {
            const settings = JSON.parse(stored);
            return settings[key] || null;
        }
    } catch (e) { /* ignore */ }
    return null;
}
