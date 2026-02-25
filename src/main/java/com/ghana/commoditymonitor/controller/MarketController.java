package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.MarketRequestDto;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.MarketResponseDto;
import com.ghana.commoditymonitor.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing market-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
@Tag(name = "Markets", description = "Endpoints for managing markets")
public class MarketController {

    private final MarketService marketService;

    @GetMapping
    @Operation(summary = "Get all markets")
    public ResponseEntity<ApiResponse<List<MarketResponseDto>>> getAllMarkets() {
        log.info("REST request to get all markets");
        return ResponseEntity.ok(ApiResponse.ok(marketService.getAllMarkets()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get market by ID")
    public ResponseEntity<ApiResponse<MarketResponseDto>> getMarketById(@PathVariable Long id) {
        log.info("REST request to get market by id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(marketService.getMarketById(id)));
    }

    @GetMapping("/city/{cityId}")
    @Operation(summary = "Get markets by city ID")
    public ResponseEntity<ApiResponse<List<MarketResponseDto>>> getMarketsByCity(@PathVariable Long cityId) {
        log.info("REST request to get markets for city id: {}", cityId);
        return ResponseEntity.ok(ApiResponse.ok(marketService.getMarketsByCity(cityId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new market", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<MarketResponseDto>> createMarket(@Valid @RequestBody MarketRequestDto request) {
        log.info("REST request to create market: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Market created successfully", marketService.createMarket(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing market", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<MarketResponseDto>> updateMarket(@PathVariable Long id, @Valid @RequestBody MarketRequestDto request) {
        log.info("REST request to update market with id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Market updated successfully", marketService.updateMarket(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a market", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteMarket(@PathVariable Long id) {
        log.info("REST request to delete market with id: {}", id);
        marketService.deleteMarket(id);
        return ResponseEntity.noContent().build();
    }
}
