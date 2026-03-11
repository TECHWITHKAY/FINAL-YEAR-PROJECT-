# Data Quality Report - Implementation Guide

## Overview

The Data Quality Report endpoint provides administrators and analysts with comprehensive visibility into the completeness and integrity of price data. This feature is essential for academic credibility, investor trust, and operational excellence.

## Purpose

- Monitor data completeness across commodities and markets
- Identify data freshness issues
- Detect duplicate entries
- Flag statistical outliers
- Track field agent performance
- Support data-driven decision making

## Endpoint

### GET `/api/v1/analytics/data-quality`

**Access Control**: ADMIN and ANALYST roles only

**Response**: `DataQualityReportDto`

## Data Quality Metrics

### 1. Overall Completeness

**Metric**: Percentage of commodity-market combinations with approved data in the last 30 days

**Formula**:
```
Completeness = (Unique commodity-market pairs with data / Total possible combinations) × 100
```

**SQL Query**:
```sql
SELECT
    COUNT(DISTINCT CONCAT(pr.commodity_id, '-', pr.market_id))::FLOAT /
    NULLIF((SELECT COUNT(*) FROM commodities) *
           (SELECT COUNT(*) FROM markets), 0) * 100
FROM price_records pr
WHERE pr.status = 'APPROVED'
  AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
```

**Interpretation**:
- 100% = All commodity-market combinations have recent data
- 75% = Good coverage, some gaps
- 50% = Moderate coverage, significant gaps
- <25% = Poor coverage, major data collection issues

---

### 2. Data Freshness Breakdown

**Metric**: Distribution of markets by data recency

**Buckets**:
- `0-2 days` - Very fresh data
- `3-7 days` - Fresh data
- `8-30 days` - Aging data
- `Over 30 days` - Stale data
- `No data` - No approved records

**Logic**:
1. For each market, find the most recent approved price record
2. Calculate days since last update
3. Categorize into buckets
4. Return count per bucket

**Example Response**:
```json
{
  "0-2 days": 8,
  "3-7 days": 4,
  "8-30 days": 2,
  "Over 30 days": 1,
  "No data": 0
}
```

**Interpretation**:
- High counts in "0-2 days" indicate active data collection
- High counts in "Over 30 days" or "No data" indicate collection issues
- Use to identify markets needing attention

---

### 3. Duplicate Alerts

**Metric**: Price records with duplicate commodity-market-date combinations

**SQL Query**:
```sql
SELECT 
    pr.commodity_id, 
    c.name, 
    pr.market_id, 
    m.name, 
    pr.recorded_date, 
    COUNT(*) AS cnt
FROM price_records pr
JOIN commodities c ON pr.commodity_id = c.id
JOIN markets m ON pr.market_id = m.id
WHERE pr.status = 'APPROVED'
GROUP BY pr.commodity_id, c.name, pr.market_id, m.name, pr.recorded_date
HAVING COUNT(*) > 1
ORDER BY cnt DESC
LIMIT 50
```

**Response**: `List<DuplicateAlertDto>`

**Fields**:
- `commodityId` - Commodity identifier
- `commodityName` - Commodity name
- `marketId` - Market identifier
- `marketName` - Market name
- `recordedDate` - Date of duplicate entries
- `count` - Number of duplicate records

**Interpretation**:
- Duplicates indicate data entry errors or system issues
- Should be investigated and resolved
- May require deduplication logic
- High counts suggest systematic problems

**Action Items**:
- Review duplicate records
- Determine which record is correct
- Delete or reject incorrect duplicates
- Investigate root cause

---

### 4. Outlier Alerts

**Metric**: Price records with statistical outliers (|z-score| > 3)

**Z-Score Formula**:
```
z-score = (price - mean_price) / std_dev_price
```

**SQL Query**:
```sql
WITH stats AS (
    SELECT
        id,
        price,
        commodity_id,
        market_id,
        recorded_date,
        AVG(price) OVER (PARTITION BY commodity_id, market_id) AS mean_price,
        STDDEV(price) OVER (PARTITION BY commodity_id, market_id) AS std_price
    FROM price_records
    WHERE status = 'APPROVED'
)
SELECT
    s.id, 
    c.name AS commodity_name, 
    m.name AS market_name,
    s.price, 
    s.mean_price,
    ROUND(ABS(s.price - s.mean_price) / NULLIF(s.std_price, 0), 2) AS z_score,
    s.recorded_date
FROM stats s
JOIN commodities c ON s.commodity_id = c.id
JOIN markets m ON s.market_id = m.id
WHERE std_price > 0
  AND ABS(s.price - s.mean_price) / NULLIF(s.std_price, 0) > 3
ORDER BY z_score DESC
LIMIT 30
```

**Response**: `List<OutlierAlertDto>`

**Fields**:
- `priceRecordId` - Record identifier
- `commodityName` - Commodity name
- `marketName` - Market name
- `price` - Recorded price
- `marketMeanPrice` - Historical mean for this commodity-market
- `zScore` - Statistical z-score
- `recordedDate` - Date of record

