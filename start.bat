@echo off
title AI Chat Platform - Start

cd /d "%~dp0"

echo ========================================
echo   AI Chat Platform - Starting services...
echo ========================================
echo.

echo [1/5] Checking and releasing port 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    echo [WARN] Port 8080 occupied by PID %%a, killing...
    powershell -Command "Stop-Process -Id %%a -Force" 2>nul
    echo [OK] Old process terminated
)
echo [OK] Port 8080 is free
echo.

echo [2/5] Starting PostgreSQL...
docker compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] PostgreSQL failed to start. Is Docker Desktop running?
    pause
    exit /b 1
)
echo [OK] PostgreSQL is running
echo.

echo [3/5] Starting Backend (Spring Boot, port 8080)...
start "AI-Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"
echo [OK] Backend starting in new window...
echo.

echo [4/5] Waiting 30s for backend to be ready...
timeout /t 30 /nobreak >nul

echo [5/5] Starting Frontend (Vite, port 5173)...
start "AI-Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"
echo [OK] Frontend starting in new window...
echo.

echo ========================================
echo   All services started!
echo   Frontend: http://localhost:5173
echo   Backend:  http://localhost:8080
echo ========================================
echo.
echo Close this window safely. Services run independently.
echo To stop: close the backend/frontend windows, then run:
echo   docker compose down
echo.
pause
