package com.ghana.commoditymonitor.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LatestPriceDto(
    Long commodityId,
    String commodityName,
    String unit,
    Long marketId,
    String marketName,
    String cityName,
    BigDecimal price,
    LocalDate recordedDate,
    Integer daysAgo
) {}
