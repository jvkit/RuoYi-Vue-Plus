SET NAMES utf8mb4;
-- ============================================================
-- 采购系统 v2 数据地基脚本（ruoyi-6x / ry-vue-6x）
-- 依据：docs/采购系统详细页面规划.md (v2) 附录一 + 各页面字段表
-- 内容：改造 2 表 + 新增 11 表（附件/验收/仓库/领用/BOM三表/流转/合同/报销）
-- 幂等：ALTER 用 information_schema 判断；CREATE TABLE IF NOT EXISTS。
-- 说明：本脚本仅建表，不建菜单/流程（菜单在 procurement_6x_v2.sql，流程定义后端阶段补齐）。
-- 导入库：ry-vue-6x
-- ============================================================

-- ============================================================
-- 1. 改造已有表（幂等加列）
-- ============================================================
-- 1.1 pms_project + used_amount(已用金额)
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_project' AND COLUMN_NAME = 'used_amount');
SET @sql := IF(@col = 0,
    'ALTER TABLE pms_project ADD COLUMN used_amount decimal(18,2) DEFAULT 0.00 COMMENT ''已用金额''',
    'SELECT ''pms_project.used_amount 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 pms_procurement_request + 对接人/付款截图/报价单/开票信息
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'procurement_contact');
SET @sql := IF(@col = 0,
    'ALTER TABLE pms_procurement_request ADD COLUMN procurement_contact varchar(64) DEFAULT NULL COMMENT ''采购对接人''',
    'SELECT ''procurement_contact 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'payment_screenshot');
SET @sql := IF(@col = 0,
    'ALTER TABLE pms_procurement_request ADD COLUMN payment_screenshot varchar(500) DEFAULT NULL COMMENT ''付款截图(自购必填)''',
    'SELECT ''payment_screenshot 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'quotation_url');
SET @sql := IF(@col = 0,
    'ALTER TABLE pms_procurement_request ADD COLUMN quotation_url varchar(500) DEFAULT NULL COMMENT ''报价单(对公必填)''',
    'SELECT ''quotation_url 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'invoice_info_json');
SET @sql := IF(@col = 0,
    'ALTER TABLE pms_procurement_request ADD COLUMN invoice_info_json text DEFAULT NULL COMMENT ''开票信息JSON(对公必填)''',
    'SELECT ''invoice_info_json 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 新增表
-- ============================================================

-- 2.1 通用附件表（统一挂载验收/合同/报销等附件）
CREATE TABLE IF NOT EXISTS pms_attachment
(
    `id`          bigint       NOT NULL COMMENT '附件ID',
    `biz_type`    varchar(30)  NOT NULL COMMENT '业务类型(acceptance/contract/reimbursement/request)',
    `biz_id`      bigint       NOT NULL COMMENT '业务ID',
    `file_name`   varchar(200) DEFAULT NULL COMMENT '文件名',
    `file_url`    varchar(500) DEFAULT NULL COMMENT '文件地址',
    `file_size`   bigint       DEFAULT NULL COMMENT '文件大小(字节)',
    `create_dept` bigint       DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint       DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `del_flag`    bigint       DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE = InnoDB COMMENT = '采购通用附件';

-- 2.2 验收单
CREATE TABLE IF NOT EXISTS pms_acceptance
(
    `id`              bigint       NOT NULL COMMENT '验收ID',
    `acceptance_code` varchar(64)  DEFAULT NULL COMMENT '验收编号',
    `request_id`      bigint       DEFAULT NULL COMMENT '关联采购申请ID',
    `project_id`      bigint       DEFAULT NULL COMMENT '项目ID',
    `operator`        bigint       DEFAULT NULL COMMENT '验收操作人',
    `acceptance_date` date         DEFAULT NULL COMMENT '验收日期',
    `status`          varchar(20)  DEFAULT 'pending' COMMENT '状态(pending待验收/partial部分验收/finished已完成/rejected不合格)',
    `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
    `create_dept`     bigint       DEFAULT NULL,
    `create_by`       bigint       DEFAULT NULL,
    `create_time`     datetime     DEFAULT NULL,
    `update_by`       bigint       DEFAULT NULL,
    `update_time`     datetime     DEFAULT NULL,
    `del_flag`        bigint       DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_request` (`request_id`)
) ENGINE = InnoDB COMMENT = '采购验收单';

-- 2.3 验收明细
CREATE TABLE IF NOT EXISTS pms_acceptance_item
(
    `id`              bigint        NOT NULL COMMENT '验收明细ID',
    `acceptance_id`   bigint        NOT NULL COMMENT '验收单ID',
    `request_item_id` bigint        DEFAULT NULL COMMENT '关联采购明细ID',
    `item_name`       varchar(200)  DEFAULT NULL COMMENT '品名',
    `spec`            varchar(200)  DEFAULT NULL COMMENT '规格型号',
    `apply_price`     decimal(18,2) DEFAULT '0.00' COMMENT '申请单价',
    `invoice_price`   decimal(18,2) DEFAULT '0.00' COMMENT '发票金额',
    `price_check`     varchar(20)   DEFAULT NULL COMMENT '金额核对(pass发票≤申请/over发票>申请冲红)',
    `photo_url`       varchar(1000) DEFAULT NULL COMMENT '实物图片(必填)',
    `invoice_url`     varchar(1000) DEFAULT NULL COMMENT '发票附件(必填,PDF/图片)',
    `ai_opinion`      text          COMMENT 'AI审核意见(二阶段)',
    `result`          varchar(20)   DEFAULT NULL COMMENT '验收结果(pass通过/over冲红)',
    `remark`          varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_dept`     bigint        DEFAULT NULL,
    `create_by`       bigint        DEFAULT NULL,
    `create_time`     datetime      DEFAULT NULL,
    `update_by`       bigint        DEFAULT NULL,
    `update_time`     datetime      DEFAULT NULL,
    `del_flag`        bigint        DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_acceptance` (`acceptance_id`)
) ENGINE = InnoDB COMMENT = '采购验收明细';

-- 2.4 仓库库存
CREATE TABLE IF NOT EXISTS pms_warehouse_stock
(
    `id`             bigint        NOT NULL COMMENT '库存ID',
    `item_name`      varchar(200)  NOT NULL COMMENT '品名',
    `spec`           varchar(200)  DEFAULT NULL COMMENT '规格型号',
    `brand`          varchar(100)  DEFAULT NULL COMMENT '品牌',
    `unit`           varchar(20)   DEFAULT NULL COMMENT '单位',
    `qty_available`  decimal(18,2) DEFAULT '0.00' COMMENT '可用数量',
    `source_item_id` bigint        DEFAULT NULL COMMENT '关联采购明细ID(溯源/复购)',
    `project_id`     bigint        DEFAULT NULL COMMENT '关联项目',
    `inbound_date`   date          DEFAULT NULL COMMENT '入库日期',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_dept`    bigint        DEFAULT NULL,
    `create_by`      bigint        DEFAULT NULL,
    `create_time`    datetime      DEFAULT NULL,
    `update_by`      bigint        DEFAULT NULL,
    `update_time`    datetime      DEFAULT NULL,
    `del_flag`       bigint        DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_item` (`item_name`)
) ENGINE = InnoDB COMMENT = '仓库库存';

-- 2.5 出入库流水（只增不删）
CREATE TABLE IF NOT EXISTS pms_stock_movement
(
    `id`            bigint        NOT NULL COMMENT '流水ID',
    `stock_id`      bigint        NOT NULL COMMENT '库存ID',
    `movement_type` varchar(10)   DEFAULT NULL COMMENT '类型(in入库/out出库)',
    `qty`           decimal(18,2) DEFAULT '0.00' COMMENT '数量',
    `relate_id`     bigint        DEFAULT NULL COMMENT '来源单据ID',
    `relate_type`   varchar(20)   DEFAULT NULL COMMENT '来源类型(acceptance/issue/manual)',
    `operator`      bigint        DEFAULT NULL COMMENT '操作人',
    `operate_time`  datetime      DEFAULT NULL COMMENT '操作时间',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_stock` (`stock_id`)
) ENGINE = InnoDB COMMENT = '出入库流水';

-- 2.6 领用申请
CREATE TABLE IF NOT EXISTS pms_issue_request
(
    `id`            bigint        NOT NULL COMMENT '领用ID',
    `issue_code`    varchar(64)   DEFAULT NULL COMMENT '领用编号',
    `stock_id`      bigint        DEFAULT NULL COMMENT '库存ID',
    `item_name`     varchar(200)  DEFAULT NULL COMMENT '品名',
    `spec`          varchar(200)  DEFAULT NULL COMMENT '规格',
    `qty_available` decimal(18,2) DEFAULT '0.00' COMMENT '可用数量',
    `qty_requested` decimal(18,2) DEFAULT '0.00' COMMENT '领用数量',
    `purpose`       varchar(500)  DEFAULT NULL COMMENT '用途',
    `applicant`     bigint        DEFAULT NULL COMMENT '申请人',
    `approver`      bigint        DEFAULT NULL COMMENT '审批人',
    `status`        varchar(20)   DEFAULT 'pending' COMMENT '状态(pending待审/approved已通过/rejected已拒绝/issued已出库)',
    `approve_time`  datetime      DEFAULT NULL COMMENT '审批时间',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_dept`   bigint        DEFAULT NULL,
    `create_by`     bigint        DEFAULT NULL,
    `create_time`   datetime      DEFAULT NULL,
    `update_by`     bigint        DEFAULT NULL,
    `update_time`   datetime      DEFAULT NULL,
    `del_flag`      bigint        DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_stock` (`stock_id`)
) ENGINE = InnoDB COMMENT = '领用申请';

-- 2.7 BOM物料库（物料档案库，≠仓库）
CREATE TABLE IF NOT EXISTS pms_bom_catalog
(
    `id`          bigint        NOT NULL COMMENT '物料ID',
    `item_name`   varchar(200)  NOT NULL COMMENT '品名',
    `spec`        varchar(200)  DEFAULT NULL COMMENT '规格型号',
    `brand`       varchar(100)  DEFAULT NULL COMMENT '品牌',
    `unit`        varchar(20)   DEFAULT NULL COMMENT '单位',
    `ref_price`   decimal(18,2) DEFAULT '0.00' COMMENT '参考单价',
    `category`    varchar(50)   DEFAULT NULL COMMENT '分类(材料/设备/其他)',
    `link`        varchar(500)  DEFAULT NULL COMMENT '产品链接',
    `project_id`  bigint        DEFAULT NULL COMMENT '关联项目(可空)',
    `stock_id`    bigint        DEFAULT NULL COMMENT '关联仓库库存(可空)',
    `status`      tinyint(1)    DEFAULT '1' COMMENT '状态(0停用 1正常)',
    `remark`      varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_dept` bigint        DEFAULT NULL,
    `create_by`   bigint        DEFAULT NULL,
    `create_time` datetime      DEFAULT NULL,
    `update_by`   bigint        DEFAULT NULL,
    `update_time` datetime      DEFAULT NULL,
    `del_flag`    bigint        DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = 'BOM物料库';

-- 2.8 BOM表（产品）
CREATE TABLE IF NOT EXISTS pms_bom_table
(
    `id`          bigint       NOT NULL COMMENT 'BOM表ID',
    `name`        varchar(200) NOT NULL COMMENT '产品名称',
    `spec`        varchar(200) DEFAULT NULL COMMENT '规格型号',
    `project_id`  bigint       DEFAULT NULL COMMENT '关联项目(可空)',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '状态(0停用 1正常)',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_dept` bigint       DEFAULT NULL,
    `create_by`   bigint       DEFAULT NULL,
    `create_time` datetime     DEFAULT NULL,
    `update_by`   bigint       DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `del_flag`    bigint       DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = 'BOM表(产品)';

-- 2.9 BOM节点（不限层级树：group分组/item商品/product引用产品）
CREATE TABLE IF NOT EXISTS pms_bom_node
(
    `id`               bigint        NOT NULL COMMENT '节点ID',
    `bom_table_id`     bigint        NOT NULL COMMENT 'BOM表ID',
    `parent_id`        bigint        DEFAULT '0' COMMENT '上级节点ID(0=顶层)',
    `node_type`        varchar(20)   DEFAULT NULL COMMENT '节点类型(group/item/product)',
    `group_name`       varchar(200)  DEFAULT NULL COMMENT '分组名称(group时)',
    `catalog_id`       bigint        DEFAULT NULL COMMENT '关联BOM库物料(item时)',
    `item_name`        varchar(200)  DEFAULT NULL COMMENT '品名(带出,只读)',
    `spec`             varchar(200)  DEFAULT NULL COMMENT '规格(带出)',
    `brand`            varchar(100)  DEFAULT NULL COMMENT '品牌(带出)',
    `unit`             varchar(20)   DEFAULT NULL COMMENT '单位(带出)',
    `qty_per_unit`     decimal(18,2) DEFAULT '0.00' COMMENT '单套用量',
    `ref_price`        decimal(18,2) DEFAULT '0.00' COMMENT '参考单价(带出)',
    `stock_qty`        decimal(18,2) DEFAULT '0.00' COMMENT '仓库库存(实时)',
    `ref_bom_table_id` bigint        DEFAULT NULL COMMENT '引用BOM表ID(product时)',
    `sort_no`          int           DEFAULT '0' COMMENT '排序号',
    `remark`           varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_dept`      bigint        DEFAULT NULL,
    `create_by`        bigint        DEFAULT NULL,
    `create_time`      datetime      DEFAULT NULL,
    `update_by`        bigint        DEFAULT NULL,
    `update_time`      datetime      DEFAULT NULL,
    `del_flag`         bigint        DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_table` (`bom_table_id`)
) ENGINE = InnoDB COMMENT = 'BOM节点';

-- 2.10 流转记录（系统自动写）
CREATE TABLE IF NOT EXISTS pms_operation_log
(
    `id`            bigint       NOT NULL COMMENT '日志ID',
    `biz_type`      varchar(30)  DEFAULT NULL COMMENT '业务类型(request/acceptance/issue/stock)',
    `biz_id`        bigint       DEFAULT NULL COMMENT '业务ID',
    `action`        varchar(50)  DEFAULT NULL COMMENT '操作动作',
    `operator`      bigint       DEFAULT NULL COMMENT '操作人',
    `operator_name` varchar(64)  DEFAULT NULL COMMENT '操作人姓名',
    `operate_time`  datetime     DEFAULT NULL COMMENT '操作时间',
    `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
    `from_status`   varchar(20)  DEFAULT NULL COMMENT '操作前状态',
    `to_status`     varchar(20)  DEFAULT NULL COMMENT '操作后状态',
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE = InnoDB COMMENT = '流转记录';

-- 2.11 采购合同
CREATE TABLE IF NOT EXISTS pms_purchase_contract
(
    `id`                bigint        NOT NULL COMMENT '合同ID',
    `contract_no`       varchar(64)   DEFAULT NULL COMMENT '合同编号',
    `request_id`        bigint        DEFAULT NULL COMMENT '关联采购申请',
    `title`             varchar(200)  DEFAULT NULL COMMENT '合同标题',
    `invoice_info_json` text          COMMENT '开票信息',
    `items_json`        text          COMMENT '物料明细',
    `amount`            decimal(18,2) DEFAULT '0.00' COMMENT '合同总金额',
    `quotation_url`     varchar(500)  DEFAULT NULL COMMENT '报价单附件',
    `content`           text          COMMENT '合同内容(模板渲染)',
    `file_url`          varchar(500)  DEFAULT NULL COMMENT '合同文件',
    `status`            varchar(20)   DEFAULT 'draft' COMMENT '状态(draft/generated/sent)',
    `generate_time`     datetime      DEFAULT NULL COMMENT '生成时间',
    `create_dept`       bigint        DEFAULT NULL,
    `create_by`         bigint        DEFAULT NULL,
    `create_time`       datetime      DEFAULT NULL,
    `update_by`         bigint        DEFAULT NULL,
    `update_time`       datetime      DEFAULT NULL,
    `del_flag`          bigint        DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '采购合同';

-- 2.12 报销
CREATE TABLE IF NOT EXISTS pms_reimbursement
(
    `id`                 bigint       NOT NULL COMMENT '报销ID',
    `reimbursement_code` varchar(64)  DEFAULT NULL COMMENT '报销编号',
    `request_id`         bigint       DEFAULT NULL COMMENT '关联采购申请',
    `acceptance_id`      bigint       DEFAULT NULL COMMENT '关联验收单',
    `project_id`         bigint       DEFAULT NULL COMMENT '项目',
    `applicant`          varchar(64)  DEFAULT NULL COMMENT '申请人',
    `file_url`           varchar(500) DEFAULT NULL COMMENT '打包文件',
    `content_json`       text         COMMENT '包含内容',
    `status`             varchar(20)  DEFAULT 'packing' COMMENT '状态(packing/packed/sent)',
    `create_dept`        bigint       DEFAULT NULL,
    `create_by`          bigint       DEFAULT NULL,
    `create_time`        datetime     DEFAULT NULL,
    `update_by`          bigint       DEFAULT NULL,
    `update_time`        datetime     DEFAULT NULL,
    `del_flag`           bigint       DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '报销导出';

-- ============================================================
-- 3. 补列（修正：附件/流转记录漏了 BaseEntity 字段或 del_flag，实体按模板要求这些列）
-- ============================================================
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_attachment' AND COLUMN_NAME='update_by');
SET @sql := IF(@col=0,'ALTER TABLE pms_attachment ADD COLUMN update_by bigint DEFAULT NULL COMMENT ''更新者''','SELECT ''attachment.update_by 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_attachment' AND COLUMN_NAME='update_time');
SET @sql := IF(@col=0,'ALTER TABLE pms_attachment ADD COLUMN update_time datetime DEFAULT NULL COMMENT ''更新时间''','SELECT ''attachment.update_time 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_operation_log' AND COLUMN_NAME='del_flag');
SET @sql := IF(@col=0,'ALTER TABLE pms_operation_log ADD COLUMN del_flag bigint DEFAULT 0 COMMENT ''删除标志''','SELECT ''log.del_flag 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_operation_log' AND COLUMN_NAME='create_dept');
SET @sql := IF(@col=0,'ALTER TABLE pms_operation_log ADD COLUMN create_dept bigint DEFAULT NULL COMMENT ''创建部门''','SELECT ''log.create_dept 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_operation_log' AND COLUMN_NAME='create_by');
SET @sql := IF(@col=0,'ALTER TABLE pms_operation_log ADD COLUMN create_by bigint DEFAULT NULL COMMENT ''创建者''','SELECT ''log.create_by 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_operation_log' AND COLUMN_NAME='create_time');
SET @sql := IF(@col=0,'ALTER TABLE pms_operation_log ADD COLUMN create_time datetime DEFAULT NULL COMMENT ''创建时间''','SELECT ''log.create_time 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_operation_log' AND COLUMN_NAME='update_by');
SET @sql := IF(@col=0,'ALTER TABLE pms_operation_log ADD COLUMN update_by bigint DEFAULT NULL COMMENT ''更新者''','SELECT ''log.update_by 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_operation_log' AND COLUMN_NAME='update_time');
SET @sql := IF(@col=0,'ALTER TABLE pms_operation_log ADD COLUMN update_time datetime DEFAULT NULL COMMENT ''更新时间''','SELECT ''log.update_time 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 4. 类型对齐：operator/applicant/approver 前端+实体为 String，列改 varchar（后续优化为用户ID+自动带出）
-- ============================================================
ALTER TABLE pms_acceptance MODIFY COLUMN operator varchar(64) DEFAULT NULL COMMENT '验收操作人';
ALTER TABLE pms_issue_request MODIFY COLUMN applicant varchar(64) DEFAULT NULL COMMENT '申请人';
ALTER TABLE pms_issue_request MODIFY COLUMN approver varchar(64) DEFAULT NULL COMMENT '审批人';

-- ============================================================
-- 5. 审批流数据地基：项目加 leader_id(用户ID) + 双节点流程定义(负责人→对接人)
-- ============================================================
-- 5.1 pms_project 加 leader_id（审批流按负责人用户ID分派；leader 保留姓名用于展示）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pms_project' AND COLUMN_NAME='leader_id');
SET @sql := IF(@col=0,'ALTER TABLE pms_project ADD COLUMN leader_id bigint DEFAULT NULL COMMENT ''项目负责人用户ID''','SELECT ''leader_id 已存在'' AS msg'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 5.2 双节点流程：start → leader(负责人,${leaderId}) → contact(对接人,${contactId}) → end
DELETE FROM flow_skip WHERE definition_id = 900000000000000001;
DELETE FROM flow_node WHERE definition_id = 900000000000000001;
INSERT INTO flow_node (id, node_type, definition_id, node_code, node_name, permission_flag, node_ratio, coordinate, any_node_skip, listener_type, listener_path, form_custom, form_path, version, create_time, create_by, update_time, update_by, ext, del_flag, tenant_id) VALUES
(900000000000000014, 0, 900000000000000001, 'start', '开始', NULL, NULL, '80,120|80,120', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000'),
(900000000000000015, 1, 900000000000000001, 'leader', '项目负责人审批', '${leaderId}', '100', '300,120|300,120', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000'),
(900000000000000016, 1, 900000000000000001, 'contact', '采购对接人审批', '${contactId}', '100', '520,120|520,120', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000'),
(900000000000000017, 2, 900000000000000001, 'end', '结束', NULL, NULL, '740,120|740,120', NULL, NULL, NULL, 'N', NULL, '1.0', sysdate(), '1', sysdate(), '1', NULL, '0', '000000');
INSERT INTO flow_skip (id, definition_id, now_node_code, now_node_type, next_node_code, next_node_type, skip_name, skip_type, skip_condition, coordinate, create_time, create_by, update_time, update_by, del_flag, tenant_id) VALUES
(900000000000000024, 900000000000000001, 'start', 0, 'leader', 1, '提交', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000'),
(900000000000000025, 900000000000000001, 'leader', 1, 'contact', 1, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000'),
(900000000000000026, 900000000000000001, 'leader', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000'),
(900000000000000027, 900000000000000001, 'contact', 1, 'end', 2, '通过', 'PASS', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000'),
(900000000000000028, 900000000000000001, 'contact', 1, 'start', 0, '退回', 'REJECT', NULL, NULL, sysdate(), '1', sysdate(), '1', '0', '000000');
