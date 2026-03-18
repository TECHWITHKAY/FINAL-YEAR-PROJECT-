package com.ghana.commoditymonitor.scheduler;

import com.ghana.commoditymonitor.entity.PriceRecord;
import com.ghana.commoditymonitor.enums.PriceRecordStatus;
import com.ghana.commoditymonitor.repository.ExportLogRepository;
import com.ghana.commoditymonitor.repository.MarketHealthScoreRepository;
import com.ghana.commoditymonitor.repository.PriceRecordRepository;
import com.ghana.commoditymonitor.service.MarketHealthScoreService;
import com.ghana.commoditymonitor.service.PasswordResetService;
import com.ghana.commoditymonitor.service.SeasonalPatternService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommodityScheduler {

    private final MarketHealthScoreService marketHealthScoreService;
    private final PasswordResetService passwordResetService;
    private final SeasonalPatternService seasonalPatternService;
    private final MarketHealthScoreRepository marketHealthScoreRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final ExportLogRepository exportLogRepository;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 2 * * *")
    public void refreshMarketHealthScores() {
        log.info("Starting scheduled market health score refresh");
        try {
            long startTime = System.currentTimeMillis();

            List<com.ghana.commoditymonitor.entity.MarketHealthScore> scores = 
                    marketHealthScoreService.computeAllMarketScores();
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Market health scores refreshed for {} markets in {}ms", scores.size(), elapsed);
        } catch (Exception e) {
            log.error("Failed to refresh market health scores", e);
        }

        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(90);
            marketHealthScoreRepository.deleteByComputedAtBefore(cutoffDate);
            log.debug("Deleted market health scores older than 90 days");
        } catch (Exception e) {
            log.error("Failed to cleanup old market health scores", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * *")
    public void refreshSeasonalPatterns() {
        log.info("Starting scheduled seasonal pattern recomputation");
        try {
            seasonalPatternService.computeAllPatterns();
            log.info("Seasonal patterns recomputed for all commodities");
        } catch (Exception e) {
            log.error("Failed to recompute seasonal patterns", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkPendingSlaBreaches() {
        log.debug("Checking for pending price record SLA breaches");
        
        OffsetDateTime slaThreshold = OffsetDateTime.now().minusHours(24);
        
        List<PriceRecord> pendingRecords = priceRecordRepository
                .findByStatusAndCreatedAtBefore(PriceRecordStatus.PENDING, slaThreshold);

        for (PriceRecord record : pendingRecords) {
            long hoursPending = Duration.between(record.getCreatedAt(), OffsetDateTime.now()).toHours();
            String submitter = record.getSubmittedBy() != null ? 
                    record.getSubmittedBy().getUsername() : "unknown";
            
            log.warn("SLA BREACH: Price record ID {} submitted by {} has been PENDING for {} hours",
                    record.getId(), submitter, hoursPending);
        }

        if (!pendingRecords.isEmpty()) {
            log.info("Found {} price records breaching 24-hour SLA", pendingRecords.size());
        }
    }

    @Scheduled(fixedRate = 900000)
    public void evictDashboardCache() {
        log.debug("Evicting dashboardSummary cache");
        
        var cache = cacheManager.getCache("dashboardSummary");
        if (cache != null) {
            cache.clear();
            log.debug("dashboardSummary cache evicted");
        }
    }

    @Scheduled(cron = "0 0 4 * * SUN")
    public void cleanupOldExportLogs() {
        log.info("Starting cleanup of old export logs");
        
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(180);
        
        long deletedCount = exportLogRepository.deleteByExportedAtBefore(cutoffDate);
        
        log.info("Cleaned up {} old export log entries", deletedCount);
    }

    @Scheduled(cron = "0 30 4 * * *")
    public void cleanupExpiredResetTokens() {
        log.info("Starting nightly cleanup of expired password reset tokens");
        passwordResetService.cleanupExpiredTokens();
    }
}
