#!/usr/bin/env bash
set -e

echo "============================================"
echo "  Smart Expense Reimbursement - Quick Start"
echo "============================================"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# 1. Infrastructure: MySQL + Redis
echo "[1/3] Starting MySQL + Redis ..."
cd "$ROOT/deploy"
docker-compose up -d mysql redis

# 2. Backend: Spring Boot
echo "[2/3] Starting backend ..."
cd "$ROOT/server"
mvn spring-boot:run &
SERVER_PID=$!

# 3. Frontend: Vue web-admin
echo "[3/3] Starting web-admin ..."
cd "$ROOT/web-admin"
npm run dev &
WEB_PID=$!

echo ""
echo "Started."
echo "  Web admin : http://localhost:3000"
echo "  Swagger   : http://localhost:8080/doc.html"
echo "Demo accounts (password: 123456): admin / zhangsan / lisi / wangwu"
echo "First launch auto-creates demo data (6 reimbursements + 5 trips)."
echo ""
echo "Press Ctrl+C to stop."

wait $SERVER_PID $WEB_PID
