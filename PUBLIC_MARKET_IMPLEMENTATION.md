# Public Market Controller - Implementation Guide

## Overview

The PublicMarketController provides guest-aware analytics endpoints designed for the frontend dashboard. These endpoints intelligently differentiate between guest and authenticated users, providing limited preview data to guests while offering full analytics to authenticated users.

## Key Features

- Guest-aware data filtering
- Cached dashboard summary with scheduled eviction
- Market-level and national-level price data
- Comprehensive commodity spotlight for authenticated users
- Strategic data gating to drive user registration

## Architecture

### Caching Strategy

The dashboard summary endpoint uses Spring Cache with:
- Cache name: `dashboardSummary`
- TTL: 5 minutes (300,000ms)
- Eviction: Scheduled task clears cache every 5 minutes
- Annotation: `@Cacheable("dashboardSummary")`

### Dependencies Added

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### Configuration

- `@EnableCaching` added to `CommodityMonitorApplication.java`
- `CacheConfig.java` configures cache manager and scheduled eviction
- `@EnableScheduling` enables automatic cache clearing

## Endpoints

### Base Path: `/api/v1/public`

All endpoints are publicly accessible (no authentication required).

---

### 1. GET `/dashboard-summary`

Returns dashboard overview with different data based on authentication status.

#### Guest Response (GuestDashboardDto)
```json
{
  "totalCommoditiesTracked": 25,
  "totalMarketsTracked": 15,
  "totalCitiesTracked": 10,
  "lastDataUpdateAt": "2024-03-10T10:30:00Z",
  "topThreeCommoditiesByNationalAvgPrice": [
    {
      "commodityId": 1,
      "commodityName": "Rice",
      "unit": "kg",
      "nationalAvgPrice": 45.50
    }
  ],
  "dataGateMessage": "Sign in to access full price history, city breakdowns, and export tools."
}
```

#### Authenticated Response (FullDashboardDto)
Extends guest data with:
```json
{
  ...guestFields,
  "dataGateMessage": null,
  "topRisingCommodities": [...],
  "topFallingCommodities": [...],
  "mostVolatileCommodities": [...],
  "marketHealthSummary": {
    "A": 2,
    "B": 5,
    "C": 3,
    "D": 1,
    "F": 0
  },
  "pendingSubmissionsCount": 12
}
```

#### Features
- Cached for 5 minutes
- Top 3 commodities by national average (last 7 days)
- Rising/falling commodities (month-over-month comparison)
- Most volatile commodities (30-day standard deviation)
- Market health grade distribution
- Pending submissions count (ADMIN only)

---

### 2. GET `/latest-prices`

Returns latest approved prices with filtering options.

#### Query Parameters
- `commodityId` (optional): Filter by commodity
- `cityId` (optional): Filter by city
- `limit` (default: 50): Maximum results

#### Guest Access
- Last 7 days only
- National average prices
- No market-level details
- `marketId` and `marketName` are null

#### Authenticated Access
- Last 30 days
- Market-level prices
- Full market details included

#### Response (LatestPriceDto)
```json
{
  "commodityId": 1,
  "commodityName": "Rice",
  "unit": "kg",
  "marketId": 5,
  "marketName": "Makola Market",
  "cityName": "Accra",
  "price": 45.50,
  "recordedDate": "2024-03-10",
  "daysAgo": 0
}
```

---

### 3. GET `/price-range/{commodityId}`

Returns price statistics for a commodity.

#### Query Parameters
- `cityId` (optional): Filter by city

#### Guest Access
- Last 7 days only
- National range
- Includes `guestNote`: "Showing last 7 days. Sign in for full history."

#### Authenticated Access
- Full date range (365 days)
- Per-city breakdown available
- No `guestNote`

