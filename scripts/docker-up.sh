#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PROJECT_NAME=${COMPOSE_PROJECT_NAME:-hsms_live}
BUILD=true
RESET_DB=false
WAIT_TIMEOUT_SECONDS=${HSMS_WAIT_TIMEOUT_SECONDS:-120}

usage() {
  cat <<'EOF'
Usage: scripts/docker-up.sh [options]

Idempotently builds, starts and verifies the full HSMS Docker stack:
PostgreSQL, Spring backend, React frontend, Caddy, Prometheus and Grafana.

Options:
  --project-name NAME     Docker Compose project name. Default: hsms_live
  --https-port PORT       Public HTTPS port for HSMS/Caddy. Default: 8443
  --grafana-port PORT     Public Grafana port. Default: 3000
  --prometheus-port PORT  Public Prometheus port. Default: 9090
  --postgres-port PORT    Public PostgreSQL port. Default: 5432
  --token-secret VALUE    JWT secret, at least 32 chars. Default is local-only.
  --no-build              Reuse existing images.
  --reset-db              Stop stack and remove this project's volumes before start.
                           Use when Flyway reports checksum mismatch after migration edits.
  --timeout SECONDS       Health wait timeout. Default: 120
  -h, --help              Show this help.

Examples:
  scripts/docker-up.sh
  scripts/docker-up.sh --reset-db
  scripts/docker-up.sh --project-name hsms_live --https-port 8443
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --project-name)
      PROJECT_NAME=${2:?--project-name requires a value}
      shift 2
      ;;
    --https-port)
      export HSMS_HTTPS_PORT=${2:?--https-port requires a value}
      shift 2
      ;;
    --grafana-port)
      export HSMS_GRAFANA_PORT=${2:?--grafana-port requires a value}
      shift 2
      ;;
    --prometheus-port)
      export HSMS_PROMETHEUS_PORT=${2:?--prometheus-port requires a value}
      shift 2
      ;;
    --postgres-port)
      export HSMS_POSTGRES_PORT=${2:?--postgres-port requires a value}
      shift 2
      ;;
    --token-secret)
      export HSMS_TOKEN_SECRET=${2:?--token-secret requires a value}
      shift 2
      ;;
    --timeout)
      WAIT_TIMEOUT_SECONDS=${2:?--timeout requires a value}
      shift 2
      ;;
    --no-build)
      BUILD=false
      shift
      ;;
    --reset-db)
      RESET_DB=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

require_command docker
require_command curl

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required: docker compose" >&2
  exit 1
fi

