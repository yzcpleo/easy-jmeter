@echo off
chcp 65001 >nul
REM =================================
REM Easy JMeter Server Development Mode Startup Script
REM Supports hot reload, auto restart after code changes
REM =================================

echo.
echo ========================================
echo   Starting Easy JMeter Server (Dev Mode)
echo   Hot Reload Enabled - Auto Restart on Code Changes
echo ========================================
echo.

REM Set environment variables - Ensure UTF-8 encoding
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
set MAVEN_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8

REM Check if Maven is available
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found, please ensure Maven is installed and in PATH
    echo [INFO] Or use start-dev-simple.cmd to start the compiled JAR file
    pause
    exit /b 1
)

REM Check if Java is available
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found, please ensure Java is installed and in PATH
    pause
    exit /b 1
)

REM Display Java version information
echo [INFO] Checking Java version...
java -version 2>&1 | findstr /C:"version"
echo.

REM Check if pom.xml exists
if not exist "pom.xml" (
    echo [ERROR] pom.xml file not found
    echo [INFO] Please ensure you are running this script in the api directory
    pause
    exit /b 1
)

REM Check if ports are in use, wait for release if occupied
echo [INFO] Checking port availability...
set HTTP_PORT=5000
set SOCKET_PORT=9006
set PORT_CHECKED=0

REM Check HTTP port 5000
netstat -ano | findstr ":%HTTP_PORT%" >nul 2>&1
if not errorlevel 1 (
    echo [WARN] Port %HTTP_PORT% is in use, waiting for release...
    set PORT_CHECKED=1
    REM Wait up to 10 seconds
    for /L %%i in (1,1,10) do (
        timeout /t 1 /nobreak >nul 2>&1
        netstat -ano | findstr ":%HTTP_PORT%" >nul 2>&1
        if errorlevel 1 (
            echo [INFO] Port %HTTP_PORT% has been released
            goto check_socket_port
        )
        echo [INFO] Waiting... %%i/10 seconds
    )
    echo [WARN] Port %HTTP_PORT% is still in use, attempting to continue...
)

:check_socket_port
REM Check SocketIO port 9006
netstat -ano | findstr ":%SOCKET_PORT%" >nul 2>&1
if not errorlevel 1 (
    echo [WARN] Port %SOCKET_PORT% is in use, waiting for release...
    set PORT_CHECKED=1
    REM Wait up to 10 seconds
    for /L %%i in (1,1,10) do (
        timeout /t 1 /nobreak >nul 2>&1
        netstat -ano | findstr ":%SOCKET_PORT%" >nul 2>&1
        if errorlevel 1 (
            echo [INFO] Port %SOCKET_PORT% has been released
            goto start_app
        )
        echo [INFO] Waiting... %%i/10 seconds
    )
    echo [WARN] Port %SOCKET_PORT% is still in use, attempting to continue...
)

:start_app
if %PORT_CHECKED%==1 (
    echo [INFO] Waiting completed, continuing startup...
    echo.
)

echo [INFO] Starting development mode (hot reload enabled)...
echo [INFO] Service will auto-restart after code changes
echo [INFO] Press Ctrl+C to stop the service
echo.

REM Compile project first (if needed)
if not exist "target\classes" (
    echo [INFO] First startup, compiling project...
    mvn compile -DskipTests -Dfile.encoding=UTF-8
    if errorlevel 1 (
        echo [ERROR] Compilation failed, please check error messages
        pause
        exit /b 1
    )
)

REM Start using Maven with hot reload support
REM Use full plugin coordinates to avoid plugin not found issues
mvn org.springframework.boot:spring-boot-maven-plugin:2.7.5:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--spring.config.additional-location=file:./application-dev.yml" -Dfile.encoding=UTF-8

if errorlevel 1 (
    echo.
    echo [ERROR] Startup failed
    pause
)
