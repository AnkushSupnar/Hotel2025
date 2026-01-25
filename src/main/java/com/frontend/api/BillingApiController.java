package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.dto.BillingDto.*;
import com.frontend.entity.*;
import com.frontend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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
                    TransactionItemDto dto = convertToTransactionDto(trans, tableId);
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
     * Add item to table order
     */
    @Operation(summary = "Add item to order", description = "Add an item to a table's order")
    @PostMapping("/tables/{tableId}/transactions")
    public ResponseEntity<ApiResponse> addItemToTable(
            @Parameter(description = "Table ID") @PathVariable Integer tableId,
            @RequestBody AddItemRequest request) {
        try {
            // Validate request
            if (request.getItemName() == null || request.getItemName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Item name is required", false));
            }
            if (request.getQuantity() == null || request.getQuantity() == 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Valid quantity is required", false));
            }
            if (request.getWaitorId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Waiter ID is required", false));
            }

            // Get item rate if not provided
            Float rate = request.getRate();
            if (rate == null) {
                Optional<Item> item = itemService.getItemByName(request.getItemName());
                if (item.isPresent()) {
                    rate = item.get().getRate();
                } else {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse("Item not found: " + request.getItemName(), false));
                }
            }

            // Create temp transaction
            TempTransaction tempTransaction = new TempTransaction();
            tempTransaction.setTableNo(tableId);
            tempTransaction.setItemName(request.getItemName());
            tempTransaction.setQty(request.getQuantity());
            tempTransaction.setRate(rate);
            tempTransaction.setAmt(request.getQuantity() * rate);
            tempTransaction.setWaitorId(request.getWaitorId());
            tempTransaction.setPrintQty(request.getQuantity() > 0 ? request.getQuantity() : 0f);

            // Save to database
            TempTransaction saved = tempTransactionService.addOrUpdateTransaction(tempTransaction);

            LOG.info("Added item {} to table {}", request.getItemName(), tableId);
            return ResponseEntity.ok(new ApiResponse("Item added successfully", true, convertToTransactionDto(saved)));
        } catch (Exception e) {
            LOG.error("Error adding item to table {}: {}", tableId, e.getMessage());
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

            Bill bill = billService.markBillAsPaid(
                    billNo,
                    request.getCashReceived(),
                    request.getReturnAmount() != null ? request.getReturnAmount() : 0f,
                    request.getDiscount() != null ? request.getDiscount() : 0f,
                    request.getPaymode() != null ? request.getPaymode() : "CASH",
                    request.getBankId()
            );

            LOG.info("Bill #{} marked as PAID", billNo);
            BillResponseDto responseDto = convertToBillResponseDto(bill);
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

    private TransactionItemDto convertToTransactionDto(Transaction trans, Integer tableNo) {
        TransactionItemDto dto = new TransactionItemDto();
        dto.setId(trans.getId());
        dto.setItemName(trans.getItemName());
        dto.setQuantity(trans.getQty());
        dto.setRate(trans.getRate());
        dto.setAmount(trans.getAmt());
        dto.setTableNo(tableNo);
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
        if (bill.getCustomerId() != null) {
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
        if (bill.getWaitorId() != null) {
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
        if (bill.getBankId() != null) {
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
                items.add(convertToTransactionDto(trans, bill.getTableNo()));
            }
            dto.setItems(items);
        }

        return dto;
    }
}
