package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.SalesBillPayment;
import com.frontend.entity.SalesPaymentReceipt;
import com.frontend.print.ReceivePaymentPrint;
import com.frontend.service.BillService;
import com.frontend.service.CustomerService;
import com.frontend.service.SalesPaymentReceiptService;
import com.frontend.service.SalesPaymentReceiptService.BillPaymentAllocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * REST API Controller for Receive Payment functionality.
 * Web equivalent of desktop ReceivePaymentController.java
 */
@RestController
@RequestMapping("/api/v1/receive-payment")
@Profile("server")
public class ReceivePaymentApiController {

    private static final Logger LOG = LoggerFactory.getLogger(ReceivePaymentApiController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private SalesPaymentReceiptService salesPaymentReceiptService;

    @Autowired
    private BillService billService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ReceivePaymentPrint receivePaymentPrint;

    /**
     * Get pending credit bills for a customer.
     * Matches desktop loadPendingBillsForCustomer().
     */
    @GetMapping("/pending-bills/{customerId}")
    public ResponseEntity<ApiResponse> getPendingBills(@PathVariable Integer customerId) {
        try {
            List<Bill> bills = billService.getCreditBillsWithPendingBalanceByCustomerId(customerId);
            List<Map<String, Object>> result = new ArrayList<>();
            double totalPending = 0.0;

            for (Bill bill : bills) {
                float balance = bill.getBalanceAmount();
                if (balance > 0) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("billNo", bill.getBillNo());
                    row.put("billDate", bill.getBillDate() != null ? bill.getBillDate() : "");
                    row.put("billTime", bill.getBillTime() != null ? bill.getBillTime() : "");
                    row.put("netAmount", bill.getNetAmount() != null ? bill.getNetAmount() : 0.0);
                    row.put("paidAmount", bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0);
                    row.put("balanceAmount", balance);
                    row.put("status", bill.getStatus());
                    result.add(row);
                    totalPending += balance;
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("bills", result);
            response.put("totalPending", totalPending);

            LOG.info("Loaded {} pending bills for customer {}, total pending: {}",
                    result.size(), customerId, totalPending);
            return ResponseEntity.ok(new ApiResponse("Pending bills loaded", true, response));
        } catch (Exception e) {
            LOG.error("Error loading pending bills for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to load pending bills: " + e.getMessage(), false, null));
        }
    }

    /**
     * Process a payment (receive payment from customer).
     * Matches desktop processPayment() + recordGroupedPayment().
     */
    @PostMapping("/receive")
    public ResponseEntity<ApiResponse> receivePayment(@RequestBody Map<String, Object> request) {
        try {
            Integer customerId = (Integer) request.get("customerId");
            Double totalAmount = ((Number) request.get("totalAmount")).doubleValue();
            Integer bankId = (Integer) request.get("bankId");
            String paymentMode = (String) request.get("paymentMode");
            String chequeNo = (String) request.get("chequeNo");
            String referenceNo = (String) request.get("referenceNo");
            String remarks = (String) request.get("remarks");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocationsRaw = (List<Map<String, Object>>) request.get("allocations");

            if (customerId == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Customer is required", false, null));
            }
            if (totalAmount == null || totalAmount <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Payment amount must be greater than 0", false, null));
            }
            if (bankId == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Payment mode is required", false, null));
            }
            if (allocationsRaw == null || allocationsRaw.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("At least one bill must be selected", false, null));
            }

            // Build allocations
            List<BillPaymentAllocation> allocations = new ArrayList<>();
            for (Map<String, Object> alloc : allocationsRaw) {
                Integer billNo = (Integer) alloc.get("billNo");
                Double amount = ((Number) alloc.get("amount")).doubleValue();
                allocations.add(new BillPaymentAllocation(billNo, amount));
            }

            // Process the payment
            SalesPaymentReceipt receipt = salesPaymentReceiptService.recordGroupedPayment(
                    customerId, totalAmount, bankId, paymentMode,
                    chequeNo, referenceNo, remarks, allocations
            );

            // Build response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("receiptNo", receipt.getReceiptNo());
            result.put("totalAmount", receipt.getTotalAmount());
            result.put("billsCount", receipt.getBillsCount());
            result.put("paymentMode", receipt.getPaymentMode());
            result.put("paymentDate", receipt.getPaymentDate() != null ? receipt.getPaymentDate().format(DATE_FORMAT) : "");

            // Bill payment details
            List<Map<String, Object>> billDetails = new ArrayList<>();
            if (receipt.getBillPayments() != null) {
                for (SalesBillPayment bp : receipt.getBillPayments()) {
                    Map<String, Object> bd = new LinkedHashMap<>();
                    bd.put("billNo", bp.getBillNo());
                    bd.put("amount", bp.getPaymentAmount());
                    billDetails.add(bd);
                }
            }
            result.put("billPayments", billDetails);

            LOG.info("Payment received: Receipt#{}, Amount={}, Bills={}",
                    receipt.getReceiptNo(), totalAmount, allocations.size());
            return ResponseEntity.ok(new ApiResponse("Payment received successfully", true, result));

        } catch (Exception e) {
            LOG.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Payment failed: " + e.getMessage(), false, null));
        }
    }

