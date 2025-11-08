package com.frontend.entity;

public enum Designation {
    ADMIN("Admin"),
    MANAGER("Manager"),
    CAPTAIN("Captain"),
    WAITER("Waiter"),
    COOK("Cook"),
    HELPER("Helper"),
    CASHIER("Cashier"),
    CHEF("Chef"),
    SUPERVISOR("Supervisor");

    private final String displayName;

    Designation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
