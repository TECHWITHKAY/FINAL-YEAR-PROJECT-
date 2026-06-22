# RDS PostgreSQL Setup Guide for CommodityGH

## Prerequisites

- AWS CLI v2 configured with appropriate credentials
- A default VPC (or custom VPC with private subnets)
- An App Runner VPC Connector already created (see `apprunner.json`)

## 1. Create Security Group

Restrict inbound traffic to port 5432, allowing only the App Runner VPC Connector's subnet CIDR.

```bash
# Create the security group
aws ec2 create-security-group \
  --group-name commoditygh-rds-sg \
  --description "CommodityGH RDS - allow PostgreSQL from App Runner VPC Connector" \
  --no-cli-pager

# Note the GroupId from the output, e.g. sg-0abc123def456

# Authorize inbound from App Runner VPC Connector CIDR
# Replace <SG_ID> with the GroupId above
# Replace <APPRUNNER_SUBNET_CIDR> with your VPC Connector's subnet CIDR (e.g. 10.0.1.0/24)
aws ec2 authorize-security-group-ingress \
  --group-id <SG_ID> \
  --protocol tcp \
  --port 5432 \
  --cidr <APPRUNNER_SUBNET_CIDR> \
  --no-cli-pager
```

## 2. Create the RDS Instance

```bash
aws rds create-db-instance \
  --db-instance-identifier commoditygh-db \
  --db-instance-class db.t4g.micro \
  --engine postgres \
  --engine-version "16" \
  --master-username commoditygh \
  --master-user-password "<STRONG_PASSWORD>" \
  --allocated-storage 20 \
  --storage-type gp3 \
  --db-name commoditygh \
  --vpc-security-group-ids <SG_ID> \
  --backup-retention-period 7 \
  --no-publicly-accessible \
  --storage-encrypted \
  --no-cli-pager
```

> [!IMPORTANT]
> Replace `<STRONG_PASSWORD>` with a strong password. This same password must be stored in SSM as `DB_PASSWORD`.

## 3. Wait for the Instance to Become Available

```bash
aws rds wait db-instance-available \
  --db-instance-identifier commoditygh-db
```

## 4. Retrieve the Endpoint

```bash
aws rds describe-db-instances \
  --db-instance-identifier commoditygh-db \
  --query "DBInstances[0].Endpoint.Address" \
  --output text
```

The output will be something like:
```
commoditygh-db.xxxxxxxxxxxx.us-east-1.rds.amazonaws.com
```

## 5. Set the SSM Parameter

Use the endpoint to construct the JDBC URL and store it in SSM:

```bash
export DB_URL="jdbc:postgresql://commoditygh-db.xxxxxxxxxxxx.us-east-1.rds.amazonaws.com:5432/commoditygh"
export DB_USER="commoditygh"
export DB_PASSWORD="<STRONG_PASSWORD>"

# Then run the SSM setup script:
bash scripts/setup-ssm-params.sh
```

## 6. Flyway Migrations

**No manual SQL is required.** The application uses Flyway with `baseline-on-migrate: true`. On the first startup, Flyway will automatically apply all 14 migrations in order:

| Migration | Description |
|-----------|-------------|
| V1 | Create cities table |
| V2 | Create markets table |
| V3 | Create commodities table |
| V4 | Create price_records table |
| V5 | Create users table |
| V6 | Seed initial data |
| V7 | Add approval workflow to price_records |
| V8 | Create price_record_audits table |
| V9 | Create market_health_scores table |
| V10 | Create seasonal_patterns table |
| V11 | Create export_logs table |
| V12 | Add check constraints |
| V13 | Create password_reset_tokens table |
| V14 | Add agent metadata to users |

## 7. Verify Connectivity

After App Runner deploys, confirm the health check passes:

```bash
# From the App Runner service URL
curl https://<apprunner-service-url>/actuator/health
# Expected: {"status":"UP"}
```

If the health check fails, verify:
1. The VPC Connector's subnets can reach the RDS security group
2. The `DB_URL` SSM parameter has the correct JDBC endpoint
3. The `DB_USER` and `DB_PASSWORD` SSM parameters match the RDS credentials

## Cleanup

To delete the RDS instance (destructive — all data will be lost):

```bash
aws rds delete-db-instance \
  --db-instance-identifier commoditygh-db \
  --skip-final-snapshot \
  --no-cli-pager
```
