SET NAMES utf8mb4;
-- ============================================================================
-- v22 发票改挂按钮 + 采购经理权限补绑（2026-09-24）
-- 1) 新增按钮 1801164「发票改挂」procurement:invoice:assign（发票台账下，拖拽修正用）
-- 2) 采购经理角色（1761300000000000014）补绑：删除(1801162)/改挂(1801164)/查询(1801161)
-- ============================================================================

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `remark`)
SELECT 1801164, '发票改挂', 1801160, 4, '#', 'N', 'Y', 'F', '0', '0', 'procurement:invoice:assign', '#', NOW(), '发票台账拖拽改挂按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 1801164);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1761300000000000014, 1801161
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1761300000000000014 AND `menu_id` = 1801161);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1761300000000000014, 1801162
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1761300000000000014 AND `menu_id` = 1801162);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1761300000000000014, 1801164
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1761300000000000014 AND `menu_id` = 1801164);
