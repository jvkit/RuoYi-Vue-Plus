SET NAMES utf8mb4;
-- ============================================================
-- 采购管理模块增量脚本（ruoyi-6x / ry-vue-6x）
-- 在 procurement_6x.sql（基线：建表 + 菜单 + 字典 + 流程定义）基础上，固化后续手工数据库改动。
-- 可重复执行（幂等）：加列用 information_schema 判断、字典/角色先删后插、图标用 UPDATE。
-- 导入库：ry-vue-6x
-- ============================================================

SET @admin_id = 1761100000000000001; -- admin 用户 id

-- ============================================================
-- 1. 采购申请表增加 采购方式/标题名称 列
--    申请标题由 采购方式(自购/对公) + 标题名称 自动拼接，界面不手填 title。
--    MySQL 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 判断保证幂等。
-- ============================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'title_type');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE pms_procurement_request ADD COLUMN title_type varchar(32) DEFAULT NULL COMMENT ''采购方式（自购/对公）''',
    'SELECT ''pms_procurement_request.title_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'title_name');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE pms_procurement_request ADD COLUMN title_name varchar(200) DEFAULT NULL COMMENT ''标题名称''',
    'SELECT ''pms_procurement_request.title_name 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 采购类型字典：基线为 物资/服务/固定资产(goods/service/fixed_asset)，
--    改为 材料/设备/服务/工程/危化品/其他，且材料(material)排第一（默认选中材料）。
--    先删后插，保证幂等。
-- ============================================================
DELETE FROM sys_dict_data WHERE dict_type = 'pms_purchase_type';
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_by, create_time, remark) VALUES
(1803011, 1, '材料',   'material',    'pms_purchase_type', '', 'primary', 'N', @admin_id, sysdate(), ''),
(1803010, 2, '设备',   'equipment',   'pms_purchase_type', '', 'primary', 'N', @admin_id, sysdate(), ''),
(1803012, 3, '服务',   'service',     'pms_purchase_type', '', 'primary', 'N', @admin_id, sysdate(), ''),
(1803013, 4, '工程',   'engineering', 'pms_purchase_type', '', 'primary', 'N', @admin_id, sysdate(), ''),
(1803014, 5, '危化品', 'dangerous',   'pms_purchase_type', '', 'primary', 'N', @admin_id, sysdate(), ''),
(1803015, 6, '其他',   'other',       'pms_purchase_type', '', 'primary', 'N', @admin_id, sysdate(), '');

-- ============================================================
-- 3. 采购菜单图标纠正：基线用了本地 svg 不存在的图标名（侧边栏不显示），
--    改为 Element Plus 图标（ep: 前缀，前端已离线注入 @iconify-json/ep）。
--    UPDATE 天然幂等。
-- ============================================================
UPDATE sys_menu SET icon = 'ep:shopping-cart'  WHERE menu_id = 1801000; -- 采购管理
UPDATE sys_menu SET icon = 'ep:office-building' WHERE menu_id = 1801010; -- 采购项目
UPDATE sys_menu SET icon = 'ep:grid'            WHERE menu_id = 1801030; -- BOM
UPDATE sys_menu SET icon = 'ep:goods'           WHERE menu_id = 1801050; -- 采购订单

-- ============================================================
-- 4. 新增角色「采购专员」（role_key=purchase_staff）：
--    可见整个采购模块（项目/供应商/BOM/申请/订单），完整增删改查，不暴露系统管理等其它菜单。
--    先删后插，保证幂等。
-- ============================================================
DELETE FROM sys_role_menu WHERE role_id = 1761300000000000005;
DELETE FROM sys_role     WHERE role_id = 1761300000000000005;

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES (1761300000000000005, '采购专员', 'purchase_staff', 3, '1', 0, 1, '0', '0', @admin_id, sysdate(), '采购模块完整增删改查');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1761300000000000005, menu_id FROM sys_menu WHERE menu_id >= 1801000 AND menu_id < 1802000;
