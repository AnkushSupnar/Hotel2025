package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.dto.BillingDto.*;
import com.frontend.entity.KitchenOrder;
import com.frontend.entity.KitchenOrderItem;
import com.frontend.service.KitchenOrderService;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Kitchen Order (KOT) operations.
 * Extracted from BillingApiController for better separation of concerns.
 */
@RestController
@RequestMapping("/api/v1/kitchen-orders")
@Tag(name = "Kitchen Orders", description = "Kitchen Order Ticket (KOT) management - status tracking and updates")
@Profile("server")
public class KitchenOrderApiController {

    private static final Logger LOG = LoggerFactory.getLogger(KitchenOrderApiController.class);

    @Autowired
    private KitchenOrderService kitchenOrderService;

    @Operation(
        summary = "Get all pending kitchen orders grouped by table",
        description = "Returns all KOTs with status SENT across all tables, grouped by table name."
    )
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse> getPendingKitchenOrders() {
        try {
            List<KitchenOrder> orders = kitchenOrderService.getAllPendingKitchenOrders();
            List<KitchenOrdersByTableDto> grouped = groupKitchenOrdersByTable(orders);
            LOG.info("Retrieved {} pending kitchen orders across {} tables", orders.size(), grouped.size());
            return ResponseEntity.ok(new ApiResponse("Pending kitchen orders retrieved", true, grouped));
        } catch (Exception e) {
            LOG.error("Error retrieving pending kitchen orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(
        summary = "Get all ready kitchen orders grouped by table",
        description = "Returns all KOTs with status READY across all tables, grouped by table name."
    )
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse> getReadyKitchenOrders() {
        try {
            List<KitchenOrder> orders = kitchenOrderService.getAllReadyKitchenOrders();
            List<KitchenOrdersByTableDto> grouped = groupKitchenOrdersByTable(orders);
            LOG.info("Retrieved {} ready kitchen orders across {} tables", orders.size(), grouped.size());
            return ResponseEntity.ok(new ApiResponse("Ready kitchen orders retrieved", true, grouped));
        } catch (Exception e) {
            LOG.error("Error retrieving ready kitchen orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(
        summary = "Get all kitchen orders grouped by table",
        description = "Returns all KOTs across all tables regardless of status (SENT, READY, SERVE), grouped by table name."
    )
    @GetMapping
    public ResponseEntity<ApiResponse> getAllKitchenOrders() {
        try {
            List<KitchenOrder> orders = kitchenOrderService.getAllKitchenOrders();
            List<KitchenOrdersByTableDto> grouped = groupKitchenOrdersByTable(orders);
            LOG.info("Retrieved {} total kitchen orders across {} tables", orders.size(), grouped.size());
            return ResponseEntity.ok(new ApiResponse("All kitchen orders retrieved", true, grouped));
        } catch (Exception e) {
            LOG.error("Error retrieving all kitchen orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(
        summary = "Get kitchen orders for a table",
        description = "Returns all KOTs for a specific table, regardless of status."
    )
    @GetMapping("/table/{tableId}")
    public ResponseEntity<ApiResponse> getKitchenOrdersForTable(
            @Parameter(description = "Table ID") @PathVariable Integer tableId) {
        try {
            List<KitchenOrder> orders = kitchenOrderService.getKitchenOrdersForTable(tableId);
            List<KitchenOrderDto> dtos = new ArrayList<>();
            for (KitchenOrder ko : orders) {
                dtos.add(convertToKitchenOrderDto(ko));
            }
            LOG.info("Retrieved {} kitchen orders for table {}", dtos.size(), tableId);
            return ResponseEntity.ok(new ApiResponse("Kitchen orders retrieved", true, dtos));
        } catch (Exception e) {
            LOG.error("Error retrieving kitchen orders for table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(
        summary = "Update kitchen order status",
        description = "Update the status of a single KOT. Valid transitions: SENT->READY, READY->SERVE."
    )
    @PutMapping("/{kotId}/status")
    public ResponseEntity<ApiResponse> updateKotStatus(
            @Parameter(description = "Kitchen Order ID") @PathVariable Integer kotId,
            @RequestBody Map<String, String> body) {
        try {
            String newStatus = body != null ? body.get("status") : null;
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Status is required. Valid values: READY, SERVE", false));
            }
            newStatus = newStatus.trim().toUpperCase();

            KitchenOrder ko;
            switch (newStatus) {
                case "READY":
                    ko = kitchenOrderService.markAsReady(kotId);
                    break;
                case "SERVE":
                    ko = kitchenOrderService.markAsServed(kotId);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse("Invalid status: " + newStatus + ". Valid values: READY, SERVE", false));
            }

            KitchenOrderDto dto = convertToKitchenOrderDto(ko);
            LOG.info("KOT #{} status updated to {}", kotId, newStatus);
            return ResponseEntity.ok(new ApiResponse("Kitchen order status updated to " + newStatus, true, dto));
        } catch (RuntimeException e) {
            LOG.error("Error updating KOT #{} status: {}", kotId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), false));
        } catch (Exception e) {
            LOG.error("Error updating KOT #{} status: {}", kotId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(summary = "Mark a single KOT as READY")
    @PutMapping("/{kotId}/ready")
    public ResponseEntity<ApiResponse> markSingleKotReady(
            @Parameter(description = "Kitchen Order ID") @PathVariable Integer kotId) {
        try {
            KitchenOrder ko = kitchenOrderService.markAsReady(kotId);
            KitchenOrderDto dto = convertToKitchenOrderDto(ko);
            LOG.info("KOT #{} marked as READY", kotId);
            return ResponseEntity.ok(new ApiResponse("Kitchen order marked as ready", true, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(summary = "Mark a single KOT as SERVE")
    @PutMapping("/{kotId}/serve")
    public ResponseEntity<ApiResponse> markSingleKotServed(
            @Parameter(description = "Kitchen Order ID") @PathVariable Integer kotId) {
        try {
            KitchenOrder ko = kitchenOrderService.markAsServed(kotId);
            KitchenOrderDto dto = convertToKitchenOrderDto(ko);
            LOG.info("KOT #{} marked as SERVE", kotId);
            return ResponseEntity.ok(new ApiResponse("Kitchen order marked as served", true, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(summary = "Mark all SENT orders as READY for a table")
    @PostMapping("/table/{tableId}/complete")
    public ResponseEntity<ApiResponse> completeKitchenOrders(
            @Parameter(description = "Table ID") @PathVariable Integer tableId) {
        try {
            kitchenOrderService.markAllAsReadyForTable(tableId);
            List<KitchenOrder> orders = kitchenOrderService.getKitchenOrdersForTable(tableId);
            List<KitchenOrderDto> dtos = new ArrayList<>();
            for (KitchenOrder ko : orders) {
                dtos.add(convertToKitchenOrderDto(ko));
            }
            return ResponseEntity.ok(new ApiResponse("Kitchen orders marked as ready", true, dtos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    @Operation(summary = "Mark all READY orders as SERVE for a table")
    @PostMapping("/table/{tableId}/serve")
    public ResponseEntity<ApiResponse> serveKitchenOrders(
            @Parameter(description = "Table ID") @PathVariable Integer tableId) {
        try {
            kitchenOrderService.markAllAsServedForTable(tableId);
            List<KitchenOrder> orders = kitchenOrderService.getKitchenOrdersForTable(tableId);
            List<KitchenOrderDto> dtos = new ArrayList<>();
            for (KitchenOrder ko : orders) {
                dtos.add(convertToKitchenOrderDto(ko));
            }
            return ResponseEntity.ok(new ApiResponse("Kitchen orders marked as served", true, dtos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    // ==================== HELPER METHODS ====================

    private List<KitchenOrdersByTableDto> groupKitchenOrdersByTable(List<KitchenOrder> orders) {
        LinkedHashMap<Integer, List<KitchenOrder>> byTable = new LinkedHashMap<>();
        for (KitchenOrder ko : orders) {
            byTable.computeIfAbsent(ko.getTableNo(), k -> new ArrayList<>()).add(ko);
        }

        List<KitchenOrdersByTableDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<KitchenOrder>> entry : byTable.entrySet()) {
            List<KitchenOrderDto> kotDtos = new ArrayList<>();
            String tableName = null;
            for (KitchenOrder ko : entry.getValue()) {
                kotDtos.add(convertToKitchenOrderDto(ko));
                if (tableName == null) {
                    tableName = ko.getTableName();
                }
            }
            result.add(new KitchenOrdersByTableDto(entry.getKey(), tableName, kotDtos));
        }
        return result;
    }

    private KitchenOrderDto convertToKitchenOrderDto(KitchenOrder ko) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        KitchenOrderDto dto = new KitchenOrderDto();
        dto.setId(ko.getId());
        dto.setTableNo(ko.getTableNo());
        dto.setTableName(ko.getTableName());
        dto.setWaitorId(ko.getWaitorId());
        dto.setStatus(ko.getStatus());
        dto.setItemCount(ko.getItemCount());
        dto.setTotalQty(ko.getTotalQty());
        dto.setSentAt(ko.getSentAt() != null ? ko.getSentAt().format(fmt) : null);
        dto.setReadyAt(ko.getReadyAt() != null ? ko.getReadyAt().format(fmt) : null);

        if (ko.getItems() != null) {
            List<KitchenOrderItemDto> itemDtos = new ArrayList<>();
            for (KitchenOrderItem item : ko.getItems()) {
                KitchenOrderItemDto itemDto = new KitchenOrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setItemId(item.getItemId());
                itemDto.setItemName(item.getItemName());
                itemDto.setQty(item.getQty());
                itemDto.setRate(item.getRate());
                itemDtos.add(itemDto);
            }
            dto.setItems(itemDtos);
        }

        return dto;
    }
}
