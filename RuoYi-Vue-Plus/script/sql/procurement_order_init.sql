-- default-character-set=utf8mb4
-- ----------------------------
-- 采购订单模块初始化（增量）
-- ----------------------------

-- 采购订单菜单
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
    `create_by`       bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`       bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime        DEFAULT NULL COMMENT '更新时间',
    `del_flag`        bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购订单明细';
