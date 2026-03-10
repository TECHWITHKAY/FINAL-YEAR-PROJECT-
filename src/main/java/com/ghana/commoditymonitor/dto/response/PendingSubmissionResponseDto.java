package com.ghana.commoditymonitor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class PendingSubmissionResponseDto {
    private Long id;
    private Long commodityId;
    private String commodityName;
    private Long marketId;
    private String marketName;
    private String cityName;
    private BigDecimal price;
    private LocalDate recordedDate;
    private String source;
    private String status;
    private String submittedByUsername;
    private Long daysPending;
    private OffsetDateTime createdAt;
}
