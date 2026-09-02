SET NAMES utf8mb4;
-- ============================================================
-- v9：发票台账菜单权限修复
-- ============================================================
-- 背景：procurement_role_menu_cleanup_v2.sql 清理 common_user 菜单后重建时
--       未包含发票台账菜单（1801160~1801162），导致台账无任何角色可见。
-- 幂等：先删后插
-- 另：仓库库存按钮权限（add/edit/remove）从 common_user 移除，
--     普通用户对库存只读（list/query），操作权限归仓库管理员。
-- ============================================================

SET @common_user_role_id = 1761300000000000013;

-- ---------- 1. 发票台账：common_user 绑菜单 + 查询（删除权限不给） ----------
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id IN (1801160, 1801161, 1801162);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (@common_user_role_id, 1801160),
  (@common_user_role_id, 1801161);

-- superadmin 特判走全量权限，无需绑定；admin 超管同上。

-- ---------- 2. 仓库库存：common_user 移除操作按钮，保留 list/query ----------
DELETE FROM sys_role_menu
WHERE role_id = @common_user_role_id
  AND menu_id IN (
    1801072,  -- 手动入库 procurement:warehouse:add
    1801073,  -- 库存修改 procurement:warehouse:edit
    1801074   -- 库存删除 procurement:warehouse:remove
  );

-- 校验
SELECT rm.menu_id, m.menu_name, m.perms FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @common_user_role_id AND rm.menu_id BETWEEN 1801070 AND 1801162 ORDER BY rm.menu_id;
