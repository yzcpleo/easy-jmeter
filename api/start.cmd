@echo off
chcp 65001 >nul
REM =================================
REM Easy JMeter Server 启动脚本
REM 自动检测并使用 Java 8
REM =================================

echo.
echo ========================================
echo   启动 Easy JMeter Server
echo ========================================
echo.

REM 设置变量
set JAR_FILE=target\easyJmeter-0.1.0-RELEASE.jar
set CONFIG_FILE=application-dev.yml

REM 检查 JAR 文件是否存在
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR文件不存在: %JAR_FILE%
    echo [INFO] 请先编译项目: mvn clean package -DskipTests
    pause
    exit /b 1
)

REM 检查配置文件是否存在
if not exist "%CONFIG_FILE%" (
    echo [WARNING] 配置文件不存在: %CONFIG_FILE%
    echo [INFO] 将使用默认配置
)

REM 查找 Java 8
set JAVA_CMD=
set JAVA_FOUND=0

REM 方法1: 检查常见的 Java 8 安装路径
set JAVA_PATHS[0]=C:\Program Files\Java\jdk1.8.0_341\bin\java.exe
set JAVA_PATHS[1]=C:\Program Files\Java\jdk1.8.0_271\bin\java.exe
set JAVA_PATHS[2]=C:\Program Files\Java\jdk1.8.0_202\bin\java.exe
set JAVA_PATHS[3]=C:\Program Files (x86)\Java\jdk1.8.0_341\bin\java.exe
set JAVA_PATHS[4]=C:\Program Files (x86)\Java\jdk1.8.0_271\bin\java.exe
set JAVA_PATHS[5]=C:\Program Files (x86)\Java\jdk1.8.0_202\bin\java.exe

setlocal enabledelayedexpansion
for /L %%i in (0,1,5) do (
    call set "CURRENT_PATH=%%JAVA_PATHS[%%i]%%"
    if exist "!CURRENT_PATH!" (
        set JAVA_CMD=!CURRENT_PATH!
        set JAVA_FOUND=1
        echo [INFO] 找到 Java 8: !CURRENT_PATH!
        goto :found_java
    )
)

REM 方法2: 检查 JAVA_HOME 环境变量
if defined JAVA_HOME (
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
    if exist "%JAVA_CMD%" (
        echo [INFO] 使用 JAVA_HOME: %JAVA_HOME%
        set JAVA_FOUND=1
        goto :found_java
    )
)

REM 方法3: 使用系统 PATH 中的 java
where java >nul 2>&1
if not errorlevel 1 (
    set JAVA_CMD=java
    set JAVA_FOUND=1
    echo [INFO] 使用系统 PATH 中的 Java
    goto :found_java
)

REM 如果都没找到
echo [ERROR] 未找到 Java 安装
echo [INFO] 请安装 Java 8 或设置 JAVA_HOME 环境变量
pause
exit /b 1

:found_java
REM 检查 Java 版本
echo [INFO] 检查 Java 版本...
"%JAVA_CMD%" -version 2>&1 | findstr /C:"version" >nul
if errorlevel 1 (
    echo [ERROR] 无法获取 Java 版本信息
    pause
    exit /b 1
)

REM 获取 Java 版本号（简化检查）
"%JAVA_CMD%" -version 2>&1 | findstr /C:"1.8" >nul
if errorlevel 1 (
    "%JAVA_CMD%" -version 2>&1 | findstr /C:"8" >nul
    if errorlevel 1 (
        echo [WARNING] 检测到非 Java 8 版本
        echo [WARNING] 项目要求 Java 8，可能会出现兼容性问题
        echo [INFO] 继续启动...
        timeout /t 3 >nul
    ) else (
        echo [INFO] Java 版本检查通过
    )
) else (
    echo [INFO] Java 版本检查通过 (Java 8)
)

echo.
echo [INFO] 启动配置:
echo   Java: %JAVA_CMD%
echo   JAR文件: %JAR_FILE%
echo   配置文件: %CONFIG_FILE%
echo.

REM 启动应用
echo [INFO] 正在启动 Easy JMeter Server...
echo.

"%JAVA_CMD%" "-Dfile.encoding=UTF-8" "-Dspring.profiles.active=dev" -jar "%JAR_FILE%" "--spring.config.additional-location=file:./%CONFIG_FILE%"

REM 如果启动失败，暂停以便查看错误信息
if errorlevel 1 (
    echo.
    echo [ERROR] 启动失败，请检查错误信息
    pause
)

endlocal
