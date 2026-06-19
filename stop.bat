@echo off
chcp 65001 >nul
title Z-Writer 开发环境停止器

echo ========================================
echo    Z-Writer 开发环境停止器
echo ========================================
echo.

:: 停止后端服务
echo [1/2] 停止后端服务...
taskkill /FI "WINDOWTITLE eq Z-Writer Backend*" >nul 2>&1
echo ✅ 后端服务已停止
echo.

:: 停止前端服务
echo [2/2] 停止前端服务...
taskkill /FI "WINDOWTITLE eq Z-Writer Frontend*" >nul 2>&1
echo ✅ 前端服务已停止
echo.

:: 停止 Docker 容器
echo [3/3] 停止基础设施服务...
docker compose down
echo ✅ 基础设施服务已停止
echo.

echo ========================================
echo    所有服务已停止
echo ========================================
echo.
pause
