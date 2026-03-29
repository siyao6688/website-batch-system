# Docker 快速启动指南

## 🚀 快速启动

### 方法一：使用启动脚本（推荐）

#### Windows 用户
```bash
start-docker.bat
```

#### Linux/Mac 用户
```bash
chmod +x start-docker.sh
./start-docker.sh
```

---

### 方法二：使用 Docker Compose

#### 1. 确保在项目根目录
```bash
cd /d/Code/website-batch-system  # Windows
# 或
cd /path/to/website-batch-system  # Linux/Mac
```

#### 2. 启动所有服务
```bash
docker-compose up -d --build
```

---

## 📋 启动流程

### 第一阶段：环境检查
- [ ] Docker 已安装
- [ ] Docker Compose 已安装
- [ ] 端口 8080 未被占用

### 第二阶段：服务启动
1. **启动 MySQL**
   - 镜像：mysql:8.0
   - 端口：3306
   - 数据库：website_batch_system
   - 用户名：root
   - 密码：root

2. **等待 MySQL 就绪**
   - 预计时间：30-60秒
   - 状态：healthy

3. **启动后端**
   - 镜像：Maven构建的Spring Boot应用
   - 端口：8080
   - 数据源：mysql:3306

---

## 🔍 查看服务状态

```bash
# 查看所有服务
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务
docker-compose logs -f mysql
docker-compose logs -f backend
```

---

## 🌐 访问地址

启动成功后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost:8080 | 主页面 |
| Excel导入 | http://localhost:8080/excel | 导入页面 |
| 后端API | http://localhost:8080/api/excel/companies | API测试 |

---

## 🛠️ 常用命令

### 服务管理
```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重启特定服务
docker-compose restart mysql
docker-compose restart backend

# 查看状态
docker-compose ps
```

### 查看日志
```bash
# 查看所有日志
docker-compose logs -f

# 查看MySQL日志
docker-compose logs -f mysql

# 查看后端日志
docker-compose logs -f backend

# 查看最近100行
docker-compose logs --tail=100 backend
```

### 进入容器
```bash
# 进入MySQL容器
docker-compose exec mysql bash

# 进入后端容器
docker-compose exec backend sh
```

### 数据库操作
```bash
# 连接到MySQL
docker-compose exec mysql mysql -uroot -proot website_batch_system

# 导出数据库
docker-compose exec mysql mysqldump -uroot -proot website_batch_system > backup.sql

# 导入数据库
docker-compose exec -T mysql mysql -uroot -proot website_batch_system < backup.sql
```

---

## ⚠️ 常见问题

### 问题1：端口已被占用

**错误信息**：
```
ERROR: for website-batch-backend  ... bind: address already in use
```

**解决方法**：
```bash
# Windows - 查找并杀死占用进程
netstat -ano | findstr :8080
taskkill /F /PID <进程ID>

# Linux/Mac - 查找并杀死占用进程
lsof -ti :8080 | xargs kill -9

# 或修改端口映射（编辑 docker-compose.yml）
ports:
  - "8081:8080"  # 使用8081端口
```

---

### 问题2：Docker Compose 未安装

**错误信息**：
```
docker-compose: command not found
```

**解决方法**：

**方法一：使用 Docker Desktop（推荐）**
```bash
# 安装 Docker Desktop
# https://www.docker.com/products/docker-desktop

# Docker Desktop 已包含 docker compose（带空格）
docker compose up -d
```

**方法二：安装 Docker Compose**
```bash
# 下载最新版本
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 验证
docker-compose --version
```

---

### 问题3：构建失败

**错误信息**：
```
ERROR: Service 'backend' failed to build
```

**解决方法**：

**方法一：查看详细错误**
```bash
docker-compose build backend
```

**方法二：清除缓存重新构建**
```bash
docker-compose build --no-cache backend
```

**方法三：检查Maven依赖**
```bash
# 检查 pom.xml 是否正确
cat backend/pom.xml

# 手动下载依赖
docker-compose exec backend mvn dependency:resolve
```

---

### 问题4：MySQL 启动失败

**错误信息**：
```
ERROR: for mysql  ... cannot start service mysql
```

**解决方法**：

**方法一：查看MySQL日志**
```bash
docker-compose logs mysql
```

**方法二：检查端口**
```bash
# MySQL 使用3306端口，确保未被占用
netstat -ano | findstr :3306
```

**方法三：检查数据卷**
```bash
# 检查数据卷
docker volume ls

# 删除旧数据卷重新开始
docker-compose down -v
docker-compose up -d
```

---

### 问题5：权限问题

**错误信息**：
```
permission denied
```

**解决方法**：

