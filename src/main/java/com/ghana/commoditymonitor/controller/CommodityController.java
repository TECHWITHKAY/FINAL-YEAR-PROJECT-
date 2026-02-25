package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.CommodityRequestDto;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.CommodityResponseDto;
import com.ghana.commoditymonitor.service.CommodityService;
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
 * Controller for managing commodity-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/commodities")
@RequiredArgsConstructor
@Tag(name = "Commodities", description = "Endpoints for managing commodities")
public class CommodityController {

    private final CommodityService commodityService;

    @GetMapping
    @Operation(summary = "Get all commodities")
    public ResponseEntity<ApiResponse<List<CommodityResponseDto>>> getAllCommodities() {
        log.info("REST request to get all commodities");
        return ResponseEntity.ok(ApiResponse.ok(commodityService.getAllCommodities()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get commodity by ID")
    public ResponseEntity<ApiResponse<CommodityResponseDto>> getCommodityById(@PathVariable Long id) {
        log.info("REST request to get commodity by id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(commodityService.getCommodityById(id)));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get commodities by category")
    public ResponseEntity<ApiResponse<List<CommodityResponseDto>>> getCommoditiesByCategory(@PathVariable String category) {
        log.info("REST request to get commodities by category: {}", category);
        return ResponseEntity.ok(ApiResponse.ok(commodityService.getCommoditiesByCategory(category)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new commodity", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<CommodityResponseDto>> createCommodity(@Valid @RequestBody CommodityRequestDto request) {
        log.info("REST request to create commodity: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Commodity created successfully", commodityService.createCommodity(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing commodity", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<CommodityResponseDto>> updateCommodity(@PathVariable Long id, @Valid @RequestBody CommodityRequestDto request) {
        log.info("REST request to update commodity with id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Commodity updated successfully", commodityService.updateCommodity(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a commodity", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteCommodity(@PathVariable Long id) {
        log.info("REST request to delete commodity with id: {}", id);
        commodityService.deleteCommodity(id);
        return ResponseEntity.noContent().build();
    }
}
