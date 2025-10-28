# Agent机器配置指南

## 📋 概述

Easy JMeter 性能测试平台采用分布式架构，由以下组件组成：
- **Server（服务端）**：提供Web管理界面和API服务
- **Agent（压力机）**：执行JMeter性能测试任务
- **数据库**：MySQL、MongoDB、InfluxDB等存储服务

本指南将帮助您配置和添加Agent压力机到系统中。

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Frontend  │    │     Server      │    │   Agent机器1    │
│   (Vue.js)      │◄───┤   (Spring Boot) │◄───┤   (JMeter)      │
└─────────────────┘    │                 │    └─────────────────┘
                       │                 │    ┌─────────────────┐
                       │                 │◄───┤   Agent机器2    │
                       │                 │    │   (JMeter)      │
                       └─────────────────┘    └─────────────────┘
```

## 🛠️ Agent机器环境要求

### 硬件要求
- **CPU**: 2核心以上推荐
- **内存**: 4GB以上推荐
- **磁盘**: 10GB以上可用空间
- **网络**: 能够访问Server服务器

### 软件要求
- **操作系统**: Windows/Linux/macOS
- **Java**: JDK 8 或 OpenJDK 8
- **JMeter**: Apache JMeter 5.6.2 (推荐版本)

## 📦 方式一：Docker部署Agent (推荐)

### 1. 准备Docker环境

确保已安装Docker和Docker Compose：
```bash
# 检查Docker版本
docker --version
docker-compose --version
```

### 2. 准备JMeter

下载并解压Apache JMeter 5.6.2：
```bash
# Linux/macOS
cd /opt
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.2.tgz
tar -xzf apache-jmeter-5.6.2.tgz

# Windows (使用PowerShell)
# 下载到 C:\Program Files\ 目录
```

### 3. 创建Agent配置

创建 `docker-compose-agent.yml` 文件：

```yaml
version: "3"

services:
  agent:
    image: guojiaxing1995/easy-jmeter-api:v1
    container_name: agent-machine-1
    restart: always
    command: ["agent", "prod"]
    environment:
      - INFLUXDB_HOST=<SERVER_IP>  # 替换为Server服务器IP
      - INFLUXDB_PORT=8086
      - SERVER_HOST=<SERVER_IP>    # 替换为Server服务器IP
      - SERVER_PORT=9000           # SocketIO端口
    networks:
      - ej
    volumes:
      - ./logs/agent/:/opt/logs/
      - /opt/apache-jmeter-5.6.2:/opt/apache-jmeter  # JMeter路径映射
      
networks:
  ej:
    external: true  # 如果需要连接到已存在的网络
```

### 4. 启动Agent

```bash
# 启动Agent容器
docker-compose -f docker-compose-agent.yml up -d

# 查看日志
docker-compose -f docker-compose-agent.yml logs -f
```

## 🖥️ 方式二：直接部署Agent

### 1. 准备环境

```bash
# 检查Java版本
java -version

# 应该显示Java 8相关信息
```

### 2. 下载应用程序

从项目中复制编译好的JAR文件：
```bash
# 复制JAR文件到Agent机器
cp api/target/easyJmeter-0.1.0-RELEASE.jar /opt/easy-jmeter/
```

### 3. 创建配置文件

创建 `application-agent.yml`：

```yaml
server:
  port: 5000

spring:
  application:
    name: easy-jmeter-agent
  profiles:
    active: prod

# SocketIO客户端配置 - 连接到Server
socket:
  client:
    serverUrl: http://<SERVER_IP>:9000  # 替换为Server服务器地址
    enable: true
  server:
    enable: false

# InfluxDB配置 - 用于存储性能数据  
spring:
  influx:
    url: http://<SERVER_IP>:8086  # 替换为InfluxDB地址
    user: root
    password: root
    database: easyJmeter

# JMeter配置
jmeter:
  path: /opt/apache-jmeter-5.6.2  # JMeter安装路径

logging:
  level:
    io.github.guojiaxing1995.easyJmeter: INFO
  file:
    name: /opt/easy-jmeter/logs/agent.log
