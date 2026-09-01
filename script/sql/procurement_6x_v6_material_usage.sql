-- ============================================================
-- 采购申请明细增加物料用途字段
-- ============================================================
-- 用途：支持在采购申请商品明细中填写物料用途（默认"研发"）
-- 幂等：先判断列是否存在，不存在则添加
-- ============================================================

SET NAMES utf8mb4;

SELECT IF(
    NOT EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'pms_procurement_request_item'
          AND column_name = 'material_usage'
    ),
    'ALTER TABLE pms_procurement_request_item ADD COLUMN material_usage varchar(100) DEFAULT NULL COMMENT "物料用途";',
    'SELECT 1;'
) INTO @sql FROM DUAL;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
