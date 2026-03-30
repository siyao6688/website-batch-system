#!/bin/bash
# 快速部署脚本 - 将本地修改部署到腾讯云服务器
# 使用方法：bash quick-deploy.sh <服务器IP>

set -e

SERVER_IP="$1"
SERVER_USER="root"  # 或改为您的用户名，如 "ubuntu"

if [ -z "$SERVER_IP" ]; then
    echo "错误：请提供服务器IP地址"
    echo "用法：bash quick-deploy.sh <服务器IP>"
    echo "示例：bash quick-deploy.sh 123.123.123.123"
    exit 1
fi

echo "🔧 开始快速部署到服务器：$SERVER_IP"
echo "========================================"

# 1. 构建后端
echo "1️⃣ 构建后端JAR..."
cd backend
mvn package -DskipTests -q
JAR_FILE="$(pwd)/target/website-batch-system-1.0.0.jar"
echo "  生成JAR文件: $JAR_FILE"
cd ..

# 2. 构建前端
echo "2️⃣ 构建前端..."
cd frontend
npm install
npm run build --silent
echo "  前端构建完成 (dist/)"
cd ..

# 3. 传输文件到服务器
echo "3️⃣ 传输文件到服务器..."
echo "  传输后端JAR..."
scp "$JAR_FILE" "$SERVER_USER@$SERVER_IP:/tmp/website-batch-system-new.jar"

echo "  传输前端文件..."
tar -czf /tmp/frontend-dist.tar.gz -C frontend/dist .
scp /tmp/frontend-dist.tar.gz "$SERVER_USER@$SERVER_IP:/tmp/"

# 4. 在服务器上部署
echo "4️⃣ 在服务器上部署..."
ssh "$SERVER_USER@$SERVER_IP" "
echo '停止服务...'
systemctl stop website-batch-system || true

echo '备份旧版本...'
timestamp=\$(date +%Y%m%d_%H%M%S)
if [ -f /opt/website-batch-system/backend/*.jar ]; then
    cp /opt/website-batch-system/backend/*.jar /opt/website-batch-system/backend/backup_\$timestamp.jar
fi

echo '部署新版本...'
# 部署后端
cp /tmp/website-batch-system-new.jar /opt/website-batch-system/backend/website-batch-system-1.0.0.jar

# 部署前端
rm -rf /var/www/html/*
tar -xzf /tmp/frontend-dist.tar.gz -C /var/www/html/
chown -R www-data:www-data /var/www/html 2>/dev/null || chown -R nginx:nginx /var/www/html 2>/dev/null || true

echo '清理临时文件...'
rm -f /tmp/website-batch-system-new.jar /tmp/frontend-dist.tar.gz
"

# 5. 重启服务
echo "5️⃣ 重启服务..."
ssh "$SERVER_USER@$SERVER_IP" "
echo '启动后端服务...'
systemctl start website-batch-system

echo '重启Nginx...'
systemctl restart nginx
"

# 6. 验证部署
echo "6️⃣ 验证部署..."
ssh "$SERVER_USER@$SERVER_IP" "
echo '=== 服务状态 ==='
systemctl status website-batch-system --no-pager | head -5

echo ''
echo '=== 文件检查 ==='
ls -lh /opt/website-batch-system/backend/*.jar
ls -lh /var/www/html/index.html 2>/dev/null || echo '前端文件不存在'

echo ''
echo '=== 端口检查 ==='
netstat -tulpn | grep -E ':80|:8080' || echo '端口未监听'
"

echo ""
echo "✅ 部署完成！"
echo "🌐 访问地址：http://$SERVER_IP"
echo "📊 查看日志：ssh $SERVER_USER@$SERVER_IP 'journalctl -u website-batch-system -f'"
echo ""
echo "如需回滚："
echo "  ssh $SERVER_USER@$SERVER_IP 'systemctl stop website-batch-system && cp /opt/website-batch-system/backend/backup_*.jar /opt/website-batch-system/backend/website-batch-system-1.0.0.jar && systemctl start website-batch-system'"