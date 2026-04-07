@echo off
chcp 65001 >nul
echo ========================================
echo 闲鱼助手 - 后端构建脚本
echo ========================================
echo.

echo [步骤 1/3] 检查环境...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 Maven，请先安装 Maven
    pause
    exit /b 1
)
echo ✅ Maven 已安装

echo.
echo [步骤 2/3] 构建后端项目...
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
echo [步骤 3/3] 验证构建结果...
if exist "target\XianYuAssistant-0.0.1-SNAPSHOT.jar" (
    echo ✅ 后端构建成功！
    echo.
    echo ========================================
    echo 构建完成！
    echo ========================================
    echo.
    echo JAR包位置: target/XianYuAssistant-0.0.1-SNAPSHOT.jar
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
