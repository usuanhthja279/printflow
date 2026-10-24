#!/usr/bin/env bash
# Phase 0 local smoke: bash scripts/smoke.sh [http://localhost:8080]
set -euo pipefail
API_BASE="${1:-http://localhost:8080}"
if [[ -z "${DEMO_ADMIN_KEY:-}" && -f .env ]]; then
  DEMO_ADMIN_KEY="$(grep '^DEMO_ADMIN_KEY=' .env | head -n1 | cut -d'=' -f2-)"
fi
: "${DEMO_ADMIN_KEY:?Set DEMO_ADMIN_KEY or run from repository root after copying .env.example to .env}"
command -v python3 >/dev/null
command -v curl >/dev/null
printf '[1/5] Health...\n'
curl -fsS "$API_BASE/actuator/health" | python3 -c 'import json,sys;assert json.load(sys.stdin)["status"]=="UP"'
printf '[2/5] Catalogue...\n'
curl -fsS "$API_BASE/api/public/catalog" | python3 -c 'import json,sys;assert len(json.load(sys.stdin))>=5'
printf '[3/5] Create enquiry...\n'
created="$(curl -fsS -H 'Content-Type: application/json' -d '{"customerName":"PrintFlow Smoke Test","email":"smoke@example.com","phone":"9876543210","productCode":"BROCHURES","quantity":5000,"privacyConsent":true,"privacyVersion":"2026-10-23","size":"A4","paperGsm":170,"printSides":"DOUBLE","finish":"MATTE","notes":"Synthetic API smoke record"}' "$API_BASE/api/public/enquiries")"
ID="$(printf '%s' "$created" | python3 -c 'import json,sys;print(json.load(sys.stdin)["publicId"])')"
TOKEN="$(printf '%s' "$created" | python3 -c 'import json,sys;print(json.load(sys.stdin)["trackingToken"])')"
printf '[4/5] Owner update...\n'
admin_list="$(curl -fsS -H "X-Demo-Key: $DEMO_ADMIN_KEY" "$API_BASE/api/demo/enquiries")"
# Simpler, reliable interpolation through process environment.
internal_id="$(TARGET_PUBLIC_ID="$ID" LIST_JSON="$admin_list" python3 -c 'import os,json;print(next(e["id"] for e in json.loads(os.environ["LIST_JSON"]) if e["publicId"]==os.environ["TARGET_PUBLIC_ID"]))')"
curl -fsS -X PATCH -H "X-Demo-Key: $DEMO_ADMIN_KEY" -H 'Content-Type: application/json' -d '{"status":"REVIEWING"}' "$API_BASE/api/demo/enquiries/$internal_id/status" | python3 -c 'import json,sys;assert json.load(sys.stdin)["status"]=="REVIEWING"'
printf '[5/5] Customer secure tracking...\n'
curl -fsS -H "X-Tracking-Token: $TOKEN" "$API_BASE/api/public/enquiries/$ID" | python3 -c 'import json,sys;d=json.load(sys.stdin);assert d["status"]=="REVIEWING" and len(d["events"])>=2'
printf 'PASS: full Phase 0 API workflow\n'
