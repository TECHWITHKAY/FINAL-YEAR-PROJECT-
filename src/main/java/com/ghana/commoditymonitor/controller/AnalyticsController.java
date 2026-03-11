package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.DataQualityReportDto;
import com.ghana.commoditymonitor.dto.response.analytics.*;
import com.ghana.commoditymonitor.security.CurrentUser;
import com.ghana.commoditymonitor.security.UserPrincipal;
import com.ghana.commoditymonitor.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for statistical and trend analytics.
 * <p>
 * All endpoints support optional authentication via @CurrentUser annotation.
 * Example usage:
 * <pre>
 * {@code
 * @GetMapping("/trends/{id}")
 * public ResponseEntity<?> getTrend(@PathVariable Long id,
 *                                    @CurrentUser UserPrincipal principal) {
 *     // principal is null for guests, populated for authenticated users
 *     // Service layer can provide different data based on authentication status
 *     return ResponseEntity.ok(analyticsService.getTrend(id, principal));
 * }
 * }
 * </pre>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for statistical insights and price trends")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/trends/{commodityId}")
    @Operation(summary = "Get monthly price trend", description = "Returns average prices grouped by month. Supports optional authentication.")
    public ResponseEntity<ApiResponse<List<MonthlyTrendDto>>> getMonthlyPriceTrend(
            @PathVariable Long commodityId, 
            @RequestParam(defaultValue = "12") int months,
            @CurrentUser UserPrincipal principal) {
        log.info("REST request to get monthly price trend for commodity: {} over {} months (user: {})", 
                 commodityId, months, principal != null ? principal.username() : "guest");
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getMonthlyPriceTrend(commodityId, months)));
    }

    @GetMapping("/city-comparison/{commodityId}")
    @Operation(summary = "Compare prices across cities", description = "Returns average prices for a commodity in different cities")
    public ResponseEntity<ApiResponse<List<CityComparisonDto>>> getCityPriceComparison(@PathVariable Long commodityId) {
        log.info("REST request to get city comparison for commodity: {}", commodityId);
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getCityPriceComparison(commodityId)));
    }

    @GetMapping("/volatility")
    @Operation(summary = "Get price volatility", description = "Returns standard deviation and volatility rating for all commodities")
    public ResponseEntity<ApiResponse<List<VolatilityDto>>> getPriceVolatility() {
        log.info("REST request to get price volatility for all commodities");
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getPriceVolatility()));
    }

    @GetMapping("/inflation/{commodityId}")
    @Operation(summary = "Get inflation trend", description = "Compares current month average with previous month")
    public ResponseEntity<ApiResponse<InflationTrendDto>> getInflationTrend(@PathVariable Long commodityId) {
        log.info("REST request to get inflation trend for commodity: {}", commodityId);
        return analyticsService.getInflationTrend(commodityId)
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/forecast/{commodityId}")
    @Operation(summary = "Get price forecast", description = "Returns a 3-month moving average prediction for next month")
    public ResponseEntity<ApiResponse<MovingAverageDto>> getMovingAverageForecast(@PathVariable Long commodityId) {
        log.info("REST request to get price forecast for commodity: {}", commodityId);
        return analyticsService.getMovingAverageForecast(commodityId)
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/data-quality")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get data quality report", 
               description = "Comprehensive data quality analysis for administrators and analysts")
    public ResponseEntity<ApiResponse<DataQualityReportDto>> getDataQualityReport() {
        log.info("REST request to generate data quality report");
        DataQualityReportDto report = analyticsService.generateDataQualityReport();
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
