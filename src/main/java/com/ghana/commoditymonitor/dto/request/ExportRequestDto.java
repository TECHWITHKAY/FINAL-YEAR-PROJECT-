package com.ghana.commoditymonitor.dto.request;

import com.ghana.commoditymonitor.enums.ExportType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExportRequestDto(
    Long commodityId,
    Long marketId,
    Long cityId,
    LocalDate fromDate,
    LocalDate toDate,
    @NotNull(message = "Export type is required")
    ExportType exportType,
    Boolean includeAnalyticsSummary
) {
    public ExportRequestDto {
        if (includeAnalyticsSummary == null) {
            includeAnalyticsSummary = false;
        }
    }
}
