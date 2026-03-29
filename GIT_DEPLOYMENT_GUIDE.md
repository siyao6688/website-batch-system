# Git与自动化部署配置指南

## 已完成的工作

✅ **Git初始化完成**
- 已创建 `.gitignore` 文件（忽略 node_modules、target、IDE文件等）
- 已初始化本地Git仓库
- 已配置Git用户信息（用户名：zoe_siyao，邮箱：zoe_siyao@163.com）
- 已提交初始代码到本地仓库

✅ **GitHub Actions工作流已创建**
- 创建了 `.github/workflows/deploy.yml` 工作流
- 包含以下功能：
  - 自动构建后端（Maven）和前端（npm）
  - 上传构建产物
  - 通过SSH部署到服务器
  - Docker镜像构建（可选）

## 下一步操作指南

### 1. 在GitHub上创建远程仓库
1. 访问 https://github.com/new
2. 填写仓库信息：
   - Repository name: `website-batch-system`
   - Description: `网站批量处理系统`
   - 选择 Public 或 Private
   - **不要**初始化 README、.gitignore 或 license（已有本地仓库）
3. 点击 "Create repository"

### 2. 将本地仓库推送到GitHub
```bash
# 在项目根目录执行
git remote add origin https://github.com/你的用户名/website-batch-system.git
git branch -M main
git push -u origin main
```

### 3. 配置GitHub Secrets（自动化部署所需）
在GitHub仓库页面：
1. 进入 **Settings** → **Secrets and variables** → **Actions**
2. 点击 **New repository secret** 添加以下密钥：

| Secret名称 | 说明 | 示例值 |
|-----------|------|--------|
| `SERVER_HOST` | 服务器IP地址 | `123.45.67.89` |
| `SERVER_USER` | 服务器用户名 | `root` 或 `ubuntu` |
| `SERVER_PORT` | SSH端口（默认22） | `22` |
| `SSH_PRIVATE_KEY` | SSH私钥内容 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |

**如何获取SSH私钥？**
```bash
# 在本地计算机查看 ~/.ssh/id_rsa 文件
cat ~/.ssh/id_rsa

# 如果没有SSH密钥，生成新的：
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# 然后复制私钥内容
cat ~/.ssh/id_rsa
```

**在服务器上添加公钥：**
```bash
# 将公钥添加到服务器的 ~/.ssh/authorized_keys
echo "你的公钥内容" >> ~/.ssh/authorized_keys
```

### 4. 服务器准备（首次部署前）
确保服务器已安装：
- Docker 和 Docker Compose
- Java 17（如果使用传统部署）
- Node.js（如果使用传统部署）

可以使用现有脚本快速安装：
```bash
# 上传 deploy-server.sh 到服务器并执行
bash deploy-server.sh
```

### 5. 测试自动化部署
1. 推送代码到main分支：
   ```bash
   git add .
   git commit -m "测试自动化部署"
   git push
   ```
2. 在GitHub仓库查看Actions运行状态：
   - 进入 **Actions** 标签页
   - 查看 `Deploy Website Batch System` 工作流
3. 检查部署结果：
   ```bash
   # 在服务器上检查服务状态
   docker ps
   docker-compose logs
   ```

## 部署方式说明

### 方案A：Docker Compose部署（推荐）
- 使用现有 `docker-compose.yml` 文件
- 在服务器上自动构建和启动容器
- 包含MySQL数据库和后端服务

### 方案B：传统服务器部署
- 使用 `deploy-server.sh` 脚本
- 在服务器上安装Java、Node.js等依赖
- 直接运行jar包和静态文件

## 故障排除

### 常见问题
1. **SSH连接失败**
   - 检查服务器防火墙是否开放22端口
   - 确认SSH密钥权限正确：`chmod 600 ~/.ssh/id_rsa`
   - 验证公钥已添加到服务器的 `authorized_keys`

2. **构建失败**
   - 检查Java版本是否为17
   - 检查Node.js版本是否为20
   - 查看GitHub Actions日志获取详细错误信息

3. **部署后服务无法访问**
   - 检查服务器防火墙是否开放80/8080端口
   - 检查Docker容器是否正常运行：`docker ps`
   - 查看容器日志：`docker-compose logs`

### 修改部署配置
如需调整部署流程，编辑 `.github/workflows/deploy.yml`：
- 修改触发条件（分支、路径）
- 调整构建步骤
- 更改部署脚本

## 后续优化建议

1. **添加测试阶段**：在构建后运行单元测试
2. **环境分离**：设置开发、测试、生产多环境
3. **数据库迁移**：添加Flyway或Liquibase管理数据库变更
4. **监控告警**：集成监控和错误报告

## 联系方式
如有问题，请参考项目文档或联系维护者。

---
*本指南由Claude Code生成，最后更新：2026-03-29*