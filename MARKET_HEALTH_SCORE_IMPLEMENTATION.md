# Market Health Score Implementation

## Overview

The Market Health Score system provides a composite score (0-100) and letter grade (A-F) for each market based on three measurable sub-scores computed from approved price records only.

## Scoring Formula

```
totalScore = (dataFreshness × 0.40) + (priceStability × 0.35) + (coverage × 0.25)
```

### Sub-Score 1: Data Freshness (0-100)

Measures how recently the market has reported prices.

**SQL Query:**
```sql
SELECT MAX(recorded_date) 
FROM price_records 
WHERE market_id = :id AND status = 'APPROVED'
```

**Scoring:**
- 0-2 days ago → 100
- 3-7 days → 80
- 8-14 days → 60
- 15-30 days → 40
- 31-90 days → 20
- > 90 days / no data → 0

### Sub-Score 2: Price Stability (0-100)

Measures price consistency over the last 90 days using standard deviation.

**SQL Query:**
```sql
SELECT COALESCE(STDDEV(price), 0) 
FROM price_records
WHERE market_id = :id 
  AND status = 'APPROVED'
  AND recorded_date >= CURRENT_DATE - INTERVAL '90 days'
```

**Formula:**
```
score = GREATEST(0, 100 - (stdDev * 2.0))
```

- stdDev of 0 = perfect 100
- stdDev of 50 = 0
- Lower standard deviation = more stable prices = higher score

### Sub-Score 3: Coverage (0-100)

Measures what percentage of all commodities have been priced in the last 30 days.

**SQL Query:**
```sql
SELECT 
    COUNT(DISTINCT pr.commodity_id)::FLOAT / 
    NULLIF((SELECT COUNT(*) FROM commodities), 0) * 100
FROM price_records pr
WHERE pr.market_id = :id
  AND pr.status = 'APPROVED'
  AND pr.recorded_date >= CURRENT_DATE - INTERVAL '30 days'
```

**Scoring:**
- 100% of commodities priced → 100
- 50% of commodities priced → 50
- 0% of commodities priced → 0

## Grade Mapping

| Score Range | Grade |
|-------------|-------|
| >= 80 | A |
| >= 65 | B |
| >= 50 | C |
| >= 35 | D |
| < 35 | F |

## Architecture

### Entity: MarketHealthScore

**Table:** `market_health_scores`

**Fields:**
- `id` (BIGSERIAL) - Primary key
- `market_id` (BIGINT) - Foreign key to markets
- `score` (DECIMAL 5,2) - Total composite score
- `data_freshness` (DECIMAL 5,2) - Data freshness sub-score
- `price_stability` (DECIMAL 5,2) - Price stability sub-score
- `coverage` (DECIMAL 5,2) - Coverage sub-score
- `grade` (VARCHAR 1) - Letter grade (A-F)
- `computed_at` (TIMESTAMPTZ) - When score was computed

**Indexes:**
- `idx_health_market_id` - For market lookups
- `idx_health_computed_at` - For time-based queries
- `idx_health_score` - For ranking
- `idx_health_grade` - For filtering by grade

### Repository: MarketHealthScoreRepository

**Methods:**

1. `findTopByMarketIdOrderByComputedAtDesc(Long marketId)`
   - Returns the most recent score for a market

2. `findAllDistinctLatestScores(Pageable)`
   - Returns latest score for each market
   - Uses subquery to get MAX(id) per market
   - Orders by score DESC

3. `deleteByComputedAtBefore(OffsetDateTime cutoff)`
   - Cleanup method for old scores
   - Keep 90 days of history

### Service: MarketHealthScoreService

**Core Methods:**

1. **computeScoreForMarket(Long marketId)**
   - Runs 3 sub-score queries via EntityManager
   - Computes totalScore and grade in Java
   - Persists and returns MarketHealthScore

2. **computeAllMarketScores()**
   - Fetches all market IDs
   - Iterates and calls computeScoreForMarket for each
   - Logs: "Health score computed for {n} markets in {ms}ms"

3. **getLatestScoreForMarket(Long marketId, UserPrincipal)**
   - If no score exists, computes on-demand
   - Returns appropriate DTO based on authentication

4. **getAllLatestScores(UserPrincipal)**
   - Returns latest score for all markets
   - Filters data based on authentication

5. **getTopPerformingMarkets(int limit, UserPrincipal)**
   - Returns top N markets by score
   - Ordered by score DESC

6. **getUnderperformingMarkets(int limit, UserPrincipal)**
   - Returns markets with grade D or F
   - Limited to specified count

### DTO: MarketHealthScoreDto

**Guest Version** (principal == null):
- marketId
- marketName
- cityName
- grade
- computedAt

**Authenticated Version** (principal != null):
- All guest fields PLUS:
- score
- dataFreshness
- priceStability
- coverage

**Factory Methods:**
- `MarketHealthScoreDto.forGuest(MarketHealthScore entity)`
- `MarketHealthScoreDto.forAuthenticated(MarketHealthScore entity)`

### Controller: MarketHealthController

**Base Path:** `/api/v1/health`

**Endpoints:**

1. `GET /` - Get all latest market health scores
2. `GET /{marketId}` - Get latest score for specific market
3. `GET /top?limit=5` - Get top performing markets
4. `GET /underperforming?limit=10` - Get underperforming markets
5. `POST /recompute` - Recompute all market scores (ADMIN only)

## API Usage Examples

### 1. Get All Latest Scores (Guest)

**Request:**
```http
GET /api/v1/health
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "marketId": 1,
      "marketName": "Makola Market",
      "cityName": "Accra",
      "score": null,
      "dataFreshness": null,
      "priceStability": null,
      "coverage": null,
      "grade": "A",
      "computedAt": "2026-03-10T14:00:00Z"
    }
  ]
}
```

