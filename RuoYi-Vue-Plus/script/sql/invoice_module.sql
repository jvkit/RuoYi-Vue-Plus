SET NAMES utf8mb4;

-- 发票管理模块：字典 + 菜单 + 权限

-- 字典类型：发票类型
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_by, create_time, remark)
VALUES (16, '000000', '发票类型', 'invoice_type', 1, sysdate(), '发票类型字典');

-- 发票类型字典数据
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_by, create_time, remark)
VALUES
(60, '000000', 1, '增值税普通发票', 'normal', 'invoice_type', '', 'default', 'Y', 1, sysdate(), ''),
(61, '000000', 2, '增值税专用发票', 'special', 'invoice_type', '', 'primary', 'N', 1, sysdate(), ''),
(62, '000000', 3, '电子普通发票', 'electronic', 'invoice_type', '', 'success', 'N', 1, sysdate(), '');

-- 字典类型：发票状态
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_by, create_time, remark)
VALUES (17, '000000', '发票状态', 'invoice_status', 1, sysdate(), '发票状态字典');

-- 发票状态字典数据
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_by, create_time, remark)
VALUES
(63, '000000', 1, '草稿', 'draft', 'invoice_status', '', 'info', 'Y', 1, sysdate(), ''),
(64, '000000', 2, '已提交', 'submitted', 'invoice_status', '', 'primary', 'N', 1, sysdate(), ''),
(65, '000000', 3, '已认证', 'approved', 'invoice_status', '', 'success', 'N', 1, sysdate(), ''),
(66, '000000', 4, '已驳回', 'rejected', 'invoice_status', '', 'danger', 'N', 1, sysdate(), '');

-- 菜单：发票管理目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12007, '发票管理', 0, 9, 'invoice', '', '', 1, 0, 'M', '0', '0', '', 'ticket', 1, sysdate(), '发票管理目录');

-- 菜单：发票信息管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12008, '发票信息', 12007, 1, 'info', 'invoice/info/index', '', 1, 0, 'C', '0', '0', 'invoice:info:list', 'edit', 1, sysdate(), '发票信息菜单');

-- 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12009, '发票信息查询', 12008, 1, '#', '', '', 1, 0, 'F', '0', '0', 'invoice:info:query', '#', 1, sysdate(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12010, '发票信息新增', 12008, 2, '#', '', '', 1, 0, 'F', '0', '0', 'invoice:info:add', '#', 1, sysdate(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12011, '发票信息修改', 12008, 3, '#', '', '', 1, 0, 'F', '0', '0', 'invoice:info:edit', '#', 1, sysdate(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12012, '发票信息删除', 12008, 4, '#', '', '', 1, 0, 'F', '0', '0', 'invoice:info:remove', '#', 1, sysdate(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (12013, '发票信息导出', 12008, 5, '#', '', '', 1, 0, 'F', '0', '0', 'invoice:info:export', '#', 1, sysdate(), '');
