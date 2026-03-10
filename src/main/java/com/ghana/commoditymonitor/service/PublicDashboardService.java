package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.response.*;
import com.ghana.commoditymonitor.dto.response.analytics.CityComparisonDto;
import com.ghana.commoditymonitor.dto.response.analytics.MonthlyTrendDto;
import com.ghana.commoditymonitor.dto.response.analytics.VolatilityDto;
import com.ghana.commoditymonitor.enums.Direction;
import com.ghana.commoditymonitor.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicDashboardService {

    private final EntityManager entityManager;
    private final SeasonalPatternService seasonalPatternService;
    private final MarketHealthScoreService marketHealthScoreService;
    private final AnalyticsService analyticsService;
    private final com.ghana.commoditymonitor.repository.CommodityRepository commodityRepository;

    @Cacheable("dashboardSummary")
    public Object getDashboardSummary(UserPrincipal principal) {
        log.debug("Building dashboard summary for user: {}", 
                  principal != null ? principal.username() : "guest");
        
        if (principal == null) {
            return buildGuestDashboard();
        }
        return buildFullDashboard(principal);
    }

    private GuestDashboardDto buildGuestDashboard() {
        String countSql = """
            SELECT 
                (SELECT COUNT(*) FROM commodities) AS commodity_count,
                (SELECT COUNT(*) FROM markets) AS market_count,
                (SELECT COUNT(*) FROM cities) AS city_count,
                (SELECT MAX(recorded_date) FROM price_records WHERE status = 'APPROVED') AS last_update
            """;

        Query countQuery = entityManager.createNativeQuery(countSql);
        Object[] counts = (Object[]) countQuery.getSingleResult();

        String topCommoditiesSql = """
            SELECT 
                c.id,
                c.name,
                c.unit,
                AVG(pr.price) AS avg_price
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            WHERE pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '7 days'
            GROUP BY c.id, c.name, c.unit
            ORDER BY avg_price DESC
            LIMIT 3
            """;

        Query topQuery = entityManager.createNativeQuery(topCommoditiesSql);
        @SuppressWarnings("unchecked")
        List<Object[]> topResults = topQuery.getResultList();

        List<CommoditySummaryDto> topCommodities = topResults.stream()
                .map(row -> new CommoditySummaryDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP)
                ))
                .collect(Collectors.toList());

        return GuestDashboardDto.builder()
                .totalCommoditiesTracked(((Number) counts[0]).intValue())
                .totalMarketsTracked(((Number) counts[1]).intValue())
                .totalCitiesTracked(((Number) counts[2]).intValue())
                .lastDataUpdateAt(counts[3] != null ? 
                    ((java.sql.Date) counts[3]).toLocalDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC) : null)
                .topThreeCommoditiesByNationalAvgPrice(topCommodities)
                .dataGateMessage("Sign in to access full price history, city breakdowns, and export tools.")
                .build();
    }

    private FullDashboardDto buildFullDashboard(UserPrincipal principal) {
        GuestDashboardDto guestData = buildGuestDashboard();

        List<CommodityMovementDto> topRising = getTopMovingCommodities(true);
        List<CommodityMovementDto> topFalling = getTopMovingCommodities(false);
        List<VolatilityDto> mostVolatile = getMostVolatileCommodities();
        Map<String, Long> healthSummary = getMarketHealthSummary();
        Long pendingCount = principal.isAdmin() ? getPendingSubmissionsCount() : null;

        return FullDashboardDto.builder()
                .totalCommoditiesTracked(guestData.getTotalCommoditiesTracked())
                .totalMarketsTracked(guestData.getTotalMarketsTracked())
                .totalCitiesTracked(guestData.getTotalCitiesTracked())
                .lastDataUpdateAt(guestData.getLastDataUpdateAt())
                .topThreeCommoditiesByNationalAvgPrice(guestData.getTopThreeCommoditiesByNationalAvgPrice())
                .dataGateMessage(null)
                .topRisingCommodities(topRising)
                .topFallingCommodities(topFalling)
                .mostVolatileCommodities(mostVolatile)
                .marketHealthSummary(healthSummary)
                .pendingSubmissionsCount(pendingCount)
                .build();
    }

    private List<CommodityMovementDto> getTopMovingCommodities(boolean rising) {
        String sql = """
            WITH current_month AS (
                SELECT 
                    pr.commodity_id,
                    AVG(pr.price) AS avg_price
                FROM price_records pr
                WHERE pr.status = 'APPROVED'
                  AND pr.recorded_date >= DATE_TRUNC('month', CURRENT_DATE)
                GROUP BY pr.commodity_id
            ),
            previous_month AS (
                SELECT 
                    pr.commodity_id,
                    AVG(pr.price) AS avg_price
                FROM price_records pr
                WHERE pr.status = 'APPROVED'
                  AND pr.recorded_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
                  AND pr.recorded_date < DATE_TRUNC('month', CURRENT_DATE)
                GROUP BY pr.commodity_id
            )
            SELECT 
                c.id,
                c.name,
                c.unit,
                cm.avg_price AS current_avg,
                pm.avg_price AS previous_avg,
                ((cm.avg_price - pm.avg_price) / NULLIF(pm.avg_price, 0) * 100) AS pct_change
            FROM current_month cm
            JOIN previous_month pm ON cm.commodity_id = pm.commodity_id
            JOIN commodities c ON cm.commodity_id = c.id
            WHERE pm.avg_price > 0
            ORDER BY pct_change """ + (rising ? "DESC" : "ASC") + """
            LIMIT 3
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> {
                    BigDecimal currentAvg = BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal previousAvg = BigDecimal.valueOf(((Number) row[4]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal pctChange = BigDecimal.valueOf(((Number) row[5]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
                    
                    Direction direction;
                    if (pctChange.compareTo(BigDecimal.valueOf(5)) > 0) {
                        direction = Direction.UP;
                    } else if (pctChange.compareTo(BigDecimal.valueOf(-5)) < 0) {
                        direction = Direction.DOWN;
                    } else {
                        direction = Direction.STABLE;
                    }

                    return new CommodityMovementDto(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            (String) row[2],
                            currentAvg,
                            previousAvg,
                            pctChange,
                            direction
                    );
                })
                .collect(Collectors.toList());
    }

    private List<VolatilityDto> getMostVolatileCommodities() {
        String sql = """
            SELECT 
                c.id,
                c.name,
                c.unit,
                COALESCE(STDDEV(pr.price), 0) AS std_dev,
                AVG(pr.price) AS avg_price,
                COUNT(*) AS data_points
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            WHERE pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY c.id, c.name, c.unit
            HAVING COUNT(*) >= 5
            ORDER BY std_dev DESC
            LIMIT 3
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> {
                    double stdDev = ((Number) row[3]).doubleValue();
                    String interpretation;
                    if (stdDev > 10) {
                        interpretation = "HIGH";
                    } else if (stdDev > 5) {
                        interpretation = "MEDIUM";
                    } else {
                        interpretation = "LOW";
                    }
                    
                    return new VolatilityDto(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            stdDev,
                            interpretation
                    );
                })
                .collect(Collectors.toList());
    }

    private Map<String, Long> getMarketHealthSummary() {
        String sql = """
            WITH latest_scores AS (
                SELECT DISTINCT ON (market_id)
                    market_id,
                    grade
                FROM market_health_scores
                ORDER BY market_id, computed_at DESC
            )
            SELECT grade, COUNT(*) AS count
            FROM latest_scores
            GROUP BY grade
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<String, Long> summary = new HashMap<>();
        summary.put("A", 0L);
        summary.put("B", 0L);
        summary.put("C", 0L);
        summary.put("D", 0L);
        summary.put("F", 0L);

        for (Object[] row : results) {
            String grade = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            summary.put(grade, count);
        }

        return summary;
    }

    private Long getPendingSubmissionsCount() {
        String sql = "SELECT COUNT(*) FROM price_records WHERE status = 'PENDING'";
        Query query = entityManager.createNativeQuery(sql);
        return ((Number) query.getSingleResult()).longValue();
    }

    public List<LatestPriceDto> getLatestPrices(Long commodityId, Long cityId, Integer limit, UserPrincipal principal) {
        int daysBack = (principal == null) ? 7 : 30;
        
        String sql = """
            SELECT DISTINCT ON (pr.commodity_id, pr.market_id)
                pr.commodity_id,
                c.name AS commodity_name,
                c.unit,
                pr.market_id,
                m.name AS market_name,
                ci.name AS city_name,
                pr.price,
                pr.recorded_date,
                CURRENT_DATE - pr.recorded_date AS days_ago
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            JOIN markets m ON pr.market_id = m.id
            JOIN cities ci ON m.city_id = ci.id
            WHERE pr.status = 'APPROVED'
              AND (:commodityId IS NULL OR pr.commodity_id = :commodityId)
              AND (:cityId IS NULL OR ci.id = :cityId)
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL ':daysBack days'
            ORDER BY pr.commodity_id, pr.market_id, pr.recorded_date DESC
            LIMIT :limit
            """.replace(":daysBack", String.valueOf(daysBack));

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        query.setParameter("cityId", cityId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new LatestPriceDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        principal != null ? ((Number) row[3]).longValue() : null,
                        principal != null ? (String) row[4] : null,
                        (String) row[5],
                        BigDecimal.valueOf(((Number) row[6]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                        ((java.sql.Date) row[7]).toLocalDate(),
                        ((Number) row[8]).intValue()
                ))
                .collect(Collectors.toList());
    }

    public PriceRangeDto getPriceRange(Long commodityId, Long cityId, UserPrincipal principal) {
        int daysBack = (principal == null) ? 7 : 365;
        
        String sql = """
            SELECT
                MIN(pr.price) AS min_price,
                MAX(pr.price) AS max_price,
                AVG(pr.price) AS avg_price,
                PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY pr.price) AS median_price,
                COUNT(*) AS data_point_count,
                MIN(pr.recorded_date) AS date_from,
                MAX(pr.recorded_date) AS date_to,
                c.name AS commodity_name,
                c.unit,
                ci.name AS city_name
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            JOIN markets m ON pr.market_id = m.id
            LEFT JOIN cities ci ON m.city_id = ci.id
            WHERE pr.commodity_id = :commodityId
              AND pr.status = 'APPROVED'
              AND (:cityId IS NULL OR m.city_id = :cityId)
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL ':daysBack days'
            GROUP BY c.name, c.unit, ci.name
            """.replace(":daysBack", String.valueOf(daysBack));

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        query.setParameter("cityId", cityId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return null;
        }

        Object[] row = results.get(0);
        String guestNote = (principal == null) ? "Showing last 7 days. Sign in for full history." : null;

        return new PriceRangeDto(
                commodityId,
                (String) row[7],
                (String) row[8],
                (String) row[9],
                BigDecimal.valueOf(((Number) row[0]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(((Number) row[1]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(((Number) row[2]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                ((Number) row[4]).intValue(),
                ((java.sql.Date) row[5]).toLocalDate(),
                ((java.sql.Date) row[6]).toLocalDate(),
                guestNote
        );
    }

    public CommoditySpotlightDto getCommoditySpotlight(Long commodityId, UserPrincipal principal) {
        var commodity = commodityRepository.findById(commodityId)
                .orElseThrow(() -> new com.ghana.commoditymonitor.exception.ResourceNotFoundException("Commodity", "id", commodityId));

        BigDecimal currentNationalAvg = getCurrentNationalAverage(commodityId);
        BigDecimal priceChangePercentage = getPriceChangePercentage(commodityId);
        
        SeasonalOutlookDto seasonalOutlook = seasonalPatternService.getCurrentMonthOutlook(commodityId);
        
        List<MarketHealthScoreDto> marketHealthScores = getMarketHealthScoresForCommodity(commodityId, principal);
        
        List<MonthlyTrendDto> last6MonthsTrend = analyticsService.getMonthlyPriceTrend(commodityId, 6);
        
        List<CityComparisonDto> cityComparison = analyticsService.getCityPriceComparison(commodityId);
        
        VolatilityDto volatility = getCommodityVolatility(commodityId);
        
        LatestPriceDto cheapest = getCheapestMarket(commodityId);
        LatestPriceDto mostExpensive = getMostExpensiveMarket(commodityId);

        return CommoditySpotlightDto.builder()
                .commodityId(commodity.getId())
                .commodityName(commodity.getName())
                .unit(commodity.getUnit())
                .category(commodity.getCategory())
                .currentNationalAvgPrice(currentNationalAvg)
                .priceChangePercentage(priceChangePercentage)
                .seasonalOutlook(seasonalOutlook)
                .marketHealthScores(marketHealthScores)
                .last6MonthsTrend(last6MonthsTrend)
                .cityPriceComparison(cityComparison)
                .volatilityRating(volatility)
                .cheapestMarket(cheapest)
                .mostExpensiveMarket(mostExpensive)
                .build();
    }

    private BigDecimal getCurrentNationalAverage(Long commodityId) {
        String sql = """
            SELECT AVG(price)
            FROM price_records
            WHERE commodity_id = :commodityId
              AND status = 'APPROVED'
              AND recorded_date >= CURRENT_DATE - INTERVAL '30 days'
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        
        Object result = query.getSingleResult();
        return result != null ? 
            BigDecimal.valueOf(((Number) result).doubleValue()).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
    }

    private BigDecimal getPriceChangePercentage(Long commodityId) {
        String sql = """
            WITH current_month AS (
                SELECT AVG(price) AS avg_price
                FROM price_records
                WHERE commodity_id = :commodityId
                  AND status = 'APPROVED'
                  AND recorded_date >= DATE_TRUNC('month', CURRENT_DATE)
            ),
            previous_month AS (
                SELECT AVG(price) AS avg_price
                FROM price_records
                WHERE commodity_id = :commodityId
                  AND status = 'APPROVED'
                  AND recorded_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
                  AND recorded_date < DATE_TRUNC('month', CURRENT_DATE)
            )
            SELECT 
                ((cm.avg_price - pm.avg_price) / NULLIF(pm.avg_price, 0) * 100) AS pct_change
            FROM current_month cm, previous_month pm
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        
        Object result = query.getSingleResult();
        return result != null ? 
            BigDecimal.valueOf(((Number) result).doubleValue()).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
    }

    private List<MarketHealthScoreDto> getMarketHealthScoresForCommodity(Long commodityId, UserPrincipal principal) {
        String sql = """
            SELECT DISTINCT m.id
            FROM markets m
            JOIN price_records pr ON pr.market_id = m.id
            WHERE pr.commodity_id = :commodityId
              AND pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        
        @SuppressWarnings("unchecked")
        List<Number> marketIds = query.getResultList();

        return marketIds.stream()
                .map(id -> marketHealthScoreService.getLatestScoreForMarket(id.longValue(), principal))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private VolatilityDto getCommodityVolatility(Long commodityId) {
        String sql = """
            SELECT 
                c.id,
                c.name,
                c.unit,
                COALESCE(STDDEV(pr.price), 0) AS std_dev,
                AVG(pr.price) AS avg_price,
                COUNT(*) AS data_points
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            WHERE pr.commodity_id = :commodityId
              AND pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY c.id, c.name, c.unit
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return null;
        }

        Object[] row = results.get(0);
        double stdDev = ((Number) row[3]).doubleValue();
        String interpretation;
        if (stdDev > 10) {
            interpretation = "HIGH";
        } else if (stdDev > 5) {
            interpretation = "MEDIUM";
        } else {
            interpretation = "LOW";
        }
        
        return new VolatilityDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                stdDev,
                interpretation
        );
    }

    private LatestPriceDto getCheapestMarket(Long commodityId) {
        String sql = """
            SELECT DISTINCT ON (pr.market_id)
                pr.commodity_id,
                c.name AS commodity_name,
                c.unit,
                pr.market_id,
                m.name AS market_name,
                ci.name AS city_name,
                pr.price,
                pr.recorded_date,
                CURRENT_DATE - pr.recorded_date AS days_ago
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            JOIN markets m ON pr.market_id = m.id
            JOIN cities ci ON m.city_id = ci.id
            WHERE pr.commodity_id = :commodityId
              AND pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '7 days'
            ORDER BY pr.market_id, pr.recorded_date DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new LatestPriceDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        (String) row[4],
                        (String) row[5],
                        BigDecimal.valueOf(((Number) row[6]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                        ((java.sql.Date) row[7]).toLocalDate(),
                        ((Number) row[8]).intValue()
                ))
                .min(Comparator.comparing(LatestPriceDto::price))
                .orElse(null);
    }

    private LatestPriceDto getMostExpensiveMarket(Long commodityId) {
        String sql = """
            SELECT DISTINCT ON (pr.market_id)
                pr.commodity_id,
                c.name AS commodity_name,
                c.unit,
                pr.market_id,
                m.name AS market_name,
                ci.name AS city_name,
                pr.price,
                pr.recorded_date,
                CURRENT_DATE - pr.recorded_date AS days_ago
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            JOIN markets m ON pr.market_id = m.id
            JOIN cities ci ON m.city_id = ci.id
            WHERE pr.commodity_id = :commodityId
              AND pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '7 days'
            ORDER BY pr.market_id, pr.recorded_date DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new LatestPriceDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        (String) row[4],
                        (String) row[5],
                        BigDecimal.valueOf(((Number) row[6]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                        ((java.sql.Date) row[7]).toLocalDate(),
                        ((Number) row[8]).intValue()
                ))
                .max(Comparator.comparing(LatestPriceDto::price))
                .orElse(null);
    }
}
