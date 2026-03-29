@echo off
chcp 65001 >nul
echo ========================================
echo   Website Batch System - Docker 启动
echo ========================================
echo.

:: 检查 Docker 是否安装
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker 未安装
    echo 请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

echo ✓ Docker 已安装
docker --version
echo.

:: 检查 Docker Compose
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    docker compose version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ❌ Docker Compose 未安装
        echo 请安装 Docker Compose 或使用 Docker Desktop
        pause
        exit /b 1
    ) else (
        set COMPOSE_CMD=docker compose
        echo ✓ Docker Compose 已安装
        docker compose version
    )
) else (
    set COMPOSE_CMD=docker-compose
    echo ✓ Docker Compose 已安装
    docker-compose --version
)

echo.
echo.

:: 停止旧服务
echo [1/4] 停止旧服务...
%COMPOSE_CMD% down 2>nul
echo ✓ 旧服务已停止
echo.

:: 检查端口占用
echo [2/4] 检查端口占用...
netstat -ano | findstr :8080 >nul
if %errorlevel% equ 0 (
    echo ⚠️  端口 8080 被占用
    set /p kill_process=是否停止占用端口的进程？(y/n):
    if /i "%kill_process%"=="y" (
        for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
            taskkill /F /PID %%a 2>nul
        )
        echo ✓ 端口已释放
    )
)
echo.

:: 构建并启动
echo [3/4] 构建并启动服务...
%COMPOSE_CMD% up -d --build
echo ✓ 服务正在启动...
echo.

:: 等待服务启动
echo [4/4] 等待服务启动...
timeout /t 5 /nobreak >nul

echo.
echo 📊 服务状态：
echo.
%COMPOSE_CMD% ps

echo.
echo ⏳ 等待 MySQL 就绪...
timeout /t 3 /nobreak >nul
%COMPOSE_CMD% ps mysql | findstr healthy >nul
if %errorlevel% equ 0 (
    echo ✓ MySQL 已就绪
) else (
    echo ⚠️  MySQL 可能还在启动中...
    echo 查看日志：%COMPOSE_CMD% logs mysql
)

echo.
echo ========================================
echo   ✓ 服务启动完成！
echo ========================================
echo.
echo 访问地址：
echo   - 前端页面：http://localhost:8080
echo   - Excel导入：http://localhost:8080/excel
echo   - 后端API：  http://localhost:8080/api/excel/companies
echo.
echo ========================================
echo   常用命令：
echo ========================================
echo   查看日志：%COMPOSE_CMD% logs -f
echo   查看MySQL：%COMPOSE_CMD% logs -f mysql
echo   查看后端：%COMPOSE_CMD% logs -f backend
echo   停止服务：%COMPOSE_CMD% down
echo   重启服务：%COMPOSE_CMD% restart
echo.
echo ========================================
echo.

pause
