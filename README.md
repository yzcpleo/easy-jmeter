<h1 align="center">🚀 Easy JMeter 性能测试平台</h1>
<h4 align="center">基于JMeter的企业级分布式性能测试平台</h4>

<p align="center">
  <strong>🔄 Forked from: <a href="https://github.com/guojiaxing1995/easy-jmeter">guojiaxing1995/easy-jmeter</a></strong>
</p>

<p align="center">
  <a href="https://www.oracle.com/cn/java/technologies/downloads/"><img src="https://img.shields.io/badge/Java-8%2B-orange?style=flat-square&logo=openjdk" alt="Java version"></a>
  <a href="https://nodejs.org/en/"><img src="https://img.shields.io/badge/Node.js-12.13.0+-green?style=flat-square&logo=node.js" alt="Node.js version"></a>
  <a href="https://vuejs.org/"><img src="https://img.shields.io/badge/Vue.js-2.x-4FC08D?style=flat-square&logo=vue.js" alt="Vue version"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-2.3.12-6DB33F?style=flat-square&logo=spring-boot" alt="Spring Boot version"></a>
</p>

<p align="center">
  <a href="https://www.mysql.com/cn/"><img src="https://img.shields.io/badge/MySQL-5.7+-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL version"></a>
  <a href="https://www.mongodb.com/zh-cn"><img src="https://img.shields.io/badge/MongoDB-4.2+-47A248?style=flat-square&logo=mongodb&logoColor=white" alt="MongoDB version"></a>
  <a href="https://influxdb-v1-docs-cn.cnosdb.com/influxdb/v1.8/"><img src="https://img.shields.io/badge/InfluxDB-1.8+-22ADF6?style=flat-square&logo=influxdb&logoColor=white" alt="InfluxDB version"></a>
  <a href="https://min.io/"><img src="https://img.shields.io/badge/MinIO-Latest-C72E29?style=flat-square&logo=minio&logoColor=white" alt="MinIO version"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-lightgrey?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/JMeter-5.6.2-D22128?style=flat-square&logo=apache&logoColor=white" alt="JMeter version">
</p>



## 📋 项目介绍

**Easy JMeter 性能测试平台**是一个基于Apache JMeter的企业级分布式性能测试平台，专为大规模性能测试场景设计。平台提供完整的测试用例管理、分布式压力测试、实时监控、结果分析等功能，支持Windows和Linux双平台部署。

### ✨ 核心特性

- 🎯 **分布式测试架构** - 支持多节点压力机分布式执行测试
- 🖥️ **跨平台支持** - Server端和Agent端均支持Windows/Linux部署
- 📊 **实时监控** - 基于SocketIO的实时数据传输和监控
- 📈 **数据可视化** - 丰富的图表展示和测试结果分析
- 🔧 **智能化管理** - 自动化的Agent管理和资源分配
- 🚀 **一键启动** - 提供完整的启动脚本套件，简化部署运维

### 🏗️ 技术架构

- **前端**：Vue.js 2.x + Element UI
- **后端**：Spring Boot 2.3.12 + MyBatis Plus
- **数据存储**：MySQL + MongoDB + InfluxDB
- **文件存储**：MinIO 对象存储
- **压力测试引擎**：Apache JMeter 5.6.2
- **实时通信**：SocketIO
- **部署方式**：Docker + 原生部署

### 📚 相关链接

