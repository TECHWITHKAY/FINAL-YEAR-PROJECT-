package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.enums.SeasonalOutlook;

import java.math.BigDecimal;

public record SeasonalOutlookDto(
    Long commodityId,
    String commodityName,
    String currentMonthName,
    BigDecimal seasonalIndex,
    SeasonalOutlook outlook,
    BigDecimal percentageFromAverage,
    String message
) {}
