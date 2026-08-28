SET NAMES utf8mb4;
-- ============================================================
-- 资金管理：资金流水表 + 菜单（幂等，可重复执行）
--   触发：采购申请(pms_procurement_request)状态变为 finish(审批通过)时，
--         由 PmsProcurementRequestServiceImpl.processHandler 写入一条 out 流水。
--   前端：plus-ui-6x/src/views/procurement/fund/index.vue（资金管理面板）
--   汇总：项目剩余资金 = pms_project.budget - used_amount（实时维护）
--         本月流出 = SUM(pms_fund_flow.amount WHERE occur_date 当月)
-- ============================================================

-- ---------- 1. 建表 ----------
CREATE TABLE IF NOT EXISTS `pms_fund_flow` (
  `id` bigint NOT NULL COMMENT '主键',
  `flow_no` varchar(64) NOT NULL COMMENT '流水编号 FUND-yyyyMMdd-NNN',
  `flow_type` varchar(10) NOT NULL DEFAULT 'out' COMMENT '类型(out=流出 in=流入)',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `project_name` varchar(200) DEFAULT NULL COMMENT '项目名快照',
  `request_id` bigint DEFAULT NULL COMMENT '采购申请ID',
  `request_code` varchar(64) DEFAULT NULL COMMENT '申请编号快照',
  `request_title` varchar(500) DEFAULT NULL COMMENT '申请标题快照',
  `amount` decimal(18,2) DEFAULT '0.00' COMMENT '金额(正数)',
  `occur_date` date DEFAULT NULL COMMENT '发生日期(审批通过日)',
  `operator_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '审批人姓名',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_occur_date` (`occur_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';

-- ---------- 2. 反写历史流水（已审批通过的申请，未写过的补写，幂等） ----------
INSERT INTO pms_fund_flow (`id`, `flow_no`, `flow_type`, `project_id`, `project_name`, `request_id`, `request_code`, `request_title`, `amount`, `occur_date`, `operator_id`, `operator_name`, `remark`, `create_by`, `create_time`, `del_flag`)
SELECT
  r.id + 1000000000000000000 AS id,              -- 用申请id派生流水id，避免冲突
  CONCAT('FUND-', DATE_FORMAT(r.update_time, '%Y%m%d'), '-', LPAD(@seq := @seq + 1, 3, '0')) AS flow_no,
  'out',
  r.project_id,
  p.project_name,
  r.id,
  r.request_code,
  r.title,
  r.amount,
  DATE(r.update_time),
  r.update_by,
  NULL,
  '历史数据反写(审批通过)',
  r.update_by,
  r.update_time,
  0
FROM (SELECT @seq := 0) s,
     pms_procurement_request r
LEFT JOIN pms_project p ON r.project_id = p.id AND p.del_flag = '0'
WHERE r.status = 'finish'
  AND r.del_flag = '0'
  AND NOT EXISTS (SELECT 1 FROM pms_fund_flow f WHERE f.request_id = r.id AND f.flow_type = 'out' AND f.del_flag = '0');

-- ---------- 3. 菜单：资金管理 ----------
SET @dept_id = 1761000000000000103;
SET @admin_id = 1761100000000000001;

-- 菜单（先删后插，幂等）
DELETE FROM sys_menu WHERE menu_id = 1801140;
DELETE FROM sys_menu WHERE menu_id IN (1801141, 1801142);

INSERT INTO sys_menu (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `active_menu`, `ext`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1801140, '资金管理', 1801000, 14, 'fund', 'procurement/fund/index', NULL, '1', '0', 'C', '0', '0', 'procurement:fund:list', 'money', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '采购资金管理面板（视图+流水明细）');

INSERT INTO sys_menu (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `active_menu`, `ext`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1801141, '资金查询', 1801140, 1, '#', NULL, NULL, '1', '0', 'F', '0', '0', 'procurement:fund:query', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `active_menu`, `ext`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1801142, '资金导出', 1801140, 2, '#', NULL, NULL, '1', '0', 'F', '0', '0', 'procurement:fund:export', '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

-- 给采购相关角色补资金菜单权限（可选，需要的话取消注释）
-- INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT role_id, 1801140 FROM sys_role WHERE del_flag = '0' AND role_key IN ('admin', 'team_leader', 'dept_leader', 'acceptance_contact', 'warehouse_admin');
-- INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT role_id, 1801141 FROM sys_role WHERE del_flag = '0' AND role_key IN ('admin', 'team_leader', 'dept_leader');
-- INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT role_id, 1801142 FROM sys_role WHERE del_flag = '0' AND role_key IN ('admin', 'team_leader', 'dept_leader');

-- 校验
SELECT menu_id, menu_name, parent_id, menu_type, path, component, perms FROM sys_menu WHERE menu_id BETWEEN 1801140 AND 1801142 ORDER BY menu_id;
SELECT COUNT(*) AS fund_flow_count FROM pms_fund_flow WHERE del_flag = '0';
