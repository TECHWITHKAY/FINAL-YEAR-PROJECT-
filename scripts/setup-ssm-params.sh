#!/usr/bin/env bash
# ============================================================================
# setup-ssm-params.sh
# Creates/updates SSM Parameter Store SecureString parameters for CommodityGH.
# All values are read from local shell environment variables — never hardcoded.
# Idempotent: safe to run multiple times (uses --overwrite).
#
# Usage:
#   export DB_URL="jdbc:postgresql://<rds-endpoint>:5432/commoditygh"
#   export DB_USER="commoditygh"
#   export DB_PASSWORD="..."
#   export JWT_SECRET="..."
#   export MAIL_HOST="smtp-relay.brevo.com"
#   export MAIL_USERNAME="..."
#   export MAIL_PASSWORD="..."
#   export ADMIN_USERNAME="..."
#   export ADMIN_PASSWORD="..."
#   export ADMIN_EMAIL="..."
#   export CORS_ALLOWED_ORIGINS="https://<cloudfront-domain>"
#   export SPRING_PROFILES_ACTIVE="prod"
#   bash scripts/setup-ssm-params.sh
# ============================================================================

set -euo pipefail

PREFIX="/commoditygh/prod"

PARAMS=(
  "DB_URL"
  "DB_USER"
  "DB_PASSWORD"
  "JWT_SECRET"
  "MAIL_HOST"
  "MAIL_USERNAME"
  "MAIL_PASSWORD"
  "ADMIN_USERNAME"
  "ADMIN_PASSWORD"
  "ADMIN_EMAIL"
  "CORS_ALLOWED_ORIGINS"
  "SPRING_PROFILES_ACTIVE"
)

echo "==> Creating/updating SSM parameters under ${PREFIX}/ ..."

for PARAM in "${PARAMS[@]}"; do
  VALUE="${!PARAM:-}"

  if [ -z "$VALUE" ]; then
    echo "WARNING: Environment variable ${PARAM} is not set — skipping."
    continue
  fi

  aws ssm put-parameter \
    --name "${PREFIX}/${PARAM}" \
    --value "${VALUE}" \
    --type SecureString \
    --overwrite \
    --no-cli-pager

  echo "  ✓ ${PREFIX}/${PARAM}"
done

echo "==> Done. ${#PARAMS[@]} parameters processed."
