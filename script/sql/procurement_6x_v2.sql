SET NAMES utf8mb4;
-- ============================================================
-- 采购系统 v2 权限与菜单脚本（ruoyi-6x / ry-vue-6x）
-- 依据：docs/采购系统详细页面规划.md (v2)
-- 目标：为「前端先行」提供可点击的权限基础 —— 角色、菜单授权、测试账号。
-- 本脚本仅含权限/菜单/角色/测试用户；建表/流程定义在后续阶段脚本中补齐。
-- 幂等可重复执行：角色/菜单/用户先删后插；菜单显隐用 UPDATE。
-- 导入库：ry-vue-6x
-- ============================================================

SET @admin_id  := 1761100000000000001; -- admin 用户 id
SET @root_dept := 1761000000000000103; -- admin 所在部门（测试用户挂此部门）
-- 测试用户统一密码 666666（与 ry_vue.sql 中 test/test1 相同 BCrypt 值）
SET @pwd := '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne';

-- ============================================================
-- 1. 隐藏旧菜单（保留代码，仅隐藏入口）：供应商 / 旧BOM / 订单
-- ============================================================
UPDATE sys_menu SET visible = '0' WHERE menu_id IN (1801020, 1801030, 1801050);

-- ============================================================
-- 2. 新增按钮权限：项目树选择（采购申请表单的项目选择器用）
--    普通申请人可调用 /procurement/project/tree，但不暴露「采购项目」列表页。
--    对应后端改动：PmsProjectController.tree() 的 @SaCheckPermission 由
--    procurement:project:list 改为 procurement:project:tree（后端阶段完成）。
-- ============================================================
DELETE FROM sys_menu WHERE menu_id = 1801016;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801016, '项目树选择', 1801010, 6, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:project:tree', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 3. 新增角色（role_id 段 176130000000000000X）
--    data_scope：1全部 2自定 3本部门 4本部门及以下 5仅本人 6部门及以下或本人
--    「项目负责人 = 我负责的项目」为自定义数据权限，后端阶段实现（暂置 1 全部）。
-- ============================================================
DELETE FROM sys_role_menu WHERE role_id IN (1761300000000000002, 1761300000000000006, 1761300000000000007, 1761300000000000008);
DELETE FROM sys_user_role WHERE role_id IN (1761300000000000002, 1761300000000000006, 1761300000000000007, 1761300000000000008);
DELETE FROM sys_role WHERE role_id IN (1761300000000000002, 1761300000000000006, 1761300000000000007, 1761300000000000008);

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1761300000000000002, '采购申请人', 'procurement_applicant', 5, '5', 0, 1, '0', '0', @root_dept, @admin_id, sysdate(), NULL, NULL, '提交采购申请，仅本人数据'),
(1761300000000000006, '采购对接人', 'procurement_contact', 6, '1', 0, 1, '0', '0', @root_dept, @admin_id, sysdate(), NULL, NULL, '审批+转单+合同+验收，全量数据'),
(1761300000000000007, '项目负责人', 'project_leader', 7, '1', 0, 1, '0', '0', @root_dept, @admin_id, sysdate(), NULL, NULL, '审批所属项目申请（数据权限=我负责的项目，后端实现）'),
(1761300000000000008, '仓库看管人', 'warehouse_keeper', 8, '1', 0, 1, '0', '0', @root_dept, @admin_id, sysdate(), NULL, NULL, '入库/出库/领用审批');

-- ============================================================
-- 4. 角色-菜单授权
--    采购管理目录 1801000；采购申请 1801040 + 按钮 1801041~1801046；
--    项目树选择 1801016；我的任务组 1618/1619/1629/1632/1633。
-- ============================================================

-- 4.1 采购申请人：申请全操作 + 项目树选择 + 我的任务
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000002, 1801000),
(1761300000000000002, 1801016),
(1761300000000000002, 1801040), (1761300000000000002, 1801041), (1761300000000000002, 1801042),
(1761300000000000002, 1801043), (1761300000000000002, 1801044), (1761300000000000002, 1801045),
(1761300000000000002, 1801046),
(1761300000000000002, 1761400000000011618), (1761300000000000002, 1761400000000011629),
(1761300000000000002, 1761400000000011619), (1761300000000000002, 1761400000000011632),
(1761300000000000002, 1761400000000011633);

-- 4.2 采购对接人：申请查看/导出 + 项目树选择 + 我的任务（后续阶段追加验收/合同/报销）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000006, 1801000),
(1761300000000000006, 1801016),
(1761300000000000006, 1801040), (1761300000000000006, 1801041), (1761300000000000006, 1801045),
(1761300000000000006, 1761400000000011618), (1761300000000000006, 1761400000000011629),
(1761300000000000006, 1761400000000011619), (1761300000000000006, 1761400000000011632),
(1761300000000000006, 1761400000000011633);

-- 4.3 项目负责人：申请查看 + 项目树选择 + 我的任务
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000007, 1801000),
(1761300000000000007, 1801016),
(1761300000000000007, 1801040), (1761300000000000007, 1801041),
(1761300000000000007, 1761400000000011618), (1761300000000000007, 1761400000000011629),
(1761300000000000007, 1761400000000011619), (1761300000000000007, 1761400000000011632),
(1761300000000000007, 1761400000000011633);

