package com.ghana.commoditymonitor.dto.response;

import java.math.BigDecimal;

public record CommoditySummaryDto(
    Long commodityId,
    String commodityName,
    String unit,
    BigDecimal nationalAvgPrice
) {}
