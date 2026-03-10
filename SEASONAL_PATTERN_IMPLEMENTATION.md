# Seasonal Pattern Detection System - Implementation Guide

## Overview

The Seasonal Pattern Detection System analyzes historical approved price data to identify recurring monthly price patterns for commodities. This helps users understand when prices are typically higher or lower throughout the year, enabling better purchasing decisions.

## Core Concept: Seasonal Index

For each commodity C and month M:

```
seasonalIndex(C, M) = AVG(price of C in month M, across all years) / AVG(price of C across all months and years)
```

### Index Interpretation

- `> 1.10` → EXPENSIVE (more than 10% above average)
- `0.90 – 1.10` → AVERAGE (within 10% of average)
- `< 0.90` → CHEAP (more than 10% below average)

## Database Schema

### Table: `seasonal_patterns`

```sql
CREATE TABLE seasonal_patterns (
    id BIGSERIAL PRIMARY KEY,
    commodity_id BIGINT NOT NULL,
    month_of_year SMALLINT NOT NULL CHECK (month_of_year BETWEEN 1 AND 12),
    seasonal_index DECIMAL(6, 4) NOT NULL,
    avg_price DECIMAL(12, 2) NOT NULL,
    data_year_from SMALLINT NOT NULL,
    data_year_to SMALLINT NOT NULL,
    sample_size INTEGER NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_seasonal_commodity FOREIGN KEY (commodity_id) 
        REFERENCES commodities(id) ON DELETE CASCADE,
    CONSTRAINT uq_seasonal_commodity_month UNIQUE (commodity_id, month_of_year)
);
```

### Indexes

- `idx_seasonal_commodity_id` - Fast lookup by commodity
- `idx_seasonal_month` - Filter by month
- `idx_seasonal_index` - Sort by seasonal index
- `idx_seasonal_computed_at` - Track computation history

## Entity Model

### SeasonalPattern Entity

```java
@Entity
@Table(name = "seasonal_patterns")
public class SeasonalPattern {
    private Long id;
    private Commodity commodity;
    private Short monthOfYear;           // 1-12
    private BigDecimal seasonalIndex;    // Computed ratio
    private BigDecimal avgPrice;         // Average price for this month
    private Short dataYearFrom;          // Earliest year in dataset
    private Short dataYearTo;            // Latest year in dataset
    private Integer sampleSize;          // Number of records used
    private OffsetDateTime computedAt;   // When pattern was computed
}
```

## Core SQL Query

The seasonal index computation uses a CTE-based query:

```sql
WITH monthly_avgs AS (
    SELECT
        pr.commodity_id,
        EXTRACT(MONTH FROM pr.recorded_date)::SMALLINT AS month_of_year,
        AVG(pr.price) AS month_avg,
        COUNT(*) AS sample_size,
        MIN(EXTRACT(YEAR FROM pr.recorded_date))::SMALLINT AS year_from,
        MAX(EXTRACT(YEAR FROM pr.recorded_date))::SMALLINT AS year_to
    FROM price_records pr
    WHERE pr.status = 'APPROVED'
      AND pr.commodity_id = :commodityId
    GROUP BY pr.commodity_id, month_of_year
),
overall_avg AS (
    SELECT AVG(price) AS grand_avg
    FROM price_records
    WHERE status = 'APPROVED'
      AND commodity_id = :commodityId
)
SELECT
    ma.commodity_id,
    ma.month_of_year,
    ma.month_avg,
    ROUND((ma.month_avg / NULLIF(oa.grand_avg, 0))::NUMERIC, 4) AS seasonal_index,
    ma.sample_size,
    ma.year_from,
    ma.year_to
FROM monthly_avgs ma
CROSS JOIN overall_avg oa
ORDER BY ma.month_of_year;
```

## Service Layer

### SeasonalPatternService

#### Key Methods

**1. computePatternsForCommodity(Long commodityId)**
- Validates minimum 12 approved records exist
- Executes seasonal index SQL query
- Deletes existing patterns for commodity (upsert pattern)
- Saves new patterns
- Returns computed patterns

**2. computeAllPatterns()**
- Iterates through all commodities
- Calls `computePatternsForCommodity` for each
- Logs total computation time
- Used by ADMIN for bulk recomputation

**3. getPatternsForCommodity(Long commodityId, UserPrincipal principal)**
- Fetches patterns from database
- If none exist, computes on-demand
- GUEST users: Returns 3-month preview (current + next 2 months)
- AUTHENTICATED users: Returns all 12 months

**4. getBestMonthToBuy(Long commodityId)**
- Returns month with lowest seasonal index
- Public endpoint (no authentication required)
- Drives user engagement

**5. getWorstMonthToBuy(Long commodityId)**
- Returns month with highest seasonal index
- Public endpoint

**6. getCurrentMonthOutlook(Long commodityId)**
- Gets pattern for current month
- Builds actionable message based on outlook
- Public endpoint (strategic preview to drive registration)

## DTOs

### SeasonalPatternDto

```java
public record SeasonalPatternDto(
    Long commodityId,
    String commodityName,
    Integer monthOfYear,        // 1-12
    String monthName,           // "January", "February", etc.
    BigDecimal avgPrice,
    BigDecimal seasonalIndex,
    SeasonalOutlook interpretation,  // CHEAP/AVERAGE/EXPENSIVE
    Integer sampleSize,
    Integer dataYearFrom,
    Integer dataYearTo
) {}
```

