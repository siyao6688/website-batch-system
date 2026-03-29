# 本地MySQL快速配置指南

## 📝 已完成的配置修改

`application-cloud.yml` 已更新为本地MySQL配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/website_batch_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: <请修改为你的MySQL密码>
```

---

## 🚀 快速配置步骤

### 第一步：在服务器上安装MySQL

#### Ubuntu/Debian系统
```bash
# 更新软件包
sudo apt update

# 安装MySQL服务器
sudo apt install -y mysql-server

# 启动MySQL服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 检查状态
sudo systemctl status mysql
```

#### CentOS/RHEL系统
```bash
# 安装MySQL
sudo yum install -y mysql-server

# 启动MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

---

### 第二步：配置MySQL安全

#### Ubuntu/Debian
```bash
# 运行安全配置脚本
sudo mysql_secure_installation

# 提示回答：
# Y - 设置root密码（建议设置强密码）
# Y - 移除匿名用户
# Y - 禁止root远程登录
# Y - 删除test数据库
# Y - 重新加载权限表
```

#### CentOS/RHEL
```bash
# 登录MySQL
sudo mysql

# 设置root密码
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的强密码';

# 授权远程访问（如果需要）
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '你的强密码' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;
```

---

### 第三步：创建数据库和用户

#### 方法一：使用命令行
```bash
# 登录MySQL
mysql -u root -p

# 执行以下SQL
CREATE DATABASE website_batch_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON website_batch_system.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 验证
mysql -u root -p website_batch_system -e "SHOW TABLES;"
```

#### 方法二：使用SQL脚本
```bash
# 创建SQL文件
nano create_database.sql
```

写入以下内容：
```sql
CREATE DATABASE IF NOT EXISTS website_batch_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON website_batch_system.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

```bash
# 执行脚本
mysql -u root -p < create_database.sql

# 验证
mysql -u root -p website_batch_system -e "SHOW TABLES;"
```

---

### 第四步：执行数据库表结构

```bash
# 执行schema.sql
mysql -u root -p website_batch_system < backend/src/main/resources/schema.sql

# 验证表是否创建成功
mysql -u root -p website_batch_system -e "SHOW TABLES;"

# 应该看到 companies 表
```

---

### 第五步：修改application-cloud.yml配置

编辑配置文件：
```bash
nano /opt/website-batch-system/backend/src/main/resources/application-cloud.yml
```

修改数据库密码：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/website_batch_system?...
    username: root
    password: 你的实际MySQL密码  # 修改这里
```

---

### 第六步：重新编译并启动后端

```bash
cd /opt/website-batch-system/backend

# 重新编译
mvn clean package -DskipTests

# 启动后端服务
systemctl start website-batch-system

# 查看状态
systemctl status website-batch-system

# 查看日志
journalctl -u website-batch-system -f
```

---

### 第七步：测试API

```bash
# 测试获取公司列表
curl http://localhost:8080/api/excel/companies

# 应该返回空数组
# {"data":[]}
```

---

## 🔍 验证MySQL配置

### 检查MySQL状态
```bash
sudo systemctl status mysql
```

### 登录MySQL测试
```bash
mysql -u root -p

# 查看数据库
SHOW DATABASES;

# 查看表
USE website_batch_system;
SHOW TABLES;

# 查看表结构
DESCRIBE companies;
```

### 测试Spring Boot连接
```bash
# 查看后端日志
tail -f /var/log/website-batch-system/website-batch-system.log
```

---

## 📋 MySQL版本要求

- **推荐版本**：MySQL 5.7 或 8.0
- **最低版本**：MySQL 5.7.8+
- **字符集**：utf8mb4
- **排序规则**：utf8mb4_unicode_ci

---

## 🔧 常用MySQL命令

### 数据库管理
```bash
# 登录MySQL
mysql -u root -p

# 查看所有数据库
SHOW DATABASES;

# 创建数据库
CREATE DATABASE database_name CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 选择数据库
USE database_name;

# 查看所有表
SHOW TABLES;

# 查看表结构
DESCRIBE table_name;

# 导出数据库
mysqldump -u root -p database_name > backup.sql

# 导入数据库
mysql -u root -p database_name < backup.sql
```

### 备份数据库
```bash
# 创建备份脚本
nano /root/backup-mysql.sh
```

写入：
```bash
#!/bin/bash
BACKUP_DIR="/root/mysql-backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump -u root -p<密码> website_batch_system > $BACKUP_DIR/website_batch_system_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -type f -mtime +7 -delete

echo "备份完成：$DATE"
```

```bash
# 添加到cron（每天凌晨2点备份）
crontab -e
# 添加以下行
0 2 * * * /root/backup-mysql.sh
```

---

## ⚠️ 常见问题

### 问题1：MySQL无法启动
```bash
# 查看错误日志
sudo tail -f /var/log/mysql/error.log

# 常见原因：
# - 端口被占用
# - 配置文件错误
# - 权限问题
```

### 问题2：无法连接到MySQL
```bash
# 检查MySQL是否运行
sudo systemctl status mysql

# 检查端口
netstat -tlnp | grep 3306

# 检查防火墙
sudo ufw allow 3306
```

### 问题3：权限不足
```bash
# 授予root本地访问权限
mysql -u root -p
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;
```

### 问题4：连接超时
```bash
# 检查MySQL bind-address配置
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf

# 确保 bind-address 为 127.0.0.1 或 0.0.0.0
bind-address = 127.0.0.1

# 重启MySQL
sudo systemctl restart mysql
```

---

## ✅ 配置检查清单

- [ ] MySQL服务器已安装并运行
- [ ] MySQL root密码已设置
- [ ] 数据库 website_batch_system 已创建
- [ ] 表结构已创建（companies表）
- [ ] application-cloud.yml 中密码已修改
- [ ] 后端服务已重新编译
- [ ] 后端服务已启动
- [ ] API测试成功

---

## 📞 获取帮助

### 查看日志
```bash
# MySQL日志
sudo tail -f /var/log/mysql/error.log

# 后端日志
journalctl -u website-batch-system -f
```

### 测试连接
```bash
# 从服务器本机测试
mysql -u root -p

# 应该能成功登录
```

---

## 🎉 完成

配置完成后，你就可以：
1. 通过后端API导入公司数据
2. 在前端管理页面查看数据
3. 生成网站并访问

**访问地址**：
- 后端管理页面：http://你的域名/
- 后端API：http://你的域名/api/
- Excel导入：http://你的域名/excel

数据库配置为本地MySQL后，系统将更简单、更快，无需配置网络和安全组！
