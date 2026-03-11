package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.response.*;
import com.ghana.commoditymonitor.dto.response.analytics.*;
import com.ghana.commoditymonitor.enums.Direction;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.CommodityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final EntityManager entityManager;
    private final CommodityRepository commodityRepository;

    @SuppressWarnings("unchecked")
    public List<MonthlyTrendDto> getMonthlyPriceTrend(Long commodityId, int months) {
        log.info("Calculating monthly price trend for commodity: {} over {} months", commodityId, months);
        validateCommodity(commodityId);

        String sql = """
                SELECT pr.commodity_id, co.name, DATE_TRUNC('month', pr.recorded_date) AS month, AVG(pr.price) AS avg_price
                FROM price_records pr
                JOIN commodities co ON pr.commodity_id = co.id
                WHERE pr.commodity_id = :id 
                  AND pr.recorded_date >= NOW() - CAST(:months || ' months' AS INTERVAL)
                  AND pr.status = 'APPROVED'
                GROUP BY pr.commodity_id, co.name, month
                ORDER BY month ASC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", commodityId);
        query.setParameter("months", months);

        List<Object[]> results = query.getResultList();

        List<MonthlyTrendDto> response = new ArrayList<>();

        for (Object[] row : results) {
            Timestamp ts = (Timestamp) row[2];
            response.add(new MonthlyTrendDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    YearMonth.from(ts.toLocalDateTime()),
                    BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    public List<CityComparisonDto> getCityPriceComparison(Long commodityId) {
        log.info("Comparing city prices for commodity: {}", commodityId);
        validateCommodity(commodityId);

        String sql = """
                SELECT c.name AS city_name, co.name as commodity_name, AVG(pr.price) AS avg_price
                FROM price_records pr
                JOIN markets m ON pr.market_id = m.id
                JOIN cities c ON m.city_id = c.id
                JOIN commodities co ON pr.commodity_id = co.id
                WHERE pr.commodity_id = :id
                  AND pr.status = 'APPROVED'
                GROUP BY c.name, co.name
                ORDER BY avg_price DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", commodityId);

        List<Object[]> results = query.getResultList();
        List<CityComparisonDto> response = new ArrayList<>();

        for (Object[] row : results) {
            response.add(new CityComparisonDto(
                    (String) row[0],
                    (String) row[1],
                    BigDecimal.valueOf(((Number) row[2]).doubleValue()).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    public List<VolatilityDto> getPriceVolatility() {
        log.info("Calculating price volatility for all commodities");

        String sql = """
                SELECT pr.commodity_id, co.name, STDDEV(pr.price) AS std_dev
                FROM price_records pr
                JOIN commodities co ON pr.commodity_id = co.id
                WHERE pr.status = 'APPROVED'
                GROUP BY pr.commodity_id, co.name
                ORDER BY std_dev DESC
                """;


        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<VolatilityDto> response = new ArrayList<>();

        for (Object[] row : results) {
            double stdDev = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            String interpretation = stdDev < 5 ? "LOW" : (stdDev <= 20 ? "MEDIUM" : "HIGH");

            response.add(new VolatilityDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    stdDev,
                    interpretation
            ));
        }
        return response;
    }

    public Optional<InflationTrendDto> getInflationTrend(Long commodityId) {
        log.info("Calculating inflation trend for commodity: {}", commodityId);
        validateCommodity(commodityId);

        String sql = """
                WITH monthly_avgs AS (
                    SELECT DATE_TRUNC('month', recorded_date) as month, AVG(price) as avg_price
                    FROM price_records
                    WHERE commodity_id = :id
                      AND status = 'APPROVED'
                    GROUP BY month
                )
                SELECT 
                    (SELECT avg_price FROM monthly_avgs WHERE month = DATE_TRUNC('month', NOW())) as current_avg,
                    (SELECT avg_price FROM monthly_avgs WHERE month = DATE_TRUNC('month', NOW() - INTERVAL '1 month')) as last_avg,
                    co.name
                FROM commodities co WHERE co.id = :id
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", commodityId);

        Object[] row = (Object[]) query.getSingleResult();
        if (row[0] == null || row[1] == null) {
            return Optional.empty();
        }

        BigDecimal current = BigDecimal.valueOf(((Number) row[0]).doubleValue());
        BigDecimal last = BigDecimal.valueOf(((Number) row[1]).doubleValue());
        String name = (String) row[2];

        BigDecimal change = current.subtract(last).divide(last, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        Direction direction = change.compareTo(BigDecimal.ONE) > 0 ? Direction.UP :
                (change.compareTo(BigDecimal.valueOf(-1)) < 0 ? Direction.DOWN : Direction.STABLE);

        return Optional.of(new InflationTrendDto(commodityId, name, current.setScale(2, RoundingMode.HALF_UP),
                last.setScale(2, RoundingMode.HALF_UP), change.setScale(2, RoundingMode.HALF_UP), direction));
    }


    @SuppressWarnings("unchecked")
    public Optional<MovingAverageDto> getMovingAverageForecast(Long commodityId) {
        log.info("Calculating moving average forecast for commodity: {}", commodityId);
        validateCommodity(commodityId);

        String sql = """
                SELECT AVG(price) as avg_price
                FROM price_records
                WHERE commodity_id = :id
                  AND status = 'APPROVED'
                GROUP BY DATE_TRUNC('month', recorded_date)
                ORDER BY DATE_TRUNC('month', recorded_date) DESC
                LIMIT 3
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", commodityId);
        List<Number> results = query.getResultList();

        if (results.size() < 3) {
            log.warn("Not enough data to calculate movement average for commodity: {}", commodityId);
            return Optional.empty();
        }

        double sum = results.stream().mapToDouble(Number::doubleValue).sum();
        BigDecimal forecast = BigDecimal.valueOf(sum / 3).setScale(2, RoundingMode.HALF_UP);

        String name = commodityRepository.findById(commodityId).get().getName();

        return Optional.of(new MovingAverageDto(
                commodityId,
                name,
                YearMonth.from(LocalDate.now().plusMonths(1)),
                forecast,
                3
        ));
    }

    public DataQualityReportDto generateDataQualityReport() {
        log.info("Generating data quality report");

        Double overallCompleteness = calculateOverallCompleteness();
        Map<String, Long> dataFreshnessBreakdown = calculateDataFreshnessBreakdown();
        List<DuplicateAlertDto> duplicateAlerts = findDuplicateAlerts();
        List<OutlierAlertDto> outlierAlerts = findOutlierAlerts();
        List<AgentSubmissionSummaryDto> submissionsByAgent = getSubmissionsByAgent();

        return DataQualityReportDto.builder()
                .overallCompleteness(overallCompleteness)
                .dataFreshnessBreakdown(dataFreshnessBreakdown)
                .duplicateAlerts(duplicateAlerts)
                .outlierAlerts(outlierAlerts)
                .submissionsByAgent(submissionsByAgent)
                .reportGeneratedAt(java.time.OffsetDateTime.now())
                .build();
    }


    private Double calculateOverallCompleteness() {
        String sql = """
            SELECT
                COUNT(DISTINCT CONCAT(pr.commodity_id, '-', pr.market_id))::FLOAT /
                NULLIF((SELECT COUNT(*) FROM commodities) *
                       (SELECT COUNT(*) FROM markets), 0) * 100
            FROM price_records pr
            WHERE pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
            """;

        Query query = entityManager.createNativeQuery(sql);
        Object result = query.getSingleResult();

        return result != null ? ((Number) result).doubleValue() : 0.0;
    }

    private Map<String, Long> calculateDataFreshnessBreakdown() {
        String sql = """
            SELECT m.id, MAX(pr.recorded_date) AS last_update
            FROM markets m
            LEFT JOIN price_records pr ON pr.market_id = m.id AND pr.status = 'APPROVED'
            GROUP BY m.id
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("0-2 days", 0L);
        breakdown.put("3-7 days", 0L);
        breakdown.put("8-30 days", 0L);
        breakdown.put("Over 30 days", 0L);
        breakdown.put("No data", 0L);

        LocalDate now = LocalDate.now();

        for (Object[] row : results) {
            if (row[1] == null) {
                breakdown.put("No data", breakdown.get("No data") + 1);
            } else {
                LocalDate lastUpdate = ((java.sql.Date) row[1]).toLocalDate();
                long daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastUpdate, now);

                if (daysSince <= 2) {
                    breakdown.put("0-2 days", breakdown.get("0-2 days") + 1);
                } else if (daysSince <= 7) {
                    breakdown.put("3-7 days", breakdown.get("3-7 days") + 1);
                } else if (daysSince <= 30) {
                    breakdown.put("8-30 days", breakdown.get("8-30 days") + 1);
                } else {
                    breakdown.put("Over 30 days", breakdown.get("Over 30 days") + 1);
                }
            }
        }

        return breakdown;
    }


    private List<DuplicateAlertDto> findDuplicateAlerts() {
        String sql = """
            SELECT 
                pr.commodity_id, 
                c.name, 
                pr.market_id, 
                m.name, 
                pr.recorded_date, 
                COUNT(*) AS cnt
            FROM price_records pr
            JOIN commodities c ON pr.commodity_id = c.id
            JOIN markets m ON pr.market_id = m.id
            WHERE pr.status = 'APPROVED'
            GROUP BY pr.commodity_id, c.name, pr.market_id, m.name, pr.recorded_date
            HAVING COUNT(*) > 1
            ORDER BY cnt DESC
            LIMIT 50
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new DuplicateAlertDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        (String) row[3],
                        ((java.sql.Date) row[4]).toLocalDate(),
                        ((Number) row[5]).intValue()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    private List<OutlierAlertDto> findOutlierAlerts() {
        String sql = """
            WITH stats AS (
                SELECT
                    id,
                    price,
                    commodity_id,
                    market_id,
                    recorded_date,
                    AVG(price) OVER (PARTITION BY commodity_id, market_id) AS mean_price,
                    STDDEV(price) OVER (PARTITION BY commodity_id, market_id) AS std_price
                FROM price_records
                WHERE status = 'APPROVED'
            )
            SELECT
                s.id, 
                c.name AS commodity_name, 
                m.name AS market_name,
                s.price, 
                s.mean_price,
                ROUND(ABS(s.price - s.mean_price) / NULLIF(s.std_price, 0), 2) AS z_score,
                s.recorded_date
            FROM stats s
            JOIN commodities c ON s.commodity_id = c.id
            JOIN markets m ON s.market_id = m.id
            WHERE std_price > 0
              AND ABS(s.price - s.mean_price) / NULLIF(s.std_price, 0) > 3
            ORDER BY z_score DESC
            LIMIT 30
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new OutlierAlertDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(((Number) row[4]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(((Number) row[5]).doubleValue()).setScale(2, RoundingMode.HALF_UP),
                        ((java.sql.Date) row[6]).toLocalDate()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<AgentSubmissionSummaryDto> getSubmissionsByAgent() {
        String sql = """
            SELECT
                u.id,
                u.username,
                COUNT(pr.id) AS total_submitted,
                COUNT(CASE WHEN pr.status = 'APPROVED' THEN 1 END) AS total_approved,
                COUNT(CASE WHEN pr.status = 'REJECTED' THEN 1 END) AS total_rejected,
                COUNT(CASE WHEN pr.status = 'PENDING' THEN 1 END) AS total_pending,
                ROUND(
                    COUNT(CASE WHEN pr.status = 'APPROVED' THEN 1 END)::NUMERIC /
                    NULLIF(COUNT(pr.id), 0) * 100, 2
                ) AS approval_rate
            FROM users u
            LEFT JOIN price_records pr ON pr.submitted_by = u.id
            WHERE u.role = 'FIELD_AGENT'
            GROUP BY u.id, u.username
            ORDER BY total_submitted DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new AgentSubmissionSummaryDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        ((Number) row[5]).longValue(),
                        row[6] != null ? BigDecimal.valueOf(((Number) row[6]).doubleValue()) : BigDecimal.ZERO
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    private void validateCommodity(Long commodityId) {
        commodityRepository.findById(commodityId)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", commodityId));
    }
}
