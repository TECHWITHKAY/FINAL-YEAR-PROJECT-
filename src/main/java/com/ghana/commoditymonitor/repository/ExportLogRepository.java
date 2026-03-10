package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.ExportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Repository
public interface ExportLogRepository extends JpaRepository<ExportLog, Long> {

    Page<ExportLog> findByUserIdOrderByExportedAtDesc(Long userId, Pageable pageable);

    Page<ExportLog> findAllByOrderByExportedAtDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM ExportLog e WHERE e.exportedAt < :cutoffDate")
    long deleteByExportedAtBefore(@Param("cutoffDate") OffsetDateTime cutoffDate);
}
