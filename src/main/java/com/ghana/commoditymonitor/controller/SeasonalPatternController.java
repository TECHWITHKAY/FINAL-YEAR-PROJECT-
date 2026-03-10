package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.SeasonalOutlookDto;
import com.ghana.commoditymonitor.dto.response.SeasonalPatternDto;
import com.ghana.commoditymonitor.security.CurrentUser;
import com.ghana.commoditymonitor.security.UserPrincipal;
import com.ghana.commoditymonitor.service.SeasonalPatternService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/seasonal")
@RequiredArgsConstructor
@Tag(name = "Seasonal Patterns", description = "Endpoints for seasonal price pattern analysis")
public class SeasonalPatternController {

    private final SeasonalPatternService seasonalPatternService;

    @GetMapping("/{commodityId}")
    @Operation(summary = "Get seasonal patterns for a commodity", 
               description = "Guests see 3-month preview, authenticated users see all 12 months")
    public ResponseEntity<ApiResponse<List<SeasonalPatternDto>>> getPatternsForCommodity(
            @PathVariable Long commodityId,
            @CurrentUser UserPrincipal principal) {
        log.info("REST request to get seasonal patterns for commodity: {} (user: {})", 
                 commodityId, principal != null ? principal.username() : "guest");
        return ResponseEntity.ok(ApiResponse.ok(
                seasonalPatternService.getPatternsForCommodity(commodityId, principal)));
    }

    @GetMapping("/{commodityId}/best-month")
    @Operation(summary = "Get best month to buy a commodity", 
               description = "Returns the month with lowest seasonal index (cheapest)")
    public ResponseEntity<ApiResponse<SeasonalPatternDto>> getBestMonthToBuy(
            @PathVariable Long commodityId) {
        log.info("REST request to get best month to buy commodity: {}", commodityId);
        return ResponseEntity.ok(ApiResponse.ok(
                seasonalPatternService.getBestMonthToBuy(commodityId)));
    }

    @GetMapping("/{commodityId}/worst-month")
    @Operation(summary = "Get worst month to buy a commodity", 
               description = "Returns the month with highest seasonal index (most expensive)")
    public ResponseEntity<ApiResponse<SeasonalPatternDto>> getWorstMonthToBuy(
            @PathVariable Long commodityId) {
        log.info("REST request to get worst month to buy commodity: {}", commodityId);
        return ResponseEntity.ok(ApiResponse.ok(
                seasonalPatternService.getWorstMonthToBuy(commodityId)));
    }

    @GetMapping("/{commodityId}/outlook")
    @Operation(summary = "Get current month outlook for a commodity", 
               description = "Public endpoint with actionable buying advice")
    public ResponseEntity<ApiResponse<SeasonalOutlookDto>> getCurrentMonthOutlook(
            @PathVariable Long commodityId) {
        log.info("REST request to get current month outlook for commodity: {}", commodityId);
        return ResponseEntity.ok(ApiResponse.ok(
                seasonalPatternService.getCurrentMonthOutlook(commodityId)));
    }

    @PostMapping("/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recompute seasonal patterns for all commodities", 
               description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<String>> computeAllPatterns() {
        log.info("REST request to recompute all seasonal patterns");
        seasonalPatternService.computeAllPatterns();
        return ResponseEntity.ok(ApiResponse.ok("Seasonal patterns recomputed successfully"));
    }

    @PostMapping("/recompute/{commodityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recompute seasonal patterns for a specific commodity", 
               description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<String>> computePatternsForCommodity(
            @PathVariable Long commodityId) {
        log.info("REST request to recompute seasonal patterns for commodity: {}", commodityId);
        seasonalPatternService.computePatternsForCommodity(commodityId);
        return ResponseEntity.ok(ApiResponse.ok(
                String.format("Seasonal patterns recomputed for commodity %d", commodityId)));
    }
}
