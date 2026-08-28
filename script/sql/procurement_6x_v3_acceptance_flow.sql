SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：采购验收审批流（幂等，可重复执行）
--   流程：start → apply(验收人/发起人,${initiator})
--            → applicant(采购申请人,${applicantId})
--            → leader(项目负责人,${leaderId})
--            → team_leader(团队上级,role:<团队上级角色id>)
--            → end
--   说明：
--     * 采购申请人 = 关联采购申请(pms_procurement_request)的 create_by，动态传入，
--       验收人(验收单发起人)与采购申请人未必是同一个人。
--     * 验收完成后自动入库逻辑在 PmsAcceptanceServiceImpl.processHandler 中
--       （流程 finish 时触发 stockInOnFinish）。
--     * 变量 applicantId / leaderId 由提交验收时传入。
-- ============================================================
SET @def_id = 900000000000000002;
SET @role_team_leader = 1761300000000000009;  -- 团队上级

-- 先删旧节点/跳转，保证幂等
DELETE FROM flow_skip WHERE definition_id = @def_id;
DELETE FROM flow_node WHERE definition_id = @def_id;
DELETE FROM flow_definition WHERE id = @def_id;

-- ---------- 流程定义 ----------
INSERT INTO flow_definition (`id`, `flow_code`, `flow_name`, `model_value`, `category`, `version`, `is_publish`, `form_custom`, `form_path`, `activity_status`, `listener_type`, `listener_path`, `ext`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (@def_id, 'pms_acceptance', '采购验收审批', 'CLASSICS', '100', '1.0', 1, 'N', '/procurement/acceptance/detail', 1, NULL, NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- ---------- 节点 ----------
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000031, 0, @def_id, 'start', '开始', NULL, '0.000', '80,120|80,120', NULL, NULL, NULL, 'N', '/procurement/acceptance/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000032, 1, @def_id, 'apply', '验收发起人', '${initiator}', '0.000', '320,120|320,120', NULL, NULL, NULL, 'N', '/procurement/acceptance/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000033, 1, @def_id, 'applicant', '采购申请人确认', '${applicantId}', '0.000', '560,120|560,120', NULL, NULL, NULL, 'N', '/procurement/acceptance/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000034, 1, @def_id, 'leader', '项目负责人审批', '${leaderId}', '0.000', '800,120|800,120', NULL, NULL, NULL, 'N', '/procurement/acceptance/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000035, 1, @def_id, 'team_leader', '团队上级审批', CONCAT('role:', @role_team_leader), '0.000', '1040,120|1040,120', NULL, NULL, NULL, 'N', '/procurement/acceptance/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000036, 2, @def_id, 'end', '结束', NULL, '0.000', '1280,120|1280,120', NULL, NULL, NULL, 'N', '/procurement/acceptance/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- ---------- 跳转 ----------
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000041, @def_id, 'start', 0, 'apply', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000042, @def_id, 'apply', 1, 'applicant', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000043, @def_id, 'applicant', 1, 'leader', 1, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000044, @def_id, 'leader', 1, 'team_leader', 1, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000045, @def_id, 'team_leader', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 退回（回到 start 终止）
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000046, @def_id, 'applicant', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000047, @def_id, 'leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000048, @def_id, 'team_leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 校验
SELECT id, flow_code, flow_name, form_path FROM flow_definition WHERE id = @def_id;
SELECT node_code, node_name, node_type FROM flow_node WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
SELECT now_node_code, next_node_code, skip_type, skip_condition FROM flow_skip WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
