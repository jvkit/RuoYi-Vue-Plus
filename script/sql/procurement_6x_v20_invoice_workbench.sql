-- =============================================================
-- procurement_6x_v20_invoice_workbench.sql（2026-09-23）
-- 发票上传工作台（后端 PmsInvoiceWorkbenchController）配套 SQL
--
-- 内容：
--   1. pms_procurement_request 加 3 列（invoice_done_flag/time/by，标记发票上传完成留痕）
--   2. 菜单：采购管理目录下新增 C 菜单「上传发票」(1801170, procurement/invoiceWorkbench/index)
--      + F 按钮 工作台查询(1801171) / 标记完成(1801172)
--   3. 角色绑定：采购经理(1761300000000000014) 绑 1801170~1801172
--
-- 幂等：可重复执行（加列用 information_schema 判断；菜单/角色绑定先删后插）
-- =============================================================
SET NAMES utf8mb4;

SET @admin_id          := 1761100000000000001;
SET @dept_id           := 1761000000000000103;
SET @role_manager_id   := 1761300000000000014;  -- 采购经理

-- ============================================================
-- 1. pms_procurement_request 加列：发票上传完成标志 + 留痕
-- ============================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'invoice_done_flag');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN invoice_done_flag tinyint(1) DEFAULT 0 COMMENT ''发票上传完成标志(0未完成 1已完成)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'invoice_done_time');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN invoice_done_time datetime DEFAULT NULL COMMENT ''标记发票上传完成时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'invoice_done_by');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN invoice_done_by bigint DEFAULT NULL COMMENT ''标记发票上传完成的操作人ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 菜单：上传发票工作台（挂在「采购管理」目录下，目录按 path+menu_type 动态查找）
-- ============================================================
SET @parent_id := (SELECT menu_id FROM sys_menu WHERE path = 'procurement' AND menu_type = 'M' ORDER BY menu_id LIMIT 1);

-- 找不到采购目录则跳过菜单与角色绑定（避免挂到根节点）
SET @menu_ddl := IF(@parent_id IS NULL, 'SELECT 1',
  'DELETE FROM sys_menu WHERE menu_id IN (1801170, 1801171, 1801172)');
PREPARE stmt FROM @menu_ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @menu_ddl := IF(@parent_id IS NULL, 'SELECT 1',
  'INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES '
  '(1801170, ''上传发票'', @parent_id, 16, ''invoiceWorkbench'', ''procurement/invoiceWorkbench/index'', NULL, ''N'', ''Y'', ''C'', ''0'', ''0'', ''procurement:workbench:list'', ''ep:document'', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, ''发票上传工作台（已验收申请的逐单跟进）'')');
PREPARE stmt FROM @menu_ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @menu_ddl := IF(@parent_id IS NULL, 'SELECT 1',
  'INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES '
  '(1801171, ''工作台查询'', 1801170, 1, ''#'', NULL, NULL, ''N'', ''Y'', ''F'', ''0'', ''0'', ''procurement:workbench:query'', ''#'', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, ''''), '
  '(1801172, ''标记完成'', 1801170, 2, ''#'', NULL, NULL, ''N'', ''Y'', ''F'', ''0'', ''0'', ''procurement:workbench:edit'', ''#'', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, ''标记/取消标记发票上传完成'')');
PREPARE stmt FROM @menu_ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 角色绑定：采购经理可见工作台并可标记完成
-- ============================================================
SET @menu_ddl := IF(@parent_id IS NULL, 'SELECT 1',
  'DELETE FROM sys_role_menu WHERE role_id = @role_manager_id AND menu_id IN (1801170, 1801171, 1801172)');
PREPARE stmt FROM @menu_ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @menu_ddl := IF(@parent_id IS NULL, 'SELECT 1',
  'INSERT INTO sys_role_menu (role_id, menu_id) VALUES '
  '(@role_manager_id, 1801170), '
  '(@role_manager_id, 1801171), '
  '(@role_manager_id, 1801172)');
PREPARE stmt FROM @menu_ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 验证（重复执行本脚本后以下查询结果不变即幂等）
-- ============================================================
-- SELECT menu_id, menu_name, parent_id, path, component, perms FROM sys_menu WHERE menu_id BETWEEN 1801170 AND 1801172 ORDER BY menu_id;
-- SELECT role_id, menu_id FROM sys_role_menu WHERE role_id = 1761300000000000014 AND menu_id BETWEEN 1801170 AND 1801172 ORDER BY menu_id;
