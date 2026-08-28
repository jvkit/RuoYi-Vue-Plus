#!/bin/bash
# ============================================================
# OA 6x 服务器完整部署脚本
# 运行位置：服务器 /home/liyang/jvkit/oa-workspace
# 功能：git pull → build → SQL → 重启
# ============================================================
set -euo pipefail

# 加载环境变量（兼容 ssh 非登录 shell）
[ -f "$HOME/.profile" ] && source "$HOME/.profile"

OA_WORKSPACE="${OA_WORKSPACE:-$HOME/jvkit/oa-workspace}"
OA_DEPLOY="${OA_DEPLOY:-$HOME/jvkit/oa}"
BACKEND_PORT="${BACKEND_PORT:-8092}"
BACKEND_LOG="$OA_DEPLOY/logs/oa-backend.log"

log()  { echo -e "\033[1;36m[deploy]\033[0m $*"; }
warn() { echo -e "\033[1;33m[warn]\033[0m $*"; }
die()  { echo -e "\033[1;31m[error]\033[0m $*"; exit 1; }

[ "$(whoami)" = "liyang" ] || die "请用 liyang 用户执行"

# ---------- 1. 更新代码 ----------
log "拉取后端代码..."
cd "$OA_WORKSPACE/ruoyi-6x"
git pull origin "$(git branch --show-current)"

log "拉取前端代码..."
cd "$OA_WORKSPACE/plus-ui-6x"
git pull origin "$(git branch --show-current)"

# ---------- 2. 构建后端 ----------
log "构建后端 jar..."
cd "$OA_WORKSPACE/ruoyi-6x"
mvn package -DskipTests -pl ruoyi-admin -am -q || die "后端构建失败"

# ---------- 3. 构建前端 ----------
log "安装前端依赖..."
cd "$OA_WORKSPACE/plus-ui-6x"
pnpm install --frozen-lockfile || die "前端依赖安装失败"

log "构建前端静态文件..."
pnpm build:prod || die "前端构建失败"

# ---------- 4. 执行 SQL 增量脚本 ----------
log "执行 SQL 增量脚本..."
bash "$OA_WORKSPACE/ruoyi-6x/script/sql/apply-sql.sh" || die "SQL 执行失败"

# ---------- 5. 停止旧后端 ----------
OLD_PID=$(ss -tlnp 2>/dev/null | grep ":$BACKEND_PORT " | grep -oP 'pid=\K[0-9]+' | head -1 || true)
if [ -n "$OLD_PID" ]; then
  log "停止旧后端进程 $OLD_PID..."
  kill "$OLD_PID" || true
  for i in $(seq 1 30); do
    ss -tlnp 2>/dev/null | grep -q ":$BACKEND_PORT " || break
    sleep 1
  done
fi

# ---------- 6. 部署产物 ----------
log "替换后端 jar..."
cp "$OA_WORKSPACE/ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar" "$OA_DEPLOY/backend/ruoyi-admin.jar"

log "替换前端 dist..."
rm -rf "$OA_DEPLOY/dist"
cp -r "$OA_WORKSPACE/plus-ui-6x/dist" "$OA_DEPLOY/dist"

# ---------- 7. 启动后端 ----------
log "启动后端（端口 $BACKEND_PORT）..."
mkdir -p "$(dirname "$BACKEND_LOG")"
nohup java -jar "$OA_DEPLOY/backend/ruoyi-admin.jar" \
  --server.port=$BACKEND_PORT \
  --spring.profiles.active=prod \
  --spring.datasource.dynamic.datasource.master.url="jdbc:mysql://127.0.0.1:3306/ry-vue-6x?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true" \
  --spring.datasource.dynamic.datasource.master.username=root \
  --spring.datasource.dynamic.datasource.master.password=ruoyi123 \
  --spring.data.redis.host=127.0.0.1 \
  --spring.data.redis.password=ruoyi123 \
  --spring.data.redis.port=6379 \
  > "$BACKEND_LOG" 2>&1 &

# 等待后端就绪
log "等待后端就绪..."
for i in $(seq 1 60); do
  if curl -sf "http://127.0.0.1:$BACKEND_PORT/" >/dev/null 2>&1; then
    log "后端已就绪"
    break
  fi
  [ "$i" = 60 ] && die "后端 60s 内未就绪，请检查 $BACKEND_LOG"
  sleep 1
done

# ---------- 8. 重载 nginx ----------
log "重载 nginx..."
sudo nginx -t && sudo nginx -s reload || warn "nginx 重载失败，请手动检查"

# ---------- 9. 冒烟验证 ----------
log "冒烟验证..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$BACKEND_PORT/")
[ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ] || die "后端健康检查失败: $HTTP_CODE"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://172.16.16.110/oa/")
[ "$HTTP_CODE" = "200" ] || die "前端访问失败: $HTTP_CODE"

log "部署完成: http://172.16.16.110/oa"
log "后端日志: $BACKEND_LOG"
