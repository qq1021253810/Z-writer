@echo off
chcp 65001 >nul
title Z-Writer 开发环境启动器

echo ========================================
echo    Z-Writer 开发环境启动器
echo ========================================
echo.

:: 检查 Docker 是否运行
echo [1/4] 检查 Docker 状态...
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)
echo ✅ Docker 运行正常
echo.

:: 启动 Docker 容器
echo [2/4] 启动基础设施服务 (PostgreSQL, Redis, ChromaDB)...
docker compose up -d
if errorlevel 1 (
    echo ❌ Docker 容器启动失败
    pause
    exit /b 1
)
echo ✅ 基础设施服务已启动
echo.

:: 等待服务就绪
echo 等待服务就绪...
timeout /t 5 /nobreak >nul

:: 启动后端服务
echo [3/4] 启动后端服务 (Spring Boot)...
cd backend
start "Z-Writer Backend" cmd /k "mvn spring-boot:run"
cd ..
echo ✅ 后端服务启动中 (端口 8080)
echo.

:: 启动前端服务
echo [4/4] 启动前端服务 (Vite)...
cd frontend
start "Z-Writer Frontend" cmd /k "npm run dev"
cd ..
echo ✅ 前端服务启动中 (端口 5173)
echo.

echo ========================================
echo    启动完成！
echo ========================================
echo.
echo 服务地址:
echo   - 前端界面: http://localhost:5173
echo   - 后端 API: http://localhost:8080
echo   - PostgreSQL: localhost:5432
echo   - Redis: localhost:6379
echo   - ChromaDB: localhost:8000
echo.
echo 按任意键关闭此窗口（服务将继续运行）
pause >nul
