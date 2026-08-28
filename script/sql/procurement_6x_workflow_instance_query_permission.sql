SET NAMES utf8mb4;

-- ============================================================
-- 采购模块审批视角查看流程进度/审批记录所需权限
-- ============================================================
-- 问题：approvalRecord.vue 点击"流程进度"会调用
--       /workflow/instance/flowHisTaskList/{businessId}，需要 workflow:instance:query 权限。
--       审批人/申请人等普通角色默认没有该权限，导致弹窗报"无权"。
-- 修复：给所有正常状态角色统一补授权限按钮 menu_id = 1761400000000011653（流程实例查询）。
-- 幂等：已授权的角色不会重复插入。
-- ============================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 1761400000000011653
FROM sys_role r
WHERE r.status = '0'
  AND r.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id
      AND rm.menu_id = 1761400000000011653
  );
