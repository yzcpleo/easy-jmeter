-- 性能指标路径配置表
-- 用于定义链路中的性能指标路径，如订单上行穿透时延、订单下行时延、行情时延等
-- 创建时间：2025-01-XX
-- 使用方法：mysql -uroot -proot -P3307 -h127.0.0.1 easy-jmeter < init_db/add_performance_metric_path_table.sql

USE `easy-jmeter`;

CREATE TABLE IF NOT EXISTS chain_performance_metric_path (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    chain_id BIGINT NOT NULL COMMENT '所属链路ID',
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称，如：订单上行穿透时延、订单下行时延、行情时延',
    metric_type VARCHAR(50) NOT NULL COMMENT '指标类型：ORDER_UPSTREAM-订单上行，ORDER_DOWNSTREAM-订单下行，MARKET-行情等',
    start_node_id BIGINT NOT NULL COMMENT '起点节点ID',
    start_node_name VARCHAR(50) NOT NULL COMMENT '起点节点名称',
    start_time_field VARCHAR(20) DEFAULT 'REQUEST_START_TIME' COMMENT '起点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间',
    end_node_id BIGINT NOT NULL COMMENT '终点节点ID',
    end_node_name VARCHAR(50) NOT NULL COMMENT '终点节点名称',
    end_time_field VARCHAR(20) DEFAULT 'REQUEST_END_TIME' COMMENT '终点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间',
    description TEXT COMMENT '路径描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_by VARCHAR(50) COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间',
    FOREIGN KEY (chain_id) REFERENCES chain_trace_config(id) ON DELETE CASCADE,
    FOREIGN KEY (start_node_id) REFERENCES chain_node_config(id) ON DELETE CASCADE,
    FOREIGN KEY (end_node_id) REFERENCES chain_node_config(id) ON DELETE CASCADE,
    INDEX idx_chain_id (chain_id),
    INDEX idx_metric_type (metric_type),
    INDEX idx_start_node_id (start_node_id),
    INDEX idx_end_node_id (end_node_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能指标路径配置表';

