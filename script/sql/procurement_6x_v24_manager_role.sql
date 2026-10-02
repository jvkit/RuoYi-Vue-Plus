SET NAMES utf8mb4;

-- =====================================================================
-- v24：采购经理角色分配（王建龙 / 裴天姿 / 彭赛威）
-- 采购经理(procurement_manager=1761300000000000014)持有资金管理/发票台账/
-- 上传发票/报销导出等管理菜单（46 个）。sys_user_role 无唯一约束，NOT EXISTS 防重。
-- 幂等：可重复执行。
-- =====================================================================
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, 1761300000000000014
FROM sys_user u
WHERE u.user_name IN ('wangjianlong', 'peitianzi', 'pengsaiwei')
  AND u.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
      AND ur.role_id = 1761300000000000014
  );
