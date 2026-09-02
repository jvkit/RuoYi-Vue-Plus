SET NAMES utf8mb4;
-- ============================================================
-- 采购 v11：发票号允许为空（幂等，可重复执行）
-- 背景：AI OCR 偶尔识别不出发票号（invoice_number 返回 null），
-- 而 invoice_info.invoice_number 为 NOT NULL 且无默认值，
-- 导致 /procurement/acceptance/ai-invoice-match 整体 500。
-- 修复：放宽为 NULL，识别不出发票号的发票照常入台账留痕，
-- 仅跳过重复检测（findValidByCodeAndNumber 对空号返回 null）。
-- ============================================================

SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'invoice_info'
  AND column_name = 'invoice_number' AND is_nullable = 'NO';
SET @sql = IF(@cnt > 0, 'ALTER TABLE invoice_info MODIFY COLUMN invoice_number varchar(50) NULL COMMENT "发票号码（AI未能识别时为空）"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 校验
SELECT column_name, is_nullable, column_type FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'invoice_info'
  AND column_name = 'invoice_number';
