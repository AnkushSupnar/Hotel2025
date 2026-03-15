package com.frontend.api.dashboard;

import com.frontend.dto.ApiResponse;
import com.frontend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API Controller for Web Dashboard data.
 * Mirrors desktop DashboardController using DashboardService.
 * Only active in 'server' profile.
 */
@RestController
@RequestMapping("/api/v1/web/dashboard")
@Tag(name = "Web Dashboard", description = "Web dashboard data endpoints")
@Profile("server")
public class WebDashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(WebDashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * GET /api/v1/web/dashboard
     * Dashboard data - table status counts: total, active, closedBill, available, todayCompleted
     */
    @Operation(summary = "Dashboard Data", description = "Get dashboard table status data (same as desktop)")
    @GetMapping
    public ResponseEntity<ApiResponse> getDashboardData() {
        try {
            Map<String, Long> tableStatus = dashboardService.getTableStatus();
            return ResponseEntity.ok(new ApiResponse("Dashboard data retrieved", true, tableStatus));
        } catch (Exception e) {
            LOG.error("Error getting dashboard data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/v1/web/dashboard/full
     * Full dashboard data - same as desktop DashboardController using DashboardService.getAllDashboardData()
     * Returns KPIs, charts, table status, order status, top items, recent transactions, etc.
     */
    @Operation(summary = "Full Dashboard Data", description = "Get comprehensive dashboard data (same as desktop DashboardController)")
    @GetMapping("/full")
    public ResponseEntity<ApiResponse> getFullDashboardData() {
        try {
            Map<String, Object> allData = dashboardService.getAllDashboardData();
            return ResponseEntity.ok(new ApiResponse("Full dashboard data retrieved", true, allData));
        } catch (Exception e) {
            LOG.error("Error getting full dashboard data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }
}
