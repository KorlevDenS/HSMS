#!/usr/bin/env sh
set -eu

BASE_URL=${HSMS_SMOKE_BASE_URL:-https://localhost:${HSMS_HTTPS_PORT:-18443}}
RPS=${HSMS_NFR_RPS:-200}
DURATION_SECONDS=${HSMS_NFR_DURATION_SECONDS:-10}
CONCURRENCY=${HSMS_NFR_CONCURRENCY:-50}
MAX_P95_SECONDS=${HSMS_NFR_MAX_P95_SECONDS:-2.0}
RUN_ID=$(date +%Y%m%d%H%M%S)
RESULTS_FILE=$(mktemp "${TMPDIR:-/tmp}/hsms-nfr-results.XXXXXX")

json_field() {
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data[sys.argv[1]])' "$1"
}

login_token() {
  curl -skfsS \
    -H 'Content-Type: application/json' \
    -d "{\"login\":\"$1\",\"password\":\"hsms123\"}" \
    "$BASE_URL/api/v1/auth/login" |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])'
}

api_json() {
  method=$1
  path=$2
  token=$3
  body=$4
  curl -skfsS \
    -X "$method" \
    -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' \
    -d "$body" \
    "$BASE_URL/api/v1$path"
}

DISPATCHER_TOKEN=$(login_token dispatcher)
TELEMETRY_TOKEN=$(login_token crew)

HARVESTER_ID=$(api_json POST /harvesters "$DISPATCHER_TOKEN" "{\"name\":\"HV-NFR-$RUN_ID\",\"type\":\"LOAD\",\"status\":\"READY\",\"noiseLevel\":0.22,\"capacity\":130}" | json_field id)
CREW_ID=$(api_json POST /crews "$DISPATCHER_TOKEN" "{\"name\":\"Экипаж NFR $RUN_ID\",\"status\":\"READY\",\"contactChannel\":\"nfr-$RUN_ID\",\"memberCount\":5,\"assignedLogin\":\"crew\"}" | json_field id)

MISSION_BODY=$(python3 - "$HARVESTER_ID" "$CREW_ID" <<'PY'
import datetime as dt
import json
import sys

harvester_id = int(sys.argv[1])
crew_id = int(sys.argv[2])
now = dt.datetime.now(dt.timezone.utc)
payload = {
    "title": f"Docker NFR load {now.isoformat()}",
    "zoneId": 3,
    "harvesterId": harvester_id,
    "crewId": crew_id,
    "plannedStart": (now + dt.timedelta(minutes=5)).isoformat(),
    "plannedEnd": (now + dt.timedelta(hours=4)).isoformat(),
    "route": [
        {"seqNo": 1, "lat": 20.01, "lon": 51.74},
        {"seqNo": 2, "lat": 20.11, "lon": 51.89},
        {"seqNo": 3, "lat": 20.23, "lon": 52.02},
    ],
}
print(json.dumps(payload, ensure_ascii=False))
PY
)
MISSION_ID=$(api_json POST /missions "$DISPATCHER_TOKEN" "$MISSION_BODY" | json_field id)
api_json POST "/missions/$MISSION_ID/risk-assessments" "$DISPATCHER_TOKEN" '{}' >/dev/null
api_json POST "/missions/$MISSION_ID/launch" "$DISPATCHER_TOKEN" '{"confirmWarning":true,"reason":"Docker NFR load verification"}' >/dev/null

send_telemetry() {
  index=$1
  payload=$(python3 - "$RUN_ID" "$index" <<'PY'
import datetime as dt
import json
import sys

run_id = sys.argv[1]
index = int(sys.argv[2])
now = dt.datetime.now(dt.timezone.utc)
payload = {
    "externalEventId": f"nfr-{run_id}-{index}",
    "eventTime": now.isoformat(),
    "lat": 20.01 + (index % 20) * 0.001,
    "lon": 51.74 + (index % 20) * 0.001,
    "equipmentStatus": "NORMAL",
}
print(json.dumps(payload))
PY
)
  curl -skS -o /dev/null \
    -w "%{http_code} %{time_total}\n" \
    -H "Authorization: Bearer $TELEMETRY_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "$payload" \
    "$BASE_URL/api/v1/missions/$MISSION_ID/telemetry" >> "$RESULTS_FILE" 2>/dev/null
}

TOTAL_REQUESTS=$((RPS * DURATION_SECONDS))
START_SECONDS=$(date +%s)
request_index=0
for _second in $(seq 1 "$DURATION_SECONDS"); do
  batch_start=$(date +%s)
  in_batch=0
  while [ "$in_batch" -lt "$RPS" ]; do
    request_index=$((request_index + 1))
    in_batch=$((in_batch + 1))
    send_telemetry "$request_index" &
    if [ $((in_batch % CONCURRENCY)) -eq 0 ]; then
      wait
    fi
  done
  wait
  elapsed=$(( $(date +%s) - batch_start ))
  if [ "$elapsed" -lt 1 ]; then
    sleep 1
  fi
done
ELAPSED_SECONDS=$(( $(date +%s) - START_SECONDS ))
if [ "$ELAPSED_SECONDS" -le 0 ]; then
  ELAPSED_SECONDS=1
fi

TOTAL=$(wc -l < "$RESULTS_FILE" | tr -d ' ')
ERRORS=$(awk '$1 < 200 || $1 >= 300 { errors++ } END { print errors + 0 }' "$RESULTS_FILE")
STATUS_COUNTS=$(awk '{ counts[$1]++ } END { for (status in counts) printf "%s:%s ", status, counts[status] }' "$RESULTS_FILE")
P95=$(awk '{ print $2 }' "$RESULTS_FILE" | sort -n | awk -v total="$TOTAL" 'BEGIN { target = int(total * 0.95); if (target < 1) target = 1 } NR == target { print; found = 1 } END { if (!found) print 0 }')
ACHIEVED_RPS=$(awk -v total="$TOTAL" -v seconds="$ELAPSED_SECONDS" 'BEGIN { printf "%.2f", total / seconds }')

printf 'HSMS Docker NFR load: mission=%s total=%s expected=%s elapsed=%ss achieved_rps=%s p95=%ss errors=%s statuses=%s\n' \
  "$MISSION_ID" "$TOTAL" "$TOTAL_REQUESTS" "$ELAPSED_SECONDS" "$ACHIEVED_RPS" "$P95" "$ERRORS" "$STATUS_COUNTS"

awk -v errors="$ERRORS" 'BEGIN { exit errors == 0 ? 0 : 1 }'
awk -v p95="$P95" -v max="$MAX_P95_SECONDS" 'BEGIN { exit p95 <= max ? 0 : 1 }'
awk -v achieved="$ACHIEVED_RPS" -v target="$RPS" 'BEGIN { exit achieved >= target * 0.8 ? 0 : 1 }'
