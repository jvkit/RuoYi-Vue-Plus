SET NAMES utf8mb4;
-- ============================================================
-- 采购 v5：角色改名、李迪升 admin、普通用户菜单精简
-- 幂等：可重复执行
-- ============================================================

-- ---------- 1. 角色改名 ----------
UPDATE sys_role SET role_name = 'CEO' WHERE role_key = 'team_leader';
UPDATE sys_role SET role_name = '最高决策人' WHERE role_key = 'dept_leader';

-- ---------- 2. 李迪绑定 superadmin（admin 权限）----------
SET @lidi_user_id = 1761100000000000016;
SET @superadmin_role_id = 1761300000000000001;

-- 幂等：先删后插
DELETE FROM sys_user_role WHERE user_id = @lidi_user_id AND role_id = @superadmin_role_id;
INSERT INTO sys_user_role (user_id, role_id) VALUES (@lidi_user_id, @superadmin_role_id);

-- ---------- 3. 普通用户菜单精简 ----------
SET @common_user_role_id = 1761300000000000013;

-- 3.1 先移除普通用户不需要的菜单绑定
DELETE FROM sys_role_menu
WHERE role_id = @common_user_role_id
  AND menu_id IN (
    1801010,  -- 项目管理
    1801011, 1801012, 1801013, 1801014, 1801015, 1801016, -- 项目管理按钮
    1801020,  -- 供应商
    1801021, 1801022, 1801023, 1801024, 1801025, -- 供应商按钮
    1801030,  -- BOM管理
    1801031, 1801032, 1801033, 1801034, 1801035, -- BOM管理按钮
    1801090,  -- BOM分类
    1801091, 1801092, 1801093, 1801094, -- BOM分类按钮
    1801100,  -- BOM表格
    1801101, 1801102, 1801103, 1801104, -- BOM表格按钮
    1801110,  -- 操作日志
    1801111,  -- 操作日志按钮
    1801120,  -- 合同
    1801121, 1801122, 1801123, -- 合同按钮
    1801130,  -- 报销
    1801131, 1801132, 1801133, -- 报销按钮
    1801140,  -- 资金
    1801141, 1801142, -- 资金按钮
    -- OSS 管理菜单对普通用户隐藏，但上传/下载按钮权限需要保留，见下方 3.3
    1761400000000011622,  -- 流程分类管理
    1761400000000011623, 1761400000000011624, 1761400000000011625, 1761400000000011626, 1761400000000011627, -- 流程分类按钮
    1761400000000011631,  -- 所有待办（管理视角）
    1761400000000011660,  -- 所有待办编辑按钮
    1761400000000011801,  -- Spel管理
    1761400000000011802, 1761400000000011803, 1761400000000011804, 1761400000000011805, 1761400000000011806  -- Spel按钮
  );

-- 3.2 移除普通用户对流程定义/实例的管理按钮权限，只保留查询
DELETE FROM sys_role_menu
WHERE role_id = @common_user_role_id
  AND menu_id IN (
    1761400000000011645, -- workflow:definition:add
    1761400000000011646, -- workflow:definition:edit
    1761400000000011647, -- workflow:definition:remove
    1761400000000011648, -- workflow:definition:export
    1761400000000011649, -- workflow:definition:import
    1761400000000011650, -- workflow:definition:publish
    1761400000000011651, -- workflow:definition:copy
    1761400000000011652, -- workflow:definition:active
    1761400000000011654, -- workflow:instance:variableQuery
    1761400000000011655, -- workflow:instance:variable
    1761400000000011656, -- workflow:instance:active
    1761400000000011657, -- workflow:instance:remove
    1761400000000011658, -- workflow:instance:invalid
    1761400000000011659  -- workflow:instance:cancel
  );

-- 3.3 确保普通用户有必要菜单（幂等：先删后插避免重复）
-- 采购管理目录
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801000;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801000);

-- 采购申请
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801040;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801040);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801041;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801041);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801042;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801042);

-- 采购验收
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801060;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801060);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801061;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801061);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801062;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801062);

-- 仓库库存
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801070;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801070);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801071;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801071);

-- 领用申请
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801080;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801080);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801081;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801081);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801082;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801082);

-- 手机端专属
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801150;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801150);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1801151;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1801151);

-- PRIME AI
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1805000;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1805000);

-- 工作流目录（只读）
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011616;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011616);
-- 流程定义列表
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011620;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011620);
-- 流程实例列表
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011630;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011630);
-- 流程定义/实例查询按钮
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011644;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011644);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011653;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011653);

-- 我的任务目录
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011618;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011618);
-- 我的待办
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011619;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011619);
-- 我的已办
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011632;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011632);
-- 我的请求
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000011629;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000011629);

-- OSS 上传/下载按钮权限（不显示 OSS 管理菜单，但需要上传发票/截图）
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000001601;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000001601);
DELETE FROM sys_role_menu WHERE role_id = @common_user_role_id AND menu_id = 1761400000000001602;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (@common_user_role_id, 1761400000000001602);

-- 校验
SELECT role_id, role_name, role_key FROM sys_role WHERE role_key IN ('team_leader', 'dept_leader');
SELECT r.role_name, u.user_name FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.role_id JOIN sys_user u ON ur.user_id = u.user_id WHERE u.user_name = 'lidi';
SELECT COUNT(*) AS common_user_menu_count FROM sys_role_menu WHERE role_id = @common_user_role_id;
