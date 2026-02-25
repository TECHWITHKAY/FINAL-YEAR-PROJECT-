package com.ghana.commoditymonitor.dto.response.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * DTO representing a monthly average price trend for a commodity.
 */
public record MonthlyTrendDto(
    Long commodityId,
    String commodityName,
    YearMonth month,
    BigDecimal avgPrice
) {}
