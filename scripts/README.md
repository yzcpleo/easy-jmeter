# Easy JMeter 启动脚本使用说明

## 📋 概述

本目录包含Easy JMeter系统的启动和停止脚本，支持Windows和Linux平台，提供Server和Agent两种运行模式。

## 📁 目录结构

```
scripts/
├── windows/                 # Windows平台脚本
│   ├── start-server.bat    # 启动Server服务
│   ├── start-agent.bat     # 启动Agent服务  
│   ├── stop-server.bat     # 停止Server服务
│   └── stop-agent.bat      # 停止Agent服务
├── linux/                  # Linux平台脚本
│   ├── start-server.sh     # 启动Server服务
│   ├── start-agent.sh      # 启动Agent服务
│   ├── stop-server.sh      # 停止Server服务
│   └── stop-agent.sh       # 停止Agent服务
├── quick-start.bat         # Windows快速启动菜单
├── quick-start.sh          # Linux快速启动菜单
├── setup-permissions.sh    # Linux权限设置脚本
└── README.md               # 本说明文档
```

## 🚀 使用方法

### 🎯 快速启动 (推荐新手使用)

#### Windows
```batch
# 运行快速启动菜单
scripts\quick-start.bat
```

#### Linux
```bash
# 首次使用设置权限
chmod +x scripts/quick-start.sh

# 运行快速启动菜单  
./scripts/quick-start.sh
```

快速启动菜单提供：
- ✅ 图形化菜单界面
- 📊 运行状态检查
- 📋 日志查看功能
- 🔧 系统信息显示
- 🚀 一键启动/停止

### 📝 手动使用

### Windows平台

#### 启动Server (管理端)
```batch
# 进入脚本目录
cd scripts\windows

# 启动Server
start-server.bat

# 停止Server
stop-server.bat
```

#### 启动Agent (压力机)
```batch
# 进入脚本目录
cd scripts\windows

# 启动Agent
start-agent.bat

# 停止Agent
stop-agent.bat
```

### Linux平台

#### 首次使用 - 设置执行权限
```bash
# 给所有脚本添加执行权限
chmod +x scripts/linux/*.sh

# 或者单独设置
chmod +x scripts/linux/start-server.sh
chmod +x scripts/linux/start-agent.sh
chmod +x scripts/linux/stop-server.sh
chmod +x scripts/linux/stop-agent.sh
```

#### 启动Server (管理端)
```bash
# 进入脚本目录
cd scripts/linux

# 启动Server
./start-server.sh

# 停止Server
./stop-server.sh

# 强制停止所有相关进程
./stop-server.sh --force
```

#### 启动Agent (压力机)
```bash
# 进入脚本目录  
cd scripts/linux

# 启动Agent
./start-agent.sh

# 停止Agent
./stop-agent.sh

# 强制停止所有Agent进程
./stop-agent.sh --force
```

## ⚙️ 配置说明

### Server配置 (`application-dev.yml`)
- 启动前确保该配置文件存在于项目根目录
- 包含数据库、Redis、MinIO等基础服务配置
- 默认端口：5000 (HTTP API) + 9000 (SocketIO)

### Agent配置 (`application-agent.yml`)
- 首次运行时脚本会自动创建默认配置
- 需要修改的关键配置：
  ```yaml
  socket:
    client:
      serverUrl: http://YOUR_SERVER_IP:9000  # Server地址
  
  jmeter:
    path: /path/to/apache-jmeter  # JMeter安装路径
  ```

## 🔧 系统要求

### 通用要求
- Java 8或更高版本
- 已编译的JAR文件：`api/target/easyJmeter-0.1.0-RELEASE.jar`

### Server额外要求  
- MySQL数据库
- MongoDB数据库
- InfluxDB (可选)
- MinIO对象存储

### Agent额外要求
- Apache JMeter 5.6.2 (推荐版本)
- 充足的系统内存 (推荐4GB+)

## 📊 JVM参数优化

脚本会根据系统内存自动设置JVM参数：

### Server内存分配
| 系统内存 | 堆内存设置 |
|---------|-----------|
| > 4GB   | -Xms2g -Xmx4g |
| > 2GB   | -Xms1g -Xmx2g |
| ≤ 2GB   | -Xms512m -Xmx1g |

### Agent内存分配 (需要更多内存)
| 系统内存 | 堆内存设置 |
|---------|-----------|
| > 8GB   | -Xms4g -Xmx6g |
| > 4GB   | -Xms2g -Xmx4g |
| > 2GB   | -Xms1g -Xmx2g |
| ≤ 2GB   | -Xms512m -Xmx1g |

## 📁 日志文件位置

- **Server日志**: `logs/server/server.log`
- **Agent日志**: `logs/agent/agent.log`
- **GC日志**: `logs/server/gc.log` 或 `logs/agent/gc.log`
- **堆转储**: `logs/*/heapdump.hprof` (OOM时自动生成)

## 🔍 故障排除

### 常见问题

1. **JAR文件不存在**
   ```bash
   # 重新编译项目
   cd api
   mvn clean package -DskipTests
   ```

2. **端口被占用**
   ```bash
   # Linux检查端口占用
   netstat -tlnp | grep :5000
   lsof -i :5000
   
   # Windows检查端口占用  
   netstat -an | findstr :5000
   ```

3. **内存不足**
   - 检查系统可用内存
   - 调整JVM参数 (-Xms -Xmx)
   - 关闭不必要的应用程序

4. **Agent连接失败**
   - 检查Server是否正常运行
   - 验证网络连通性：`telnet SERVER_IP 9000`
   - 检查防火墙设置
   - 确认配置文件中的serverUrl正确

5. **JMeter路径错误**
   - 验证JMeter安装：`/path/to/jmeter/bin/jmeter -v`
   - 检查配置文件中的jmeter.path
   - 确保JMeter可执行文件有执行权限

### 手动检查进程

```bash
# Linux查看Easy JMeter相关进程
ps aux | grep easyJmeter

# Windows查看Java进程
tasklist | findstr java.exe

# 查看进程详细信息
# Linux
ps -ef | grep java
# Windows  
wmic process where "name='java.exe'" get commandline
```

### 日志分析

```bash
# 查看实时日志
tail -f logs/server/server.log
tail -f logs/agent/agent.log

# 搜索错误信息
grep -i error logs/server/server.log
grep -i exception logs/server/server.log

# 查看启动日志
head -50 logs/server/server.log
```

## 🛡️ 生产环境建议

1. **系统服务**：将脚本注册为系统服务，实现开机自启
2. **监控告警**：配置进程监控和资源告警
3. **日志轮转**：配置logrotate避免日志文件过大
4. **定期维护**：定期重启清理内存，检查磁盘空间
5. **安全设置**：配置防火墙，使用非root用户运行

## 📞 技术支持

如遇到问题，请检查：
1. 系统要求是否满足
2. 配置文件是否正确
3. 网络连接是否正常
4. 日志文件中的错误信息
5. JVM内存和系统资源

---

**祝您使用愉快！** 🎉
