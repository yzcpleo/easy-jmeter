# API测试脚本
$baseUrl = "http://localhost:5000"

# 1. 测试登录
Write-Host "`n=== 测试登录 ===" -ForegroundColor Green
$loginBody = @{
    username = "root"
    password = "123456"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/cms/user/login" -Method POST -ContentType "application/json" -Body $loginBody
    $token = $loginResponse.access_token
    Write-Host "登录成功！Token: $($token.Substring(0, [Math]::Min(30, $token.Length)))..." -ForegroundColor Green
    
    $headers = @{
        Authorization = "Bearer $token"
    }
    
    # 2. 测试获取链路列表
    Write-Host "`n=== 测试获取链路列表 ===" -ForegroundColor Green
    try {
        $chainListResponse = Invoke-RestMethod -Uri "$baseUrl/v1/chain/trace/page?page=0&count=10" -Method GET -Headers $headers
        Write-Host "获取链路列表成功！总数: $($chainListResponse.total)" -ForegroundColor Green
        
        if ($chainListResponse.items -and $chainListResponse.items.Count -gt 0) {
            $chainId = $chainListResponse.items[0].id
            $chainName = $chainListResponse.items[0].chain_name
            Write-Host "使用链路: $chainName (ID: $chainId)" -ForegroundColor Cyan
            
            # 3. 测试获取链路统计
            Write-Host "`n=== 测试获取链路统计 ===" -ForegroundColor Green
            $startTime = (Get-Date).AddHours(-1).ToString("yyyy-MM-dd HH:mm:ss")
            $endTime = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            
            try {
                $statsResponse = Invoke-RestMethod -Uri "$baseUrl/v1/chain/collection/chain-stats/$chainId" -Method GET -Headers $headers -Body @{
                    startTime = $startTime
                    endTime = $endTime
                }
                Write-Host "获取链路统计成功！" -ForegroundColor Green
                Write-Host "  平均延时: $($statsResponse.avgLatency)ms" -ForegroundColor Yellow
                Write-Host "  总请求数: $($statsResponse.totalRequests)" -ForegroundColor Yellow
                Write-Host "  成功率: $($statsResponse.successRate)%" -ForegroundColor Yellow
            } catch {
                Write-Host "获取链路统计失败: $($_.Exception.Message)" -ForegroundColor Red
            }
            
            # 4. 测试获取延时趋势
            Write-Host "`n=== 测试获取延时趋势 ===" -ForegroundColor Green
            try {
                $trendResponse = Invoke-RestMethod -Uri "$baseUrl/v1/chain/collection/latency-trend" -Method GET -Headers $headers -Body @{
                    chainId = $chainId
                    startTime = $startTime
                    endTime = $endTime
                    granularity = "5minute"
                }
                Write-Host "获取延时趋势成功！数据点数量: $($trendResponse.Count)" -ForegroundColor Green
                if ($trendResponse.Count -gt 0) {
                    Write-Host "  第一个数据点: 时间=$($trendResponse[0].time), 平均延时=$($trendResponse[0].avgLatency)ms" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "获取延时趋势失败: $($_.Exception.Message)" -ForegroundColor Red
            }
            
            # 5. 测试获取节点性能
            Write-Host "`n=== 测试获取节点性能 ===" -ForegroundColor Green
            try {
                $nodePerfResponse = Invoke-RestMethod -Uri "$baseUrl/v1/chain/collection/node-performance" -Method GET -Headers $headers -Body @{
                    chainId = $chainId
                    startTime = $startTime
                    endTime = $endTime
                }
                Write-Host "获取节点性能成功！节点数量: $($nodePerfResponse.Count)" -ForegroundColor Green
                if ($nodePerfResponse.Count -gt 0) {
                    Write-Host "  第一个节点: $($nodePerfResponse[0].nodeName), 平均延时=$($nodePerfResponse[0].avgLatency)ms" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "获取节点性能失败: $($_.Exception.Message)" -ForegroundColor Red
            }
            
        } else {
            Write-Host "没有链路数据，跳过后续测试" -ForegroundColor Yellow
        }
        
    } catch {
        Write-Host "获取链路列表失败: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "登录失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "请确保服务已启动并且数据库连接正常" -ForegroundColor Yellow
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green

