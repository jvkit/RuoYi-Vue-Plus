#!/bin/bash
# ============================================================
# 6x 项目一键启动脚本
# 作用：启动 MySQL/Redis 容器 + 放行 UFW 端口 + 启动后端 + Snail-AI + 前端
# 用法：
#   scripts/start-6x.sh            # 启动全部
#   scripts/start-6x.sh --no-front # 不启动前端（仅后端+AI）
#   scripts/start-6x.sh --no-ai    # 不启动 Snail-AI
# ============================================================
set -u

# ---------- 基础配置 ----------
OA_HOME="/home/jvkit/workspace/oa"
BACKEND_JAR="$OA_HOME/ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar"
SNAILAI_JAR="$OA_HOME/ruoyi-6x/ruoyi-extend/ruoyi-snailai-server/target/ruoyi-snailai-server.jar"
FRONTEND_DIR="$OA_HOME/plus-ui-6x"

BACKEND_PORT=8091
FRONTEND_PORT=8082
SNAILAI_WEB_PORT=8900
SNAILAI_GRPC_PORT=18888

# UFW 需放行的端口（本机被 UFW 默认拒绝入站，漏放会导致连不上）
UFW_PORTS="3306 33060 6379 8091 8082 8900 18888 38091 9090 8800"

NO_FRONT=false
NO_AI=false
for arg in "$@"; do
  case "$arg" in
    --no-front) NO_FRONT=true ;;
    --no-ai)    NO_AI=true ;;
  esac
done

log()  { echo -e "\033[1;36m[start]\033[0m $*"; }
warn() { echo -e "\033[1;33m[warn]\033[0m $*"; }
die()  { echo -e "\033[1;31m[error]\033[0m $*"; exit 1; }

# ---------- 1. 依赖检查 ----------
command -v docker >/dev/null 2>&1 || die "未找到 docker"
[ -f "$BACKEND_JAR" ] || die "后端 jar 不存在：$BACKEND_JAR（请先执行 mvn package）"
if [ "$NO_AI" = false ]; then
  [ -f "$SNAILAI_JAR" ] || die "Snail-AI jar 不存在：$SNAILAI_JAR"
fi

# ---------- 2. UFW 放行（防漏） ----------
log "检查 UFW 端口放行..."
if command -v ufw >/dev/null 2>&1 && sudo -n ufw status 2>/dev/null | grep -q "Status: active"; then
  for p in $UFW_PORTS; do
    if ! sudo -n ufw status 2>/dev/null | grep -qE "^$p/tcp"; then
      sudo -n ufw allow ${p}/tcp >/dev/null 2>&1 && log "  UFW 放行 $p" || warn "  UFW 放行 $p 失败（可能无 sudo 权限）"
    fi
  done
else
  warn "  UFW 未激活或无权限，跳过（若访问不到 redis/后端请手动放行 6379/8091/8082 等）"
fi

# ---------- 3. 启动 MySQL / Redis ----------
log "启动 MySQL / Redis 容器..."
docker start mysql redis >/dev/null 2>&1

# 等待 MySQL 就绪
log "等待 MySQL(3306) 就绪..."
for i in $(seq 1 30); do
  if docker exec mysql mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; then
    log "  MySQL 就绪"; break
  fi
  [ "$i" = 30 ] && die "MySQL 30s 内未就绪"
  sleep 1
done

# 等待 Redis 就绪（含密码）
log "等待 Redis(6379) 就绪..."
for i in $(seq 1 20); do
  if docker exec redis redis-cli -a ruoyi123 ping >/dev/null 2>&1; then
    log "  Redis 就绪"; break
  fi
  [ "$i" = 20 ] && die "Redis 20s 内未就绪"
  sleep 1
done

# ---------- 4. 清理旧进程 ----------
log "清理旧进程..."
for pat in "ruoyi-admin.jar" "ruoyi-snailai-server.jar" "plus-ui-6x.*vite"; do
  pkill -f "$pat" 2>/dev/null && log "  已停止 $pat" || true
done
sleep 2

# ---------- 5. 启动后端 ----------
log "启动后端($BACKEND_PORT)..."
cd "$OA_HOME/ruoyi-6x/ruoyi-admin"
nohup java -jar "$BACKEND_JAR" --server.port=$BACKEND_PORT > /tmp/ruoyi-6x-admin.log 2>&1 &
log "  后端 PID $!，日志 /tmp/ruoyi-6x-admin.log"

# 等待后端就绪（最多 90s，后端启动较慢）
log "等待后端就绪..."
for i in $(seq 1 90); do
  if curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$BACKEND_PORT/swagger-ui/index.html" 2>/dev/null | grep -q 200; then
    log "  后端就绪 $BACKEND_PORT"; break
  fi
  [ "$i" = 90 ] && warn "  后端 90s 未返回 200，请查看 /tmp/ruoyi-6x-admin.log"
  sleep 1
done

# ---------- 6. 启动 Snail-AI ----------
if [ "$NO_AI" = false ]; then
  log "启动 Snail-AI($SNAILAI_WEB_PORT)..."
  cd "$OA_HOME/ruoyi-6x/ruoyi-extend/ruoyi-snailai-server"
  nohup java -jar "$SNAILAI_JAR" --server.port=$SNAILAI_WEB_PORT > /tmp/ruoyi-snailai-server.log 2>&1 &
  log "  Snail-AI PID $!，日志 /tmp/ruoyi-snailai-server.log"
fi

# ---------- 7. 启动前端 ----------
if [ "$NO_FRONT" = false ]; then
  log "启动前端($FRONTEND_PORT)..."
  cd "$FRONTEND_DIR"
  nohup pnpm dev > /tmp/plus-ui-6x-dev.log 2>&1 &
  log "  前端 PID $!，日志 /tmp/plus-ui-6x-dev.log"
fi

# ---------- 8. 汇总 ----------
sleep 2
log "========== 启动汇总 =========="
for p in $BACKEND_PORT $FRONTEND_PORT $SNAILAI_WEB_PORT $SNAILAI_GRPC_PORT; do
  echo -n "  端口 $p : "
  ss -tln 2>/dev/null | grep -q ":$p " && echo "监听中" || echo "未监听"
done
echo ""
log "后端      http://127.0.0.1:$BACKEND_PORT"
[ "$NO_FRONT" = false ] && log "前端      http://localhost:$FRONTEND_PORT  (admin/admin123)"
[ "$NO_AI" = false ]    && log "Snail-AI  http://127.0.0.1:$SNAILAI_WEB_PORT/snail-ai"
