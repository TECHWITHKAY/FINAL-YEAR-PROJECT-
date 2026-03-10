package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.MarketHealthScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface MarketHealthScoreRepository extends JpaRepository<MarketHealthScore, Long> {

    Optional<MarketHealthScore> findTopByMarketIdOrderByComputedAtDesc(Long marketId);

    @Query("""
        SELECT mhs FROM MarketHealthScore mhs
        WHERE mhs.id IN (
            SELECT MAX(mhs2.id)
            FROM MarketHealthScore mhs2
            GROUP BY mhs2.market.id
        )
        ORDER BY mhs.score DESC
        """)
    Page<MarketHealthScore> findAllDistinctLatestScores(Pageable pageable);

    void deleteByComputedAtBefore(OffsetDateTime cutoff);
}
