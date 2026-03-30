#!/bin/bash
# 部署前端代码到服务器的 /opt/website-batch-system/frontend/ 目录
# 使用方法：bash deploy-to-opt.sh <服务器IP>

set -e

SERVER_IP="$1"
SERVER_USER="ubuntu"

if [ -z "$SERVER_IP" ]; then
    echo "错误：请提供服务器IP地址"
    echo "用法：bash deploy-to-opt.sh <服务器IP>"
    echo "示例：bash deploy-to-opt.sh 124.223.45.101"
    exit 1
fi

echo "🔧 部署前端代码到服务器的 /opt/website-batch-system/ 目录"
echo "=========================================================="

# 1. 确保前端已构建
echo "1️⃣ 检查本地前端构建..."
if [ ! -f "frontend/dist/index.html" ]; then
    echo "  前端未构建，正在构建..."
    cd frontend
    npm run build
    cd ..
else
    echo "  前端已构建: $(stat -c %y frontend/dist/index.html)"
fi

# 2. 创建部署包
echo "2️⃣ 创建部署包..."
DEPLOY_DIR="deploy-frontend-$(date +%Y%m%d_%H%M%S)"
mkdir -p "/tmp/$DEPLOY_DIR"
cp -r frontend/dist/* "/tmp/$DEPLOY_DIR/"

# 检查文件
echo "  包含文件:"
ls -la "/tmp/$DEPLOY_DIR/"

# 3. 打包上传
echo "3️⃣ 打包并上传到服务器..."
cd /tmp
tar -czf "$DEPLOY_DIR.tar.gz" "$DEPLOY_DIR/"
scp "$DEPLOY_DIR.tar.gz" "$SERVER_USER@$SERVER_IP:/tmp/"
cd - > /dev/null

# 4. 服务器部署命令
echo ""
echo "4️⃣ 请在服务器上执行以下命令："
cat << 'EOF'
# SSH登录服务器
ssh ubuntu@124.223.45.101

# 备份当前前端文件
timestamp=$(date +%Y%m%d_%H%M%S)
if [ -d "/opt/website-batch-system/frontend" ]; then
    sudo cp -r /opt/website-batch-system/frontend /opt/website-batch-system/frontend_backup_$timestamp
    echo "备份前端文件到: /opt/website-batch-system/frontend_backup_$timestamp"
else
    echo "前端目录不存在，创建新目录"
    sudo mkdir -p /opt/website-batch-system/frontend
fi

# 清理前端目录
echo "清理前端目录..."
sudo rm -rf /opt/website-batch-system/frontend/*

# 解压新文件到前端目录
echo "部署新文件..."
sudo tar -xzf /tmp/deploy-frontend-*.tar.gz -C /opt/website-batch-system/frontend/

# 设置权限
sudo chown -R www-data:www-data /opt/website-batch-system/frontend 2>/dev/null || sudo chown -R nginx:nginx /opt/website-batch-system/frontend 2>/dev/null || true

# 清理临时文件
rm -f /tmp/deploy-frontend-*.tar.gz

# 验证部署
echo "验证部署..."
sudo ls -la /opt/website-batch-system/frontend/index.html
sudo ls -la /opt/website-batch-system/frontend/assets/ 2>/dev/null || echo "无assets目录"
sudo grep -c "login" /opt/website-batch-system/frontend/assets/*.js 2>/dev/null | head -1

# 重启Nginx
sudo systemctl restart nginx
echo "✅ 前端部署完成！"
EOF

# 5. 清理本地临时文件
rm -rf "/tmp/$DEPLOY_DIR" "/tmp/$DEPLOY_DIR.tar.gz"

echo ""
echo "📋 部署总结："
echo "   1. 本地文件已打包"
echo "   2. 已上传到服务器 /tmp/$DEPLOY_DIR.tar.gz"
echo "   3. 请在服务器上执行上述命令"
echo "   4. 然后访问：http://$SERVER_IP/login"
echo ""
echo "🔍 验证部署："
echo "   curl -I http://$SERVER_IP/"
echo "   grep -c 'login' /opt/website-batch-system/frontend/assets/*.js 2>/dev/null"