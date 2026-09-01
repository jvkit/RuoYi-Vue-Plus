-- ============================================================
-- common_user 角色补齐 OSS 查询权限（发票上传后前端回显文件列表必需）
-- ============================================================
-- 背景：验收/采购申请上传文件后，前端调用 /resource/oss/listByIds/{id}
--       需要 system:oss:query 权限；此前 common_user 只绑了 upload/download，
--       导致彭赛威等多PDF发票上传流程中断（发票匹配 AI 已返回结果，但保存/回显报错）。
-- 幂等：先判断是否已绑，未绑则新增
-- ============================================================

SET NAMES utf8mb4;

-- common_user (role_key='common_user')
SELECT IF(
    NOT EXISTS(
        SELECT 1
        FROM sys_role_menu rm
        JOIN sys_role r ON rm.role_id = r.role_id
        WHERE r.role_key = 'common_user'
          AND rm.menu_id = 1761400000000001600  -- system:oss:query
    ),
    'INSERT INTO sys_role_menu (role_id, menu_id) SELECT role_id, 1761400000000001600 FROM sys_role WHERE role_key = ''common_user''',
    'SELECT 1;'
) INTO @sql FROM DUAL;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- superadmin 特判走全量权限，无需补
-- 其它含 procurement:acceptance:add 的角色也一并补齐（保险）
SELECT IF(
    NOT EXISTS(
        SELECT 1
        FROM sys_role_menu rm
        JOIN sys_role r ON rm.role_id = r.role_id
        WHERE r.role_key IN ('procurement_contact','purchase_staff','procurement_applicant')
          AND rm.menu_id = 1761400000000001600
    ),
    'INSERT INTO sys_role_menu (role_id, menu_id) SELECT role_id, 1761400000000001600 FROM sys_role WHERE role_key IN ( ''procurement_contact'' , ''purchase_staff'' , ''procurement_applicant'' )',
    'SELECT 1;'
) INTO @sql FROM DUAL;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
