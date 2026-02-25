package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for PriceRecord entity.
 */
@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    List<PriceRecord> findByCommodityId(Long commodityId);

    List<PriceRecord> findByMarketId(Long marketId);

    List<PriceRecord> findByCommodityIdAndRecordedDateBetween(Long commodityId, LocalDate from, LocalDate to);

    List<PriceRecord> findByMarketIdAndCommodityId(Long marketId, Long commodityId);

    /**
     * Find all price records for a commodity joined with market and city data.
     */
    @Query("SELECT pr FROM PriceRecord pr " +
           "JOIN FETCH pr.market m " +
           "JOIN FETCH m.city c " +
           "WHERE pr.commodity.id = :commodityId " +
           "ORDER BY pr.recordedDate DESC")
    List<PriceRecord> findAllByCommodityWithMarketAndCity(@Param("commodityId") Long commodityId);
}
