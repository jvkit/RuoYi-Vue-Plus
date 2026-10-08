-- v26 账户与备用金初始化（幂等）
-- 1. 新建真实用户：符龙/蒋泽涛/王旭东（普通用户角色，密码 666666 与现网一致）
-- 2. 新建测试账户：t_apply（纯普通用户）、t_finance（普通用户+采购经理），密码 666666
-- 3. 备用金：所有真实用户 + 测试账户初始化 10000，已存在跳过
SET NAMES utf8mb4;

SET @common_role := 1761300000000000013;
SET @manager_role := 1761300000000000014;
SET @pwd := '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne'; -- 666666（与现网用户一致）
SET @dept := 1761000000000000103;

-- ---------- 1. 用户（user_name 查重，已存在则不动） ----------
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phone_number, gender, avatar, password, status, del_flag, create_dept, create_by, create_time, remark)
SELECT u.user_id, @dept, u.user_name, u.nick_name, 'sys_user', NULL, NULL, '2', 0, @pwd, '0', '0', NULL, 1761100000000000001, NOW(), 'v26 初始化创建'
FROM (
  SELECT 2094000000000000001 AS user_id, 'fulong'      AS user_name, '符龙'   AS nick_name
  UNION ALL SELECT 2094000000000000002, 'jiangzetao', '蒋泽涛'
  UNION ALL SELECT 2094000000000000003, 'wangxudong', '王旭东'
  UNION ALL SELECT 2094000000000000101, 't_apply',    '测试-申请人'
  UNION ALL SELECT 2094000000000000102, 't_finance',  '测试-财务'
) u
WHERE NOT EXISTS (SELECT 1 FROM sys_user s WHERE s.user_name = u.user_name);

-- ---------- 2. 角色绑定 ----------
-- 全部五个新账户都绑普通用户
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, @common_role
FROM (
  SELECT 2094000000000000001 AS user_id UNION ALL SELECT 2094000000000000002 UNION ALL SELECT 2094000000000000003
  UNION ALL SELECT 2094000000000000101 UNION ALL SELECT 2094000000000000102
) u
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role x WHERE x.user_id = u.user_id AND x.role_id = @common_role);

-- t_finance 追加采购经理
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2094000000000000102, @manager_role
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role x WHERE x.user_id = 2094000000000000102 AND x.role_id = @manager_role);

-- ---------- 3. 备用金初始化（每人 10000，已存在跳过） ----------
INSERT INTO pms_reserve_account (id, person_id, person_name, quota, remark, create_dept, create_by, create_time)
SELECT
  1804000000000000100 + ROW_NUMBER() OVER (ORDER BY u.user_id),
  u.user_id,
  u.nick_name,
  10000.00,
  'v26 初始化',
  @dept,
  1761100000000000001,
  NOW()
FROM sys_user u
WHERE u.user_type = 'sys_user'
  AND u.del_flag = '0'
  AND u.user_name <> 'admin'
  AND NOT EXISTS (SELECT 1 FROM pms_reserve_account r WHERE r.person_id = u.user_id AND r.del_flag = '0');
