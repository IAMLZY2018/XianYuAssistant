@echo off
chcp 65001 >nul
echo ========================================
echo 闲鱼助手 - 完整构建脚本
echo ========================================
echo.

echo [步骤 1/4] 检查环境...
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 Node.js，请先安装 Node.js
    pause
    exit /b 1
)
echo ✅ Node.js 已安装

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 npm
    pause
    exit /b 1
)
echo ✅ npm 已安装

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 Maven，请先安装 Maven
    pause
    exit /b 1
)
echo ✅ Maven 已安装

echo.
echo [步骤 2/4] 构建前端项目...
cd vue-code

echo 检查 node_modules...
if not exist "node_modules" (
    echo 首次构建，正在安装依赖...
    call npm install
    if %errorlevel% neq 0 (
        echo ❌ 依赖安装失败！
        cd ..
        pause
        exit /b 1
    )
)

echo.
echo 清理旧的构建文件...
if exist "..\src\main\resources\static" (
    rmdir /s /q "..\src\main\resources\static"
    echo 已清理旧文件
)

echo.
echo 构建 Vue 项目...
call npm run build
if %errorlevel% neq 0 (
    echo ❌ 前端构建失败！
    cd ..
    pause
    exit /b 1
)

echo.
echo 验证前端构建结果...
if exist "..\src\main\resources\static\index.html" (
    echo ✅ 前端构建成功！
    echo 文件已部署到: src/main/resources/static/
) else (
    echo ❌ 前端构建文件未找到！
    cd ..
    pause
    exit /b 1
)

cd ..

echo.
echo [步骤 3/4] 构建后端项目 (Maven)...
echo 清理旧的构建文件...
call mvn clean
if %errorlevel% neq 0 (
    echo ❌ Maven clean 失败！
    pause
    exit /b 1
)

echo.
echo 编译并打包 Java 项目 (跳过测试)...
call mvn package -Dmaven.test.skip=true
if %errorlevel% neq 0 (
    echo ❌ Maven 打包失败！
    pause
    exit /b 1
)

echo.
echo [步骤 4/4] 验证构建结果...
if exist "target\XianYuAssistant-0.0.1-SNAPSHOT.jar" (
    echo ✅ 后端构建成功！
    echo.
    echo ========================================
    echo 构建完成！
    echo ========================================
    echo.
    echo 前端文件: src/main/resources/static/
    echo 后端JAR包: target/XianYuAssistant-0.0.1-SNAPSHOT.jar
    echo.
    echo 启动方式:
    echo   java -jar target/XianYuAssistant-0.0.1-SNAPSHOT.jar
    echo.
    echo 访问地址: http://localhost:12400
    echo ========================================
) else (
    echo ❌ 后端构建文件未找到！
    pause
    exit /b 1
)

pause
