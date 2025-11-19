-- 简单的ALTER TABLE语句，如果字段已存在会报错，但可以忽略
USE `easy-jmeter`;

ALTER TABLE chain_node_config ADD COLUMN node_host VARCHAR(255) COMMENT '节点主机地址';
ALTER TABLE chain_node_config ADD COLUMN node_port INT DEFAULT 22 COMMENT 'SSH端口';
ALTER TABLE chain_node_config ADD COLUMN node_username VARCHAR(100) COMMENT '远程连接用户名';
ALTER TABLE chain_node_config ADD COLUMN node_password VARCHAR(255) COMMENT '远程连接密码';
ALTER TABLE chain_node_config ADD COLUMN os_type VARCHAR(20) DEFAULT 'LINUX' COMMENT '操作系统类型';
ALTER TABLE chain_node_config ADD COLUMN connection_type VARCHAR(20) DEFAULT 'LOCAL' COMMENT '连接类型';
ALTER TABLE chain_node_config ADD COLUMN ssh_key_path VARCHAR(500) COMMENT 'SSH私钥路径';
ALTER TABLE chain_node_config ADD COLUMN connection_timeout INT DEFAULT 30 COMMENT '连接超时时间';
ALTER TABLE chain_node_config ADD COLUMN read_timeout INT DEFAULT 60 COMMENT '读取超时时间';
ALTER TABLE chain_node_config ADD COLUMN custom_shell_script TEXT COMMENT '自定义Shell脚本';
ALTER TABLE chain_node_config ADD COLUMN use_custom_script TINYINT DEFAULT 0 COMMENT '是否使用自定义脚本';
ALTER TABLE chain_node_config ADD COLUMN parse_method VARCHAR(20) DEFAULT 'JAVA_REGEX' COMMENT '解析方式';

CREATE INDEX idx_node_host ON chain_node_config(node_host);
CREATE INDEX idx_os_type ON chain_node_config(os_type);

