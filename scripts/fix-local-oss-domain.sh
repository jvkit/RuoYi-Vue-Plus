#!/usr/bin/env bash
# =============================================================
# 本地开发环境专用：把 OSS 访问地址从服务器地址改回本地 MinIO
#
# 为什么需要这个脚本：
#   ruoyi-6x/script/sql/procurement_6x_v13_minio_domain.sql 里写死了服务器地址
#   （domain_url=172.16.16.110:9000），那是给服务器用的。如果它在本地库被执行过，
#   本地新上传的附件 URL 会指向服务器 MinIO —— 文件其实在本地 MinIO 里，
#   于是所有图片/PDF 打开都是 404（审批人看不到材料）。
#
# 本脚本做三件事（只动本地库与本地 Redis，绝不碰服务器）：
#   1. sys_oss_config.domain_url 清空 → URL 回退用 endpoint(127.0.0.1:9000) 生成
#   2. 存量 sys_oss.url 里的服务器地址批量换回 127.0.0.1:9000
#   3. 清 Redis 里的 sys_oss / sys_oss_config 缓存（否则后端继续返回旧地址）
# 之后必须重启后端（OSS 配置在启动时装载）。
#
# ⚠️ 不要在服务器上跑这个脚本，服务器需要 domain_url=172.16.16.110:9000。
# ⚠️ 也不要把等效 SQL 放进 ruoyi-6x/script/sql/，那里会被 apply-sql.sh 自动执行到服务器。
# =============================================================
set -euo pipefail

MYSQL="docker exec -i mysql mysql -uroot -proot --default-character-set=utf8mb4 -D ry-vue-6x"
SERVER_HOST="172.16.16.110:9000"
LOCAL_HOST="127.0.0.1:9000"

echo "== 修复前 =="
$MYSQL -e "SELECT config_key, endpoint, IFNULL(domain_url,'(空)') domain_url FROM sys_oss_config WHERE config_key='minio';
           SELECT COUNT(*) AS 指向服务器的OSS记录 FROM sys_oss WHERE url LIKE 'http://${SERVER_HOST}/%';" 2>/dev/null

$MYSQL -e "
UPDATE sys_oss_config SET domain_url = '' WHERE config_key = 'minio';
UPDATE sys_oss SET url = REPLACE(url, 'http://${SERVER_HOST}', 'http://${LOCAL_HOST}')
 WHERE url LIKE 'http://${SERVER_HOST}/%';" 2>/dev/null

echo "== 清 Redis OSS 缓存 =="
docker exec redis redis-cli -a ruoyi123 --no-auth-warning DEL sys_oss sys_oss_config 2>/dev/null || echo "（Redis 清理失败，可手动 docker exec redis redis-cli -a ruoyi123 DEL sys_oss sys_oss_config）"

echo "== 修复后 =="
$MYSQL -e "SELECT config_key, endpoint, IFNULL(domain_url,'(空)') domain_url FROM sys_oss_config WHERE config_key='minio';
           SELECT COUNT(*) AS 仍指向服务器 FROM sys_oss WHERE url LIKE 'http://${SERVER_HOST}/%';
           SELECT COUNT(*) AS 指向本地 FROM sys_oss WHERE url LIKE 'http://${LOCAL_HOST}/%';" 2>/dev/null

echo
echo "✅ 完成。请重启后端使配置生效："
echo "   bash scripts/start-6x.sh --no-front"
