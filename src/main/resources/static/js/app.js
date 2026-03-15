/**
 * Hotel Management - Web Frontend
 * Core API Client & Utilities
 */

const API_BASE = '/api/v1';

// ==================== AUTH HELPERS ====================

function getToken() {
    return localStorage.getItem('token');
}

function setToken(token) {
    localStorage.setItem('token', token);
}

function clearToken() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
}

function getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
}

function setUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
}

function isLoggedIn() {
    return !!getToken();
}

function requireAuth() {
    if (!isLoggedIn()) {
        window.location.href = '/index.html';
        return false;
    }
    return true;
}

function logout() {
    clearToken();
    window.location.href = '/index.html';
}

// ==================== API CLIENT ====================

async function apiCall(endpoint, options = {}) {
    const url = API_BASE + endpoint;
    const token = getToken();

    const defaultHeaders = {
        'Content-Type': 'application/json',
    };

    if (token) {
        defaultHeaders['Authorization'] = 'Bearer ' + token;
    }

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers,
        },
    };

    try {
        const response = await fetch(url, config);

        if (response.status === 401 || response.status === 403) {
            clearToken();
            window.location.href = '/index.html';
            return null;
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        return await response.text();
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

function apiGet(endpoint) {
    return apiCall(endpoint, { method: 'GET' });
}

function apiPost(endpoint, data) {
    return apiCall(endpoint, {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

function apiPut(endpoint, data) {
    return apiCall(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

function apiDelete(endpoint) {
    return apiCall(endpoint, { method: 'DELETE' });
}

// ==================== UI HELPERS ====================

function showSuccess(message) {
    Swal.fire({
        icon: 'success',
        title: 'Success',
        text: message,
        timer: 2000,
        showConfirmButton: false,
    });
}

function showError(message) {
    Swal.fire({
        icon: 'error',
        title: 'Error',
        text: message,
    });
}

function showWarning(message) {
    Swal.fire({
        icon: 'warning',
        title: 'Warning',
        text: message,
    });
}

async function showConfirm(message) {
    const result = await Swal.fire({
        icon: 'question',
        title: 'Confirm',
        text: message,
        showCancelButton: true,
        confirmButtonColor: '#1976D2',
        cancelButtonColor: '#757575',
        confirmButtonText: 'Yes',
        cancelButtonText: 'No',
    });
    return result.isConfirmed;
}

function showLoading(message = 'Loading...') {
    Swal.fire({
        title: message,
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading(),
    });
}

function hideLoading() {
    Swal.close();
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2,
    }).format(amount || 0);
}

// Format date
function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    });
}

// Format date-time
function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleString('en-IN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

// Set active nav item
function setActiveNav(navId) {
    document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));
    const activeLink = document.getElementById(navId);
    if (activeLink) activeLink.classList.add('active');
}

// Display username in navbar
function displayUserInfo() {
    const user = getUser();
    if (user) {
        const el = document.getElementById('currentUserName');
        if (el) el.textContent = user.employeeName || user.username || 'User';
        const roleEl = document.getElementById('currentUserRole');
        if (roleEl) roleEl.textContent = user.role || '';
    }
}
