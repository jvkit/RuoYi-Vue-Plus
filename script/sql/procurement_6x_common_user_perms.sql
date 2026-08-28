SET NAMES utf8mb4;

-- ============================================================
-- 普通用户采购模块基础权限补齐
-- ============================================================
-- 目标：所有普通用户都能看到采购相关页面、提交采购申请/验收/领用，
--       先不做数据隔离（全体可见），但保留关键管理操作权限（如项目管理、
--       手动入库、订单/BOM 增删改）不开放给普通用户。
-- 幂等：已授权则跳过。
-- ============================================================

SET @common_role_id = (SELECT role_id FROM sys_role WHERE role_name = '普通用户' AND status = '0' AND del_flag = '0' LIMIT 1);

-- 若不存在普通用户角色则直接退出
SELECT @common_role_id AS common_role_id;

-- 1. 采购管理父目录（必须在，否则左侧菜单不显示）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, 1801000
WHERE @common_role_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = 1801000);

-- 2. 项目管理：仅查看 + 树选择（用于采购申请下拉），不开放增删改
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND m.menu_id IN (1801010, 1801011, 1801016)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

-- 3. 采购申请：全部权限（查看 + 增删改 + 导出 + 审批详情）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND (m.parent_id = 1801040 OR m.menu_id = 1801040 OR m.menu_id = 1801046)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

-- 4. 采购验收：全部权限（查看 + 增删改 + 导出 + 提交 + 审批详情）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND (m.parent_id = 1801060 OR m.menu_id = 1801060 OR m.menu_id = 1801067)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

-- 5. 仓库库存：仅查看，不开放手动入库/修改/删除
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND (m.menu_id = 1801070 OR m.parent_id = 1801070 AND m.perms LIKE 'procurement:warehouse:query%')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

-- 6. 领用申请：全部权限（查看 + 增删改 + 提交 + 审批详情）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND (m.parent_id = 1801080 OR m.menu_id = 1801080 OR m.menu_id = 1801087)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

-- 7. 其他采购菜单仅开放查看（供应商、采购订单、BOM、物料库、BOM表、流转记录、合同、报销、资金管理）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND m.menu_type = 'C'
  AND m.menu_id IN (1801020, 1801050, 1801030, 1801090, 1801100, 1801110, 1801120, 1801130, 1801140)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND m.menu_type = 'F'
  AND m.perms LIKE '%:query'
  AND m.parent_id IN (1801020, 1801050, 1801030, 1801090, 1801100, 1801110, 1801120, 1801130, 1801140)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);

-- 8. OSS 文件上传/下载（采购申请付款截图、验收拍照、发票等都需要）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @common_role_id, m.menu_id
FROM sys_menu m
WHERE @common_role_id IS NOT NULL
  AND m.menu_id IN (1761400000000000118, 1761400000000001601, 1761400000000001602)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @common_role_id AND menu_id = m.menu_id);