```

### 4. 启动Agent

```bash
# 启动Agent服务
java -Dfile.encoding=UTF-8 \
     -Dsocket.server.enable=false \
     -Dsocket.client.enable=true \
     -jar easyJmeter-0.1.0-RELEASE.jar \
     --spring.config.location=application-agent.yml
```

### 5. 设置为系统服务 (可选)

**Linux systemd服务**：
```bash
# 创建服务文件
sudo vim /etc/systemd/system/easy-jmeter-agent.service
```

```ini
[Unit]
Description=Easy JMeter Agent
After=network.target

[Service]
Type=simple
User=jmeter
WorkingDirectory=/opt/easy-jmeter
ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -Dsocket.server.enable=false -Dsocket.client.enable=true -jar easyJmeter-0.1.0-RELEASE.jar --spring.config.location=application-agent.yml
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启用并启动服务
sudo systemctl enable easy-jmeter-agent
sudo systemctl start easy-jmeter-agent
sudo systemctl status easy-jmeter-agent
```

## 🌐 在Web界面中添加Agent机器

### 1. 登录系统

访问: `http://<SERVER_IP>:3000`
- 用户名: admin
- 密码: admin (如已设置)

### 2. 添加压力机

1. 进入 **压力机管理** 页面
2. 点击 **添加压力机** 按钮
3. 填写以下信息：
   - **机器名称**: 如 "Agent-01"、"生产环境压力机-1" 
   - **机器地址**: Agent机器的IP地址，如 "192.168.1.100"

### 3. 验证连接

添加成功后，可以在压力机列表中看到：
- **在线状态**: 绿色表示在线，红色表示离线
- **JMeter状态**: IDLE表示空闲可用
- **版本信息**: 显示JMeter版本号

## 🔧 故障排除

### 1. Agent无法连接到Server

**检查网络连通性**：
```bash
# 测试SocketIO端口连通性
telnet <SERVER_IP> 9000

# 测试InfluxDB连通性
curl http://<SERVER_IP>:8086/ping
```

**检查Agent日志**：
```bash
# Docker方式
docker-compose logs -f agent

# 直接部署方式
tail -f /opt/easy-jmeter/logs/agent.log
```

### 2. JMeter路径配置错误

**错误信息**: `Cannot run program "null\bin\jmeter"`

**解决方案**：
1. 确保JMeter正确安装在指定路径
2. 检查JMeter可执行文件权限
3. 验证环境变量配置

```bash
# 测试JMeter是否正常
/opt/apache-jmeter-5.6.2/bin/jmeter -v
```

### 3. 内存不足

**症状**: Agent频繁重启或性能测试中途失败

**解决方案**:
```bash
# 增加JVM堆内存
java -Xms2g -Xmx4g -Dfile.encoding=UTF-8 -jar ...
```

### 4. 端口冲突

**检查端口占用**：
```bash
# Linux/macOS
netstat -tlnp | grep :5000

# Windows
netstat -an | findstr :5000
```

## 📊 监控和维护

### 1. Agent状态监控

- **CPU使用率**: 测试期间监控CPU负载
- **内存使用**: 注意JVM堆内存使用情况
- **网络带宽**: 确保网络不成为瓶颈
- **磁盘空间**: 定期清理日志文件

### 2. 日志管理

```bash
# 定期清理日志（添加到crontab）
find /opt/easy-jmeter/logs -name "*.log" -mtime +7 -delete
```

### 3. 性能调优

**JVM参数优化**：
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
```

## 🚀 最佳实践

1. **资源规划**: 根据并发用户数规划Agent数量和配置
2. **网络优化**: Agent与Server之间使用高速网络连接
3. **监控告警**: 设置Agent离线告警机制
4. **定期维护**: 定期重启Agent清理内存
5. **备份配置**: 保存Agent配置文件便于快速恢复

## 📞 技术支持

如遇到问题，请检查：
1. Agent机器日志文件
2. Server端连接日志
3. 网络防火墙设置
4. JMeter安装和权限

---

**注意事项**：
- 确保所有机器时间同步
- 生产环境建议使用SSL加密通信
- 定期更新JMeter版本保持兼容性
- 建议Agent专用，不要运行其他高负载应用

**配置完成后，您就可以在Web界面中使用这些Agent机器执行分布式性能测试了！** 🎉
