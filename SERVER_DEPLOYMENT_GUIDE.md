# 服务器部署指南

## 一、服务器环境要求

- 操作系统：Ubuntu 22.04+
- 已安装：Java 17、MySQL 8.0、Nginx、Node.js 18+

## 二、首次部署步骤

### 1. 克隆代码

```bash
cd /opt
git clone https://github.com/siyao6688/website-batch-system.git
cd website-batch-system
```

### 2. 创建数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE website_batch_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON website_batch_system.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. 配置后端

编辑 `backend/src/main/resources/application-cloud.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/website_batch_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的数据库密码

website:
  deployment:
    enabled: true  # 启用部署功能
    server:
      host: 124.223.45.101  # 服务器IP
      private-key-path: /root/.ssh/id_rsa  # SSH私钥路径
```

### 4. 构建后端

```bash
cd /opt/website-batch-system/backend
mvn clean package -DskipTests
```

### 5. 构建前端

```bash
cd /opt/website-batch-system/frontend
npm install
npm run build
```

### 6. 部署前端

```bash
# 创建前端目录
mkdir -p /opt/website-batch-system/frontend/dist

# 复制构建产物
cp -r dist/* /opt/website-batch-system/frontend/dist/
```

### 7. 配置 SSH 密钥（用于网站部署）

```bash
# 生成SSH密钥（如果没有）
ssh-keygen -t rsa -b 4096 -f /root/.ssh/id_rsa -N ""

# 将公钥添加到authorized_keys
cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys

# 测试SSH连接
ssh -i /root/.ssh/id_rsa root@localhost "echo SSH OK"
```

### 8. 配置 systemd 服务

创建服务文件：

```bash
cat > /etc/systemd/system/website-batch-system.service << 'EOF'
[Unit]
Description=Website Batch System Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/website-batch-system/backend
ExecStart=/usr/bin/java -jar /opt/website-batch-system/backend/target/website-batch-system-1.0.0.jar --spring.profiles.active=cloud
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
```

启用并启动服务：

```bash
systemctl daemon-reload
systemctl enable website-batch-system.service
systemctl start website-batch-system.service
```

### 9. 配置 Nginx

创建前端配置：

```bash
cat > /etc/nginx/sites-available/frontend << 'EOF'
server {
    listen 8088;
    server_name _;

    root /opt/website-batch-system/frontend/dist;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

# 启用配置
ln -sf /etc/nginx/sites-available/frontend /etc/nginx/sites-enabled/frontend

# 测试并重载
nginx -t && systemctl reload nginx
```

### 10. 优化 Nginx（处理大量网站）

编辑 `/etc/nginx/nginx.conf`，在 `events` 块前添加：

```nginx
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
}
```

## 三、日常更新流程

### 方式一：手动更新

```bash
# 1. 拉取最新代码
cd /opt/website-batch-system
git pull origin main

# 2. 构建后端
cd backend
mvn clean package -DskipTests

# 3. 构建前端
cd ../frontend
npm run build
cp -r dist/* /opt/website-batch-system/frontend/dist/

# 4. 重启服务
systemctl restart website-batch-system.service
```

### 方式二：使用部署脚本

```bash
cd /opt/website-batch-system/backend
./deploy.sh
```

## 四、常用命令

### 服务管理

| 操作 | 命令 |
|------|------|
| 启动服务 | `systemctl start website-batch-system` |
| 停止服务 | `systemctl stop website-batch-system` |
| 重启服务 | `systemctl restart website-batch-system` |
| 查看状态 | `systemctl status website-batch-system` |
| 查看日志 | `journalctl -u website-batch-system -f` |
| 查看应用日志 | `tail -f /opt/website-batch-system/backend/app.log` |

### 数据库

| 操作 | 命令 |
|------|------|
| 登录数据库 | `mysql -u root -p website_batch_system` |
| 查看公司数量 | `SELECT COUNT(*) FROM companies;` |
| 查看已发布网站 | `SELECT COUNT(*) FROM companies WHERE is_published=1;` |

### Nginx

| 操作 | 命令 |
|------|------|
| 测试配置 | `nginx -t` |
| 重载配置 | `systemctl reload nginx` |
| 查看错误日志 | `tail -f /var/log/nginx/error.log` |

## 五、故障排查

### 1. 服务无法启动

```bash
# 检查日志
journalctl -u website-batch-system -n 50

# 检查端口占用
netstat -tlnp | grep 8080

# 检查Java进程
ps aux | grep java
```

### 2. 网站显示 502/500

```bash
# 检查后端服务是否运行
systemctl status website-batch-system

# 检查Nginx配置
nginx -t

# 检查Nginx错误日志
tail -f /var/log/nginx/error.log
```

### 3. SSH 部署失败

```bash
# 检查SSH密钥权限
ls -la /root/.ssh/id_rsa
# 应该是 600

# 测试SSH连接
ssh -i /root/.ssh/id_rsa root@localhost "echo OK"
```

### 4. 前端页面空白或报错

```bash
# 清除浏览器缓存 (Ctrl+F5)

# 检查前端文件是否存在
ls -la /opt/website-batch-system/frontend/dist/

# 重新构建前端
cd /opt/website-batch-system/frontend
npm run build
cp -r dist/* /opt/website-batch-system/frontend/dist/
```

## 六、配置文件说明

| 文件 | 用途 |
|------|------|
| `application.yml` | 本地开发配置 |
| `application-cloud.yml` | 服务器生产配置（使用 `--spring.profiles.active=cloud` 激活） |

**关键配置项：**

```yaml
website:
  deployment:
    enabled: true/false        # 是否启用SSH部署
    server:
      host: 服务器IP
      port: 22
      username: root
      private-key-path: /root/.ssh/id_rsa  # SSH私钥路径
    remote:
      web-root: /var/www/html              # 网站文件目录
      nginx-sites-available: /etc/nginx/sites-available
      nginx-sites-enabled: /etc/nginx/sites-enabled
```

## 七、访问地址

- 管理后台：http://服务器IP:8088
- 默认账号：xl / xl123456
- API地址：http://服务器IP:8088/api