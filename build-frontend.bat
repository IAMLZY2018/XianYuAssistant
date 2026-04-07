@echo off
chcp 65001 >nul
echo ========================================
echo 闲鱼助手 - 前端构建脚本
echo ========================================
echo.

echo [步骤 1/3] 检查环境...
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

echo.
echo [步骤 2/3] 构建前端项目...
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

cd ..

echo.
echo [步骤 3/3] 验证构建结果...
if exist "src\main\resources\static\index.html" (
    echo ✅ 前端构建成功！
    echo.
    echo ========================================
    echo 构建完成！
    echo ========================================
    echo.
    echo 文件已部署到: src/main/resources/static/
    echo.
    echo 现在可以启动 Spring Boot 应用，访问 http://localhost:12400
    echo ========================================
) else (
    echo ❌ 前端构建文件未找到！
    pause
    exit /b 1
)

pause
