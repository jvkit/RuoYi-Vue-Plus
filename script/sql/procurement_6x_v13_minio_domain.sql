-- ============================================================
-- v13: MinIO 对外访问域名配置（服务器独立 MinIO 部署配套）
-- 背景：sys_oss.url 在对象上传时按「domain_url 优先，否则 endpoint」生成，
--       本地 dev 库 dump 到服务器后存量 url 是 127.0.0.1:9000，浏览器无法访问。
-- 做法：
--   1) minio 配置的 domain_url 设为服务器对外地址（后端本机上传仍走 127.0.0.1 endpoint）
--   2) 存量 sys_oss.url 中的 127.0.0.1:9000 批量替换为对外地址
-- 幂等：domain_url 仅当为空时更新；url 替换只作用于仍指向 127.0.0.1 的记录。
-- ============================================================
SET NAMES utf8mb4;

UPDATE sys_oss_config
SET domain_url = '172.16.16.110:9000'
WHERE config_key = 'minio'
  AND (domain_url IS NULL OR domain_url = '');

UPDATE sys_oss
SET url = REPLACE(url, 'http://127.0.0.1:9000', 'http://172.16.16.110:9000')
WHERE url LIKE 'http://127.0.0.1:9000/%';