### SeasonalOutlookDto

```java
public record SeasonalOutlookDto(
    Long commodityId,
    String commodityName,
    String currentMonthName,
    BigDecimal seasonalIndex,
    SeasonalOutlook outlook,
    BigDecimal percentageFromAverage,
    String message              // Actionable buying advice
) {}
```

## Controller Endpoints

### Base Path: `/api/v1/seasonal`

All endpoints are publicly accessible (configured in SecurityConfig).

#### 1. GET `/{commodityId}`
- Get seasonal patterns for a commodity
- Guest: 3-month preview
- Authenticated: All 12 months
- Uses `@CurrentUser` for optional authentication

#### 2. GET `/{commodityId}/best-month`
- Returns month with lowest seasonal index
- Public endpoint
- Drives engagement

#### 3. GET `/{commodityId}/worst-month`
- Returns month with highest seasonal index
- Public endpoint

#### 4. GET `/{commodityId}/outlook`
- Returns current month outlook with actionable advice
- Public endpoint
- Strategic preview to drive registration

#### 5. POST `/recompute`
- Recomputes patterns for all commodities
- Requires ADMIN role
- Returns success message

#### 6. POST `/recompute/{commodityId}`
- Recomputes patterns for specific commodity
- Requires ADMIN role
- Returns success message

## Access Control Strategy

### Guest Users (Unauthenticated)
- 3-month preview of seasonal patterns
- Access to best/worst month endpoints
- Access to current month outlook
- Strategic limitation to encourage registration

### Authenticated Users
- Full 12-month seasonal pattern data
- All public endpoints
- Complete historical context

### Admin Users
- All authenticated user access
- Ability to trigger pattern recomputation
- Bulk and individual commodity recomputation

## Data Quality Requirements

### Minimum Sample Size
- At least 12 approved price records required
- Ensures meaningful seasonal patterns
- Logs warning if insufficient data

### Data Filtering
- Only APPROVED price records used
- Ensures data quality and accuracy
- Consistent with analytics queries

## Outlook Messages

The system generates actionable messages based on seasonal outlook:

### EXPENSIVE (index > 1.10)
```
"Prices for {commodity} are historically {pct}% above average in {month}. 
Consider buying smaller quantities."
```

### CHEAP (index < 0.90)
```
"{commodity} is typically {pct}% cheaper in {month} — a good time to stock up."
```

### AVERAGE (0.90 ≤ index ≤ 1.10)
```
"{commodity} prices in {month} are historically close to the annual average."
```

## Usage Examples

### Compute Patterns for All Commodities (Admin)
```bash
POST /api/v1/seasonal/recompute
Authorization: Bearer {admin_token}
```

### Get Seasonal Patterns (Guest - 3 months)
```bash
GET /api/v1/seasonal/1
```

### Get Seasonal Patterns (Authenticated - 12 months)
```bash
GET /api/v1/seasonal/1
Authorization: Bearer {token}
```

### Get Best Month to Buy
```bash
GET /api/v1/seasonal/1/best-month
```

### Get Current Month Outlook
```bash
GET /api/v1/seasonal/1/outlook
```

## Integration Points

### With Price Record System
- Filters by `status = 'APPROVED'`
- Uses `recorded_date` for temporal analysis
- Joins with `commodities` table

### With Authentication System
- Uses `@CurrentUser` annotation
- Differentiates guest vs authenticated access
- Enforces ADMIN role for recomputation

### With Analytics System
- Complements price trend analysis
- Provides seasonal context for volatility
- Enhances market intelligence

## Performance Considerations

### On-Demand Computation
- Patterns computed if not in database
- First request may be slower
- Subsequent requests use cached patterns

### Bulk Recomputation
- Admin-triggered for all commodities
- Logs execution time
- Should be scheduled during off-peak hours

### Database Indexes
- Optimized for commodity lookup
- Fast sorting by seasonal index
- Efficient month filtering

## Testing Recommendations

### Unit Tests
- Test seasonal index calculation
- Test outlook determination logic
- Test message generation

### Integration Tests
- Test pattern computation with sample data
- Test guest vs authenticated access
- Test on-demand computation

### Data Validation
- Verify minimum 12 records requirement
- Test with edge cases (single year data)
- Validate seasonal index ranges

## Future Enhancements

1. **Scheduled Recomputation**: Automatic pattern updates via cron job
2. **Multi-Year Comparison**: Show how patterns change over time
3. **Regional Patterns**: Seasonal analysis by city/market
4. **Confidence Intervals**: Statistical significance of patterns
5. **Anomaly Detection**: Identify unusual seasonal deviations

## Migration

Migration file: `V10__create_seasonal_patterns_table.sql`

Run migrations:
```bash
./mvnw flyway:migrate
```

## Troubleshooting

### Issue: No patterns returned
- Check if commodity has at least 12 approved records
- Verify price records have `status = 'APPROVED'`
- Check logs for computation warnings

### Issue: Patterns not updating
- Trigger manual recomputation via `/recompute` endpoint
- Verify ADMIN authentication
- Check database constraints

### Issue: Incorrect seasonal index
- Verify price data quality
- Check for outliers in price records
- Review SQL query execution

## Summary

The Seasonal Pattern Detection System provides valuable insights into commodity price seasonality, helping users make informed purchasing decisions. The system balances public accessibility (to drive engagement) with authenticated features (to encourage registration), while maintaining data quality through approved-only filtering.
