#!/bin/bash
# 完整部署脚本 - 部署前端和后端代码到服务器的 /opt/website-batch-system/ 根目录
# 使用方法：bash deploy-full.sh <服务器IP>

set -e

SERVER_IP="$1"
SERVER_USER="ubuntu"

if [ -z "$SERVER_IP" ]; then
    echo "错误：请提供服务器IP地址"
    echo "用法：bash deploy-full.sh <服务器IP>"
    echo "示例：bash deploy-full.sh 124.223.45.101"
    exit 1
fi

echo "🚀 完整部署：前端 + 后端到服务器：$SERVER_IP"
echo "========================================================"

# 1. 检查并构建前端
echo "1️⃣ 检查并构建前端..."
if [ ! -f "frontend/dist/index.html" ]; then
    echo "  前端未构建，正在构建..."
    cd frontend
    npm run build
    cd ..
else
    echo "  前端已构建: $(stat -c %y frontend/dist/index.html)"
fi

# 2. 检查并构建后端
echo "2️⃣ 检查并构建后端..."
cd backend
if [ ! -f "target/website-batch-system-1.0.0.jar" ]; then
    echo "  后端未构建，正在构建..."
    mvn clean package -DskipTests -q
else
    # 检查是否需要重新构建（基于pom.xml和src/修改时间）
    if [ "pom.xml" -nt "target/website-batch-system-1.0.0.jar" ] || \
       find src/ -type f -newer "target/website-batch-system-1.0.0.jar" 2>/dev/null | grep -q .; then
        echo "  检测到代码变更，重新构建..."
        mvn clean package -DskipTests -q
    else
        echo "  后端已构建: $(stat -c %y target/website-batch-system-1.0.0.jar)"
    fi
fi
JAR_FILE="$(pwd)/target/website-batch-system-1.0.0.jar"
echo "  JAR文件: $JAR_FILE ($(stat -c %s "$JAR_FILE" | awk '{printf "%.2fMB", $1/1024/1024}'))"
cd ..

# 3. 创建部署包
echo "3️⃣ 创建部署包..."
DEPLOY_TIME=$(date +%Y%m%d_%H%M%S)
FRONTEND_DIR="deploy-frontend-$DEPLOY_TIME"
BACKEND_DIR="deploy-backend-$DEPLOY_TIME"

# 前端包
mkdir -p "/tmp/$FRONTEND_DIR"
cp -r frontend/dist/* "/tmp/$FRONTEND_DIR/"
echo "  前端包: /tmp/$FRONTEND_DIR.tar.gz"

# 后端包
mkdir -p "/tmp/$BACKEND_DIR"
cp "$JAR_FILE" "/tmp/$BACKEND_DIR/"
echo "  后端包: /tmp/$BACKEND_DIR.tar.gz"

# 4. 打包上传
echo "4️⃣ 打包并上传到服务器..."
cd /tmp
tar -czf "$FRONTEND_DIR.tar.gz" "$FRONTEND_DIR/"
tar -czf "$BACKEND_DIR.tar.gz" "$BACKEND_DIR/"
scp "$FRONTEND_DIR.tar.gz" "$BACKEND_DIR.tar.gz" "$SERVER_USER@$SERVER_IP:/tmp/"
cd - > /dev/null

# 5. 服务器部署命令
echo ""
echo "5️⃣ 请在服务器上执行以下命令："
cat << 'EOF'
# SSH登录服务器
ssh ubuntu@124.223.45.101

# 备份时间戳
timestamp=$(date +%Y%m%d_%H%M%S)
echo "备份时间戳: $timestamp"

# 1. 备份前端文件
echo "备份前端文件..."
if [ -f "/opt/website-batch-system/index.html" ] || [ -d "/opt/website-batch-system/assets" ]; then
    sudo mkdir -p /opt/website-batch-system/root_backup_$timestamp
    sudo cp -r /opt/website-batch-system/index.html /opt/website-batch-system/assets /opt/website-batch-system/root_backup_$timestamp/ 2>/dev/null || true
    echo "✅ 前端备份到: /opt/website-batch-system/root_backup_$timestamp/"
else
    echo "前端文件不存在，无需备份"
fi

# 2. 备份后端
if [ -d "/opt/website-batch-system/backend" ]; then
    sudo cp -r /opt/website-batch-system/backend /opt/website-batch-system/backend_backup_$timestamp
    echo "✅ 后端备份到: /opt/website-batch-system/backend_backup_$timestamp"
else
    echo "后端目录不存在，创建新目录"
    sudo mkdir -p /opt/website-batch-system/backend
fi

# 3. 停止服务
echo "停止服务..."
sudo systemctl stop website-batch-system || echo "服务未运行或不存在"

# 4. 清理旧文件
echo "清理旧文件..."
sudo rm -f /opt/website-batch-system/index.html
sudo rm -rf /opt/website-batch-system/assets 2>/dev/null || true
sudo rm -rf /opt/website-batch-system/backend/*

# 5. 部署前端
echo "部署前端..."
sudo tar -xzf /tmp/deploy-frontend-*.tar.gz -C /opt/website-batch-system/

# 6. 部署后端
echo "部署后端..."
sudo tar -xzf /tmp/deploy-backend-*.tar.gz -C /opt/website-batch-system/backend/

# 7. 设置权限
echo "设置权限..."
sudo chown -R www-data:www-data /opt/website-batch-system 2>/dev/null || sudo chown -R nginx:nginx /opt/website-batch-system 2>/dev/null || true
sudo chown -R root:root /opt/website-batch-system/backend 2>/dev/null || true
sudo chmod 755 /opt/website-batch-system/backend/*.jar 2>/dev/null || true

# 8. 清理临时文件
echo "清理临时文件..."
rm -f /tmp/deploy-frontend-*.tar.gz /tmp/deploy-backend-*.tar.gz

# 9. 重启服务
echo "重启服务..."
sudo systemctl daemon-reload
sudo systemctl start website-batch-system || echo "启动失败，可能需要检查配置"
sudo systemctl restart nginx

# 10. 验证部署
echo "验证部署..."
echo "=== 前端 ==="
sudo ls -la /opt/website-batch-system/index.html
sudo grep -c "login" /opt/website-batch-system/assets/*.js 2>/dev/null | head -1

echo "=== 后端 ==="
sudo ls -lh /opt/website-batch-system/backend/*.jar
sudo systemctl status website-batch-system --no-pager | head -5

echo "=== 服务状态 ==="
sudo netstat -tulpn | grep -E ':80|:8080' || echo "端口未监听"

echo "✅ 完整部署完成！"
EOF

# 6. 清理本地临时文件
rm -rf "/tmp/$FRONTEND_DIR" "/tmp/$FRONTEND_DIR.tar.gz" "/tmp/$BACKEND_DIR" "/tmp/$BACKEND_DIR.tar.gz"

echo ""
echo "📋 部署总结："
echo "   1. 前端构建: frontend/dist/"
echo "   2. 后端构建: backend/target/website-batch-system-1.0.0.jar"
echo "   3. 已上传到服务器: /tmp/deploy-frontend-*.tar.gz, /tmp/deploy-backend-*.tar.gz"
echo "   4. 请在服务器上执行上述命令"
echo "   5. 然后访问：http://$SERVER_IP:8088/login"
echo ""
echo "🔍 验证部署："
echo "   前端: curl -I http://$SERVER_IP:8088/"
echo "   后端API: curl http://$SERVER_IP:8088/api/health"
echo "   登录测试: curl -X POST http://$SERVER_IP:8088/api/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"xl\",\"password\":\"shuaixiaohuo\"}'"