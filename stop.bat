@echo off
title Z-Writer Dev Environment Stopper

echo ========================================
echo    Z-Writer Dev Environment Stopper
echo ========================================
echo.

:: Stop backend
echo [1/3] Stopping backend...
taskkill /FI "WINDOWTITLE eq Z-Writer Backend*" >nul 2>&1
echo [OK] Backend stopped
echo.

:: Stop frontend
echo [2/3] Stopping frontend...
taskkill /FI "WINDOWTITLE eq Z-Writer Frontend*" >nul 2>&1
echo [OK] Frontend stopped
echo.

:: Stop Docker containers
echo [3/3] Stopping infrastructure services...
docker compose down
echo [OK] Infrastructure services stopped
echo.

echo ========================================
echo    All services stopped
echo ========================================
echo.
pause
