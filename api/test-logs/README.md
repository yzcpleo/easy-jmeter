# 测试日志文件说明

## 文件列表

本目录包含用于测试链路数据收集功能的模拟日志文件：

1. **gateway.log** - 网关节点日志（20条记录）
2. **order-service.log** - 订单服务节点日志（13条记录）
3. **payment-service.log** - 支付服务节点日志（8条记录）
4. **database.log** - 数据库节点日志（20条记录）

## 日志格式

所有日志文件使用统一的格式：

```
2024-11-19 10:00:00.123 [INFO] Request received: requestId=req_001, path=/api/order, latency=45ms
```

**格式说明**：
- 时间戳：`yyyy-MM-dd HH:mm:ss.SSS`
- 请求ID：`requestId=req_XXX`
- 延时：`latency=XXXms`

## 数据统计

### 请求分布

- **总请求数**：20个（req_001 到 req_020）
- **订单相关请求**：12个（req_001, req_002, req_004, req_006, req_007, req_009, req_011, req_012, req_014, req_016, req_017, req_019）
- **支付相关请求**：8个（req_003, req_005, req_008, req_010, req_013, req_015, req_018, req_020）

### 各节点数据统计

| 节点 | 记录数 | 平均延时 | 延时范围 | 说明 |
|------|--------|----------|----------|------|
| 网关 | 20条 | ~50ms | 36-67ms | 所有请求都经过网关 |
| 订单服务 | 13条 | ~130ms | 118-145ms | 只有订单相关请求 |
| 支付服务 | 8条 | ~90ms | 83-98ms | 只有支付相关请求 |
| 数据库 | 20条 | ~26ms | 21-31ms | 所有请求都访问数据库 |

### 预期统计结果

**整体链路性能**：
- 平均延时：约120-150ms
- P95延时：约250-300ms
- 成功率：100%

**各节点性能**：
- 网关：平均50ms，P95 67ms
- 订单服务：平均130ms，P95 145ms（瓶颈节点）
- 支付服务：平均90ms，P95 98ms
- 数据库：平均26ms，P95 31ms

## 使用说明

### 1. 配置节点日志路径

在系统中配置节点时，使用以下绝对路径：

- 网关：`F:\BaiduNetdiskDownload\easy-jmeter1\easy-jmeter\api\test-logs\gateway.log`
- 订单服务：`F:\BaiduNetdiskDownload\easy-jmeter1\easy-jmeter\api\test-logs\order-service.log`
- 支付服务：`F:\BaiduNetdiskDownload\easy-jmeter1\easy-jmeter\api\test-logs\payment-service.log`
- 数据库：`F:\BaiduNetdiskDownload\easy-jmeter1\easy-jmeter\api\test-logs\database.log`

### 2. 配置日志解析规则

**日志匹配模式**：
- 网关：`Request received`
- 订单服务：`Processing order request`
- 支付服务：`Processing payment request`
- 数据库：`Database query executed`

**时间戳模式**（所有节点相同）：
```
(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})
```

**请求ID模式**（所有节点相同）：
```
requestId=([^,]+)
```

**延时模式**（所有节点相同）：
```
latency=(\d+)ms
```

## 测试流程

1. 配置链路和节点（参考 `docs/链路数据收集测试指南.md`）
2. 启动数据收集
3. 等待5-10秒
4. 查看收集的数据和统计结果
5. 验证各节点耗时和整体耗时统计

## 注意事项

- 日志文件路径必须使用**绝对路径**
- 确保日志文件有读取权限
- 日志解析规则必须正确配置
- 测试完成后可以删除或备份这些日志文件

