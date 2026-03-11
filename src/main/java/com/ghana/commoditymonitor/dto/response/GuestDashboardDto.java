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
    private Integer totalCommodities;
    private Integer totalMarkets;
    private Integer totalCities;
    private OffsetDateTime lastUpdated;
    private List<CommoditySummaryDto> topThreeCommoditiesByNationalAvgPrice;
    private String dataGateMessage;
}
