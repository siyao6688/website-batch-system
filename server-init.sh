#!/bin/bash

# 服务器初始化脚本 - 为无Docker部署准备环境
# 在服务器上执行：bash server-init.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${YELLOW}ℹ $1${NC}"; }

# 检查root权限
if [ "$EUID" -ne 0 ]; then
    print_error "请使用root用户运行此脚本"
    exit 1
fi

# 检测操作系统
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$NAME
        VER=$VERSION_ID
    else
        print_error "无法检测操作系统"
        exit 1
    fi
}

# 安装Java
install_java() {
    print_info "安装Java 17..."

    case $ID in
        ubuntu|debian)
            apt update
            apt install -y openjdk-17-jdk
            ;;
        centos|rhel|fedora)
            yum install -y java-17-openjdk-devel
            ;;
        *)
            print_error "不支持的操作系统: $ID"
            exit 1
            ;;
    esac

    print_success "Java安装完成: $(java -version 2>&1 | head -1)"
}

# 安装Nginx
install_nginx() {
    print_info "安装Nginx..."

    case $ID in
        ubuntu|debian)
            apt install -y nginx
            systemctl enable nginx
            systemctl start nginx
            ;;
        centos|rhel|fedora)
            yum install -y epel-release
            yum install -y nginx
            systemctl enable nginx
            systemctl start nginx
            ;;
    esac

    print_success "Nginx安装完成"
}

# 安装MySQL客户端（可选）
install_mysql_client() {
    print_info "安装MySQL客户端..."

    case $ID in
        ubuntu|debian)
            apt install -y default-mysql-client
            ;;
        centos|rhel|fedora)
            yum install -y mysql
            ;;
    esac

    print_success "MySQL客户端安装完成"
}

# 创建目录结构
create_directories() {
    print_info "创建目录结构..."

    mkdir -p /opt/website-batch-system/backend
    mkdir -p /var/www/html
    mkdir -p /var/log/website-batch-system

    # 创建日志目录
    mkdir -p /var/log/website-batch-system

    print_success "目录创建完成"
}

# 配置Nginx
configure_nginx() {
    print_info "配置Nginx..."

    # 备份原配置
    if [ -f /etc/nginx/sites-available/default ]; then
        cp /etc/nginx/sites-available/default /etc/nginx/sites-available/default.backup
    fi

    # 创建网站配置
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

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 前端静态文件
    location / {
        root /var/www/html;
        index index.html;
        try_files $uri $uri/ /index.html;

        # 缓存设置
        expires 1h;
        add_header Cache-Control "public, max-age=3600";
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        root /var/www/html;
        expires 1y;
        add_header Cache-Control "public, max-age=31536000";
    }

    access_log /var/log/nginx/website-batch-system-access.log;
    error_log /var/log/nginx/website-batch-system-error.log;
}
EOF

    # 启用配置
    if [ -d /etc/nginx/sites-enabled ]; then
        ln -sf /etc/nginx/sites-available/website-batch-system /etc/nginx/sites-enabled/
    else
        # CentOS等系统
        cp /etc/nginx/sites-available/website-batch-system /etc/nginx/conf.d/website-batch-system.conf
    fi

    # 测试并重载配置
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
Wants=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/website-batch-system/backend
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar website-batch-system-1.0.0.jar --spring.profiles.active=cloud
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=website-batch-system

# 环境变量
Environment="SPRING_PROFILES_ACTIVE=cloud"
Environment="TZ=Asia/Shanghai"

# 安全设置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ReadWritePaths=/opt/website-batch-system/backend /var/log/website-batch-system

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

    case $ID in
        ubuntu|debian)
            if command -v ufw &> /dev/null; then
                ufw allow 22/tcp
                ufw allow 80/tcp
                ufw allow 443/tcp
                ufw --force enable
                print_success "UFW防火墙配置完成"
            fi
            ;;
        centos|rhel|fedora)
            if command -v firewall-cmd &> /dev/null; then
                firewall-cmd --permanent --add-service=http
                firewall-cmd --permanent --add-service=https
                firewall-cmd --reload
                print_success "FirewallD配置完成"
            fi
            ;;
    esac
}

# 设置日志轮转
setup_logrotate() {
    print_info "设置日志轮转..."

    cat > /etc/logrotate.d/website-batch-system <<'EOF'
/var/log/website-batch-system/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 root root
    sharedscripts
    postrotate
        systemctl reload website-batch-system > /dev/null 2>&1 || true
    endscript
}
EOF

    print_success "日志轮转配置完成"
}

# 显示系统信息
show_system_info() {
    echo ""
    print_info "=== 系统初始化完成 ==="
    echo ""
    echo "操作系统: $OS $VER"
    echo "Java版本: $(java -version 2>&1 | head -1)"
    echo "Nginx版本: $(nginx -v 2>&1)"
    echo ""
    echo "目录结构:"
    echo "  后端目录: /opt/website-batch-system/backend"
    echo "  前端目录: /var/www/html"
    echo "  日志目录: /var/log/website-batch-system"
    echo ""
    echo "服务配置:"
    echo "  Nginx配置: /etc/nginx/sites-available/website-batch-system"
    echo "  后端服务: /etc/systemd/system/website-batch-system.service"
    echo ""
    echo "后续步骤:"
    echo "  1. 上传后端JAR到 /opt/website-batch-system/backend/"
    echo "  2. 上传前端文件到 /var/www/html/"
    echo "  3. 启动服务: systemctl start website-batch-system"
    echo "  4. 访问地址: http://$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')"
    echo ""
    print_info "================================="
    echo ""
}

# 主函数
main() {
    print_info "开始服务器初始化..."

    # 检测操作系统
    detect_os
    echo "检测到操作系统: $OS $VER"

    # 更新系统
    print_info "更新系统包..."
    case $ID in
        ubuntu|debian)
            apt update && apt upgrade -y
            ;;
        centos|rhel|fedora)
            yum update -y
            ;;
    esac

    # 安装必要工具
    print_info "安装必要工具..."
    case $ID in
        ubuntu|debian)
            apt install -y curl wget git unzip net-tools
            ;;
        centos|rhel|fedora)
            yum install -y curl wget git unzip net-tools
            ;;
    esac

    # 执行各步骤
    install_java
    install_nginx
    install_mysql_client
    create_directories
    configure_nginx
    configure_backend_service
    configure_firewall
    setup_logrotate

    print_success "服务器初始化完成！"
    show_system_info
}

# 执行主函数
main "$@"