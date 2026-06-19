@echo off
chcp 65001 >nul
title Z-Writer 服务状态检查

echo ========================================
echo    Z-Writer 服务状态检查
echo ========================================
echo.

:: 检查 Docker
echo [1/5] Docker 状态:
docker info >nul 2>&1
if errorlevel 1 (
    echo   ❌ Docker 未运行
) else (
    echo   ✅ Docker 运行正常
)
echo.

:: 检查 Docker 容器
echo [2/5] 容器状态:
docker ps --filter "name=zwriter" --format "table {{.Names}}\t{{.Status}}"
echo.

:: 检查后端端口
echo [3/5] 后端服务 (8080):
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo   ❌ 后端服务未运行
) else (
    echo   ✅ 后端服务运行中
)
echo.

:: 检查前端端口
echo [4/5] 前端服务 (5173):
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo   ❌ 前端服务未运行
) else (
    echo   ✅ 前端服务运行中
)
echo.

:: 显示访问地址
echo [5/5] 访问地址:
echo   - 前端界面: http://localhost:5173
echo   - 后端 API: http://localhost:8080
echo.

pause