#### Response (PriceRangeDto)
```json
{
  "commodityId": 1,
  "commodityName": "Rice",
  "unit": "kg",
  "cityName": "Accra",
  "minPrice": 40.00,
  "maxPrice": 50.00,
  "avgPrice": 45.25,
  "medianPrice": 45.00,
  "dataPointCount": 120,
  "dateFrom": "2024-02-10",
  "dateTo": "2024-03-10",
  "guestNote": "Showing last 7 days. Sign in for full history."
}
```

---

### 4. GET `/commodity-spotlight/{commodityId}`

Comprehensive single-commodity analytics view.

#### Access Control
- **Authenticated users only**
- Guests receive HTTP 401 with message:
  ```json
  {
    "success": false,
    "message": "Full commodity spotlight is available to registered users. Create a free account.",
    "data": null
  }
  ```

#### Response (CommoditySpotlightDto)
```json
{
  "commodityId": 1,
  "commodityName": "Rice",
  "unit": "kg",
  "category": "Grains",
  "currentNationalAvgPrice": 45.50,
  "priceChangePercentage": 5.2,
  "seasonalOutlook": {
    "commodityId": 1,
    "commodityName": "Rice",
    "currentMonthName": "March",
    "seasonalIndex": 1.15,
    "outlook": "EXPENSIVE",
    "percentageFromAverage": 15.0,
    "message": "Prices for Rice are historically 15.0% above average in March..."
  },
  "marketHealthScores": [...],
  "last6MonthsTrend": [...],
  "cityPriceComparison": [...],
  "volatilityRating": {...},
  "cheapestMarket": {...},
  "mostExpensiveMarket": {...}
}
```

#### Data Sources
- Commodity info from CommodityRepository
- Current national average (last 30 days)
- Price change (current month vs previous month)
- Seasonal outlook from SeasonalPatternService
- Market health scores from MarketHealthScoreService
- 6-month trend from AnalyticsService
- City comparison from AnalyticsService
- Volatility rating (30-day standard deviation)
- Cheapest/most expensive markets (last 7 days)

## DTOs

### New DTOs Created

1. **GuestDashboardDto** - Base dashboard data for guests
2. **FullDashboardDto** - Extended dashboard for authenticated users
3. **CommoditySummaryDto** - Commodity with national average
4. **CommodityMovementDto** - Price movement with direction
5. **LatestPriceDto** - Latest price with market details
6. **PriceRangeDto** - Price statistics with range
7. **CommoditySpotlightDto** - Comprehensive commodity view

### Existing DTOs Reused

- SeasonalOutlookDto
- MarketHealthScoreDto
- MonthlyTrendDto
- CityComparisonDto
- VolatilityDto

## Service Layer

### PublicDashboardService

Core service handling all public market analytics.

#### Key Methods

**1. getDashboardSummary(UserPrincipal principal)**
- Cached with `@Cacheable("dashboardSummary")`
- Delegates to `buildGuestDashboard()` or `buildFullDashboard()`
- Returns different DTO based on authentication

**2. getLatestPrices(commodityId, cityId, limit, principal)**
- Filters by 7 days (guest) or 30 days (authenticated)
- Returns market details only for authenticated users
- Uses DISTINCT ON for latest price per market

**3. getPriceRange(commodityId, cityId, principal)**
- Computes min, max, avg, median prices
- Filters by 7 days (guest) or 365 days (authenticated)
- Adds guest note for unauthenticated users

**4. getCommoditySpotlight(commodityId)**
- Aggregates data from multiple services
- Authenticated users only
- Comprehensive single-commodity view

#### Private Helper Methods

- `buildGuestDashboard()` - Constructs guest dashboard
- `buildFullDashboard()` - Constructs full dashboard
- `getTopMovingCommodities(rising)` - Top 3 rising/falling
- `getMostVolatileCommodities()` - Top 3 by volatility
- `getMarketHealthSummary()` - Grade distribution
- `getPendingSubmissionsCount()` - Pending records count
- `getCurrentNationalAverage()` - 30-day average
- `getPriceChangePercentage()` - Month-over-month change
- `getMarketHealthScoresForCommodity()` - Health scores
- `getCommodityVolatility()` - Volatility metrics
- `getCheapestMarket()` - Lowest price market
- `getMostExpensiveMarket()` - Highest price market

