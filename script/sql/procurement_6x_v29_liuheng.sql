-- v29 新增用户：刘恒（普通用户 + 采购经理），密码 666666，备用金初始化 1 万
-- 幂等：user_name 查重，角色/备用金均先判断再插
SET NAMES utf8mb4;

SET @common_role  := 1761300000000000013;  -- 普通用户
SET @manager_role := 1761300000000000014;  -- 采购经理
SET @pwd := '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne'; -- 666666（与现网一致）
SET @dept := 1761000000000000103;
SET @uid  := 2094000000000000004;          -- 刘恒 user_id（接符龙/蒋泽涛/王旭东之后）

-- ---------- 1. 用户 ----------
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phone_number, gender, avatar, password, status, del_flag, create_dept, create_by, create_time, remark)
SELECT @uid, @dept, 'liuheng', '刘恒', 'sys_user', NULL, NULL, '2', 0, @pwd, '0', '0', NULL, 1761100000000000001, NOW(), 'v29 初始化创建'
WHERE NOT EXISTS (SELECT 1 FROM sys_user s WHERE s.user_name = 'liuheng');

-- ---------- 2. 角色绑定：普通用户 + 采购经理 ----------
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, @common_role FROM sys_user u
WHERE u.user_name = 'liuheng'
  AND NOT EXISTS (SELECT 1 FROM sys_user_role x WHERE x.user_id = u.user_id AND x.role_id = @common_role);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, @manager_role FROM sys_user u
WHERE u.user_name = 'liuheng'
  AND NOT EXISTS (SELECT 1 FROM sys_user_role x WHERE x.user_id = u.user_id AND x.role_id = @manager_role);

-- ---------- 3. 备用金初始化 1 万 ----------
INSERT INTO pms_reserve_account (id, person_id, person_name, quota, remark, create_dept, create_by, create_time)
SELECT 1804000000000000201, u.user_id, u.nick_name, 10000.00, 'v29 初始化', @dept, 1761100000000000001, NOW()
FROM sys_user u
WHERE u.user_name = 'liuheng'
  AND NOT EXISTS (SELECT 1 FROM pms_reserve_account r WHERE r.person_id = u.user_id AND r.del_flag = '0');
