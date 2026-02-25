package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.PriceRecordRequestDto;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.PriceRecordResponseDto;
import com.ghana.commoditymonitor.service.PriceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing historical price records.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/price-records")
@RequiredArgsConstructor
@Tag(name = "Price Records", description = "Endpoints for managing historical price data")
public class PriceRecordController {

    private final PriceRecordService priceRecordService;

    @GetMapping
    @Operation(summary = "Get all price records with pagination")
    public ResponseEntity<ApiResponse<Page<PriceRecordResponseDto>>> getAllPriceRecords(Pageable pageable) {
        log.info("REST request to get all price records");
        return ResponseEntity.ok(ApiResponse.ok(priceRecordService.getAllPriceRecords(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get price record by ID")
    public ResponseEntity<ApiResponse<PriceRecordResponseDto>> getPriceRecordById(@PathVariable Long id) {
        log.info("REST request to get price record by id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(priceRecordService.getPriceRecordById(id)));
    }

    @GetMapping("/commodity/{commodityId}")
    @Operation(summary = "Get price records by commodity ID")
    public ResponseEntity<ApiResponse<List<PriceRecordResponseDto>>> getPriceRecordsByCommodity(@PathVariable Long commodityId) {
        log.info("REST request to get price records for commodity: {}", commodityId);
        return ResponseEntity.ok(ApiResponse.ok(priceRecordService.getPriceRecordsByCommodity(commodityId)));
    }

    @GetMapping("/market/{marketId}")
    @Operation(summary = "Get price records by market ID")
    public ResponseEntity<ApiResponse<List<PriceRecordResponseDto>>> getPriceRecordsByMarket(@PathVariable Long marketId) {
        log.info("REST request to get price records for market: {}", marketId);
        return ResponseEntity.ok(ApiResponse.ok(priceRecordService.getPriceRecordsByMarket(marketId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new price record", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<PriceRecordResponseDto>> createPriceRecord(@Valid @RequestBody PriceRecordRequestDto request) {
        log.info("REST request to create price record");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Price record created successfully", priceRecordService.createPriceRecord(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing price record", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<PriceRecordResponseDto>> updatePriceRecord(@PathVariable Long id, @Valid @RequestBody PriceRecordRequestDto request) {
        log.info("REST request to update price record with id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Price record updated successfully", priceRecordService.updatePriceRecord(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a price record", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deletePriceRecord(@PathVariable Long id) {
        log.info("REST request to delete price record with id: {}", id);
        priceRecordService.deletePriceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
