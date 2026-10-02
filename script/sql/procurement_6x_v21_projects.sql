SET NAMES utf8mb4;
-- ============================================================================
-- v21 项目树重建（2026-09-24）
-- 1) 幂等确保两个归属（长三角/天目湖）存在（与《项目归属.md》同源）
-- 2) 删除旧测试项目（本地隐私保护项目、全画幅结构光超分辨显微镜系统）
-- 3) 按固定 ID 段 21000000000000000xx 重建项目树（重复执行不产生重复数据）
--    树：全域数字化平台(长三角) = 对内(400万) + 对外(1300万)；超分辨光学显微镜(天目湖,500万)
--    父级 budget/used_amount 由代码按「子级之和」自动同步（见 PmsProjectServiceImpl.syncAncestors）
-- ============================================================================

-- 1) 归属（已存在则跳过）
INSERT INTO `pms_fund_source` (`id`, `parent_id`, `name`, `sort`, `status`, `create_time`)
SELECT 1900000000000000001, 0, '长三角', 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_fund_source` WHERE `id` = 1900000000000000001);
INSERT INTO `pms_fund_source` (`id`, `parent_id`, `name`, `sort`, `status`, `create_time`)
SELECT 1900000000000000002, 0, '天目湖', 2, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_fund_source` WHERE `id` = 1900000000000000002);

-- 2) 删除旧测试项目（仅这两个已知旧 ID，避免误伤后续新建项目）
DELETE FROM `pms_project` WHERE `id` IN (2089591013069398018, 2090000000000000010);

-- 3) 项目树（固定 ID，NOT EXISTS 幂等）
-- 一级：全域数字化平台（归属长三角，负责人空，预算=对内400万+对外1300万=1700万）
INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000001, 'purp-20260924-001', '全域数字化平台', 0, 1900000000000000001, NULL, NULL, 1700000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000001);

-- 一级：超分辨光学显微镜（归属天目湖，负责人李迪，预算500万，备注剩余211万）
INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000002, 'purp-20260924-002', '超分辨光学显微镜', 0, 1900000000000000002, '李迪', 1761100000000000016, 5000000.00, 0.00, 1, '剩余211万', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000002);

-- 二级：对内（预算=6个叶子之和=400万）
INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000003, 'purp-20260924-003', '对内', 2100000000000000001, 1900000000000000001, NULL, NULL, 4000000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000003);

-- 二级：对外（预算=R200+I700+S200+E200=1300万）
INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000004, 'purp-20260924-004', '对外', 2100000000000000001, 1900000000000000001, NULL, NULL, 13000000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000004);

-- 对内叶子（归属长三角，负责人/预算/备注见下）
INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000005, 'purp-20260924-005', 'OA一体化与AI辅助应用建设', 2100000000000000003, 1900000000000000001, '冯忠俊', 2093000000000000005, 650000.00, 0.00, 1, '由RISE平台拨付', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000005);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000006, 'purp-20260924-006', '智能巡检平台建设', 2100000000000000003, 1900000000000000001, '彭赛威', 2093000000000000001, 800000.00, 0.00, 1, '由RISE平台拨付', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000006);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000007, 'purp-20260924-007', '科研仪器共享预约平台建设', 2100000000000000003, 1900000000000000001, '彭赛威', 2093000000000000001, 400000.00, 0.00, 1, '由RISE平台拨付', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000007);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000008, 'purp-20260924-008', '共享存储、私有云盘与备份恢复建设', 2100000000000000003, 1900000000000000001, '李炳晨', 2093000000000000003, 400000.00, 0.00, 1, '由RISE平台拨付', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000008);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000009, 'purp-20260924-009', '网络升级与基础设施建设', 2100000000000000003, 1900000000000000001, '彭赛威', 2093000000000000001, 1150000.00, 0.00, 1, '网络拓展与管理75万元+网络基建40万元', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000009);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `remark`, `create_time`)
SELECT 2100000000000000010, 'purp-20260924-010', '网络架构、安全运维与综合交付公共建设', 2100000000000000003, 1900000000000000001, '王建龙', 1761100000000000015, 600000.00, 0.00, 1, '网络运维服务人员60万元', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000010);

-- 对外叶子（负责人均王建龙；金额单位万：R200/I700/S200/E200）
INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000011, 'purp-20260924-011', 'R(Resource)企业知识智能', 2100000000000000004, 1900000000000000001, '王建龙', 1761100000000000015, 2000000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000011);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000012, 'purp-20260924-012', 'I(Industry)产业数据与专有', 2100000000000000004, 1900000000000000001, '王建龙', 1761100000000000015, 7000000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000012);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000013, 'purp-20260924-013', 'S(Security)智能安全巡检', 2100000000000000004, 1900000000000000001, '王建龙', 1761100000000000015, 2000000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000013);

INSERT INTO `pms_project` (`id`, `project_code`, `project_name`, `parent_id`, `owner_id`, `leader`, `leader_id`, `budget`, `used_amount`, `status`, `create_time`)
SELECT 2100000000000000014, 'purp-20260924-014', 'E(Executive)企业智能治理', 2100000000000000004, 1900000000000000001, '王建龙', 1761100000000000015, 2000000.00, 0.00, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `pms_project` WHERE `id` = 2100000000000000014);
