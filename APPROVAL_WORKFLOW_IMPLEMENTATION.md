# Price Record Approval Workflow Implementation

## Overview

This implementation adds a field agent submission and admin approval workflow to the price record system. Field agents can submit price records that require admin approval before being included in analytics.

## Architecture Changes

### Entity Updates

#### 1. PriceRecord Entity
**New Fields:**
- `status` (PriceRecordStatus enum: PENDING, APPROVED, REJECTED)
- `submittedBy` (ManyToOne → User)
- `reviewedBy` (ManyToOne → User)
- `reviewedAt` (OffsetDateTime)
- `rejectionReason` (String)

#### 2. PriceRecordAudit Entity (New)
**Purpose:** Track all actions performed on price records

**Fields:**
- `id` (BIGSERIAL)
- `priceRecord` (ManyToOne)
- `action` (String: SUBMITTED, APPROVED, REJECTED)
- `performedBy` (ManyToOne User)
- `oldPrice` (BigDecimal)
- `newPrice` (BigDecimal)
- `note` (String)
- `performedAt` (OffsetDateTime)

### New Enums

#### PriceRecordStatus
```java
public enum PriceRecordStatus {
    PENDING,    // Awaiting admin review
    APPROVED,   // Approved and included in analytics
    REJECTED    // Rejected with reason
}
```

### New DTOs

#### PriceRecordApprovalDto
```java
public record PriceRecordApprovalDto(
    @NotNull Long priceRecordId,
    @NotNull Boolean approved,
    String rejectionReason  // Required when approved=false
) {}
```

#### PendingSubmissionResponseDto
Extends PriceRecordResponseDto with:
- `submittedByUsername`
- `daysPending` (calculated field)

### Repository Changes

#### PriceRecordRepository
**New Methods:**
- `findByStatus(PriceRecordStatus)` - Get records by status
- `findBySubmittedByIdAndStatus(Long, PriceRecordStatus)` - Get user's submissions
- `countByStatus(PriceRecordStatus)` - Count records by status

#### PriceRecordAuditRepository (New)
**Methods:**
- `findByPriceRecordIdOrderByPerformedAtDesc(Long)` - Get audit trail

### Service Layer Changes

#### PriceRecordService

**Updated Methods:**

1. **createPriceRecord(PriceRecordRequestDto, UserPrincipal)**
   - If submitter is ADMIN → status = APPROVED (auto-approved)
   - If submitter is FIELD_AGENT → status = PENDING (requires review)
   - Creates audit record with action = "SUBMITTED"

2. **approvePriceRecord(Long, PriceRecordApprovalDto, UserPrincipal)**
   - Validates record is PENDING
   - If approved: Sets status = APPROVED, records reviewer
   - If rejected: Validates rejection reason, sets status = REJECTED
   - Creates audit record

**New Methods:**

3. **getPendingRecords()** → List<PendingSubmissionResponseDto>
   - Returns all PENDING records with days pending calculation

4. **getMySubmissions(Long userId)** → List<PriceRecordResponseDto>
   - Returns user's PENDING submissions

#### AnalyticsService

**Critical Change:** All SQL queries now filter by `status = 'APPROVED'`

Updated queries:
- `getMonthlyPriceTrend()` - Added WHERE pr.status = 'APPROVED'
- `getCityPriceComparison()` - Added WHERE pr.status = 'APPROVED'
- `getPriceVolatility()` - Added WHERE pr.status = 'APPROVED'
- `getInflationTrend()` - Added WHERE pr.status = 'APPROVED'
- `getMovingAverageForecast()` - Added WHERE pr.status = 'APPROVED'

### Controller Changes

#### PriceRecordController

**Updated Endpoints:**

1. **POST /api/v1/price-records**
   - Now requires @CurrentUser UserPrincipal
   - Accessible by ADMIN and FIELD_AGENT
   - Calls createPriceRecord(dto, principal)

