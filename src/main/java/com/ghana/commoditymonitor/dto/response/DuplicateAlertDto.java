package com.ghana.commoditymonitor.dto.response;

import java.time.LocalDate;

public record DuplicateAlertDto(
    Long commodityId,
    String commodityName,
    Long marketId,
    String marketName,
    LocalDate recordedDate,
    Integer count
) {}
