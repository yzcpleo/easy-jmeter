@echo off
REM =================================
REM Easy JMeter 快速启动脚本 (Windows)
REM =================================

echo.
echo ========================================
echo   Easy JMeter 快速启动菜单
echo ========================================
echo.

echo 请选择要启动的服务:
echo.
echo   1. 启动 Server (管理端)
echo   2. 启动 Agent (压力机)
echo   3. 停止 Server
echo   4. 停止 Agent
echo   5. 查看运行状态
echo   6. 退出
echo.

set /p choice="请输入选项 (1-6): "

if "%choice%"=="1" (
    echo.
    echo 启动 Easy JMeter Server...
    call "%~dp0windows\start-server.bat"
) else if "%choice%"=="2" (
    echo.
    echo 启动 Easy JMeter Agent...
    call "%~dp0windows\start-agent.bat"
) else if "%choice%"=="3" (
    echo.
    echo 停止 Easy JMeter Server...
    call "%~dp0windows\stop-server.bat"
) else if "%choice%"=="4" (
    echo.
    echo 停止 Easy JMeter Agent...
    call "%~dp0windows\stop-agent.bat"
) else if "%choice%"=="5" (
    echo.
    echo 检查运行状态...
    echo.
    echo Server进程:
    tasklist /FI "IMAGENAME eq java.exe" /FI "WINDOWTITLE eq Easy JMeter Server" /FO TABLE 2>nul | find /V "INFO:"
    if errorlevel 1 echo   未找到Server进程
    echo.
    echo Agent进程:
    tasklist /FI "IMAGENAME eq java.exe" /FI "WINDOWTITLE eq Easy JMeter Agent" /FO TABLE 2>nul | find /V "INFO:"
    if errorlevel 1 echo   未找到Agent进程
    echo.
    echo Java进程概览:
    tasklist /FI "IMAGENAME eq java.exe" /FO TABLE
    echo.
    pause
    goto :start
) else if "%choice%"=="6" (
    echo 再见！
    exit /b 0
) else (
    echo.
    echo 无效的选项，请重新选择。
    timeout /t 2 /nobreak >nul
    goto :start
)

:start
cls
goto :EOF