export COMPOSE_PROJECT_NAME=$PROJECT_NAME
export POSTGRES_USER=${POSTGRES_USER:-hsms}
export POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-hsms}
export HSMS_TOKEN_SECRET=${HSMS_TOKEN_SECRET:-local-docker-secret-please-change-32chars}
export HSMS_HTTPS_PORT=${HSMS_HTTPS_PORT:-8443}
export HSMS_GRAFANA_PORT=${HSMS_GRAFANA_PORT:-3000}
export HSMS_PROMETHEUS_PORT=${HSMS_PROMETHEUS_PORT:-9090}
export HSMS_POSTGRES_PORT=${HSMS_POSTGRES_PORT:-5432}
export HSMS_ALLOWED_ORIGINS=${HSMS_ALLOWED_ORIGINS:-https://localhost:${HSMS_HTTPS_PORT},https://127.0.0.1:${HSMS_HTTPS_PORT}}
export HSMS_REQUIRE_HTTPS=${HSMS_REQUIRE_HTTPS:-true}

if [ "${#HSMS_TOKEN_SECRET}" -lt 32 ]; then
  echo "HSMS_TOKEN_SECRET must contain at least 32 characters." >&2
  exit 1
fi

BASE_URL=${HSMS_BASE_URL:-https://localhost:${HSMS_HTTPS_PORT}}
PROM_URL=${HSMS_PROM_URL:-http://localhost:${HSMS_PROMETHEUS_PORT}}
GRAFANA_URL=${HSMS_GRAFANA_URL:-http://localhost:${HSMS_GRAFANA_PORT}}

cd "$ROOT_DIR"

compose() {
  docker compose -p "$PROJECT_NAME" "$@"
}

wait_for_url() {
  label=$1
  url=$2
  expected=$3
  start=$(date +%s)
  while :; do
    if body=$(curl -skfsS "$url" 2>/dev/null); then
      if printf '%s' "$body" | grep -q "$expected"; then
        return 0
      fi
    fi
    now=$(date +%s)
    if [ $((now - start)) -ge "$WAIT_TIMEOUT_SECONDS" ]; then
      echo "Timed out waiting for $label: $url" >&2
      return 1
    fi
    sleep 2
  done
}

wait_for_http_code() {
  label=$1
  url=$2
  start=$(date +%s)
  while :; do
    code=$(curl -sk -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true)
    if [ "$code" = "200" ]; then
      return 0
    fi
    now=$(date +%s)
    if [ $((now - start)) -ge "$WAIT_TIMEOUT_SECONDS" ]; then
      echo "Timed out waiting for $label: $url, last HTTP code: $code" >&2
      return 1
    fi
    sleep 2
  done
}

wait_for_prometheus_target() {
  start=$(date +%s)
  while :; do
    if curl -fsS "$PROM_URL/api/v1/targets?state=active" 2>/dev/null | grep -q '"health":"up"'; then
      return 0
    fi
    now=$(date +%s)
    if [ $((now - start)) -ge "$WAIT_TIMEOUT_SECONDS" ]; then
      echo "Prometheus target is not UP yet." >&2
      return 1
    fi
    sleep 2
  done
}

diagnose_failure() {
  echo
  echo "Docker status:"
  compose ps || true
  echo
  echo "Spring logs:"
  compose logs --tail=160 spring || true
  if compose logs --tail=240 spring 2>/dev/null | grep -q 'checksum mismatch'; then
    echo
    echo "Flyway checksum mismatch detected."
    echo "Rerun with: scripts/docker-up.sh --reset-db"
    echo "This removes only volumes for project '$PROJECT_NAME'."
  fi
}

echo "HSMS Docker stack"
echo "  project:    $PROJECT_NAME"
echo "  app:        $BASE_URL"
echo "  grafana:    $GRAFANA_URL"
echo "  prometheus: $PROM_URL"
echo

if [ "$RESET_DB" = "true" ]; then
  echo "Resetting project containers and volumes: $PROJECT_NAME"
  compose down -v --remove-orphans
fi

if [ "$BUILD" = "true" ]; then
  compose up -d --build --remove-orphans
else
  compose up -d --remove-orphans
fi

if compose ps -q caddy >/dev/null 2>&1; then
  compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile >/dev/null 2>&1 || compose restart caddy >/dev/null
fi

if ! wait_for_url "backend health" "$BASE_URL/actuator/health" '"status":"UP"'; then
  diagnose_failure
  exit 1
fi

HEALTH=$(curl -skfsS "$BASE_URL/actuator/health")
for module in common mission risk security insurance; do
  if ! printf '%s' "$HEALTH" | grep -q "\"$module\":true"; then
    echo "Module health check failed for module: $module" >&2
    diagnose_failure
    exit 1
  fi
done

wait_for_http_code "frontend" "$BASE_URL/"
wait_for_http_code "OpenAPI" "$BASE_URL/v3/api-docs"
wait_for_http_code "Swagger UI" "$BASE_URL/swagger-ui/index.html"
wait_for_url "Prometheus metrics" "$BASE_URL/actuator/prometheus" 'hsms_module_health'
wait_for_url "Prometheus readiness" "$PROM_URL/-/ready" 'Prometheus Server is Ready'
wait_for_url "Grafana health" "$GRAFANA_URL/api/health" '"database"'

if ! wait_for_prometheus_target; then
  diagnose_failure
  exit 1
fi

echo
echo "HSMS Docker stack is UP."
echo
echo "URLs:"
echo "  App:                $BASE_URL/"
echo "  Swagger UI:         $BASE_URL/swagger-ui/index.html"
echo "  OpenAPI JSON:       $BASE_URL/v3/api-docs"
echo "  Health:             $BASE_URL/actuator/health"
echo "  Prometheus metrics: $BASE_URL/actuator/prometheus"
echo "  Prometheus:         $PROM_URL/"
echo "  Prometheus targets: $PROM_URL/targets"
echo "  Grafana:            $GRAFANA_URL/"
echo "  Grafana dashboard:  $GRAFANA_URL/d/hsms-domain-operations/hsms-domain-operations"
echo "  PostgreSQL:         localhost:${HSMS_POSTGRES_PORT} db=hsms user=${POSTGRES_USER} password=${POSTGRES_PASSWORD}"
echo
echo "HSMS users:"
echo "  dispatcher / hsms123"
echo "  crew       / hsms123"
echo "  security   / hsms123"
echo "  insurance  / hsms123"
echo "  management / hsms123"
echo "  admin      / hsms123"
echo
echo "Grafana:"
echo "  admin / admin"
echo
echo "Note: Caddy uses a local TLS certificate. Your browser may require Advanced -> Continue."
