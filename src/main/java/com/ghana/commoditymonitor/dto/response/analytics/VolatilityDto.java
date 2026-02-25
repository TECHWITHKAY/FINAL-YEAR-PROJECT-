package com.ghana.commoditymonitor.dto.response.analytics;

import java.math.BigDecimal;

/**
 * DTO representing price volatility for a commodity.
 */
public record VolatilityDto(
    Long commodityId,
    String commodityName,
    Double stdDevPrice,
    String interpretation // LOW, MEDIUM, HIGH
) {}
