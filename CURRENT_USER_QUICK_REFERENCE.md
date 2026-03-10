# @CurrentUser Quick Reference Card

## Basic Usage

```java
@GetMapping("/endpoint")
public ResponseEntity<?> method(@CurrentUser UserPrincipal principal) {
    // principal is null for guests
    // principal is populated for authenticated users
}
```

## UserPrincipal Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `id()` | `Long` | User's database ID |
| `username()` | `String` | User's username |
| `role()` | `Role` | User's role enum |
| `isAdmin()` | `boolean` | True if ADMIN |
| `isFieldAgent()` | `boolean` | True if FIELD_AGENT |
| `isAnalyst()` | `boolean` | True if ANALYST or ADMIN |
| `canReadFull()` | `boolean` | True if any authenticated role |

## Role Hierarchy

```
ADMIN         → Full access (isAdmin, isAnalyst, canReadFull)
ANALYST       → Analysis access (isAnalyst, canReadFull)
FIELD_AGENT   → Field access (isFieldAgent, canReadFull)
VIEWER        → Read access (canReadFull)
null (guest)  → Public access only
```

## Common Patterns

### Check if Authenticated
```java
if (principal != null) {
    // User is authenticated
}
```

### Check Role
```java
if (principal != null && principal.isAdmin()) {
    // Admin-specific logic
}
```

### Three-Tier Access
```java
if (principal != null && principal.isAdmin()) {
    return fullData;
} else if (principal != null) {
    return standardData;
} else {
    return publicData;
}
```

### Null-Safe Access
```java
String user = principal != null ? principal.username() : "guest";
log.info("Request from: {}", user);
```

## Testing

### As Guest
```bash
curl http://localhost:8080/api/v1/analytics/trends/1
```

### As Authenticated User
```bash
curl http://localhost:8080/api/v1/analytics/trends/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Security Config

These endpoints support optional authentication:
- `/api/v1/public/**`
- `/api/v1/health/**`
- `/api/v1/seasonal/**`
- `/api/v1/analytics/**`
- All GET requests to `/api/v1/commodities/**`, `/api/v1/cities/**`, `/api/v1/markets/**`, `/api/v1/price-records/**`

## Key Points

✅ **Never throws exceptions** - invalid tokens return null  
✅ **Null-safe** - always check `principal != null`  
✅ **Service layer gating** - filter data in services, not controllers  
✅ **Progressive enhancement** - same endpoint, different data levels  
✅ **Clean code** - no try-catch blocks needed  

## Files Created

1. `CurrentUser.java` - Annotation
2. `UserPrincipal.java` - Principal record
3. `CurrentUserArgumentResolver.java` - Resolver
4. `WebMvcConfig.java` - Configuration
5. `SecurityConfig.java` - Updated security rules
6. `Role.java` - Updated with FIELD_AGENT and ANALYST

## Example Controller

```java
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    
    @GetMapping("/trends/{id}")
    public ResponseEntity<?> getTrend(
            @PathVariable Long id,
            @CurrentUser UserPrincipal principal) {
        
        log.info("User: {}", principal != null ? principal.username() : "guest");
        
        if (principal != null && principal.isAnalyst()) {
            return ResponseEntity.ok(service.getAdvancedTrend(id));
        }
        return ResponseEntity.ok(service.getBasicTrend(id));
    }
}
```
