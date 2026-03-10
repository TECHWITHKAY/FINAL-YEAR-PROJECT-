package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.entity.MarketHealthScore;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarketHealthScoreDto(
    Long marketId,
    String marketName,
    String cityName,
    BigDecimal score,
    BigDecimal dataFreshness,
    BigDecimal priceStability,
    BigDecimal coverage,
    String grade,
    OffsetDateTime computedAt
) {
    public static MarketHealthScoreDto forGuest(MarketHealthScore entity) {
        return new MarketHealthScoreDto(
            entity.getMarket().getId(),
            entity.getMarket().getName(),
            entity.getMarket().getCity().getName(),
            null,
            null,
            null,
            null,
            entity.getGrade(),
            entity.getComputedAt()
        );
    }

    public static MarketHealthScoreDto forAuthenticated(MarketHealthScore entity) {
        return new MarketHealthScoreDto(
            entity.getMarket().getId(),
            entity.getMarket().getName(),
            entity.getMarket().getCity().getName(),
            entity.getScore(),
            entity.getDataFreshness(),
            entity.getPriceStability(),
            entity.getCoverage(),
            entity.getGrade(),
            entity.getComputedAt()
        );
    }
}
