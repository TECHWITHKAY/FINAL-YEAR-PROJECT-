package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.enums.Direction;

import java.math.BigDecimal;

public record CommodityMovementDto(
    Long commodityId,
    String commodityName,
    String unit,
    BigDecimal currentMonthAvg,
    BigDecimal previousMonthAvg,
    BigDecimal percentageChange,
    Direction direction
) {}