**New Endpoints:**

2. **POST /api/v1/price-records/{id}/approve**
   - Approve or reject a price record
   - Requires ADMIN role
   - Body: PriceRecordApprovalDto

3. **GET /api/v1/price-records/pending**
   - Get all pending submissions
   - Requires ADMIN role
   - Returns List<PendingSubmissionResponseDto>

4. **GET /api/v1/price-records/my-submissions**
   - Get current user's pending submissions
   - Requires FIELD_AGENT or ADMIN role
   - Uses @CurrentUser to identify user

### Exception Handling

**New Exceptions:**

1. **BusinessRuleException**
   - Thrown when business rules are violated
   - Example: "Only PENDING records can be reviewed"

2. **ValidationException**
   - Thrown for validation errors
   - Example: "Rejection reason is required when rejecting"

Both handled in GlobalExceptionHandler with 400 Bad Request

### Database Migrations

#### V7__add_approval_workflow_to_price_records.sql
```sql
ALTER TABLE price_records
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
ADD COLUMN submitted_by BIGINT,
ADD COLUMN reviewed_by BIGINT,
ADD COLUMN reviewed_at TIMESTAMPTZ,
ADD COLUMN rejection_reason VARCHAR(500);

-- Foreign keys and indexes
```

#### V8__create_price_record_audits_table.sql
```sql
CREATE TABLE price_record_audits (
    id BIGSERIAL PRIMARY KEY,
    price_record_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT,
    old_price DECIMAL(12, 2),
    new_price DECIMAL(12, 2),
    note VARCHAR(500),
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## Workflow Diagrams

### Submission Flow

```
Field Agent submits price record
    ↓
Status = PENDING
    ↓
Audit: action = "SUBMITTED"
    ↓
Awaits admin review
```

### Admin Submission Flow

```
Admin submits price record
    ↓
Status = APPROVED (auto-approved)
    ↓
reviewedBy = submitter
reviewedAt = now
    ↓
Audit: action = "SUBMITTED"
    ↓
Immediately available in analytics
```

### Approval Flow

```
Admin reviews PENDING record
    ↓
Decision: Approve or Reject?
    ↓
If Approve:
    - Status = APPROVED
    - reviewedBy = admin
    - reviewedAt = now
    - Audit: action = "APPROVED"
    - Now included in analytics
    ↓
If Reject:
    - Validate rejection reason
    - Status = REJECTED
    - rejectionReason = reason
    - reviewedBy = admin
    - reviewedAt = now
    - Audit: action = "REJECTED"
    - Excluded from analytics
```

## API Usage Examples

### 1. Field Agent Submits Price Record

**Request:**
```http
POST /api/v1/price-records
Authorization: Bearer <field_agent_token>
Content-Type: application/json

