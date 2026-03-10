package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.dto.response.analytics.CityComparisonDto;
import com.ghana.commoditymonitor.dto.response.analytics.MonthlyTrendDto;
import com.ghana.commoditymonitor.dto.response.analytics.VolatilityDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommoditySpotlightDto {
    private Long commodityId;
    private String commodityName;
    private String unit;
    private String category;
    
    private BigDecimal currentNationalAvgPrice;
    private BigDecimal priceChangePercentage;
    
    private SeasonalOutlookDto seasonalOutlook;
    
    private List<MarketHealthScoreDto> marketHealthScores;
    
    private List<MonthlyTrendDto> last6MonthsTrend;
    
    private List<CityComparisonDto> cityPriceComparison;
    
    private VolatilityDto volatilityRating;
    
    private LatestPriceDto cheapestMarket;
    private LatestPriceDto mostExpensiveMarket;
}
