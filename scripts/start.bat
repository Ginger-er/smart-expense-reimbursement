@echo off
chcp 65001 >nul
setlocal

echo ============================================
echo   Smart Expense Reimbursement - Quick Start
echo ============================================
echo.

REM ---- 1. Infrastructure: MySQL + Redis ----
echo [1/3] Starting MySQL + Redis ...
cd /d "%~dp0..\deploy"
docker-compose up -d mysql redis
if errorlevel 1 (
  echo [ERROR] Failed to start MySQL/Redis. Is Docker Desktop running?
  pause
  exit /b 1
)

REM ---- 2. Backend: Spring Boot (needs JDK17 + Maven) ----
echo [2/3] Starting backend server (new window) ...
cd /d "%~dp0..\server"
start "smart-expense-server" cmd /k "mvn spring-boot:run"

REM ---- 3. Frontend: Vue web-admin ----
echo [3/3] Starting web-admin (new window) ...
cd /d "%~dp0..\web-admin"
start "smart-expense-web" cmd /k "npm run dev"

echo.
echo ============================================
echo   Started. Open:
echo     Web admin : http://localhost:3000
echo     Swagger   : http://localhost:8080/doc.html
echo.
echo   Demo accounts (password: 123456):
echo     admin     - Administrator
echo     zhangsan  - Employee
echo     lisi      - Manager
echo     wangwu    - Finance
echo.
echo   First launch auto-creates departments, users
echo   and demo data (6 reimbursements + 5 trips).
echo ============================================
echo.
pause
endlocal