**Interpretation**:
- Z-score > 3: Price is 3+ standard deviations above mean
- Z-score < -3: Price is 3+ standard deviations below mean
- May indicate data entry errors, market shocks, or genuine price spikes
- Requires manual review

**Action Items**:
- Verify price with original source
- Check for data entry errors (decimal point, unit conversion)
- Investigate market conditions on that date
- Correct if error, annotate if genuine

---

### 5. Submissions by Agent

**Metric**: Performance metrics for field agents

**SQL Query**:
```sql
SELECT
    u.id, 
    u.username,
    COUNT(*) AS total_submitted,
    SUM(CASE WHEN pr.status='APPROVED' THEN 1 ELSE 0 END) AS total_approved,
    SUM(CASE WHEN pr.status='REJECTED' THEN 1 ELSE 0 END) AS total_rejected,
    SUM(CASE WHEN pr.status='PENDING' THEN 1 ELSE 0 END) AS total_pending,
    ROUND(SUM(CASE WHEN pr.status='APPROVED' THEN 1 ELSE 0 END)::NUMERIC /
          NULLIF(COUNT(*), 0) * 100, 1) AS approval_rate
FROM users u
JOIN price_records pr ON pr.submitted_by_id = u.id
WHERE u.role = 'FIELD_AGENT'
GROUP BY u.id, u.username
ORDER BY total_submitted DESC
```

**Response**: `List<AgentSubmissionSummaryDto>`

**Fields**:
- `userId` - User identifier
- `username` - Agent username
- `totalSubmitted` - Total submissions
- `totalApproved` - Approved submissions
- `totalRejected` - Rejected submissions
- `totalPending` - Pending submissions
- `approvalRate` - Approval percentage

**Interpretation**:
- High approval rate (>90%): Reliable agent
- Moderate approval rate (70-90%): Acceptable performance
- Low approval rate (<70%): Training needed
- High pending count: Review bottleneck

**Action Items**:
- Recognize high-performing agents
- Provide training for low-performing agents
- Investigate systematic rejection patterns
- Address pending submission backlogs

---

### 6. Report Generated At

**Field**: `reportGeneratedAt`

**Type**: `OffsetDateTime`

**Purpose**: Timestamp when report was generated

**Usage**: Track report freshness and audit trail

## DTOs

### DataQualityReportDto

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityReportDto {
    private Double overallCompleteness;
    private Map<String, Long> dataFreshnessBreakdown;
    private List<DuplicateAlertDto> duplicateAlerts;
    private List<OutlierAlertDto> outlierAlerts;
    private List<AgentSubmissionSummaryDto> submissionsByAgent;
    private OffsetDateTime reportGeneratedAt;
}
```

### DuplicateAlertDto

```java
public record DuplicateAlertDto(
    Long commodityId,
    String commodityName,
    Long marketId,
    String marketName,
    LocalDate recordedDate,
    Integer count
) {}
```

### OutlierAlertDto

```java
public record OutlierAlertDto(
    Long priceRecordId,
    String commodityName,
    String marketName,
    BigDecimal price,
    BigDecimal marketMeanPrice,
    BigDecimal zScore,
    LocalDate recordedDate
) {}
```

### AgentSubmissionSummaryDto

```java
public record AgentSubmissionSummaryDto(
    Long userId,
    String username,
    Long totalSubmitted,
    Long totalApproved,
    Long totalRejected,
    Long totalPending,
    BigDecimal approvalRate
) {}
```

## Service Implementation

### AnalyticsService.generateDataQualityReport()

**Method Signature**:
```java
@Transactional(readOnly = true)
public DataQualityReportDto generateDataQualityReport()
```

**Process**:
1. Calculate overall completeness
2. Calculate data freshness breakdown
3. Find duplicate alerts
4. Find outlier alerts
5. Get submissions by agent
6. Build and return report DTO

**Performance**:
- Read-only transaction
- Multiple native SQL queries
- Optimized with indexes
- Typical execution: 1-3 seconds

## Controller Implementation

### AnalyticsController

**Endpoint**: `GET /api/v1/analytics/data-quality`

**Annotations**:
- `@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")`
- `@Operation` for Swagger documentation

