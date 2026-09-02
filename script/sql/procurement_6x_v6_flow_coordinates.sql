SET NAMES utf8mb4;
-- ============================================================
-- 采购 v6:工作流流程图坐标修正(幂等,可重复执行)
--   说明:
--     * warm-flow 坐标语义(以验收流程 900000000000000002 为正确参考):
--       - flow_node.coordinate = 节点中心点  x,y  (必要),文字坐标可加  x,y|x,y
--         普通/开始/结束节点尺寸 100x80(左右边缘 = 中心 ±50,上下 ±40)
--         网关节点尺寸 40x40
--       - flow_skip.coordinate = 起;折点;...;终|标签坐标
--         起点 = 源节点出边口(右侧中点),终点 = 目标节点入边口(左侧中点)
--     * 采购申请(pms_request):主链 start→apply→leader→ceo→gateway_amount,
--       分支 <1000 直通 end(右上)、>=1000 走 supreme_decision_maker(右下)再进 end
--     * 领用申请(pms_issue_request):start→apply→leader→warehouse→end 单线
--     * 退回线从审批节点底部向下绕回 start 底部(参考验收回流线)
-- ============================================================

-- ---------- 900000000000000001 pms_request 采购申请审批 ----------
UPDATE flow_node SET coordinate = '80,120'           WHERE definition_id = 900000000000000001 AND node_code = 'start' AND del_flag = '0';
UPDATE flow_node SET coordinate = '330,120|330,120'  WHERE definition_id = 900000000000000001 AND node_code = 'apply' AND del_flag = '0';
UPDATE flow_node SET coordinate = '580,120|580,120'  WHERE definition_id = 900000000000000001 AND node_code = 'leader' AND del_flag = '0';
UPDATE flow_node SET coordinate = '830,120|830,120'  WHERE definition_id = 900000000000000001 AND node_code = 'ceo' AND del_flag = '0';
UPDATE flow_node SET coordinate = '980,120|980,120'  WHERE definition_id = 900000000000000001 AND node_code = 'gateway_amount' AND del_flag = '0';
UPDATE flow_node SET coordinate = '1330,40|1330,40'    WHERE definition_id = 900000000000000001 AND node_code = 'end' AND del_flag = '0';
UPDATE flow_node SET coordinate = '1330,200|1330,200'  WHERE definition_id = 900000000000000001 AND node_code = 'supreme_decision_maker' AND del_flag = '0';

-- 主链 PASS
UPDATE flow_skip SET coordinate = '130,120;280,120|205,120' WHERE definition_id = 900000000000000001 AND now_node_code = 'start' AND next_node_code = 'apply' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '380,120;530,120|455,120' WHERE definition_id = 900000000000000001 AND now_node_code = 'apply' AND next_node_code = 'leader' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '630,120;780,120|705,120' WHERE definition_id = 900000000000000001 AND now_node_code = 'leader' AND next_node_code = 'ceo' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '880,120;960,120|920,120' WHERE definition_id = 900000000000000001 AND now_node_code = 'ceo' AND next_node_code = 'gateway_amount' AND del_flag = '0';
-- 分支:gateway → end(<1000,右上) / gateway → supreme(>=1000,右下)
UPDATE flow_skip SET coordinate = '1000,120;1140,120;1140,40;1280,40|1195,45'   WHERE definition_id = 900000000000000001 AND now_node_code = 'gateway_amount' AND next_node_code = 'end' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '1000,120;1140,120;1140,200;1280,200|1195,165' WHERE definition_id = 900000000000000001 AND now_node_code = 'gateway_amount' AND next_node_code = 'supreme_decision_maker' AND del_flag = '0';
-- supreme → end(向上短竖线)
UPDATE flow_skip SET coordinate = '1330,160;1330,80|1290,120' WHERE definition_id = 900000000000000001 AND now_node_code = 'supreme_decision_maker' AND next_node_code = 'end' AND del_flag = '0';

-- 退回 REJECT(底部回流到 start)
UPDATE flow_skip SET coordinate = '580,160;580,220;80,220;80,168|330,220'   WHERE definition_id = 900000000000000001 AND now_node_code = 'leader' AND next_node_code = 'start' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '830,160;830,220;80,220;80,168|330,220'   WHERE definition_id = 900000000000000001 AND now_node_code = 'ceo' AND next_node_code = 'start' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '1330,240;1330,260;80,260;80,168|400,260' WHERE definition_id = 900000000000000001 AND now_node_code = 'supreme_decision_maker' AND next_node_code = 'start' AND del_flag = '0';

-- ---------- 900000000000000003 pms_issue_request 领用申请审批 ----------
UPDATE flow_node SET coordinate = '80,120'          WHERE definition_id = 900000000000000003 AND node_code = 'start' AND del_flag = '0';
UPDATE flow_node SET coordinate = '330,120|330,120' WHERE definition_id = 900000000000000003 AND node_code = 'apply' AND del_flag = '0';
UPDATE flow_node SET coordinate = '580,120|580,120' WHERE definition_id = 900000000000000003 AND node_code = 'leader' AND del_flag = '0';
UPDATE flow_node SET coordinate = '830,120|830,120' WHERE definition_id = 900000000000000003 AND node_code = 'warehouse' AND del_flag = '0';
UPDATE flow_node SET coordinate = '1080,120|1080,120' WHERE definition_id = 900000000000000003 AND node_code = 'end' AND del_flag = '0';

UPDATE flow_skip SET coordinate = '130,120;280,120|205,120' WHERE definition_id = 900000000000000003 AND now_node_code = 'start' AND next_node_code = 'apply' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '380,120;530,120|455,120' WHERE definition_id = 900000000000000003 AND now_node_code = 'apply' AND next_node_code = 'leader' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '630,120;780,120|705,120' WHERE definition_id = 900000000000000003 AND now_node_code = 'leader' AND next_node_code = 'warehouse' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '880,120;1030,120|955,120' WHERE definition_id = 900000000000000003 AND now_node_code = 'warehouse' AND next_node_code = 'end' AND del_flag = '0';
UPDATE flow_skip SET coordinate = '830,160;830,220;80,220;80,168|330,220' WHERE definition_id = 900000000000000003 AND now_node_code = 'warehouse' AND next_node_code = 'start' AND del_flag = '0';

-- ---------- 900000000000000002 pms_acceptance 采购验收审批(仅捋直 start→apply 小折线,其余保留) ----------
UPDATE flow_skip SET coordinate = '130,120;277,118|203,120' WHERE definition_id = 900000000000000002 AND now_node_code = 'start' AND next_node_code = 'apply' AND del_flag = '0';

-- 校验
SELECT definition_id, node_code, node_name, coordinate FROM flow_node WHERE definition_id IN (900000000000000001, 900000000000000003) AND del_flag='0' ORDER BY definition_id, id;
SELECT definition_id, now_node_code, next_node_code, coordinate FROM flow_skip WHERE definition_id IN (900000000000000001, 900000000000000003) AND del_flag='0' ORDER BY definition_id, id;