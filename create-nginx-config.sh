#!/bin/bash
# 手动创建Nginx配置脚本
# 用法：bash create-nginx-config.sh <域名> <网站目录>

DOMAIN="$1"
WEBSITE_DIR="$2"

if [ -z "$DOMAIN" ] || [ -z "$WEBSITE_DIR" ]; then
    echo "用法：bash create-nginx-config.sh <域名> <网站目录>"
    echo "示例：bash create-nginx-config.sh example.com /var/www/html/example.com"
    exit 1
fi

CONFIG_FILE="/etc/nginx/sites-available/${DOMAIN}.conf"
ENABLED_CONFIG="/etc/nginx/sites-enabled/${DOMAIN}.conf"

echo "创建Nginx配置：$DOMAIN"
echo "网站目录：$WEBSITE_DIR"

# 创建配置内容
cat > "$CONFIG_FILE" << EOF
server {
    listen 80;
    server_name ${DOMAIN} www.${DOMAIN};

    root ${WEBSITE_DIR};
    index index.html index.htm;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)\$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    access_log /var/log/nginx/${DOMAIN}-access.log;
    error_log /var/log/nginx/${DOMAIN}-error.log;
}
EOF

echo "✅ Nginx配置已创建：$CONFIG_FILE"

# 启用配置
if [ ! -L "$ENABLED_CONFIG" ]; then
    ln -sf "$CONFIG_FILE" "$ENABLED_CONFIG"
    echo "✅ 配置已启用：$ENABLED_CONFIG"
else
    echo "⚠️  配置已存在，跳过启用"
fi

# 测试配置
echo "测试Nginx配置..."
nginx -t

if [ $? -eq 0 ]; then
    echo "✅ Nginx配置语法正确"
    echo "执行以下命令重载Nginx："
    echo "  sudo systemctl reload nginx"
else
    echo "❌ Nginx配置语法错误，请检查配置文件"
fi

echo ""
echo "🌐 网站可通过以下地址访问："
echo "  http://${DOMAIN}"
echo "  http://www.${DOMAIN}"