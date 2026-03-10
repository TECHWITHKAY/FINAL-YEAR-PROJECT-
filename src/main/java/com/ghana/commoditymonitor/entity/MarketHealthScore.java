package com.ghana.commoditymonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "market_health_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketHealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "data_freshness", nullable = false, precision = 5, scale = 2)
    private BigDecimal dataFreshness;

    @Column(name = "price_stability", nullable = false, precision = 5, scale = 2)
    private BigDecimal priceStability;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal coverage;

    @Column(nullable = false, length = 1)
    private String grade;

    @CreationTimestamp
    @Column(name = "computed_at", nullable = false, updatable = false)
    private OffsetDateTime computedAt;
}
