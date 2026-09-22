-- =============================================================
-- procurement_6x_v16_workflow_view_cancel_perms.sql（2026-09-22）
-- 修复回归：审批人/申请人看不到流程进度、撤销按钮点了报无权
--
-- 现象（Playwright + 接口实测）：
--   彭赛威/冯忠俊/裴天姿 调 GET /workflow/instance/getInfo/{businessId} → 403
--   同样账号调 PUT /workflow/instance/cancelProcessApply            → 403
--   只有超管（王建龙/李迪，走 superadmin 特判）能过。
--
-- 根因：
--   1. `workflow:instance:query`（菜单 1761400000000011653）当前只绑在 superadmin
--      和 7 个**已停用**角色上（procurement_applicant/test1/test2/purchase_staff/
--      procurement_contact/project_leader/warehouse_keeper）。
--      早先的修复脚本 procurement_6x_workflow_instance_query_permission.sql 是
--      「给所有 status='0' 角色补绑」，但它已记录在 applied-sql.log 里不会重跑，
--      而其后的角色清理脚本（procurement_role_menu_cleanup*.sql / v5 角色整理）
--      删掉了 common_user 等角色的菜单绑定，权限就再也没回来 → 回归。
--   2. `workflow:instance:cancel`（菜单 1761400000000011659）**从未绑给任何角色**。
--      而前端「我发起的」页面（myDocument.vue:122-131）的撤销按钮
--      **没有 v-hasPermi**，只按 flowStatus==='waiting' 显示 —— 所有用户都看得见、
--      点了必然 403。
--
-- 安全性：放开撤销权限是安全的，FlwInstanceServiceImpl.cancelProcessApply 内部已校验
--   「非超管只能撤销自己发起的实例」+ BusinessStatusEnum.checkCancelStatus 状态校验。
--
-- 修复：给所有**启用中**的角色补绑这两个按钮权限。
-- 幂等：NOT EXISTS 保护，可重复执行。
-- 注意：改完权限后用户需**重新登录**才生效（权限在登录时装进会话）。
-- =============================================================
SET NAMES utf8mb4;

-- 1653 流程实例查询（流程进度 / 流程图 / 审批详情）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 1761400000000011653
FROM sys_role r
WHERE r.status = '0' AND r.del_flag = '0'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm
                  WHERE rm.role_id = r.role_id AND rm.menu_id = 1761400000000011653);

-- 1659 流程实例撤销（申请人撤回自己发起的流程）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 1761400000000011659
FROM sys_role r
WHERE r.status = '0' AND r.del_flag = '0'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm
                  WHERE rm.role_id = r.role_id AND rm.menu_id = 1761400000000011659);

-- ---------- 校验输出 ----------
SELECT m.menu_id, m.menu_name, m.perms,
       GROUP_CONCAT(r.role_key ORDER BY r.role_id) AS bound_roles
FROM sys_menu m
JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
JOIN sys_role r ON r.role_id = rm.role_id AND r.status = '0' AND r.del_flag = '0'
WHERE m.menu_id IN (1761400000000011653, 1761400000000011659)
GROUP BY m.menu_id, m.menu_name, m.perms;
