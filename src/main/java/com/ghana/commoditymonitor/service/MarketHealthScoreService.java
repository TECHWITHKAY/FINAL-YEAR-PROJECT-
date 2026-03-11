package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.response.MarketHealthScoreDto;
import com.ghana.commoditymonitor.entity.Market;
import com.ghana.commoditymonitor.entity.MarketHealthScore;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.MarketHealthScoreRepository;
import com.ghana.commoditymonitor.repository.MarketRepository;
import com.ghana.commoditymonitor.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketHealthScoreService {

    private final MarketHealthScoreRepository healthScoreRepository;
    private final MarketRepository marketRepository;
    private final EntityManager entityManager;

    @Transactional
    public MarketHealthScore computeScoreForMarket(Long marketId) {
        log.info("Computing health score for market: {}", marketId);

        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", marketId));

        BigDecimal dataFreshness = computeDataFreshness(marketId);
        BigDecimal priceStability = computePriceStability(marketId);
        BigDecimal coverage = computeCoverage(marketId);

        BigDecimal totalScore = dataFreshness.multiply(new BigDecimal("0.40"))
                .add(priceStability.multiply(new BigDecimal("0.35")))
                .add(coverage.multiply(new BigDecimal("0.25")))
                .setScale(2, RoundingMode.HALF_UP);

        String grade = calculateGrade(totalScore);

        MarketHealthScore healthScore = MarketHealthScore.builder()
                .market(market)
                .score(totalScore)
                .dataFreshness(dataFreshness)
                .priceStability(priceStability)
                .coverage(coverage)
                .grade(grade)
                .build();

        return healthScoreRepository.save(healthScore);
    }

    @Transactional
    public List<MarketHealthScore> computeAllMarketScores() {
        long startTime = System.currentTimeMillis();
        
        List<Long> marketIds = marketRepository.findAll().stream()
                .map(Market::getId)
                .toList();

        List<MarketHealthScore> scores = new ArrayList<>();
        for (Long marketId : marketIds) {
            try {
                scores.add(computeScoreForMarket(marketId));
            } catch (ResourceNotFoundException e) {
                log.warn("Market {} disappeared during recomputation — skipping.", marketId);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Health score computed for {} markets in {}ms", scores.size(), duration);

        return scores;
    }

    public MarketHealthScoreDto getLatestScoreForMarket(Long marketId, UserPrincipal principal) {
        log.debug("Fetching latest health score for market: {}", marketId);

        MarketHealthScore score = healthScoreRepository.findTopByMarketIdOrderByComputedAtDesc(marketId)
                .orElseGet(() -> computeScoreForMarket(marketId));

        return mapToDto(score, principal);
    }

    public List<MarketHealthScoreDto> getAllLatestScores(UserPrincipal principal) {
        log.debug("Fetching all latest health scores");
        
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        List<MarketHealthScore> scores = healthScoreRepository.findAllDistinctLatestScores(pageable).getContent();

        return scores.stream()
                .map(score -> mapToDto(score, principal))
                .collect(Collectors.toList());
    }

    public List<MarketHealthScoreDto> getTopPerformingMarkets(int limit, UserPrincipal principal) {
        log.debug("Fetching top {} performing markets", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<MarketHealthScore> scores = healthScoreRepository.findAllDistinctLatestScores(pageable).getContent();

        return scores.stream()
                .map(score -> mapToDto(score, principal))
                .collect(Collectors.toList());
    }

    public List<MarketHealthScoreDto> getUnderperformingMarkets(int limit, UserPrincipal principal) {
        log.debug("Fetching underperforming markets");
        
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        List<MarketHealthScore> scores = healthScoreRepository.findAllDistinctLatestScores(pageable).getContent();

        return scores.stream()
                .filter(score -> "D".equals(score.getGrade()) || "F".equals(score.getGrade()))
                .limit(limit)
                .map(score -> mapToDto(score, principal))
                .collect(Collectors.toList());
    }

    private BigDecimal computeDataFreshness(Long marketId) {
        String sql = """
            SELECT MAX(recorded_date) 
            FROM price_records 
            WHERE market_id = :id AND status = 'APPROVED'
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", marketId);

        Date maxDate = (Date) query.getSingleResult();
        
        if (maxDate == null) {
            return BigDecimal.ZERO;
        }

        LocalDate mostRecent = maxDate.toLocalDate();
        long daysSince = ChronoUnit.DAYS.between(mostRecent, LocalDate.now());

        if (daysSince <= 2) return new BigDecimal("100");
        if (daysSince <= 7) return new BigDecimal("80");
        if (daysSince <= 14) return new BigDecimal("60");
        if (daysSince <= 30) return new BigDecimal("40");
        if (daysSince <= 90) return new BigDecimal("20");
        return BigDecimal.ZERO;
    }

    private BigDecimal computePriceStability(Long marketId) {
        String sql = """
            SELECT COALESCE(STDDEV(price), 0) 
            FROM price_records
            WHERE market_id = :id 
              AND status = 'APPROVED'
              AND recorded_date >= CURRENT_DATE - INTERVAL '90 days'
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", marketId);

        Number stdDev = (Number) query.getSingleResult();
        double stdDevValue = stdDev != null ? stdDev.doubleValue() : 0.0;

        double score = Math.max(0, 100 - (stdDevValue * 2.0));
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeCoverage(Long marketId) {
        String sql = """
            SELECT 
                COUNT(DISTINCT pr.commodity_id)::FLOAT / 
                NULLIF((SELECT COUNT(*) FROM commodities), 0) * 100
            FROM price_records pr
            WHERE pr.market_id = :id
              AND pr.status = 'APPROVED'
              AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", marketId);

        Number coverageValue = (Number) query.getSingleResult();
        double coverage = coverageValue != null ? coverageValue.doubleValue() : 0.0;

        return BigDecimal.valueOf(coverage).setScale(2, RoundingMode.HALF_UP);
    }

    private String calculateGrade(BigDecimal score) {
        double scoreValue = score.doubleValue();
        
        if (scoreValue >= 80) return "A";
        if (scoreValue >= 65) return "B";
        if (scoreValue >= 50) return "C";
        if (scoreValue >= 35) return "D";
        return "F";
    }

    private MarketHealthScoreDto mapToDto(MarketHealthScore score, UserPrincipal principal) {
        if (principal == null) {
            return MarketHealthScoreDto.forGuest(score);
        }
        return MarketHealthScoreDto.forAuthenticated(score);
    }
}
