-- 更新现有节点数据，为新字段设置默认值
-- 如果字段已存在，此脚本不会报错（使用IF EXISTS检查）

-- 更新现有节点的默认值
UPDATE chain_node_config 
SET 
    os_type = COALESCE(os_type, 'LINUX'),
    connection_type = COALESCE(connection_type, 'LOCAL'),
    parse_method = COALESCE(parse_method, 'JAVA_REGEX'),
    node_port = COALESCE(node_port, 22),
    connection_timeout = COALESCE(connection_timeout, 30),
    read_timeout = COALESCE(read_timeout, 60),
    use_custom_script = COALESCE(use_custom_script, 0)
WHERE 
    os_type IS NULL 
    OR connection_type IS NULL 
    OR parse_method IS NULL
    OR (node_port IS NULL AND node_host IS NOT NULL)
    OR connection_timeout IS NULL
    OR read_timeout IS NULL
    OR use_custom_script IS NULL;

