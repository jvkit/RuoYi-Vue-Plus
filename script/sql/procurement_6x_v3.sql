SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：新增角色 + 测试账户（幂等）
--   角色：团队上级 / 部门上级 / 验收对接人 / 仓库管理员
--   账户：王建龙(团队上级) / 李迪(部门上级) / 裴天姿(验收对接人)  密码统一 666666
--   先删后插，保证幂等可重复执行
-- ============================================================
SET @admin_id = 1761100000000000001;

-- 角色 id（19 位，续现有 1761300000000000001~8）
SET @role_team_leader = 1761300000000000009;      -- 团队上级
SET @role_dept_leader = 1761300000000000010;      -- 部门上级
SET @role_acceptance  = 1761300000000000011;      -- 验收对接人
SET @role_warehouse   = 1761300000000000012;      -- 仓库管理员

-- 用户 id（19 位，续现有 1761100000000000011~14）
SET @user_wang = 1761100000000000015;  -- 王建龙
SET @user_li   = 1761100000000000016;  -- 李迪
SET @user_pei  = 1761100000000000017;  -- 裴天姿
SET @pwd = '$2a$10$I5ogK2Payc.q0d2pJJtvJ.tt3Wmm8q6542IvJrHGiTtnE.t44qrje'; -- 666666
SET @dept = 1761000000000000103; -- 研发部门（mock）

-- ============================================================
-- 1. 角色（先删后插）
-- ============================================================
DELETE FROM sys_role_menu WHERE role_id IN (@role_team_leader, @role_dept_leader, @role_acceptance, @role_warehouse);
DELETE FROM sys_role WHERE role_id IN (@role_team_leader, @role_dept_leader, @role_acceptance, @role_warehouse);

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark) VALUES
(@role_team_leader, '团队上级', 'team_leader', 10, '1', 0, 1, '0', '0', @admin_id, sysdate(), '采购金额<1000 审批人'),
(@role_dept_leader, '部门上级', 'dept_leader', 11, '1', 0, 1, '0', '0', @admin_id, sysdate(), '采购金额>=1000 审批人'),
(@role_acceptance,  '验收对接人', 'acceptance_contact', 12, '1', 0, 1, '0', '0', @admin_id, sysdate(), '采购验收审批人'),
(@role_warehouse,   '仓库管理员', 'warehouse_admin', 13, '1', 0, 1, '0', '0', @admin_id, sysdate(), '仓库出入库管理');

-- 角色可见采购模块菜单（项目管理/采购申请/采购验收/仓库等），不暴露系统管理
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM (SELECT @role_team_leader AS role_id UNION ALL SELECT @role_dept_leader UNION ALL SELECT @role_acceptance UNION ALL SELECT @role_warehouse) r
JOIN sys_menu m ON m.menu_id >= 1801000 AND m.menu_id < 1802000 AND m.menu_type IN ('M','C')
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);

-- ============================================================
-- 2. 账户（先删后插）
-- ============================================================
DELETE FROM sys_user_role WHERE user_id IN (@user_wang, @user_li, @user_pei);
DELETE FROM sys_user WHERE user_id IN (@user_wang, @user_li, @user_pei);

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phone_number, gender, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(@user_wang, @dept, 'wangjianlong', '王建龙', 'sys_user', '', '', 0, NULL, @pwd, '0', '0', '', NULL, @dept, @admin_id, sysdate(), NULL, NULL, '团队上级'),
(@user_li,   @dept, 'lidi',         '李迪',   'sys_user', '', '', 0, NULL, @pwd, '0', '0', '', NULL, @dept, @admin_id, sysdate(), NULL, NULL, '部门上级'),
(@user_pei,  @dept, 'peitianzi',    '裴天姿', 'sys_user', '', '', 0, NULL, @pwd, '0', '0', '', NULL, @dept, @admin_id, sysdate(), NULL, NULL, '验收对接人');

-- ============================================================
-- 3. 账户-角色绑定
-- ============================================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(@user_wang, @role_team_leader),
(@user_li,   @role_dept_leader),
(@user_pei,  @role_acceptance);

-- ============================================================
-- 4. 校验
-- ============================================================
SELECT role_id, role_name, role_key FROM sys_role WHERE role_id IN (@role_team_leader, @role_dept_leader, @role_acceptance, @role_warehouse);
SELECT u.user_id, u.user_name, u.nick_name, r.role_key
FROM sys_user u
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id
WHERE u.user_id IN (@user_wang, @user_li, @user_pei);
