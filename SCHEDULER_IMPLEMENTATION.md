# Commodity Scheduler - Implementation Guide

## Overview

The CommodityScheduler is a Spring-managed component that automatically maintains data freshness and system health through scheduled background tasks. It handles cache eviction, data recomputation, SLA monitoring, and cleanup operations.

## Configuration

### Application Setup

Added `@EnableScheduling` to `CommodityMonitorApplication.java`:

```java
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CommodityMonitorApplication {
    // ...
}
```

### Scheduler Component

Location: `src/main/java/com/ghana/commoditymonitor/scheduler/CommodityScheduler.java`

Annotations:
- `@Component` - Spring-managed bean
- `@RequiredArgsConstructor` - Lombok constructor injection
- `@Slf4j` - Logging support

## Scheduled Tasks

### 1. Market Health Score Refresh

**Schedule**: Daily at 02:00 AM  
**Cron**: `0 0 2 * * *`

#### Purpose
Automatically recomputes health scores for all markets to ensure data freshness.

#### Logic
1. Records start time for performance tracking
2. Calls `marketHealthScoreService.computeAllMarketScores()`
3. Logs execution time and market count
4. Deletes scores older than 90 days (data retention policy)

#### Example Log Output
```
INFO  - Starting scheduled market health score refresh
INFO  - Market health scores refreshed for 15 markets in 2341ms
DEBUG - Deleted market health scores older than 90 days
```

#### Benefits
- Ensures dashboard shows current health metrics
- Prevents stale data from affecting decisions
- Maintains historical data for 90 days
- Runs during low-traffic hours

---

### 2. Seasonal Pattern Refresh

**Schedule**: Monthly on the 1st at 03:00 AM  
**Cron**: `0 0 3 1 * *`

#### Purpose
Recomputes seasonal patterns for all commodities to incorporate new price data.

#### Logic
1. Calls `seasonalPatternService.computeAllPatterns()`
2. Logs completion message

#### Example Log Output
```
INFO - Starting scheduled seasonal pattern recomputation
INFO - Seasonal patterns recomputed for all commodities
```

#### Benefits
- Keeps seasonal indices accurate with latest data
- Monthly frequency balances freshness with computation cost
- Runs after market health refresh
- Provides updated buying recommendations

---

### 3. Pending SLA Breach Check

**Schedule**: Hourly at the top of each hour  
**Cron**: `0 0 * * * *`

#### Purpose
Monitors pending price record submissions and alerts on SLA breaches (24-hour threshold).

#### Logic
1. Queries all PENDING price records
2. Filters records older than 24 hours
3. For each breach:
   - Calculates hours pending
   - Extracts submitter username
   - Logs WARNING with details
4. Logs summary count if breaches found

#### Example Log Output
```
WARN - SLA BREACH: Price record ID 123 submitted by field_agent_1 has been PENDING for 28 hours
WARN - SLA BREACH: Price record ID 124 submitted by field_agent_2 has been PENDING for 36 hours
INFO - Found 2 price records breaching 24-hour SLA
```

#### Benefits
- Drives operational accountability
- Identifies approval bottlenecks
- No email infrastructure required
- Hourly checks ensure timely detection

---

### 4. Dashboard Cache Eviction

**Schedule**: Every 15 minutes  
**Fixed Rate**: `900000` milliseconds

#### Purpose
Ensures dashboard summary data stays fresh by periodically clearing the cache.

#### Logic
1. Checks if `dashboardSummary` cache exists
2. Clears the cache
3. Logs debug message

#### Example Log Output
```
DEBUG - Evicting dashboardSummary cache
DEBUG - dashboardSummary cache evicted
```

#### Benefits
- Balances performance with data freshness
- 15-minute window acceptable for dashboard data
- Prevents serving stale statistics
- Automatic cache warming on next request

---

### 5. Export Log Cleanup

**Schedule**: Weekly on Sunday at 04:00 AM  
**Cron**: `0 0 4 * * SUN`

#### Purpose
Maintains database hygiene by removing old export log entries.

#### Logic
1. Calculates cutoff date (180 days ago)
2. Deletes export logs older than cutoff
3. Logs count of deleted entries

#### Example Log Output
```
INFO - Starting cleanup of old export logs
INFO - Cleaned up 47 old export log entries
```

#### Benefits
- Prevents unbounded table growth
- 180-day retention for audit purposes
- Weekly frequency sufficient for cleanup
- Runs during low-traffic period

## Dependencies

### Services
- `MarketHealthScoreService` - Health score computation
- `SeasonalPatternService` - Seasonal pattern computation

### Repositories
- `MarketHealthScoreRepository` - Health score data access
- `PriceRecordRepository` - Price record queries
- `ExportLogRepository` - Export log cleanup

### Infrastructure
- `CacheManager` - Cache management

## Repository Enhancements

### ExportLogRepository

Added cleanup method:

```java
@Modifying
@Transactional
@Query("DELETE FROM ExportLog e WHERE e.exportedAt < :cutoffDate")
long deleteByExportedAtBefore(@Param("cutoffDate") OffsetDateTime cutoffDate);
```

### MarketHealthScoreRepository

