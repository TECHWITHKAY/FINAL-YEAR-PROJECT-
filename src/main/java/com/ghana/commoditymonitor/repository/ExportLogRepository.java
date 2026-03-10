package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.ExportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportLogRepository extends JpaRepository<ExportLog, Long> {

    Page<ExportLog> findByUserIdOrderByExportedAtDesc(Long userId, Pageable pageable);

    Page<ExportLog> findAllByOrderByExportedAtDesc(Pageable pageable);
}
