SET NAMES utf8mb4;

-- ============================================================
-- 采购模块移动端验收入口菜单
-- ============================================================
-- 在系统菜单中增加「手机端专属」目录及其下的「移动验收」菜单，
-- 组件指向 mobile/acceptance/index，前端已通过 constantRoutes 注册同路径路由，
-- 点击菜单或手机直接访问 /mobile/acceptance 均可进入全屏移动端验收页。
-- 幂等：菜单存在则更新，角色授权已存在则跳过。
-- ============================================================

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES
  (1801150, '手机端专属', 0, 99, 'mobile', NULL, 'M', 0, 0, NULL, 'phone', NOW())
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  menu_type = VALUES(menu_type),
  visible = VALUES(visible),
  status = VALUES(status),
  icon = VALUES(icon);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES
  (1801151, '移动验收', 1801150, 1, 'acceptance', 'mobile/acceptance/index', 'C', 0, 0, 'procurement:mobile:acceptance', 'mobile', NOW())
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  parent_id = VALUES(parent_id),
  path = VALUES(path),
  component = VALUES(component),
  menu_type = VALUES(menu_type),
  visible = VALUES(visible),
  status = VALUES(status),
  perms = VALUES(perms),
  icon = VALUES(icon);

-- 给所有正常角色授权「手机端专属」目录与「移动验收」菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 1801150
FROM sys_role r
WHERE r.status = '0'
  AND r.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id
      AND rm.menu_id = 1801150
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 1801151
FROM sys_role r
WHERE r.status = '0'
  AND r.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id
      AND rm.menu_id = 1801151
  );
