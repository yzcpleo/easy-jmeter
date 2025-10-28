@echo off
REM =================================
REM Easy JMeter Agent 启动脚本 (Windows)
REM =================================

echo.
echo ========================================
echo   启动 Easy JMeter Agent
echo ========================================
echo.

REM 设置变量
set APP_HOME=%~dp0..\..
set JAR_FILE=%APP_HOME%\api\target\easyJmeter-0.1.0-RELEASE.jar
set CONFIG_FILE=%APP_HOME%\application-agent.yml
set LOG_DIR=%APP_HOME%\logs\agent
set PID_FILE=%APP_HOME%\agent.pid

REM 创建日志目录
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM 检查JAR文件是否存在
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR文件不存在: %JAR_FILE%
    echo [INFO] 请先编译项目: cd api && mvn clean package -DskipTests
    pause
    exit /b 1
)

REM 检查配置文件是否存在，如果不存在则创建默认配置
if not exist "%CONFIG_FILE%" (
    echo [INFO] 配置文件不存在，创建默认配置: %CONFIG_FILE%
    call :CREATE_DEFAULT_CONFIG "%CONFIG_FILE%"
)

REM 检查JMeter是否安装
set JMETER_PATH=C:\Program Files\apache-jmeter
if not exist "%JMETER_PATH%\bin\jmeter.bat" (
    echo [WARNING] JMeter未找到: %JMETER_PATH%
    echo [INFO] 请安装Apache JMeter到 %JMETER_PATH%
    set /p CUSTOM_PATH="请输入JMeter安装路径 (或按Enter跳过): "
    if defined CUSTOM_PATH set JMETER_PATH=%CUSTOM_PATH%
)

REM 检查是否已经运行
if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /FI "PID eq %OLD_PID%" 2>nul | find /I "java.exe" >nul
    if not errorlevel 1 (
        echo [WARNING] Agent已经在运行中 (PID: %OLD_PID%)
        echo [INFO] 如果需要重启，请先运行 stop-agent.bat
        pause
        exit /b 1
    ) else (
        del "%PID_FILE%" 2>nul
    )
)

echo [INFO] 启动配置:
echo   JAR文件: %JAR_FILE%
echo   配置文件: %CONFIG_FILE%
echo   JMeter路径: %JMETER_PATH%
echo   日志目录: %LOG_DIR%
echo.

REM JVM参数设置 (Agent通常需要更多内存用于JMeter执行)
set JVM_OPTS=-server
set JVM_OPTS=%JVM_OPTS% -Xms2g -Xmx4g
set JVM_OPTS=%JVM_OPTS% -XX:+UseG1GC
set JVM_OPTS=%JVM_OPTS% -XX:MaxGCPauseMillis=200
set JVM_OPTS=%JVM_OPTS% -XX:+HeapDumpOnOutOfMemoryError
set JVM_OPTS=%JVM_OPTS% -XX:HeapDumpPath=%LOG_DIR%\heapdump.hprof

REM 应用参数设置
set APP_OPTS=-Dfile.encoding=UTF-8
set APP_OPTS=%APP_OPTS% -Dspring.profiles.active=prod
set APP_OPTS=%APP_OPTS% -Dsocket.server.enable=false
set APP_OPTS=%APP_OPTS% -Dsocket.client.enable=true
set APP_OPTS=%APP_OPTS% -Dlogging.file.path=%LOG_DIR%
set APP_OPTS=%APP_OPTS% -Djmeter.home="%JMETER_PATH%"

echo [INFO] 正在启动 Easy JMeter Agent...
echo.

REM 启动应用
start "Easy JMeter Agent" /B java %JVM_OPTS% %APP_OPTS% -jar "%JAR_FILE%" --spring.config.location="%CONFIG_FILE%" > "%LOG_DIR%\agent.log" 2>&1

REM 等待启动
timeout /t 3 /nobreak >nul

REM 获取进程ID
for /f "tokens=2" %%i in ('tasklist /FI "WINDOWTITLE eq Easy JMeter Agent" /FO CSV ^| find /V "PID"') do (
    set AGENT_PID=%%i
    echo %AGENT_PID% > "%PID_FILE%"
)

if defined AGENT_PID (
    echo [SUCCESS] Easy JMeter Agent 启动成功!
    echo   进程ID: %AGENT_PID%
    echo   连接地址: 根据配置文件中的socket.client.serverUrl
    echo   实时日志: tail -f "%LOG_DIR%\agent.log"
    echo.
    echo [INFO] 按任意键继续查看日志，或关闭窗口后台运行...
    pause
    
    REM 显示实时日志
    echo.
    echo ======== 实时日志 (按 Ctrl+C 退出) ========
    powershell -Command "Get-Content '%LOG_DIR%\agent.log' -Wait -Tail 50"
) else (
    echo [ERROR] 启动失败，请检查日志: %LOG_DIR%\agent.log
    pause
    exit /b 1
)

goto :EOF

:CREATE_DEFAULT_CONFIG
echo # Easy JMeter Agent Configuration > "%~1"
echo server: >> "%~1"
echo   port: 5000 >> "%~1"
echo. >> "%~1"
echo spring: >> "%~1"
echo   application: >> "%~1"
echo     name: easy-jmeter-agent >> "%~1"
echo   profiles: >> "%~1"
echo     active: prod >> "%~1"
echo. >> "%~1"
echo # SocketIO客户端配置 >> "%~1"
echo socket: >> "%~1"
echo   client: >> "%~1"
echo     serverUrl: http://localhost:9000 >> "%~1"
echo     enable: true >> "%~1"
echo   server: >> "%~1"
echo     enable: false >> "%~1"
echo. >> "%~1"
echo # JMeter配置 >> "%~1"
echo jmeter: >> "%~1"
echo   path: "C:\\Program Files\\apache-jmeter" >> "%~1"
echo. >> "%~1"
echo logging: >> "%~1"
echo   level: >> "%~1"
echo     io.github.guojiaxing1995.easyJmeter: INFO >> "%~1"

echo [INFO] 已创建默认配置文件: %~1
echo [INFO] 请根据实际情况修改 socket.client.serverUrl 等配置
goto :EOF
