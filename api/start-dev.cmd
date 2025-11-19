@echo off
chcp 65001 >nul
REM =================================
REM Easy JMeter Server 开发模式启动脚本
REM 支持热加载，修改代码后自动重启
REM =================================

echo.
echo ========================================
echo   启动 Easy JMeter Server (开发模式)
echo   支持热加载 - 修改代码后自动重启
echo ========================================
echo.

REM 设置变量 - 确保UTF-8编码
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
set MAVEN_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8

REM 检查 Maven 是否可用
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Maven，请确保 Maven 已安装并在 PATH 中
    echo [INFO] 或者使用 start-dev-simple.cmd 启动已编译的 JAR 包
    pause
    exit /b 1
)

REM 检查 Java 是否可用
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请确保 Java 已安装并在 PATH 中
    pause
    exit /b 1
)

REM 显示 Java 版本信息
echo [INFO] 检查 Java 版本...
java -version 2>&1 | findstr /C:"version"
echo.

REM 检查 pom.xml 是否存在
if not exist "pom.xml" (
    echo [ERROR] 未找到 pom.xml 文件
    echo [INFO] 请确保在 api 目录下运行此脚本
    pause
    exit /b 1
)

REM 检查端口是否被占用，如果被占用则等待释放
echo [INFO] 检查端口占用情况...
set HTTP_PORT=5000
set SOCKET_PORT=9006
set PORT_CHECKED=0

REM 检查HTTP端口5000
netstat -ano | findstr ":%HTTP_PORT%" >nul 2>&1
if not errorlevel 1 (
    echo [WARN] 端口 %HTTP_PORT% 已被占用，等待端口释放...
    set PORT_CHECKED=1
    REM 等待最多10秒
    for /L %%i in (1,1,10) do (
        timeout /t 1 /nobreak >nul 2>&1
        netstat -ano | findstr ":%HTTP_PORT%" >nul 2>&1
        if errorlevel 1 (
            echo [INFO] 端口 %HTTP_PORT% 已释放
            goto :check_socket_port
        )
        echo [INFO] 等待中... %%i/10秒
    )
    echo [WARN] 端口 %HTTP_PORT% 仍然被占用，尝试继续启动...
)

:check_socket_port
REM 检查SocketIO端口9006
netstat -ano | findstr ":%SOCKET_PORT%" >nul 2>&1
if not errorlevel 1 (
    echo [WARN] 端口 %SOCKET_PORT% 已被占用，等待端口释放...
    set PORT_CHECKED=1
    REM 等待最多10秒
    for /L %%i in (1,1,10) do (
        timeout /t 1 /nobreak >nul 2>&1
        netstat -ano | findstr ":%SOCKET_PORT%" >nul 2>&1
        if errorlevel 1 (
            echo [INFO] 端口 %SOCKET_PORT% 已释放
            goto :start_app
        )
        echo [INFO] 等待中... %%i/10秒
    )
    echo [WARN] 端口 %SOCKET_PORT% 仍然被占用，尝试继续启动...
)

:start_app
if %PORT_CHECKED%==1 (
    echo [INFO] 等待完成，继续启动...
    echo.
)

echo [INFO] 正在启动开发模式（支持热加载）...
echo [INFO] 修改代码后，服务会自动重启
echo [INFO] 按 Ctrl+C 停止服务
echo.

REM 先编译项目（如果需要）
if not exist "target\classes" (
    echo [INFO] 首次启动，正在编译项目...
    mvn compile -DskipTests -Dfile.encoding=UTF-8
    if errorlevel 1 (
        echo [ERROR] 编译失败，请检查错误信息
        pause
        exit /b 1
    )
)

REM 使用 Maven 启动，支持热加载
REM 使用完整插件坐标，避免找不到插件的问题
mvn org.springframework.boot:spring-boot-maven-plugin:2.7.5:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--spring.config.additional-location=file:./application-dev.yml" -Dfile.encoding=UTF-8

if errorlevel 1 (
    echo.
    echo [ERROR] 启动失败
    pause
)

