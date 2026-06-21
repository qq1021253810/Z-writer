@echo off
title Z-Writer Service Status Check

echo ========================================
echo    Z-Writer Service Status Check
echo ========================================
echo.

:: Check Docker
echo [1/5] Docker:
docker info >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] Docker is not running
) else (
    echo   [OK] Docker is running
)
echo.

:: Check containers
echo [2/5] Containers:
docker ps --filter "name=zwriter" --format "table {{.Names}}\t{{.Status}}"
echo.

:: Check backend port
echo [3/5] Backend (8080):
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] Backend is not running
) else (
    echo   [OK] Backend is running
)
echo.

:: Check frontend port
echo [4/5] Frontend (5173):
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] Frontend is not running
) else (
    echo   [OK] Frontend is running
)
echo.

:: URLs
echo [5/5] Access URLs:
echo   Frontend:    http://localhost:5173
echo   Backend API: http://localhost:8080
echo.

pause
