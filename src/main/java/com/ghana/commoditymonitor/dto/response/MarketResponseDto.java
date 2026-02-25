package com.ghana.commoditymonitor.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Response DTO for market information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketResponseDto {
    private Long id;
    private String name;
    private Long cityId;
    private String cityName;
    private OffsetDateTime createdAt;
}
