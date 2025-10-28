# Project Context

## Purpose
Easy JMeter 性能测试平台是一个基于Apache JMeter的企业级分布式性能测试平台，专为大规模性能测试场景设计。平台提供完整的测试用例管理、分布式压力测试、实时监控、结果分析等功能，支持Windows和Linux双平台部署。

**主要目标**:
- 提供Web界面的性能测试管理平台
- 支持分布式JMeter压力机管理
- 实时监控和结果可视化
- 简化性能测试的部署和运维

## Tech Stack

### 后端技术栈
- **Java 8+** - 主要编程语言
- **Spring Boot 2.3.12** - Web框架
- **MyBatis Plus** - ORM框架
- **MySQL 5.7+** - 主数据库
- **MongoDB 4.2+** - 文档存储
- **InfluxDB 1.8+** - 时序数据库，用于性能数据
- **MinIO** - 对象存储
- **SocketIO** - 实时通信
- **Apache JMeter 5.6.2** - 性能测试引擎

### 前端技术栈
- **Node.js 12.13.0+** - 运行环境
- **Vue.js 2.x** - 前端框架
- **Element UI** - UI组件库
- **ECharts** - 数据可视化

### 部署和运维
- **Docker** - 容器化部署
- **Maven 3.6+** - 构建工具
- **Nginx** - 反向代理

## Project Conventions

### Code Style
- **Java**: 遵循Google Java Style Guide，使用驼峰命名
- **JavaScript/Vue**: 遵循Vue.js官方风格指南，使用kebab-case命名组件
- **数据库**: 使用snake_case命名表和字段
- **API**: RESTful风格，使用kebab-case命名端点

### Architecture Patterns
- **前后端分离架构**: Vue.js前端 + Spring Boot后端
- **微服务架构**: 支持分布式部署
- **事件驱动**: 使用SocketIO进行实时通信
- **分层架构**: Controller -> Service -> Mapper

### Testing Strategy
- **单元测试**: 使用JUnit测试Java后端
- **集成测试**: 测试API端点和数据库交互
- **性能测试**: 使用JMeter进行压力测试
- **前端测试**: 使用Jest进行Vue组件测试

### Git Workflow
- **分支策略**: GitFlow (master/develop/feature分支)
- **提交规范**: 使用Conventional Commits格式
- **代码审查**: 所有PR需要代码审查
- **自动化CI/CD**: 使用GitHub Actions或Jenkins

## Domain Context

### 核心概念
- **Project**: 性能测试项目，包含多个测试用例
- **Case**: 测试用例，包含JMeter脚本和配置
- **Task**: 执行任务，运行特定的测试用例
- **Machine**: 压力机，执行JMeter测试的节点
- **Agent**: 部署在压力机上的代理程序

### 业务流程
1. 用户创建Project并上传JMeter脚本
2. 系统配置压力机和Agent
3. 用户创建Task并选择测试用例
4. Agent分布式执行测试并收集结果
5. 实时显示测试结果和性能指标
6. 生成测试报告

## Important Constraints

### 技术约束
- **Java 8兼容性**: 必须支持Java 8运行环境
- **跨平台支持**: 支持Windows和Linux部署
- **分布式架构**: 支持多压力机横向扩展
- **实时性**: 测试结果需要实时传输和显示

### 业务约束
- **数据安全**: 测试数据和脚本需要安全存储
- **权限管理**: 支持用户组和权限控制
- **审计日志**: 记录所有重要操作
- **资源限制**: 控制并发测试数量和资源使用

## External Dependencies

### 核心依赖
- **Apache JMeter 5.6.2**: 性能测试引擎
- **MySQL**: 业务数据存储
- **InfluxDB**: 性能时序数据存储
- **MongoDB**: 文档和日志存储
- **MinIO**: 文件存储(测试脚本等)

### 第三方服务
- **Docker Hub**: 容器镜像仓库
- **GitHub**: 源代码管理和CI/CD
- **NPM**: 前端依赖包管理

### API接口
- **SocketIO客户端**: 与Agent的实时通信
- **RESTful API**: 前后端数据交互
- **InfluxDB API**: 性能数据查询和存储
