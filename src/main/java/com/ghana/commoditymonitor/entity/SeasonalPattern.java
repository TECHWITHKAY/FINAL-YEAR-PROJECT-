package com.ghana.commoditymonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "seasonal_patterns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id", nullable = false)
    private Commodity commodity;

    @Column(name = "month_of_year", nullable = false)
    private Short monthOfYear;

    @Column(name = "seasonal_index", nullable = false, precision = 6, scale = 4)
    private BigDecimal seasonalIndex;

    @Column(name = "avg_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal avgPrice;

    @Column(name = "data_year_from", nullable = false)
    private Short dataYearFrom;

    @Column(name = "data_year_to", nullable = false)
    private Short dataYearTo;

    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @CreationTimestamp
    @Column(name = "computed_at", nullable = false, updatable = false)
    private OffsetDateTime computedAt;
}