- 📖 **使用文档**：[CSDN博客](https://blog.csdn.net/qq_36450484/article/details/136213502)
- 🎨 **原型设计**：[墨刀原型](https://modao.cc/app/Qf56LAncrokbxs3iOBMRap#screen=slcycrmormft43z)
- 🔄 **原始项目**：[guojiaxing1995/easy-jmeter](https://github.com/guojiaxing1995/easy-jmeter)
- 💾 **当前仓库**：[yzcpleo/easy-jmeter](https://github.com/yzcpleo/easy-jmeter)

## 🚀 快速开始

### 📦 环境要求

**基础环境**：
- Java 8+ (推荐 OpenJDK 8)
- Node.js 12.13.0+
- Maven 3.6+

**基础服务**：
- MySQL 5.7+
- MongoDB 4.2+
- InfluxDB 1.8+
- MinIO 对象存储

**压力机环境**：
- Apache JMeter 5.6.2+（Agent节点）

### ⚡ 一键启动

项目提供完整的启动脚本，支持Windows和Linux平台：

#### Windows用户
```batch
# 运行快速启动菜单
scripts\quick-start.bat
```

#### Linux用户
```bash
# 设置权限（首次运行）
chmod +x scripts/quick-start.sh

# 运行快速启动菜单
./scripts/quick-start.sh
```

### 📋 快速部署流程

1. **准备基础服务**
   ```bash
   # 使用Docker快速启动基础服务
   docker-compose up -d mysql mongodb influxdb minio
   ```

2. **编译项目**
   ```bash
   # 编译后端
   cd api && mvn clean package -DskipTests
   
   # 编译前端
   cd web && npm install && npm run build
   ```

3. **启动Server**
   - Windows: `scripts\windows\start-server.bat`
   - Linux: `./scripts/linux/start-server.sh`

4. **启动Agent**（可选，用于分布式测试）
   - Windows: `scripts\windows\start-agent.bat` 
   - Linux: `./scripts/linux/start-agent.sh`

5. **访问系统**
   - Web界面: http://localhost:3000
   - API文档: http://localhost:5000/swagger-ui.html

### 📖 详细文档

- 🔧 [Agent配置指南](Agent机器配置指南.md)
- 💻 [Windows Agent配置](Windows-Agent配置补充说明.md)
- 🚀 [启动脚本使用指南](启动脚本使用指南.md)

## 🎨 功能展示

![用例列表](https://img-blog.csdnimg.cn/direct/d4cde4d0325d4060bc6075c880db6295.jpeg#pic_center)

![编辑用例](https://img-blog.csdnimg.cn/direct/74b1642b8b134e30aa37b75766aa416d.jpeg#pic_center)

![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d5a17d1478434266ab211193b111b030.jpeg#pic_center)

![图表报告1](https://img-blog.csdnimg.cn/direct/d348bef4da924558b438df110ed5947a.jpeg#pic_center)

## 🏛️ 系统架构

![系统架构](https://img-blog.csdnimg.cn/direct/1c82503a78a644d484df87b6f9dd8f75.png#pic_center)

### 架构说明

**用户访问层**：
- Web前端提供可视化操作界面
- 通过HTTP RESTful API与后端服务通信

**应用服务层**：
- **Server端**：核心业务逻辑处理，任务调度，数据管理
- **Agent端**：压力机节点，执行JMeter测试任务
- **实时通信**：基于SocketIO的双向通信，支持任务下发和状态上报

**数据存储层**：
- **MySQL**：业务数据存储（用户、项目、测试用例等）
- **MongoDB**：测试结果详细数据存储
- **InfluxDB**：压测过程实时监控数据存储
- **MinIO**：文件对象存储（测试脚本、数据文件、测试报告）

### 🌟 架构特点

- ✅ **跨平台支持**：Server和Agent均支持Windows/Linux部署
- 🔄 **弹性扩展**：支持动态添加/移除Agent节点
- 📊 **实时监控**：SocketIO实现毫秒级状态同步
- 🛡️ **高可用**：多Agent节点故障转移机制
- 📈 **性能优化**：分层存储架构，热数据实时访问


## 🔧 开发调试

### 前端开发

```bash
# 进入前端目录
cd web

# 安装依赖
npm install

# 启动开发服务器
npm run serve

# 构建生产版本
npm run build
```

### 后端开发

#### Server端启动

```bash
# 进入后端目录
cd api

# Maven编译
mvn clean package -DskipTests

# 配置Server模式
# 在application-dev.yml中设置：
socket:
  server:
    enable: true
  client:
    enable: false

# 启动应用
java -Dspring.profiles.active=dev -jar target/easyJmeter-0.1.0-RELEASE.jar
```

#### Agent端启动

```bash
# 配置Agent模式
# 在application-agent.yml中设置：
socket:
  server:
    enable: false
  client:
    enable: true
    serverUrl: http://localhost:9000

# 启动Agent
java -Dspring.profiles.active=prod -jar target/easyJmeter-0.1.0-RELEASE.jar
```

### 🛠️ 开发工具推荐

- **IDE**：IntelliJ IDEA / VS Code
- **API测试**：Postman / Swagger UI
- **数据库管理**：Navicat / DBeaver
- **版本控制**：Git

## 🚀 生产部署

### 📋 部署前准备

#### 1. 基础环境安装

**数据库服务**：
```bash
# 安装MySQL 5.7+
# 导入数据库结构：api/src/main/resources/schema.sql

# 安装MongoDB 4.2+
# 安装InfluxDB 1.8+

# 安装MinIO对象存储
# 创建bucket并设置public访问策略
```

**JMeter环境**（Agent节点必需）：
```bash
# 下载Apache JMeter 5.6.2
# 配置环境变量 JMETER_HOME
```

#### 2. 应用编译

```bash
# 编译后端
cd api
mvn clean package -DskipTests

# 编译前端
cd web
npm install
npm run build
```

### 🎯 方式一：原生部署（推荐）

#### Server端部署

```bash
# 1. 创建配置文件 application-prod.yml
# 2. 使用启动脚本
./scripts/linux/start-server.sh    # Linux
scripts\windows\start-server.bat   # Windows

# 3. 访问验证
# Web界面: http://localhost:3000
# API接口: http://localhost:5000
```

#### Agent端部署

```bash
# 1. 配置JMeter环境
export JMETER_HOME=/path/to/jmeter

# 2. 创建Agent配置文件 application-agent.yml
# 3. 使用启动脚本
./scripts/linux/start-agent.sh     # Linux  
scripts\windows\start-agent.bat    # Windows
```

#### Nginx部署（Web服务）

```bash
# 1. 安装Nginx
# 2. 使用提供的配置文件
cp web/default.conf /etc/nginx/conf.d/
# 3. 复制前端构建文件到Nginx目录
# 4. 重启Nginx服务
```

### 🐳 方式二：容器化部署

#### Docker Compose一键部署

```bash
# 1. 编辑docker-compose.yaml配置
# 2. 启动基础服务
docker-compose up -d mysql mongodb influxdb minio

# 3. 导入数据库初始化SQL
# 4. Server本地启动（推荐）
./scripts/linux/start-server.sh
```

#### 混合部署架构

**推荐的生产架构**：
- 🐳 **基础服务**：使用Docker部署（MySQL、MongoDB、InfluxDB、MinIO）  
- 🖥️ **Server端**：物理机/虚拟机原生部署
- 🔧 **Agent端**：压力机原生部署（获得更好性能）
- 🌐 **前端**：Nginx反向代理

### 🛠️ 部署脚本特性

我们提供的启动脚本具有以下特性：

- ✅ **智能内存管理**：根据系统内存自动设置JVM参数
- 🔍 **环境检查**：自动检查Java、JMeter等环境
- 📊 **进程管理**：PID文件管理，防止重复启动
- 📋 **日志管理**：自动创建日志目录，支持日志查看
- 🎨 **友好界面**：快速启动菜单，操作简便
- 🔄 **优雅停止**：支持平滑重启和强制停止

### 🔧 高可用部署

```bash
# 多Agent节点部署
Agent-1: 172.16.1.10
Agent-2: 172.16.1.11  
Agent-3: 172.16.1.12

# 负载均衡配置
# 在系统管理->压力机管理中添加多个Agent节点
# 系统会自动进行负载分发
```

### 📊 性能调优建议

**Server端**：
- 内存：推荐4GB+
- CPU：4核心+
- 磁盘：SSD推荐

**Agent端**：
- 内存：推荐8GB+（执行大型测试）
- CPU：8核心+（并发处理能力）
- 网络：千兆网络+（数据传输）

### 🔍 部署验证

```bash
# 检查服务状态
./scripts/linux/stop-server.sh     # 查看Server状态
./scripts/linux/stop-agent.sh      # 查看Agent状态

# 查看日志
tail -f logs/server/server.log
tail -f logs/agent/agent.log

# 功能验证
# 1. 访问Web界面创建测试项目
# 2. 添加Agent压力机
# 3. 执行简单的性能测试
```

## 🔄 版本更新记录

### v2.0.0 (最新版本)
- ✅ **跨平台支持**：新增Windows Server Agent支持
- 🚀 **启动脚本**：提供完整的Windows/Linux启动脚本套件  
- 🔧 **Java 8兼容**：完全支持Java 8，降低部署门槛
- 📋 **配置管理**：优化配置文件结构，支持外置配置
- 🛠️ **问题修复**：修复UUID异常、中文编码、空指针等问题
- 📊 **智能优化**：自动内存管理、环境检测等智能化特性
- 📖 **文档完善**：提供详细的部署和配置指南

### v1.x.x (历史版本)  
- 基于Java 11的原始版本
- 仅支持Linux Agent部署
- 基础的性能测试功能

## 🤝 贡献指南

我们欢迎任何形式的贡献！

### 如何贡献
1. 🍴 Fork 本项目
2. 🌿 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 💻 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 📤 推送到分支 (`git push origin feature/AmazingFeature`)
5. 🔄 创建 Pull Request

### 贡献类型
- 🐛 Bug修复
- ✨ 新功能开发
- 📚 文档改进
- 🎨 UI/UX优化
- ⚡ 性能优化

## 📞 技术支持

### 常见问题
- 📋 查看 [Agent配置指南](Agent机器配置指南.md)
- 💻 参考 [Windows配置说明](Windows-Agent配置补充说明.md)  
- 🚀 使用 [启动脚本指南](启动脚本使用指南.md)

### 问题反馈
- 🐛 **Bug报告**：[GitHub Issues](https://github.com/guojiaxing1995/easy-jmeter/issues)
- 💡 **功能建议**：[GitHub Discussions](https://github.com/guojiaxing1995/easy-jmeter/discussions)
- 📧 **技术交流**：欢迎提交Issue或PR

### 社区支持
- ⭐ **Star项目**：如果对您有帮助，请给我们Star！
- 🔄 **分享推荐**：帮助更多人了解这个项目
- 📣 **反馈建议**：您的建议是我们改进的动力

## 📄 开源许可

本项目基于 **MIT License** 开源协议，详情请参阅 [LICENSE](LICENSE) 文件。

### 使用声明
- ✅ 个人和商业用途
- ✅ 修改和分发  
- ✅ 私有使用
- ⚠️ 需保留原始许可证和版权声明

---

<div align="center">

**🌟 如果这个项目对您有帮助，请不要忘记给我们一个Star！**

**感谢所有贡献者对项目的支持！** 🙏

</div>

