package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.response.*;
import com.ghana.commoditymonitor.security.CurrentUser;
import com.ghana.commoditymonitor.security.UserPrincipal;
import com.ghana.commoditymonitor.service.PublicDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Market", description = "Guest-aware analytics endpoints for frontend dashboard")
public class PublicMarketController {

    private final PublicDashboardService publicDashboardService;

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Get dashboard summary", 
               description = "Guests see limited data with gate message, authenticated users see full analytics")
    public ResponseEntity<ApiResponse<Object>> getDashboardSummary(@CurrentUser UserPrincipal principal) {
        log.info("REST request to get dashboard summary (user: {})", 
                 principal != null ? principal.username() : "guest");
        
        Object summary = publicDashboardService.getDashboardSummary(principal);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/latest-prices")
    @Operation(summary = "Get latest prices", 
               description = "Guests: national average only, last 7 days. Authenticated: market-level, 30 days")
    public ResponseEntity<ApiResponse<List<LatestPriceDto>>> getLatestPrices(
            @RequestParam(required = false) Long commodityId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(defaultValue = "50") Integer limit,
            @CurrentUser UserPrincipal principal) {
        
        log.info("REST request to get latest prices (commodityId: {}, cityId: {}, user: {})",
                 commodityId, cityId, principal != null ? principal.username() : "guest");
        
        List<LatestPriceDto> prices = publicDashboardService.getLatestPrices(
                commodityId, cityId, limit, principal);
        
        return ResponseEntity.ok(ApiResponse.ok(prices));
    }

    @GetMapping("/price-range/{commodityId}")
    @Operation(summary = "Get price range for a commodity", 
               description = "Guests: national range, last 7 days. Authenticated: per-city breakdown, full date range")
    public ResponseEntity<ApiResponse<PriceRangeDto>> getPriceRange(
            @PathVariable Long commodityId,
            @RequestParam(required = false) Long cityId,
            @CurrentUser UserPrincipal principal) {
        
        log.info("REST request to get price range for commodity: {} (cityId: {}, user: {})",
                 commodityId, cityId, principal != null ? principal.username() : "guest");
        
        PriceRangeDto priceRange = publicDashboardService.getPriceRange(commodityId, cityId, principal);
        
        if (priceRange == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        
        return ResponseEntity.ok(ApiResponse.ok(priceRange));
    }

    @GetMapping("/commodity-spotlight/{commodityId}")
    @Operation(summary = "Get comprehensive commodity spotlight", 
               description = "Authenticated users only. Returns rich single-commodity view with all analytics")
    public ResponseEntity<ApiResponse<CommoditySpotlightDto>> getCommoditySpotlight(
            @PathVariable Long commodityId,
            @CurrentUser UserPrincipal principal) {
        
        if (principal == null) {
            log.warn("Guest attempted to access commodity spotlight for commodity: {}", commodityId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Full commodity spotlight is available to registered users. Create a free account."));
        }
        
        log.info("REST request to get commodity spotlight for commodity: {} (user: {})",
                 commodityId, principal.username());
        
        CommoditySpotlightDto spotlight = publicDashboardService.getCommoditySpotlight(commodityId, principal);
        return ResponseEntity.ok(ApiResponse.ok(spotlight));
    }
}
