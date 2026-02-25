package com.ghana.commoditymonitor.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Response DTO for city information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponseDto {
    private Long id;
    private String name;
    private String region;
    private OffsetDateTime createdAt;
}
