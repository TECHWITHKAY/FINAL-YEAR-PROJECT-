package com.ghana.commoditymonitor.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OutlierAlertDto(
    Long priceRecordId,
    String commodityName,
    String marketName,
    BigDecimal price,
    BigDecimal marketMeanPrice,
    BigDecimal zScore,
    LocalDate recordedDate
) {}
