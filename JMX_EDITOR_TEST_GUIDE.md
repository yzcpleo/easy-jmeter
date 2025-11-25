# JMX Editor 测试指南

## 🔧 修复记录

### 问题 1: `handleEditJmx is not a function`
**原因**: 函数未在 Vue setup() 的 return 中导出
**修复**: 在 `case-list.vue` 的 return 对象中添加 `handleEditJmx`
**状态**: ✅ 已修复

### 问题 2: 路由找不到 `jmx-builder`
**原因**: 动态路由配置可能未正确加载
**修复**: 在 `web/src/router/route.js` 中手动添加 JMX Builder 路由
**状态**: ✅ 已修复

## 🧪 测试步骤

### 前提条件

1. **后端已启动**
   ```bash
   cd api
   .\start-dev-simple.cmd
   ```

2. **前端已部署**
   - 生产模式：`web/dist` 已构建
   - 开发模式：运行 `cd web && npm run serve`

3. **数据库已迁移**
   - `jmx_structure` 表已创建
   - `case.creation_mode` 字段已添加

4. **清除浏览器缓存**
   - 按 `Ctrl + Shift + Delete` 清除缓存
   - 或按 `Ctrl + F5` 强制刷新

### 测试用例 1: 访问用例列表

**步骤**:
1. 打开浏览器，访问 http://localhost:8080
2. 登录系统
3. 导航到 "用例管理" → "用例列表"

**预期结果**:
- ✅ 页面正常加载
- ✅ 显示现有用例列表
- ✅ 每个用例卡片上有 "编辑 JMX" 按钮（带编辑图标）

### 测试用例 2: 点击编辑 JMX 按钮

**步骤**:
1. 在用例列表中找到任意用例
2. 点击用例卡片上的 "编辑 JMX" 按钮

**预期结果**:
- ✅ 页面跳转到 `/case/jmx-builder/[caseId]`
- ✅ 不出现路由错误
- ✅ JMX Builder 页面开始加载

### 测试用例 3: JMX 文件解析

**步骤**:
1. 点击有 JMX 文件的用例的 "编辑 JMX" 按钮
2. 等待页面加载

**预期结果**:
- ✅ 后端调用 `/v1/case/{id}/jmx/parse` API
- ✅ JMX 文件被解析为树形结构
- ✅ 左侧面板显示测试计划的树形结构
- ✅ 可以看到 TestPlan、ThreadGroup、HTTP Sampler 等元素

**检查控制台**:
打开浏览器开发者工具（F12），查看：
- Network 标签：检查 API 调用是否成功
- Console 标签：检查是否有 JavaScript 错误

### 测试用例 4: 选择和编辑元素

**步骤**:
1. 在左侧树形面板中，点击一个元素（如 HTTP Sampler）
2. 查看右侧属性面板

**预期结果**:
- ✅ 右侧显示该元素的属性编辑器
- ✅ 可以编辑 Domain、Path、Method 等属性
- ✅ 属性值实时更新

### 测试用例 5: 加载模板

**步骤**:
1. 点击顶部 "Load Template" 按钮
2. 选择一个模板（如 "simple"）

**预期结果**:
- ✅ 弹出模板选择对话框
- ✅ 显示可用模板列表
- ✅ 点击模板后，树形结构更新为模板内容

### 测试用例 6: 验证结构

**步骤**:
1. 编辑一些属性
2. 点击 "Validate" 按钮

**预期结果**:
- ✅ 显示加载状态
- ✅ 验证成功：显示成功消息
- ✅ 验证失败：显示错误详情

### 测试用例 7: 保存结构

**步骤**:
1. 编辑测试计划
2. 点击 "Save" 按钮

**预期结果**:
- ✅ 显示加载状态
- ✅ 后端调用保存和生成 API
- ✅ 显示保存成功消息
- ✅ 数据库中创建新版本记录

### 测试用例 8: 添加新元素

**步骤**:
1. 选择 ThreadGroup 节点
2. 点击 "Add Element" 按钮
3. 选择 "HTTP Request"
4. 填写名称，点击 "Add"

**预期结果**:
- ✅ 弹出添加元素对话框
- ✅ 显示可添加的元素类型
- ✅ 新元素添加到树中
- ✅ 可以编辑新元素的属性

### 测试用例 9: 删除元素

**步骤**:
1. 选择一个非 TestPlan 的元素
2. 点击 "Delete" 按钮
3. 确认删除

**预期结果**:
- ✅ 显示确认对话框
- ✅ 确认后元素从树中移除
- ✅ TestPlan 无法删除

### 测试用例 10: 直接访问 JMX Builder

**步骤**:
1. 在浏览器地址栏输入: `http://localhost:8080/#/case/jmx-builder`
2. 按 Enter

