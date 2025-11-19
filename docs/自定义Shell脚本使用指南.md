# 自定义Shell脚本使用指南

## 概述

对于非标准格式的日志，系统支持使用自定义Shell脚本来解析日志。这种方式提供了最大的灵活性，允许用户根据自己的日志格式编写解析脚本。

## 适用场景

自定义Shell脚本适用于以下场景：

1. **非标准格式的日志**
   - 日志格式不固定，无法用简单的正则表达式匹配
   - 需要复杂的逻辑处理才能提取数据

2. **多行日志**
   - 一条完整的日志记录可能跨越多行
   - 需要上下文信息才能正确解析

3. **特殊编码或格式**
   - 日志包含特殊字符或编码
   - 需要预处理才能解析

4. **性能优化**
   - 对于大型日志文件，自定义脚本可能比AWK更高效
   - 可以利用系统工具（如grep、sed、awk等）组合处理

## 使用限制

- **仅支持远程Linux节点**：自定义脚本通过SSH在远程服务器上执行
- **需要SSH访问权限**：必须能够SSH连接到远程服务器
- **脚本必须是bash脚本**：系统使用bash执行脚本

## 脚本编写规范

### 输出格式

脚本必须按照以下格式输出解析结果：

```
requestId|timestamp|latency|originalLine
```

**字段说明**：
- `requestId`：请求ID（如果无法提取，输出空字符串）
- `timestamp`：时间戳（格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd HH:mm:ss.SSS）
- `latency`：延时值（单位：毫秒，如果无法提取，输出0或空字符串）
- `originalLine`：原始日志行（可选，用于调试）

**示例输出**：
```
req_001|2024-11-19 10:00:00.123|45|2024-11-19 10:00:00.123 [INFO] Request received: requestId=req_001, latency=45ms
req_002|2024-11-19 10:00:01.234|52|2024-11-19 10:00:01.234 [INFO] Request received: requestId=req_002, latency=52ms
```

### 可用变量

脚本中可以使用以下变量：

- `$LOG_PATH`：日志文件路径（由系统自动替换）

### 脚本示例

#### 示例1：标准格式日志解析

```bash
#!/bin/bash
# 解析标准格式日志：2024-11-19 10:00:00.123 [INFO] Request received: requestId=req_001, latency=45ms

while IFS= read -r line; do
    # 提取请求ID
    request_id=$(echo "$line" | grep -oP 'requestId=\K[^,]+' || echo "")
    
    # 提取时间戳
    timestamp=$(echo "$line" | grep -oP '\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}' || echo "")
    
    # 提取延时
    latency=$(echo "$line" | grep -oP 'latency=\K\d+' || echo "0")
    
    # 输出结果
    if [ -n "$request_id" ] || [ -n "$timestamp" ] || [ -n "$latency" ]; then
        echo "$request_id|$timestamp|$latency|$line"
    fi
done < "$LOG_PATH"
```

#### 示例2：JSON格式日志解析

```bash
#!/bin/bash
# 解析JSON格式日志：{"timestamp":"2024-11-19 10:00:00.123","requestId":"req_001","latency":45}

while IFS= read -r line; do
    # 使用jq解析JSON（需要安装jq工具）
    if command -v jq &> /dev/null; then
        request_id=$(echo "$line" | jq -r '.requestId // ""')
        timestamp=$(echo "$line" | jq -r '.timestamp // ""')
        latency=$(echo "$line" | jq -r '.latency // 0')
        echo "$request_id|$timestamp|$latency|$line"
    else
        # 如果没有jq，使用grep和sed解析
        request_id=$(echo "$line" | grep -oP '"requestId"\s*:\s*"\K[^"]+' || echo "")
        timestamp=$(echo "$line" | grep -oP '"timestamp"\s*:\s*"\K[^"]+' || echo "")
        latency=$(echo "$line" | grep -oP '"latency"\s*:\s*\K\d+' || echo "0")
        echo "$request_id|$timestamp|$latency|$line"
    fi
done < "$LOG_PATH"
```

#### 示例3：多行日志解析

```bash
#!/bin/bash
# 解析多行日志，每条日志以空行分隔

buffer=""
while IFS= read -r line || [ -n "$line" ]; do
    if [ -z "$line" ]; then
        # 空行表示一条完整日志结束，处理buffer
        if [ -n "$buffer" ]; then
            # 从buffer中提取信息
            request_id=$(echo "$buffer" | grep -oP 'requestId=\K[^,]+' || echo "")
            timestamp=$(echo "$buffer" | head -1 | grep -oP '\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}' || echo "")
            latency=$(echo "$buffer" | grep -oP 'latency=\K\d+' || echo "0")
            echo "$request_id|$timestamp|$latency|$buffer"
            buffer=""
        fi
    else
        # 累积到buffer
        if [ -z "$buffer" ]; then
            buffer="$line"
        else
            buffer="$buffer\n$line"
        fi
    fi
done < "$LOG_PATH"

# 处理最后一条日志
if [ -n "$buffer" ]; then
    request_id=$(echo "$buffer" | grep -oP 'requestId=\K[^,]+' || echo "")
    timestamp=$(echo "$buffer" | head -1 | grep -oP '\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}' || echo "")
    latency=$(echo "$buffer" | grep -oP 'latency=\K\d+' || echo "0")
    echo "$request_id|$timestamp|$latency|$buffer"
fi
```

#### 示例4：使用AWK解析

