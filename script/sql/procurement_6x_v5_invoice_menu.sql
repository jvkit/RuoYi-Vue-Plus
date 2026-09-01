SET NAMES utf8mb4;
-- ============================================================
-- 采购 v5：发票台账菜单（挂在采购管理下）+ 普通用户权限
-- 幂等：可重复执行
-- ============================================================

SET @admin_id = 1761100000000000001;
SET @dept_id  = 1761000000000000103;
SET @common_user_role_id = 1761300000000000013;

-- ---------- 1. 发票台账菜单 ----------
DELETE FROM sys_menu WHERE menu_id = 1801160;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801160, '发票台账', 1801000, 15, 'invoice', 'procurement/invoice/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:invoice:list', 'ticket', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '采购发票台账菜单');

-- 查询按钮
DELETE FROM sys_menu WHERE menu_id = 1801161;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801161, '发票台账查询', 1801160, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:invoice:query', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

-- 删除按钮
DELETE FROM sys_menu WHERE menu_id = 1801162;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801162, '发票台账删除', 1801160, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:invoice:remove', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

-- ---------- 2. 普通用户绑定（可见发票台账 + 查询，删除留给管理员） ----------
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id IN (1801160, 1801161, 1801162);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (@common_user_role_id, 1801160),
  (@common_user_role_id, 1801161);

-- ---------- 3. 校验 ----------
SELECT menu_id, menu_name, parent_id, path, component, perms FROM sys_menu WHERE menu_id BETWEEN 1801160 AND 1801162 ORDER BY menu_id;
SELECT role_id, menu_id FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id IN (1801160, 1801161, 1801162) ORDER BY menu_id;
