package com.ghana.commoditymonitor.dto.response.analytics;

import java.math.BigDecimal;

/**
 * DTO representing an average price comparison between cities.
 */
public record CityComparisonDto(
    String cityName,
    String commodityName,
    BigDecimal avgPrice
) {}
