#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PROJECT_NAME=${COMPOSE_PROJECT_NAME:-hsms_nfr_smoke}
KEEP_STACK=${HSMS_SMOKE_KEEP:-false}

export POSTGRES_USER=${POSTGRES_USER:-hsms}
export POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-hsms}
export HSMS_TOKEN_SECRET=${HSMS_TOKEN_SECRET:-hsms-production-smoke-secret-32-chars-min}
export HSMS_HTTPS_PORT=${HSMS_HTTPS_PORT:-18443}
export HSMS_PROMETHEUS_PORT=${HSMS_PROMETHEUS_PORT:-19090}
export HSMS_GRAFANA_PORT=${HSMS_GRAFANA_PORT:-13000}
export HSMS_POSTGRES_PORT=${HSMS_POSTGRES_PORT:-15432}
BASE_URL=${HSMS_SMOKE_BASE_URL:-https://localhost:${HSMS_HTTPS_PORT}}
PROM_URL=${HSMS_SMOKE_PROM_URL:-http://localhost:${HSMS_PROMETHEUS_PORT}}
export HSMS_ALLOWED_ORIGINS=${HSMS_ALLOWED_ORIGINS:-https://localhost:${HSMS_HTTPS_PORT},https://127.0.0.1:${HSMS_HTTPS_PORT}}
export HSMS_REQUIRE_HTTPS=${HSMS_REQUIRE_HTTPS:-true}

cd "$ROOT_DIR"

compose() {
  docker compose -p "$PROJECT_NAME" "$@"
}

cleanup() {
  if [ "$KEEP_STACK" != "true" ]; then
    compose down -v --remove-orphans >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

wait_for_health() {
  attempt=0
  until curl -skfsS "$BASE_URL/actuator/health" | grep -q '"status":"UP"'; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 90 ]; then
      echo "HSMS health check did not become UP" >&2
      compose logs --tail=200 spring >&2 || true
      return 1
    fi
    sleep 2
  done
}

login_token() {
  curl -skfsS \
    -H 'Content-Type: application/json' \
    -d '{"login":"admin","password":"hsms123"}' \
    "$BASE_URL/api/v1/auth/login" |
    sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

db_user_count() {
  compose exec -T db sh -c \
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d hsms -Atc "select count(*) from hsms_user;"'
}

echo "Starting HSMS production smoke stack: $PROJECT_NAME"
compose up -d --build db spring react caddy prometheus
wait_for_health

TOKEN=$(login_token)
if [ -z "$TOKEN" ]; then
  echo "Could not obtain admin token" >&2
  exit 1
fi

curl -skfsS -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/bootstrap" >/dev/null
USER_COUNT_BEFORE=$(db_user_count)
if [ "$USER_COUNT_BEFORE" -le 0 ]; then
  echo "Unexpected empty hsms_user table before restart" >&2
  exit 1
fi

curl -skfsS "$BASE_URL/actuator/prometheus" | grep -Eq '^hsms_missions_active|^hsms_incidents_open|^hsms_insurance_cases_open'
curl -fsS "$PROM_URL/api/v1/query?query=up%7Bjob%3D%22spring%22%7D" | grep -q '"status":"success"'

echo "Restarting backend to verify PostgreSQL persistence"
compose restart spring >/dev/null
wait_for_health

TOKEN=$(login_token)
curl -skfsS -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/bootstrap" >/dev/null
USER_COUNT_AFTER=$(db_user_count)

if [ "$USER_COUNT_BEFORE" != "$USER_COUNT_AFTER" ]; then
  echo "Persistence check failed: users before=$USER_COUNT_BEFORE after=$USER_COUNT_AFTER" >&2
  exit 1
fi

echo "HSMS production smoke passed: health, auth, bootstrap, domain metrics, Prometheus scrape, restart persistence."