## SQL Queries

### Dashboard Counts
```sql
SELECT 
    (SELECT COUNT(*) FROM commodities) AS commodity_count,
    (SELECT COUNT(*) FROM markets) AS market_count,
    (SELECT COUNT(*) FROM cities) AS city_count,
    (SELECT MAX(recorded_date) FROM price_records WHERE status = 'APPROVED') AS last_update
```

### Top Commodities by Price
```sql
SELECT 
    c.id, c.name, c.unit,
    AVG(pr.price) AS avg_price
FROM price_records pr
JOIN commodities c ON pr.commodity_id = c.id
WHERE pr.status = 'APPROVED'
  AND pr.recorded_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY c.id, c.name, c.unit
ORDER BY avg_price DESC
LIMIT 3
```

### Price Movement (Rising/Falling)
```sql
WITH current_month AS (
    SELECT commodity_id, AVG(price) AS avg_price
    FROM price_records
    WHERE status = 'APPROVED'
      AND recorded_date >= DATE_TRUNC('month', CURRENT_DATE)
    GROUP BY commodity_id
),
previous_month AS (
    SELECT commodity_id, AVG(price) AS avg_price
    FROM price_records
    WHERE status = 'APPROVED'
      AND recorded_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
      AND recorded_date < DATE_TRUNC('month', CURRENT_DATE)
    GROUP BY commodity_id
)
SELECT 
    c.id, c.name, c.unit,
    cm.avg_price AS current_avg,
    pm.avg_price AS previous_avg,
    ((cm.avg_price - pm.avg_price) / NULLIF(pm.avg_price, 0) * 100) AS pct_change
FROM current_month cm
JOIN previous_month pm ON cm.commodity_id = pm.commodity_id
JOIN commodities c ON cm.commodity_id = c.id
WHERE pm.avg_price > 0
ORDER BY pct_change DESC
LIMIT 3
```

### Latest Prices
```sql
SELECT DISTINCT ON (pr.commodity_id, pr.market_id)
    pr.commodity_id, c.name, c.unit,
    pr.market_id, m.name, ci.name,
    pr.price, pr.recorded_date,
    CURRENT_DATE - pr.recorded_date AS days_ago
FROM price_records pr
JOIN commodities c ON pr.commodity_id = c.id
JOIN markets m ON pr.market_id = m.id
JOIN cities ci ON m.city_id = ci.id
WHERE pr.status = 'APPROVED'
  AND pr.recorded_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY pr.commodity_id, pr.market_id, pr.recorded_date DESC
```

### Price Range with Median
```sql
SELECT
    MIN(pr.price) AS min_price,
    MAX(pr.price) AS max_price,
    AVG(pr.price) AS avg_price,
    PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY pr.price) AS median_price,
    COUNT(*) AS data_point_count,
    MIN(pr.recorded_date) AS date_from,
    MAX(pr.recorded_date) AS date_to
FROM price_records pr
JOIN markets m ON pr.market_id = m.id
WHERE pr.commodity_id = :commodityId
  AND pr.status = 'APPROVED'
GROUP BY c.name, c.unit, ci.name
```

## Access Control Strategy

### Guest Users
- Limited to 7-day data windows
- National averages only (no market details)
- Preview of top commodities
- Strategic gate messages to encourage registration
- Access to best/worst month endpoints
- No commodity spotlight access

### Authenticated Users
- Extended data windows (30-365 days)
- Market-level price details
- Full analytics and trends
- No gate messages
- Complete commodity spotlight
- City-level breakdowns

### Admin Users
- All authenticated user access
- Additional pending submissions count
- Ability to trigger cache eviction

## Performance Considerations

### Caching
- Dashboard summary cached for 5 minutes
- Reduces database load for frequent requests
- Scheduled eviction ensures data freshness
- Cache key includes authentication status