**预期结果**:
- ✅ 页面正常加载
- ✅ 显示空白测试计划
- ✅ 可以从模板开始或手动构建

## 🐛 常见问题排查

### 问题: 点击按钮后路由错误

**检查项**:
1. 打开浏览器控制台（F12）
2. 查看 Console 中的错误信息
3. 确认错误是否为 "No match for"

**解决方案**:
```bash
# 1. 清除浏览器缓存
Ctrl + Shift + Delete

# 2. 重新构建前端
cd web
npm run build

# 3. 如果使用开发模式，重启服务器
npm run serve
```

### 问题: JMX 解析失败

**检查项**:
1. 查看浏览器 Network 标签
2. 检查 `/v1/case/{id}/jmx/parse` API 响应
3. 查看后端日志

**可能原因**:
- JMX 文件不存在或路径错误
- JMX 文件格式不支持
- 后端服务未启动

### 问题: 属性编辑器不显示

**检查项**:
1. 确认是否选中了树节点
2. 查看控制台是否有错误
3. 确认元素类型是否支持

**支持的元素类型**:
- ThreadGroup ✅
- HTTPSampler ✅
- JavaSampler ✅
- 其他类型：显示基本属性

### 问题: 保存失败

**检查项**:
1. 验证结构是否通过
2. 检查 Network 标签的 API 响应
3. 确认有 "用例管理" 权限

## 📊 验证数据库

### 检查 JMX 结构是否保存

```sql
-- 查看 jmx_structure 表
SELECT * FROM jmx_structure 
WHERE case_id = [your_case_id] 
ORDER BY version DESC;

-- 查看最新版本
SELECT id, case_id, version, 
       LEFT(structure_json, 100) as json_preview,
       create_time 
FROM jmx_structure 
WHERE case_id = [your_case_id] 
  AND delete_time IS NULL
ORDER BY version DESC 
LIMIT 1;

-- 检查 creation_mode 字段
SELECT id, name, creation_mode, create_time 
FROM `case` 
WHERE delete_time IS NULL 
ORDER BY id DESC 
LIMIT 10;
```

## 🎯 功能完整性检查清单

### 前端功能
- [ ] 用例列表显示正常
- [ ] "编辑 JMX" 按钮可见且可点击
- [ ] 路由跳转正常
- [ ] JMX Builder 页面加载成功
- [ ] 树形结构显示正确
- [ ] 属性编辑器工作正常
- [ ] 模板加载功能正常
- [ ] 验证功能正常
- [ ] 保存功能正常
- [ ] 添加/删除元素功能正常

### 后端 API
- [ ] GET `/v1/case/{id}/jmx/parse` - 解析 JMX
- [ ] GET `/v1/case/{id}/jmx/tree` - 获取结构
- [ ] POST `/v1/case/{id}/jmx/structure` - 保存结构
- [ ] POST `/v1/case/{id}/jmx/generate` - 生成 JMX
- [ ] GET `/v1/case/{id}/jmx/versions` - 版本列表
- [ ] POST `/v1/jmx/builder/create` - 创建空计划
- [ ] POST `/v1/jmx/builder/template/{name}` - 获取模板
- [ ] POST `/v1/jmx/builder/validate` - 验证结构
- [ ] GET `/v1/jmx/builder/templates` - 模板列表

### 数据库
- [ ] `jmx_structure` 表存在
- [ ] `case.creation_mode` 字段存在
- [ ] 数据能正确保存和读取
- [ ] 版本管理正常工作

## 📝 测试报告模板

```
测试日期: [日期]
测试人员: [姓名]
环境: [开发/生产]

测试结果:
- 测试用例 1: [通过/失败] - [备注]
- 测试用例 2: [通过/失败] - [备注]
- 测试用例 3: [通过/失败] - [备注]
...

发现的问题:
1. [问题描述]
   - 重现步骤: [步骤]
   - 预期结果: [描述]
   - 实际结果: [描述]
   - 优先级: [高/中/低]

总体评价:
[功能正常/需要修复]
```

## 🚀 快速测试命令

```bash
# 1. 确保后端运行
cd api
.\start-dev-simple.cmd

# 2. 在新终端，访问 API 测试
curl http://localhost:5000/v1/jmx/builder/templates

# 3. 测试解析 API（需要替换 {id}）
curl http://localhost:5000/v1/case/1/jmx/tree

# 4. 检查数据库
mysql -u root -proot -P 3307 -h localhost easy-jmeter -e "SELECT COUNT(*) as count FROM jmx_structure"
```

## ✅ 测试完成后

请将测试结果反馈：
- 所有功能是否正常工作？
- 是否发现任何 bug？
- 用户体验是否流畅？
- 是否需要改进的地方？

---

**版本**: 1.0
**最后更新**: 2025-11-25

