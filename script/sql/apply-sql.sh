#!/bin/bash
# ============================================================
# OA 6x SQL 增量应用脚本（服务器端执行）
# 按顺序执行 ruoyi-6x/script/sql/ 下新增/未执行的 .sql 文件
# 要求所有 SQL 脚本幂等可重复执行
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APPLIED_LOG="${OA_DEPLOY:-$HOME/jvkit/oa}/sql/applied-sql.log"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-ruoyi123}"
DB_NAME="${DB_NAME:-ry-vue-6x}"

log()  { echo -e "\033[1;36m[sql]\033[0m $*"; }
warn() { echo -e "\033[1;33m[warn]\033[0m $*"; }
die()  { echo -e "\033[1;31m[error]\033[0m $*"; exit 1; }

mkdir -p "$(dirname "$APPLIED_LOG")"
touch "$APPLIED_LOG"

# 如果宿主机没有 mysql 客户端，尝试用 docker exec 调用容器内的 mysql
if command -v mysql &>/dev/null; then
  MYSQL_CMD=(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS")
elif docker exec ruoyi-mysql mysql -u"$DB_USER" -p"$DB_PASS" -e "SELECT 1" "$DB_NAME" &>/dev/null; then
  MYSQL_CMD=(docker exec -i ruoyi-mysql mysql -u"$DB_USER" -p"$DB_PASS")
else
  die "未找到 mysql 客户端，且无法通过 docker exec ruoyi-mysql 连接"
fi

# 测试连接
if ! "${MYSQL_CMD[@]}" -e "SELECT 1" "$DB_NAME" &>/dev/null; then
  die "无法连接 MySQL: $DB_HOST:$DB_PORT/$DB_NAME"
fi

# 已执行过的文件名集合（按整行匹配）
mapfile -t APPLIED < "$APPLIED_LOG"

# 按文件名排序执行
RUN_COUNT=0
for sql_file in "$SCRIPT_DIR"/*.sql; do
  [ -f "$sql_file" ] || continue
  filename=$(basename "$sql_file")

  if printf '%s\n' "${APPLIED[@]}" | grep -qxF "$filename"; then
    log "跳过已执行: $filename"
    continue
  fi

  log "执行: $filename"
  if "${MYSQL_CMD[@]}" --default-character-set=utf8mb4 "$DB_NAME" < "$sql_file"; then
    echo "$filename" >> "$APPLIED_LOG"
    ((RUN_COUNT++))
    log "完成: $filename"
  else
    die "执行失败: $filename"
  fi
done

log "SQL 应用完成，本次执行 $RUN_COUNT 个脚本"
