@echo off
REM 网站批量生成系统 - Windows 部署脚本
REM 使用方法：双击运行或在命令行执行 deploy.bat

setlocal

echo ==========================================
echo 网站批量生成系统 - 部署脚本
echo ==========================================

REM 1. 构建
echo [1/4] 正在构建项目...
cd /d %~dp0
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo 构建失败!
    exit /b 1
)
echo ✓ 构建完成

REM 2. 上传
echo [2/4] 正在上传 JAR 文件到服务器...
scp -i C:/Users/1/.ssh/id_rsa target/website-batch-system-1.0.0.jar root@124.223.45.101:/opt/website-batch-system/backend/target/
if errorlevel 1 (
    echo 上传失败!
    exit /b 1
)
echo ✓ 上传完成

REM 3. 重启服务
echo [3/4] 正在重启服务...
ssh -i C:/Users/1/.ssh/id_rsa root@124.223.45.101 "pkill -f 'website-batch-system-1.0.0.jar' || true"
timeout /t 3 /nobreak > nul
ssh -i C:/Users/1/.ssh/id_rsa root@124.223.45.101 "cd /opt/website-batch-system/backend && nohup java -jar target/website-batch-system-1.0.0.jar > app.log 2>&1 &"
echo ✓ 服务已重启

REM 4. 验证
echo [4/4] 验证服务状态...
timeout /t 10 /nobreak > nul
ssh -i C:/Users/1/.ssh/id_rsa root@124.223.45.101 "ps aux | grep 'website-batch-system' | grep -v grep && echo --- && curl -s http://localhost:8080/api/companies/1 | head -c 200"

echo.
echo ==========================================
echo ✓ 部署完成！
echo ==========================================

endlocal
pause
