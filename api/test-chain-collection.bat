@echo off
chcp 65001 >nul
REM =================================
REM 链路数据收集功能测试脚本
REM =================================

echo.
echo ========================================
echo   链路数据收集功能测试
echo ========================================
echo.

set BASE_URL=http://localhost:5000
set TASK_ID=1
set CHAIN_ID=1

echo [INFO] 测试配置:
echo [INFO]   后端地址: %BASE_URL%
echo [INFO]   任务ID: %TASK_ID%
echo [INFO]   链路ID: %CHAIN_ID%
echo.

REM 检查后端是否启动
echo [INFO] 检查后端服务是否启动...
curl -s %BASE_URL%/v1/chain/trace/page?page=0^&count=1 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 后端服务未启动，请先启动后端服务
    echo [INFO] 运行: cd api ^&^& start-dev.cmd
    pause
    exit /b 1
)
echo [INFO] 后端服务已启动
echo.

REM 测试1: 启动数据收集
echo ========================================
echo 测试1: 启动数据收集
echo ========================================
echo [INFO] 启动数据收集任务...
curl -X POST "%BASE_URL%/v1/chain/collection/start/%TASK_ID%/%CHAIN_ID%" ^
    -H "Content-Type: application/json" ^
    -H "Authorization: Bearer YOUR_TOKEN" 2>nul
echo.
echo.

REM 等待5秒，让数据收集执行
echo [INFO] 等待5秒，让数据收集执行...
timeout /t 5 /nobreak >nul
echo.

REM 测试2: 查询收集状态
echo ========================================
echo 测试2: 查询收集状态
echo ========================================
echo [INFO] 查询数据收集状态...
curl -s "%BASE_URL%/v1/chain/collection/status/%TASK_ID%/%CHAIN_ID%" ^
    -H "Authorization: Bearer YOUR_TOKEN" 2>nul
echo.
echo.

REM 测试3: 查询收集的数据
echo ========================================
echo 测试3: 查询收集的数据
echo ========================================
echo [INFO] 查询收集的延时数据...
curl -s "%BASE_URL%/v1/chain/collection/data?chainId=%CHAIN_ID%^&taskId=%TASK_ID%^&page=0^&count=10" ^
    -H "Authorization: Bearer YOUR_TOKEN" 2>nul
echo.
echo.

REM 测试4: 查询链路性能统计
echo ========================================
echo 测试4: 查询链路性能统计
echo ========================================
set START_TIME=2024-11-19 10:00:00
set END_TIME=2024-11-19 10:20:00
echo [INFO] 查询链路性能统计...
echo [INFO] 时间范围: %START_TIME% 至 %END_TIME%
curl -s "%BASE_URL%/v1/chain/collection/chain-stats/%CHAIN_ID%?startTime=%START_TIME%^&endTime=%END_TIME%" ^
    -H "Authorization: Bearer YOUR_TOKEN" 2>nul
echo.
echo.

REM 测试5: 查询节点性能对比
echo ========================================
echo 测试5: 查询节点性能对比
echo ========================================
echo [INFO] 查询节点性能对比...
curl -s "%BASE_URL%/v1/chain/collection/node-performance?chainId=%CHAIN_ID%^&startTime=%START_TIME%^&endTime=%END_TIME%" ^
    -H "Authorization: Bearer YOUR_TOKEN" 2>nul
echo.
echo.

REM 测试6: 停止数据收集
echo ========================================
echo 测试6: 停止数据收集
echo ========================================
echo [INFO] 停止数据收集任务...
curl -X POST "%BASE_URL%/v1/chain/collection/stop/%TASK_ID%/%CHAIN_ID%" ^
    -H "Content-Type: application/json" ^
    -H "Authorization: Bearer YOUR_TOKEN" 2>nul
echo.
echo.

echo ========================================
echo 测试完成
echo ========================================
echo.
echo [INFO] 请检查上述输出，确认功能是否正常
echo [INFO] 如果看到数据，说明数据收集功能正常
echo.

pause