### Query Optimization
- Uses DISTINCT ON for latest prices
- Indexes on status, recorded_date, commodity_id
- CTEs for complex aggregations
- LIMIT clauses to prevent large result sets

### Data Filtering
- Always filters by `status = 'APPROVED'`
- Date range filters reduce dataset size
- Optional commodity/city filters for targeted queries

## Integration Points

### With Authentication System
- Uses `@CurrentUser` annotation
- Null-safe principal checking
- Different responses based on authentication
- HTTP 401 for restricted endpoints

### With Analytics Services
- SeasonalPatternService for seasonal outlook
- MarketHealthScoreService for health scores
- AnalyticsService for trends and comparisons
- CommodityRepository for commodity details

### With Price Record System
- Filters by APPROVED status only
- Uses recorded_date for temporal analysis
- Joins with markets, cities, commodities

## Testing Recommendations

### Unit Tests
- Test guest vs authenticated responses
- Test cache behavior
- Test null handling
- Test date range filtering

### Integration Tests
- Test endpoint responses
- Test authentication flow
- Test data filtering
- Test error handling

### Performance Tests
- Test cache effectiveness
- Test query performance
- Test large result sets
- Test concurrent requests

## Usage Examples

### Get Dashboard Summary (Guest)
```bash
GET /api/v1/public/dashboard-summary
```

### Get Dashboard Summary (Authenticated)
```bash
GET /api/v1/public/dashboard-summary
Authorization: Bearer {token}
```

### Get Latest Prices for Commodity
```bash
GET /api/v1/public/latest-prices?commodityId=1&limit=20
```

### Get Price Range with City Filter
```bash
GET /api/v1/public/price-range/1?cityId=2
Authorization: Bearer {token}
```

### Get Commodity Spotlight (Authenticated)
```bash
GET /api/v1/public/commodity-spotlight/1
Authorization: Bearer {token}
```

### Get Commodity Spotlight (Guest - Returns 401)
```bash
GET /api/v1/public/commodity-spotlight/1
```

## Files Created

```
src/main/java/com/ghana/commoditymonitor/
├── controller/PublicMarketController.java
├── service/PublicDashboardService.java
├── config/CacheConfig.java
└── dto/response/
    ├── GuestDashboardDto.java
    ├── FullDashboardDto.java
    ├── CommoditySummaryDto.java
    ├── CommodityMovementDto.java
    ├── LatestPriceDto.java
    ├── PriceRangeDto.java
    └── CommoditySpotlightDto.java
```

## Configuration Files Modified

- `pom.xml` - Added spring-boot-starter-cache dependency
- `CommodityMonitorApplication.java` - Added @EnableCaching annotation

## Future Enhancements

1. **Redis Cache**: Replace in-memory cache with Redis for distributed caching
2. **Cache Warming**: Pre-populate cache on application startup
3. **Personalized Dashboards**: User-specific commodity tracking
4. **Real-time Updates**: WebSocket notifications for price changes
5. **Advanced Filtering**: More granular filtering options
6. **Export Integration**: Direct export from public endpoints
7. **Rate Limiting**: Prevent abuse of public endpoints
8. **Analytics Tracking**: Monitor guest vs authenticated usage

## Troubleshooting

### Issue: Cache not clearing
- Check `@EnableScheduling` is present
- Verify scheduled task is running
- Check cache manager configuration

### Issue: Guest seeing authenticated data
- Verify principal null checking
- Check SecurityConfig permits public endpoints
- Review response DTO construction

### Issue: Slow query performance
- Check database indexes
- Review query execution plans
- Consider adding more specific indexes
- Verify LIMIT clauses are in place

## Summary

The PublicMarketController provides a strategic balance between public accessibility and authenticated features. By offering limited preview data to guests while providing comprehensive analytics to authenticated users, it drives user registration while maintaining a positive user experience. The caching strategy ensures performance, while the guest-aware filtering protects sensitive data and encourages account creation.
