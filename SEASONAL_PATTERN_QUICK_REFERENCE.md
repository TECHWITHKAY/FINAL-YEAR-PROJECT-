# Seasonal Pattern System - Quick Reference

## What It Does

Analyzes historical price data to identify when commodities are typically cheaper or more expensive throughout the year.

## Key Formula

```
Seasonal Index = Monthly Average Price / Overall Average Price
```

- Index > 1.10 = EXPENSIVE (10%+ above average)
- Index 0.90-1.10 = AVERAGE
- Index < 0.90 = CHEAP (10%+ below average)

## API Endpoints

### Public Endpoints (No Auth Required)

```bash
# Get seasonal patterns (guest: 3 months, auth: 12 months)
GET /api/v1/seasonal/{commodityId}

# Get best month to buy (lowest price)
GET /api/v1/seasonal/{commodityId}/best-month

# Get worst month to buy (highest price)
GET /api/v1/seasonal/{commodityId}/worst-month

# Get current month outlook with buying advice
GET /api/v1/seasonal/{commodityId}/outlook
```

### Admin Endpoints

```bash
# Recompute all patterns
POST /api/v1/seasonal/recompute
Authorization: Bearer {admin_token}

# Recompute specific commodity
POST /api/v1/seasonal/recompute/{commodityId}
Authorization: Bearer {admin_token}
```

## Response Examples

### Seasonal Pattern
```json
{
  "commodityId": 1,
  "commodityName": "Rice",
  "monthOfYear": 3,
  "monthName": "March",
  "avgPrice": 45.50,
  "seasonalIndex": 0.85,
  "interpretation": "CHEAP",
  "sampleSize": 24,
  "dataYearFrom": 2022,
  "dataYearTo": 2024
}
```

### Current Month Outlook
```json
{
  "commodityId": 1,
  "commodityName": "Rice",
  "currentMonthName": "March",
  "seasonalIndex": 0.85,
  "outlook": "CHEAP",
  "percentageFromAverage": -15.0,
  "message": "Rice is typically 15.0% cheaper in March — a good time to stock up."
}
```

## Access Control

| User Type | Access |
|-----------|--------|
| Guest | 3-month preview, best/worst month, current outlook |
| Authenticated | All 12 months, all public endpoints |
| Admin | All + recomputation triggers |

## Data Requirements

- Minimum 12 approved price records per commodity
- Only APPROVED status records used
- Patterns computed on-demand if not cached

## Files Created

```
src/main/java/com/ghana/commoditymonitor/
├── entity/SeasonalPattern.java
├── repository/SeasonalPatternRepository.java
├── service/SeasonalPatternService.java
├── controller/SeasonalPatternController.java
├── dto/response/
│   ├── SeasonalPatternDto.java
│   └── SeasonalOutlookDto.java
└── enums/SeasonalOutlook.java

src/main/resources/db/migration/
└── V10__create_seasonal_patterns_table.sql
```

## Testing in Postman

### 1. Get Current Outlook (No Auth)
```
GET http://localhost:8080/api/v1/seasonal/1/outlook
```

### 2. Get Best Month (No Auth)
```
GET http://localhost:8080/api/v1/seasonal/1/best-month
```

### 3. Get All Patterns (Guest - 3 months)
```
GET http://localhost:8080/api/v1/seasonal/1
```

### 4. Get All Patterns (Authenticated - 12 months)
```
GET http://localhost:8080/api/v1/seasonal/1
Authorization: Bearer {your_token}
```

### 5. Recompute Patterns (Admin)
```
POST http://localhost:8080/api/v1/seasonal/recompute
Authorization: Bearer {admin_token}
```

## Common Use Cases

### For Buyers
- Check current month outlook before purchasing
- Find best month to buy for cost savings
- View seasonal trends for planning

### For Analysts
- Analyze price seasonality patterns
- Identify commodities with strong seasonal effects
- Compare seasonal indices across commodities

### For Admins
- Trigger pattern recomputation after data updates
- Monitor data quality (sample sizes)
- Maintain pattern freshness

## Integration with Other Features

- **Price Records**: Uses approved records only
- **Analytics**: Complements trend analysis
- **Market Health**: Provides seasonal context
- **Authentication**: Optional auth for enhanced access

## Next Steps After Implementation

1. Run database migrations: `./mvnw flyway:migrate`
2. Start application: `./mvnw spring-boot:run`
3. Test endpoints in Postman
4. Verify guest vs authenticated access
5. Trigger initial pattern computation (Admin)
