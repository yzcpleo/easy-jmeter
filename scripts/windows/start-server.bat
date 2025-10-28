@echo off
REM =================================
REM Easy JMeter Server 启动脚本 (Windows)
REM =================================

echo.
echo ========================================
echo   启动 Easy JMeter Server
echo ========================================
echo.

REM 设置变量
set APP_HOME=%~dp0..\..
set JAR_FILE=%APP_HOME%\api\target\easyJmeter-0.1.0-RELEASE.jar
set CONFIG_FILE=%APP_HOME%\application-dev.yml
set LOG_DIR=%APP_HOME%\logs\server
set PID_FILE=%APP_HOME%\server.pid

REM 创建日志目录
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM 检查JAR文件是否存在
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR文件不存在: %JAR_FILE%
    echo [INFO] 请先编译项目: cd api && mvn clean package -DskipTests
    pause
    exit /b 1
)

REM 检查配置文件是否存在
if not exist "%CONFIG_FILE%" (
    echo [ERROR] 配置文件不存在: %CONFIG_FILE%
    echo [INFO] 请确保配置文件存在
    pause
    exit /b 1
)

REM 检查是否已经运行
if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /FI "PID eq %OLD_PID%" 2>nul | find /I "java.exe" >nul
    if not errorlevel 1 (
        echo [WARNING] Server已经在运行中 (PID: %OLD_PID%)
        echo [INFO] 如果需要重启，请先运行 stop-server.bat
        pause
        exit /b 1
    ) else (
        del "%PID_FILE%" 2>nul
    )
)

echo [INFO] 启动配置:
echo   JAR文件: %JAR_FILE%
echo   配置文件: %CONFIG_FILE%
echo   日志目录: %LOG_DIR%
echo.

REM JVM参数设置
set JVM_OPTS=-server
set JVM_OPTS=%JVM_OPTS% -Xms1g -Xmx2g
set JVM_OPTS=%JVM_OPTS% -XX:+UseG1GC
set JVM_OPTS=%JVM_OPTS% -XX:MaxGCPauseMillis=200
set JVM_OPTS=%JVM_OPTS% -XX:+HeapDumpOnOutOfMemoryError
set JVM_OPTS=%JVM_OPTS% -XX:HeapDumpPath=%LOG_DIR%\heapdump.hprof

REM 应用参数设置
set APP_OPTS=-Dfile.encoding=UTF-8
set APP_OPTS=%APP_OPTS% -Dspring.profiles.active=dev
set APP_OPTS=%APP_OPTS% -Dsocket.server.enable=true
set APP_OPTS=%APP_OPTS% -Dsocket.client.enable=false
set APP_OPTS=%APP_OPTS% -Dlogging.file.path=%LOG_DIR%

echo [INFO] 正在启动 Easy JMeter Server...
echo.

REM 启动应用
start "Easy JMeter Server" /B java %JVM_OPTS% %APP_OPTS% -jar "%JAR_FILE%" --spring.config.location="%CONFIG_FILE%" > "%LOG_DIR%\server.log" 2>&1

REM 等待启动
timeout /t 3 /nobreak >nul

REM 获取进程ID
for /f "tokens=2" %%i in ('tasklist /FI "WINDOWTITLE eq Easy JMeter Server" /FO CSV ^| find /V "PID"') do (
    set SERVER_PID=%%i
    echo %SERVER_PID% > "%PID_FILE%"
)

if defined SERVER_PID (
    echo [SUCCESS] Easy JMeter Server 启动成功!
    echo   进程ID: %SERVER_PID%
    echo   Web地址: http://localhost:5000
    echo   管理界面: http://localhost:3000
    echo   实时日志: tail -f "%LOG_DIR%\server.log"
    echo.
    echo [INFO] 按任意键继续查看日志，或关闭窗口后台运行...
    pause
    
    REM 显示实时日志
    echo.
    echo ======== 实时日志 (按 Ctrl+C 退出) ========
    powershell -Command "Get-Content '%LOG_DIR%\server.log' -Wait -Tail 50"
) else (
    echo [ERROR] 启动失败，请检查日志: %LOG_DIR%\server.log
    pause
    exit /b 1
)
