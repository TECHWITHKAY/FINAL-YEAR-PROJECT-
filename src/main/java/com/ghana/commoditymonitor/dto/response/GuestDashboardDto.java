package com.ghana.commoditymonitor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestDashboardDto {
    private Integer totalCommoditiesTracked;
    private Integer totalMarketsTracked;
    private Integer totalCitiesTracked;
    private OffsetDateTime lastDataUpdateAt;
    private List<CommoditySummaryDto> topThreeCommoditiesByNationalAvgPrice;
    private String dataGateMessage;
}
