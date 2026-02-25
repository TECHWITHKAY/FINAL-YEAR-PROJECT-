package com.ghana.commoditymonitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a city.
 */
public record CityRequestDto(
    @NotBlank(message = "City name is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Region name is required")
    @Size(max = 100, message = "Region name must not exceed 100 characters")
    String region
) {}
