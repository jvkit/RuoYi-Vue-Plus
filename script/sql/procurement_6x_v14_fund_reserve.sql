-- =============================================================
-- procurement_6x_v14_fund_reserve.sql（2026-09-21）
-- 资金与报销体系 P1：双账本（项目账本 + 备用金额度占用制）+ 资金状态机
-- 设计依据：docs/9.20/资金与报销体系设计-v4-确认版.md
--
-- 口径（已与业务确认）：
--   K1 拨入/额度不算项目已用；K2/K3 自购+对公采购都扣项目预算；
--   K4 对公与备用金无关；K5 报销回笼只释放备用金占用，项目 used 不变；
--   K6 备用金额度按人（默认 10000，管理员可配）。
--   备用金可用 = 额度 − 占用；占用 = 自购申请金额（fund_status ∈ 已采购未报销/已报销未汇款）
--
-- 内容：
--   1. pms_fund_flow 加 3 列（title_type / applicant_id / applicant_name）
--   2. pms_procurement_request 加 7 列（fund_status + 报销/汇款留痕）
--   3. 新表 pms_reserve_account（备用金额度，一人一行）
--   4. 字典 pms_fund_status（4 值）
--   5. sys_config：procurement.reserve.default_quota = 10000
--   6. 菜单：资金管理下 3 按钮（状态/额度/维护）+ 报销下 2 按钮（编辑/删除）
--   7. 角色：采购经理 procurement_manager + 菜单绑定 + 王建龙绑定
--   8. 存量回填：流水补维度、已完成申请置资金状态、备用金账户初始化
--
-- 全部幂等，可重复执行；表结构只做加法（不改列名、不删列、不加 NOT NULL 无默认值列）。
-- =============================================================
SET NAMES utf8mb4;

SET @admin_id          := 1761100000000000001;  -- admin 用户
SET @dept_id           := 1761000000000000103;  -- 研发部门（与其他脚本一致）
SET @role_fund_manager := 1761300000000000014;  -- 采购经理（新角色，接 ...013 普通用户之后）
SET @user_wangjianlong := 1761100000000000015;  -- 王建龙（默认指派为采购经理，后续可调整）
SET @dict_type_id      := 1802002;              -- 字典类型 ID（接 1802001 采购类型之后）

