package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.dto.response.analytics.VolatilityDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullDashboardDto {
    private Integer totalCommodities;
    private Integer totalMarkets;
    private Integer totalCities;
    private OffsetDateTime lastUpdated;
    private List<CommoditySummaryDto> topThreeCommoditiesByNationalAvgPrice;
    private String dataGateMessage;
    
    private List<CommodityMovementDto> topRisingCommodities;
    private List<CommodityMovementDto> topFallingCommodities;
    private List<VolatilityDto> mostVolatileCommodities;
    private Map<String, Long> marketHealthSummary;
    private Long pendingCount;
}