```bash
#!/bin/bash
# 使用AWK解析日志

awk -v log_path="$LOG_PATH" '
BEGIN {
    FS = " ";
    OFS = "|";
}
/Request received/ {
    request_id = "";
    timestamp = "";
    latency = "";
    
    # 提取时间戳
    if (match($0, /[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}/)) {
        timestamp = substr($0, RSTART, RLENGTH);
    }
    
    # 提取请求ID
    if (match($0, /requestId=[^,]+/)) {
        request_id = substr($0, RSTART + 10, RLENGTH - 10);
    }
    
    # 提取延时
    if (match($0, /latency=[0-9]+/)) {
        latency = substr($0, RSTART + 7, RLENGTH - 7);
    }
    
    if (request_id != "" || timestamp != "" || latency != "") {
        print request_id, timestamp, latency, $0;
    }
}
' "$LOG_PATH"
```

## 配置步骤

### 1. 创建或编辑节点

1. 进入 **链路延时监控** → **链路管理** → **节点配置**
2. 选择 **远程Linux节点（SSH）**
3. 填写SSH连接信息（主机地址、端口、用户名、密码等）

### 2. 选择解析方式

在节点配置表单中：
1. 选择 **解析方式** → **使用自定义Shell脚本**
2. 在 **自定义Shell脚本** 文本框中输入脚本内容

### 3. 编写脚本

参考上面的脚本示例，编写适合自己日志格式的脚本。

**注意事项**：
- 脚本中必须使用 `$LOG_PATH` 变量表示日志文件路径
- 输出格式必须严格按照：`requestId|timestamp|latency|originalLine`
- 如果某个字段无法提取，输出空字符串（不要输出null或undefined）

### 4. 测试脚本

在提交配置前，建议先在远程服务器上测试脚本：

```bash
# 在远程服务器上测试
export LOG_PATH="/var/log/app/app.log"
# 粘贴你的脚本内容并执行
```

## 调试技巧

### 1. 查看脚本执行日志

系统会在日志中记录脚本执行情况：
- 成功：输出记录数
- 失败：错误输出和退出码

### 2. 测试脚本输出格式

在远程服务器上手动执行脚本，检查输出格式：

```bash
# 设置日志路径
export LOG_PATH="/var/log/app/app.log"

# 执行脚本（粘贴脚本内容）
bash << 'SCRIPT_EOF'
# 你的脚本内容
SCRIPT_EOF
```

### 3. 验证字段提取

确保脚本能正确提取所有字段：
- 请求ID：必须唯一标识一个请求
- 时间戳：格式正确，能被系统解析
- 延时：数值格式，单位为毫秒

## 常见问题

### Q1: 脚本执行失败怎么办？

**检查清单**：
1. ✅ 脚本语法是否正确？
2. ✅ 日志文件路径是否正确？
3. ✅ 远程服务器是否有执行权限？
4. ✅ 脚本中使用的工具是否已安装（如jq、grep等）？

**解决方法**：
- 查看系统日志获取详细错误信息
- 在远程服务器上手动执行脚本测试
- 检查脚本中的命令是否在远程服务器上可用

### Q2: 输出格式不正确？

**检查清单**：
1. ✅ 是否严格按照 `requestId|timestamp|latency|originalLine` 格式？
2. ✅ 字段之间是否使用管道符 `|` 分隔？
3. ✅ 是否有多余的空格或换行？

**解决方法**：
- 使用 `echo` 命令输出，避免使用 `printf`（可能引入额外字符）
- 确保字段值中不包含管道符 `|`
- 使用 `trim()` 去除首尾空格

### Q3: 脚本执行超时？

**可能原因**：
- 日志文件过大
- 脚本逻辑复杂，执行时间长
- 网络延迟

**解决方法**：
- 优化脚本逻辑，提高执行效率
- 只处理最近的日志（使用tail命令）
- 增加读取超时时间配置

### Q4: 如何调试脚本？

**方法1：在远程服务器上直接测试**
```bash
export LOG_PATH="/var/log/app/app.log"
# 粘贴脚本并执行
```

**方法2：添加调试输出**
```bash
# 在脚本中添加调试信息（输出到stderr，不会影响解析）
echo "Processing line: $line" >&2
```

**方法3：查看系统日志**
- 系统会记录脚本执行的错误输出
- 查看应用日志获取详细信息

## 最佳实践

### 1. 脚本简洁性

- 保持脚本简洁，避免复杂的逻辑
- 优先使用系统工具（grep、sed、awk等）
- 避免在脚本中执行耗时操作

### 2. 错误处理

- 对可能失败的操作添加错误处理
- 使用 `|| echo ""` 处理提取失败的情况
- 确保脚本在任何情况下都能正常退出

### 3. 性能优化

- 对于大型日志文件，考虑使用流式处理
- 避免将整个日志文件加载到内存
- 使用高效的工具（如awk）而不是多次调用grep

### 4. 可维护性

- 添加注释说明脚本逻辑
- 使用有意义的变量名
- 保持代码格式整洁

## 总结

自定义Shell脚本功能提供了最大的灵活性，可以处理各种非标准格式的日志。正确使用后，可以轻松解析复杂的日志格式，提取所需的延时数据。

**关键要点**：
- 仅支持远程Linux节点
- 输出格式必须严格按照规范
- 使用 `$LOG_PATH` 变量表示日志路径
- 在提交前充分测试脚本

