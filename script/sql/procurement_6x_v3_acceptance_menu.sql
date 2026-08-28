SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：验收提交按钮 + 验收审批详情隐藏菜单 + 角色绑定（幂等）
--   1. 1801066「验收提交」按钮(procurement:acceptance:submit) 挂到 采购验收 1801060 下
--   2. 1801067「验收审批详情」隐藏菜单(C, visible=1) 挂到 采购管理 1801000 下，
--      path=acceptance/detail → 生成前端路由 /procurement/acceptance/detail，
--      与 flow_definition.form_path 对应，供「我的任务」待办/已办跳转。
--   3. 给验收相关角色绑定 1801066/1801067。
--   4. 验收流程定义 category 关联到「采购申请」子分类（原 '100' 无效，待办分类无法按采购筛选）。
-- 导入库：ry-vue-6x
-- ============================================================
SET @admin_id = 1761100000000000001; -- admin 用户 id
SET @dept_id  = 1761000000000000103; -- 主部门 id
SET @cat_req   = 1762300000000000201; -- 采购>采购申请 子分类（procurement_6x_category.sql 中定义）

-- 角色 id
SET @role_applicant    = 1761300000000000002; -- 采购申请人
SET @role_commissioner = 1761300000000000005; -- 采购专员
SET @role_contact      = 1761300000000000006; -- 采购对接人
SET @role_leader       = 1761300000000000007; -- 项目负责人
SET @role_team_leader  = 1761300000000000009; -- 团队上级
SET @role_dept_leader  = 1761300000000000010; -- 部门上级
SET @role_acceptance   = 1761300000000000011; -- 验收对接人
SET @role_warehouse    = 1761300000000000012; -- 仓库管理员
SET @role_common       = 1761300000000000013; -- 普通用户

-- ============================================================
-- 1. 验收提交按钮
-- ============================================================
DELETE FROM sys_menu WHERE menu_id = 1801066;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801066, '验收提交', 1801060, 6, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:acceptance:submit', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 2. 验收审批详情隐藏菜单
--    必须挂在「采购管理」目录(M 1801000)下、path 带子路径(acceptance/detail)，
--    目录才会递归生成路由；visible=1 表示隐藏不显示在侧边栏。
-- ============================================================
DELETE FROM sys_menu WHERE menu_id = 1801067;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801067, '采购验收审批详情', 1801000, 21, 'acceptance/detail', 'procurement/acceptance/detail', NULL, 'N', 'Y', 'C', '1', '0', 'procurement:acceptance:query', 'form', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '验收待办/已办办理与查看跳转页（隐藏）');

-- ============================================================
-- 3. 角色绑定（先删后插，幂等）
--    1801067 隐藏菜单：所有可能审批/发起验收的角色都要，否则前端不生成跳转路由
--    1801066 提交按钮：能发起验收的角色（申请人/对接人/专员/验收对接人/普通用户）
-- ============================================================
DELETE FROM sys_role_menu WHERE menu_id IN (1801066, 1801067);

-- 隐藏详情菜单（全员涉验收角色）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(@role_applicant, 1801067),
(@role_commissioner, 1801067),
(@role_contact, 1801067),
(@role_leader, 1801067),
(@role_team_leader, 1801067),
(@role_dept_leader, 1801067),
(@role_acceptance, 1801067),
(@role_warehouse, 1801067),
(@role_common, 1801067);

-- 提交按钮（发起验收）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(@role_applicant, 1801066),
(@role_commissioner, 1801066),
(@role_contact, 1801066),
(@role_acceptance, 1801066),
(@role_common, 1801066);

-- ============================================================
-- 4. 验收流程定义 category 关联到采购分类
-- ============================================================
UPDATE flow_definition
SET category = CAST(@cat_req AS CHAR)
WHERE flow_code = 'pms_acceptance' AND tenant_id = '000000';

-- ============================================================
-- 5. 校验
-- ============================================================
SELECT menu_id, menu_name, parent_id, path, component, visible FROM sys_menu WHERE menu_id IN (1801066, 1801067);
SELECT role_id, menu_id FROM sys_role_menu WHERE menu_id IN (1801066, 1801067) ORDER BY role_id;
SELECT flow_code, category, form_path FROM flow_definition WHERE flow_code IN ('pms_acceptance', 'pms_request');
