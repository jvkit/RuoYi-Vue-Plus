SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：采购申请「验收标志」列（幂等，可重复执行）
--   目的：一张采购申请只能验收一次。在 pms_procurement_request 上
--         增加 acceptance_status 列：none未验收 / processing验收中 / done已完成验收。
--         - 新建验收单时置 processing（插入 PmsAcceptance 时）
--         - 验收流程 finish 时置 done
--         - 删除唯一验收单时恢复 none
--         - 采购验收页「关联采购申请」下拉只列 acceptance_status 为 none/null 的申请
--   存量数据回填：已有验收单的申请置 processing，其中验收已完成(finished)的置 done。
-- 导入库：ry-vue-6x
-- ============================================================

-- ---------- 1. 加列（幂等：information_schema 判断） ----------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'acceptance_status');
SET @ddl = IF(@col_exists = 0,
              'ALTER TABLE pms_procurement_request ADD COLUMN acceptance_status VARCHAR(20) DEFAULT NULL COMMENT ''验收标志(none未验收/processing验收中/done已完成验收)'' AFTER status',
              'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 存量数据回填 ----------
-- 2.1 已有验收单（未删除）的申请 → processing
UPDATE pms_procurement_request r
JOIN (SELECT DISTINCT request_id FROM pms_acceptance WHERE del_flag = '0') a ON a.request_id = r.id
SET r.acceptance_status = 'processing'
WHERE r.del_flag = '0';

-- 2.2 其中验收已完成(finished)的申请 → done
UPDATE pms_procurement_request r
JOIN (SELECT DISTINCT request_id FROM pms_acceptance WHERE del_flag = '0' AND status = 'finished') a ON a.request_id = r.id
SET r.acceptance_status = 'done'
WHERE r.del_flag = '0';

-- ---------- 3. 校验 ----------
SELECT column_name, column_type, column_comment FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'acceptance_status';
SELECT acceptance_status, COUNT(*) cnt FROM pms_procurement_request WHERE del_flag = '0' GROUP BY acceptance_status;
