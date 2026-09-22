-- =============================================================
-- procurement_6x_v15_flow_skip_supreme.sql（2026-09-22）
-- P3：采购申请流程「暂时跳过最高决策人节点」
--
-- 背景：最高决策人（李老师）现阶段不参与审批，流程走到该节点直接结束；
--       后续需要恢复时，执行本文件末尾「恢复语句」（注释状态，不会被 apply-sql.sh 执行）。
--
-- 做法（方案 A，零代码）：把排他网关 gateway_amount 的 >=1000 出边指向 end，
--       原条件 skip_condition 保留不动 —— 恢复时只需把 next_node_code 改回去。
--       supreme_decision_maker 节点与角色绑定全部保留，流程图上成为不可达节点，
--       正好作为「后续要加回来」的视觉提醒。
--
-- 影响：跳过期间该环节无任何审批痕迹（现阶段李老师完全不参与，可接受）。
-- 注意：改完必须重启后端，warm-flow 的流程定义有内存缓存。
-- 幂等：重复执行结果一致（按 flow_code + 节点 + 条件精确定位，不写死 id）。
-- =============================================================
SET NAMES utf8mb4;

-- 取采购申请流程定义ID（避免跨环境写死 id）
SET @def_id := (SELECT id FROM flow_definition
                WHERE flow_code = 'pms_request' AND del_flag = '0'
                ORDER BY id DESC LIMIT 1);

-- 网关 >=1000 出边：最高决策人 → 结束
-- next_node_type 必须同步改（end=2，普通审批节点=1），否则引擎按旧类型流转会出错
UPDATE flow_skip
SET next_node_code = 'end',
    next_node_type = '2',
    skip_name      = '通过(>=1000,暂跳过最高决策人)',
    update_time    = sysdate()
WHERE definition_id = @def_id
  AND now_node_code = 'gateway_amount'
  AND skip_type = 'PASS'
  AND skip_condition = 'ge@@amount|1000'
  AND next_node_code <> 'end';

-- ---------- 校验输出 ----------
SELECT s.id, s.now_node_code, s.next_node_code, s.skip_name, s.skip_condition
FROM flow_skip s
WHERE s.definition_id = @def_id AND s.now_node_code = 'gateway_amount'
ORDER BY s.id;

-- =============================================================
-- 恢复语句（需要李老师重新参与审批时，手动执行下面这段；
-- 故意保持注释状态：apply-sql.sh 会自动执行目录下所有未记录脚本，
-- 若可执行会把跳过改回去，造成"刚部署就失效"）
-- =============================================================
-- UPDATE flow_skip
-- SET next_node_code = 'supreme_decision_maker',
--     next_node_type = '1',
--     skip_name      = '通过(>=1000)',
--     update_time    = sysdate()
-- WHERE definition_id = (SELECT id FROM flow_definition
--                        WHERE flow_code = 'pms_request' AND del_flag = '0'
--                        ORDER BY id DESC LIMIT 1)
--   AND now_node_code = 'gateway_amount'
--   AND skip_type = 'PASS'
--   AND skip_condition = 'ge@@amount|1000';
-- 执行后同样需要重启后端。
