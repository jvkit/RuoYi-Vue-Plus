SET NAMES utf8mb4;
-- ============================================================
-- 发票管理模块初始化脚本（ruoyi-6x / ry-vue-6x）
-- 由 5.6.2 迁移而来，适配 6.x：移除多租户（无 tenant_id）、雪花 BigInt id。
-- 可重复执行（幂等）：建表用 CREATE TABLE IF NOT EXISTS，菜单/字典先删后插。
-- 导入库：ry-vue-6x
-- ============================================================

SET @admin_id = 1761100000000000001; -- admin 用户 id

-- ============================================================
-- 1. 发票信息表（含 AI 审核/真伪查验/财务单号/订单号列，6x 无租户）
-- ============================================================
CREATE TABLE IF NOT EXISTS `invoice_info` (
  `id` bigint NOT NULL COMMENT '主键id',
  `invoice_code` varchar(50) DEFAULT NULL COMMENT '发票代码',
  `invoice_number` varchar(50) NOT NULL COMMENT '发票号码',
  `invoice_type` varchar(20) DEFAULT 'normal' COMMENT '发票类型（normal普票/special专票/electronic电子发票）',
  `amount` decimal(18,2) DEFAULT NULL COMMENT '不含税金额',
  `tax_amount` decimal(18,2) DEFAULT NULL COMMENT '税额',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '价税合计',
  `invoice_date` date DEFAULT NULL COMMENT '开票日期',
  `seller_name` varchar(255) DEFAULT NULL COMMENT '销售方名称',
  `buyer_name` varchar(255) DEFAULT NULL COMMENT '购买方名称',
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态（draft草稿/submitted已提交/approved已认证/rejected已驳回）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `ai_opinion` text COMMENT 'AI审核意见',
  `verify_status` varchar(20) DEFAULT 'unverified' COMMENT '真伪状态 unverified/real/fake/failed',
  `verify_time` datetime DEFAULT NULL COMMENT '查验时间',
  `fin_query_no` varchar(50) DEFAULT NULL COMMENT '财务查询单号',
  `order_no` varchar(100) DEFAULT NULL COMMENT '关联订单号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` bigint DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票信息表';

-- ============================================================
-- 2. 发票使用记录表（修正 5.6.2 缺失的 del_flag）
-- ============================================================
CREATE TABLE IF NOT EXISTS `invoice_usage_record` (
  `id` bigint NOT NULL COMMENT '主键id',
  `invoice_id` bigint NOT NULL COMMENT '发票ID',
  `biz_type` varchar(20) DEFAULT NULL COMMENT '业务类型 reimbursement/payment/purchase',
  `biz_no` varchar(50) DEFAULT NULL COMMENT '业务单号',
  `used_by` bigint DEFAULT NULL COMMENT '使用人',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `used_amount` decimal(18,2) DEFAULT NULL COMMENT '使用金额',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` bigint DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_invoice_id` (`invoice_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票使用记录表';

-- ============================================================
-- 3. 字典：发票类型 / 发票状态（先删后插，幂等）
-- ============================================================
DELETE FROM sys_dict_data WHERE dict_type IN ('invoice_type', 'invoice_status');
DELETE FROM sys_dict_type WHERE dict_type IN ('invoice_type', 'invoice_status');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1805001, '发票类型', 'invoice_type', NULL, @admin_id, sysdate(), NULL, NULL, '发票类型字典');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1806001, 1, '增值税普通发票', 'normal',    'invoice_type', '', 'default', 'Y', NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1806002, 2, '增值税专用发票', 'special',   'invoice_type', '', 'primary', 'N', NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1806003, 3, '电子普通发票',   'electronic','invoice_type', '', 'success', 'N', NULL, @admin_id, sysdate(), NULL, NULL, '');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1805002, '发票状态', 'invoice_status', NULL, @admin_id, sysdate(), NULL, NULL, '发票状态字典');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1806004, 1, '草稿',   'draft',     'invoice_status', '', 'info',    'Y', NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1806005, 2, '已提交', 'submitted', 'invoice_status', '', 'primary', 'N', NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1806006, 3, '已认证', 'approved',  'invoice_status', '', 'success', 'N', NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1806007, 4, '已驳回', 'rejected',  'invoice_status', '', 'danger',  'N', NULL, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 4. 菜单：发票管理（目录+发票信息+按钮+发票提交+发票使用记录，先删后插，幂等）
--    图标用 Element Plus（ep: 前缀，前端已离线注入），保证离线可显示
-- ============================================================
DELETE FROM sys_menu WHERE menu_id >= 1804000 AND menu_id < 1805000;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1804000, '发票管理', 0, 9, 'invoice', NULL, NULL, 'N', 'Y', 'M', '0', '0', NULL, 'ep:ticket', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '发票管理目录');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1804001, '发票信息', 1804000, 1, 'info', 'invoice/info/index', NULL, 'N', 'Y', 'C', '0', '0', 'invoice:info:list', 'ep:edit', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '发票信息菜单');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1804002, '发票信息查询', 1804001, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:info:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804003, '发票信息新增', 1804001, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:info:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804004, '发票信息修改', 1804001, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:info:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804005, '发票信息删除', 1804001, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:info:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804006, '发票信息导出', 1804001, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:info:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1804007, '发票提交', 1804000, 2, 'employee', 'invoice/employee/index', NULL, 'N', 'Y', 'C', '0', '0', 'invoice:employee:list', 'ep:upload', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '发票提交（员工上传+AI识别）菜单');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1804008, '发票使用记录', 1804000, 3, 'usage', 'invoice/usage/index', NULL, 'N', 'Y', 'C', '0', '0', 'invoice:usage:list', 'ep:document', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '发票使用记录菜单');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1804009, '使用记录查询', 1804008, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:usage:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804010, '使用记录新增', 1804008, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:usage:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804011, '使用记录修改', 1804008, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:usage:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, ''),
(1804012, '使用记录删除', 1804008, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'invoice:usage:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
