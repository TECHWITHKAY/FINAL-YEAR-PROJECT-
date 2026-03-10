# @CurrentUser Annotation Implementation

## Overview

This implementation provides an optional authentication mechanism for Spring Boot controllers using a custom `@CurrentUser` annotation. Unlike Spring Security's `@AuthenticationPrincipal`, this approach allows endpoints to be publicly accessible while still providing user context when authentication is present.

## Architecture

### Key Components

1. **@CurrentUser** - Custom annotation for method parameters
2. **UserPrincipal** - Lightweight record representing authenticated user
3. **CurrentUserArgumentResolver** - Resolves @CurrentUser parameters from JWT tokens
4. **WebMvcConfig** - Registers the argument resolver
5. **SecurityConfig** - Updated to permit public access to analytics endpoints

## Files Created

### 1. CurrentUser.java
**Location**: `src/main/java/com/ghana/commoditymonitor/security/CurrentUser.java`

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {}
```

**Purpose**: Marks controller method parameters that should receive the authenticated user.

---

### 2. UserPrincipal.java
**Location**: `src/main/java/com/ghana/commoditymonitor/security/UserPrincipal.java`

```java
public record UserPrincipal(Long id, String username, Role role) {
    public boolean isAdmin()       { return role == Role.ADMIN; }
    public boolean isFieldAgent()  { return role == Role.FIELD_AGENT; }
    public boolean isAnalyst()     { return role == Role.ANALYST || role == Role.ADMIN; }
    public boolean canReadFull()   { return role != null; }
}
```

**Purpose**: Lightweight principal object with convenient role-checking methods.

**Available Roles**:
- `ADMIN` - Full system access
- `VIEWER` - Read-only access
- `FIELD_AGENT` - Field data collection
- `ANALYST` - Data analysis capabilities

---

### 3. CurrentUserArgumentResolver.java
**Location**: `src/main/java/com/ghana/commoditymonitor/security/CurrentUserArgumentResolver.java`

**Key Features**:
- Extracts JWT token from Authorization header
- Validates token using JwtTokenProvider
- Loads user from UserRepository
- Returns null for guests (no exception thrown)
- Returns UserPrincipal for authenticated users

**Resolution Logic**:
1. Extract Bearer token from Authorization header (may be absent)
2. If token is absent or invalid → return null (guest)
3. If token is valid → extract username, load user, return UserPrincipal
4. Never throws exceptions - guests are valid callers

---

### 4. WebMvcConfig.java
**Location**: `src/main/java/com/ghana/commoditymonitor/config/WebMvcConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

**Purpose**: Registers the CurrentUserArgumentResolver with Spring MVC.

---

### 5. SecurityConfig.java (Updated)
**Location**: `src/main/java/com/ghana/commoditymonitor/config/SecurityConfig.java`

**Updated Permissions**:
```java
// Public Endpoints - permitAll (data gate enforced in service layer)
.requestMatchers("/api/v1/public/**").permitAll()
.requestMatchers("/api/v1/health/**").permitAll()
.requestMatchers("/api/v1/seasonal/**").permitAll()
.requestMatchers("/api/v1/analytics/**").permitAll()

// Read endpoints - permitAll (optional authentication via @CurrentUser)
.requestMatchers(HttpMethod.GET, "/api/v1/commodities/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/cities/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/markets/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/price-records/**").permitAll()
```

**Key Change**: Analytics and read endpoints are now `permitAll()`, with data gating enforced in the service layer based on UserPrincipal.

---

### 6. Role.java (Updated)
**Location**: `src/main/java/com/ghana/commoditymonitor/enums/Role.java`

**Added Roles**:
- `FIELD_AGENT` - For field data collectors
- `ANALYST` - For data analysts

---

## Usage Pattern

### Basic Usage in Controllers

```java
@GetMapping("/trends/{id}")
public ResponseEntity<?> getTrend(@PathVariable Long id,
                                   @CurrentUser UserPrincipal principal) {
    // principal is null for guests, populated for authenticated users
    log.info("User: {}", principal != null ? principal.username() : "guest");
    return ResponseEntity.ok(analyticsService.getTrend(id, principal));
}
```

### Role-Based Logic

```java
@GetMapping("/data/{id}")
public ResponseEntity<?> getData(@PathVariable Long id,
                                  @CurrentUser UserPrincipal principal) {
    if (principal != null && principal.isAdmin()) {
        // Return full data for admins
        return ResponseEntity.ok(service.getFullData(id));
    } else if (principal != null && principal.canReadFull()) {
        // Return standard data for authenticated users
        return ResponseEntity.ok(service.getStandardData(id));
    } else {
        // Return limited data for guests
        return ResponseEntity.ok(service.getPublicData(id));
    }
}
```

### Service Layer Pattern

