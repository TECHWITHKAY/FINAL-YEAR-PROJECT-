package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.PriceRecordAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRecordAuditRepository extends JpaRepository<PriceRecordAudit, Long> {
    
    List<PriceRecordAudit> findByPriceRecordIdOrderByPerformedAtDesc(Long priceRecordId);
}
