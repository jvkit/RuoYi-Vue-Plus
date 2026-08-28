SET NAMES utf8mb4;
-- ============================================================
-- OA 6x 采购系统 v4 清理与权限整理脚本（ry-vue-6x）
-- 幂等执行，本地与服务器共用
-- ============================================================

SET @admin_id = 1761100000000000001;

-- ============================================================
-- 1. PRIME AI 内嵌菜单
-- ============================================================
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1805000, 'PRIME AI', 0, 7, 'http://172.16.16.110:3305', NULL, NULL, 'N', 'N', 'M', '0', '0', NULL, 'ep:chat-dot-round', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, 'PRIME AI 内嵌打开')
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  component = VALUES(component),
  is_frame = VALUES(is_frame),
  menu_type = VALUES(menu_type),
  visible = VALUES(visible),
  status = VALUES(status),
  icon = VALUES(icon),
  remark = VALUES(remark);

-- ============================================================
-- 2. 角色整理：停用不需要的角色
-- 保留：superadmin(1761300000000000001)、common_user(1761300000000000013)、
--       warehouse_admin(1761300000000000012)、team_leader(1761300000000000009)、
--       dept_leader(1761300000000000010)
-- ============================================================
UPDATE sys_role SET status = '1' WHERE role_id IN (
  1761300000000000002, -- procurement_applicant
  1761300000000000003, -- test1
  1761300000000000004, -- test2
  1761300000000000005, -- purchase_staff
  1761300000000000006, -- procurement_contact
  1761300000000000007, -- project_leader
  1761300000000000008, -- warehouse_keeper
  1761300000000000011  -- acceptance_contact
);

-- ============================================================
-- 3. 普通用户角色权限：所有采购菜单 + PRIME AI + 我的任务
-- ============================================================
SET @common_user_role_id = 1761300000000000013;

-- 先清掉普通用户旧的采购菜单绑定，再重新绑定完整的
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id BETWEEN 1801000 AND 1801999;
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1805000;
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011616;

-- 绑定采购管理目录及所有子菜单（含按钮）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_user_role_id, menu_id FROM sys_menu
WHERE menu_id BETWEEN 1801000 AND 1801999 AND status = '0'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 绑定 PRIME AI
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1805000)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 绑定我的任务（workflow 目录）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011616)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ============================================================
-- 4. 创建 8 个新用户，默认绑定普通用户角色
-- 彭赛威、张岩、李炳晨、蒋泽鹏、冯忠俊、陈俊荣、何鑫、贺鑫雨
-- ============================================================
-- 创建用户
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phone_number, gender, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
VALUES
(2093000000000000001, 1761000000000000103, 'pengsaiwei', '彭赛威', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000002, 1761000000000000103, 'zhangyan', '张岩', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000003, 1761000000000000103, 'libingchen', '李炳晨', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000004, 1761000000000000103, 'jiangzepeng', '蒋泽鹏', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000005, 1761000000000000103, 'fengzhongjun', '冯忠俊', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000006, 1761000000000000103, 'chenjunrong', '陈俊荣', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000007, 1761000000000000103, 'hexin', '何鑫', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL),
(2093000000000000008, 1761000000000000103, 'hexinyu', '贺鑫雨', 'sys_user', NULL, NULL, '0', 0, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', NULL, NULL, @admin_id, sysdate(), NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
  nick_name = VALUES(nick_name),
  password = VALUES(password),
  status = VALUES(status),
  del_flag = VALUES(del_flag);

-- 绑定普通用户角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT user_id, @common_user_role_id FROM sys_user WHERE user_id BETWEEN 2093000000000000001 AND 2093000000000000008
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ============================================================
-- 5. 项目管理菜单提级（与采购管理平级）
-- ============================================================
UPDATE sys_menu SET parent_id = 0, order_num = 6 WHERE menu_id = 1801010;

-- ============================================================
-- 6. 领用申请流程改造：申请人 → 项目负责人 → 仓库管理员 → 结束
-- ============================================================
SET @issue_def_id = 900000000000000003;

-- 清除旧节点和 skip（本地开发环境，允许重建）
DELETE FROM flow_node WHERE definition_id = @issue_def_id;
DELETE FROM flow_skip WHERE definition_id = @issue_def_id;

-- 插入新节点
INSERT INTO flow_node (id, node_type, definition_id, node_code, node_name, permission_flag, node_ratio, coordinate, any_node_skip, listener_type, listener_path, form_custom, form_path, version, create_time, create_by, update_time, update_by, del_flag, tenant_id)
VALUES
(900000000000000051, 0, @issue_def_id, 'start', '开始', NULL, '0.000', '100,100', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000052, 1, @issue_def_id, 'apply', '提交申请', '${initiator}', '0.000', '250,100', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000053, 1, @issue_def_id, 'leader', '项目负责人', '${leaderId}', '0.000', '400,100', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000054, 1, @issue_def_id, 'warehouse', '仓库管理员', 'role:1761300000000000012', '0.000', '550,100', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000055, 2, @issue_def_id, 'end', '结束', NULL, '0.000', '700,100', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL);

-- 插入新 skip
INSERT INTO flow_skip (id, definition_id, now_node_code, now_node_type, next_node_code, next_node_type, skip_name, skip_type, skip_condition, coordinate, create_time, create_by, update_time, update_by, del_flag, tenant_id)
VALUES
(900000000000000061, @issue_def_id, 'start', 0, 'apply', 1, NULL, 'PASS', NULL, NULL, sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000062, @issue_def_id, 'apply', 1, 'leader', 1, NULL, 'PASS', NULL, NULL, sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000063, @issue_def_id, 'leader', 1, 'warehouse', 1, NULL, 'PASS', NULL, NULL, sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000064, @issue_def_id, 'warehouse', 1, 'end', 2, NULL, 'PASS', NULL, NULL, sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL),
(900000000000000065, @issue_def_id, 'warehouse', 1, 'start', 0, NULL, 'REJECT', NULL, NULL, sysdate(), @admin_id, sysdate(), @admin_id, '0', NULL);
