# JMX Editor 部署和使用指南

## ✅ 完成状态

所有核心功能已实现并部署完成！

### 已完成的任务：

1. ✅ 数据库迁移 - `jmx_structure` 表和 `creation_mode` 字段已创建
2. ✅ 后端编译 - 包含新的 JMeter Java 依赖
3. ✅ 前端编译 - JMX Builder 页面和组件已构建
4. ✅ 路由配置 - 前端路由已配置完成

## 🚀 启动应用

### 1. 启动后端

```bash
cd api
.\start-dev-simple.cmd
```

或使用开发模式（支持热重载）：

```bash
cd api
.\start-dev.cmd
```

### 2. 启动前端

开发模式：
```bash
cd web
npm run serve
```

生产模式（已构建）：
- 前端资源已打包到 `web/dist` 目录
- 可以使用 Nginx 或其他 Web 服务器部署

## 📖 使用指南

### 方式一：编辑现有 JMX 文件

1. 访问"用例管理"页面
2. 在用例列表中，找到要编辑的用例
3. 点击用例卡片上的 **"编辑 JMX"** 按钮（带编辑图标）
4. 系统会自动解析 JMX 文件并在树形编辑器中显示
5. 在树形编辑器中修改配置：
   - 左侧面板：查看和选择元素
   - 右侧面板：编辑选中元素的属性
6. 点击"验证"按钮检查结构正确性
7. 点击"保存"按钮保存修改

### 方式二：从零创建 JMX 测试计划

1. 直接访问 JMX Builder 页面：`/case/jmx-builder`
2. 选择以下方式之一：

   **A. 使用模板快速开始**
   - 点击"Load Template"按钮
   - 选择预定义模板：
     - `simple`: 简单 HTTP 测试（1线程，1次循环）
     - `load_test`: 负载测试（10线程，5分钟）
     - `stress_test`: 压力测试（100线程，10分钟）
   
   **B. 从空白测试计划开始**
   - 系统自动创建空白测试计划
   - 选择根节点，点击"Add Element"添加元素
   - 逐步构建测试结构

3. 配置测试元素：
   - **Thread Group**: 配置线程数、Ramp-Up、循环次数等
   - **HTTP Sampler**: 配置 URL、Method、Headers、Body等
   - **Java Sampler**: 配置类名和参数
   - **CSV Data Set**: 配置数据文件和变量

4. 保存测试计划

## 🎯 支持的 JMX 元素

### 完全支持：
- ✅ **TestPlan** - 测试计划根元素
- ✅ **ThreadGroup** - 线程组（虚拟用户）
- ✅ **HTTPSampler** - HTTP 请求
- ✅ **JavaSampler** - Java 采样器
- ✅ **CSVDataSet** - CSV 数据源
- ✅ **HeaderManager** - HTTP 头管理器
- ✅ **ResultCollector** - 结果收集器

### 属性配置：

**ThreadGroup 属性：**
- Number of Threads (线程数)
- Ramp-Up Period (启动时间)
- Loop Count (循环次数)
- Duration (持续时间)
- Startup Delay (启动延迟)
- Scheduler (调度器开关)

**HTTP Sampler 属性：**
- Protocol (http/https)
- Server/IP (域名或IP)
- Port (端口)
- Path (路径)
- Method (GET/POST/PUT/DELETE等)
- Parameters (参数列表)
- Body Data (请求体)
- Headers (请求头)
- Timeouts (连接/响应超时)

**Java Sampler 属性：**
- Classname (完整类名)
- Arguments (参数列表)

## 🔧 功能特性

### 1. 可视化树形编辑
- 清晰的层级结构展示
- 拖拽调整元素顺序
- 添加/删除元素
- 启用/禁用元素

### 2. 智能属性编辑
- 根据元素类型显示对应的编辑器
- 实时属性验证
- 默认值自动填充

### 3. 版本管理
- 每次保存创建新版本
- 可查看历史版本
- 支持版本回退

### 4. 模板系统
- 内置常用测试模板
- 快速开始测试
- 可扩展自定义模板

### 5. 数据持久化
- JMX 结构以 JSON 格式存储在数据库
- 同时保留 JMX 文件
- 双向同步：JSON ↔ JMX

## 🔌 API 端点

### 用例相关 API

```
# 解析 JMX 文件
GET /v1/case/{id}/jmx/parse

# 获取 JMX 树结构
GET /v1/case/{id}/jmx/tree

# 保存 JMX 结构
POST /v1/case/{id}/jmx/structure
Body: {
  "caseId": 1,
  "structure": {...}
}

# 生成 JMX 文件
POST /v1/case/{id}/jmx/generate

# 获取所有版本
GET /v1/case/{id}/jmx/versions

# 获取指定版本
GET /v1/case/{id}/jmx/version/{version}
```

