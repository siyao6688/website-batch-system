#!/bin/bash
# 快速部署脚本 - 将本地修改部署到腾讯云服务器
# 使用方法：bash quick-deploy.sh <服务器IP>

set -e

SERVER_IP="$1"
SERVER_USER="ubuntu"  # 或改为您的用户名，如 "ubuntu"

if [ -z "$SERVER_IP" ]; then
    echo "错误：请提供服务器IP地址"
    echo "用法：bash quick-deploy.sh <服务器IP>"
    echo "示例：bash quick-deploy.sh 123.123.123.123"
    exit 1
fi

echo "🔧 开始快速部署到服务器：$SERVER_IP"
echo "========================================"

# 确保Nginx配置目录存在
echo "0️⃣ 确保Nginx配置正确..."
ssh "$SERVER_USER@$SERVER_IP" "mkdir -p /etc/nginx/sites-available /etc/nginx/sites-enabled"

# 检查并创建/更新Nginx配置
ssh "$SERVER_USER@$SERVER_IP" "cat > /etc/nginx/sites-available/website-batch-system <<'NGINX_EOF'
server {
    listen 80;
    server_name _;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        root /var/www/html;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    access_log /var/log/nginx/website-batch-system-access.log;
    error_log /var/log/nginx/website-batch-system-error.log;
}
NGINX_EOF"

ssh "$SERVER_USER@$SERVER_IP" "ln -sf /etc/nginx/sites-available/website-batch-system /etc/nginx/sites-enabled/"

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

# 2.5 打包模板静态资源和预览网站目录
echo "2.5️⃣ 打包模板和预览资源..."
# 打包模板静态资源
tar -czf /tmp/templates-static.tar.gz -C backend/src/main/resources/templates static
# 打包预览网站目录（如果存在）
if [ -d "backend/preview-websites" ]; then
    tar -czf /tmp/preview-websites.tar.gz -C backend preview-websites
    echo "  预览网站目录已打包"
else
    echo "  预览网站目录不存在，跳过"
fi

# 3. 传输文件到服务器
echo "3️⃣ 传输文件到服务器..."
echo "  传输后端JAR..."
scp "$JAR_FILE" "$SERVER_USER@$SERVER_IP:/tmp/website-batch-system-new.jar"

echo "  传输前端文件..."
tar -czf /tmp/frontend-dist.tar.gz -C frontend/dist .
scp /tmp/frontend-dist.tar.gz "$SERVER_USER@$SERVER_IP:/tmp/"

echo "  传输模板静态资源..."
scp /tmp/templates-static.tar.gz "$SERVER_USER@$SERVER_IP:/tmp/"

echo "  传输预览网站目录..."
if [ -f /tmp/preview-websites.tar.gz ]; then
    scp /tmp/preview-websites.tar.gz "$SERVER_USER@$SERVER_IP:/tmp/"
fi

# 4. 在服务器上部署
echo "4️⃣ 在服务器上部署..."
ssh "$SERVER_USER@$SERVER_IP" "
echo '停止服务...'
#systemctl stop website-batch-system || true
pkill -f 'website-batch-system-1.0.0.jar'

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

# 部署模板静态资源到后端工作目录
mkdir -p /opt/website-batch-system/backend/templates
tar -xzf /tmp/templates-static.tar.gz -C /opt/website-batch-system/backend/templates/
echo '模板静态资源已部署'

# 部署预览网站目录
if [ -f /tmp/preview-websites.tar.gz ]; then
    mkdir -p /opt/website-batch-system/backend/preview-websites
    tar -xzf /tmp/preview-websites.tar.gz -C /opt/website-batch-system/backend/
    echo '预览网站目录已部署'
fi

echo '清理临时文件...'
rm -f /tmp/website-batch-system-new.jar /tmp/frontend-dist.tar.gz /tmp/templates-static.tar.gz /tmp/preview-websites.tar.gz
"

# 5. 重启服务
echo "5️⃣ 重启服务..."
ssh "$SERVER_USER@$SERVER_IP" "
echo '测试Nginx配置...'
nginx -t

echo '启动后端服务...'
# systemctl start website-batch-system
cd /opt/website-batch-system/backend
nohup java -jar website-batch-system-1.0.0.jar \
  --website.templates-path=/opt/website-batch-system/backend/templates \
  --website.preview-output-path=/opt/website-batch-system/backend/preview-websites \
  > app.log 2>&1 &
echo '后端服务已启动'

echo '重启Nginx...'
#systemctl reload nginx
sudo systemctl restart nginx
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