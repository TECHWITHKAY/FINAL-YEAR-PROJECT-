package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.response.analytics.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for complex analytical queries using native SQL via EntityManager.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final EntityManager entityManager;
    private final CommodityRepository commodityRepository;

    /**
     * Retrieves monthly average price trends for a specific commodity.
     */
    @SuppressWarnings("unchecked")
    public List<MonthlyTrendDto> getMonthlyPriceTrend(Long commodityId, int months) {
        log.info("Calculating monthly price trend for commodity: {} over {} months", commodityId, months);
        validateCommodity(commodityId);

        String sql = """
                SELECT pr.commodity_id, co.name, DATE_TRUNC('month', pr.recorded_date) AS month, AVG(pr.price) AS avg_price
                FROM price_records pr
                JOIN commodities co ON pr.commodity_id = co.id
                WHERE pr.commodity_id = :id AND pr.recorded_date >= NOW() - CAST(:months || ' months' AS INTERVAL)
                GROUP BY pr.commodity_id, co.name, month
                ORDER BY month ASC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", commodityId);
        query.setParameter("months", months);

        List<Object[]> results = query.getResultList();
        List<MonthlyTrendDto> response = new ArrayList<>();

        // Result mapping: [0]: id, [1]: name, [2]: month (Timestamp), [3]: avg_price (Numeric)
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

    /**
     * Compares average prices of a commodity across different cities.
     */
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
                GROUP BY c.name, co.name
                ORDER BY avg_price DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", commodityId);

        List<Object[]> results = query.getResultList();
        List<CityComparisonDto> response = new ArrayList<>();

        // Result mapping: [0]: city_name, [1]: commodity_name, [2]: avg_price
        for (Object[] row : results) {
            response.add(new CityComparisonDto(
                    (String) row[0],
                    (String) row[1],
                    BigDecimal.valueOf(((Number) row[2]).doubleValue()).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return response;
    }

    /**
     * Calculates price volatility for all commodities.
     */
    @SuppressWarnings("unchecked")
    public List<VolatilityDto> getPriceVolatility() {
        log.info("Calculating price volatility for all commodities");

        String sql = """
                SELECT pr.commodity_id, co.name, STDDEV(pr.price) AS std_dev
                FROM price_records pr
                JOIN commodities co ON pr.commodity_id = co.id
                GROUP BY pr.commodity_id, co.name
                ORDER BY std_dev DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<VolatilityDto> response = new ArrayList<>();

        // Result mapping: [0]: commodity_id, [1]: name, [2]: std_dev
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

    /**
     * Analyzes inflation trend for a specific commodity (current vs last month).
     */
    public Optional<InflationTrendDto> getInflationTrend(Long commodityId) {
        log.info("Calculating inflation trend for commodity: {}", commodityId);
        validateCommodity(commodityId);

        String sql = """
                WITH monthly_avgs AS (
                    SELECT DATE_TRUNC('month', recorded_date) as month, AVG(price) as avg_price
                    FROM price_records
                    WHERE commodity_id = :id
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

        String direction = change.compareTo(BigDecimal.ONE) > 0 ? "UP" : 
                          (change.compareTo(BigDecimal.valueOf(-1)) < 0 ? "DOWN" : "STABLE");

        return Optional.of(new InflationTrendDto(commodityId, name, current.setScale(2, RoundingMode.HALF_UP), 
                last.setScale(2, RoundingMode.HALF_UP), change.setScale(2, RoundingMode.HALF_UP), direction));
    }

    /**
     * Forecasts the price for the next month based on a 3-month moving average.
     */
    @SuppressWarnings("unchecked")
    public Optional<MovingAverageDto> getMovingAverageForecast(Long commodityId) {
        log.info("Calculating moving average forecast for commodity: {}", commodityId);
        validateCommodity(commodityId);

        String sql = """
                SELECT AVG(price) as avg_price
                FROM price_records
                WHERE commodity_id = :id
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

    private void validateCommodity(Long id) {
        if (!commodityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Commodity", "id", id);
        }
    }
}
