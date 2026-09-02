#!/bin/bash
# ============================================================
# OA 6x 服务器部署脚本
# 在服务器 172.16.16.110 上执行（或本地打包后上传执行）
# 作用：部署前端 dist + 后端 jar，配置 nginx /oa 路径
# ============================================================
set -euo pipefail

# ---------- 配置项 ----------
OA_HOME="/home/jvkit/workspace/oa"
NGINX_HTML="/docker/nginx/html"
NGINX_CONF="/docker/nginx/conf/nginx.conf"
BACKEND_PORT=8091
BACKEND_LOG="/home/jvkit/workspace/oa/logs/oa-backend.log"

log()  { echo -e "\033[1;36m[deploy]\033[0m $*"; }
warn() { echo -e "\033[1;33m[warn]\033[0m $*"; }
die()  { echo -e "\033[1;31m[error]\033[0m $*"; exit 1; }

# ---------- 前置检查 ----------
[ -f "$OA_HOME/ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar" ] || die "后端 jar 不存在：$OA_HOME/ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar"
[ -d "$OA_HOME/plus-ui-6x/dist" ] || die "前端 dist 不存在：$OA_HOME/plus-ui-6x/dist"
[ -d "$NGINX_HTML" ] || die "nginx html 目录不存在：$NGINX_HTML"
[ -f "$NGINX_CONF" ] || die "nginx 配置文件不存在：$NGINX_CONF"

# ---------- 停止旧服务 ----------
log "停止旧 OA 后端进程（端口 $BACKEND_PORT）..."
OLD_PID=$(ss -tlnp 2>/dev/null | grep ":$BACKEND_PORT " | grep -oP 'pid=\K[0-9]+' | head -1)
if [ -n "$OLD_PID" ]; then
    kill "$OLD_PID" || true
    sleep 2
fi

# ---------- 部署前端 ----------
log "部署前端 dist 到 $NGINX_HTML/oa ..."
rm -rf "$NGINX_HTML/oa"
mkdir -p "$NGINX_HTML/oa"
cp -r "$OA_HOME/plus-ui-6x/dist/"* "$NGINX_HTML/oa/"

# ---------- 部署后端 ----------
log "启动 OA 后端（端口 $BACKEND_PORT）..."
mkdir -p "$(dirname "$BACKEND_LOG")"
nohup java -jar "$OA_HOME/ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar" \
    --server.port=$BACKEND_PORT \
    --spring.profiles.active=prod \
    > "$BACKEND_LOG" 2>&1 &

# 等待后端就绪
log "等待后端 $BACKEND_PORT 就绪..."
for i in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:$BACKEND_PORT/" >/dev/null 2>&1; then
        log "后端已就绪"; break
    fi
    [ "$i" = 60 ] && die "后端 60s 内未就绪，请检查 $BACKEND_LOG"
    sleep 1
done

# ---------- 重载 nginx ----------
log "检查 nginx 配置并重载..."
nginx -t || die "nginx 配置测试失败"
nginx -s reload || systemctl reload nginx || die "nginx 重载失败"

log "部署完成：http://172.16.16.110/oa"
log "后端日志：$BACKEND_LOG"
