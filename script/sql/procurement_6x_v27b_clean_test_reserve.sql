-- v27b 清理测试/演示账户的备用金（幂等）
-- 这些账户是角色测试号（proc_*）、数据权限测试号（test/test1）和临时联测号，不参与真实备用金业务
SET NAMES utf8mb4;

UPDATE pms_reserve_account r
JOIN sys_user u ON u.user_id = r.person_id
SET r.del_flag = '1', r.update_time = NOW()
WHERE r.del_flag = '0'
  AND (
    u.user_name LIKE 'test%'          -- test / test1（数据权限测试）
    OR u.user_name LIKE 'proc\_%'     -- proc_applier / proc_contact / proc_leader / proc_keeper
    OR u.user_name LIKE 't\_%'        -- t_apply / t_finance（测试账户）
    OR u.user_name = 'reserve_view_e2e' -- 备用金查看（联测临时）
  );
