package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.response.SeasonalOutlookDto;
import com.ghana.commoditymonitor.dto.response.SeasonalPatternDto;
import com.ghana.commoditymonitor.entity.Commodity;
import com.ghana.commoditymonitor.entity.SeasonalPattern;
import com.ghana.commoditymonitor.enums.SeasonalOutlook;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.CommodityRepository;
import com.ghana.commoditymonitor.repository.PriceRecordRepository;
import com.ghana.commoditymonitor.repository.SeasonalPatternRepository;
import com.ghana.commoditymonitor.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonalPatternService {

    private final SeasonalPatternRepository seasonalPatternRepository;
    private final CommodityRepository commodityRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final EntityManager entityManager;

    @Transactional
    public List<SeasonalPattern> computePatternsForCommodity(Long commodityId) {
        log.info("Computing seasonal patterns for commodity: {}", commodityId);

        Commodity commodity = commodityRepository.findById(commodityId)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", commodityId));

        long totalRecords = priceRecordRepository.countByCommodityIdAndStatus(commodityId, com.ghana.commoditymonitor.enums.PriceRecordStatus.APPROVED);

        if (totalRecords < 12) {
            log.warn("Insufficient data for commodity {}: only {} approved records (minimum 12 required)", 
                     commodityId, totalRecords);
            return new ArrayList<>();
        }

        String sql = """
            WITH monthly_avgs AS (
                SELECT
                    pr.commodity_id,
                    EXTRACT(MONTH FROM pr.recorded_date)::SMALLINT AS month_of_year,
                    AVG(pr.price) AS month_avg,
                    COUNT(*) AS sample_size,
                    MIN(EXTRACT(YEAR FROM pr.recorded_date))::SMALLINT AS year_from,
                    MAX(EXTRACT(YEAR FROM pr.recorded_date))::SMALLINT AS year_to
                FROM price_records pr
                WHERE pr.status = 'APPROVED'
                  AND pr.commodity_id = :commodityId
                GROUP BY pr.commodity_id, month_of_year
            ),
            overall_avg AS (
                SELECT AVG(price) AS grand_avg
                FROM price_records
                WHERE status = 'APPROVED'
                  AND commodity_id = :commodityId
            )
            SELECT
                ma.commodity_id,
                ma.month_of_year,
                ma.month_avg,
                ROUND((ma.month_avg / NULLIF(oa.grand_avg, 0))::NUMERIC, 4) AS seasonal_index,
                ma.sample_size,
                ma.year_from,
                ma.year_to
            FROM monthly_avgs ma
            CROSS JOIN overall_avg oa
            ORDER BY ma.month_of_year
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commodityId", commodityId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        seasonalPatternRepository.deleteAllByCommodityId(commodityId);
        seasonalPatternRepository.flush(); // Ensure deletion is committed before insertion

        List<SeasonalPattern> patterns = results.stream()
                .map(row -> SeasonalPattern.builder()
                        .commodity(commodity)
                        .monthOfYear(((Number) row[1]).shortValue())
                        .avgPrice(BigDecimal.valueOf(((Number) row[2]).doubleValue()).setScale(2, RoundingMode.HALF_UP))
                        .seasonalIndex(BigDecimal.valueOf(((Number) row[3]).doubleValue()).setScale(4, RoundingMode.HALF_UP))
                        .sampleSize(((Number) row[4]).intValue())
                        .dataYearFrom(((Number) row[5]).shortValue())
                        .dataYearTo(((Number) row[6]).shortValue())
                        .build())
                .collect(Collectors.toList());

        return seasonalPatternRepository.saveAll(patterns);
    }

    @Transactional
    public void computeAllPatterns() {
        long startTime = System.currentTimeMillis();
        
        List<Long> commodityIds = commodityRepository.findAllIds();

        int computed = 0;
        for (Long commodityId : commodityIds) {
            try {
                List<SeasonalPattern> patterns = computePatternsForCommodity(commodityId);
                if (!patterns.isEmpty()) {
                    computed++;
                }
            } catch (ResourceNotFoundException e) {
                log.warn("Commodity {} disappeared during recomputation — skipping.", commodityId);
            } finally {
                entityManager.clear();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Seasonal patterns computed for {} commodities in {}ms", computed, duration);
    }

    public List<SeasonalPatternDto> getPatternsForCommodity(Long commodityId, UserPrincipal principal) {
        log.debug("Fetching seasonal patterns for commodity: {}", commodityId);

        List<SeasonalPattern> patterns = seasonalPatternRepository.findByCommodityIdOrderByMonthOfYear(commodityId);

        if (patterns.isEmpty()) {
            patterns = computePatternsForCommodity(commodityId);
        }

        List<SeasonalPatternDto> dtos = patterns.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        if (principal == null) {
            int currentMonth = LocalDate.now().getMonthValue();
            return dtos.stream()
                    .filter(dto -> {
                        int month = dto.monthOfYear();
                        return month == currentMonth || 
                               month == (currentMonth % 12) + 1 || 
                               month == ((currentMonth + 1) % 12) + 1;
                    })
                    .collect(Collectors.toList());
        }

        return dtos;
    }

    public SeasonalPatternDto getBestMonthToBuy(Long commodityId) {
        log.debug("Finding best month to buy for commodity: {}", commodityId);

        List<SeasonalPattern> patterns = seasonalPatternRepository.findByCommodityIdOrderByMonthOfYear(commodityId);

        if (patterns.isEmpty()) {
            patterns = computePatternsForCommodity(commodityId);
        }

        return patterns.stream()
                .min(Comparator.comparing(SeasonalPattern::getSeasonalIndex))
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Seasonal pattern", "commodityId", commodityId));
    }

    public SeasonalPatternDto getWorstMonthToBuy(Long commodityId) {
        log.debug("Finding worst month to buy for commodity: {}", commodityId);

        List<SeasonalPattern> patterns = seasonalPatternRepository.findByCommodityIdOrderByMonthOfYear(commodityId);

        if (patterns.isEmpty()) {
            patterns = computePatternsForCommodity(commodityId);
        }

        return patterns.stream()
                .max(Comparator.comparing(SeasonalPattern::getSeasonalIndex))
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Seasonal pattern", "commodityId", commodityId));
    }

    public SeasonalOutlookDto getCurrentMonthOutlook(Long commodityId) {
        log.debug("Getting current month outlook for commodity: {}", commodityId);

        int currentMonth = LocalDate.now().getMonthValue();
        
        List<SeasonalPattern> patterns = seasonalPatternRepository.findByCommodityIdOrderByMonthOfYear(commodityId);

        if (patterns.isEmpty()) {
            patterns = computePatternsForCommodity(commodityId);
        }

        SeasonalPattern currentPattern = patterns.stream()
                .filter(p -> p.getMonthOfYear() == currentMonth)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Seasonal pattern for current month", "commodityId", commodityId));

        BigDecimal index = currentPattern.getSeasonalIndex();
        SeasonalOutlook outlook = determineOutlook(index);
        BigDecimal percentageFromAverage = index.subtract(BigDecimal.ONE)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);

        String monthName = java.time.Month.of(currentMonth)
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        String message = buildOutlookMessage(
                currentPattern.getCommodity().getName(),
                monthName,
                outlook,
                percentageFromAverage.abs()
        );

        return new SeasonalOutlookDto(
                commodityId,
                currentPattern.getCommodity().getName(),
                monthName,
                index,
                outlook,
                percentageFromAverage,
                message
        );
    }

    private SeasonalPatternDto mapToDto(SeasonalPattern pattern) {
        SeasonalOutlook interpretation = determineOutlook(pattern.getSeasonalIndex());
        
        String monthName = java.time.Month.of(pattern.getMonthOfYear())
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        return new SeasonalPatternDto(
                pattern.getCommodity().getId(),
                pattern.getCommodity().getName(),
                pattern.getMonthOfYear().intValue(),
                monthName,
                pattern.getAvgPrice(),
                pattern.getSeasonalIndex(),
                interpretation,
                pattern.getSampleSize(),
                pattern.getDataYearFrom().intValue(),
                pattern.getDataYearTo().intValue()
        );
    }

    private SeasonalOutlook determineOutlook(BigDecimal seasonalIndex) {
        if (seasonalIndex.compareTo(new BigDecimal("1.10")) > 0) {
            return SeasonalOutlook.EXPENSIVE;
        } else if (seasonalIndex.compareTo(new BigDecimal("0.90")) < 0) {
            return SeasonalOutlook.CHEAP;
        }
        return SeasonalOutlook.AVERAGE;
    }

    private String buildOutlookMessage(String commodityName, String monthName, 
                                       SeasonalOutlook outlook, BigDecimal percentage) {
        return switch (outlook) {
            case EXPENSIVE -> String.format(
                "Prices for %s are historically %.1f%% above average in %s. Consider buying smaller quantities.",
                commodityName, percentage, monthName
            );
            case CHEAP -> String.format(
                "%s is typically %.1f%% cheaper in %s — a good time to stock up.",
                commodityName, percentage, monthName
            );
            case AVERAGE -> String.format(
                "%s prices in %s are historically close to the annual average.",
                commodityName, monthName
            );
        };
    }
}