-- ============================================================
-- 1. pms_fund_flow 加列：分账维度 + 谁的钱
-- ============================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_fund_flow' AND COLUMN_NAME = 'title_type');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_fund_flow ADD COLUMN title_type varchar(20) DEFAULT NULL COMMENT ''采购方式(自购/对公),取自申请单快照''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_fund_flow' AND COLUMN_NAME = 'applicant_id');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_fund_flow ADD COLUMN applicant_id bigint DEFAULT NULL COMMENT ''申请人ID(=谁的钱),取自申请单 create_by''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_fund_flow' AND COLUMN_NAME = 'applicant_name');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_fund_flow ADD COLUMN applicant_name varchar(64) DEFAULT NULL COMMENT ''申请人姓名快照''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引同样要幂等（重复执行 CREATE INDEX 会报 duplicate key name）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_fund_flow' AND INDEX_NAME = 'idx_fund_applicant');
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fund_applicant ON pms_fund_flow (applicant_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. pms_procurement_request 加列：资金状态机 + 留痕
--    状态单向不可回溯（业务要求），因此每次变更必须记录操作人与时间
-- ============================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'fund_status');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN fund_status varchar(32) DEFAULT NULL COMMENT ''资金状态(purchased_unreimbursed已采购未报销/reimbursed_unpaid已报销未汇款/reimbursed_paid已报销已汇款/not_applicable不适用对公)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'reimburse_date');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN reimburse_date datetime DEFAULT NULL COMMENT ''标记已报销时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'reimburse_by');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN reimburse_by bigint DEFAULT NULL COMMENT ''标记已报销的操作人ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'reimburse_by_name');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN reimburse_by_name varchar(64) DEFAULT NULL COMMENT ''标记已报销的操作人姓名''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'paid_date');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN paid_date datetime DEFAULT NULL COMMENT ''汇款确认时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'paid_by');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN paid_by bigint DEFAULT NULL COMMENT ''汇款确认人ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pms_procurement_request' AND COLUMN_NAME = 'paid_by_name');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE pms_procurement_request ADD COLUMN paid_by_name varchar(64) DEFAULT NULL COMMENT ''汇款确认人姓名''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 新表 pms_reserve_account：备用金额度（一人一行）
--    注意：不建 person_id 唯一索引——本表走逻辑删除(del_flag)，唯一索引会导致
--    「删除后重建同一人账户」失败；唯一性由 Service 层保证（先查后建）。
--    不存 occupied/available 列：占用与可用一律实时聚合，避免双写不一致。
-- ============================================================
CREATE TABLE IF NOT EXISTS pms_reserve_account (
  id           bigint        NOT NULL                COMMENT '主键',
  person_id    bigint        NOT NULL                COMMENT '用户ID',
  person_name  varchar(64)   DEFAULT NULL            COMMENT '姓名快照',
  quota        decimal(18,2) NOT NULL DEFAULT 10000.00 COMMENT '备用金额度(元),管理员可配',
  remark       varchar(500)  DEFAULT NULL            COMMENT '备注(如调增原因)',
  create_dept  bigint        DEFAULT NULL            COMMENT '创建部门',
  create_by    bigint        DEFAULT NULL            COMMENT '创建者',
  create_time  datetime      DEFAULT NULL            COMMENT '创建时间',
  update_by    bigint        DEFAULT NULL            COMMENT '更新者',
  update_time  datetime      DEFAULT NULL            COMMENT '更新时间',
  del_flag     bigint        NOT NULL DEFAULT 0      COMMENT '删除标志',
  PRIMARY KEY (id),
  KEY idx_reserve_person (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备用金额度账户(一人一行)';

-- ============================================================
-- 4. 字典 pms_fund_status（先删后插，幂等）
-- ============================================================
DELETE FROM sys_dict_data WHERE dict_type = 'pms_fund_status';
DELETE FROM sys_dict_type WHERE dict_type = 'pms_fund_status';

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (@dict_type_id, '资金状态', 'pms_fund_status', @dept_id, @admin_id, sysdate(), NULL, NULL, '采购申请资金状态（单向不可回溯）');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1803020, 1, '已采购未报销', 'purchased_unreimbursed', 'pms_fund_status', '', 'warning', 'Y', @dept_id, @admin_id, sysdate(), NULL, NULL, '审批通过后默认状态'),
(1803021, 2, '已报销未汇款', 'reimbursed_unpaid',      'pms_fund_status', '', 'primary', 'N', @dept_id, @admin_id, sysdate(), NULL, NULL, '资金管理员勾选已报销'),
(1803022, 3, '已报销已汇款', 'reimbursed_paid',        'pms_fund_status', '', 'success', 'N', @dept_id, @admin_id, sysdate(), NULL, NULL, '终态,备用金占用释放'),
(1803023, 4, '不适用(对公)', 'not_applicable',         'pms_fund_status', '', 'info',    'N', @dept_id, @admin_id, sysdate(), NULL, NULL, '对公直付,不涉及备用金');

-- ============================================================
-- 5. sys_config：备用金默认额度
-- ============================================================
DELETE FROM sys_config WHERE config_key = 'procurement.reserve.default_quota';
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1761700000000000104, '备用金-默认额度', 'procurement.reserve.default_quota', '10000', 'Y', @dept_id, @admin_id, sysdate(), NULL, NULL, '新建备用金账户时的默认额度(元)');

-- ============================================================
-- 6. 菜单：补齐按钮权限（含两个历史遗留无菜单的权限标识）
--    1801143 资金状态管理 procurement:fund:status（新）
--    1801144 备用金额度配置 procurement:fund:quota（新）
--    1801145 资金维护 procurement:fund:edit（历史遗留：sync 接口用了该权限但无菜单）
--    1801134 报销编辑 procurement:reimbursement:edit（历史遗留无菜单）
--    1801135 报销删除 procurement:reimbursement:remove（历史遗留无菜单）
-- ============================================================
DELETE FROM sys_role_menu WHERE menu_id IN (1801143, 1801144, 1801145, 1801134, 1801135);
DELETE FROM sys_menu      WHERE menu_id IN (1801143, 1801144, 1801145, 1801134, 1801135);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1801143, '资金状态管理',   1801140, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:fund:status',           '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '勾选已报销/确认汇款(单向不可回溯)'),
(1801144, '备用金额度配置', 1801140, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:fund:quota',            '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '配置每人备用金额度'),
(1801145, '资金维护',       1801140, 6, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:fund:edit',             '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, 'sync 重算接口权限'),
(1801134, '报销编辑',       1801130, 4, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:reimbursement:edit',    '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, ''),
(1801135, '报销删除',       1801130, 5, '#', NULL, NULL, 'N', 'Y', 'F', '0', '0', 'procurement:reimbursement:remove',  '#', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '');

