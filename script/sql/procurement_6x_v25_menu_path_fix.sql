-- 修正报销导出/资金管理/上传发票三个菜单的 path：误写为绝对路径导致动态路由 404，改回与兄弟菜单一致的相对路径
-- 幂等：直接 UPDATE 固定值，可重复执行
SET NAMES utf8mb4;

UPDATE sys_menu SET path = 'reimbursement'    WHERE menu_id = 1801130;
UPDATE sys_menu SET path = 'fund'             WHERE menu_id = 1801140;
UPDATE sys_menu SET path = 'invoiceWorkbench' WHERE menu_id = 1801170;
