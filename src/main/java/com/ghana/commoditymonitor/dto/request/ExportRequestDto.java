package com.ghana.commoditymonitor.dto.request;

import com.ghana.commoditymonitor.enums.ExportType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ExportRequestDto(
    List<Long> commodityIds,
    List<Long> marketIds,
    List<Long> cityIds,
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
