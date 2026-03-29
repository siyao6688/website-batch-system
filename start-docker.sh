#!/bin/bash

# Docker Compose 启动脚本

set -e

echo "========================================"
echo "  Website Batch System - Docker 启动"
echo "========================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装"
    echo "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

echo "✓ Docker 已安装"
echo "  版本: $(docker --version)"
echo ""

# 检查 Docker Compose
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
    echo "✓ Docker Compose 已安装"
    echo "  版本: $(docker-compose --version)"
elif docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
    echo "✓ Docker Compose 已安装"
    echo "  版本: $(docker compose version)"
else
    echo "❌ Docker Compose 未安装"
    echo "请安装 Docker Compose 或使用 Docker Desktop"
    exit 1
fi

echo ""

# 停止旧服务
echo "🛑 停止旧服务..."
$COMPOSE_CMD down 2>/dev/null || true
echo "✓ 旧服务已停止"
echo ""

# 检查端口占用
echo "🔍 检查端口占用..."
if lsof -i :8080 > /dev/null 2>&1; then
    echo "⚠️  端口 8080 被占用"
    read -p "是否停止占用端口的进程？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        lsof -ti :8080 | xargs kill -9 2>/dev/null || true
        echo "✓ 端口已释放"
    fi
fi

echo ""
echo "🚀 启动服务..."

# 构建并启动
$COMPOSE_CMD up -d --build

echo ""
echo "⏳ 等待服务启动..."
sleep 5

# 检查服务状态
echo ""
echo "📊 服务状态："
$COMPOSE_CMD ps

echo ""
echo "⏳ 等待 MySQL 就绪..."
# 等待 MySQL 健康检查
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if $COMPOSE_CMD ps mysql | grep -q "healthy"; then
        echo "✓ MySQL 已就绪"
        break
    fi
    echo -n "."
    sleep 2
    WAITED=$((WAITED + 2))
done

if [ $WAITED -ge $MAX_WAIT ]; then
    echo ""
    echo "⚠️  MySQL 启动超时"
    echo "查看 MySQL 日志："
    $COMPOSE_CMD logs mysql
fi

echo ""
echo "✓ 服务启动完成！"
echo ""
echo "========================================"
echo "  访问地址："
echo "========================================"
echo "  前端页面：http://localhost:8080"
echo "  Excel导入：http://localhost:8080/excel"
echo "  后端API：  http://localhost:8080/api/excel/companies"
echo ""
echo "========================================"
echo "  常用命令："
echo "========================================"
echo "  查看日志：$COMPOSE_CMD logs -f"
echo "  查看MySQL：$COMPOSE_CMD logs -f mysql"
echo "  查看后端：$COMPOSE_CMD logs -f backend"
echo "  停止服务：$COMPOSE_CMD down"
echo "  重启服务：$COMPOSE_CMD restart"
echo ""
echo "========================================"
echo ""