**方法一：使用 sudo（Linux/Mac）**
```bash
sudo docker-compose up -d
```

**方法二：添加当前用户到 docker 组**
```bash
# Linux/Mac
sudo usermod -aG docker $USER
newgrp docker
```

---

## 📊 服务架构

```
┌─────────────────────────────────────────────────────┐
│                   用户浏览器                          │
└────────────────────┬────────────────────────────────┘
                     │
                     │ HTTP
                     ↓
┌─────────────────────────────────────────────────────┐
│              Nginx (可选，Docker配置中已包含)         │
└────────────────────┬────────────────────────────────┘
                     │
                     │
                     ↓
        ┌────────────┴────────────┐
        ↓                         ↓
┌──────────────────┐     ┌──────────────────┐
│   MySQL 8.0      │     │  Spring Boot     │
│   (端口 3306)    │     │  (端口 8080)     │
│  - 数据存储      │     │  - REST API      │
│  - 查询数据      │     │  - Excel导入     │
└──────────────────┘     └──────────────────┘
        ↑                         ↑
        └────────────┬────────────┘
                     │
                     │ docker network
                     ↓
        ┌──────────────────────┐
        │   Docker Network     │
        │   (backend)          │
        └──────────────────────┘
```

---

## 🔧 配置文件说明

### docker-compose.yml

| 组件 | 说明 |
|------|------|
| mysql | MySQL 8.0 数据库 |
| backend | Spring Boot 后端服务 |
| mysql_data | MySQL 数据持久化卷 |
| nginx_html | 静态文件存储卷 |
| backend | Docker 网络 |

### 数据卷映射

**MySQL 数据**：
```yaml
volumes:
  - mysql_data:/var/lib/mysql
```

**应用配置**：
```yaml
volumes:
  - ./backend/src/main/resources/templates:/app/templates
  - ./backend/src/main/resources/static:/app/static
```

**静态文件输出**：
```yaml
volumes:
  - nginx_html:/var/www/html
```

---

## 💾 数据备份

### 备份 MySQL 数据
```bash
# 导出数据库
docker-compose exec mysql mysqldump -uroot -proot website_batch_system > backup_$(date +%Y%m%d).sql

# 打包导出
docker-compose exec mysql mysqldump -uroot -proot website_batch_system | gzip > backup_$(date +%Y%m%d).sql.gz
```

### 恢复 MySQL 数据
```bash
# 解压备份
gunzip backup_20260328.sql.gz

# 导入数据库
cat backup_20260328.sql | docker-compose exec -T mysql mysql -uroot -proot website_batch_system
```

### 备份数据卷
```bash
# 备份 MySQL 数据卷
docker run --rm -v website-batch-system_mysql_data:/data -v $(pwd):/backup ubuntu tar czf /backup/mysql_backup.tar.gz /data

# 备份 Nginx 文件卷
docker run --rm -v website-batch-system_nginx_html:/data -v $(pwd):/backup ubuntu tar czf /backup/html_backup.tar.gz /data
```

---

## 🧹 清理和重置

### 停止所有服务
```bash
docker-compose down
```

### 停止并删除数据卷
```bash
docker-compose down -v
```

### 清理所有相关容器和网络
```bash
docker-compose down -v --remove-orphans
```

### 重新构建并启动
```bash
docker-compose down -v
docker-compose up -d --build
```

---

## ✅ 部署检查清单

### 启动前
- [ ] Docker 已安装并运行
- [ ] Docker Compose 已安装
- [ ] 端口 8080 未被占用
- [ ] 端口 3306 未被占用（除非本地有MySQL）

### 启动中
- [ ] MySQL 容器已启动
- [ ] MySQL 状态为 healthy
- [ ] 后端容器已启动
- [ ] 后端可以访问

### 启动后
- [ ] 前端页面可访问：http://localhost:8080
- [ ] Excel导入页面可访问：http://localhost:8080/excel
- [ ] API可访问：curl http://localhost:8080/api/excel/companies
- [ ] 数据库连接正常
- [ ] 可以导入Excel文件

---

## 📞 获取帮助

### 查看帮助信息
```bash
docker-compose --help
docker-compose mysql --help
docker-compose backend --help
```

### 查看系统信息
```bash
# Docker 版本
docker --version
docker-compose --version

# 磁盘使用
docker system df

# 网络状态
docker network ls
```

---

## 🎉 完成！

启动成功后，你可以：

1. **访问系统**：http://localhost:8080
2. **导入数据**：点击"Excel导入"按钮
3. **查看数据**：在公司列表中查看导入的公司
4. **生成网站**：选择公司并生成网站

**常用命令**：
```bash
# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 停止服务
docker-compose down
```

祝使用愉快！🚀
