SET NAMES utf8mb4;

-- 报销单状态列长度不足修复（幂等）
-- 背景：pms_reimbursement.status 原为 varchar(20)，但业务状态值
--   purchased_unreimbursed（22 字符）/ reimbursed_unpaid / reimbursed_paid 会超长，
--   导致新建报销单时报 "Data too long for column 'status'"。
-- 处理：统一扩到 varchar(32)，可重复执行。

SET @col_len = (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pms_reimbursement'
    AND COLUMN_NAME = 'status'
);

SET @sql = IF(@col_len IS NOT NULL AND @col_len < 32,
  'ALTER TABLE pms_reimbursement MODIFY COLUMN status varchar(32) DEFAULT ''packing'' COMMENT ''报销状态''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
