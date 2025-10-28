# Easy-JMeter 部署指南

## 1. 前提条件
- 已安装 Docker 和 Docker Compose
- Windows 10/11 或 Linux 系统

## 2. JMeter 准备（用于压力测试代理）
由于项目需要JMeter作为压力测试引擎，您需要：

### 下载JMeter
```bash
# 创建JMeter目录
mkdir C:\opt
# 下载 Apache JMeter 5.6.2
# 访问: https://jmeter.apache.org/download_jmeter.cgi
# 下载二进制版本并解压到 C:\opt\apache-jmeter-5.6.2
```

### 或者使用简化方案（推荐）
如果您只想快速体验系统，可以修改 docker-compose.yaml 文件，去掉 agent1 和 agent2 服务，只运行 server、web 和数据库服务。

## 3. 部署步骤

### 方案A: 完整部署（包含压力测试代理）
1. 下载并解压JMeter到指定目录
2. 运行: `docker-compose up -d`

### 方案B: 简化部署（仅管理系统）
1. 运行修改后的docker-compose文件（已为您准备）

## 4. 访问系统
- Web界面: http://localhost
- 后端API: http://localhost:8037
- 数据库管理: MySQL端口 9081, MongoDB端口 9090

## 5. 默认用户
- 需要导入初始化SQL后创建用户
