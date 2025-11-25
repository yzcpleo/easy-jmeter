-- Migration: Add JMX asset management
-- Description: Introduce reusable JMX asset table and allow jmx_structure to reference assets
-- Date: 2025-11-25

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- JMX asset table
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmx_asset` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT 'JMX asset name',
    `project_id` int(11) NOT NULL COMMENT 'Associated project',
    `description` varchar(500) DEFAULT NULL COMMENT 'Description',
    `jmx_file_id` int(11) DEFAULT NULL COMMENT 'Generated JMX file (jfile id)',
    `creator` int(11) DEFAULT NULL COMMENT 'Creator user id',
    `creation_mode` varchar(20) DEFAULT 'UPLOAD' COMMENT 'UPLOAD or BUILDER',
    `latest_structure_version` int(11) DEFAULT 0 COMMENT 'Latest structure version',
    `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `delete_time` datetime(3) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reusable JMX assets for cases';

-- ----------------------------
-- Extend jmx_structure to link assets
-- ----------------------------
ALTER TABLE `jmx_structure`
    MODIFY COLUMN `case_id` int(11) NULL COMMENT 'Case owner',
    ADD COLUMN `asset_id` int(11) NULL COMMENT 'Asset owner' AFTER `case_id`,
    ADD CONSTRAINT `fk_jmx_structure_asset` FOREIGN KEY (`asset_id`) REFERENCES `jmx_asset`(`id`) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;


