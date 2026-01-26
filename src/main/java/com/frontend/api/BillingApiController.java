package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.dto.BillingDto.*;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.entity.*;
import com.frontend.print.BillPrint;
import com.frontend.print.KOTOrderPrint;
import com.frontend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REST API Controller for Billing operations
 * Provides endpoints for tables, orders, and bills management
 * Only active in 'server' profile
 */
@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "*")
@Tag(name = "Billing", description = "Billing operations - Tables, Orders, and Bills management")
@Profile("server")
public class BillingApiController {

    private static final Logger LOG = LoggerFactory.getLogger(BillingApiController.class);

    @Autowired
    private TableMasterService tableMasterService;

    @Autowired
    private TempTransactionService tempTransactionService;

    @Autowired
    private BillService billService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EmployeesService employeesService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CategoryApiService categoryApiService;

    @Autowired
    private BankService bankService;

    @Autowired
    private BankTransactionService bankTransactionService;

    @Autowired
    private KOTOrderPrint kotOrderPrint;

    @Autowired
    private BillPrint billPrint;

    // ==================== TABLE ENDPOINTS ====================

    /**
     * GET /api/billing/tables
     * Get all tables with their current status
     */
    @Operation(summary = "Get all tables", description = "Get all tables with their current status (Available, Ongoing, Closed)")
    @GetMapping("/tables")
    public ResponseEntity<ApiResponse> getAllTables() {
        try {
            List<String> sections = tableMasterService.getUniqueDescriptionsOrdered();
            List<TableStatusDto> tableList = new ArrayList<>();

            for (String section : sections) {
                List<TableMaster> tables = tableMasterService.getTablesByDescription(section);
                for (TableMaster table : tables) {
                    String status = calculateTableStatus(table.getId());
                    TableStatusDto dto = new TableStatusDto(
                            table.getId(),
                            table.getTableName(),
                            section,
                            status,
                            null
                    );
                    tableList.add(dto);
                }
            }

            LOG.info("Retrieved {} tables", tableList.size());
            return ResponseEntity.ok(new ApiResponse("Tables retrieved successfully", true, tableList));
        } catch (Exception e) {
            LOG.error("Error retrieving tables: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving tables: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/billing/tables/{tableId}
     * Get table by ID with its status
     */
    @GetMapping("/tables/{tableId}")
    public ResponseEntity<ApiResponse> getTableById(@PathVariable Integer tableId) {
        try {
            TableMaster table = tableMasterService.getTableById(tableId);
            if (table == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Table not found", false));
            }

            String status = calculateTableStatus(tableId);
            TableStatusDto dto = new TableStatusDto(
                    table.getId(),
                    table.getTableName(),
                    table.getDescription(),
                    status,
                    null
            );

            return ResponseEntity.ok(new ApiResponse("Table retrieved successfully", true, dto));
        } catch (Exception e) {
            LOG.error("Error retrieving table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving table: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/billing/tables/{tableId}/status
     * Get only the status of a table
     */
    @GetMapping("/tables/{tableId}/status")
    public ResponseEntity<ApiResponse> getTableStatusEndpoint(@PathVariable("tableId") Integer tableIdParam) {
        try {
            String status = calculateTableStatus(tableIdParam);
            return ResponseEntity.ok(new ApiResponse("Status retrieved", true, status));
        } catch (Exception e) {
            LOG.error("Error getting table status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== TRANSACTION ENDPOINTS ====================

    /**
     * GET /api/billing/tables/{tableId}/transactions
     * Get all transactions (temp + closed bill) for a table
     */
    @Operation(summary = "Get table transactions", description = "Get all transactions (temp + closed bill items) for a table")
    @GetMapping("/tables/{tableId}/transactions")
    public ResponseEntity<ApiResponse> getTransactionsForTable(@Parameter(description = "Table ID") @PathVariable Integer tableId) {
        try {
            List<TransactionItemDto> items = new ArrayList<>();

            // Get closed bill transactions if exists
            Bill closedBill = billService.getClosedBillForTable(tableId);
            if (closedBill != null) {
                List<Transaction> closedTransactions = billService.getTransactionsForBill(closedBill.getBillNo());
                for (Transaction trans : closedTransactions) {
                    TransactionItemDto dto = convertToTransactionDto(trans, tableId, closedBill.getWaitorId());
                    dto.setId(-trans.getId()); // Negative ID for closed bill items
                    items.add(dto);
                }
            }

            // Get temp transactions
            List<TempTransaction> tempTransactions = tempTransactionService.getTransactionsByTableNo(tableId);
            for (TempTransaction temp : tempTransactions) {
                items.add(convertToTransactionDto(temp));
            }

            LOG.info("Retrieved {} transactions for table {}", items.size(), tableId);
            return ResponseEntity.ok(new ApiResponse("Transactions retrieved successfully", true, items));
        } catch (Exception e) {
            LOG.error("Error retrieving transactions for table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/billing/tables/{tableId}/transactions
     * Add multiple items to table order (works like desktop application)
     *
     * Behavior:
     * - If item with same name and rate exists: quantity is ADDED to existing
     * - If item doesn't exist: new transaction is created
     * - Negative quantity: reduces existing item (use for removing items)
     * - If quantity becomes 0 or less: item is deleted
     * - If KOT already printed and item is reduced: tracks in reduced_item table
     *
     * Example request:
     * {
     *   "waitorId": 3,
     *   "userId": 1,
     *   "userName": "admin",
     *   "items": [
     *     { "itemName": "Chicken Biryani", "quantity": 2 },
     *     { "itemName": "Cold Drink", "quantity": 3 },
     *     { "itemName": "Naan", "quantity": -1 }  // Reduces existing Naan by 1
     *   ]
     * }
     */
    @Operation(summary = "Add/Update items to order",
               description = "Add multiple items to a table's order. If item exists, quantity is added. " +
                       "Negative quantity reduces item and tracks kitchen items in reduced_item table.")
    @PostMapping("/tables/{tableId}/transactions")
    public ResponseEntity<ApiResponse> addItemsToTable(
            @Parameter(description = "Table ID") @PathVariable Integer tableId,
            @RequestBody AddItemsRequest request) {
        try {
            // Validate request
            if (request.getWaitorId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Waiter ID is required", false));
            }
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("At least one item is required", false));
            }

            List<TransactionItemDto> processedItems = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int reducedItemsTracked = 0;

            // Process each item
            for (OrderItemDto item : request.getItems()) {
                // Validate item
                if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                    errors.add("Item name is required");
                    continue;
                }
                if (item.getQuantity() == null || item.getQuantity() == 0) {
                    errors.add("Valid quantity is required for: " + item.getItemName());
                    continue;
                }

                // Get item from database (needed for rate and category-based printQty)
                Optional<Item> dbItemOpt = itemService.getItemByName(item.getItemName());
                if (dbItemOpt.isEmpty()) {
                    errors.add("Item not found: " + item.getItemName());
                    continue;
                }
                Item dbItem = dbItemOpt.get();

                Float rate = item.getRate();
                if (rate == null) {
                    rate = dbItem.getRate();
                }

                // Calculate printQty based on category stock setting (matches desktop logic)
                Float printQty = calculatePrintQty(dbItem, item.getQuantity());

                // Create temp transaction
                TempTransaction tempTransaction = new TempTransaction();
                tempTransaction.setTableNo(tableId);
                tempTransaction.setItemName(item.getItemName());
                tempTransaction.setQty(item.getQuantity());
                tempTransaction.setRate(rate);
                tempTransaction.setAmt(item.getQuantity() * rate);
                tempTransaction.setWaitorId(request.getWaitorId());
                tempTransaction.setPrintQty(printQty);

                TempTransaction saved;

                // Use tracking method for negative quantities (reducing items)
                // This tracks kitchen items (already printed KOT) in reduced_item table
                if (item.getQuantity() < 0) {
                    saved = tempTransactionService.addOrUpdateTransactionWithTracking(
                            tempTransaction,
                            request.getUserId(),
                            request.getUserName()
                    );
                    reducedItemsTracked++;
                    LOG.info("Reduced item {} for table {} (qty: {}) - tracked for kitchen",
                            item.getItemName(), tableId, item.getQuantity());
                } else {
                    // Positive quantity - use normal method
                    saved = tempTransactionService.addOrUpdateTransaction(tempTransaction);
                    LOG.info("Added item {} for table {} (qty: {})",
                            item.getItemName(), tableId, item.getQuantity());
                }

                if (saved != null) {
                    processedItems.add(convertToTransactionDto(saved));
                } else {
                    LOG.info("Item {} removed from table {} (qty became 0)",
                            item.getItemName(), tableId);
                }
            }

            // Get all current transactions for the table
            List<TempTransaction> allTransactions = tempTransactionService.getTransactionsByTableNo(tableId);
            List<TransactionItemDto> allItems = new ArrayList<>();
            for (TempTransaction trans : allTransactions) {
                allItems.add(convertToTransactionDto(trans));
            }

            // Calculate totals
            Float totalAmount = tempTransactionService.getTotalAmount(tableId);
            Float totalQuantity = tempTransactionService.getTotalQuantity(tableId);

            // Auto-print KOT to server printer (mirrors desktop "Order" button behavior)
            boolean kotPrinted = false;
            int kotItemsPrinted = 0;
            String kotPrintError = null;
            try {
                TableMaster table = tableMasterService.getTableById(tableId);
                List<TempTransaction> printableItems = tempTransactionService.getPrintableItemsByTableNo(tableId);

                if (table != null && !printableItems.isEmpty()) {
                    Integer waitorId = request.getWaitorId();
                    LOG.info("Auto-printing KOT for table {} with {} items", table.getTableName(), printableItems.size());

                    kotOrderPrint.clearLastPrintError();
                    boolean printSuccess = kotOrderPrint.printKOT(table.getTableName(), tableId, printableItems, waitorId);

                    if (printSuccess) {
                        tempTransactionService.resetPrintQtyForTable(tableId);
                        kotPrinted = true;
                        kotItemsPrinted = printableItems.size();
                        LOG.info("KOT auto-printed successfully for table {} - {} items sent to kitchen",
                                table.getTableName(), printableItems.size());
                    } else {
                        kotPrintError = kotOrderPrint.getLastPrintError();
                        LOG.warn("KOT auto-print failed for table {}: {}",
                                table.getTableName(), kotPrintError != null ? kotPrintError : "unknown error");
                    }
                }
            } catch (Exception printEx) {
                kotPrintError = printEx.getMessage();
                LOG.warn("KOT auto-print error for table {}: {}", tableId, printEx.getMessage());
            }

            // Build response
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("transactions", allItems);
            result.put("totalAmount", totalAmount);
            result.put("totalQuantity", totalQuantity);
            result.put("itemCount", allItems.size());
            result.put("kotPrinted", kotPrinted);
            if (kotPrinted) {
                result.put("kotItemsPrinted", kotItemsPrinted);
            }
            if (kotPrintError != null) {
                result.put("kotPrintError", kotPrintError);
            }
            if (reducedItemsTracked > 0) {
                result.put("reducedItemsTracked", reducedItemsTracked);
            }
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }

            String message = errors.isEmpty()
                    ? "Items processed successfully"
                    : "Items processed with some errors";
            if (kotPrinted) {
                message += ". KOT printed (" + kotItemsPrinted + " items sent to kitchen)";
            }

            LOG.info("Processed {} items for table {}. Total: {} items, Amount: {}, Reduced tracked: {}, KOT printed: {}",
                    request.getItems().size(), tableId, allItems.size(), totalAmount, reducedItemsTracked, kotPrinted);

            return ResponseEntity.ok(new ApiResponse(message, errors.isEmpty(), result));

        } catch (Exception e) {
            LOG.error("Error adding items to table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * PUT /api/billing/transactions/{transactionId}
     * Update a temp transaction
     */
    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse> updateTransaction(
            @PathVariable Integer transactionId,
            @RequestBody TransactionItemDto request) {
        try {
            TempTransaction existing = tempTransactionService.getTransactionById(transactionId);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Transaction not found", false));
            }

            if (request.getQuantity() != null) {
                existing.setQty(request.getQuantity());
            }
            if (request.getRate() != null) {
                existing.setRate(request.getRate());
            }
            existing.setAmt(existing.getQty() * existing.getRate());

            tempTransactionService.updateTransaction(existing);

            LOG.info("Updated transaction {}", transactionId);
            return ResponseEntity.ok(new ApiResponse("Transaction updated successfully", true, convertToTransactionDto(existing)));
        } catch (Exception e) {
            LOG.error("Error updating transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * DELETE /api/billing/transactions/{transactionId}
     * Remove a temp transaction
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse> deleteTransaction(@PathVariable Integer transactionId) {
        try {
            tempTransactionService.deleteTransaction(transactionId);
            LOG.info("Deleted transaction {}", transactionId);
            return ResponseEntity.ok(new ApiResponse("Transaction removed successfully", true));
        } catch (Exception e) {
            LOG.error("Error deleting transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== KOT PRINT ENDPOINT ====================

    /**
     * POST /api/billing/tables/{tableId}/print-kot
     * Print KOT (Kitchen Order Ticket) for items that haven't been sent to kitchen yet
     * Only prints items with printQty > 0 and resets printQty after successful print
     */
    @Operation(summary = "Print KOT", description = "Print Kitchen Order Ticket for new/unprintd items on a table")
    @PostMapping("/tables/{tableId}/print-kot")
    public ResponseEntity<ApiResponse> printKOT(
            @Parameter(description = "Table ID") @PathVariable Integer tableId) {
        try {
            // Get table name
            TableMaster table = tableMasterService.getTableById(tableId);
            if (table == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Table not found", false));
            }

            // Get items with printQty > 0 (items that need kitchen printing)
            List<TempTransaction> printableItems = tempTransactionService.getPrintableItemsByTableNo(tableId);
            if (printableItems.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("No new items to print. All items have already been sent to kitchen.", false));
            }

            // Get waiter ID from first transaction
            Integer waitorId = printableItems.get(0).getWaitorId();

            LOG.info("Printing KOT for table {} with {} items", table.getTableName(), printableItems.size());

            // Print KOT to configured thermal printer (no dialog for API calls)
            kotOrderPrint.clearLastPrintError();
            boolean printSuccess = kotOrderPrint.printKOT(table.getTableName(), tableId, printableItems, waitorId);

            if (printSuccess) {
                // Reset printQty to 0 after successful print
                tempTransactionService.resetPrintQtyForTable(tableId);

                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("tableName", table.getTableName());
                result.put("itemsPrinted", printableItems.size());

                LOG.info("KOT printed successfully for table {} - {} items sent to kitchen",
                        table.getTableName(), printableItems.size());
                return ResponseEntity.ok(new ApiResponse("KOT printed successfully! " +
                        printableItems.size() + " items sent to kitchen.", true, result));
            } else {
                String printError = kotOrderPrint.getLastPrintError();
                String errorMsg = printError != null && !printError.isEmpty()
                        ? "KOT print failed: " + printError
                        : "KOT print failed";
                LOG.error("KOT print failed for table {}: {}", table.getTableName(), errorMsg);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse(errorMsg, false));
            }

        } catch (Exception e) {
            LOG.error("Error printing KOT for table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error printing KOT: " + e.getMessage(), false));
        }
    }

    // ==================== BILL ENDPOINTS ====================

    /**
     * POST /api/billing/tables/{tableId}/close
     * Close a table (create bill with CLOSE status)
     */
    @Operation(summary = "Close table", description = "Close a table and create a bill with CLOSE status")
    @PostMapping("/tables/{tableId}/close")
    public ResponseEntity<ApiResponse> closeTable(
            @Parameter(description = "Table ID") @PathVariable Integer tableId,
            @RequestBody CloseTableRequest request) {
        try {
            // Get temp transactions for the table
            List<TempTransaction> tempTransactions = tempTransactionService.getTransactionsByTableNo(tableId);

            // Check for existing closed bill
            Bill existingClosedBill = billService.getClosedBillForTable(tableId);

            Bill savedBill;
            if (existingClosedBill != null && !tempTransactions.isEmpty()) {
                // Add new items to existing closed bill
                savedBill = billService.addTransactionsToClosedBill(tableId, tempTransactions);
                LOG.info("Added {} items to existing closed bill #{}", tempTransactions.size(), savedBill.getBillNo());
            } else if (!tempTransactions.isEmpty()) {
                // Create new closed bill
                savedBill = billService.createClosedBill(
                        tableId,
                        request.getCustomerId(),
                        request.getWaitorId(),
                        request.getUserId(),
                        tempTransactions
                );
                LOG.info("Created new closed bill #{}", savedBill.getBillNo());
            } else if (existingClosedBill != null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Table already closed with no new items", false));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("No items to close", false));
            }

            BillResponseDto responseDto = convertToBillResponseDto(savedBill);

            // Generate PDF bytes (with QR code if default bank has UPI ID) and attach as Base64
            try {
                byte[] pdfBytes = generateBillPdfWithDefaultBank(savedBill, responseDto.getTableName());
                if (pdfBytes != null) {
                    responseDto.setPdfBase64(java.util.Base64.getEncoder().encodeToString(pdfBytes));
                }
            } catch (Exception pdfEx) {
                LOG.warn("Could not generate bill PDF for bill #{}: {}", savedBill.getBillNo(), pdfEx.getMessage());
            }

            return ResponseEntity.ok(new ApiResponse("Table closed successfully", true, responseDto));
        } catch (Exception e) {
            LOG.error("Error closing table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/billing/bills/{billNo}/pay
     * Mark bill as paid
     */
    @Operation(summary = "Mark bill as paid", description = "Mark a closed bill as PAID with payment details")
    @PostMapping("/bills/{billNo}/pay")
    public ResponseEntity<ApiResponse> markBillAsPaid(
            @Parameter(description = "Bill Number") @PathVariable Integer billNo,
            @RequestBody PayBillRequest request) {
        try {
            // Validate request
            if (request.getCashReceived() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Cash received is required", false));
            }
            if (request.getBankId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Bank ID is required", false));
            }

            // Validate bill exists
            Bill existingBill = billService.getBillByBillNo(billNo);
            if (existingBill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Bill not found: " + billNo, false));
            }

            // Validate bill status - must be CLOSE (same as desktop Pay button)
            if ("PAID".equalsIgnoreCase(existingBill.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Bill already PAID", false));
            }
            if ("CREDIT".equalsIgnoreCase(existingBill.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Bill already marked as CREDIT", false));
            }
            if (!"CLOSE".equalsIgnoreCase(existingBill.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Only closed bills can be processed. Current status: " + existingBill.getStatus(), false));
            }

            // Calculate payment values same as desktop Pay button
            float cashReceived = request.getCashReceived();
            float returnToCustomer = request.getReturnAmount() != null ? request.getReturnAmount() : 0f;
            float billAmount = existingBill.getBillAmt();
            String paymode = request.getPaymode() != null ? request.getPaymode() : "CASH";
            Integer bankId = request.getBankId();

            // Auto-calculate discount same as desktop:
            // netReceived = cashReceived - returnToCustomer
            // discount = billAmount - netReceived (when netReceived < billAmount)
            float netReceived = cashReceived - returnToCustomer;
            if (netReceived < 0) netReceived = 0;
            float discount = 0f;
            if (netReceived < billAmount) {
                discount = billAmount - netReceived;
            }

            // Mark bill as PAID (also reduces stock for sale inside service)
            Bill bill = billService.markBillAsPaid(
                    billNo,
                    cashReceived,
                    returnToCustomer,
                    discount,
                    paymode,
                    bankId
            );

            // Record bank transaction (same as desktop Pay button)
            if (bankId != null && netReceived > 0) {
                try {
                    // Get table name for bank transaction particulars
                    String tableName = null;
                    if (bill.getTableNo() != null) {
                        try {
                            TableMaster table = tableMasterService.getTableById(bill.getTableNo());
                            if (table != null) {
                                tableName = table.getTableName();
                            }
                        } catch (Exception te) {
                            LOG.warn("Could not get table name for bill #{}: {}", billNo, te.getMessage());
                        }
                    }
                    bankTransactionService.recordBillPayment(
                            bankId,
                            bill.getBillNo(),
                            (double) netReceived,
                            tableName
                    );
                    LOG.info("Bank transaction recorded: Bill #{}, Bank ID {}, Amount ₹{}",
                            bill.getBillNo(), bankId, netReceived);
                } catch (Exception e) {
                    LOG.error("Bill saved but bank transaction recording failed: ", e);
                    // Bill is already saved, don't fail the whole operation
                }
            }

            LOG.info("Bill #{} marked as PAID ({}). Net: ₹{}, Discount: ₹{}",
                    billNo, paymode, bill.getNetAmount(), discount);
            BillResponseDto responseDto = convertToBillResponseDto(bill);

            // Generate bill PDF (same as desktop Pay button prints the bill)
            try {
                String tblName = responseDto.getTableName();
                byte[] pdfBytes = generateBillPdfWithDefaultBank(bill, tblName);
                if (pdfBytes != null) {
                    responseDto.setPdfBase64(java.util.Base64.getEncoder().encodeToString(pdfBytes));
                }
            } catch (Exception pdfEx) {
                LOG.warn("Could not generate bill PDF for bill #{}: {}", bill.getBillNo(), pdfEx.getMessage());
            }

            return ResponseEntity.ok(new ApiResponse("Bill marked as paid successfully", true, responseDto));
        } catch (Exception e) {
            LOG.error("Error marking bill {} as paid: {}", billNo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/billing/bills/{billNo}/credit
     * Mark bill as credit
     */
    @Operation(summary = "Mark bill as credit", description = "Mark a closed bill as CREDIT for a customer")
    @PostMapping("/bills/{billNo}/credit")
    public ResponseEntity<ApiResponse> markBillAsCredit(
            @Parameter(description = "Bill Number") @PathVariable Integer billNo,
            @RequestBody CreditBillRequest request) {
        try {
            // Validate request
            if (request.getCustomerId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Customer ID is required for credit bills", false));
            }

            Bill bill = billService.markBillAsCredit(
                    billNo,
                    request.getCustomerId(),
                    request.getCashReceived() != null ? request.getCashReceived() : 0f,
                    request.getReturnAmount() != null ? request.getReturnAmount() : 0f,
                    request.getDiscount() != null ? request.getDiscount() : 0f
            );

            LOG.info("Bill #{} marked as CREDIT for customer {}", billNo, request.getCustomerId());
            BillResponseDto responseDto = convertToBillResponseDto(bill);
            return ResponseEntity.ok(new ApiResponse("Bill marked as credit successfully", true, responseDto));
        } catch (Exception e) {
            LOG.error("Error marking bill {} as credit: {}", billNo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/billing/bills/{billNo}
     * Get bill by bill number
     */
    @GetMapping("/bills/{billNo}")
    public ResponseEntity<ApiResponse> getBillByNo(@PathVariable Integer billNo) {
        try {
            Bill bill = billService.getBillWithTransactions(billNo);
            if (bill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Bill not found", false));
            }

            BillResponseDto responseDto = convertToBillResponseDto(bill);
            return ResponseEntity.ok(new ApiResponse("Bill retrieved successfully", true, responseDto));
        } catch (Exception e) {
            LOG.error("Error retrieving bill {}: {}", billNo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/billing/bills/{billNo}/pdf
     * Download bill as a thermal-receipt PDF
     */
    @Operation(summary = "Download bill PDF", description = "Generate and download the thermal-receipt PDF for a bill")
    @GetMapping(value = "/bills/{billNo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> getBillPdf(
            @Parameter(description = "Bill Number") @PathVariable Integer billNo) {
        try {
            Bill bill = billService.getBillWithTransactions(billNo);
            if (bill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Bill not found", false));
            }

            String tableName = "";
            if (bill.getTableNo() != null) {
                TableMaster table = tableMasterService.getTableById(bill.getTableNo());
                if (table != null) {
                    tableName = table.getTableName();
                }
            }

            byte[] pdfBytes = generateBillPdfWithDefaultBank(bill, tableName);
            if (pdfBytes == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse("Failed to generate PDF", false));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "bill_" + billNo + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error generating PDF for bill {}: {}", billNo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/billing/bills/today
     * Get today's bills (PAID and CREDIT)
     */
    @Operation(summary = "Get today's bills", description = "Get all PAID and CREDIT bills for today with summary")
    @GetMapping("/bills/today")
    public ResponseEntity<ApiResponse> getTodaysBills() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            List<Bill> paidBills = billService.getBillsByStatusAndDateOrderByBillNoAsc("PAID", today);
            List<Bill> creditBills = billService.getBillsByStatusAndDateOrderByBillNoAsc("CREDIT", today);

            List<BillResponseDto> allBills = new ArrayList<>();
            for (Bill bill : paidBills) {
                allBills.add(convertToBillResponseDto(bill));
            }
            for (Bill bill : creditBills) {
                allBills.add(convertToBillResponseDto(bill));
            }

            // Calculate summary
            BillSummaryDto summary = new BillSummaryDto();
            float totalCash = 0f, totalCredit = 0f;
            for (Bill b : paidBills) totalCash += b.getBillAmt();
            for (Bill b : creditBills) totalCredit += b.getBillAmt();
            summary.setTotalCash(totalCash);
            summary.setTotalCredit(totalCredit);
            summary.setTotalAmount(totalCash + totalCredit);
            summary.setBillCount(allBills.size());

            // Return both bills and summary
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("bills", allBills);
            result.put("summary", summary);

            LOG.info("Retrieved {} bills for today", allBills.size());
            return ResponseEntity.ok(new ApiResponse("Today's bills retrieved successfully", true, result));
        } catch (Exception e) {
            LOG.error("Error retrieving today's bills: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/billing/bills/search
     * Search bills by criteria
     */
    @PostMapping("/bills/search")
    public ResponseEntity<ApiResponse> searchBills(@RequestBody BillSearchRequest request) {
        try {
            List<Bill> results = new ArrayList<>();

            if (request.getBillNo() != null) {
                // Search by bill number
                billService.getBillByNo(request.getBillNo()).ifPresent(results::add);
            } else if (request.getDate() != null && !request.getDate().isEmpty()) {
                // Search by date
                List<Bill> paidBills = billService.getBillsByStatusAndDate("PAID", request.getDate());
                List<Bill> creditBills = billService.getBillsByStatusAndDate("CREDIT", request.getDate());
                results.addAll(paidBills);
                results.addAll(creditBills);

                // Filter by customer if provided
                if (request.getCustomerId() != null) {
                    results.removeIf(b -> !request.getCustomerId().equals(b.getCustomerId()));
                }
            } else if (request.getCustomerId() != null) {
                // Search by customer
                results = billService.getBillsByCustomerId(request.getCustomerId());
                results.removeIf(b -> !"PAID".equalsIgnoreCase(b.getStatus()) && !"CREDIT".equalsIgnoreCase(b.getStatus()));
            }

            List<BillResponseDto> responseDtos = new ArrayList<>();
            for (Bill bill : results) {
                responseDtos.add(convertToBillResponseDto(bill));
            }

            LOG.info("Search found {} bills", responseDtos.size());
            return ResponseEntity.ok(new ApiResponse("Search completed", true, responseDtos));
        } catch (Exception e) {
            LOG.error("Error searching bills: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/billing/tables/shift
     * Shift items from one table to another
     */
    @PostMapping("/tables/shift")
    public ResponseEntity<ApiResponse> shiftTable(@RequestBody ShiftTableRequest request) {
        try {
            if (request.getSourceTableId() == null || request.getTargetTableId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Source and target table IDs are required", false));
            }

            if (request.getSourceTableId().equals(request.getTargetTableId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Source and target tables must be different", false));
            }

            // Shift temp transactions
            int tempShifted = tempTransactionService.shiftTransactionsToTable(
                    request.getSourceTableId(), request.getTargetTableId());

            // Shift closed bill if exists
            Bill closedBill = billService.getClosedBillForTable(request.getSourceTableId());
            if (closedBill != null) {
                billService.shiftBillToTable(closedBill.getBillNo(), request.getTargetTableId());
            }

            LOG.info("Shifted items from table {} to table {}", request.getSourceTableId(), request.getTargetTableId());
            return ResponseEntity.ok(new ApiResponse("Table shift completed successfully", true));
        } catch (Exception e) {
            LOG.error("Error shifting table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * PUT /api/billing/bills/{billNo}
     * Update an existing bill
     */
    @PutMapping("/bills/{billNo}")
    public ResponseEntity<ApiResponse> updateBill(
            @PathVariable Integer billNo,
            @RequestBody UpdateBillRequest request) {
        try {
            // Convert DTOs to TempTransaction and calculate totals
            List<TempTransaction> tempTransactions = new ArrayList<>();
            float totalAmt = 0f;
            float totalQty = 0f;

            if (request.getItems() != null) {
                for (TransactionItemDto dto : request.getItems()) {
                    TempTransaction temp = new TempTransaction();
                    temp.setItemName(dto.getItemName());
                    temp.setQty(dto.getQuantity());
                    temp.setRate(dto.getRate());
                    float amt = dto.getAmount() != null ? dto.getAmount() : (dto.getQuantity() * dto.getRate());
                    temp.setAmt(amt);
                    tempTransactions.add(temp);

                    totalAmt += amt;
                    totalQty += dto.getQuantity();
                }
            }

            // Determine status based on request
            String newStatus = "PAID"; // Default status
            if (request.getPaymode() != null) {
                newStatus = request.getCustomerId() != null ? "CREDIT" : "PAID";
            }

            Bill updatedBill = billService.updateBillWithTransactions(
                    billNo,
                    tempTransactions,
                    request.getWaitorId(),
                    request.getCustomerId(),
                    totalAmt,
                    totalQty,
                    request.getCashReceived(),
                    request.getReturnAmount(),
                    newStatus
            );

            LOG.info("Updated bill #{}", billNo);
            BillResponseDto responseDto = convertToBillResponseDto(updatedBill);
            return ResponseEntity.ok(new ApiResponse("Bill updated successfully", true, responseDto));
        } catch (Exception e) {
            LOG.error("Error updating bill {}: {}", billNo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/billing/tables/{tableId}/closed-bill
     * Get closed bill for a table
     */
    @GetMapping("/tables/{tableId}/closed-bill")
    public ResponseEntity<ApiResponse> getClosedBillForTable(@PathVariable Integer tableId) {
        try {
            Bill closedBill = billService.getClosedBillForTable(tableId);
            if (closedBill == null) {
                return ResponseEntity.ok(new ApiResponse("No closed bill found for table", true, null));
            }

            BillResponseDto responseDto = convertToBillResponseDto(closedBill);
            return ResponseEntity.ok(new ApiResponse("Closed bill retrieved successfully", true, responseDto));
        } catch (Exception e) {
            LOG.error("Error getting closed bill for table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate printQty based on item's category stock setting.
     * Mirrors desktop BillingController.calculatePrintQty logic:
     * - If category stock = 'N': item needs kitchen print → printQty = qty
     * - If category stock = 'Y': item has stock tracking, no kitchen print → printQty = 0
     */
    private Float calculatePrintQty(Item dbItem, Float qty) {
        if (qty <= 0) {
            return 0f;
        }
        try {
            Integer categoryId = dbItem.getCategoryId();
            if (categoryId != null) {
                CategoryMasterDto category = categoryApiService.getCategoryById(categoryId);
                if (category != null && "N".equalsIgnoreCase(category.getStock())) {
                    return qty;
                } else {
                    return 0f;
                }
            }
        } catch (Exception e) {
            LOG.error("Error calculating printQty for item: {}", dbItem.getItemName(), e);
        }
        // Default to qty if unable to determine
        return qty;
    }

    private String calculateTableStatus(Integer tableId) {
        boolean hasTempTransactions = tempTransactionService.hasTransactions(tableId);
        boolean hasClosedBill = billService.hasClosedBill(tableId);

        if (hasClosedBill) {
            return "Closed";
        } else if (hasTempTransactions) {
            return "Ongoing";
        } else {
            return "Available";
        }
    }

    private TransactionItemDto convertToTransactionDto(TempTransaction temp) {
        TransactionItemDto dto = new TransactionItemDto();
        dto.setId(temp.getId());
        dto.setItemName(temp.getItemName());
        dto.setQuantity(temp.getQty());
        dto.setRate(temp.getRate());
        dto.setAmount(temp.getAmt());
        dto.setTableNo(temp.getTableNo());
        dto.setWaitorId(temp.getWaitorId());
        dto.setPrintQty(temp.getPrintQty());
        return dto;
    }

    private TransactionItemDto convertToTransactionDto(Transaction trans, Integer tableNo, Integer waitorId) {
        TransactionItemDto dto = new TransactionItemDto();
        dto.setId(trans.getId());
        dto.setItemName(trans.getItemName());
        dto.setQuantity(trans.getQty());
        dto.setRate(trans.getRate());
        dto.setAmount(trans.getAmt());
        dto.setTableNo(tableNo);
        dto.setWaitorId(waitorId);
        dto.setPrintQty(0f);
        return dto;
    }

    private BillResponseDto convertToBillResponseDto(Bill bill) {
        BillResponseDto dto = new BillResponseDto();
        dto.setBillNo(bill.getBillNo());
        dto.setBillDate(bill.getBillDate());
        dto.setTableNo(bill.getTableNo());
        dto.setCustomerId(bill.getCustomerId());
        dto.setWaitorId(bill.getWaitorId());
        dto.setBillAmount(bill.getBillAmt());
        dto.setNetAmount(bill.getNetAmount());
        dto.setCashReceived(bill.getCashReceived());
        dto.setReturnAmount(bill.getReturnAmount());
        dto.setDiscount(bill.getDiscount());
        dto.setStatus(bill.getStatus());
        dto.setPaymode(bill.getPaymode());
        dto.setBankId(bill.getBankId());

        // Get table name
        if (bill.getTableNo() != null) {
            try {
                TableMaster table = tableMasterService.getTableById(bill.getTableNo());
                if (table != null) {
                    dto.setTableName(table.getTableName());
                }
            } catch (Exception e) {
                LOG.warn("Could not get table name for tableNo: {}", bill.getTableNo());
            }
        }

        // Get customer name
        if (bill.getCustomerId() != null && bill.getCustomerId() > 0) {
            try {
                Customer customer = customerService.getCustomerById(bill.getCustomerId());
                if (customer != null) {
                    dto.setCustomerName(customer.getFullName());
                }
            } catch (Exception e) {
                LOG.warn("Could not get customer name for customerId: {}", bill.getCustomerId());
            }
        }

        // Get waiter name
        if (bill.getWaitorId() != null && bill.getWaitorId() > 0) {
            try {
                Employees waiter = employeesService.getEmployeeById(bill.getWaitorId());
                if (waiter != null) {
                    dto.setWaitorName(waiter.getFirstName());
                }
            } catch (Exception e) {
                LOG.warn("Could not get waiter name for waitorId: {}", bill.getWaitorId());
            }
        }

        // Get bank name
        if (bill.getBankId() != null && bill.getBankId() > 0) {
            try {
                bankService.getBankById(bill.getBankId()).ifPresent(bank -> {
                    dto.setBankName(bank.getBankName());
                });
            } catch (Exception e) {
                LOG.warn("Could not get bank name for bankId: {}", bill.getBankId());
            }
        }

        // Get transactions
        if (bill.getTransactions() != null && !bill.getTransactions().isEmpty()) {
            List<TransactionItemDto> items = new ArrayList<>();
            for (Transaction trans : bill.getTransactions()) {
                items.add(convertToTransactionDto(trans, bill.getTableNo(), bill.getWaitorId()));
            }
            dto.setItems(items);
        }

        return dto;
    }

    /**
     * Resolve the default bank configured in application settings.
     * Exactly mirrors desktop BillingController.setupPaymentMode() logic:
     * 1. Read "default_billing_bank" from application settings
     * 2. Search through ALL active banks (same as desktop getActiveBanks())
     * 3. Case-sensitive match on bank name (same as desktop line 2276: equals, not equalsIgnoreCase)
     * If no configured default found, falls back to cash bank (IFSC="cash").
     */
    private Bank resolveDefaultBank() {
        try {
            List<Bank> activeBanks = bankService.getActiveBanks();
            if (activeBanks == null || activeBanks.isEmpty()) {
                LOG.warn("No active banks found");
                return null;
            }

            String defaultBankName = SessionService.getApplicationSetting("default_billing_bank");
            Bank defaultBank = null;
            Bank cashBank = null;

            for (Bank bank : activeBanks) {
                // Track cash bank (IFSC="cash") as fallback - same as desktop
                if ("cash".equalsIgnoreCase(bank.getIfsc())) {
                    cashBank = bank;
                }
                // Match configured default bank by name (case-sensitive, same as desktop)
                if (defaultBankName != null && !defaultBankName.trim().isEmpty()
                        && defaultBankName.equals(bank.getBankName())) {
                    defaultBank = bank;
                }
            }

            // Return configured default bank, otherwise cash bank, otherwise first bank
            if (defaultBank != null) {
                LOG.info("Resolved configured default bank: '{}'", defaultBank.getBankName());
                return defaultBank;
            } else if (cashBank != null) {
                LOG.info("No configured default bank found, using cash bank");
                return cashBank;
            } else if (!activeBanks.isEmpty()) {
                LOG.warn("No cash bank found, using first active bank");
                return activeBanks.get(0);
            }
        } catch (Exception e) {
            LOG.warn("Could not resolve default bank: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generate bill PDF bytes, including QR code if applicable.
     * Exactly mirrors desktop BillingController.closeTable() + updatePrintQRVisibility() logic:
     * - QR code is included ONLY when the default bank is non-cash (IFSC != "cash")
     *   AND has a non-empty UPI ID (same as desktop line 2336)
     */
    private byte[] generateBillPdfWithDefaultBank(Bill bill, String tableName) {
        Bank defaultBank = resolveDefaultBank();
        if (defaultBank != null) {
            // Mirror desktop updatePrintQRVisibility(): QR only for non-cash banks with UPI ID
            boolean isCash = "cash".equalsIgnoreCase(defaultBank.getIfsc());
            boolean hasUpi = defaultBank.getUpiId() != null && !defaultBank.getUpiId().trim().isEmpty();

            if (!isCash && hasUpi) {
                LOG.info("Default bank '{}' is non-cash with UPI ID '{}', generating PDF with QR code",
                        defaultBank.getBankName(), defaultBank.getUpiId());
                return billPrint.generateBillPdfBytesWithQR(bill, tableName,
                        defaultBank.getUpiId(), defaultBank.getBankName());
            } else {
                LOG.info("Default bank '{}' (cash={}, hasUpi={}) - generating PDF without QR",
                        defaultBank.getBankName(), isCash, hasUpi);
            }
        } else {
            LOG.warn("No default bank resolved - generating PDF without QR");
        }
        return billPrint.generateBillPdfBytes(bill, tableName);
    }
}
