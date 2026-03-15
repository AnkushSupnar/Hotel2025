/**
 * Hotel Management - Dashboard Page Logic
 * Exact replica of desktop HomeController behavior.
 * Uses DashboardService.getTableStatus() via /api/v1/web/dashboard
 */

document.addEventListener('DOMContentLoaded', function () {
    if (!requireAuth()) return;

    initializeHeader();
    loadDashboardData();

    // Sidebar header click -> reload dashboard (like desktop sidebarHeader.setOnMouseClicked)
    document.getElementById('sidebarHeader').addEventListener('click', function () {
        loadDashboardData();
    });
});

/**
 * Initialize header with session data
 * (like desktop HomeController.initialize: lblShopeeName, lblSidebarShopName, txtUserName, txtDesignation)
 */
function initializeHeader() {
    const user = getUser();
    if (!user) return;

    // Set restaurant name (like desktop: SessionService.getCurrentRestaurantName())
    const restaurantName = user.restaurantName || 'Hotel Management System';
    document.getElementById('lblShopeeName').textContent = restaurantName;
    document.getElementById('lblSidebarShopName').textContent = restaurantName;

    // Set user info (like desktop: txtUserName, txtDesignation)
    document.getElementById('txtUserName').textContent = user.employeeName || user.username || 'Guest User';
    document.getElementById('txtDesignation').textContent = user.role || '';
}

/**
 * Load dashboard data from server
 * (like desktop HomeController.initializeDashboardData using DashboardService.getTableStatus)
 */
async function loadDashboardData() {
    // Set loading state (like desktop: lblTotalTables.setText("..."))
    document.getElementById('lblTotalTables').textContent = '...';
    document.getElementById('lblActiveTables').textContent = '...';
    document.getElementById('lblClosedBillTables').textContent = '...';
    document.getElementById('lblAvailableTables').textContent = '...';
    document.getElementById('lblTodayCompleted').textContent = '...';

    try {
        const result = await apiGet('/web/dashboard');

        if (result && result.success) {
            const data = result.data;
            document.getElementById('lblTotalTables').textContent = data.total || 0;
            document.getElementById('lblActiveTables').textContent = data.active || 0;
            document.getElementById('lblClosedBillTables').textContent = data.closedBill || 0;
            document.getElementById('lblAvailableTables').textContent = data.available || 0;
            document.getElementById('lblTodayCompleted').textContent = data.todayCompleted || 0;
        }
    } catch (error) {
        console.error('Dashboard load error:', error);
        document.getElementById('lblTotalTables').textContent = '0';
        document.getElementById('lblActiveTables').textContent = '0';
        document.getElementById('lblClosedBillTables').textContent = '0';
        document.getElementById('lblAvailableTables').textContent = '0';
        document.getElementById('lblTodayCompleted').textContent = '0';
    }
}

/**
 * Show support dialog (like desktop HomeController.showSupportDialog)
 */
function showSupportDialog() {
    Swal.fire({
        title: '',
        html: `
            <div style="text-align: center;">
                <div style="width: 60px; height: 60px; border-radius: 50%; background: rgba(25,118,210,0.1); display: inline-flex; align-items: center; justify-content: center; margin-bottom: 10px;">
                    <i class="bi bi-headphones" style="font-size: 2em; color: #1976D2;"></i>
                </div>
                <h4 style="color: #1976D2; margin-bottom: 5px;">Contact Support</h4>
                <p style="color: #757575; font-size: 12px;">We're here to help you</p>
            </div>
            <hr>
            <div style="text-align: center; margin-bottom: 10px;">
                <strong style="color: #1976D2; font-size: 16px;">Ankush Supnar</strong><br>
                <small style="color: #757575;">Software Developer & Support</small>
            </div>
            <div style="text-align: left; display: flex; flex-direction: column; gap: 8px;">
                <div style="background: #F8F9FA; border-radius: 10px; padding: 10px 12px; border: 1px solid #E8E8E8;">
                    <small style="color: #9E9E9E; font-weight: 600;">Mobile Number</small><br>
                    <strong style="color: #333;">+91 8329394603, +91 9960855742</strong>
                </div>
                <div style="background: #F8F9FA; border-radius: 10px; padding: 10px 12px; border: 1px solid #E8E8E8;">
                    <small style="color: #9E9E9E; font-weight: 600;">WhatsApp</small><br>
                    <strong style="color: #333;">+91 8329394603</strong>
                </div>
                <div style="background: #F8F9FA; border-radius: 10px; padding: 10px 12px; border: 1px solid #E8E8E8;">
                    <small style="color: #9E9E9E; font-weight: 600;">Email Address</small><br>
                    <strong style="color: #333;">ankushsupnar@gmail.com</strong>
                </div>
                <div style="background: #F8F9FA; border-radius: 10px; padding: 10px 12px; border: 1px solid #E8E8E8;">
                    <small style="color: #9E9E9E; font-weight: 600;">Location</small><br>
                    <strong style="color: #333;">Ahilyanagar, Maharashtra, India</strong>
                </div>
            </div>
            <div style="margin-top: 10px;">
                <span style="background: #E8F5E9; border-radius: 16px; padding: 6px 14px; font-size: 10px; color: #2E7D32; font-weight: 600;">
                    <i class="bi bi-circle-fill" style="font-size: 6px; color: #4CAF50;"></i>
                    Available Mon - Sat, 9:00 AM - 9:00 PM
                </span>
            </div>
        `,
        showCloseButton: true,
        confirmButtonText: 'Close',
        confirmButtonColor: '#1976D2',
        width: 420,
    });
}
