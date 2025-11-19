-- 生成链路延迟测试数据
-- 创建时间：2024-11-19
-- 说明：为链路追踪功能生成测试数据，用于前端展示和测试

-- 1. 首先确保有测试用的链路配置
-- 如果没有链路配置，先插入一条测试链路
INSERT INTO chain_trace_config (task_id, chain_name, chain_description, version, created_by, status) 
SELECT 1, 'test-service-chain', '测试服务调用链路', '1.0', 'admin', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM chain_trace_config WHERE chain_name = 'test-service-chain'
);

-- 获取链路ID（假设是最新创建的）
SET @chain_id = (SELECT id FROM chain_trace_config WHERE chain_name = 'test-service-chain' LIMIT 1);

-- 2. 插入节点配置（如果不存在）
INSERT IGNORE INTO chain_node_config (chain_id, node_name, node_type, node_description, sequence_order, log_path, status) VALUES
(@chain_id, 'API网关', 'APPLICATION', 'API Gateway服务', 1, '/var/log/gateway/app.log', 1),
(@chain_id, '用户服务', 'APPLICATION', 'User Service微服务', 2, '/var/log/user-service/app.log', 1),
(@chain_id, 'MySQL数据库', 'DATABASE', '用户信息数据库', 3, '/var/log/mysql/slow.log', 1),
(@chain_id, 'Redis缓存', 'CACHE', '用户缓存Redis', 4, '/var/log/redis/redis.log', 1);

-- 获取节点ID
SET @node_gateway = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id AND node_name = 'API网关' LIMIT 1);
SET @node_user_service = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id AND node_name = '用户服务' LIMIT 1);
SET @node_mysql = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id AND node_name = 'MySQL数据库' LIMIT 1);
SET @node_redis = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id AND node_name = 'Redis缓存' LIMIT 1);

-- 3. 生成链路延时数据
-- 为最近24小时生成测试数据，每个节点每小时生成10-20条数据

-- API网关节点数据（延时较低：50-200ms）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1 as task_id,
    @chain_id as chain_id,
    @node_gateway as node_id,
    CONCAT('REQ-GW-', LPAD(seq, 6, '0')) as request_id,
    'API网关' as node_name,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as request_start_time,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) + INTERVAL FLOOR(50 + RAND() * 150) MILLISECOND as request_end_time,
    FLOOR(50 + RAND() * 150) as latency,
    IF(RAND() > 0.05, 1, 0) as success,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as collection_time
FROM (
    SELECT @row := @row + 1 as seq
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT @row := 0) r
    LIMIT 300
) numbers;

-- 用户服务节点数据（延时中等：100-500ms）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1 as task_id,
    @chain_id as chain_id,
    @node_user_service as node_id,
    CONCAT('REQ-US-', LPAD(seq, 6, '0')) as request_id,
    '用户服务' as node_name,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as request_start_time,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) + INTERVAL FLOOR(100 + RAND() * 400) MILLISECOND as request_end_time,
    FLOOR(100 + RAND() * 400) as latency,
    IF(RAND() > 0.03, 1, 0) as success,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as collection_time
FROM (
    SELECT @row2 := @row2 + 1 as seq
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT @row2 := 0) r
    LIMIT 300
) numbers;

-- MySQL数据库节点数据（延时较高：200-1000ms，偶尔有慢查询）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1 as task_id,
    @chain_id as chain_id,
    @node_mysql as node_id,
    CONCAT('REQ-DB-', LPAD(seq, 6, '0')) as request_id,
    'MySQL数据库' as node_name,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as request_start_time,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) + INTERVAL FLOOR(200 + RAND() * 800 + IF(RAND() > 0.9, 1000, 0)) MILLISECOND as request_end_time,
    FLOOR(200 + RAND() * 800 + IF(RAND() > 0.9, 1000, 0)) as latency,
    IF(RAND() > 0.02, 1, 0) as success,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as collection_time
FROM (
    SELECT @row3 := @row3 + 1 as seq
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT @row3 := 0) r
    LIMIT 300
) numbers;

-- Redis缓存节点数据（延时很低：5-30ms）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1 as task_id,
    @chain_id as chain_id,
    @node_redis as node_id,
    CONCAT('REQ-RD-', LPAD(seq, 6, '0')) as request_id,
    'Redis缓存' as node_name,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as request_start_time,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) + INTERVAL FLOOR(5 + RAND() * 25) MILLISECOND as request_end_time,
    FLOOR(5 + RAND() * 25) as latency,
    IF(RAND() > 0.01, 1, 0) as success,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR) as collection_time
FROM (
    SELECT @row4 := @row4 + 1 as seq
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT @row4 := 0) r
    LIMIT 300
) numbers;

-- 4. 生成一些失败请求的错误信息
UPDATE chain_latency_data 
SET error_message = CASE 
    WHEN node_id = @node_gateway THEN 'Gateway Timeout'
    WHEN node_id = @node_user_service THEN 'Service Unavailable'
    WHEN node_id = @node_mysql THEN 'Connection Timeout'
    WHEN node_id = @node_redis THEN 'Redis Connection Failed'
END
WHERE success = 0 AND error_message IS NULL;

-- 5. 更新request_end_time以确保逻辑正确
UPDATE chain_latency_data 
SET request_end_time = DATE_ADD(request_start_time, INTERVAL latency MILLISECOND)
WHERE request_end_time < request_start_time;

-- 验证数据
SELECT 
    n.node_name as '节点名称',
    COUNT(*) as '数据量',
    MIN(l.latency) as '最小延时(ms)',
    AVG(l.latency) as '平均延时(ms)',
    MAX(l.latency) as '最大延时(ms)',
    SUM(CASE WHEN l.success = 1 THEN 1 ELSE 0 END) as '成功数',
    SUM(CASE WHEN l.success = 0 THEN 1 ELSE 0 END) as '失败数',
    ROUND(SUM(CASE WHEN l.success = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as '成功率(%)'
FROM chain_latency_data l
JOIN chain_node_config n ON l.node_id = n.id
WHERE l.chain_id = @chain_id
GROUP BY n.node_name, n.sequence_order
ORDER BY n.sequence_order;

-- 显示最近的10条数据
SELECT 
    l.id,
    l.request_id as '请求ID',
    n.node_name as '节点',
    l.latency as '延时(ms)',
    CASE WHEN l.success = 1 THEN '成功' ELSE '失败' END as '状态',
    l.error_message as '错误信息',
    l.collection_time as '收集时间'
FROM chain_latency_data l
JOIN chain_node_config n ON l.node_id = n.id
WHERE l.chain_id = @chain_id
ORDER BY l.collection_time DESC
LIMIT 10;

-- 完成提示
SELECT 
    CONCAT('✓ 成功生成链路延时测试数据！') as '状态',
    CONCAT('链路ID: ', @chain_id) as '链路信息',
    CONCAT('总数据量: ', COUNT(*), ' 条') as '数据统计'
FROM chain_latency_data
WHERE chain_id = @chain_id;
