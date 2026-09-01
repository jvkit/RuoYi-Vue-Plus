SET NAMES utf8mb4;
-- ============================================================
-- 采购 v5：发票信息表扩展字段（幂等，可重复执行）
-- 用于采购发票台账：记录验收上传的发票、关联订单/项目、
-- 重复检测、有效/无效标记、冲红标记、OCR 原始 JSON。
-- ============================================================

-- ---------- 1. 新增字段（不存在时才添加） ----------
SET @tbl = 'invoice_info';

-- 关联验收单 ID
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'acceptance_id';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN acceptance_id bigint NULL COMMENT "关联验收单ID"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 关联验收明细 ID
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'acceptance_item_id';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN acceptance_item_id bigint NULL COMMENT "关联验收明细ID"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 关联采购申请 ID
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'request_id';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN request_id bigint NULL COMMENT "关联采购申请ID"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 关联项目 ID
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'project_id';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN project_id bigint NULL COMMENT "关联项目ID"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 发票 PDF URL
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'pdf_url';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN pdf_url varchar(1000) NULL COMMENT "发票PDF文件URL"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 是否冲红 0否 1是
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'red_flag';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN red_flag tinyint(1) DEFAULT 0 COMMENT "是否冲红：0否 1是"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 是否有效 0无效 1有效
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'valid_flag';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN valid_flag tinyint(1) DEFAULT 1 COMMENT "是否有效：0无效 1有效"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 无效原因
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'invalid_reason';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN invalid_reason varchar(500) NULL COMMENT "无效原因"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- OCR 原始 JSON
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'ocr_json';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN ocr_json text NULL COMMENT "OCR识别原始JSON"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 发票PDF文件OSS ID
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl AND column_name = 'pdf_oss_id';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD COLUMN pdf_oss_id varchar(64) NULL COMMENT "发票PDF文件OSS ID"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 为发票代码+号码建立普通索引，加速重复检测 ----------
SELECT COUNT(*) INTO @cnt FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = @tbl AND index_name = 'idx_invoice_code_number';
SET @sql = IF(@cnt = 0, 'ALTER TABLE invoice_info ADD INDEX idx_invoice_code_number (invoice_code, invoice_number)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. 校验 ----------
SELECT column_name, data_type, column_comment FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = @tbl
  AND column_name IN ('acceptance_id', 'acceptance_item_id', 'request_id', 'project_id', 'pdf_url', 'red_flag', 'valid_flag', 'invalid_reason', 'ocr_json')
ORDER BY ordinal_position;