**Response**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "overallCompleteness": 78.5,
    "dataFreshnessBreakdown": {
      "0-2 days": 8,
      "3-7 days": 4,
      "8-30 days": 2,
      "Over 30 days": 1,
      "No data": 0
    },
    "duplicateAlerts": [...],
    "outlierAlerts": [...],
    "submissionsByAgent": [...],
    "reportGeneratedAt": "2024-03-10T14:30:00Z"
  }
}
```

## Use Cases

### For Administrators

1. **Data Quality Monitoring**
   - Review overall completeness regularly
   - Track data freshness trends
   - Identify systematic issues

2. **Operational Management**
   - Monitor field agent performance
   - Identify training needs
   - Allocate resources effectively

3. **Data Cleanup**
   - Review and resolve duplicates
   - Investigate outliers
   - Maintain data integrity

### For Analysts

1. **Research Validation**
   - Assess data quality before analysis
   - Identify potential biases
   - Document data limitations

2. **Report Preparation**
   - Include data quality metrics in reports
   - Provide confidence intervals
   - Support academic credibility

3. **Trend Analysis**
   - Track data quality over time
   - Correlate quality with outcomes
   - Identify improvement opportunities

## Integration Points

### With Price Record System
- Filters by APPROVED status
- Uses submission metadata
- Tracks approval workflow

### With User Management
- Links to field agent accounts
- Tracks performance metrics
- Supports accountability

### With Analytics System
- Complements other analytics
- Provides quality context
- Supports data-driven decisions

## Performance Considerations

### Query Optimization
- Uses window functions for outliers
- Indexed columns for fast filtering
- LIMIT clauses to prevent large results

### Caching Strategy
- Report can be cached for 1 hour
- Invalidate on data updates
- Consider scheduled generation

### Resource Usage
- Read-only queries
- No table locks
- Minimal memory footprint

## Testing Recommendations

### Unit Tests
- Test each metric calculation
- Test edge cases (no data, all duplicates)
- Test z-score calculation

### Integration Tests
- Test with sample data
- Verify SQL query correctness
- Test access control

### Performance Tests
- Test with large datasets
- Measure query execution time
- Verify index usage

## Monitoring and Alerts

### Key Metrics to Monitor

1. **Completeness Trends**
   - Alert if drops below 60%
   - Track weekly changes
   - Identify seasonal patterns

2. **Duplicate Growth**
   - Alert if >10 duplicates found
   - Track duplicate rate
   - Investigate root causes

3. **Outlier Frequency**
   - Alert if >20 outliers found
   - Review outlier patterns
   - Validate data entry processes

4. **Agent Performance**
   - Alert if approval rate <70%
   - Track submission volumes
   - Identify training needs

## Best Practices

### Regular Review
- Generate report weekly
- Review with data team
- Document action items
- Track improvements

### Data Cleanup
- Address duplicates promptly
- Investigate outliers thoroughly
- Maintain audit trail
- Document decisions

### Agent Management
- Provide regular feedback
- Recognize high performers
- Support struggling agents
- Continuous training

### Academic Use
- Include quality metrics in papers
- Document data limitations
- Provide transparency
- Support reproducibility

## Future Enhancements

1. **Automated Alerts**: Email notifications for quality issues
2. **Trend Visualization**: Charts showing quality over time
3. **Predictive Analytics**: Forecast quality issues
4. **Automated Cleanup**: Suggest duplicate resolutions
5. **Quality Scoring**: Overall data quality score
6. **Export Functionality**: Download report as PDF/Excel

## Troubleshooting

### Issue: Low Completeness Score

**Check**:
- Field agent activity
- Market coverage
- Data collection schedules
- System availability

### Issue: Many Duplicates

**Check**:
- Data entry processes
- System validation rules
- Import procedures
- User training

### Issue: High Outlier Count

**Check**:
- Data entry accuracy
- Unit conversions
- Market conditions
- Price validation rules

### Issue: Low Approval Rates

**Check**:
- Agent training
- Submission guidelines
- Review criteria
- Communication channels

## Summary

The Data Quality Report provides comprehensive visibility into price data integrity, supporting operational excellence, academic credibility, and investor trust. By monitoring completeness, freshness, duplicates, outliers, and agent performance, administrators and analysts can maintain high data quality standards and make informed decisions.

## Files Created

```
src/main/java/com/ghana/commoditymonitor/
├── dto/response/
│   ├── DataQualityReportDto.java
│   ├── DuplicateAlertDto.java
│   ├── OutlierAlertDto.java
│   └── AgentSubmissionSummaryDto.java
└── service/AnalyticsService.java (updated)
└── controller/AnalyticsController.java (updated)
```

## API Usage Example

```bash
# Get data quality report (requires ADMIN or ANALYST role)
GET /api/v1/analytics/data-quality
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "overallCompleteness": 78.5,
    "dataFreshnessBreakdown": {
      "0-2 days": 8,
      "3-7 days": 4,
      "8-30 days": 2,
      "Over 30 days": 1,
      "No data": 0
    },
    "duplicateAlerts": [
      {
        "commodityId": 1,
        "commodityName": "Rice",
        "marketId": 5,
        "marketName": "Makola Market",
        "recordedDate": "2024-03-10",
        "count": 3
      }
    ],
    "outlierAlerts": [
      {
        "priceRecordId": 123,
        "commodityName": "Rice",
        "marketName": "Makola Market",
        "price": 150.00,
        "marketMeanPrice": 45.50,
        "zScore": 5.23,
        "recordedDate": "2024-03-10"
      }
    ],
    "submissionsByAgent": [
      {
        "userId": 10,
        "username": "field_agent_1",
        "totalSubmitted": 150,
        "totalApproved": 142,
        "totalRejected": 5,
        "totalPending": 3,
        "approvalRate": 94.7
      }
    ],
    "reportGeneratedAt": "2024-03-10T14:30:00Z"
  }
}
```
