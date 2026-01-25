package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.dto.CategoryMasterDto;
import com.frontend.dto.ItemDto;
import com.frontend.entity.*;
import com.frontend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for Master Data
 * Provides endpoints for customers, items, categories, employees, and banks
 * Only active in 'server' profile
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "Master Data", description = "Master data management - Customers, Items, Categories, Employees, Banks")
@Profile("server")
public class MasterDataApiController {

    private static final Logger LOG = LoggerFactory.getLogger(MasterDataApiController.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CategoryApiService categoryApiService;

    @Autowired
    private EmployeesService employeesService;

    @Autowired
    private BankService bankService;

    // ==================== CUSTOMER ENDPOINTS ====================

    /**
     * GET /api/customers
     * Get all customers
     */
    @Operation(summary = "Get all customers", description = "Retrieve all customers")
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse> getAllCustomers() {
        try {
            List<Customer> customers = customerService.getAllCustomers();
            LOG.info("Retrieved {} customers", customers.size());
            return ResponseEntity.ok(new ApiResponse("Customers retrieved successfully", true, customers));
        } catch (Exception e) {
            LOG.error("Error retrieving customers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/customers/{id}
     * Get customer by ID
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse> getCustomerById(@PathVariable Integer id) {
        try {
            Customer customer = customerService.getCustomerById(id);
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Customer not found", false));
            }
            return ResponseEntity.ok(new ApiResponse("Customer retrieved successfully", true, customer));
        } catch (Exception e) {
            LOG.error("Error retrieving customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/customers/search
     * Search customers by name or mobile
     */
    @GetMapping("/customers/search")
    public ResponseEntity<ApiResponse> searchCustomers(@RequestParam String query) {
        try {
            List<Customer> allCustomers = customerService.getAllCustomers();
            String searchLower = query.toLowerCase();

            List<Customer> results = new ArrayList<>();
            for (Customer c : allCustomers) {
                if ((c.getFullName() != null && c.getFullName().toLowerCase().contains(searchLower)) ||
                    (c.getMobileNo() != null && c.getMobileNo().contains(query))) {
                    results.add(c);
                }
            }

            LOG.info("Customer search '{}' found {} results", query, results.size());
            return ResponseEntity.ok(new ApiResponse("Search completed", true, results));
        } catch (Exception e) {
            LOG.error("Error searching customers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== CATEGORY ENDPOINTS ====================

    /**
     * GET /api/categories
     * Get all categories
     */
    @Operation(summary = "Get all categories", description = "Retrieve all item categories")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse> getAllCategories() {
        try {
            List<CategoryMasterDto> categories = categoryApiService.getAllCategories();
            LOG.info("Retrieved {} categories", categories.size());
            return ResponseEntity.ok(new ApiResponse("Categories retrieved successfully", true, categories));
        } catch (Exception e) {
            LOG.error("Error retrieving categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/categories/{id}
     * Get category by ID
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable Integer id) {
        try {
            CategoryMasterDto category = categoryApiService.getCategoryById(id);
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Category not found", false));
            }
            return ResponseEntity.ok(new ApiResponse("Category retrieved successfully", true, category));
        } catch (Exception e) {
            LOG.error("Error retrieving category {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== ITEM ENDPOINTS ====================

    /**
     * GET /api/items
     * Get all items
     */
    @Operation(summary = "Get all items", description = "Retrieve all menu items")
    @GetMapping("/items")
    public ResponseEntity<ApiResponse> getAllItems() {
        try {
            List<ItemDto> items = itemService.getAllItems();
            LOG.info("Retrieved {} items", items.size());
            return ResponseEntity.ok(new ApiResponse("Items retrieved successfully", true, items));
        } catch (Exception e) {
            LOG.error("Error retrieving items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/items/{id}
     * Get item by ID
     */
    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse> getItemById(@PathVariable Integer id) {
        try {
            ItemDto item = itemService.getItemById(id);
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Item not found", false));
            }
            return ResponseEntity.ok(new ApiResponse("Item retrieved successfully", true, item));
        } catch (Exception e) {
            LOG.error("Error retrieving item {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/items/category/{categoryId}
     * Get items by category ID
     */
    @GetMapping("/items/category/{categoryId}")
    public ResponseEntity<ApiResponse> getItemsByCategory(@PathVariable Integer categoryId) {
        try {
            List<ItemDto> items = itemService.getItemsByCategoryId(categoryId);
            LOG.info("Retrieved {} items for category {}", items.size(), categoryId);
            return ResponseEntity.ok(new ApiResponse("Items retrieved successfully", true, items));
        } catch (Exception e) {
            LOG.error("Error retrieving items for category {}: {}", categoryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/items/code/{code}
     * Get item by code
     */
    @GetMapping("/items/code/{code}")
    public ResponseEntity<ApiResponse> getItemByCode(@PathVariable Integer code) {
        try {
            Item item = itemService.getItemByCode(code);
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Item not found with code: " + code, false));
            }
            return ResponseEntity.ok(new ApiResponse("Item retrieved successfully", true, item));
        } catch (Exception e) {
            LOG.error("Error retrieving item by code {}: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/items/name/{name}
     * Get item by name
     */
    @GetMapping("/items/name/{name}")
    public ResponseEntity<ApiResponse> getItemByName(@PathVariable String name) {
        try {
            Optional<Item> item = itemService.getItemByName(name);
            if (item.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Item not found: " + name, false));
            }
            return ResponseEntity.ok(new ApiResponse("Item retrieved successfully", true, item.get()));
        } catch (Exception e) {
            LOG.error("Error retrieving item by name {}: {}", name, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/items/search
     * Search items by name
     */
    @GetMapping("/items/search")
    public ResponseEntity<ApiResponse> searchItems(@RequestParam String query) {
        try {
            List<ItemDto> allItems = itemService.getAllItems();
            String searchLower = query.toLowerCase();

            List<ItemDto> results = new ArrayList<>();
            for (ItemDto item : allItems) {
                if (item.getItemName() != null && item.getItemName().toLowerCase().contains(searchLower)) {
                    results.add(item);
                }
            }

            LOG.info("Item search '{}' found {} results", query, results.size());
            return ResponseEntity.ok(new ApiResponse("Search completed", true, results));
        } catch (Exception e) {
            LOG.error("Error searching items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== EMPLOYEE/WAITER ENDPOINTS ====================

    /**
     * GET /api/employees/waiters
     * Get all waiters
     */
    @Operation(summary = "Get all waiters", description = "Retrieve all waiters/servers")
    @GetMapping("/employees/waiters")
    public ResponseEntity<ApiResponse> getAllWaiters() {
        try {
            List<String> waiterNames = employeesService.getWaiterNames();

            // Get waiter details
            List<Map<String, Object>> waiters = new ArrayList<>();
            for (String name : waiterNames) {
                List<Employees> matches = employeesService.searchByFirstName(name);
                if (!matches.isEmpty()) {
                    Employees waiter = matches.get(0);
                    Map<String, Object> waiterMap = new HashMap<>();
                    waiterMap.put("id", waiter.getEmployeeId());
                    waiterMap.put("name", waiter.getFirstName());
                    waiterMap.put("fullName", waiter.getFullName());
                    waiters.add(waiterMap);
                }
            }

            LOG.info("Retrieved {} waiters", waiters.size());
            return ResponseEntity.ok(new ApiResponse("Waiters retrieved successfully", true, waiters));
        } catch (Exception e) {
            LOG.error("Error retrieving waiters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/employees/{id}
     * Get employee by ID
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<ApiResponse> getEmployeeById(@PathVariable Integer id) {
        try {
            Employees employee = employeesService.getEmployeeById(id);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Employee not found", false));
            }
            return ResponseEntity.ok(new ApiResponse("Employee retrieved successfully", true, employee));
        } catch (Exception e) {
            LOG.error("Error retrieving employee {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== BANK ENDPOINTS ====================

    /**
     * GET /api/banks
     * Get all active banks
     */
    @Operation(summary = "Get all banks", description = "Retrieve all active banks for payment")
    @GetMapping("/banks")
    public ResponseEntity<ApiResponse> getAllBanks() {
        try {
            List<Bank> banks = bankService.getActiveBanks();
            LOG.info("Retrieved {} active banks", banks.size());
            return ResponseEntity.ok(new ApiResponse("Banks retrieved successfully", true, banks));
        } catch (Exception e) {
            LOG.error("Error retrieving banks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/banks/{id}
     * Get bank by ID
     */
    @GetMapping("/banks/{id}")
    public ResponseEntity<ApiResponse> getBankById(@PathVariable Integer id) {
        try {
            Optional<Bank> bank = bankService.getBankById(id);
            if (bank.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Bank not found", false));
            }
            return ResponseEntity.ok(new ApiResponse("Bank retrieved successfully", true, bank.get()));
        } catch (Exception e) {
            LOG.error("Error retrieving bank {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/banks/cash
     * Get cash bank (IFSC = "cash")
     */
    @GetMapping("/banks/cash")
    public ResponseEntity<ApiResponse> getCashBank() {
        try {
            List<Bank> banks = bankService.getActiveBanks();
            for (Bank bank : banks) {
                if ("cash".equalsIgnoreCase(bank.getIfsc())) {
                    return ResponseEntity.ok(new ApiResponse("Cash bank retrieved successfully", true, bank));
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Cash bank not found", false));
        } catch (Exception e) {
            LOG.error("Error retrieving cash bank: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }
}
