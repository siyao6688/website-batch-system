#!/bin/bash
# 批量创建Nginx配置脚本
# 自动扫描网站目录并为每个域名创建Nginx配置

set -e

echo "🌐 批量创建Nginx配置脚本"
echo "========================================="

# 可能的网站目录（按优先级）
POSSIBLE_DIRS=(
    "/var/www/html"
    "/opt/website-batch-system/backend/generated-websites"
    "/opt/website-batch-system/generated-websites"
    "/home/ubuntu/website-batch-system/backend/generated-websites"
)

# 查找包含网站的目录
WEBSITE_ROOT=""
for DIR in "${POSSIBLE_DIRS[@]}"; do
    if [ -d "$DIR" ] && [ -n "$(ls -A "$DIR" 2>/dev/null)" ]; then
        WEBSITE_ROOT="$DIR"
        echo "✅ 找到网站目录: $DIR"
        echo "   包含网站: $(ls "$DIR" | tr '\n' ' ')"
        break
    fi
done

if [ -z "$WEBSITE_ROOT" ]; then
    echo "❌ 未找到网站目录，请检查路径"
    echo "   检查的目录："
    for DIR in "${POSSIBLE_DIRS[@]}"; do
        if [ -d "$DIR" ]; then
            echo "   - $DIR (存在但为空)"
        else
            echo "   - $DIR (不存在)"
        fi
    done
    exit 1
fi

# 统计网站数量
DOMAIN_COUNT=$(ls -d "$WEBSITE_ROOT"/*/ 2>/dev/null | grep -c "/" || true)
if [ "$DOMAIN_COUNT" -eq 0 ]; then
    echo "❌ 在 $WEBSITE_ROOT 中未找到网站目录"
    exit 1
fi

echo ""
echo "📊 找到 $DOMAIN_COUNT 个网站"
echo "========================================="

# 批量创建配置
SUCCESS_COUNT=0
FAIL_COUNT=0

for SITE_DIR in "$WEBSITE_ROOT"/*/; do
    # 提取域名（目录名）
    DOMAIN=$(basename "$SITE_DIR")

    # 检查是否是有效域名（包含点号）
    if [[ "$DOMAIN" == *.* ]]; then
        echo ""
        echo "🔧 处理: $DOMAIN"
        echo "   目录: $SITE_DIR"

        # 检查网站文件是否存在
        if [ ! -f "$SITE_DIR/index.html" ]; then
            echo "   ⚠️  警告: index.html 不存在"
            ((FAIL_COUNT++)) || true
            continue
        fi

        # 创建Nginx配置
        CONFIG_FILE="/etc/nginx/sites-available/${DOMAIN}.conf"
        ENABLED_CONFIG="/etc/nginx/sites-enabled/${DOMAIN}.conf"

        # 创建配置内容
        cat > "$CONFIG_FILE" << EOF
server {
    listen 80;
    server_name ${DOMAIN} www.${DOMAIN};

    root ${SITE_DIR};
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

        echo "   ✅ 配置已创建: $CONFIG_FILE"

        # 启用配置（如果未启用）
        if [ ! -L "$ENABLED_CONFIG" ]; then
            ln -sf "$CONFIG_FILE" "$ENABLED_CONFIG"
            echo "   ✅ 配置已启用: $ENABLED_CONFIG"
        else
            echo "   ⚠️  配置已存在，跳过启用"
        fi

        ((SUCCESS_COUNT++)) || true
    else
        echo ""
        echo "⏭️  跳过无效域名目录: $DOMAIN"
    fi
done

echo ""
echo "========================================="
echo "📊 批量创建完成"
echo "   成功: $SUCCESS_COUNT 个"
echo "   失败: $FAIL_COUNT 个"

if [ "$SUCCESS_COUNT" -gt 0 ]; then
    echo ""
    echo "🔧 下一步操作："
    echo "   1. 测试Nginx配置："
    echo "      sudo nginx -t"
    echo ""
    echo "   2. 如果测试通过，重载Nginx："
    echo "      sudo systemctl reload nginx"
    echo ""
    echo "   3. 验证网站访问："
    echo "      for domain in $(ls -d "$WEBSITE_ROOT"/*/ 2>/dev/null | xargs -I {} basename {}); do"
    echo "        echo \"http://\$domain\""
    echo "      done"
    echo ""
    echo "⚠️  注意："
    echo "   - 确保域名DNS已解析到服务器IP (124.223.45.101)"
    echo "   - 如果使用HTTPS，需要额外配置SSL证书"
    echo "   - 如需回滚，删除 /etc/nginx/sites-available/*.conf 和 /etc/nginx/sites-enabled/*.conf"
else
    echo ""
    echo "❌ 没有成功创建任何配置"
    echo "   请检查网站目录结构是否正确"
fi