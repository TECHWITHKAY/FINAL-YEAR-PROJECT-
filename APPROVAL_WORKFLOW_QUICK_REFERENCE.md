# Approval Workflow Quick Reference

## Status Flow

```
PENDING → APPROVED (included in analytics)
        → REJECTED (excluded from analytics)
```

## Role Permissions

| Action | ADMIN | FIELD_AGENT |
|--------|-------|-------------|
| Submit (auto-approved) | ✓ | ✗ |
| Submit (pending) | ✗ | ✓ |
| Approve/Reject | ✓ | ✗ |
| View pending | ✓ | ✗ |
| View own submissions | ✓ | ✓ |

## API Endpoints

### Submit Price Record
```http
POST /api/v1/price-records
Authorization: Bearer <token>

{
  "commodityId": 1,
  "marketId": 1,
  "price": 45.50,
  "recordedDate": "2026-03-10",
  "source": "Field Survey"
}
```

### View Pending (Admin Only)
```http
GET /api/v1/price-records/pending
Authorization: Bearer <admin_token>
```

### Approve Record (Admin Only)
```http
POST /api/v1/price-records/{id}/approve
Authorization: Bearer <admin_token>

{
  "priceRecordId": 101,
  "approved": true
}
```

### Reject Record (Admin Only)
```http
POST /api/v1/price-records/{id}/approve
Authorization: Bearer <admin_token>

{
  "priceRecordId": 102,
  "approved": false,
  "rejectionReason": "Price unrealistic"
}
```

### View My Submissions
```http
GET /api/v1/price-records/my-submissions
Authorization: Bearer <field_agent_token>
```

## Business Rules

1. **Only PENDING records can be reviewed**
   - Attempting to review APPROVED/REJECTED → BusinessRuleException

2. **Rejection requires reason**
   - Rejecting without reason → ValidationException

3. **Admin submissions auto-approved**
   - status = APPROVED
   - reviewedBy = submitter
   - reviewedAt = now

4. **Field agent submissions pending**
   - status = PENDING
   - Awaits admin review

5. **Analytics filter by APPROVED**
   - All analytics queries: WHERE status = 'APPROVED'
   - PENDING/REJECTED excluded

## Audit Trail

Every action creates audit record:
- SUBMITTED - When record created
- APPROVED - When admin approves
- REJECTED - When admin rejects

## Database Schema

### price_records (new columns)
- status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
- submitted_by BIGINT
- reviewed_by BIGINT
- reviewed_at TIMESTAMPTZ
- rejection_reason VARCHAR(500)

### price_record_audits (new table)
- id BIGSERIAL PRIMARY KEY
- price_record_id BIGINT NOT NULL
- action VARCHAR(50) NOT NULL
- performed_by BIGINT
- old_price DECIMAL(12, 2)
- new_price DECIMAL(12, 2)
- note VARCHAR(500)
- performed_at TIMESTAMPTZ NOT NULL

## Response Fields

### PriceRecordResponseDto
```json
{
  "id": 101,
  "status": "PENDING",
  "submittedByUsername": "field_agent_1",
  "reviewedByUsername": null,
  "reviewedAt": null,
  "rejectionReason": null
}
```

### PendingSubmissionResponseDto
```json
{
  "id": 101,
  "status": "PENDING",
  "submittedByUsername": "field_agent_1",
  "daysPending": 2
}
```

## Error Responses

### BusinessRuleException (400)
```json
{
  "status": "error",
  "message": "Only PENDING records can be reviewed"
}
```

### ValidationException (400)
```json
{
  "status": "error",
  "message": "Rejection reason is required when rejecting a price record"
}
```

## Testing Commands

### As Field Agent
```bash
# Submit price record
curl -X POST http://localhost:8080/api/v1/price-records \
  -H "Authorization: Bearer $FIELD_AGENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"commodityId":1,"marketId":1,"price":45.50,"recordedDate":"2026-03-10"}'

# View my submissions
curl http://localhost:8080/api/v1/price-records/my-submissions \
  -H "Authorization: Bearer $FIELD_AGENT_TOKEN"
```

### As Admin
```bash
# View pending
curl http://localhost:8080/api/v1/price-records/pending \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Approve
curl -X POST http://localhost:8080/api/v1/price-records/101/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"priceRecordId":101,"approved":true}'

# Reject
curl -X POST http://localhost:8080/api/v1/price-records/102/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"priceRecordId":102,"approved":false,"rejectionReason":"Price unrealistic"}'
```

## Migration Commands

```bash
# Run migrations
./mvnw flyway:migrate

# Check migration status
./mvnw flyway:info

# Repair if needed
./mvnw flyway:repair
```

## Key Files

### Entities
- `PriceRecord.java` - Updated with approval fields
- `PriceRecordAudit.java` - New audit entity

### Services
- `PriceRecordService.java` - Approval logic
- `AnalyticsService.java` - Status filtering

### Controllers
- `PriceRecordController.java` - Approval endpoints

### Migrations
- `V7__add_approval_workflow_to_price_records.sql`
- `V8__create_price_record_audits_table.sql`
