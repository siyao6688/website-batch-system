#!/bin/bash
# 快速部署脚本（Ubuntu用户版）- 将本地修改部署到腾讯云服务器
# 使用方法：bash quick-deploy-ubuntu.sh <服务器IP>

set -e

SERVER_IP="$1"
SERVER_USER="ubuntu"  # 腾讯云默认用户名

if [ -z "$SERVER_IP" ]; then
    echo "错误：请提供服务器IP地址"
    echo "用法：bash quick-deploy-ubuntu.sh <服务器IP>"
    echo "示例：bash quick-deploy-ubuntu.sh 123.123.123.123"
    exit 1
fi

echo "🔧 开始快速部署到服务器：$SERVER_IP（用户：$SERVER_USER）"
echo "=================================================="

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
sudo systemctl stop website-batch-system || true

echo '备份旧版本...'
timestamp=\$(date +%Y%m%d_%H%M%S)
if [ -f /opt/website-batch-system/backend/*.jar ]; then
    sudo cp /opt/website-batch-system/backend/*.jar /opt/website-batch-system/backend/backup_\$timestamp.jar
fi

echo '部署新版本...'
# 部署后端
sudo cp /tmp/website-batch-system-new.jar /opt/website-batch-system/backend/website-batch-system-1.0.0.jar

# 部署前端
sudo rm -rf /var/www/html/*
sudo tar -xzf /tmp/frontend-dist.tar.gz -C /var/www/html/
sudo chown -R www-data:www-data /var/www/html 2>/dev/null || sudo chown -R nginx:nginx /var/www/html 2>/dev/null || true

echo '清理临时文件...'
rm -f /tmp/website-batch-system-new.jar /tmp/frontend-dist.tar.gz
"

# 5. 配置和重启服务
echo "5️⃣ 配置和重启服务..."
ssh "$SERVER_USER@$SERVER_IP" '
# 检查并创建systemd服务（如果不存在）
if [ ! -f /etc/systemd/system/website-batch-system.service ]; then
    echo "创建systemd服务..."
    sudo tee /etc/systemd/system/website-batch-system.service > /dev/null << EOF
[Unit]
Description=Website Batch System Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/website-batch-system/backend
ExecStart=/usr/bin/java -jar /opt/website-batch-system/backend/website-batch-system-1.0.0.jar --spring.profiles.active=cloud
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable website-batch-system
fi

echo "启动后端服务..."
sudo systemctl start website-batch-system

echo "重启Nginx..."
sudo systemctl restart nginx
'

# 6. 验证部署
echo "6️⃣ 验证部署..."
ssh "$SERVER_USER@$SERVER_IP" "
echo '=== 服务状态 ==='
sudo systemctl status website-batch-system --no-pager | head -5

echo ''
echo '=== 文件检查 ==='
sudo ls -lh /opt/website-batch-system/backend/*.jar
sudo ls -lh /var/www/html/index.html 2>/dev/null || echo '前端文件不存在'

echo ''
echo '=== 端口检查 ==='
sudo netstat -tulpn | grep -E ':80|:8080' || echo '端口未监听'
"

echo ""
echo "✅ 部署完成！"
echo "🌐 访问地址：http://$SERVER_IP"
echo "📊 查看日志：ssh $SERVER_USER@$SERVER_IP 'sudo journalctl -u website-batch-system -f'"
echo ""
echo "如需回滚："
echo "  ssh $SERVER_USER@$SERVER_IP 'sudo systemctl stop website-batch-system && sudo cp /opt/website-batch-system/backend/backup_*.jar /opt/website-batch-system/backend/website-batch-system-1.0.0.jar && sudo systemctl start website-batch-system'"