#!/bin/bash

# 网站批量处理系统 - 简化部署脚本（无Docker版本）
# 使用方法：bash simple-deploy.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

# 安装Java 17
install_java() {
    if ! command -v java &> /dev/null; then
        print_info "安装Java 17..."
        apt update
        apt install -y openjdk-17-jdk
        print_success "Java 17安装完成"
    else
        print_success "Java已安装: $(java -version 2>&1 | head -1)"
    fi
}

# 安装Nginx
install_nginx() {
    if ! command -v nginx &> /dev/null; then
        print_info "安装Nginx..."
        apt install -y nginx
        systemctl enable nginx
        systemctl start nginx
        print_success "Nginx安装完成"
    else
        print_success "Nginx已安装"
    fi
}

# 安装MySQL客户端（可选）
install_mysql_client() {
    if ! command -v mysql &> /dev/null; then
        print_info "安装MySQL客户端..."
        apt install -y default-mysql-client
        print_success "MySQL客户端安装完成"
    fi
}

# 创建部署目录
create_directories() {
    print_info "创建部署目录..."

    # 后端目录
    mkdir -p /opt/website-batch-system/backend
    mkdir -p /var/log/website-batch-system

    # 前端目录
    mkdir -p /var/www/html

    print_success "目录创建完成"
}

# 配置Nginx
configure_nginx() {
    print_info "配置Nginx..."

    cat > /etc/nginx/sites-available/website-batch-system <<'EOF'
server {
    listen 80;
    server_name _;

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 前端静态文件
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

    # 测试并重载Nginx
    nginx -t && systemctl reload nginx

    print_success "Nginx配置完成"
}

# 配置后端服务
configure_backend_service() {
    print_info "配置后端服务..."

    cat > /etc/systemd/system/website-batch-system.service <<'EOF'
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

    systemctl daemon-reload
    systemctl enable website-batch-system

    print_success "后端服务配置完成"
}

# 配置防火墙
configure_firewall() {
    print_info "配置防火墙..."

    # 检查ufw是否安装
    if command -v ufw &> /dev/null; then
        ufw allow 80/tcp
        ufw allow 22/tcp
        ufw --force enable
        print_success "防火墙配置完成"
    else
        print_info "UFW未安装，跳过防火墙配置"
    fi
}

# 部署应用文件
deploy_application() {
    print_info "部署应用文件..."

    # 这里假设JAR和前端文件已经在当前目录
    # 实际使用中，应该从构建产物复制

    if [ -f "backend/target/*.jar" ]; then
        cp backend/target/*.jar /opt/website-batch-system/backend/
        print_success "后端JAR文件已复制"
    fi

    if [ -d "frontend/dist" ]; then
        cp -r frontend/dist/* /var/www/html/
        print_success "前端文件已复制"
    fi

    print_success "应用文件部署完成"
}

# 启动服务
start_services() {
    print_info "启动服务..."

    systemctl restart nginx
    systemctl restart website-batch-system

    print_success "服务启动完成"
}

# 显示部署信息
show_deployment_info() {
    echo ""
    print_info "================== 部署完成 =================="
    echo ""
    print_info "服务状态："
    systemctl status website-batch-system --no-pager
    echo ""
    print_info "访问地址：http://$(curl -s ifconfig.me) 或 http://服务器IP"
    echo ""
    print_info "日志查看："
    echo "  后端日志：journalctl -u website-batch-system -f"
    echo "  Nginx日志：tail -f /var/log/nginx/website-batch-system-access.log"
    echo ""
    print_info "配置文件位置："
    echo "  后端JAR：/opt/website-batch-system/backend/"
    echo "  前端文件：/var/www/html/"
    echo "  Nginx配置：/etc/nginx/sites-available/website-batch-system"
    echo ""
    print_info "=============================================="
    echo ""
}

# 主函数
main() {
    print_info "开始部署网站批量处理系统（无Docker版本）"

    # 更新系统
    print_info "更新系统..."
    apt update && apt upgrade -y

    # 安装必要工具
    apt install -y curl wget git

    # 执行各步骤
    install_java
    install_nginx
    install_mysql_client
    create_directories
    configure_nginx
    configure_backend_service
    configure_firewall
    deploy_application
    start_services
    show_deployment_info

    print_success "部署完成！"
}

# 执行主函数
main "$@"