#!/bin/bash

# 快速启动脚本（本地MySQL版本）

set -e

echo "========================================"
echo "  Website Batch System 启动脚本"
echo "  数据库：本地MySQL"
echo "========================================"
echo ""

# 检查Java
if ! command -v java &> /dev/null; then
    echo "❌ Java未安装"
    echo "请先安装Java 17: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | awk '{print $3}' | sed 's/"//g')
echo "✓ Java已安装: $JAVA_VERSION"
echo ""

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven未安装"
    echo "请先安装Maven: https://maven.apache.org/install.html"
    exit 1
fi

MAVEN_VERSION=$(mvn -version 2>&1 | head -1 | awk '{print $3}')
echo "✓ Maven已安装: $MAVEN_VERSION"
echo ""

# 检查MySQL
echo "🔍 检查MySQL连接..."
if ! command -v mysql &> /dev/null; then
    echo "⚠️  警告：MySQL未安装"
    echo "   你可以先安装MySQL："
    echo "   Ubuntu/Debian: sudo apt install -y mysql-server"
    echo "   CentOS/RHEL:   sudo yum install -y mysql-server"
    echo ""
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✓ MySQL客户端已安装"
fi

# 检查数据库连接
echo "🔍 检查数据库连接..."
mysql -u root -e "SELECT 1;" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "⚠️  MySQL连接失败，请检查配置"
    echo "   确保 application-cloud.yml 中的数据库密码正确"
    echo ""
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✓ MySQL连接成功"
fi

# 编译后端
echo ""
echo "📦 编译后端..."
cd backend
mvn clean package -DskipTests
echo "✓ 后端编译完成"
echo ""

# 启动后端
echo "🚀 启动后端服务..."
java -jar target/website-batch-system-1.0.0.jar > ../backend.log 2>&1 &
BACKEND_PID=$!
echo "✓ 后端服务已启动 (PID: $BACKEND_PID)"
echo "  日志文件: backend.log"
echo ""

# 等待后端启动
echo "⏳ 等待后端启动..."
sleep 10

# 检查后端是否启动成功
if ps -p $BACKEND_PID > /dev/null; then
    echo "✓ 后端服务运行正常"
else
    echo "❌ 后端启动失败"
    tail -50 backend.log
    exit 1
fi

# 测试API
echo ""
echo "🔍 测试API端点..."
sleep 5
curl -s http://localhost:8080/api/excel/companies
echo ""
echo "✓ API测试完成"
echo ""

echo "========================================"
echo "  后端启动成功！"
echo "========================================"
echo ""
echo "访问地址："
echo "  - 前端：http://localhost:5173"
echo "  - 后端API：http://localhost:8080/api"
echo "  - Excel导入页面：http://localhost:5173/excel"
echo ""
echo "停止后端："
echo "  kill $BACKEND_PID"
echo ""
echo "查看日志："
echo "  tail -f backend.log"
echo ""
echo "========================================"
echo ""
echo "现在启动前端..."
echo ""

# 启动前端
cd ../frontend
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
echo "✓ 前端服务已启动 (PID: $FRONTEND_PID)"
echo "  日志文件: frontend.log"
echo ""

# 等待前端启动
sleep 8

# 检查前端是否启动成功
if ps -p $FRONTEND_PID > /dev/null; then
    echo "✓ 前端服务运行正常"
else
    echo "❌ 前端启动失败"
    tail -50 frontend.log
    exit 1
fi

echo ""
echo "========================================"
echo "  🎉 系统启动成功！"
echo "========================================"
echo ""
echo "前端地址：http://localhost:5173"
echo "数据库：本地MySQL"
echo ""
echo "========================================"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo ""