-- 4.4 仓库看管人：申请查看 + 我的任务（后续阶段追加仓库/领用）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000008, 1801000),
(1761300000000000008, 1801040), (1761300000000000008, 1801041),
(1761300000000000008, 1761400000000011618), (1761300000000000008, 1761400000000011629),
(1761300000000000008, 1761400000000011619), (1761300000000000008, 1761400000000011632),
(1761300000000000008, 1761400000000011633);

-- ============================================================
-- 5. 测试用户（user_id 段 176110000000000001X；密码统一 666666）
-- ============================================================
DELETE FROM sys_user_role WHERE user_id IN (1761100000000000011, 1761100000000000012, 1761100000000000013, 1761100000000000014);
DELETE FROM sys_user WHERE user_id IN (1761100000000000011, 1761100000000000012, 1761100000000000013, 1761100000000000014);

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phone_number, gender, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1761100000000000011, @root_dept, 'proc_applier', '采购申请人(密码666666)', 'sys_user', '', '', '0', NULL, @pwd, '0', '0', '127.0.0.1', sysdate(), @root_dept, @admin_id, sysdate(), NULL, NULL, '测试：采购申请人'),
(1761100000000000012, @root_dept, 'proc_contact', '采购对接人(密码666666)', 'sys_user', '', '', '0', NULL, @pwd, '0', '0', '127.0.0.1', sysdate(), @root_dept, @admin_id, sysdate(), NULL, NULL, '测试：采购对接人'),
(1761100000000000013, @root_dept, 'proc_leader',  '项目负责人(密码666666)', 'sys_user', '', '', '0', NULL, @pwd, '0', '0', '127.0.0.1', sysdate(), @root_dept, @admin_id, sysdate(), NULL, NULL, '测试：项目负责人'),
(1761100000000000014, @root_dept, 'proc_keeper',  '仓库看管人(密码666666)', 'sys_user', '', '', '0', NULL, @pwd, '0', '0', '127.0.0.1', sysdate(), @root_dept, @admin_id, sysdate(), NULL, NULL, '测试：仓库看管人');

-- ============================================================
-- 6. 用户-角色绑定
-- ============================================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1761100000000000011, 1761300000000000002),
(1761100000000000012, 1761300000000000006),
(1761100000000000013, 1761300000000000007),
(1761100000000000014, 1761300000000000008);

-- ============================================================
-- 7. 校正「采购专员」角色：v2 中项目管理仅管理员可见，移除其采购项目菜单
-- ============================================================
DELETE FROM sys_role_menu WHERE role_id = 1761300000000000005 AND menu_id >= 1801010 AND menu_id < 1801020;

-- ============================================================
-- 8. v2 新增页面菜单（菜单段 1801060~1801139，全部挂在「采购管理」1801000 下）
--    前端页面组件须存在于 plus-ui-6x/src/views/procurement/<path>/index.vue
-- ============================================================
DELETE FROM sys_role_menu WHERE menu_id >= 1801060 AND menu_id <= 1801139;
DELETE FROM sys_menu WHERE menu_id >= 1801060 AND menu_id <= 1801139;

