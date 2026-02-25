package com.ghana.commoditymonitor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a price record.
 */
public record PriceRecordRequestDto(
    @NotNull(message = "Commodity ID is required")
    @Positive(message = "Commodity ID must be positive")
    Long commodityId,

    @NotNull(message = "Market ID is required")
    @Positive(message = "Market ID must be positive")
    Long marketId,

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    BigDecimal price,

    @NotNull(message = "Recorded date is required")
    @PastOrPresent(message = "Recorded date cannot be in the future")
    LocalDate recordedDate,

    @Size(max = 200, message = "Source must not exceed 200 characters")
    String source
) {}
