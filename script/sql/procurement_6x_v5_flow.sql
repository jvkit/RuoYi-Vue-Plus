SET NAMES utf8mb4;
-- ============================================================
-- 采购 v5：采购申请审批流再次改造（幂等，可重复执行）
--   新流程：start → apply(申请人) → leader(项目负责人) → ceo(CEO)
--            → gateway_amount(排他网关)
--                amount < 1000  → end
--                amount >= 1000 → supreme_decision_maker(最高决策人) → end
--   说明：
--     * 把原来的 team_leader 节点改为 ceo 节点，只改 node_name/node_code。
--     * 把原来的 dept_leader 节点改为 supreme_decision_maker 节点，只改 node_name/node_code。
--     * 金额网关从 ceo 之后出分支，这样 CEO 只需审批一次。
-- ============================================================
SET @def_id = 900000000000000001;
SET @role_ceo = 1761300000000000009;  -- CEO（原团队上级）
SET @role_supreme = 1761300000000000010;  -- 最高决策人（原部门上级）

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

-- ceo (type=1) - CEO审批（原团队上级）
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000015, 1, @def_id, 'ceo', 'CEO审批', CONCAT('role:', @role_ceo), '0.000', '740,80|740,80', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- gateway_amount (type=3) - 排他网关（条件分支）
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000014, 3, @def_id, 'gateway_amount', '金额分支网关', NULL, '0.000', '960,120|960,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- supreme_decision_maker (type=1) - 最高决策人审批（原部门上级）
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000016, 1, @def_id, 'supreme_decision_maker', '最高决策人审批', CONCAT('role:', @role_supreme), '0.000', '1180,160|1180,160', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- end (type=2)
INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000017, 2, @def_id, 'end', '结束', NULL, '0.000', '1400,120|1400,120', NULL, NULL, NULL, 'N', '/procurement/request/detail', '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

-- ---------- 跳转 ----------
-- 顺序流：start → apply → leader → ceo
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000021, @def_id, 'start', 0, 'apply', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000022, @def_id, 'apply', 1, 'leader', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000023, @def_id, 'leader', 1, 'ceo', 1, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000030, @def_id, 'ceo', 1, 'gateway_amount', 3, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 条件分支（排他网关 → 二选一）
-- 注意：条件边在前，无条件边在后作为默认兜底
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000024, @def_id, 'gateway_amount', 3, 'end', 2, '通过(<1000)', 'PASS', 'lt@@amount|1000', NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000025, @def_id, 'gateway_amount', 3, 'supreme_decision_maker', 1, '通过(>=1000)', 'PASS', 'ge@@amount|1000', NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 最高决策人到结束
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000027, @def_id, 'supreme_decision_maker', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 退回（回到 start 终止）
INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000028, @def_id, 'leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000029, @def_id, 'ceo', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000031, @def_id, 'supreme_decision_maker', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- 校验
SELECT node_code, node_name, node_type, permission_flag FROM flow_node WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
SELECT now_node_code, now_node_type, next_node_code, next_node_type, skip_type, skip_condition FROM flow_skip WHERE definition_id = @def_id AND del_flag = '0' ORDER BY id;
