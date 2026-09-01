SET NAMES utf8mb4;
-- ============================================================
-- 采购 v5：采购申请导出全局配置（sys_config）
-- 幂等：按 config_key 先删后插
-- ============================================================

SET @admin_id = 1761100000000000001;
SET @dept_id  = 1761000000000000103;

-- 项目归属（导出 Excel 中"项目归属"列的默认值）
DELETE FROM sys_config WHERE config_key = 'procurement.export.project_belong';
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1761700000000000101, '采购导出-项目归属', 'procurement.export.project_belong', '李迪科学家工作室', 'Y', @dept_id, @admin_id, sysdate(), NULL, NULL, '采购申请单导出时项目归属列的默认值');

-- 申请人（导出 Excel 中"申请人"的默认值）
DELETE FROM sys_config WHERE config_key = 'procurement.export.applicant';
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1761700000000000102, '采购导出-申请人', 'procurement.export.applicant', '彭赛威', 'Y', @dept_id, @admin_id, sysdate(), NULL, NULL, '采购申请单导出时申请人的默认值');

-- 保管人（导出 Excel 中"保管人"的默认值）
DELETE FROM sys_config WHERE config_key = 'procurement.export.keeper';
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1761700000000000103, '采购导出-保管人', 'procurement.export.keeper', '裴天姿', 'Y', @dept_id, @admin_id, sysdate(), NULL, NULL, '采购申请单导出时保管人的默认值');

-- 校验
SELECT config_id, config_name, config_key, config_value FROM sys_config WHERE config_key LIKE 'procurement.export.%' ORDER BY config_id;
