package com.ghana.commoditymonitor.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response DTO for historical price record information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRecordResponseDto {
    private Long id;
    private Long commodityId;
    private String commodityName;
    private Long marketId;
    private String marketName;
    private String cityName;
    private BigDecimal price;
    private LocalDate recordedDate;
    private String source;
    private OffsetDateTime createdAt;
}
