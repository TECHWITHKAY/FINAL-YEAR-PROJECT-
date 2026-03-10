package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.enums.SeasonalOutlook;

import java.math.BigDecimal;

public record SeasonalPatternDto(
    Long commodityId,
    String commodityName,
    Integer monthOfYear,
    String monthName,
    BigDecimal avgPrice,
    BigDecimal seasonalIndex,
    SeasonalOutlook interpretation,
    Integer sampleSize,
    Integer dataYearFrom,
    Integer dataYearTo
) {}