-- ============================================================
-- 7. 角色：采购经理（先删后插，幂等）
--    data_scope='1' 全部数据权限（资金管理需要看全量）
-- ============================================================
DELETE FROM sys_role_menu WHERE role_id = @role_fund_manager;
DELETE FROM sys_user_role WHERE role_id = @role_fund_manager;
DELETE FROM sys_role      WHERE role_id = @role_fund_manager;

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES (@role_fund_manager, '采购经理', 'procurement_manager', 14, '1', 0, 1, '0', '0', @admin_id, sysdate(),
        '资金管理与报销材料管控：资金状态勾选、汇款确认、备用金额度配置、报销包生成下载');

-- 7.1 采购管理目录下的目录/菜单（含隐藏详情页，保证「我的任务」跳转可用）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_fund_manager, m.menu_id
FROM sys_menu m
WHERE m.menu_id >= 1801000 AND m.menu_id < 1802000 AND m.menu_type IN ('M', 'C')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = @role_fund_manager AND rm.menu_id = m.menu_id);

-- 7.2 资金管理 + 报销的按钮权限（其他模块按钮不给，避免误操作业务数据）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_fund_manager, m.menu_id
FROM sys_menu m
WHERE m.menu_id IN (1801141, 1801142, 1801143, 1801144, 1801145,
                    1801131, 1801132, 1801133, 1801134, 1801135)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = @role_fund_manager AND rm.menu_id = m.menu_id);

-- 7.3 我的任务树（待办/我发起/已办/抄送）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_fund_manager, m.menu_id
FROM sys_menu m
WHERE m.menu_id IN (1761400000000011618, 1761400000000011619, 1761400000000011629,
                    1761400000000011632, 1761400000000011633)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = @role_fund_manager AND rm.menu_id = m.menu_id);

-- 7.4 默认指派：王建龙（后续可在角色管理里调整人员）
INSERT INTO sys_user_role (user_id, role_id)
SELECT @user_wangjianlong, @role_fund_manager
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = @user_wangjianlong AND role_id = @role_fund_manager);

-- ============================================================
-- 8. 存量回填
-- ============================================================
-- 8.1 流水补分账维度与申请人（只补空的，幂等）
UPDATE pms_fund_flow f
JOIN pms_procurement_request r ON r.id = f.request_id
LEFT JOIN sys_user u ON u.user_id = r.create_by
SET f.title_type     = r.title_type,
    f.applicant_id   = r.create_by,
    f.applicant_name = u.nick_name
WHERE f.title_type IS NULL OR f.applicant_id IS NULL;

-- 8.2 已完成申请置资金状态
--     自购（含历史 title_type 为空的旧单，历史上均为自购口径）→ 已采购未报销
UPDATE pms_procurement_request
SET fund_status = 'purchased_unreimbursed'
WHERE status = 'finish'
  AND fund_status IS NULL
  AND (title_type = '自购' OR title_type IS NULL OR title_type = '');

--     对公 → 不适用（不进三态，报销包仍可生成做材料归档）
UPDATE pms_procurement_request
SET fund_status = 'not_applicable'
WHERE status = 'finish'
  AND fund_status IS NULL
  AND title_type = '对公';

-- 8.3 备用金账户初始化：为已有自购申请的申请人建账户，额度取 sys_config 默认值
--     ID 用 1804000000000000000 段（与雪花 ID 段不冲突），ROW_NUMBER 保证唯一
INSERT INTO pms_reserve_account (id, person_id, person_name, quota, remark, create_dept, create_by, create_time, del_flag)
SELECT 1804000000000000000 + ROW_NUMBER() OVER (ORDER BY u.user_id),
       u.user_id,
       u.nick_name,
       CAST((SELECT config_value FROM sys_config WHERE config_key = 'procurement.reserve.default_quota') AS DECIMAL(18,2)),
       'v14 初始化',
       @dept_id, @admin_id, sysdate(), 0
FROM sys_user u
WHERE u.del_flag = '0'
  AND EXISTS (SELECT 1 FROM pms_procurement_request r WHERE r.create_by = u.user_id AND r.title_type = '自购')
  AND NOT EXISTS (SELECT 1 FROM pms_reserve_account a WHERE a.person_id = u.user_id);

-- ============================================================
-- 9. 校验输出（执行后人工核对数量）
-- ============================================================
SELECT '资金状态分布' AS item, fund_status AS k, COUNT(*) AS cnt FROM pms_procurement_request GROUP BY fund_status
UNION ALL
SELECT '流水已补维度', 'title_type非空', COUNT(*) FROM pms_fund_flow WHERE title_type IS NOT NULL
UNION ALL
SELECT '流水缺维度', 'title_type为空', COUNT(*) FROM pms_fund_flow WHERE title_type IS NULL
UNION ALL
SELECT '备用金账户', '行数', COUNT(*) FROM pms_reserve_account
UNION ALL
SELECT '采购经理菜单', '绑定数', COUNT(*) FROM sys_role_menu WHERE role_id = 1761300000000000014;
