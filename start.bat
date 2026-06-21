@echo off
title Z-Writer Dev Environment Launcher

echo ========================================
echo    Z-Writer Dev Environment Launcher
echo ========================================
echo.

:: Check Docker
echo [1/4] Checking Docker...
docker info >nul 2>&1
if errorlevel 1 (
    echo [FAIL] Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)
echo [OK] Docker is running
echo.

:: Start Docker containers
echo [2/4] Starting infrastructure services (PostgreSQL, Redis, ChromaDB)...
docker compose up -d
if errorlevel 1 (
    echo [FAIL] Docker containers failed to start
    pause
    exit /b 1
)
echo [OK] Infrastructure services started
echo.

:: Wait for services
echo Waiting for services to be ready...
timeout /t 5 /nobreak >nul

:: Start backend
echo [3/4] Starting backend (Spring Boot)...
cd backend
start "Z-Writer Backend" cmd /k "mvn spring-boot:run"
cd ..
echo [OK] Backend starting on port 8080
echo.

:: Start frontend
echo [4/4] Starting frontend (Vite)...
cd frontend
start "Z-Writer Frontend" cmd /k "npm run dev"
cd ..
echo [OK] Frontend starting on port 5173
echo.

echo ========================================
echo    All services launched!
echo ========================================
echo.
echo URLs:
echo   Frontend:    http://localhost:5173
echo   Backend API: http://localhost:8080
echo   PostgreSQL:  localhost:5432
echo   Redis:       localhost:6379
echo   ChromaDB:    localhost:8000
echo.
echo Press any key to close this window (services will keep running)
pause >nul
