# QR Code Printing on Bill Implementation Plan

## Overview
Add QR code to printed bills when the CLOSE button is clicked, only when:
1. A bank (non-cash) is selected
2. The selected bank has a UPI ID configured
3. The "Print QR" checkbox is checked

The QR code will NOT be printed when using the PAID button or other printing functionality.

## Implementation Steps

### Step 1: Add ZXing Dependencies to pom.xml
Add Google's ZXing library for QR code generation:
```xml
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

### Step 2: Create QRCodeGenerator Utility Class
Create `src/main/java/com/frontend/util/QRCodeGenerator.java`:
- Method: `generateUPIQRCode(String upiId, String payeeName, double amount)` returns `byte[]`
- Generates UPI payment string: `upi://pay?pa=<UPI_ID>&pn=<NAME>&am=<AMOUNT>&cu=INR`
- Creates QR code image (150x150 pixels) suitable for thermal printer
- Returns PNG image as byte array for embedding in PDF

### Step 3: Modify BillPrint.java
Add overloaded method to support QR code printing:
- New method: `printBill(Bill bill, Table table, boolean printQR, String upiId)`
- Existing method calls new method with `printQR=false` for backward compatibility
- In `generateBillPdf()`, if `printQR=true`:
  - Generate QR code using QRCodeGenerator
  - Add QR code image centered above the footer section
  - Add text "Scan to Pay" below QR code

### Step 4: Modify BillingController.closeTable()
Update the close table flow:
- Check if `isPrintQRSelected()` returns true
- If true, get the UPI ID using `getSelectedBankUpiId()`
- Call the new `printBill(bill, table, true, upiId)` method
- If false, call existing `printBill(bill, table)` method (no QR)

### Step 5: Ensure PAID Button Flow Unchanged
- The PAID button flow should continue using the existing `printBill(bill, table)` method
- No QR code will be printed for PAID bills

## Files to Modify
1. `pom.xml` - Add ZXing dependencies
2. `src/main/java/com/frontend/util/QRCodeGenerator.java` - New file
3. `src/main/java/com/frontend/print/BillPrint.java` - Add QR code support
4. `src/main/java/com/frontend/controller/transaction/BillingController.java` - Pass QR parameters

## QR Code Details
- Size: 150x150 pixels (suitable for 80mm thermal printer)
- Format: UPI payment link
- Position: Centered, above footer text
- Label: "Scan to Pay" below QR code
