SET NAMES utf8mb4;
-- ============================================================
-- 服务器 OA 菜单/字典补全脚本（ry-vue-6x）
-- 用于补齐服务器相对于本地的缺失项：
--   1. PRIME AI 外链跳转菜单
--   2. 项目管理 菜单名重命名（服务器仍为"采购项目"）
--   3. test 测试菜单及按钮
--   4. 采购订单菜单隐藏（visible=1）
-- 幂等执行：先删后插 或 UPDATE 条件判断
-- ============================================================

SET @admin_id = 1761100000000000001;

-- ============================================================
-- 1. PRIME AI 外链跳转菜单
--    点击后新标签页打开 PRIME AI（服务器 172.16.16.110:3305）
-- ============================================================
DELETE FROM sys_menu WHERE menu_id = 1805000;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1805000, 'PRIME AI', 0, 7, 'http://172.16.16.110:3305', NULL, NULL, 'Y', 'N', 'C', '0', '0', NULL, 'ep:chat-dot-round', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, 'PRIME AI 外链跳转');

-- ============================================================
-- 2. 将"采购项目"统一重命名为"项目管理"
-- ============================================================
UPDATE sys_menu SET menu_name = '项目管理' WHERE menu_id = 1801010 AND menu_name = '采购项目';
UPDATE sys_menu SET menu_name = REPLACE(menu_name, '采购项目', '项目管理') WHERE parent_id = 1801010;

-- ============================================================
-- 3. test 测试菜单及按钮（幂等：先删后插）
-- ============================================================
DELETE FROM sys_menu WHERE menu_id IN (2092428635528671234, 2092428938349031425, 2092429067458097154);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(2092428635528671234, 'test', 1801000, 30, 'test', 'procurement/test/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:test:list', 'bug', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '测试菜单');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(2092428938349031425, '新整一条数据', 2092428635528671234, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:test:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(2092429067458097154, '删除一条数据', 2092428635528671234, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:test:delete', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 4. 采购订单菜单隐藏（当前未启用，避免侧边栏显示）
-- ============================================================
UPDATE sys_menu SET visible = '1' WHERE menu_id = 1801050 AND path = 'order';
UPDATE sys_menu SET visible = '1' WHERE parent_id = 1801050;
