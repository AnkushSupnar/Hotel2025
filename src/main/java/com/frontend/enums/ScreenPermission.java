package com.frontend.enums;

/**
 * Enum defining all controllable screens in the application.
 * Used for role-based screen-level access control.
 */
public enum ScreenPermission {

    // Sales screens
    BILLING("Billing", "/fxml/transaction/BillingFrame.fxml", "SALES"),
    RECEIVE_PAYMENT("Receive Payment", "/fxml/transaction/ReceivePaymentFrame.fxml", "SALES"),

    // Purchase screens
    PURCHASE_ORDER("Purchase Order", "/fxml/transaction/PurchaseOrderFrame.fxml", "PURCHASE"),
    PURCHASE_INVOICE("Purchase Invoice", "/fxml/transaction/PurchaseBillFrame.fxml", "PURCHASE"),
    PAY_RECEIPT("Pay Receipt", "/fxml/transaction/PayReceiptFrame.fxml", "PURCHASE"),

    // Master data screens
    CATEGORY("Category Management", "/fxml/master/AddCategory.fxml", "MASTER"),
    ITEM("Item Management", "/fxml/master/AddItem.fxml", "MASTER"),
    TABLE("Table Management", "/fxml/master/AddTable.fxml", "MASTER"),
    CUSTOMER("Customer Management", "/fxml/master/AddCustomer.fxml", "MASTER"),
    EMPLOYEE("Employee Management", "/fxml/master/AddEmployee.fxml", "MASTER"),
    SUPPLIER("Supplier Management", "/fxml/master/AddSupplier.fxml", "MASTER"),
    USER("User Management", "/fxml/master/AddUser.fxml", "MASTER"),
    BANK("Bank Management", "/fxml/master/AddBank.fxml", "MASTER"),

    // Report screens
    SALES_REPORT("Sales Report", "/fxml/report/SalesReport.fxml", "REPORTS"),
    PURCHASE_REPORT("Purchase Report", "/fxml/report/PurchaseReport.fxml", "REPORTS"),
    PAYMENT_RECEIVED_REPORT("Payment Received Report", "/fxml/report/PaymentReceivedReport.fxml", "REPORTS"),
    PAY_RECEIPT_REPORT("Pay Receipt Report", "/fxml/report/PayReceiptReport.fxml", "REPORTS"),
    REDUCED_ITEM_REPORT("Reduced Item Report", "/fxml/report/ReducedItemReport.fxml", "REPORTS"),

    // Settings screens
    APPLICATION_SETTINGS("Application Settings", "/fxml/setting/ApplicationSetting.fxml", "SETTINGS"),
    USER_RIGHTS("User Rights", "/fxml/setting/UserRights.fxml", "SETTINGS");

    private final String displayName;
    private final String fxmlPath;
    private final String category;

    ScreenPermission(String displayName, String fxmlPath, String category) {
        this.displayName = displayName;
        this.fxmlPath = fxmlPath;
        this.category = category;
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
