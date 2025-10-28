@echo off
REM =================================
REM Easy JMeter Agent 停止脚本 (Windows)
REM =================================

echo.
echo ========================================
echo   停止 Easy JMeter Agent
echo ========================================
echo.

REM 设置变量
set APP_HOME=%~dp0..\..
set PID_FILE=%APP_HOME%\agent.pid

REM 检查PID文件是否存在
if not exist "%PID_FILE%" (
    echo [INFO] 未找到PID文件，尝试查找Agent进程...
    goto :FIND_PROCESS
)

REM 读取PID
set /p AGENT_PID=<"%PID_FILE%"

REM 检查进程是否存在
tasklist /FI "PID eq %AGENT_PID%" 2>nul | find /I "java.exe" >nul
if errorlevel 1 (
    echo [INFO] 进程 %AGENT_PID% 不存在，清理PID文件...
    del "%PID_FILE%" 2>nul
    echo [INFO] Easy JMeter Agent 未运行
    goto :END
)

echo [INFO] 找到Agent进程 PID: %AGENT_PID%
echo [INFO] 正在停止进程...

REM 优雅停止（发送关闭信号）
taskkill /PID %AGENT_PID% /T >nul 2>&1

REM 等待进程结束
set /a WAIT_COUNT=0
:WAIT_LOOP
timeout /t 1 /nobreak >nul
set /a WAIT_COUNT+=1

tasklist /FI "PID eq %AGENT_PID%" 2>nul | find /I "java.exe" >nul
if errorlevel 1 (
    echo [SUCCESS] Easy JMeter Agent 已停止
    del "%PID_FILE%" 2>nul
    goto :END
)

if %WAIT_COUNT% lss 10 goto :WAIT_LOOP

REM 如果10秒后还未停止，强制结束
echo [WARNING] 进程未响应，执行强制停止...
taskkill /F /PID %AGENT_PID% /T >nul 2>&1

REM 再次检查
timeout /t 2 /nobreak >nul
tasklist /FI "PID eq %AGENT_PID%" 2>nul | find /I "java.exe" >nul
if errorlevel 1 (
    echo [SUCCESS] Easy JMeter Agent 已强制停止
    del "%PID_FILE%" 2>nul
) else (
    echo [ERROR] 无法停止进程 %AGENT_PID%，请手动处理
    goto :END
)

goto :END

:FIND_PROCESS
REM 尝试通过窗口标题查找进程
for /f "skip=1 tokens=2" %%i in ('tasklist /FI "WINDOWTITLE eq Easy JMeter Agent" /FO CSV 2^>nul ^| find /V "PID"') do (
    set FOUND_PID=%%i
    echo [INFO] 通过窗口标题找到Agent进程: %FOUND_PID%
    taskkill /PID %FOUND_PID% /T >nul 2>&1
    echo [SUCCESS] Easy JMeter Agent 已停止
    goto :END
)

REM 尝试通过命令行参数查找
echo [INFO] 搜索包含 socket.client.enable=true 的Java进程...
for /f "tokens=2" %%i in ('wmic process where "CommandLine like '%%socket.client.enable=true%%'" get ProcessId /format:csv 2^>nul ^| find ","') do (
    set FOUND_PID=%%i
    if defined FOUND_PID (
        echo [INFO] 找到Agent进程: %FOUND_PID%
        taskkill /PID %FOUND_PID% /T >nul 2>&1
        echo [SUCCESS] Easy JMeter Agent 已停止
        goto :END
    )
)

echo [INFO] 未找到运行中的Easy JMeter Agent进程

:END
echo.
pause
