# 网站批量处理系统 - 简化部署指南（无Docker版本）

## 📋 部署选项对比

| 选项 | 复杂度 | 适合场景 | 特点 |
|------|--------|----------|------|
| **方案一：脚本一键部署** | ⭐⭐ | 快速上线，传统服务器 | 全自动安装，无需Docker |
| **方案二：手动部署** | ⭐ | 测试/开发环境 | 最简手动步骤 |
| **方案三：GitHub Actions自动部署** | ⭐⭐⭐ | 持续集成 | 代码推送后自动部署 |

---

## 🚀 方案一：脚本一键部署（推荐）

### 服务器要求
- Ubuntu 20.04/22.04 或 CentOS 7/8
- Root权限
- 可访问互联网

### 部署步骤

#### 1. 上传文件到服务器
```bash
# 本地计算机执行
scp -r 项目目录 root@服务器IP:/tmp/website-batch-system/
```

#### 2. 在服务器上执行部署
```bash
# 登录服务器
ssh root@服务器IP

# 进入上传的目录
cd /tmp/website-batch-system

# 给脚本执行权限
chmod +x simple-deploy.sh

# 执行部署脚本
./simple-deploy.sh
```

### 脚本功能说明
- ✅ 自动安装Java 17
- ✅ 自动安装Nginx
- ✅ 配置反向代理（API转发到8080端口）
- ✅ 创建systemd服务
- ✅ 配置防火墙
- ✅ 部署应用文件

---

## 🛠️ 方案二：最简手动部署（5步完成）

### 第1步：安装Java
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk

# CentOS/RHEL
sudo yum install -y java-17-openjdk
```

### 第2步：安装Nginx
```bash
# Ubuntu/Debian
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx

# CentOS/RHEL
sudo yum install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 第3步：上传应用文件
```bash
# 创建目录
sudo mkdir -p /opt/website-batch-system/backend
sudo mkdir -p /var/www/html

# 上传后端JAR
scp backend/target/*.jar root@服务器IP:/opt/website-batch-system/backend/

# 上传前端文件
scp -r frontend/dist/* root@服务器IP:/var/www/html/
```

### 第4步：配置Nginx
```bash
# 编辑Nginx配置
sudo nano /etc/nginx/sites-available/website-batch-system

# 粘贴以下内容：
server {
    listen 80;
    server_name _;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        root /var/www/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}

# 启用配置
sudo ln -sf /etc/nginx/sites-available/website-batch-system /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 第5步：启动后端服务
```bash
# 创建systemd服务
sudo nano /etc/systemd/system/website-batch-system.service

