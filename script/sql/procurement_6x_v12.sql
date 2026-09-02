-- =============================================================
-- procurement_6x_v12.sql（2026-09-02）
-- 1. 王建龙绑 superadmin 角色（RuoYi 超管判定 = user_id 1761100000000000001
--    或 拥有 role_id 1761300000000000001 的角色，二者任一即全系统超管待遇）
-- 2. 发票台账菜单 icon 修正（ticket 图标不存在，换 ep:collection）
-- 3. 发票台账不对普通用户展示：移除 common_user 与发票台账菜单的绑定
--    （超管角色绑全量菜单，无需显式绑定，无影响）
-- 4. invoice_info 加 matched_items 列：记录发票匹配到的商品名（AI 匹配落库）
-- 全部幂等，可重复执行。
-- =============================================================
SET NAMES utf8mb4;

-- ---------- 1. 王建龙绑 superadmin ----------
INSERT INTO sys_user_role (user_id, role_id)
SELECT 1761100000000000015, 1761300000000000001
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user_role
  WHERE user_id = 1761100000000000015 AND role_id = 1761300000000000001
);

-- ---------- 2. 发票台账菜单 icon 修正 ----------
UPDATE sys_menu SET icon = 'ep:collection' WHERE menu_id = 1801160 AND icon = 'ticket';

-- ---------- 3. 移除普通用户的发票台账菜单可见 ----------
DELETE FROM sys_role_menu
WHERE role_id = 1761300000000000013
  AND menu_id IN (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_id IN (1801160, 1801161, 1801162)) t);

-- ---------- 4. invoice_info 加 matched_items 列 ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_info' AND COLUMN_NAME = 'matched_items');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE invoice_info ADD COLUMN matched_items varchar(1000) DEFAULT NULL COMMENT ''匹配到的商品名（多个逗号分隔）'' AFTER ocr_json',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
