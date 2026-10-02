SET NAMES utf8mb4;
-- ============================================================================
-- v23 多人备用金拆账 + 人工资金流水（2026-09-24，设计见 docs/9.20/设计探讨-备用金与报销流程.md §1/§4/§5）
-- 1) pms_procurement_request 加列：reserve_people_json（备用金人+扣款顺序 JSON）、use_user_id/use_user_name（使用人）
-- 2) pms_fund_flow 加列：fund_status（仅人工备用金流水用，采购流水为空、状态仍挂申请单）
-- 3) 新增按钮 1801175「资金人工登记」perms=procurement:fund:manual，挂资金管理菜单 1801140 下，采购经理角色补绑
-- ============================================================================

-- 1) 采购申请加列 ------------------------------------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'reserve_people_json');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `pms_procurement_request` ADD COLUMN `reserve_people_json` VARCHAR(1000) NULL COMMENT ''备用金人+扣款顺序JSON：[{"personId":1,"personName":"x"},...]''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'use_user_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `pms_procurement_request` ADD COLUMN `use_user_id` BIGINT NULL COMMENT ''使用人ID''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'use_user_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `pms_procurement_request` ADD COLUMN `use_user_name` VARCHAR(64) NULL COMMENT ''使用人姓名''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 资金流水加列 ------------------------------------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_fund_flow' AND COLUMN_NAME = 'fund_status');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `pms_fund_flow` ADD COLUMN `fund_status` VARCHAR(32) NULL COMMENT ''资金状态（仅人工备用金流水用：purchased_unreimbursed/reimbursed_unpaid/reimbursed_paid）''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 菜单按钮：资金人工登记（资金管理菜单 1801140 下） ----------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `remark`)
SELECT 1801175, '资金人工登记', 1801140, 7, '#', 'N', 'Y', 'F', '0', '0', 'procurement:fund:manual', '#', NOW(), '人工登记资金消耗（自购备用金/对公直支）+ 人工流水资金状态流转'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1801175);

-- 采购经理角色（1761300000000000014）补绑
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1761300000000000014, 1801175
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1761300000000000014 AND `menu_id` = 1801175);
