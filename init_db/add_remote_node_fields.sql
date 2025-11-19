-- 为链路节点配置表添加远程连接字段
-- 支持Windows和Linux远程节点

ALTER TABLE chain_node_config 
ADD COLUMN IF NOT EXISTS node_host VARCHAR(255) COMMENT '节点主机地址（IP或域名），为空表示本地节点',
ADD COLUMN IF NOT EXISTS node_port INT DEFAULT 22 COMMENT 'SSH端口（Linux）或RDP端口（Windows），默认22',
ADD COLUMN IF NOT EXISTS node_username VARCHAR(100) COMMENT '远程连接用户名',
ADD COLUMN IF NOT EXISTS node_password VARCHAR(255) COMMENT '远程连接密码（加密存储）',
ADD COLUMN IF NOT EXISTS os_type VARCHAR(20) DEFAULT 'LINUX' COMMENT '操作系统类型：LINUX- Linux系统，WINDOWS- Windows系统',
ADD COLUMN IF NOT EXISTS connection_type VARCHAR(20) DEFAULT 'LOCAL' COMMENT '连接类型：LOCAL-本地，SSH- SSH连接，RDP- RDP连接（Windows）',
ADD COLUMN IF NOT EXISTS ssh_key_path VARCHAR(500) COMMENT 'SSH私钥路径（可选，优先使用密钥认证）',
ADD COLUMN IF NOT EXISTS connection_timeout INT DEFAULT 30 COMMENT '连接超时时间（秒）',
ADD COLUMN IF NOT EXISTS read_timeout INT DEFAULT 60 COMMENT '读取超时时间（秒）',
ADD COLUMN IF NOT EXISTS custom_shell_script TEXT COMMENT '自定义Shell脚本（用于解析非标准格式日志，输出格式：requestId|timestamp|latency|originalLine）',
ADD COLUMN IF NOT EXISTS use_custom_script TINYINT DEFAULT 0 COMMENT '是否使用自定义脚本：1-是，0-否（使用AWK或正则）',
ADD COLUMN IF NOT EXISTS parse_method VARCHAR(20) DEFAULT 'JAVA_REGEX' COMMENT '解析方式：AUTO-自动（Linux用AWK，Windows用Java正则），AWK-AWK脚本，JAVA_REGEX-Java正则表达式';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_node_host ON chain_node_config(node_host);
CREATE INDEX IF NOT EXISTS idx_os_type ON chain_node_config(os_type);

