SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：领用申请审批流 + 流程实例列 + 菜单（幂等，可重复执行）
--   流程：start → apply(发起人,${initiator})
--              → warehouse(仓库管理员,role:<仓库管理员角色id>)
--              → end
--   说明：
--     * 领用申请提交后走工作流，审批人为「仓库管理员」角色（裴天姿已绑定）。
--     * 流程完成时自动扣减库存逻辑在 PmsIssueRequestServiceImpl.processHandler 中
--       （finish 时触发 stockOutOnFinish）。
--     * 审批通过后仓库数量减少（修复"领用且审批后仓库数量没有减少"）。
-- 导入库：ry-vue-6x
-- ============================================================
SET @admin_id = 1761100000000000001; -- admin 用户 id
SET @dept_id  = 1761000000000000103; -- 主部门 id
SET @cat_req   = 1762300000000000201; -- 采购>采购申请 子分类（procurement_6x_category.sql 中定义）
SET @def_id = 900000000000000003;
SET @role_warehouse = 1761300000000000012; -- 仓库管理员

-- ---------- 流程实例列（幂等：information_schema 判断） ----------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_issue_request' AND COLUMN_NAME = 'process_instance_id');
SET @ddl = IF(@col_exists = 0,
              'ALTER TABLE pms_issue_request ADD COLUMN process_instance_id bigint DEFAULT NULL COMMENT ''流程实例ID'' AFTER status',
              'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 先删旧节点/跳转/定义，保证幂等
DELETE FROM flow_skip WHERE definition_id = @def_id;
DELETE FROM flow_node WHERE definition_id = @def_id;
DELETE FROM flow_definition WHERE id = @def_id;

-- ---------- 流程定义 ----------
INSERT INTO flow_definition (`id`, `flow_code`, `flow_name`, `model_value`, `category`, `version`, `is_publish`, `form_custom`, `form_path`, `activity_status`, `listener_type`, `listener_path`, `ext`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (@def_id, 'pms_issue_request', '领用申请审批', 'CLASSICS', CAST(@cat_req AS CHAR), '1.0', 1, 'N', '/procurement/issue/detail', 1, NULL, NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- ---------- 节点 ----------
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000051, 0, @def_id, 'start', '开始', NULL, '0.000', '80,120|80,120', NULL, NULL, NULL, 'N', '/procurement/issue/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000052, 1, @def_id, 'apply', '领用发起人', '${initiator}', '0.000', '320,120|320,120', NULL, NULL, NULL, 'N', '/procurement/issue/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000053, 1, @def_id, 'warehouse', '仓库管理员审批', CONCAT('role:', @role_warehouse), '0.000', '560,120|560,120', NULL, NULL, NULL, 'N', '/procurement/issue/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000054, 2, @def_id, 'end', '结束', NULL, '0.000', '800,120|800,120', NULL, NULL, NULL, 'N', '/procurement/issue/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- ---------- 跳转 ----------
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000061, @def_id, 'start', 0, 'apply', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000062, @def_id, 'apply', 1, 'warehouse', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000063, @def_id, 'warehouse', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000064, @def_id, 'warehouse', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- ---------- 菜单：提交按钮 1801086 + 领用审批详情隐藏菜单 1801087 ----------
DELETE FROM sys_menu WHERE menu_id IN (1801086, 1801087);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801086, '领用提交', 1801080, 6, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:issue:submit', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801087, '领用申请审批详情', 1801000, 22, 'issue/detail', 'procurement/issue/detail', NULL, 'N', 'Y', 'C', '1', '0', 'procurement:issue:query', 'form', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '领用待办/已办办理与查看跳转页（隐藏）');

-- ---------- 角色绑定（先删后插，幂等） ----------
-- 提交按钮（发起领用：申请人/对接人/专员/普通用户）
DELETE FROM sys_role_menu WHERE menu_id = 1801086;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000002, 1801086), -- 采购申请人
(1761300000000000005, 1801086), -- 采购专员
(1761300000000000006, 1801086), -- 采购对接人
(1761300000000000013, 1801086); -- 普通用户

-- 隐藏详情菜单（全员涉领用角色）
DELETE FROM sys_role_menu WHERE menu_id = 1801087;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1761300000000000002, 1801087), -- 采购申请人
(1761300000000000005, 1801087), -- 采购专员
(1761300000000000006, 1801087), -- 采购对接人
(1761300000000000007, 1801087), -- 项目负责人
(1761300000000000009, 1801087), -- 团队上级
(1761300000000000010, 1801087), -- 部门上级
(1761300000000000011, 1801087), -- 验收对接人
(1761300000000000012, 1801087), -- 仓库管理员（审批人）
(1761300000000000013, 1801087); -- 普通用户

-- 校验
SELECT id, flow_code, flow_name, category, form_path FROM flow_definition WHERE id = @def_id;
SELECT node_code, node_name, node_type FROM flow_node WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
SELECT now_node_code, next_node_code, skip_type FROM flow_skip WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
SELECT menu_id, menu_name, parent_id, path, component, visible FROM sys_menu WHERE menu_id IN (1801086, 1801087);
SELECT column_name FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_issue_request' AND COLUMN_NAME = 'process_instance_id';
