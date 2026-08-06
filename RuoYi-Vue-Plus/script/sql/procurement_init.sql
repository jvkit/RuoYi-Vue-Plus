SET NAMES utf8mb4;
-- ----------------------------
-- 采购管理模块初始化脚本
-- 包含：菜单、数据表、采购申请流程定义
-- 执行后请清空 Redis 缓存并重新登录
-- ----------------------------

-- 清理旧版采购管理菜单（12000-12099 区段）
DELETE FROM sys_menu WHERE menu_id BETWEEN 12000 AND 12059;

-- 采购管理目录
insert into sys_menu values ('12000', '采购管理', '0', '8', 'procurement', '', '', '1', '0', 'M', '0', '0', '', 'shopping-cart', 103, 1, sysdate(), NULL, NULL, '采购管理目录');

-- 采购项目
insert into sys_menu values ('12010', '采购项目', '12000', '1', 'project', 'procurement/project/index', '', '1', '0', 'C', '0', '0', 'procurement:project:list', 'office-building', 103, 1, sysdate(), NULL, NULL, '采购项目菜单');
insert into sys_menu values ('12011', '采购项目查询', '12010', '1', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:project:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12012', '采购项目新增', '12010', '2', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:project:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12013', '采购项目修改', '12010', '3', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:project:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12014', '采购项目删除', '12010', '4', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:project:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12015', '采购项目导出', '12010', '5', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:project:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 供应商管理
insert into sys_menu values ('12020', '供应商管理', '12000', '2', 'supplier', 'procurement/supplier/index', '', '1', '0', 'C', '0', '0', 'procurement:supplier:list', 'user', 103, 1, sysdate(), NULL, NULL, '供应商管理菜单');
insert into sys_menu values ('12021', '供应商查询', '12020', '1', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:supplier:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12022', '供应商新增', '12020', '2', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:supplier:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12023', '供应商修改', '12020', '3', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:supplier:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12024', '供应商删除', '12020', '4', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:supplier:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12025', '供应商导出', '12020', '5', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:supplier:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- BOM 物料清单
insert into sys_menu values ('12030', 'BOM物料清单', '12000', '3', 'bom', 'procurement/bom/index', '', '1', '0', 'C', '0', '0', 'procurement:bom:list', 'grid', 103, 1, sysdate(), NULL, NULL, 'BOM物料清单菜单');
insert into sys_menu values ('12031', 'BOM查询', '12030', '1', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:bom:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12032', 'BOM新增', '12030', '2', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:bom:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12033', 'BOM修改', '12030', '3', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:bom:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12034', 'BOM删除', '12030', '4', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:bom:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12035', 'BOM导出', '12030', '5', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:bom:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 采购申请
insert into sys_menu values ('12040', '采购申请', '12000', '4', 'request', 'procurement/request/index', '', '1', '0', 'C', '0', '0', 'procurement:request:list', 'edit', 103, 1, sysdate(), NULL, NULL, '采购申请菜单');
insert into sys_menu values ('12041', '采购申请查询', '12040', '1', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:request:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12042', '采购申请新增', '12040', '2', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:request:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12043', '采购申请修改', '12040', '3', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:request:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12044', '采购申请删除', '12040', '4', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:request:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12045', '采购申请导出', '12040', '5', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:request:export', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12046', '采购申请提交', '12040', '6', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:request:submit', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 采购申请数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS pms_procurement_request
(
    `id`                  bigint          NOT NULL COMMENT '申请ID',
    `request_code`        varchar(64)     DEFAULT NULL COMMENT '申请编号',
    `title`               varchar(200)    NOT NULL COMMENT '申请标题',
    `project_id`          bigint          NOT NULL COMMENT '项目ID',
    `supplier_id`         bigint          DEFAULT NULL COMMENT '供应商ID',
    `amount`              decimal(18, 2)  DEFAULT '0.00' COMMENT '总金额',
    `purchase_type`       varchar(32)     DEFAULT NULL COMMENT '采购类型（goods物资 service服务 fixed_asset固定资产）',
    `apply_reason`        varchar(1000)   DEFAULT NULL COMMENT '申请原因',
    `status`              varchar(32)     DEFAULT 'draft' COMMENT '状态',
    `process_instance_id` bigint          DEFAULT NULL COMMENT '流程实例ID',
    `remark`              varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`         bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`           bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`         datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`           bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`         datetime        DEFAULT NULL COMMENT '更新时间',
    `tenant_id`           varchar(40)     DEFAULT NULL COMMENT '租户ID',
    `del_flag`            bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_request_code` (`request_code`) USING BTREE,
    KEY `idx_project_id` (`project_id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购申请';

CREATE TABLE IF NOT EXISTS pms_procurement_request_item
(
    `id`           bigint          NOT NULL COMMENT '明细ID',
    `request_id`   bigint          NOT NULL COMMENT '申请ID',
    `item_name`    varchar(200)    NOT NULL COMMENT '品名',
    `spec`         varchar(200)    DEFAULT NULL COMMENT '规格型号',
    `brand`        varchar(100)    DEFAULT NULL COMMENT '品牌',
    `unit`         varchar(50)     DEFAULT NULL COMMENT '单位',
    `quantity`     decimal(18, 4)  DEFAULT '0.0000' COMMENT '数量',
    `unit_price`   decimal(18, 4)  DEFAULT '0.0000' COMMENT '单价',
    `amount`       decimal(18, 2)  DEFAULT '0.00' COMMENT '金额',
    `bom_item_id`  bigint          DEFAULT NULL COMMENT '关联BOM条目ID',
    `sort_no`      int(4)          DEFAULT '0' COMMENT '排序号',
    `remark`       varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`  bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`    bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`    bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime        DEFAULT NULL COMMENT '更新时间',
    `tenant_id`    varchar(40)     DEFAULT NULL COMMENT '租户ID',
    `del_flag`     bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_request_id` (`request_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购申请明细';

-- ----------------------------
-- 采购申请流程定义（Warm-Flow，默认租户 000000）
-- ----------------------------
DELETE FROM flow_definition WHERE flow_code = 'pms_request' AND tenant_id = '000000';
DELETE FROM flow_node WHERE id IN (900000000000000011, 900000000000000012, 900000000000000013);
DELETE FROM flow_skip WHERE id IN (900000000000000021, 900000000000000022, 900000000000000023);

INSERT INTO flow_definition (`id`, `flow_code`, `flow_name`, `model_value`, `category`, `version`, `is_publish`, `form_custom`, `form_path`, `activity_status`, `listener_type`, `listener_path`, `ext`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000001, 'pms_request', '采购申请审批', 'CLASSICS', '100', '1.0', 1, 'N', NULL, 1, NULL, NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000011, 0, 900000000000000001, 'start', '开始', NULL, NULL, '100,50', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000012, 1, 900000000000000001, 'manager', '审批', '${initiator}', NULL, '300,50', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_node (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`)
VALUES (900000000000000013, 2, 900000000000000001, 'end', '结束', NULL, NULL, '500,50', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000021, 900000000000000001, 'start', 0, 'manager', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000022, 900000000000000001, 'manager', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

INSERT INTO flow_skip (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`)
VALUES (900000000000000023, 900000000000000001, 'manager', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');

-- ----------------------------
-- 采购类型字典
-- ----------------------------
INSERT INTO sys_dict_type VALUES ('100', '000000', '采购类型', 'pms_purchase_type', 103, 1, sysdate(), NULL, NULL, '采购类型')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);
INSERT INTO sys_dict_data VALUES ('1000', '000000', 1, '物资', 'goods', 'pms_purchase_type', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
INSERT INTO sys_dict_data VALUES ('1001', '000000', 2, '服务', 'service', 'pms_purchase_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
INSERT INTO sys_dict_data VALUES ('1002', '000000', 3, '固定资产', 'fixed_asset', 'pms_purchase_type', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- ----------------------------
-- 采购订单菜单
-- ----------------------------
insert into sys_menu values ('12050', '采购订单', '12000', '5', 'order', 'procurement/order/index', '', '1', '0', 'C', '0', '0', 'procurement:order:list', 'goods', 103, 1, sysdate(), NULL, NULL, '采购订单菜单');
insert into sys_menu values ('12051', '采购订单查询', '12050', '1', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:order:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12052', '采购订单新增', '12050', '2', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:order:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12053', '采购订单修改', '12050', '3', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:order:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12054', '采购订单删除', '12050', '4', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:order:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12055', '采购订单导出', '12050', '5', '#', '', '', '1', '0', 'F', '0', '0', 'procurement:order:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 采购订单数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS pms_purchase_order
(
    `id`            bigint          NOT NULL COMMENT '订单ID',
    `order_no`      varchar(64)     DEFAULT NULL COMMENT '订单编号',
    `title`         varchar(200)    NOT NULL COMMENT '订单标题',
    `request_id`    bigint          DEFAULT NULL COMMENT '关联采购申请ID',
    `project_id`    bigint          NOT NULL COMMENT '项目ID',
    `supplier_id`   bigint          DEFAULT NULL COMMENT '供应商ID',
    `amount`        decimal(18, 2)  DEFAULT '0.00' COMMENT '订单总金额',
    `status`        varchar(32)     DEFAULT 'draft' COMMENT '状态（draft草稿 ordered已下单 partial_received部分收货 received已收货 cancelled已取消）',
    `order_date`    date            DEFAULT NULL COMMENT '下单日期',
    `delivery_date` date            DEFAULT NULL COMMENT '预计到货日期',
    `remark`        varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`   bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`     bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`     bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime        DEFAULT NULL COMMENT '更新时间',
    `tenant_id`     varchar(40)     DEFAULT NULL COMMENT '租户ID',
    `del_flag`      bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_order_no` (`order_no`) USING BTREE,
    KEY `idx_request_id` (`request_id`) USING BTREE,
    KEY `idx_project_id` (`project_id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购订单';

CREATE TABLE IF NOT EXISTS pms_purchase_order_item
(
    `id`              bigint          NOT NULL COMMENT '明细ID',
    `order_id`        bigint          NOT NULL COMMENT '订单ID',
    `request_item_id` bigint          DEFAULT NULL COMMENT '关联采购申请明细ID',
    `item_name`       varchar(200)    NOT NULL COMMENT '品名',
    `spec`            varchar(200)    DEFAULT NULL COMMENT '规格型号',
    `brand`           varchar(100)    DEFAULT NULL COMMENT '品牌',
    `unit`            varchar(50)     DEFAULT NULL COMMENT '单位',
    `quantity`        decimal(18, 4)  DEFAULT '0.0000' COMMENT '数量',
    `unit_price`      decimal(18, 4)  DEFAULT '0.0000' COMMENT '单价',
    `amount`          decimal(18, 2)  DEFAULT '0.00' COMMENT '金额',
    `sort_no`         int(4)          DEFAULT '0' COMMENT '排序号',
    `remark`          varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`     bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`       bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`       bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime        DEFAULT NULL COMMENT '更新时间',
    `tenant_id`       varchar(40)     DEFAULT NULL COMMENT '租户ID',
    `del_flag`        bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购订单明细';
