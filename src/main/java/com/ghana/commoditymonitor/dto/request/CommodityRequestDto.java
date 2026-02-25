package com.ghana.commoditymonitor.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating or updating a commodity.
 */
public record CommodityRequestDto(
    @NotBlank(message = "Commodity name is required")
    String name,

    @NotBlank(message = "Category is required")
    String category,

    @NotBlank(message = "Unit is required")
    String unit
) {}