{
  "commodityId": 1,
  "marketId": 1,
  "price": 45.50,
  "recordedDate": "2026-03-10",
  "source": "Field Survey"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Price record created successfully",
  "data": {
    "id": 101,
    "status": "PENDING",
    "submittedByUsername": "field_agent_1",
    "reviewedByUsername": null,
    "reviewedAt": null
  }
}
```

### 2. Admin Views Pending Records

**Request:**
```http
GET /api/v1/price-records/pending
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "id": 101,
      "commodityName": "Rice",
      "marketName": "Makola Market",
      "price": 45.50,
      "status": "PENDING",
      "submittedByUsername": "field_agent_1",
      "daysPending": 2
    }
  ]
}
```

### 3. Admin Approves Record

**Request:**
```http
POST /api/v1/price-records/101/approve
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "priceRecordId": 101,
  "approved": true
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Price record reviewed successfully",
  "data": {
    "id": 101,
    "status": "APPROVED",
    "reviewedByUsername": "admin",
    "reviewedAt": "2026-03-12T10:30:00Z"
  }
}
```

### 4. Admin Rejects Record

**Request:**
```http
POST /api/v1/price-records/102/approve
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "priceRecordId": 102,
  "approved": false,
  "rejectionReason": "Price seems unrealistic for this market"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Price record reviewed successfully",
  "data": {
    "id": 102,
    "status": "REJECTED",
    "rejectionReason": "Price seems unrealistic for this market",
    "reviewedByUsername": "admin",
    "reviewedAt": "2026-03-12T10:35:00Z"
  }
}
```

### 5. Field Agent Views Their Submissions

**Request:**
```http
GET /api/v1/price-records/my-submissions
Authorization: Bearer <field_agent_token>
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "id": 101,
      "status": "PENDING",
      "submittedByUsername": "field_agent_1",
      "createdAt": "2026-03-10T14:00:00Z"
    }
  ]
}
```

## Security Considerations

### Role-Based Access Control

| Endpoint | ADMIN | FIELD_AGENT | VIEWER |
|----------|-------|-------------|--------|
| POST /price-records | ✓ (auto-approved) | ✓ (pending) | ✗ |
| POST /{id}/approve | ✓ | ✗ | ✗ |
| GET /pending | ✓ | ✗ | ✗ |
| GET /my-submissions | ✓ | ✓ | ✗ |
| GET /price-records | ✓ | ✓ | ✓ |

### Data Filtering

- Analytics queries ONLY include APPROVED records
- PENDING and REJECTED records are excluded from public analytics
- Field agents can only see their own submissions
- Admins can see all submissions

## Testing Checklist

- [ ] Field agent can submit price record (status = PENDING)
- [ ] Admin can submit price record (status = APPROVED)
- [ ] Admin can view all pending records
- [ ] Admin can approve pending record
- [ ] Admin can reject pending record with reason
- [ ] Rejection without reason throws ValidationException
- [ ] Approving non-PENDING record throws BusinessRuleException
- [ ] Field agent can view their own submissions
- [ ] Analytics queries exclude PENDING/REJECTED records
- [ ] Audit trail is created for all actions
- [ ] Days pending is calculated correctly

## Migration Path

### For Existing Data

All existing price records will have:
- `status = 'APPROVED'` (default in migration)
- `submittedBy = NULL`
- `reviewedBy = NULL`

This ensures existing data remains in analytics.

### Deployment Steps

1. Run Flyway migrations (V7, V8)
2. Deploy updated application
3. Verify existing records have status = APPROVED
4. Test field agent submission workflow
5. Test admin approval workflow
6. Verify analytics queries filter correctly

## Files Created/Modified

### New Files (13)
1. `PriceRecordStatus.java` - Enum
2. `PriceRecordAudit.java` - Entity
3. `PriceRecordAuditRepository.java` - Repository
4. `PriceRecordApprovalDto.java` - DTO
5. `PendingSubmissionResponseDto.java` - DTO
6. `BusinessRuleException.java` - Exception
7. `ValidationException.java` - Exception
8. `V7__add_approval_workflow_to_price_records.sql` - Migration
9. `V8__create_price_record_audits_table.sql` - Migration
10. `APPROVAL_WORKFLOW_IMPLEMENTATION.md` - Documentation

### Modified Files (6)
1. `PriceRecord.java` - Added approval fields
2. `PriceRecordRepository.java` - Added query methods
3. `PriceRecordService.java` - Complete rewrite with approval logic
4. `PriceRecordController.java` - Added approval endpoints
5. `AnalyticsService.java` - Added status filters to all queries
6. `GlobalExceptionHandler.java` - Added new exception handlers
7. `PriceRecordResponseDto.java` - Added approval fields

## Performance Considerations

- Indexes added on `status` and `submitted_by` columns
- Audit table has index on `price_record_id` and `performed_at`
- Analytics queries remain efficient with status filter
- Consider archiving old audit records periodically

---

**Implementation Date:** March 10, 2026  
**Spring Boot Version:** 3.5.11  
**Database:** PostgreSQL
