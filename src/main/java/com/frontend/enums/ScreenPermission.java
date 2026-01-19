package com.frontend.enums;

/**
 * Enum defining all controllable screens in the application.
 * Used for role-based screen-level access control.
 */
public enum ScreenPermission {

    // Dashboard screen
    DASHBOARD("Dashboard", "/fxml/dashboard/Dashboard.fxml", "DASHBOARD",
            "Access to the main dashboard with overview statistics and quick actions"),

    // Sales screens
    BILLING("Billing", "/fxml/transaction/BillingFrame.fxml", "SALES",
            "Access to create and manage customer bills and invoices"),
    RECEIVE_PAYMENT("Receive Payment", "/fxml/transaction/ReceivePaymentFrame.fxml", "SALES",
            "Access to record payments received from customers"),

    // Purchase screens
    PURCHASE_ORDER("Purchase Order", "/fxml/transaction/PurchaseOrderFrame.fxml", "PURCHASE",
            "Access to create and manage purchase orders to suppliers"),
    PURCHASE_INVOICE("Purchase Invoice", "/fxml/transaction/PurchaseBillFrame.fxml", "PURCHASE",
            "Access to record and manage supplier invoices"),
    PURCHASE_INVOICE_FROM_PO("Purchase Invoice from PO", "/fxml/transaction/PurchaseInvoiceFromPO.fxml", "PURCHASE",
            "Access to create purchase invoices directly from approved purchase orders"),
    PAY_RECEIPT("Pay Receipt", "/fxml/transaction/PayReceiptFrame.fxml", "PURCHASE",
            "Access to record payments made to suppliers"),

    // Master data screens
    CATEGORY("Category Management", "/fxml/master/AddCategory.fxml", "MASTER",
            "Access to create and manage item categories"),
    ITEM("Item Management", "/fxml/master/AddItem.fxml", "MASTER",
            "Access to create and manage menu items and products"),
    TABLE("Table Management", "/fxml/master/AddTable.fxml", "MASTER",
            "Access to create and manage restaurant tables"),
    CUSTOMER("Customer Management", "/fxml/master/AddCustomer.fxml", "MASTER",
            "Access to create and manage customer records"),
    EMPLOYEE("Employee Management", "/fxml/master/AddEmployee.fxml", "MASTER",
            "Access to create and manage employee records"),
    SUPPLIER("Supplier Management", "/fxml/master/AddSupplier.fxml", "MASTER",
            "Access to create and manage supplier records"),
    USER("User Management", "/fxml/master/AddUser.fxml", "MASTER",
            "Access to create and manage user accounts"),
    BANK("Bank Management", "/fxml/master/AddBank.fxml", "MASTER",
            "Access to create and manage bank accounts"),

    // Report screens
    SALES_REPORT("Sales Report", "/fxml/report/SalesReport.fxml", "REPORTS",
            "Access to view and export sales reports"),
    PURCHASE_REPORT("Purchase Report", "/fxml/report/PurchaseReport.fxml", "REPORTS",
            "Access to view and export purchase reports"),
    PAYMENT_RECEIVED_REPORT("Payment Received Report", "/fxml/report/PaymentReceivedReport.fxml", "REPORTS",
            "Access to view and export payment received reports"),
    PAY_RECEIPT_REPORT("Pay Receipt Report", "/fxml/report/PayReceiptReport.fxml", "REPORTS",
            "Access to view and export payment made reports"),
    REDUCED_ITEM_REPORT("Reduced Item Report", "/fxml/report/ReducedItemReport.fxml", "REPORTS",
            "Access to view and export reduced/discounted item reports"),

    // Settings screens
    APPLICATION_SETTINGS("Application Settings", "/fxml/setting/ApplicationSetting.fxml", "SETTINGS",
            "Access to configure application settings"),
    USER_RIGHTS("User Rights", "/fxml/setting/UserRights.fxml", "SETTINGS",
            "Access to manage user role permissions"),
    SHOP_DETAILS("Shop Details", "/fxml/setting/ShopDetails.fxml", "SETTINGS",
            "Access to manage restaurant and shop information");

    private final String displayName;
    private final String fxmlPath;
    private final String category;
    private final String description;

    ScreenPermission(String displayName, String fxmlPath, String category, String description) {
        this.displayName = displayName;
        this.fxmlPath = fxmlPath;
        this.category = category;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find a ScreenPermission by its FXML path
     * @param path the FXML path to search for
     * @return the matching ScreenPermission or null if not found
     */
    public static ScreenPermission fromFxmlPath(String path) {
        if (path == null) {
            return null;
        }
        for (ScreenPermission sp : values()) {
            if (sp.fxmlPath.equals(path)) {
                return sp;
            }
        }
        return null;
    }

    /**
     * Get all permissions for a specific category
     * @param category the category to filter by (SALES, PURCHASE, MASTER, REPORTS, SETTINGS)
     * @return array of ScreenPermission for the category
     */
    public static ScreenPermission[] getByCategory(String category) {
        return java.util.Arrays.stream(values())
                .filter(sp -> sp.category.equals(category))
                .toArray(ScreenPermission[]::new);
    }
}
