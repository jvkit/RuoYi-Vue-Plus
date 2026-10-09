-- v27 清理演示公告（幂等）
-- 删除 sys_notice 中的历史演示/欢迎公告，让消息盒子「通知」页签不再被欢迎语刷屏
-- 保留业务相关公告（如有）；本脚本只清 notice_type=1（通知）且标题含欢迎/温馨提示/维护的演示数据
SET NAMES utf8mb4;

DELETE FROM sys_notice
WHERE notice_type = '1'
  AND (
    notice_title LIKE '%欢迎%'
    OR notice_title LIKE '%温馨提示%'
    OR notice_title LIKE '%维护通知%'
    OR notice_title LIKE '%公共%'
  );

-- 可选：把 notice_type=2（公告）的也清了，如需保留业务公告请注释下一行
-- DELETE FROM sys_notice WHERE notice_type = '2';
