SET NAMES utf8mb4;
-- ============================================================
-- 采购管理模块初始化脚本（RuoYi-Vue-Plus 6.0.0 / ruoyi-6x）
-- 由 5.6.2 迁移而来，适配 6.x：
--   1) 移除多租户：pms_* 表去掉 tenant_id 列
--   2) sys_menu / sys_dict 使用 6.x 雪花 BigInt id，去掉 tenant_id，补齐 active_menu/ext/create_dept
--   3) 菜单 create_by / create_dept 采用 6.x 雪花用户 id（admin）
-- 导入库：ry-vue-6x
-- 说明：Warm-Flow 工作流表在 6.x 仍保留 tenant_id，流程定义沿用 '000000'
-- ============================================================

-- ----------------------------
-- 1. 采购申请数据表（无租户）
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
    `purchase_type` varchar(32)    DEFAULT NULL COMMENT '采购种类(科研类/非科研类)',
    `category1`     varchar(100)   DEFAULT NULL COMMENT '一级分类',
    `category2`     varchar(100)   DEFAULT NULL COMMENT '二级分类',
    `project_belong` varchar(200)  DEFAULT NULL COMMENT '项目归属',
    `item_name`    varchar(200)    NOT NULL COMMENT '品名',
    `spec`         varchar(200)    DEFAULT NULL COMMENT '规格型号',
    `brand`        varchar(100)    DEFAULT NULL COMMENT '品牌',
    `unit`         varchar(50)     DEFAULT NULL COMMENT '单位',
    `quantity`     decimal(18, 4)  DEFAULT '0.0000' COMMENT '数量',
    `unit_price`   decimal(18, 4)  DEFAULT '0.0000' COMMENT '单价',
    `amount`       decimal(18, 2)  DEFAULT '0.00' COMMENT '金额',
    `bom_item_id`  bigint          DEFAULT NULL COMMENT '关联BOM条目ID',
    `link`         varchar(500)    DEFAULT NULL COMMENT '商品链接',
    `platform`     varchar(32)     DEFAULT NULL COMMENT '平台（识别自链接）',
    `supplier_id`  bigint          DEFAULT NULL COMMENT '供应商ID（明细级）',
    `sort_no`      int(4)          DEFAULT '0' COMMENT '排序号',
    `remark`       varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`  bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`    bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`    bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime        DEFAULT NULL COMMENT '更新时间',
    `del_flag`     bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_request_id` (`request_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购申请明细';

-- ----------------------------
-- 2. 采购订单数据表（无租户）
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
    `del_flag`        bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购订单明细';

-- ----------------------------
-- 3. 采购项目数据表（无租户）
-- ----------------------------
CREATE TABLE IF NOT EXISTS pms_project
(
    `id`           bigint          NOT NULL COMMENT '项目ID',
    `parent_id`    bigint          DEFAULT '0' COMMENT '上级项目ID（0=主项目，非0=二级项目）',
    `project_code` varchar(64)     NOT NULL COMMENT '项目编码',
    `project_name` varchar(200)    NOT NULL COMMENT '项目名称',
    `dept_id`      bigint          DEFAULT NULL COMMENT '归属部门ID',
    `leader`       varchar(100)    DEFAULT NULL COMMENT '项目负责人',
    `budget`       decimal(18, 2)  DEFAULT '0.00' COMMENT '项目预算',
    `start_date`   date            DEFAULT NULL COMMENT '开始日期',
    `end_date`     date            DEFAULT NULL COMMENT '结束日期',
    `status`       tinyint(1)      DEFAULT '1' COMMENT '状态（0停用 1正常）',
    `remark`       varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`  bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`    bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`    bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime        DEFAULT NULL COMMENT '更新时间',
    `del_flag`     bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT = '采购项目';

-- ----------------------------
-- 4. 供应商数据表（无租户）
-- ----------------------------
CREATE TABLE IF NOT EXISTS pms_supplier
(
    `id`            bigint          NOT NULL COMMENT '供应商ID',
    `supplier_code` varchar(64)     NOT NULL COMMENT '供应商编码',
    `supplier_name` varchar(200)    NOT NULL COMMENT '供应商名称',
    `platform`      varchar(32)     DEFAULT NULL COMMENT '平台（淘宝/天猫/京东/拼多多/1688/抖音/其他）',
    `link`          varchar(500)    DEFAULT NULL COMMENT '店铺/商品链接',
    `contact_name`  varchar(100)    DEFAULT NULL COMMENT '联系人',
    `contact_phone` varchar(50)     DEFAULT NULL COMMENT '联系电话',
    `address`       varchar(500)    DEFAULT NULL COMMENT '地址',
    `bank_name`     varchar(200)    DEFAULT NULL COMMENT '开户行',
    `bank_account`  varchar(100)    DEFAULT NULL COMMENT '银行账号',
    `status`        tinyint(1)      DEFAULT '1' COMMENT '状态（0停用 1正常）',
    `remark`        varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept`   bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`     bigint          DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`     bigint          DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime        DEFAULT NULL COMMENT '更新时间',
    `del_flag`      bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT = '供应商';

-- ----------------------------
-- 5. BOM 物料清单数据表（无租户）
-- ----------------------------
CREATE TABLE IF NOT EXISTS pms_bom_item
(
    `id`          bigint          NOT NULL COMMENT 'BOM条目ID',
    `project_id`  bigint          NOT NULL COMMENT '项目ID',
    `category`    varchar(100)    DEFAULT NULL COMMENT '分类',
    `name`        varchar(200)    NOT NULL COMMENT '品名',
    `spec`        varchar(500)    DEFAULT NULL COMMENT '规格型号',
    `brand`       varchar(200)    DEFAULT NULL COMMENT '品牌',
    `qty`         decimal(18, 2)  DEFAULT '0.00' COMMENT '数量',
    `unit`        varchar(50)     DEFAULT NULL COMMENT '单位',
    `est_price`   decimal(18, 2)  DEFAULT '0.00' COMMENT '预估单价',
    `est_total`   decimal(18, 2)  DEFAULT '0.00' COMMENT '预估总价',
    `supplier_id` bigint          DEFAULT NULL COMMENT '关联供应商ID',
    `status`      tinyint(1)      DEFAULT '0' COMMENT '状态（0待采购 1已下单 2已到货）',
    `remark`      varchar(500)    DEFAULT NULL COMMENT '备注',
    `create_dept` bigint          DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint          DEFAULT NULL COMMENT '创建者',
    `create_time` datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`   bigint          DEFAULT NULL COMMENT '更新者',
    `update_time` datetime        DEFAULT NULL COMMENT '更新时间',
    `del_flag`    bigint          DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_project_id` (`project_id`) USING BTREE
) ENGINE = InnoDB COMMENT = 'BOM物料清单';

-- ============================================================
-- 6. 采购模块菜单（6.x 雪花 id，无租户）
-- 菜单 id 段 1801xxx；create_by 用 admin 雪花用户 id
-- ============================================================
SET @admin_id := 1761100000000000001;

-- 采购管理目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801000, '采购管理', 0, 8, 'procurement', NULL, NULL, 'N', 'Y', 'M', '0', '0', NULL, 'shopping-cart', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '采购管理目录');

-- 采购项目
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801010, '采购项目', 1801000, 1, 'project', 'procurement/project/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:project:list', 'office-building', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '采购项目菜单');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801011, '采购项目查询', 1801010, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:project:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801012, '采购项目新增', 1801010, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:project:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801013, '采购项目修改', 1801010, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:project:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801014, '采购项目删除', 1801010, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:project:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801015, '采购项目导出', 1801010, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:project:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 供应商管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801020, '供应商管理', 1801000, 2, 'supplier', 'procurement/supplier/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:supplier:list', 'user', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '供应商管理菜单');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801021, '供应商查询', 1801020, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:supplier:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801022, '供应商新增', 1801020, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:supplier:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801023, '供应商修改', 1801020, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:supplier:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801024, '供应商删除', 1801020, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:supplier:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801025, '供应商导出', 1801020, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:supplier:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- BOM 物料清单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801030, 'BOM物料清单', 1801000, 3, 'bom', 'procurement/bom/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:bom:list', 'grid', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, 'BOM物料清单菜单');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801031, 'BOM查询', 1801030, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bom:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801032, 'BOM新增', 1801030, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bom:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801033, 'BOM修改', 1801030, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bom:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801034, 'BOM删除', 1801030, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bom:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801035, 'BOM导出', 1801030, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:bom:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 采购申请
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801040, '采购申请', 1801000, 4, 'request', 'procurement/request/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:request:list', 'edit', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '采购申请菜单');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801041, '采购申请查询', 1801040, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:request:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801042, '采购申请新增', 1801040, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:request:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801043, '采购申请修改', 1801040, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:request:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801044, '采购申请删除', 1801040, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:request:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801045, '采购申请导出', 1801040, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:request:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801046, '采购申请提交', 1801040, 6, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:request:submit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- 采购订单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801050, '采购订单', 1801000, 5, 'order', 'procurement/order/index', NULL, 'N', 'Y', 'C', '0', '0', 'procurement:order:list', 'goods', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '采购订单菜单');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801051, '采购订单查询', 1801050, 1, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:order:query', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801052, '采购订单新增', 1801050, 2, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:order:add', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801053, '采购订单修改', 1801050, 3, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:order:edit', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801054, '采购订单删除', 1801050, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:order:remove', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801055, '采购订单导出', 1801050, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:order:export', '#', NULL, NULL, NULL, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 7. 采购类型字典（6.x 雪花 id，无租户）
-- ============================================================
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1802001, '采购类型', 'pms_purchase_type', NULL, @admin_id, sysdate(), NULL, NULL, '采购类型');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1803001, 1, '物资', 'goods', 'pms_purchase_type', '', 'primary', 'N', NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1803002, 2, '服务', 'service', 'pms_purchase_type', '', 'success', 'N', NULL, @admin_id, sysdate(), NULL, NULL, '');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1803003, 3, '固定资产', 'fixed_asset', 'pms_purchase_type', '', 'warning', 'N', NULL, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 8. 采购申请流程定义（Warm-Flow，6.x 流程表仍带 tenant_id，沿用 '000000'）
-- ============================================================
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