-- 采购验收 1801060
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801060, '采购验收', 1801000, 6, 'acceptance', 'procurement/acceptance/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:acceptance:list', 'ep:document-checked', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '采购验收菜单'),
(1801061, '验收查询', 1801060, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:acceptance:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801062, '验收新增', 1801060, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:acceptance:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801063, '验收修改', 1801060, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:acceptance:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801064, '验收删除', 1801060, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:acceptance:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801065, '验收导出', 1801060, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:acceptance:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 仓库库存 1801070
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801070, '仓库库存', 1801000, 7, 'warehouse', 'procurement/warehouse/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:warehouse:list', 'ep:box', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '仓库库存菜单'),
(1801071, '库存查询', 1801070, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:warehouse:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801072, '手动入库', 1801070, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:warehouse:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801073, '库存修改', 1801070, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:warehouse:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801074, '库存删除', 1801070, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:warehouse:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 领用申请 1801080
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801080, '领用申请', 1801000, 8, 'issue', 'procurement/issue/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:issue:list', 'ep:takeaway-box', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '领用申请菜单'),
(1801081, '领用查询', 1801080, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:issue:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801082, '领用新增', 1801080, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:issue:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801083, '领用修改', 1801080, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:issue:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801084, '领用删除', 1801080, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:issue:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801085, '领用审批', 1801080, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:issue:approve', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- BOM物料库 1801090
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801090, 'BOM物料库', 1801000, 9, 'catalog', 'procurement/catalog/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:catalog:list', 'ep:collection', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, 'BOM物料库菜单'),
(1801091, '物料查询', 1801090, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:catalog:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801092, '物料新增', 1801090, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:catalog:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801093, '物料修改', 1801090, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:catalog:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801094, '物料删除', 1801090, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:catalog:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- BOM表(产品) 1801100
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801100, 'BOM表(产品)', 1801000, 10, 'bomtable', 'procurement/bomtable/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:bomtable:list', 'ep:cpu', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, 'BOM表产品菜单'),
(1801101, 'BOM表查询', 1801100, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bomtable:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801102, 'BOM表新增', 1801100, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bomtable:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801103, 'BOM表修改', 1801100, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bomtable:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801104, 'BOM表删除', 1801100, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bomtable:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 流转记录 1801110
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801110, '流转记录', 1801000, 11, 'operationlog', 'procurement/operationlog/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:log:list', 'ep:clock', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '流转记录菜单'),
(1801111, '日志查询', 1801110, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:log:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 采购合同 1801120
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801120, '采购合同', 1801000, 12, 'contract', 'procurement/contract/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:contract:list', 'ep:document', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '采购合同菜单'),
(1801121, '合同查询', 1801120, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:contract:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801122, '生成合同', 1801120, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:contract:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801123, '合同导出', 1801120, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:contract:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 报销导出 1801130
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801130, '报销导出', 1801000, 13, 'reimbursement', 'procurement/reimbursement/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:reimbursement:list', 'ep:files', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '报销导出菜单'),
(1801131, '报销查询', 1801130, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:reimbursement:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801132, '生成报销包', 1801130, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:reimbursement:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1801133, '报销下载', 1801130, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:reimbursement:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 9. 新增菜单的角色授权（按 v2 各页面权限表）
--    申请人=1761300000000000002 对接人=...6 负责人=...7 看管人=...8 专员=...5
-- ============================================================
DELETE FROM sys_role_menu WHERE menu_id >= 1801060 AND menu_id <= 1801139;

-- 采购申请人：验收查、库存查、领用增、BOM查、流转查、合同查、报销查
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000002, 1801060), (1761300000000000002, 1801061),
(1761300000000000002, 1801070), (1761300000000000002, 1801071),
(1761300000000000002, 1801080), (1761300000000000002, 1801081), (1761300000000000002, 1801082),
(1761300000000000002, 1801090), (1761300000000000002, 1801091),
(1761300000000000002, 1801100), (1761300000000000002, 1801101),
(1761300000000000002, 1801110), (1761300000000000002, 1801111),
(1761300000000000002, 1801120), (1761300000000000002, 1801121),
(1761300000000000002, 1801130), (1761300000000000002, 1801131);

-- 采购对接人：验收全、库存查、领用查、BOM全、流转查、合同全、报销全
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000006, 1801060), (1761300000000000006, 1801061), (1761300000000000006, 1801062), (1761300000000000006, 1801063), (1761300000000000006, 1801064), (1761300000000000006, 1801065),
(1761300000000000006, 1801070), (1761300000000000006, 1801071),
(1761300000000000006, 1801080), (1761300000000000006, 1801081),
(1761300000000000006, 1801090), (1761300000000000006, 1801091), (1761300000000000006, 1801092), (1761300000000000006, 1801093), (1761300000000000006, 1801094),
(1761300000000000006, 1801100), (1761300000000000006, 1801101), (1761300000000000006, 1801102), (1761300000000000006, 1801103), (1761300000000000006, 1801104),
(1761300000000000006, 1801110), (1761300000000000006, 1801111),
(1761300000000000006, 1801120), (1761300000000000006, 1801121), (1761300000000000006, 1801122), (1761300000000000006, 1801123),
(1761300000000000006, 1801130), (1761300000000000006, 1801131), (1761300000000000006, 1801132), (1761300000000000006, 1801133);

-- 项目负责人：验收查、BOM查、流转查、合同查
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000007, 1801060), (1761300000000000007, 1801061),
(1761300000000000007, 1801090), (1761300000000000007, 1801091),
(1761300000000000007, 1801100), (1761300000000000007, 1801101),
(1761300000000000007, 1801110), (1761300000000000007, 1801111),
(1761300000000000007, 1801120), (1761300000000000007, 1801121);

-- 仓库看管人：仓库全、领用查+审批、流转查
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000008, 1801070), (1761300000000000008, 1801071), (1761300000000000008, 1801072), (1761300000000000008, 1801073), (1761300000000000008, 1801074),
(1761300000000000008, 1801080), (1761300000000000008, 1801081), (1761300000000000008, 1801085),
(1761300000000000008, 1801110), (1761300000000000008, 1801111);

-- 采购专员：验收查+导出、合同查+导出、报销全
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000005, 1801060), (1761300000000000005, 1801061), (1761300000000000005, 1801065),
(1761300000000000005, 1801120), (1761300000000000005, 1801121), (1761300000000000005, 1801123),
(1761300000000000005, 1801130), (1761300000000000005, 1801131), (1761300000000000005, 1801132), (1761300000000000005, 1801133);

-- ============================================================
-- 10. 流转记录改用 RuoYi 自带操作日志（@Log + sys_oper_log），隐藏自定义菜单
-- ============================================================
UPDATE sys_menu SET visible = '0' WHERE menu_id IN (1801110, 1801111);
