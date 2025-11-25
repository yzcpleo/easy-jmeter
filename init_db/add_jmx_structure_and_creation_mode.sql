-- Migration: Add JMX structure table and creation_mode column
-- Description: Support JMX parsing, editing and builder mode
-- Date: 2025-11-25

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Add creation_mode column to case table
-- ----------------------------
ALTER TABLE `case`
ADD COLUMN `creation_mode` VARCHAR(20) DEFAULT 'UPLOAD' COMMENT 'JMX creation mode: UPLOAD or BUILDER' AFTER `description`;

-- ----------------------------
-- JMX structure table
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmx_structure` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `case_id` int(11) NOT NULL COMMENT 'Associated case ID',
    `structure_json` LONGTEXT NOT NULL COMMENT 'JMX structure in JSON format',
    `version` int(11) NOT NULL DEFAULT 1 COMMENT 'Version number for tracking changes',
    `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    `delete_time` datetime(3) DEFAULT NULL COMMENT 'Soft delete time',
    PRIMARY KEY (`id`),
    KEY `idx_case_id` (`case_id`),
    KEY `idx_version` (`version`),
    CONSTRAINT `fk_jmx_structure_case` FOREIGN KEY (`case_id`) REFERENCES `case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='JMX structure storage for parsing and editing';

SET FOREIGN_KEY_CHECKS = 1;