```java
public TrendDto getTrend(Long commodityId, UserPrincipal principal) {
    TrendDto trend = calculateTrend(commodityId);
    
    // Apply data filtering based on authentication
    if (principal == null) {
        // Guest - return limited data
        trend.setDetailedMetrics(null);
    } else if (principal.isAnalyst()) {
        // Analyst - return full data with advanced metrics
        trend.setAdvancedAnalytics(calculateAdvancedMetrics(commodityId));
    }
    
    return trend;
}
```

## Testing

### Test as Guest (No Authentication)

```bash
curl -X GET http://localhost:8080/api/v1/analytics/trends/1
```

**Expected**: Returns public data, principal is null in controller

### Test as Authenticated User

```bash
curl -X GET http://localhost:8080/api/v1/analytics/trends/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected**: Returns data with user context, principal is populated

### Test with Invalid Token

```bash
curl -X GET http://localhost:8080/api/v1/analytics/trends/1 \
  -H "Authorization: Bearer INVALID_TOKEN"
```

**Expected**: Returns public data (treated as guest), principal is null

## Benefits

1. **Flexible Access Control**: Endpoints can be public while still providing enhanced data for authenticated users
2. **No Exceptions**: Invalid tokens don't cause errors - they're treated as guests
3. **Clean Code**: Controllers don't need try-catch blocks for authentication
4. **Service Layer Control**: Data filtering logic lives in services, not security config
5. **Progressive Enhancement**: Same endpoint serves different data based on authentication level

## Security Considerations

1. **Data Gating**: Critical data filtering must happen in the service layer
2. **Null Checks**: Always check if principal is null before accessing properties
3. **Role Validation**: Use UserPrincipal helper methods for role checks
4. **Token Validation**: JwtTokenProvider handles all token validation
5. **No Side Effects**: Resolver never throws exceptions or modifies request state

## Example: AnalyticsController

The `AnalyticsController` has been updated to demonstrate the pattern:

```java
@GetMapping("/trends/{commodityId}")
@Operation(summary = "Get monthly price trend", 
           description = "Returns average prices grouped by month. Supports optional authentication.")
public ResponseEntity<ApiResponse<List<MonthlyTrendDto>>> getMonthlyPriceTrend(
        @PathVariable Long commodityId, 
        @RequestParam(defaultValue = "12") int months,
        @CurrentUser UserPrincipal principal) {
    log.info("REST request to get monthly price trend for commodity: {} over {} months (user: {})", 
             commodityId, months, principal != null ? principal.username() : "guest");
    return ResponseEntity.ok(ApiResponse.ok(analyticsService.getMonthlyPriceTrend(commodityId, months)));
}
```

## Migration Guide

To add optional authentication to existing endpoints:

1. **Add @CurrentUser parameter** to controller method
2. **Update SecurityConfig** to permitAll() for the endpoint
3. **Update service layer** to handle null principal (guest access)
4. **Add role-based logic** in service if needed
5. **Update API documentation** to indicate optional authentication

## Common Patterns

### Pattern 1: Public with Enhanced Data
```java
// Public endpoint with enhanced data for authenticated users
@GetMapping("/public/data")
public ResponseEntity<?> getPublicData(@CurrentUser UserPrincipal principal) {
    return ResponseEntity.ok(service.getData(principal));
}
```

### Pattern 2: Optional Premium Features
```java
// Free tier for guests, premium features for subscribers
@GetMapping("/forecast/{id}")
public ResponseEntity<?> getForecast(@PathVariable Long id,
                                      @CurrentUser UserPrincipal principal) {
    if (principal != null && principal.isAnalyst()) {
        return ResponseEntity.ok(service.getAdvancedForecast(id));
    }
    return ResponseEntity.ok(service.getBasicForecast(id));
}
```

### Pattern 3: Rate Limiting by Role
```java
// Different rate limits based on authentication
@GetMapping("/data")
public ResponseEntity<?> getData(@CurrentUser UserPrincipal principal) {
    if (principal == null) {
        rateLimiter.checkGuestLimit();
    } else {
        rateLimiter.checkAuthenticatedLimit(principal.role());
    }
    return ResponseEntity.ok(service.getData());
}
```

## Troubleshooting

### Issue: Principal is always null
**Solution**: Verify JWT token is valid and Authorization header format is correct (`Bearer TOKEN`)

### Issue: Principal is null for valid token
**Solution**: Check JwtTokenProvider.validateToken() and ensure user exists in database

### Issue: Compilation errors
**Solution**: Ensure all files are created and WebMvcConfig is registered as @Configuration

### Issue: 403 Forbidden errors
**Solution**: Verify SecurityConfig has permitAll() for the endpoint path

---

**Implementation Date**: March 10, 2026  
**Spring Boot Version**: 3.5.11  
**Java Version**: 21
