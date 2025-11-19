-- 生成链路延迟测试数据（简化版）
-- 创建时间：2024-11-19

-- 1. 获取现有链路ID（使用user-service-chain这个已存在的链路）
SET @chain_id = (SELECT id FROM chain_trace_config WHERE chain_name = 'user-service-chain' LIMIT 1);

-- 2. 获取现有节点ID
SET @node1 = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id ORDER BY sequence_order LIMIT 1);
SET @node2 = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id ORDER BY sequence_order LIMIT 1 OFFSET 1);
SET @node3 = (SELECT id FROM chain_node_config WHERE chain_id = @chain_id ORDER BY sequence_order LIMIT 1 OFFSET 2);

-- 3. 清空旧的测试数据（如果有）
DELETE FROM chain_latency_data WHERE chain_id = @chain_id;

-- 4. 生成测试数据 - 节点1（API网关，延时50-200ms）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1,
    @chain_id,
    @node1,
    CONCAT('REQ-GW-', LPAD(n, 6, '0')),
    'API网关',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR),
    FLOOR(50 + RAND() * 150),
    IF(RAND() > 0.05, 1, 0),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR)
FROM (
    SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) numbers;

-- 5. 生成测试数据 - 节点2（用户服务，延时100-500ms）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1,
    @chain_id,
    @node2,
    CONCAT('REQ-US-', LPAD(n, 6, '0')),
    '用户服务',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR),
    FLOOR(100 + RAND() * 400),
    IF(RAND() > 0.03, 1, 0),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR)
FROM (
    SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) numbers;

-- 6. 生成测试数据 - 节点3（MySQL数据库，延时200-1000ms）
INSERT INTO chain_latency_data (task_id, chain_id, node_id, request_id, node_name, request_start_time, request_end_time, latency, success, collection_time)
SELECT 
    1,
    @chain_id,
    @node3,
    CONCAT('REQ-DB-', LPAD(n, 6, '0')),
    'MySQL数据库',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR),
    FLOOR(200 + RAND() * 800),
    IF(RAND() > 0.02, 1, 0),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR)
FROM (
    SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) numbers;

-- 7. 修正时间戳（确保end_time > start_time）
UPDATE chain_latency_data 
SET request_end_time = DATE_ADD(request_start_time, INTERVAL latency MILLISECOND)
WHERE chain_id = @chain_id;

-- 8. 为失败的请求添加错误信息
UPDATE chain_latency_data 
SET error_message = CASE 
    WHEN node_name LIKE '%网关%' THEN 'Gateway Timeout'
    WHEN node_name LIKE '%服务%' THEN 'Service Unavailable'
    WHEN node_name LIKE '%数据库%' THEN 'Connection Timeout'
    ELSE 'Unknown Error'
END
WHERE success = 0 AND chain_id = @chain_id;

-- 9. 验证生成的数据
SELECT 
    '数据统计' as title,
    COUNT(*) as total_records,
    SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success_count,
    SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed_count,
    ROUND(AVG(latency), 2) as avg_latency_ms,
    MIN(latency) as min_latency_ms,
    MAX(latency) as max_latency_ms
FROM chain_latency_data
WHERE chain_id = @chain_id;

-- 10. 按节点分组统计
SELECT 
    node_name as '节点名称',
    COUNT(*) as '数据量',
    MIN(latency) as '最小延时(ms)',
    ROUND(AVG(latency), 2) as '平均延时(ms)',
    MAX(latency) as '最大延时(ms)',
    SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as '成功数',
    SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as '失败数'
FROM chain_latency_data
WHERE chain_id = @chain_id
GROUP BY node_name, node_id
ORDER BY node_id;

SELECT '✓ 链路延迟测试数据生成完成！' as status;