### Builder API

```
# 创建空测试计划
POST /v1/jmx/builder/create?name=TestPlan

# 获取模板
POST /v1/jmx/builder/template/{templateName}

# 验证结构
POST /v1/jmx/builder/validate
Body: {
  "caseId": 1,
  "structure": {...}
}

# 列出所有模板
GET /v1/jmx/builder/templates
```

## 🐛 故障排除

### 问题 1：JMX 文件解析失败
**原因**：JMX 文件格式不支持或包含未知元素
**解决**：
- 检查 JMX 文件是否由 JMeter 5.5 生成
- 查看后端日志获取详细错误信息
- 暂不支持的元素会被忽略

### 问题 2：保存后生成的 JMX 无法执行
**原因**：必填字段未填写或配置不正确
**解决**：
- 使用"验证"功能检查结构
- 确保 HTTP Sampler 的 domain 字段已填写
- 确保 Java Sampler 的 classname 字段已填写

### 问题 3：前端路由 404
**原因**：前端路由配置未生效
**解决**：
- 清除浏览器缓存
- 重新构建前端：`cd web && npm run build`
- 重启前端开发服务器

### 问题 4：编辑 JMX 按钮不显示
**原因**：用户权限不足
**解决**：
- 确保用户有"用例管理"权限
- 检查用户组配置

## 📊 数据库表结构

### jmx_structure 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键 |
| case_id | INT | 关联的用例 ID |
| structure_json | LONGTEXT | JMX 结构（JSON 格式）|
| version | INT | 版本号 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| delete_time | DATETIME | 删除时间（软删除）|

### case 表新增字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| creation_mode | VARCHAR(20) | 'UPLOAD' | 创建模式：UPLOAD 或 BUILDER |

## 🔐 权限要求

- **查看 JMX 结构**：需要"用例管理"权限
- **编辑 JMX 结构**：需要"用例管理"权限
- **创建新测试计划**：需要"用例管理"权限

## 📝 使用示例

### 示例 1：创建简单的 HTTP 性能测试

1. 进入 JMX Builder
2. 点击"Load Template" → 选择 "simple"
3. 选择 HTTP Request 节点
4. 修改属性：
   - Domain: `api.example.com`
   - Path: `/users`
   - Method: `GET`
5. 选择 Thread Group 节点
6. 修改线程数为 10
7. 点击"保存"

### 示例 2：添加 CSV 参数化

1. 打开现有测试计划
2. 选择 Thread Group 节点
3. 点击"Add Element" → 选择 "CSV Data Set"
4. 配置 CSV Data Set：
   - Filename: `/path/to/data.csv`
   - Variable Names: `username,password`
   - Delimiter: `,`
5. 在 HTTP Request 中使用变量：`${username}`
6. 保存

## 🎨 界面说明

### JMX Builder 界面

```
┌─────────────────────────────────────────────────┐
│  JMX Builder                     [Template][Save]│
├───────────────┬─────────────────────────────────┤
│  Tree View    │  Property Editor                │
│               │                                 │
│  □ TestPlan   │  Name: [My Test Plan        ]  │
│  └─□ Thread   │  Enabled: ☑                    │
│     Group     │                                 │
│    ├─□ HTTP   │  ── Thread Group Settings ──   │
│    │  Request │  Threads: [10]                 │
│    └─□ CSV    │  Ramp-Up: [60]                 │
│       DataSet │  Loops: [1]                    │
│               │  Duration: [300]               │
│  [+Add] [-Del]│  ...                           │
│               │                                 │
└───────────────┴─────────────────────────────────┘
```

## 📚 扩展开发

### 添加新的 JMX 元素类型

1. **后端**：
   - 在 `JmxParserServiceImpl` 中添加解析逻辑
   - 创建对应的 DTO 类
   - 在 `JmxBuilderService` 中添加构建方法

2. **前端**：
   - 创建属性编辑器组件
   - 在 `JmxTreeEditor.vue` 中注册编辑器
   - 添加元素类型到可选列表

### 添加新模板

在 `JmxBuilderServiceImpl.getTemplate()` 中添加：

```java
case "my_template":
    return createMyCustomTemplate();
```

## 🎉 总结

JMX Editor 功能已完全集成到 Easy JMeter 平台，提供了：

- ✅ 强大的可视化编辑能力
- ✅ 完整的版本管理
- ✅ 友好的用户界面
- ✅ 灵活的模板系统
- ✅ 可靠的数据持久化

立即开始使用 JMX Editor 简化你的性能测试工作流程！

---

**版本**: 0.1.0
**更新日期**: 2025-11-25
**维护者**: Easy JMeter 开发团队

