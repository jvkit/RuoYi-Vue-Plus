-- ============================================================
-- 采购模块角色菜单权限清理与补全
-- 目的：
--   1. 审批角色（team_leader/dept_leader/warehouse_admin/acceptance_contact）
--      仅用于 Warm-Flow 节点找人，不再绑定任何菜单，避免角色并集导致菜单显示混乱。
--   2. 普通用户（common_user）作为唯一「显示角色」，补全缺失的必要菜单/按钮。
--   3. 从 common_user 移除不应显示的「流程监控」目录。
-- 幂等：可重复执行。
-- ============================================================
SET NAMES utf8mb4;

-- --------------------------------------------------------
-- 1. 角色 ID 变量
-- --------------------------------------------------------
SET @role_team_leader       = 1761300000000000009;
SET @role_dept_leader       = 1761300000000000010;
SET @role_acceptance_contact = 1761300000000000011;
SET @role_warehouse_admin   = 1761300000000000012;
SET @role_common_user       = 1761300000000000013;

-- --------------------------------------------------------
-- 2. 清空审批角色的菜单绑定（保留角色本身用于流程）
-- --------------------------------------------------------
DELETE FROM sys_role_menu
WHERE role_id IN (
    @role_team_leader,
    @role_dept_leader,
    @role_acceptance_contact,
    @role_warehouse_admin
);

-- --------------------------------------------------------
-- 3. 为 common_user 补全缺失的菜单/按钮
-- --------------------------------------------------------

-- 3.1 项目管理（平级菜单，采购申请选择项目时需要）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (@role_common_user, 1801010); -- 项目管理
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (@role_common_user, 1801011); -- 项目管理查询
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (@role_common_user, 1801016); -- 项目树选择

-- 3.2 流程设计详情页（隐藏菜单，我的任务审批跳转用）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (@role_common_user, 1761400000000011700); -- 流程设计

-- --------------------------------------------------------
-- 4. 从 common_user 移除不应显示的菜单
-- --------------------------------------------------------

-- 流程监控目录（普通用户只看「流程定义」，流程监控给管理员）
DELETE FROM sys_role_menu
WHERE role_id = @role_common_user AND menu_id = 1761400000000011630;

-- 流程实例查询按钮（无对应可见菜单，且不在「只看流程定义」范围内）
DELETE FROM sys_role_menu
WHERE role_id = @role_common_user AND menu_id = 1761400000000011653;
