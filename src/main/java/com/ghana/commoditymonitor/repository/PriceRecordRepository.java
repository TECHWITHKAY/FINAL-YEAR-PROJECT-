package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.PriceRecord;
import com.ghana.commoditymonitor.enums.PriceRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    List<PriceRecord> findByCommodityId(Long commodityId);

    List<PriceRecord> findByMarketId(Long marketId);

    List<PriceRecord> findByCommodityIdAndRecordedDateBetween(Long commodityId, LocalDate from, LocalDate to);

    List<PriceRecord> findByMarketIdAndCommodityId(Long marketId, Long commodityId);

    List<PriceRecord> findByStatus(PriceRecordStatus status);

    List<PriceRecord> findBySubmittedByIdAndStatus(Long userId, PriceRecordStatus status);

    List<PriceRecord> findByStatusAndCreatedAtBefore(PriceRecordStatus status, java.time.OffsetDateTime threshold);

    long countByStatus(PriceRecordStatus status);
    
    long countByCommodityIdAndStatus(Long commodityId, PriceRecordStatus status);

    @Query("SELECT pr FROM PriceRecord pr " +
           "JOIN FETCH pr.market m " +
           "JOIN FETCH m.city c " +
           "WHERE pr.commodity.id = :commodityId " +
           "ORDER BY pr.recordedDate DESC")
    List<PriceRecord> findAllByCommodityWithMarketAndCity(@Param("commodityId") Long commodityId);
}
