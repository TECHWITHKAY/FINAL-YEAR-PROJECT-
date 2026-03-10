package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.MarketHealthScoreDto;
import com.ghana.commoditymonitor.entity.MarketHealthScore;
import com.ghana.commoditymonitor.security.CurrentUser;
import com.ghana.commoditymonitor.security.UserPrincipal;
import com.ghana.commoditymonitor.service.MarketHealthScoreService;
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
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Market Health", description = "Endpoints for market health score monitoring")
public class MarketHealthController {

    private final MarketHealthScoreService healthScoreService;

    @GetMapping
    @Operation(summary = "Get all latest market health scores", 
               description = "Returns grade only for guests, full details for authenticated users")
    public ResponseEntity<ApiResponse<List<MarketHealthScoreDto>>> getAllLatestScores(
            @CurrentUser UserPrincipal principal) {
        log.info("REST request to get all latest health scores (user: {})", 
                 principal != null ? principal.username() : "guest");
        return ResponseEntity.ok(ApiResponse.ok(healthScoreService.getAllLatestScores(principal)));
    }

    @GetMapping("/{marketId}")
    @Operation(summary = "Get latest health score for a specific market")
    public ResponseEntity<ApiResponse<MarketHealthScoreDto>> getLatestScoreForMarket(
            @PathVariable Long marketId,
            @CurrentUser UserPrincipal principal) {
        log.info("REST request to get health score for market: {} (user: {})", 
                 marketId, principal != null ? principal.username() : "guest");
        return ResponseEntity.ok(ApiResponse.ok(
                healthScoreService.getLatestScoreForMarket(marketId, principal)));
    }

    @GetMapping("/top")
    @Operation(summary = "Get top performing markets", 
               description = "Returns markets with highest health scores")
    public ResponseEntity<ApiResponse<List<MarketHealthScoreDto>>> getTopPerformingMarkets(
            @RequestParam(defaultValue = "5") int limit,
            @CurrentUser UserPrincipal principal) {
        log.info("REST request to get top {} performing markets", limit);
        return ResponseEntity.ok(ApiResponse.ok(
                healthScoreService.getTopPerformingMarkets(limit, principal)));
    }

    @GetMapping("/underperforming")
    @Operation(summary = "Get underperforming markets", 
               description = "Returns markets with grade D or F")
    public ResponseEntity<ApiResponse<List<MarketHealthScoreDto>>> getUnderperformingMarkets(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser UserPrincipal principal) {
        log.info("REST request to get underperforming markets");
        return ResponseEntity.ok(ApiResponse.ok(
                healthScoreService.getUnderperformingMarkets(limit, principal)));
    }

    @PostMapping("/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recompute health scores for all markets", 
               description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<String>> computeAllMarketScores() {
        log.info("REST request to recompute all market health scores");
        List<MarketHealthScore> scores = healthScoreService.computeAllMarketScores();
        return ResponseEntity.ok(ApiResponse.ok(
                String.format("Successfully computed health scores for %d markets", scores.size())));
    }
}
