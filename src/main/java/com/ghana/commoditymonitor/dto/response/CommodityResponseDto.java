package com.ghana.commoditymonitor.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Response DTO for commodity information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommodityResponseDto {
    private Long id;
    private String name;
    private String category;
    private String unit;
    private OffsetDateTime createdAt;
}
