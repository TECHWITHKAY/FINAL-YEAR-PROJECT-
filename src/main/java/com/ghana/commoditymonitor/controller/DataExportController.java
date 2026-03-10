package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.ExportRequestDto;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.ExportLogDto;
import com.ghana.commoditymonitor.security.CurrentUser;
import com.ghana.commoditymonitor.security.UserPrincipal;
import com.ghana.commoditymonitor.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Data Export", description = "Endpoints for exporting price records to CSV and Excel")
public class DataExportController {

    private final DataExportService dataExportService;

    @PostMapping("/price-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'FIELD_AGENT')")
    @Operation(summary = "Export price records to CSV or Excel", 
               description = "Accessible by ADMIN, ANALYST, and FIELD_AGENT roles only")
    public ResponseEntity<byte[]> exportPriceRecords(
            @Valid @RequestBody ExportRequestDto request,
            @CurrentUser UserPrincipal principal,
            HttpServletRequest httpRequest) {

        String ipAddress = extractIpAddress(httpRequest);
        
        log.info("REST request to export price records: type={}, user={}, ip={}", 
                 request.exportType(), principal.username(), ipAddress);

        DataExportService.ExportResult result = dataExportService.exportPriceRecords(
            request, principal, ipAddress
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, result.contentType());
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return new ResponseEntity<>(result.data(), headers, HttpStatus.OK);
    }

    @GetMapping("/my-exports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my export history", 
               description = "Returns export history for the current authenticated user")
    public ResponseEntity<ApiResponse<Page<ExportLogDto>>> getMyExportHistory(
            @CurrentUser UserPrincipal principal,
            Pageable pageable) {

        log.info("REST request to get export history for user: {}", principal.username());
        
        Page<ExportLogDto> history = dataExportService.getMyExportHistory(
            principal.id(), pageable
        );

        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/all-exports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all export history", 
               description = "Returns export history for all users (ADMIN only)")
    public ResponseEntity<ApiResponse<Page<ExportLogDto>>> getAllExportHistory(Pageable pageable) {
        log.info("REST request to get all export history");
        
        Page<ExportLogDto> history = dataExportService.getAllExportHistory(pageable);
        
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
