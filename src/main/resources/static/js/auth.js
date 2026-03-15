/**
 * Hotel Management - Login Page Logic
 * Replicates desktop LoginController behavior exactly.
 * This IS the server - no external server check needed.
 * Uses AutoCompleteTextField component (same as desktop customUI/AutoCompleteTextField.java)
 */

// AutoComplete instance for username field (like desktop: private AutoCompleteTextField autoCompleteTextField)
let usernameAutoComplete = null;

document.addEventListener('DOMContentLoaded', function () {
    // If already logged in, redirect to dashboard
    if (isLoggedIn()) {
        window.location.href = '/pages/dashboard/Dashboard.html';
        return;
    }

    // Load shops and usernames (like desktop initialize())
    loadShops();
    loadUsernames();

    // Bind form submit
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
});

/**
 * Load shops into dropdown (like desktop initializeShopDropdown)
 * Desktop: cmbShop with Kiran font, auto-select first
 */
async function loadShops() {
    const select = document.getElementById('shopSelect');
    try {
        const response = await fetch('/api/v1/web/shops');
        const result = await response.json();
        if (result.success && Array.isArray(result.data)) {
            select.innerHTML = '';
            if (result.data.length === 0) {
                select.innerHTML = '<option value="">No restaurants found</option>';
                return;
            }
            result.data.forEach(shop => {
                const option = document.createElement('option');
                option.value = shop.shopId;
                option.textContent = shop.restaurantName;
                select.appendChild(option);
            });
            // Auto-select first shop (like desktop: cmbShop.getSelectionModel().selectFirst())
            select.selectedIndex = 0;
        }
    } catch (error) {
        select.innerHTML = '<option value="">Error loading restaurants</option>';
    }
}

/**
 * Load usernames and initialize AutoCompleteTextField
 * (like desktop initializeUsernameAutocomplete with AutoCompleteTextField)
 * Desktop: new AutoCompleteTextField(txtUserName, usernames, customFont, txtPassword)
 */
async function loadUsernames() {
    try {
        const response = await fetch('/api/v1/web/usernames');
        const result = await response.json();
        if (result.success && Array.isArray(result.data)) {
            const usernameInput = document.getElementById('username');
            const passwordInput = document.getElementById('password');

            // Create AutoCompleteTextField exactly like desktop:
            // autoCompleteTextField = new AutoCompleteTextField(txtUserName, usernames, customFont, txtPassword);
            usernameAutoComplete = new AutoCompleteTextField(usernameInput, result.data, {
                customFont: 'Kiran',
                fontSize: 20,
                nextFocusField: passwordInput, // Desktop: txtPassword as nextFocusField
                useContainsFilter: false       // Desktop default: startsWith
            });
        }
    } catch (error) {
        console.error('Error loading usernames:', error);
    }
}

/**
 * Handle login (like desktop LoginController.login())
 */
async function handleLogin(e) {
    e.preventDefault();

    const shopId = document.getElementById('shopSelect').value;
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const loginBtn = document.getElementById('loginBtn');
    const errorDiv = document.getElementById('loginError');

    // Validate shop (like desktop: cmbShop.getSelectionModel().getSelectedItem() == null)
    if (!shopId) {
        showLoginError(errorDiv, 'Please select a restaurant');
        document.getElementById('shopSelect').focus();
        return;
    }

    // Validate username (like desktop: txtUserName.getText().isEmpty())
    if (!username) {
        showLoginError(errorDiv, 'Please Enter User Name');
        document.getElementById('username').focus();
        return;
    }

    // Validate password (like desktop: txtPassword.getText().isEmpty())
    if (!password) {
        showLoginError(errorDiv, 'Please Enter Password');
        document.getElementById('password').focus();
        return;
    }

    errorDiv.classList.add('d-none');
    loginBtn.disabled = true;
    loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><span>SIGNING IN...</span>';

    try {
        const response = await fetch('/api/v1/web/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, shopId }),
        });

        const result = await response.json();

        if (response.ok && result.success) {
            const data = result.data;

            // Store session (like desktop SessionService.setUserSession)
            setToken(data.token);
            setUser({
                username: data.username,
                role: data.role,
                userId: data.userId,
                employeeId: data.employeeId || null,
                employeeName: data.employeeName || data.username,
                shopId: data.shopId,
                restaurantName: data.restaurantName,
            });

            // Show success dialog (like desktop showLoginSuccessDialog with Kiran font)
            Swal.fire({
                icon: 'success',
                title: '<span style="font-size:18px; font-weight:bold; color:#4caf50;">Login Successful!</span>',
                html: '<div style="text-align:center;">' +
                      '<p style="font-family:Kiran,Segoe UI,sans-serif; font-size:20px; color:#1a237e; margin:8px 0 4px;">' + escapeHtml(data.restaurantName || '') + '</p>' +
                      '<p style="font-family:Kiran,Segoe UI,sans-serif; font-size:20px; color:#1a237e; margin:4px 0;">' + escapeHtml(data.employeeName || data.username) + '</p>' +
                      '<p style="font-size:14px; color:#424242; margin:4px 0;">Role: ' + escapeHtml(data.role || '') + '</p>' +
                      '</div>',
                timer: 1500,
                showConfirmButton: false,
            }).then(() => {
                window.location.href = '/pages/dashboard/Dashboard.html';
            });
        } else {
            showLoginError(errorDiv, result.message || 'Invalid username or password');
        }
    } catch (error) {
        showLoginError(errorDiv, 'An unexpected error occurred. Please try again.');
    } finally {
        loginBtn.disabled = false;
        loginBtn.innerHTML = '<i class="bi bi-box-arrow-in-right me-2"></i><span>SIGN IN</span>';
    }
}

function showLoginError(errorDiv, message) {
    errorDiv.textContent = message;
    errorDiv.classList.remove('d-none');
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}
