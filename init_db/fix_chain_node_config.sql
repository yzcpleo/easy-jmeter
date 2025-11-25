-- ============================================
-- 数据库修复脚本：修复 chain_node_config 表
-- ============================================
-- 问题1：缺少 data_mapping 字段
-- 问题2：可能缺少其他远程节点相关字段
-- ============================================

-- 检查并添加 data_mapping 字段
-- MySQL 5.7+ 支持 JSON 类型
ALTER TABLE `chain_node_config` 
ADD COLUMN IF NOT EXISTS `data_mapping` JSON COMMENT '数据映射配置（JSON格式）' AFTER `request_id_pattern`;

-- 如果 MySQL 版本低于 5.7，使用下面的命令（注释掉上面那行，取消注释下面这行）
-- ALTER TABLE `chain_node_config` 
-- ADD COLUMN IF NOT EXISTS `data_mapping` TEXT COMMENT '数据映射配置（JSON格式）' AFTER `request_id_pattern`;

-- 检查并添加远程节点相关字段（如果不存在的话）
ALTER TABLE `chain_node_config` 
ADD COLUMN IF NOT EXISTS `node_host` VARCHAR(255) COMMENT '节点主机地址（IP或域名），为空表示本地节点' AFTER `data_mapping`,
ADD COLUMN IF NOT EXISTS `node_port` INT DEFAULT 22 COMMENT 'SSH端口（Linux）或RDP端口（Windows），默认22' AFTER `node_host`,
ADD COLUMN IF NOT EXISTS `node_username` VARCHAR(100) COMMENT '远程连接用户名' AFTER `node_port`,
ADD COLUMN IF NOT EXISTS `node_password` VARCHAR(255) COMMENT '远程连接密码（加密存储）' AFTER `node_username`,
ADD COLUMN IF NOT EXISTS `os_type` VARCHAR(20) DEFAULT 'LINUX' COMMENT '操作系统类型：LINUX- Linux系统，WINDOWS- Windows系统' AFTER `node_password`,
ADD COLUMN IF NOT EXISTS `connection_type` VARCHAR(20) DEFAULT 'LOCAL' COMMENT '连接类型：LOCAL-本地，SSH- SSH连接，RDP- RDP连接（Windows）' AFTER `os_type`,
ADD COLUMN IF NOT EXISTS `ssh_key_path` VARCHAR(500) COMMENT 'SSH私钥路径（可选，优先使用密钥认证）' AFTER `connection_type`,
ADD COLUMN IF NOT EXISTS `connection_timeout` INT DEFAULT 30 COMMENT '连接超时时间（秒）' AFTER `ssh_key_path`,
ADD COLUMN IF NOT EXISTS `read_timeout` INT DEFAULT 60 COMMENT '读取超时时间（秒）' AFTER `connection_timeout`,
ADD COLUMN IF NOT EXISTS `custom_shell_script` TEXT COMMENT '自定义Shell脚本' AFTER `read_timeout`,
ADD COLUMN IF NOT EXISTS `use_custom_script` TINYINT DEFAULT 0 COMMENT '是否使用自定义脚本：1-是，0-否' AFTER `custom_shell_script`,
ADD COLUMN IF NOT EXISTS `parse_method` VARCHAR(20) DEFAULT 'JAVA_REGEX' COMMENT '解析方式：AUTO-自动，AWK-AWK脚本，JAVA_REGEX-Java正则表达式' AFTER `use_custom_script`;

-- 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS `idx_node_host` ON `chain_node_config`(`node_host`);
CREATE INDEX IF NOT EXISTS `idx_os_type` ON `chain_node_config`(`os_type`);

-- 验证表结构
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'chain_node_config'
ORDER BY ORDINAL_POSITION;