### 2. Get All Latest Scores (Authenticated)

**Request:**
```http
GET /api/v1/health
Authorization: Bearer <token>
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "marketId": 1,
      "marketName": "Makola Market",
      "cityName": "Accra",
      "score": 85.50,
      "dataFreshness": 100.00,
      "priceStability": 75.00,
      "coverage": 80.00,
      "grade": "A",
      "computedAt": "2026-03-10T14:00:00Z"
    }
  ]
}
```

### 3. Get Score for Specific Market

**Request:**
```http
GET /api/v1/health/1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "marketId": 1,
    "marketName": "Makola Market",
    "cityName": "Accra",
    "score": 85.50,
    "dataFreshness": 100.00,
    "priceStability": 75.00,
    "coverage": 80.00,
    "grade": "A",
    "computedAt": "2026-03-10T14:00:00Z"
  }
}
```

### 4. Get Top Performing Markets

**Request:**
```http
GET /api/v1/health/top?limit=5
Authorization: Bearer <token>
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "marketId": 1,
      "marketName": "Makola Market",
      "cityName": "Accra",
      "score": 85.50,
      "grade": "A",
      "computedAt": "2026-03-10T14:00:00Z"
    },
    {
      "marketId": 2,
      "marketName": "Kejetia Market",
      "cityName": "Kumasi",
      "score": 82.30,
      "grade": "A",
      "computedAt": "2026-03-10T14:00:00Z"
    }
  ]
}
```

### 5. Get Underperforming Markets

**Request:**
```http
GET /api/v1/health/underperforming?limit=10
Authorization: Bearer <token>
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "marketId": 5,
      "marketName": "Rural Market",
      "cityName": "Tamale",
      "score": 32.00,
      "grade": "F",
      "computedAt": "2026-03-10T14:00:00Z"
    }
  ]
}
```

### 6. Recompute All Scores (Admin Only)

**Request:**
```http
POST /api/v1/health/recompute
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "status": "success",
  "message": "Successfully computed health scores for 15 markets"
}
```

## Score Interpretation

### Grade A (80-100)
- **Excellent market health**
- Recent data (within 2-7 days)
- Stable prices
- Good commodity coverage
- Reliable for analytics

### Grade B (65-79)
- **Good market health**
- Reasonably recent data
- Moderate price stability
- Decent commodity coverage
- Generally reliable

### Grade C (50-64)
- **Average market health**
- Data may be 1-2 weeks old
- Some price volatility
- Limited commodity coverage
- Use with caution

### Grade D (35-49)
- **Poor market health**
- Stale data (2-4 weeks old)
- High price volatility
- Very limited coverage
- Unreliable for analytics

### Grade F (<35)
- **Critical market health**
- Very stale or no data
- Extreme price volatility
- Minimal/no coverage
- Should not be used for analytics

## Use Cases

### 1. Market Monitoring Dashboard
Display health scores to identify which markets need attention.

### 2. Data Quality Assurance
Identify markets with poor data freshness for field agent assignment.

### 3. Analytics Filtering
Exclude low-grade markets from price trend analysis.

### 4. Resource Allocation
Prioritize field agent visits to underperforming markets.

### 5. Public Transparency
Show grade-only view to public users for market reliability.

## Performance Considerations

### Computation Cost
- Each market requires 3 SQL queries
- Queries use indexes on status and recorded_date
- Computation is transactional and atomic

### Caching Strategy
- Scores are persisted and reused
- On-demand computation only if no score exists
- Manual recompute via admin endpoint

### Historical Data
- Keep 90 days of score history
- Use `deleteByComputedAtBefore()` for cleanup
- Allows trend analysis over time

## Security & Privacy

### Guest Access
- Can see grade only
- No detailed score breakdown
- Sufficient for public transparency

### Authenticated Access
- Full score details
- Sub-score breakdown
- Useful for analysis and decision-making

### Admin Access
- Can trigger recomputation
- Access to all historical data
- Can manage score lifecycle

## Database Migration

**File:** `V9__create_market_health_scores_table.sql`

Creates:
- `market_health_scores` table
- Foreign key to markets
- Indexes for performance
- Cascade delete on market removal

## Testing Checklist

- [ ] Compute score for market with recent data (expect high score)
- [ ] Compute score for market with old data (expect low score)
- [ ] Compute score for market with no data (expect F grade)
- [ ] Verify data freshness scoring (0-2 days = 100)
- [ ] Verify price stability calculation (low stddev = high score)
- [ ] Verify coverage calculation (all commodities = 100)
- [ ] Verify grade mapping (80+ = A, <35 = F)
- [ ] Test guest access (score fields are null)
- [ ] Test authenticated access (all fields populated)
- [ ] Test top performing markets endpoint
- [ ] Test underperforming markets endpoint
- [ ] Test recompute all markets (admin only)
- [ ] Verify on-demand computation when no score exists

## Files Created

1. `MarketHealthScore.java` - Entity
2. `MarketHealthScoreRepository.java` - Repository
3. `MarketHealthScoreService.java` - Service with computation logic
4. `MarketHealthScoreDto.java` - DTO with guest/auth factory methods
5. `MarketHealthController.java` - REST endpoints
6. `V9__create_market_health_scores_table.sql` - Database migration

## Integration Points

### With Price Records
- Only APPROVED price records are used
- Filters by status in all queries
- Ensures data quality

### With @CurrentUser
- Uses optional authentication
- Returns different DTO based on principal
- Supports public and authenticated access

### With Analytics
- Can be used to filter markets for analytics
- Provides data quality indicator
- Helps identify reliable data sources

---

**Implementation Date:** March 10, 2026  
**Spring Boot Version:** 3.5.11  
**Database:** PostgreSQL
