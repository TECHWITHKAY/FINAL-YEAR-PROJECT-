package com.ghana.commoditymonitor.dto.response;

import java.time.OffsetDateTime;

public record ExportLogDto(
    Long id,
    String username,
    String exportType,
    Integer rowCount,
    Long fileSizeKb,
    String ipAddress,
    OffsetDateTime exportedAt
) {}