    /**
     * Get receipt history with date range filter.
     * Matches desktop loadReceiptHistory().
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getReceiptHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            List<SalesPaymentReceipt> receipts = salesPaymentReceiptService.getReceiptsByDateRangeWithCustomer(from, to);
            List<Map<String, Object>> result = new ArrayList<>();

            for (SalesPaymentReceipt receipt : receipts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("receiptNo", receipt.getReceiptNo());
                row.put("paymentDate", receipt.getPaymentDate() != null ? receipt.getPaymentDate().format(DATE_FORMAT) : "");
                row.put("billsCount", receipt.getBillsCount() != null ? receipt.getBillsCount() : 1);
                row.put("customerName", receipt.getCustomerName());
                row.put("customerId", receipt.getCustomerId());
                row.put("totalAmount", receipt.getTotalAmount() != null ? receipt.getTotalAmount() : 0.0);
                row.put("paymentMode", receipt.getPaymentMode() != null ? receipt.getPaymentMode() : "");
                result.add(row);
            }

            LOG.info("Retrieved {} receipt history records ({} to {})", result.size(), from, to);
            return ResponseEntity.ok(new ApiResponse("Receipt history loaded", true, result));
        } catch (Exception e) {
            LOG.error("Error loading receipt history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to load history: " + e.getMessage(), false, null));
        }
    }

    /**
     * Get summary totals (today and this month).
     * Matches desktop updateSummaryLabels().
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getSummary() {
        try {
            Double todayTotal = salesPaymentReceiptService.getTodayTotalReceipts();
            LocalDate today = LocalDate.now();
            Double monthTotal = salesPaymentReceiptService.getTotalReceiptsByDateRange(
                    today.withDayOfMonth(1), today);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("todayTotal", todayTotal != null ? todayTotal : 0.0);
            summary.put("monthTotal", monthTotal != null ? monthTotal : 0.0);

            return ResponseEntity.ok(new ApiResponse("Summary loaded", true, summary));
        } catch (Exception e) {
            LOG.error("Error loading summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to load summary: " + e.getMessage(), false, null));
        }
    }

    /**
     * Print/download receipt as PDF.
     * Returns Base64-encoded PDF for client-side download.
     */
    @GetMapping("/receipt/{receiptNo}/pdf")
    public ResponseEntity<ApiResponse> getReceiptPdf(@PathVariable Integer receiptNo) {
        try {
            Optional<SalesPaymentReceipt> optReceipt = salesPaymentReceiptService.getReceiptWithBillPayments(receiptNo);
            if (optReceipt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Receipt not found: " + receiptNo, false, null));
            }

            SalesPaymentReceipt receipt = optReceipt.get();
            Customer customer = null;
            if (receipt.getCustomerId() != null) {
                customer = customerService.getCustomerById(receipt.getCustomerId());
            }

            byte[] pdfBytes = receivePaymentPrint.generateReceiptPdfBytes(receipt, customer);
            String base64 = Base64.getEncoder().encodeToString(pdfBytes);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pdfBase64", base64);
            result.put("receiptNo", receiptNo);

            return ResponseEntity.ok(new ApiResponse("Receipt PDF generated", true, result));
        } catch (Exception e) {
            LOG.error("Error generating receipt PDF for {}: {}", receiptNo, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to generate receipt: " + e.getMessage(), false, null));
        }
    }
}
