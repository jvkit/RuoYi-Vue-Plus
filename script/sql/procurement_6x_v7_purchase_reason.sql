-- ============================================================
-- 采购申请明细增加采购原因（每个明细单独的采购理由，对应导出Excel Q列）
-- ============================================================
-- 幂等：先判断列是否存在，不存在则添加
-- ============================================================

SET NAMES utf8mb4;

SELECT IF(
    NOT EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'pms_procurement_request_item'
          AND column_name = 'purchase_reason'
    ),
    'ALTER TABLE pms_procurement_request_item ADD COLUMN purchase_reason varchar(500) DEFAULT NULL COMMENT "采购原因(明细级)";',
    'SELECT 1;'
) INTO @sql FROM DUAL;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
