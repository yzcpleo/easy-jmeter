# 性能指标路径功能测试验证

## 测试步骤

### 1. 后端编译测试
```bash
cd api
mvn clean compile -DskipTests
```
预期结果：编译成功，无错误

### 2. 数据库验证
```sql
-- 检查表是否存在
SHOW TABLES LIKE 'chain_performance_metric_path';

-- 检查表结构
DESC chain_performance_metric_path;

-- 应该包含以下字段：
-- id, chain_id, metric_name, metric_type, start_node_id, start_node_name, 
-- start_time_field, end_node_id, end_node_name, end_time_field, 
-- description, status, created_by, created_time, updated_by, updated_time, delete_time
```

### 3. 前端API测试（浏览器控制台）

#### 测试创建路径
```javascript
// 在浏览器控制台执行
const testData = {
  chainId: 3,
  metricName: "订单上行穿透时延",
  metricType: "ORDER_UPSTREAM",
  startNodeId: 1,  // 确保这个节点存在
  startTimeField: "REQUEST_START_TIME",
  endNodeId: 2,   // 确保这个节点存在且不同于起点
  endTimeField: "REQUEST_END_TIME",
  description: "测试路径",
  status: 1
}

// 使用axios直接测试
import _axios from '@/lin/plugin/axios'
_axios({
  method: 'post',
  url: '/v1/chain/metric-path',
  data: JSON.stringify(testData),
  headers: {
    'Content-Type': 'application/json',
  },
}).then(res => {
  console.log('创建成功:', res)
}).catch(err => {
  console.error('创建失败:', err.response?.data || err)
})
```

### 4. 后端日志检查
检查后端日志，查看：
- 请求是否到达Controller
- DTO是否正确解析
- 验证错误的具体原因

### 5. 常见问题排查

#### 问题1：所有字段验证失败
**原因**：后端无法解析JSON数据
**解决**：
- 检查Content-Type是否正确设置为application/json
- 检查数据是否被正确序列化
- 检查axios拦截器是否修改了数据

#### 问题2：字段名不匹配
**原因**：前端发送camelCase，后端期望snake_case
**解决**：后端DTO使用camelCase，应该匹配

#### 问题3：起点和终点相同
**原因**：用户选择了相同的节点
**解决**：后端已有验证，会返回错误提示

## 验证清单

- [ ] 后端编译成功
- [ ] 数据库表结构正确
- [ ] 前端页面可以正常加载
- [ ] 可以查询链路列表
- [ ] 可以查询节点列表
- [ ] 可以创建路径（起点和终点不同）
- [ ] 创建路径时起点和终点相同会报错
- [ ] 可以编辑路径
- [ ] 可以删除路径
- [ ] 可以查询路径列表
- [ ] 可以查询路径时延统计

