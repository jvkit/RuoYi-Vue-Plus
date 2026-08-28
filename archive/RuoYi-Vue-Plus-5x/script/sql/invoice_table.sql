SET NAMES utf8mb4;

-- 发票信息表
CREATE TABLE IF NOT EXISTS `invoice_info` (
  `id` bigint NOT NULL COMMENT '主键id',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
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
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票信息表';
