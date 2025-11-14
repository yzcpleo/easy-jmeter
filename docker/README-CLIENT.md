# Easy JMeter Client Docker 部署指南

## 概述

本配置用于部署 Easy JMeter 的 Client 模式，该模式连接宿主机上的各种服务（MySQL、MongoDB、InfluxDB、MinIO、SocketIO）。

## 文件结构

```
docker/
├── Dockerfile.client          # Client模式的Docker镜像
├── application-client.yml     # Client模式配置文件
├── docker-entrypoint.sh       # 容器启动脚本
├── start-client.sh           # Linux启动脚本
├── start-client.bat          # Windows启动脚本
├── docker-compose.client.yml  # Docker Compose配置
└── README-CLIENT.md          # 本文档
```

## 前置要求

1. **宿主机服务**：确保以下服务在宿主机上运行：
   - MySQL: `localhost:3307`
   - MongoDB: `localhost:27018`
   - InfluxDB: `localhost:8087`
   - MinIO: `localhost:9001`
   - SocketIO服务: `localhost:9006`

2. **Docker环境**：
   - Docker Engine 20.10+
   - Docker Compose

3. **应用构建**：
   ```bash
   # 在项目根目录执行
   mvn clean package
   ```

## 快速开始

### Windows 环境

1. **构建JAR包**：
   ```cmd
   mvn clean package
   ```

2. **启动Client**：
   ```cmd
   docker\start-client.bat
   ```

### Linux 环境

1. **构建JAR包**：
   ```bash
   mvn clean package
   ```

2. **赋予执行权限**：
   ```bash
   chmod +x docker/start-client.sh
   ```

3. **启动Client**：
   ```bash
   ./docker/start-client.sh
   ```

## 配置说明

### 宿主机连接配置

Client模式通过 `host.docker.internal` 访问宿主机服务：

| 服务 | 宿主机端口 | 容器内连接地址 |
|------|-----------|----------------|
| MySQL | 3307 | `host.docker.internal:3307` |
| MongoDB | 27018 | `host.docker.internal:27018` |
| InfluxDB | 8087 | `host.docker.internal:8087` |
| MinIO | 9001 | `host.docker.internal:9001` |
| SocketIO | 9006 | `host.docker.internal:9006` |

### 网络模式

使用 `network_mode: "host"` 模式，使容器直接使用宿主机网络，便于访问宿主机服务。

### 内存配置

Client模式的JVM参数（相比Agent模式较少）：
```bash
-server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

## 使用命令

### 查看容器状态
```bash
docker-compose -f docker-compose.client.yml ps
```

### 查看日志
```bash
docker logs -f easyjmeter-client
```

### 停止服务
```bash
docker-compose -f docker-compose.client.yml down
```

### 重启服务
```bash
docker-compose -f docker-compose.client.yml restart
```

### 进入容器
```bash
docker exec -it easyjmeter-client sh
```

## 目录映射

| 宿主机目录 | 容器内目录 | 说明 |
|-----------|-----------|------|
| `./logs/` | `/opt/easy-jmeter/logs` | 应用日志 |
| `./jmeter-results/` | `/opt/easy-jmeter/jmeter-results` | JMeter测试结果 |
| `./temp/` | `/opt/easy-jmeter/temp` | 临时文件 |

## 健康检查

容器包含健康检查，每30秒检查一次：
- 检查端点：`http://localhost:5000/actuator/health`
- 超时时间：10秒
- 重试次数：3次

## 故障排查

### 1. 无法连接宿主机服务

检查宿主机防火墙设置，确保容器可以访问宿主机端口。

### 2. `host.docker.internal` 无法解析

在某些Docker版本中，可能需要手动添加：
```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

### 3. 端口冲突

确保宿主机上的端口没有被其他服务占用。

### 4. 内存不足

如果出现内存不足，可以调整JVM参数：
```bash
export JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC"
```

## 自定义配置

### 修改宿主机端口

如果宿主机服务端口不同，修改 `docker/application-client.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://host.docker.internal:3308/easy-jmeter  # 修改端口
```

### 添加环境变量

在 `docker-compose.client.yml` 中添加：
```yaml
environment:
  - CUSTOM_VAR=value
```

## 生产环境部署

1. **使用外部配置**：
   ```yaml
   volumes:
     - /path/to/production-config:/config:ro
   ```

2. **设置资源限制**：
   ```yaml
   deploy:
     resources:
       limits:
         memory: 4G
         cpus: '2'
   ```

3. **日志管理**：
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

## 安全注意事项

1. 不要在生产环境中暴露调试端口
2. 使用强密码替换默认密码
3. 定期更新基础镜像
4. 限制容器的文件系统访问权限

## 支持

如有问题，请检查：
1. 容器日志：`docker logs easyjmeter-client`
2. 宿主机服务状态
3. 网络连通性
4. 配置文件正确性