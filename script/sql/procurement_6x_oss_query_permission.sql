SET NAMES utf8mb4;

-- ============================================================
-- 采购模块审批视角查看附件/图片所需 OSS 查询权限
-- ============================================================
-- 问题：采购申请/验收/领用等详情页会把图片、报价单、发票附件等字段存为 ossId，
--       前端在审批视角通过 /resource/oss/listByIds 查询 URL，需要 system:oss:query 权限。
-- 修复：给所有正常状态角色统一补授权限按钮 menu_id = 1761400000000001600（system:oss:查询）。
-- 幂等：已授权的角色不会重复插入。
-- ============================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 1761400000000001600
FROM sys_role r
WHERE r.status = '0'
  AND r.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id
      AND rm.menu_id = 1761400000000001600
  );