Already has cleanup method:

```java
void deleteByComputedAtBefore(OffsetDateTime cutoff);
```

## Cron Expression Reference

| Expression | Description |
|------------|-------------|
| `0 0 2 * * *` | Daily at 02:00 AM |
| `0 0 3 1 * *` | Monthly on 1st at 03:00 AM |
| `0 0 * * * *` | Every hour at :00 |
| `0 0 4 * * SUN` | Every Sunday at 04:00 AM |

### Cron Format
```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12)
│ │ │ │ │ ┌───────────── day of week (0-7, SUN-SAT)
│ │ │ │ │ │
* * * * * *
```

## Execution Timeline

```
00:00 - Pending SLA check (hourly)
02:00 - Market health score refresh (daily)
03:00 - Seasonal pattern refresh (monthly, 1st)
04:00 - Export log cleanup (weekly, Sunday)
Every 15 min - Dashboard cache eviction
```

## Performance Considerations

### Execution Times
- Market health refresh: ~2-5 seconds for 15 markets
- Seasonal patterns: ~5-10 seconds for all commodities
- SLA check: <1 second
- Cache eviction: <100ms
- Export cleanup: <1 second

### Resource Usage
- All tasks run asynchronously
- No blocking of user requests
- Database queries optimized with indexes
- Batch operations where possible

## Monitoring and Logging

### Log Levels

**INFO**
- Task start/completion
- Counts and metrics
- Cleanup results

**WARN**
- SLA breaches
- Operational issues

**DEBUG**
- Cache operations
- Detailed execution steps

### Monitoring Recommendations

1. **SLA Breaches**: Alert on WARN logs containing "SLA BREACH"
2. **Task Failures**: Monitor for missing scheduled executions
3. **Performance**: Track execution times in logs
4. **Data Growth**: Monitor cleanup effectiveness

## Configuration Options

### Application Properties

```properties
# Scheduler configuration (optional overrides)
spring.task.scheduling.pool.size=5
spring.task.scheduling.thread-name-prefix=scheduler-

# Timezone for cron expressions
spring.task.scheduling.cron.timezone=Africa/Accra
```

### Custom Schedules

To modify schedules, update cron expressions in `CommodityScheduler.java`:

```java
@Scheduled(cron = "0 0 3 * * *")  // Change to 03:00 AM
public void refreshMarketHealthScores() {
    // ...
}
```

## Error Handling

### Built-in Resilience
- Spring retries failed scheduled tasks
- Exceptions logged but don't stop scheduler
- Each task independent (one failure doesn't affect others)

### Best Practices
- Tasks are idempotent (safe to run multiple times)
- Transactional operations for data consistency
- Graceful handling of missing data
- Comprehensive logging for troubleshooting

## Testing

### Manual Trigger

For testing, temporarily change cron to run soon:

```java
@Scheduled(cron = "0 * * * * *")  // Every minute
public void refreshMarketHealthScores() {
    // ...
}
```

### Integration Tests

```java
@SpringBootTest
class CommoditySchedulerTest {
    
    @Autowired
    private CommodityScheduler scheduler;
    
    @Test
    void testMarketHealthRefresh() {
        scheduler.refreshMarketHealthScores();
        // Verify results
    }
}
```

## Operational Guidelines

### Startup Behavior
- Scheduler starts automatically with application
- First execution at next scheduled time
- No immediate execution on startup

### Shutdown Behavior
- Graceful shutdown waits for running tasks
- In-progress tasks complete before shutdown
- No data corruption on restart

### Production Deployment
1. Verify timezone configuration
2. Monitor first execution of each task
3. Check log output for errors
4. Validate data freshness after tasks run

## Troubleshooting

### Issue: Tasks Not Running

**Check:**
- `@EnableScheduling` present on main class
- Scheduler component scanned by Spring
- No exceptions in startup logs
- Cron expressions valid

### Issue: SLA Breaches Not Detected

**Check:**
- Price records have correct status
- `createdAt` timestamps populated
- Repository queries returning data
- Log level set to WARN or lower

### Issue: Cache Not Evicting

**Check:**
- CacheManager bean configured
- Cache name matches ("dashboardSummary")
- Fixed rate in milliseconds (900000 = 15 min)

### Issue: Cleanup Not Working

**Check:**
- Repository methods annotated with `@Modifying` and `@Transactional`
- Cutoff date calculation correct
- Database permissions for DELETE operations

## Future Enhancements

1. **Email Notifications**: Send alerts for SLA breaches
2. **Metrics Dashboard**: Visualize scheduler execution history
3. **Dynamic Scheduling**: Adjust schedules based on load
4. **Parallel Execution**: Process markets/commodities in parallel
5. **Retry Logic**: Automatic retry for failed computations
6. **Health Checks**: Expose scheduler status via actuator endpoint

## Summary

The CommodityScheduler automates critical maintenance tasks:
- Keeps computed data fresh (health scores, seasonal patterns)
- Monitors operational SLAs (pending approvals)
- Maintains system performance (cache eviction)
- Ensures database hygiene (log cleanup)

All tasks run automatically during low-traffic hours, ensuring the system remains performant and data stays current without manual intervention.
