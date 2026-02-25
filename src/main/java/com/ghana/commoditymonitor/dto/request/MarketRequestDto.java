package com.ghana.commoditymonitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating or updating a market.
 */
public record MarketRequestDto(
    @NotBlank(message = "Market name is required")
    String name,

    @NotNull(message = "City ID is required")
    @Positive(message = "City ID must be a positive number")
    Long cityId
) {}
