package com.ghana.commoditymonitor.dto.response.analytics;

import java.math.BigDecimal;

/**
 * DTO representing inflation trends between months.
 */
public record InflationTrendDto(
    Long commodityId,
    String commodityName,
    BigDecimal currentMonthAvg,
    BigDecimal lastMonthAvg,
    BigDecimal percentageChange,
    String direction // UP, DOWN, STABLE
) {}
