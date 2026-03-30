#!/bin/bash
# 网站批量生成系统 - 一键部署脚本
# 使用方法：./deploy.sh

set -e

# 配置
SSH_KEY="C:/Users/1/.ssh/id_rsa"
SERVER_USER="root"
SERVER_HOST="124.223.45.101"
SERVER_PATH="/opt/website-batch-system/backend"
JAR_FILE="target/website-batch-system-1.0.0.jar"

echo "=========================================="
echo "网站批量生成系统 - 部署脚本"
echo "=========================================="

# 1. 构建
echo "[1/4] 正在构建项目..."
mvn clean package -DskipTests -q
echo "✓ 构建完成"

# 2. 上传
echo "[2/4] 正在上传 JAR 文件到服务器..."
scp -i "$SSH_KEY" "$JAR_FILE" "${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/target/"
echo "✓ 上传完成"

# 3. 重启服务
echo "[3/4] 正在重启服务..."
ssh -i "$SSH_KEY" "${SERVER_USER}@${SERVER_HOST}" "
  pkill -f 'website-batch-system-1.0.0.jar' || true
  sleep 2
  cd ${SERVER_PATH}
  nohup java -jar target/website-batch-system-1.0.0.jar > app.log 2>&1 &
  sleep 10
"
echo "✓ 服务已重启"

# 4. 验证
echo "[4/4] 验证服务状态..."
ssh -i "$SSH_KEY" "${SERVER_USER}@${SERVER_HOST}" "
  ps aux | grep 'website-batch-system' | grep -v grep
  echo '---'
  curl -s http://localhost:8080/api/companies/1 | head -c 200
"

echo ""
echo "=========================================="
echo "✓ 部署完成！"
echo "=========================================="
