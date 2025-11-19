@echo off
chcp 65001 >nul
REM =================================
REM Easy JMeter Server 开发模式启动脚本（简化版）
REM 使用已编译的 JAR 包 + DevTools 热加载
REM =================================

echo.
echo ========================================
echo   启动 Easy JMeter Server (开发模式)
echo   支持热加载 - 修改代码后自动重启
echo ========================================
echo.

REM 设置 UTF-8 编码环境变量
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

REM 检查 JAR 文件是否存在
if not exist "target\easyJmeter-0.1.0-RELEASE.jar" (
    echo [WARNING] JAR文件不存在，正在编译...
    echo [INFO] 首次启动需要编译，请稍候...
    call mvn clean package -DskipTests -Dmaven.clean.skip=true
    if errorlevel 1 (
        echo [ERROR] 编译失败，请检查错误信息
        pause
        exit /b 1
    )
)

REM 检查 Java 是否可用
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请确保 Java 已安装并在 PATH 中
    pause
    exit /b 1
)

echo [INFO] 正在启动开发模式（支持热加载）...
echo [INFO] 修改代码后，需要重新编译才会生效
echo [INFO] 按 Ctrl+C 停止服务
echo.

REM 使用 Java 启动 JAR 包，DevTools 会自动检测文件变化
java "-Dfile.encoding=UTF-8" "-Dspring.profiles.active=dev" -jar target\easyJmeter-0.1.0-RELEASE.jar "--spring.config.additional-location=file:./application-dev.yml"

if errorlevel 1 (
    echo.
    echo [ERROR] 启动失败
    pause
)

