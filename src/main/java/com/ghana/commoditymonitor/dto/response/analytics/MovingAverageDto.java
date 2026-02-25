package com.ghana.commoditymonitor.dto.response.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * DTO representing a moving average forecast for a commodity.
 */
public record MovingAverageDto(
    Long commodityId,
    String commodityName,
    YearMonth forecastMonth,
    BigDecimal forecastPrice,
    int basedOnMonths // always 3 as per requirement
) {}
