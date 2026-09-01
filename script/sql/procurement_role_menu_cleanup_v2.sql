-- ============================================================
-- 采购模块角色菜单权限调整 v2
-- 目的：
--   1. 普通用户需要 procurement:request:submit 权限才能提交采购申请。
--   2. 项目管理、发票台账不再对普通用户可见，但保留项目树选择按钮
--      供采购申请表单使用。
-- 幂等：可重复执行。
-- ============================================================
SET NAMES utf8mb4;

SET @role_common_user = 1761300000000000013;

-- --------------------------------------------------------
-- 1. 补全「采购申请提交」按钮权限
-- --------------------------------------------------------
-- 菜单不存在则创建（按钮类型，挂在采购申请下）
INSERT IGNORE INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES (
    1801047, '采购申请提交', 1801040, 6, '#', NULL,
    'F', '0', '0', 'procurement:request:submit', '#', 'admin', NOW(), 'admin', NOW(), '采购申请-提交并启动流程'
);

-- 绑定给普通用户
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (@role_common_user, 1801047);

-- --------------------------------------------------------
-- 2. 对普通用户隐藏「项目管理」菜单
-- --------------------------------------------------------
DELETE FROM sys_role_menu
WHERE role_id = @role_common_user AND menu_id IN (1801010, 1801011);
-- 保留 1801016 项目树选择按钮，保证采购申请里的项目选择控件可用

-- --------------------------------------------------------
-- 3. 对普通用户隐藏「发票台账」菜单
-- --------------------------------------------------------
DELETE FROM sys_role_menu
WHERE role_id = @role_common_user AND menu_id IN (1801160, 1801161, 1801162);
