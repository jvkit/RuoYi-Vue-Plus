SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：普通用户基础角色 + 项目资金修正（幂等，可重复执行）
--   1. 创建「普通用户」(common_user) 角色，绑定公共面板（我的任务树）
--      → 未来新用户只需绑此角色即可拥有基础面板，业务角色只绑业务菜单
--   2. 王建龙/李迪/裴天姿 绑定 common_user（一人多角色，叠加业务角色）
--   3. 项目资金按层级修正：一级(parent_id=0) 100万、二级 10万
--   4. 裴天姿 加绑 warehouse_admin 角色（领用申请审批）
--   说明：
--     * 首页/个人中心是前端静态路由，登录即见，无需 DB 菜单
--     * 超级管理员(superadmin) 自动拥有全部菜单，不经过 sys_role_menu
-- ============================================================

-- ---------- 1. 创建「普通用户」角色 ----------
SET @role_common = 1761300000000000013;
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, remark)
SELECT @role_common, '普通用户', 'common_user', 99, 5, 1, 1, '0', 0, 1761000000000000103, 1, sysdate(), '基础面板（我的任务），所有业务用户都应绑定'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = @role_common);

-- ---------- 2. 绑定公共菜单（我的任务树）----------
-- 我的任务 M(1761400000000011618) + 待办/我发起/已办/抄送
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_common, menu_id FROM (
  SELECT 1761400000000011618 AS menu_id UNION ALL  -- 我的任务(目录)
  SELECT 1761400000000011619 UNION ALL             -- 我的待办
  SELECT 1761400000000011629 UNION ALL             -- 我发起的
  SELECT 1761400000000011632 UNION ALL             -- 我的已办
  SELECT 1761400000000011633                       -- 我的抄送
) t
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu WHERE role_id = @role_common AND menu_id = t.menu_id
);

-- ---------- 3. 绑定用户到 common_user ----------
-- 王建龙/李迪/裴天姿
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, @role_common FROM sys_user u
WHERE u.user_name IN ('wangjianlong','lidi','peitianzi') AND u.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = @role_common
  );

-- ---------- 4. 裴天姿 加绑 warehouse_admin ----------
SET @role_wh = 1761300000000000012;
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, @role_wh FROM sys_user u
WHERE u.user_name = 'peitianzi' AND u.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = @role_wh
  );

-- warehouse_admin 绑采购菜单（若尚未绑定，确保裴天姿能看到采购模块）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_wh, m.menu_id FROM sys_menu m
WHERE m.menu_id BETWEEN 1801000 AND 1801999 AND m.menu_type IN ('M','C')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = @role_wh AND rm.menu_id = m.menu_id
  );

-- ---------- 5. 项目资金按层级修正 ----------
-- 一级(parent_id=0)：100万；二级(parent_id!=0)：10万（统一标准默认值）
UPDATE pms_project SET budget = 1000000.00 WHERE parent_id = 0 AND del_flag = 0;
UPDATE pms_project SET budget = 100000.00 WHERE parent_id != 0 AND del_flag = 0;

-- ---------- 校验 ----------
SELECT '=== 普通用户角色 ===' AS '';
SELECT role_id, role_name, role_key FROM sys_role WHERE role_id = @role_common;
SELECT '=== 普通用户菜单 ===' AS '';
SELECT rm.menu_id, m.menu_name FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE rm.role_id = @role_common ORDER BY m.menu_id;
SELECT '=== 用户-角色绑定 ===' AS '';
SELECT u.user_name, r.role_key FROM sys_user_role ur JOIN sys_user u ON u.user_id=ur.user_id JOIN sys_role r ON r.role_id=ur.role_id WHERE u.user_name IN ('wangjianlong','lidi','peitianzi') ORDER BY u.user_name, r.role_key;
SELECT '=== 项目资金 ===' AS '';
SELECT project_name, parent_id, budget FROM pms_project WHERE del_flag = 0 ORDER BY parent_id, id;
