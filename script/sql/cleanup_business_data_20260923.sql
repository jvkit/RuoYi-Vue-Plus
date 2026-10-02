-- 2026-09-23 本地真测前业务数据清理（幂等：DELETE 无行可删即天然幂等）
-- 前置备份：backups/ry-vue-6x-20260923.sql.gz
-- 只清业务运行数据，保留流程定义(flow_definition/node/skip/category/spel)、
-- 项目树(pms_project)、备用金账户(pms_reserve_account)、供应商/物料主数据、全部 sys_*
SET NAMES utf8mb4;

DELETE FROM pms_procurement_request_item;
DELETE FROM pms_procurement_request;
DELETE FROM pms_acceptance_item;
DELETE FROM pms_acceptance;
DELETE FROM pms_issue_request;
DELETE FROM pms_reimbursement;
DELETE FROM pms_fund_flow;
DELETE FROM pms_stock_movement;
DELETE FROM pms_warehouse_stock;
DELETE FROM invoice_info;
-- warm-flow 运行态（定义表不动）
DELETE FROM flow_instance_biz_ext;
DELETE FROM flow_user;
DELETE FROM flow_task;
DELETE FROM flow_his_task;
DELETE FROM flow_instance;
