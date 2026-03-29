@echo off
chcp 65001 >nul
cls
echo ========================================
echo   Website Batch System 启动脚本
echo   数据库：本地MySQL
echo ========================================
echo.

:: 检查Java
echo [1/5] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java未安装
    echo 请先安装Java 17: https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

echo ✓ Java已安装
java -version 2>&1 | findstr /i "version"
echo.

:: 检查Maven
echo [2/5] 检查Maven环境...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven未安装
    echo 请先安装Maven: https://maven.apache.org/install.html
    echo.
    pause
    exit /b 1
)

echo ✓ Maven已安装
mvn -version 2>&1 | findstr /i "Apache Maven"
echo.

:: 检查MySQL
echo [3/5] 检查MySQL环境...
where mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️  警告：MySQL未安装
    echo   你可以先安装MySQL：https://dev.mysql.com/downloads/mysql/
    echo.
    set mysql_continue=y
    if /i not "%mysql_continue%"=="y" (
        pause
        exit /b 1
    )
) else (
    echo ✓ MySQL客户端已安装
    mysql --version
)

echo.

:: 检查数据库连接
echo [4/5] 检查数据库连接...
mysql -u root -pAa123456. -e "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️  MySQL连接失败
    echo   请检查 application-cloud.yml 中的数据库密码
    echo.
    set db_continue=y
    if /i not "%db_continue%"=="y" (
        pause
        exit /b 1
    )
) else (
    echo ✓ MySQL连接成功
)

echo.
echo [5/5] 编译后端...
echo.

cd backend

echo 编译中，请稍候...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo ❌ 后端编译失败
    echo.
    echo 请查看上面的错误信息
    echo.
    cd ..
    pause
    exit /b 1
)

echo.
echo ✓ 后端编译完成
echo.

:: 启动后端
echo 🚀 启动后端服务...
start /B java -jar target/website-batch-system-1.0.0.jar > ..\backend.log 2>&1

if %errorlevel% neq 0 (
    echo.
    echo ❌ 后端启动失败
    echo.
    echo 请查看日志: type ..\backend.log
    echo.
    cd ..
    pause
    exit /b 1
)

echo ✓ 后端服务已启动
echo   日志文件: backend.log
echo.

:: 等待后端启动
echo ⏳ 等待后端启动...
timeout /t 10 /nobreak >nul

echo 🔍 测试API端点...
timeout /t 5 /nobreak >nul

echo.
echo 测试结果:
curl -s http://localhost:8080/api/excel/companies
echo.
echo ✓ API测试完成
echo.

cd ..
cls
echo ========================================
echo   后端启动成功！
echo ========================================
echo.
echo 访问地址：
echo   - 前端：http://localhost:5173
echo   - 后端API：http://localhost:8080/api
echo   - Excel导入页面：http://localhost:5173/excel
echo   - 管理页面：http://localhost:5173/admin
echo.
echo 停止后端：
echo   关闭此窗口
echo.
echo 查看后端日志：
echo   type backend.log
echo.
echo ========================================
echo.
echo 现在启动前端...
echo.

cd frontend

echo 安装依赖中...
call npm install
if %errorlevel% neq 0 (
    echo.
    echo ❌ 依赖安装失败
    echo.
    cd ..
    pause
    exit /b 1
)

echo.
echo ✓ 依赖安装完成
echo.

echo 启动前端...
start /B npm run dev > ..\frontend.log 2>&1

if %errorlevel% neq 0 (
    echo.
    echo ❌ 前端启动失败
    echo.
    echo 请查看日志: type ..\frontend.log
    echo.
    cd ..
    pause
    exit /b 1
)

echo ✓ 前端服务已启动
echo   日志文件: frontend.log
echo.

:: 等待前端启动
echo ⏳ 等待前端启动...
timeout /t 8 /nobreak >nul

cls
echo ========================================
echo   🎉 系统启动成功！
echo ========================================
echo.
echo 前端地址：http://localhost:5173
echo 数据库：本地MySQL
echo.
echo ========================================
echo.
echo 提示：
echo   1. 按 Ctrl+C 关闭此窗口停止所有服务
echo   2. 查看日志文件：
echo      - 后端: type ..\backend.log
echo      - 前端: type ..\frontend.log
echo   3. 停止服务：关闭此窗口
echo.
echo   正在打开前端页面...
echo.
start http://localhost:5173
echo.
echo ========================================
echo.
pause
