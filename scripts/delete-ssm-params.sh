#!/usr/bin/env bash
# ============================================================================
# delete-ssm-params.sh
# Tears down all SSM Parameter Store parameters for CommodityGH.
# Idempotent: silently ignores parameters that don't exist.
#
# Usage:
#   bash scripts/delete-ssm-params.sh
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

echo "==> Deleting SSM parameters under ${PREFIX}/ ..."

for PARAM in "${PARAMS[@]}"; do
  if aws ssm delete-parameter \
    --name "${PREFIX}/${PARAM}" \
    --no-cli-pager 2>/dev/null; then
    echo "  ✓ Deleted ${PREFIX}/${PARAM}"
  else
    echo "  - ${PREFIX}/${PARAM} does not exist — skipping."
  fi
done

echo "==> Done. Teardown complete."
