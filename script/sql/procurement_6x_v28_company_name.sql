-- v28 对公采购申请加公司名字段（幂等）
-- 对公采购时填写的供应商公司名称，导出申请表时写入 R 列（替代原来的链接）
SET NAMES utf8mb4;

-- pms_procurement_request 加 company_name
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'company_name'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN company_name VARCHAR(200) NULL COMMENT ''对公供应商公司名（对公时可选填）'' AFTER invoice_info_json',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
