# 网站批量生成系统

一个可视化的后台管理系统，用于批量导入公司信息并生成网站。

## ✨ 新版本更新 (2026-03-28)

- ✅ 简化数据模型，只保留核心字段
- ✅ 支持新版Excel模板（序号、公司名称、邮箱、域名、备案号）
- ✅ 添加腾讯云服务器部署支持
- ✅ 提供自动化部署脚本
- ✅ 完整的部署文档和配置指南

📖 [查看修改总结](修改总结.md) | [快速配置指南](快速配置指南.md) | [腾讯云部署指南](腾讯云部署指南.md) | [部署资源说明](部署资源说明.md)

## 项目结构

```
website-batch-system/
├── backend/                  # 后端项目
│   ├── src/main/java/com/website/
│   │   ├── controller/       # 控制器层
│   │   ├── service/          # 服务层
│   │   ├── repository/       # 数据访问层
│   │   ├── entity/           # 实体类
│   │   ├── dto/              # 数据传输对象
│   │   ├── util/             # 工具类
│   │   ├── generator/        # 网站生成器
│   │   └── WebsiteBatchSystemApplication.java
│   ├── src/main/resources/
│   │   ├── templates/        # HTML模板
│   │   └── schema.sql        # 数据库初始化脚本
│   ├── pom.xml
│   └── application.yml
│
├── frontend/                 # 前端项目
│   ├── src/
│   │   ├── components/       # 公共组件
│   │   ├── pages/            # 页面组件
│   │   ├── services/         # API服务
│   │   ├── types/            # 类型定义
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

## 数据库设计

### companies 表
- 公司基本信息
- 备案信息
- 腾讯云信息
- 网站状态信息

### website_contents 表
- 网站可定制内容
- 支持多种内容类型

### website_templates 表
- 网站模板
- 可自定义模板

## 快速开始

### 1. 数据库准备

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE website_batch_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入表结构
source backend/src/main/resources/schema.sql;
```

### 2. 后端启动

```bash
cd backend

# 修改数据库配置 (application.yml)
# 修改 username 和 password

# Maven构建
mvn clean install

# 启动项目
mvn spring-boot:run
```

后端服务地址: http://localhost:8080/api

### 3. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

前端开发地址: http://localhost:5173

## 功能说明

### 1. 后台管理（全新设计）
- 📊 数据概览 - 实时统计公司数据
- 🏢 公司管理 - 增删改查公司信息
- 🔍 搜索筛选 - 按名称、域名、备案号搜索
- ✨ 快速操作 - 一键生成网站、发布、取消发布
- 🎨 现代化界面 - 流畅的动画和响应式布局
- 📱 移动端支持 - 适配各种屏幕尺寸

### 2. Excel导入
- 下载Excel模板
- 上传Excel文件导入公司信息
- 查看导入结果

### 3. 模板管理
- 查看可用模板
- 预览模板

### 4. 网站生成
- 选择模板
- 配置公司信息
- 一键生成网站
- 网站访问: http://domain/index.html

---

## 🎨 访问管理页面

启动项目后，访问：
```
http://localhost:5173/admin
```

或
```
http://localhost:8080/admin
```

查看全新的现代化管理界面！

## Excel模板格式

| 序号 | 公司名称 | 邮箱 | 域名 | 备案号 |
|------|----------|------|------|--------|
| 1 | 测试公司1 | test1@example.com | test1.com | 京ICP备12345678号 |
| 2 | 测试公司2 | test2@example.com | test2.com | 京ICP备87654321号 |

💡 **提示**：
- 序号：从1开始递增
- 公司名称：必填
- 域名：必填，必须唯一
- 邮箱：选填
- 备案号：选填

📥 [下载模板](frontend/src/pages/ExcelUpload.jsx) - 点击页面上的"下载模板"按钮

## 部署说明

### 🚀 快速部署（推荐新手）

查看 [腾讯云部署指南](腾讯云部署指南.md) 了解详细的部署步骤。

**快速开始**：
1. 阅读 [快速配置指南](快速配置指南.md)
2. 使用自动部署脚本 `deploy-server.sh`
3. 按照指南完成部署

### 本地开发

#### 后端部署

```bash
# 构建jar包
cd backend
mvn clean package

# 运行jar包
java -jar target/website-batch-system-1.0.0.jar

# 或使用Docker
docker build -t website-batch-system .
docker run -p 8080:8080 website-batch-system
```

### 前端部署

```bash
cd frontend
npm install
npm run build

# 将 dist 目录内容部署到服务器
```

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name example.com;

    # 前端静态文件
    location / {
        root /var/www/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 网站生成目录
    location / {
        alias /var/www/html/;
    }
}
```

### 🛠️ 自动化脚本

- **start.bat / start.sh** - 本地快速启动
- **deploy-server.sh** - 服务器自动部署

### 📚 部署资源

- 📖 [腾讯云部署指南](腾讯云部署指南.md) - 详细部署文档
- 📖 [快速配置指南](快速配置指南.md) - 快速配置说明
- 📖 [部署资源说明](部署资源说明.md) - 资源清单和说明
- 📖 [修改总结](修改总结.md) - 代码修改详情

## API接口

### 公司相关
- GET /api/companies - 获取公司列表
- GET /api/companies/{id} - 获取公司详情
- POST /api/companies - 创建公司
- PUT /api/companies/{id} - 更新公司
- POST /api/companies/{id}/publish - 发布公司
- POST /api/companies/{id}/unpublish - 取消发布
- POST /api/companies/{id}/toggle-status - 切换状态

### Excel相关
- POST /api/excel/upload - 上传Excel
- GET /api/excel/companies - 获取导入的公司

### 模板相关
- GET /api/templates - 获取模板列表
- GET /api/templates/{id} - 获取模板详情

## 注意事项

1. 确保MySQL版本 >= 5.7
2. 修改数据库配置信息
3. 部署时需要修改网站输出路径
4. 网站生成需要配置Nginx访问权限

## 开发说明

### 添加新模板

1. 在 `backend/src/main/resources/templates/` 目录下创建HTML模板文件
2. 模板中使用 `${variable}` 语法来绑定数据
3. 在数据库中创建模板记录

### 自定义内容

1. 在公司详情页可以编辑公司信息
2. 在内容管理中可以添加/编辑网站内容
3. 支持多种内容类型: header, about, products, services等

## License

MIT
