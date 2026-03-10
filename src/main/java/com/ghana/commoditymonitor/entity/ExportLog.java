package com.ghana.commoditymonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "export_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "export_type", nullable = false, length = 20)
    private String exportType;

    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "exported_at", nullable = false, updatable = false)
    private OffsetDateTime exportedAt;
}
