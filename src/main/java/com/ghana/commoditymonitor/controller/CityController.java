package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.CityRequestDto;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.CityResponseDto;
import com.ghana.commoditymonitor.service.CityService;
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
 * Controller for managing city-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
@Tag(name = "Cities", description = "Endpoints for managing cities")
public class CityController {

    private final CityService cityService;

    @GetMapping
    @Operation(summary = "Get all cities")
    public ResponseEntity<ApiResponse<List<CityResponseDto>>> getAllCities() {
        log.info("REST request to get all cities");
        return ResponseEntity.ok(ApiResponse.ok(cityService.getAllCities()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get city by ID")
    public ResponseEntity<ApiResponse<CityResponseDto>> getCityById(@PathVariable Long id) {
        log.info("REST request to get city by id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(cityService.getCityById(id)));
    }

    @GetMapping("/region/{region}")
    @Operation(summary = "Get cities by region")
    public ResponseEntity<ApiResponse<List<CityResponseDto>>> getCitiesByRegion(@PathVariable String region) {
        log.info("REST request to get cities by region: {}", region);
        return ResponseEntity.ok(ApiResponse.ok(cityService.getCitiesByRegion(region)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new city", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<CityResponseDto>> createCity(@Valid @RequestBody CityRequestDto request) {
        log.info("REST request to create city: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("City created successfully", cityService.createCity(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing city", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<CityResponseDto>> updateCity(@PathVariable Long id, @Valid @RequestBody CityRequestDto request) {
        log.info("REST request to update city with id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("City updated successfully", cityService.updateCity(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a city", description = "Accessible by ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteCity(@PathVariable Long id) {
        log.info("REST request to delete city with id: {}", id);
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
