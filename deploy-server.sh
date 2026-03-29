#!/bin/bash

# 腾讯云服务器快速部署脚本
# 使用方法：bash deploy-server.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印函数
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    print_error "请使用root用户运行此脚本"
    exit 1
fi

# 更新系统
print_info "更新系统软件包..."
apt update
apt upgrade -y

# 安装必要工具
print_info "安装必要工具..."
apt install -y curl wget git vim unzip

# 安装Java 17
print_info "安装Java 17..."
if ! command -v java &> /dev/null; then
    print_info "下载SDKMAN..."
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"

    print_info "安装Java 17..."
    sdk install java 17.0.8-tem

    echo 'export JAVA_HOME=$(/usr/lib/jvm/java-17-openjdk-amd64)' >> ~/.bashrc
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
else
    print_success "Java已安装: $(java -version 2>&1 | head -1)"
fi

# 安装MySQL客户端（如果未安装）
print_info "安装MySQL客户端..."
if ! command -v mysql &> /dev/null; then
    apt install -y default-mysql-client
fi

# 创建部署目录
print_info "创建部署目录..."
mkdir -p /opt/website-batch-system
mkdir -p /var/www/html

# 创建Nginx配置目录
mkdir -p /etc/nginx/sites-available
mkdir -p /etc/nginx/sites-enabled

# 创建日志目录
mkdir -p /var/log/website-batch-system
mkdir -p /var/log/nginx

# 配置Nginx
print_info "配置Nginx..."

cat > /etc/nginx/sites-available/website-batch-system <<'EOF'
server {
    listen 80;
    server_name _;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        root /var/www/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    access_log /var/log/nginx/website-batch-system-access.log;
    error_log /var/log/nginx/website-batch-system-error.log;
}
EOF

# 启用站点配置
ln -sf /etc/nginx/sites-available/website-batch-system /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx

print_success "Nginx配置完成"

# 创建systemd服务
print_info "创建后端服务..."

cat > /etc/systemd/system/website-batch-system.service <<'EOF'
[Unit]
Description=Website Batch System Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/website-batch-system/backend
ExecStart=/usr/lib/jvm/java-17-openjdk-amd64/bin/java -jar /opt/website-batch-system/backend/target/website-batch-system-1.0.0.jar --spring.profiles.active=cloud
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable website-batch-system

print_success "后端服务配置完成"

# 配置防火墙
print_info "配置防火墙..."
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

print_success "防火墙配置完成"

# 提示用户后续步骤
echo ""
print_info "================== 部署完成 =================="
echo ""
print_info "接下来需要完成以下步骤："
echo ""
echo "1. 上传项目代码到服务器："
echo "   scp -r backend.tar.gz root@<服务器IP>:/opt/"
echo ""
echo "2. 解压代码并配置数据库："
echo "   cd /opt && tar -xzf backend.tar.gz"
echo "   cd website-batch-system/backend/src/main/resources"
echo "   nano application-cloud.yml"
echo "   修改数据库连接配置"
echo ""
echo "3. 编译和运行后端："
echo "   cd /opt/website-batch-system/backend"
echo "   mvn clean package -DskipTests"
echo "   systemctl start website-batch-system"
echo ""
echo "4. 上传前端代码到服务器："
echo "   cd frontend"
echo "   tar -czf dist.tar.gz dist/"
echo "   scp dist.tar.gz root@<服务器IP>:/opt/"
echo ""
echo "5. 解压前端代码："
echo "   cd /opt && tar -xzf dist.tar.gz"
echo ""
echo "6. 访问系统："
echo "   http://<服务器IP>"
echo ""
print_info "=============================================="
echo ""
