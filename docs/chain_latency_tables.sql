-- 链路延时数据收集功能数据库表结构
-- 创建时间：2024-11-18
-- 版本：1.0.0

-- 创建链路追踪配置表
CREATE TABLE chain_trace_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联的测试任务ID',
    chain_name VARCHAR(100) NOT NULL COMMENT '链路名称',
    chain_description TEXT COMMENT '链路描述',
    version VARCHAR(20) DEFAULT '1.0' COMMENT '链路版本',
    created_by VARCHAR(50) COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    INDEX idx_task_id (task_id),
    INDEX idx_chain_name (chain_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路追踪配置表';

-- 创建链路节点配置表
CREATE TABLE chain_node_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    chain_id BIGINT NOT NULL COMMENT '所属链路ID',
    node_name VARCHAR(50) NOT NULL COMMENT '节点名称',
    node_type VARCHAR(20) DEFAULT 'APPLICATION' COMMENT '节点类型：APPLICATION-应用，DATABASE-数据库，CACHE-缓存等',
    node_description VARCHAR(200) COMMENT '节点描述',
    sequence_order INT DEFAULT 1 COMMENT '执行顺序',
    log_path TEXT COMMENT '日志文件路径',
    log_pattern TEXT COMMENT '日志匹配模式',
    timestamp_pattern TEXT COMMENT '时间戳提取模式',
    latency_pattern TEXT COMMENT '延时提取模式',
    request_id_pattern TEXT COMMENT '请求ID提取模式',
    data_mapping JSON COMMENT '数据映射配置',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间',
    FOREIGN KEY (chain_id) REFERENCES chain_trace_config(id) ON DELETE CASCADE,
    INDEX idx_chain_id (chain_id),
    INDEX idx_node_name (node_name),
    INDEX idx_sequence_order (sequence_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路节点配置表';

-- 创建链路延时数据表
CREATE TABLE chain_latency_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '测试任务ID',
    chain_id BIGINT NOT NULL COMMENT '链路ID',
    node_id BIGINT NOT NULL COMMENT '节点ID',
    request_id VARCHAR(100) NOT NULL COMMENT '请求ID',
    node_name VARCHAR(50) NOT NULL COMMENT '节点名称',
    request_start_time DATETIME NOT NULL COMMENT '请求开始时间戳',
    request_end_time DATETIME NOT NULL COMMENT '请求结束时间戳',
    latency BIGINT NOT NULL COMMENT '延时（毫秒）',
    success TINYINT DEFAULT 1 COMMENT '是否成功：1-成功，0-失败',
    error_message TEXT COMMENT '错误信息',
    extended_data JSON COMMENT '扩展数据（JSON格式）',
    collection_time DATETIME NOT NULL COMMENT '数据收集时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_chain_id (chain_id),
    INDEX idx_node_id (node_id),
    INDEX idx_request_id (request_id),
    INDEX idx_collection_time (collection_time),
    INDEX idx_latency (latency),
    INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路延时数据表';

-- 创建链路告警规则配置表
CREATE TABLE chain_alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    chain_id BIGINT COMMENT '关联链路ID',
    node_id BIGINT COMMENT '关联节点ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(20) NOT NULL COMMENT '规则类型：LATENCY-延时，ERROR_RATE-错误率，AVAILABILITY-可用性',
    threshold_value DECIMAL(10,2) COMMENT '阈值',
    comparison_operator VARCHAR(10) COMMENT '比较操作符：GT-大于，LT-小于，EQ-等于',
    time_window INT COMMENT '时间窗口（秒）',
    alert_level VARCHAR(20) DEFAULT 'WARNING' COMMENT '告警级别：INFO，WARNING，ERROR，CRITICAL',
    notification_config JSON COMMENT '通知配置',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_by VARCHAR(50) COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间',
    INDEX idx_chain_id (chain_id),
    INDEX idx_node_id (node_id),
    INDEX idx_rule_name (rule_name),
    INDEX idx_rule_type (rule_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路告警规则配置表';

-- 创建链路告警历史表
CREATE TABLE chain_alert_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_id BIGINT NOT NULL COMMENT '告警规则ID',
    chain_id BIGINT NOT NULL COMMENT '链路ID',
    node_id BIGINT COMMENT '节点ID',
    alert_level VARCHAR(20) NOT NULL COMMENT '告警级别',
    alert_title VARCHAR(200) NOT NULL COMMENT '告警标题',
    alert_message TEXT NOT NULL COMMENT '告警消息',
    metric_value DECIMAL(10,2) COMMENT '触发告警的指标值',
    threshold_value DECIMAL(10,2) COMMENT '阈值',
    alert_time DATETIME NOT NULL COMMENT '告警时间',
    resolved_time DATETIME COMMENT '解决时间',
    status TINYINT DEFAULT 1 COMMENT '告警状态：1-活跃，0-已解决',
    notification_sent TINYINT DEFAULT 0 COMMENT '是否已发送通知：1-是，0-否',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (rule_id) REFERENCES chain_alert_rule(id) ON DELETE CASCADE,
    INDEX idx_rule_id (rule_id),
    INDEX idx_chain_id (chain_id),
    INDEX idx_node_id (node_id),
    INDEX idx_alert_time (alert_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路告警历史表';

-- 添加外键约束
ALTER TABLE chain_latency_data ADD CONSTRAINT fk_latency_chain_id
    FOREIGN KEY (chain_id) REFERENCES chain_trace_config(id) ON DELETE CASCADE;

ALTER TABLE chain_latency_data ADD CONSTRAINT fk_latency_node_id
    FOREIGN KEY (node_id) REFERENCES chain_node_config(id) ON DELETE CASCADE;

ALTER TABLE chain_alert_rule ADD CONSTRAINT fk_alert_chain_id
    FOREIGN KEY (chain_id) REFERENCES chain_trace_config(id) ON DELETE CASCADE;

ALTER TABLE chain_alert_rule ADD CONSTRAINT fk_alert_node_id
    FOREIGN KEY (node_id) REFERENCES chain_node_config(id) ON DELETE SET NULL;

-- 插入示例数据（可选）
INSERT INTO chain_trace_config (task_id, chain_name, chain_description, version, created_by, status) VALUES
(1, 'user-service-chain', '用户服务调用链路', '1.0', 'admin', 1),
(2, 'order-service-chain', '订单服务调用链路', '1.0', 'admin', 1);

INSERT INTO chain_node_config (chain_id, node_name, node_type, node_description, sequence_order, log_path, log_pattern, timestamp_pattern, latency_pattern, request_id_pattern, status) VALUES
(1, 'gateway', 'APPLICATION', 'API网关', 1, '/var/log/gateway/app.log', '.*\\[INFO\\].*', '\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\]', 'latency=(\\d+)ms', 'requestId=([^\\s]+)', 1),
(1, 'user-service', 'APPLICATION', '用户服务', 2, '/var/log/user-service/app.log', '.*\\[INFO\\].*', '\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\]', 'duration=(\\d+)ms', 'traceId=([^\\s]+)', 1),
(1, 'mysql-db', 'DATABASE', '用户数据库', 3, '/var/log/mysql/slow.log', '.*Query_time.*', 'Time: (\\d+).*', 'Query_time: (\\d+).*', NULL, 1);

INSERT INTO chain_alert_rule (chain_id, node_id, rule_name, rule_type, threshold_value, comparison_operator, time_window, alert_level, status, created_by) VALUES
(1, 1, '网关延时过高', 'LATENCY', 1000.00, 'GT', 60, 'WARNING', 1, 'admin'),
(1, 2, '用户服务延时过高', 'LATENCY', 500.00, 'GT', 60, 'WARNING', 1, 'admin'),
(1, 3, '数据库查询延时过高', 'LATENCY', 200.00, 'GT', 60, 'ERROR', 1, 'admin');