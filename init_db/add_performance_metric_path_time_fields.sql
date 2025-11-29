-- 为已存在的性能指标路径表添加时间字段配置列
-- 如果表已存在，执行此脚本添加新字段

ALTER TABLE chain_performance_metric_path 
ADD COLUMN IF NOT EXISTS start_time_field VARCHAR(20) DEFAULT 'REQUEST_START_TIME' 
COMMENT '起点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间' AFTER start_node_name;

ALTER TABLE chain_performance_metric_path 
ADD COLUMN IF NOT EXISTS end_time_field VARCHAR(20) DEFAULT 'REQUEST_END_TIME' 
COMMENT '终点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间' AFTER end_node_name;