# 粘贴以下内容：
[Unit]
Description=Website Batch System Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/website-batch-system/backend
ExecStart=/usr/bin/java -jar website-batch-system-1.0.0.jar --spring.profiles.active=cloud
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable website-batch-system
sudo systemctl start website-batch-system
```

---

## 🤖 方案三：GitHub Actions自动部署（无Docker）

### 1. 创建无Docker的工作流
创建 `.github/workflows/deploy-no-docker.yml`：

```yaml
name: Deploy Without Docker

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4

    - name: Build backend
      working-directory: ./backend
      run: mvn clean package -DskipTests

    - name: Build frontend
      working-directory: ./frontend
      run: |
        npm ci
        npm run build

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4

    - name: Deploy to server
      uses: appleboy/ssh-action@v1.0.0
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        script: |
          echo "=== 开始部署 ==="

          # 上传文件
          scp -r backend/target/*.jar ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }}:/tmp/
          scp -r frontend/dist/* ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }}:/tmp/

          # 在服务器上执行部署
          ssh ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }} "
            # 停止旧服务
            systemctl stop website-batch-system || true

            # 复制新文件
            cp /tmp/*.jar /opt/website-batch-system/backend/
            cp -r /tmp/dist/* /var/www/html/

            # 重启服务
            systemctl restart website-batch-system
            systemctl restart nginx

            echo '部署完成'
          "
```

### 2. 服务器准备脚本
创建 `server-setup.sh` 在服务器上执行：
```bash
#!/bin/bash
# 服务器初始化脚本
apt update
apt install -y openjdk-17-jdk nginx

# 创建目录
mkdir -p /opt/website-batch-system/backend
mkdir -p /var/www/html

# 配置Nginx（同上）
# 配置systemd服务（同上）
```

---

## 🔧 配置文件说明

### 1. 后端配置文件
位置：`backend/src/main/resources/application-cloud.yml`
```yaml
# 数据库配置（根据实际情况修改）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/website_batch_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /api
```

### 2. 数据库初始化
如果使用MySQL，执行：
```bash
# 安装MySQL
sudo apt install -y mysql-server

# 创建数据库
sudo mysql -u root -p
CREATE DATABASE website_batch_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit
```

---

## 📁 项目结构说明

```
/opt/website-batch-system/
├── backend/
│   └── website-batch-system-1.0.0.jar  # 后端JAR包
├── logs/                                # 日志目录（可选）
└── config/                              # 配置文件（可选）

/var/www/html/                           # 前端静态文件
├── index.html
├── assets/
└── ...
```

---

## 🚨 故障排除

### 1. Java相关问题
```bash
# 检查Java版本
java -version

# 如果没有Java 17，安装
sudo apt install -y openjdk-17-jdk

# 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Nginx相关问题
```bash
# 检查Nginx状态
sudo systemctl status nginx

# 检查Nginx配置
sudo nginx -t

# 查看Nginx日志
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/website-batch-system-access.log
```

### 3. 后端服务问题
```bash
# 检查服务状态
sudo systemctl status website-batch-system

# 查看服务日志
sudo journalctl -u website-batch-system -f

# 手动启动测试
cd /opt/website-batch-system/backend
java -jar website-batch-system-1.0.0.jar --spring.profiles.active=cloud
```

### 4. 端口占用问题
```bash
# 检查端口占用
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :80

# 如果端口被占用
sudo kill -9 <PID>
# 或者修改配置文件中的端口
```

### 5. 文件权限问题
```bash
# 确保目录权限正确
sudo chown -R root:root /opt/website-batch-system
sudo chown -R www-data:www-data /var/www/html  # Ubuntu
# 或
sudo chown -R nginx:nginx /var/www/html        # CentOS
```

---

## 🔄 更新部署流程

### 手动更新
```bash
# 1. 停止服务
sudo systemctl stop website-batch-system

# 2. 备份旧文件
cp /opt/website-batch-system/backend/*.jar /opt/website-batch-system/backend/backup/

# 3. 上传新文件
scp 新版本.jar root@服务器IP:/opt/website-batch-system/backend/
scp -r 新前端文件/* root@服务器IP:/var/www/html/

# 4. 重启服务
sudo systemctl start website-batch-system
```

### 自动化更新（配合Git）
```bash
# 服务器上创建更新脚本 update.sh
cat > /opt/website-batch-system/update.sh << 'EOF'
#!/bin/bash
cd /opt/website-batch-system
git pull
systemctl restart website-batch-system
echo "更新完成"
EOF

chmod +x /opt/website-batch-system/update.sh

# 每次更新只需执行
ssh root@服务器IP "/opt/website-batch-system/update.sh"
```

---

## 📊 监控和维护

### 查看系统状态
```bash
# 查看所有服务状态
systemctl list-units --type=service | grep website

# 查看资源使用
top -u root
df -h
free -h

# 查看应用日志
tail -f /var/log/nginx/website-batch-system-access.log
journalctl -u website-batch-system --since "1 hour ago"
```

### 定期维护
```bash
# 清理旧日志（每周）
find /var/log/nginx -name "*.log" -mtime +7 -delete
journalctl --vacuum-time=7d

# 备份数据库（每天）
mysqldump -u root -p website_batch_system > /backup/db_$(date +%Y%m%d).sql
```

---

## ✅ 部署验证清单

部署完成后检查：

1. ✅ **Java是否安装**：`java -version` 显示17.x
2. ✅ **Nginx是否运行**：`systemctl status nginx` 显示active
3. ✅ **后端服务是否运行**：`systemctl status website-batch-system` 显示active
4. ✅ **端口是否监听**：`netstat -tulpn | grep -E ':80|:8080'`
5. ✅ **网站可访问**：浏览器打开 `http://服务器IP` 显示页面
6. ✅ **API可访问**：`curl http://服务器IP/api/health` 返回正常
7. ✅ **静态文件可访问**：`curl http://服务器IP/` 返回HTML

---

## 🆘 获取帮助

遇到问题请检查：
1. **查看错误日志**（上面故障排除部分）
2. **验证配置文件**是否正确
3. **检查文件权限**和目录结构
4. **测试网络连接**和端口访问

如果仍然无法解决：
- 截图错误信息
- 提供服务器操作系统版本：`cat /etc/os-release`
- 提供相关服务状态：`systemctl status <服务名>`

---

## 📝 版本记录

- v1.0 (2026-03-29): 创建简化部署指南
- 特点：无需Docker，简单5步部署，支持手动/自动部署

---
*本指南由Claude Code生成，适用于快速部署场景*