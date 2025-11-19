# 链路数据收集功能测试脚本 (PowerShell版本)
# 使用说明: 在PowerShell中运行: .\test-chain-collection.ps1

$ErrorActionPreference = "Stop"

$BASE_URL = "http://localhost:5000"
$TASK_ID = 1
$CHAIN_ID = 1
$START_TIME = "2024-11-19 10:00:00"
$END_TIME = "2024-11-19 10:20:00"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   链路数据收集功能测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[INFO] 测试配置:" -ForegroundColor Yellow
Write-Host "[INFO]   后端地址: $BASE_URL" -ForegroundColor Gray
Write-Host "[INFO]   任务ID: $TASK_ID" -ForegroundColor Gray
Write-Host "[INFO]   链路ID: $CHAIN_ID" -ForegroundColor Gray
Write-Host ""

# 检查后端是否启动
Write-Host "[INFO] 检查后端服务是否启动..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/v1/chain/trace/page?page=0&count=1" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
    Write-Host "[INFO] 后端服务已启动" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] 后端服务未启动，请先启动后端服务" -ForegroundColor Red
    Write-Host "[INFO] 运行: cd api && start-dev.cmd" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# 测试1: 启动数据收集
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试1: 启动数据收集" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[INFO] 启动数据收集任务..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/v1/chain/collection/start/$TASK_ID/$CHAIN_ID" -Method POST -ContentType "application/json"
    Write-Host "[SUCCESS] 数据收集已启动" -ForegroundColor Green
    Write-Host "响应: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] 启动失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 等待5秒，让数据收集执行
Write-Host "[INFO] 等待5秒，让数据收集执行..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
Write-Host ""

# 测试2: 查询收集状态
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试2: 查询收集状态" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[INFO] 查询数据收集状态..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/v1/chain/collection/status/$TASK_ID/$CHAIN_ID" -Method GET
    Write-Host "[SUCCESS] 查询成功" -ForegroundColor Green
    Write-Host "状态: $($response.status)" -ForegroundColor Gray
    Write-Host "总记录数: $($response.totalRecords)" -ForegroundColor Gray
    Write-Host "成功记录数: $($response.successRecords)" -ForegroundColor Gray
    Write-Host "错误记录数: $($response.errorRecords)" -ForegroundColor Gray
    Write-Host "响应: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] 查询失败: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "[INFO] 任务不存在，这是正常的（如果任务未启动）" -ForegroundColor Yellow
    }
}
Write-Host ""

# 测试3: 查询收集的数据
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试3: 查询收集的数据" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[INFO] 查询收集的延时数据..." -ForegroundColor Yellow
try {
    $params = @{
        chainId = $CHAIN_ID
        taskId = $TASK_ID
        page = 0
        count = 10
    }
    $queryString = ($params.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "&"
    $response = Invoke-RestMethod -Uri "$BASE_URL/v1/chain/collection/data?$queryString" -Method GET
    Write-Host "[SUCCESS] 查询成功" -ForegroundColor Green
    Write-Host "总记录数: $($response.total)" -ForegroundColor Gray
    Write-Host "返回记录数: $($response.items.Count)" -ForegroundColor Gray
    if ($response.items.Count -gt 0) {
        Write-Host "前3条记录:" -ForegroundColor Gray
        $response.items[0..2] | ForEach-Object {
            Write-Host "  - 请求ID: $($_.requestId), 节点: $($_.nodeName), 延时: $($_.latency)ms" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "[ERROR] 查询失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试4: 查询链路性能统计
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试4: 查询链路性能统计" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[INFO] 查询链路性能统计..." -ForegroundColor Yellow
Write-Host "[INFO] 时间范围: $START_TIME 至 $END_TIME" -ForegroundColor Gray
try {
    $params = @{
        startTime = $START_TIME
        endTime = $END_TIME
    }
    $queryString = ($params.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Web.HttpUtility]::UrlEncode($_.Value))" }) -join "&"
    $response = Invoke-RestMethod -Uri "$BASE_URL/v1/chain/collection/chain-stats/$CHAIN_ID?$queryString" -Method GET
    Write-Host "[SUCCESS] 查询成功" -ForegroundColor Green
    Write-Host "链路名称: $($response.chainName)" -ForegroundColor Gray
    Write-Host "总请求数: $($response.totalCount)" -ForegroundColor Gray
    Write-Host "平均延时: $($response.avgLatency)ms" -ForegroundColor Gray
    Write-Host "P95延时: $($response.p95Latency)ms" -ForegroundColor Gray
    Write-Host "成功率: $($response.successRate)%" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] 查询失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试5: 查询节点性能对比
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试5: 查询节点性能对比" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[INFO] 查询节点性能对比..." -ForegroundColor Yellow
try {
    $params = @{
        chainId = $CHAIN_ID
        startTime = $START_TIME
        endTime = $END_TIME
    }
    $queryString = ($params.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Web.HttpUtility]::UrlEncode($_.Value))" }) -join "&"
    $response = Invoke-RestMethod -Uri "$BASE_URL/v1/chain/collection/node-performance?$queryString" -Method GET
    Write-Host "[SUCCESS] 查询成功" -ForegroundColor Green
    Write-Host "节点数量: $($response.Count)" -ForegroundColor Gray
    $response | ForEach-Object {
        Write-Host "节点: $($_.nodeName), 平均延时: $($_.avgLatency)ms, P95: $($_.p95Latency)ms, 错误率: $($_.errorRate)%" -ForegroundColor Gray
    }
} catch {
    Write-Host "[ERROR] 查询失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试6: 停止数据收集
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试6: 停止数据收集" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[INFO] 停止数据收集任务..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/v1/chain/collection/stop/$TASK_ID/$CHAIN_ID" -Method POST -ContentType "application/json"
    Write-Host "[SUCCESS] 数据收集已停止" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] 停止失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[INFO] 请检查上述输出，确认功能是否正常" -ForegroundColor Yellow
Write-Host "[INFO] 如果看到数据，说明数据收集功能正常" -ForegroundColor Yellow
Write-Host ""

