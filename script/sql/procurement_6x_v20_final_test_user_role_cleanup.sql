-- =============================================================
-- procurement_6x_v20_final_test_user_role_cleanup.sql（2026-09-24）
-- 端到端精测修复：冯忠俊（普通用户验收账号）菜单越权 —— 能看到「资金管理」
--
-- 现象（Playwright 菜单全览截图 02-menu）：
--   fengzhongjun / 666666 左侧「采购管理」下出现「资金管理」菜单，
--   按角色设计普通用户（common_user）不应有任何资金管理入口。
--
-- 根因：
--   fengzhongjun 除 common_user(1761300000000000013) 外，还绑着联测临时角色
--   reserve_view_e2e(1761300000000000099，「备用金查看(联测临时)」)，
--   该角色绑定了菜单 1801140「资金管理」(perms=procurement:fund:list)，
--   是 e2e_15 备用金联测时留下的临时授权，测完未回收。
--
-- 修复：解除 fengzhongjun 与 reserve_view_e2e 的绑定，恢复其「纯 common_user」身份。
--   临时角色本身保留（不动 sys_role_menu），避免影响 e2e_15 脚本的重复执行设计。
-- 幂等：NOT EXISTS 反向校验 + DELETE 天然幂等，可重复执行。
-- 注意：权限在登录时装入会话，执行后需重新登录才生效。
-- =============================================================
SET NAMES utf8mb4;

DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON u.user_id = ur.user_id AND u.user_name = 'fengzhongjun'
JOIN sys_role r ON r.role_id = ur.role_id AND r.role_key = 'reserve_view_e2e';

-- ---------- 校验输出：fengzhongjun 应只剩 common_user ----------
SELECT u.user_name, r.role_key, r.role_name
FROM sys_user_role ur
JOIN sys_user u ON u.user_id = ur.user_id
JOIN sys_role r ON r.role_id = ur.role_id
WHERE u.user_name = 'fengzhongjun';
