-- ============================================================================
-- v19 项目归属(资金来源)改造：pms_project 归属由 sys_dept 改为自维护树 pms_fund_source
-- 设计文档：项目归属.md（仓库根目录）
-- 幂等：可重复执行
-- 内容：
--   1. 新建归属树表 pms_fund_source
--   2. 种子：长三角、天目湖（固定 ID）
--   3. pms_project.dept_id 改名 owner_id（语义从"部门ID"变为"归属节点ID"）
--   4. 项目清库：仅保留 本地隐私保护项目(长三角) / 全画幅结构光超分辨显微镜系统(天目湖)
--   5. 清理 bom_item 脏数据（引用的项目均已不存在）
-- ============================================================================
SET NAMES utf8mb4;

-- 1. 归属树表 --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pms_fund_source` (
  `id`          bigint       NOT NULL COMMENT '归属ID',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '上级归属ID(0=顶级)',
  `name`        varchar(200) NOT NULL COMMENT '归属名称',
  `sort`        int          DEFAULT 0 COMMENT '显示顺序',
  `status`      tinyint(1)   DEFAULT '1' COMMENT '状态（0停用 1正常）',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by`   bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by`   bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag`    bigint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目归属(资金来源)';

-- 2. 种子数据：长三角 / 天目湖 ------------------------------------------------
INSERT INTO `pms_fund_source` (`id`, `parent_id`, `name`, `sort`, `status`, `create_time`)
SELECT 1900000000000000001, 0, '长三角', 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_fund_source` WHERE `id` = 1900000000000000001);

INSERT INTO `pms_fund_source` (`id`, `parent_id`, `name`, `sort`, `status`, `create_time`)
SELECT 1900000000000000002, 0, '天目湖', 2, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_fund_source` WHERE `id` = 1900000000000000002);

-- 3. pms_project.dept_id -> owner_id（幂等改名） ------------------------------
SET @has_dept_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_project' AND COLUMN_NAME = 'dept_id'
);
SET @ddl := IF(@has_dept_col > 0,
  'ALTER TABLE `pms_project` RENAME COLUMN `dept_id` TO `owner_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 项目清库：仅保留两个真实项目 --------------------------------------------
DELETE FROM `pms_project`
WHERE `id` NOT IN (2089591013069398018, 2090000000000000010);

-- 5. bom_item 脏数据清理（引用的项目均已不存在） -------------------------------
DELETE FROM `pms_bom_item`;

-- 6. 两个保留项目指向新归属 ---------------------------------------------------
UPDATE `pms_project` SET `owner_id` = 1900000000000000001 WHERE `id` = 2089591013069398018;
UPDATE `pms_project` SET `owner_id` = 1900000000000000002 WHERE `id` = 2090000000000000010;

-- 7. 两个项目预算各 100 万（2026-09-23 拍板，临时值） -------------------------
UPDATE `pms_project` SET `budget` = 1000000.00, `used_amount` = 0.00
WHERE `id` IN (2089591013069398018, 2090000000000000010);
