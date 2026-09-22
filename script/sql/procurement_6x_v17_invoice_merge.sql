-- ============================================================
-- 采购 v17：发票入口合并 + 报销闭环前置
--  1. pms_reimbursement 加 amount 列（报销金额快照，= 关联申请金额）
--  2. 报销按钮权限补齐（:edit/:remove 已有菜单 1801134/1801135，绑给采购经理+admin 由既有脚本覆盖，
--     此处补 admin/superadmin 绑定兜底）
--  3. 干掉老发票管理模块菜单（1804000~1804012，与发票台账共用 invoice_info 表，无数据迁移）
--  4. 发票台账加「上传发票」按钮权限 procurement:invoice:upload
-- 幂等：可重复执行
-- ============================================================
SET NAMES utf8mb4;

SET @admin_id = 1761100000000000001;
SET @dept_id  = 1761000000000000103;
SET @superadmin_role_id = 1761300000000000001;
SET @manager_role_id    = 1761300000000000014;

-- ---------- 1. pms_reimbursement 加 amount 列 ----------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_reimbursement' AND COLUMN_NAME = 'amount');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE pms_reimbursement ADD COLUMN amount decimal(18,2) NULL COMMENT ''报销金额快照（=关联申请金额）'' AFTER applicant',
  'SELECT ''pms_reimbursement.amount 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 发票台账「上传发票」按钮 ----------
DELETE FROM sys_menu WHERE menu_id = 1801163;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801163, '发票上传', 1801160, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:invoice:upload', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '发票台账上传发票按钮');

-- 绑给 superadmin / 采购经理（普通用户不可见台账，维持 v5 决定）
DELETE FROM sys_role_menu WHERE role_id IN (@superadmin_role_id, @manager_role_id) AND menu_id = 1801163;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (@superadmin_role_id, 1801163),
  (@manager_role_id, 1801163);

-- ---------- 3. 干掉老发票管理模块菜单（1804000~1804012） ----------
-- 老模块（views/invoice + ruoyi-invoice）与新台账共用 invoice_info 表，菜单摘除无数据影响
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 1804000 AND 1804012;
DELETE FROM sys_menu WHERE menu_id BETWEEN 1804000 AND 1804012;

-- ---------- 4. 报销按钮权限兜底：superadmin 补绑 ----------
DELETE FROM sys_role_menu WHERE role_id = @superadmin_role_id AND menu_id IN (1801130, 1801131, 1801132, 1801133, 1801134, 1801135);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (@superadmin_role_id, 1801130),
  (@superadmin_role_id, 1801131),
  (@superadmin_role_id, 1801132),
  (@superadmin_role_id, 1801133),
  (@superadmin_role_id, 1801134),
  (@superadmin_role_id, 1801135);
