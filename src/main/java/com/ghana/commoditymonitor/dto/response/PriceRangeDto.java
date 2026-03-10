package com.ghana.commoditymonitor.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRangeDto(
    Long commodityId,
    String commodityName,
    String unit,
    String cityName,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal avgPrice,
    BigDecimal medianPrice,
    Integer dataPointCount,
    LocalDate dateFrom,
    LocalDate dateTo,
    String guestNote
) {}
