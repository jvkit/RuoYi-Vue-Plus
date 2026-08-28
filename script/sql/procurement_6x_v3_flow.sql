SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：采购申请审批流改造（幂等，可重复执行）
--   原流程：start → apply(申请人) → leader(项目负责人) → contact(采购对接人,硬编码) → end
--   新流程：start → apply(申请人) → leader(项目负责人) → gateway_amount(排他网关)
--            → 按申请总金额条件分支：
--                amount < 1000  → team_leader(团队上级, 角色审批)
--                amount >= 1000 → dept_leader(部门上级, 角色审批)
--            → end
--   说明：
--     * warm-flow 条件分支必须通过 SERIAL 网关节点(node_type=3)实现；
--       普通节点(BETWEEN=1)的出边选择只取第一条 PASS skip，不判条件。
--     * SERIAL 网关遍历出边：第一条 skip_condition 求值为 true 的出边被选中；
--       无条件的出边作为默认兜底（顺序很重要，条件边在前）。
--     * team_leader / dept_leader 用 permission_flag=role:<角色id>，由 WorkflowPermissionHandler
--       的 convertPermissions 在任务创建时展开为拥有该角色的具体用户。
--     * skip_condition 格式：<比较符>@@<流程变量名>|<字面值>，如 lt@@amount|1000。
--     * 需在提交代码 PmsProcurementRequestServiceImpl.submitAndStartFlow 中把 amount 变量传入流程。
-- ============================================================
SET @def_id = 900000000000000001;
SET @role_team_leader = 1761300000000000009;  -- 团队上级
SET @role_dept_leader = 1761300000000000010;  -- 部门上级

-- 先删旧节点/跳转，保证幂等
DELETE FROM flow_skip WHERE definition_id = @def_id;
DELETE FROM flow_node WHERE definition_id = @def_id;

-- ---------- 节点 ----------
-- start (type=0)
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000011, 0, @def_id, 'start', '开始', NULL, '0.000', '80,120|80,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- apply (type=1) - 申请人
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000012, 1, @def_id, 'apply', '申请人', '${initiator}', '0.000', '300,120|300,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- leader (type=1) - 项目负责人审批
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000013, 1, @def_id, 'leader', '项目负责人审批', '${leaderId}', '0.000', '520,120|520,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- gateway_amount (type=3) - 排他网关（条件分支）
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000014, 3, @def_id, 'gateway_amount', '金额分支网关', NULL, '0.000', '740,120|740,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- team_leader (type=1) - 团队上级审批（角色）
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000015, 1, @def_id, 'team_leader', '团队上级审批', CONCAT('role:', @role_team_leader), '0.000', '960,80|960,80', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- dept_leader (type=1) - 部门上级审批（角色）
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000016, 1, @def_id, 'dept_leader', '部门上级审批', CONCAT('role:', @role_dept_leader), '0.000', '960,160|960,160', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- end (type=2)
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000017, 2, @def_id, 'end', '结束', NULL, '0.000', '1180,120|1180,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- ---------- 跳转 ----------
-- 顺序流：start → apply → leader → gateway
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000021, @def_id, 'start', 0, 'apply', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000022, @def_id, 'apply', 1, 'leader', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000023, @def_id, 'leader', 1, 'gateway_amount', 3, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 条件分支（排他网关 → 二选一）
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000024, @def_id, 'gateway_amount', 3, 'team_leader', 1, '通过(<1000)', 'PASS', 'lt@@amount|1000', NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000025, @def_id, 'gateway_amount', 3, 'dept_leader', 1, '通过(>=1000)', 'PASS', 'ge@@amount|1000', NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 终审到结束
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000026, @def_id, 'team_leader', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000027, @def_id, 'dept_leader', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 退回（回到 start 终止）
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000028, @def_id, 'leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000029, @def_id, 'team_leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000030, @def_id, 'dept_leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 校验
SELECT node_code, node_name, node_type, permission_flag FROM flow_node WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
SELECT now_node_code, now_node_type, next_node_code, next_node_type, skip_type, skip_condition FROM flow_skip WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
