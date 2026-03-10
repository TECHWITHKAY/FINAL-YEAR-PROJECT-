package com.ghana.commoditymonitor.dto.response.analytics;

import com.ghana.commoditymonitor.enums.Direction;

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
    Direction direction
) {}
