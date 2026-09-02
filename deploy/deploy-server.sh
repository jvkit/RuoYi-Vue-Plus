#!/bin/bash
# ============================================================
# OA 6x 服务器部署脚本（在 172.16.16.110 上执行）
# 目标：部署到 http://172.16.16.110/oa，不影响其他项目
# ============================================================
set -euo pipefail

# 配置
OA_DIR="/home/liyang/jvkit/oa"
BACKEND_PORT=8092
BACKEND_LOG="$OA_DIR/logs/oa-backend.log"
NGINX_CONF="/etc/nginx/conf.d/oa.conf"

log()  { echo -e "\033[1;36m[deploy]\033[0m $*"; }
warn() { echo -e "\033[1;33m[warn]\033[0m $*"; }
die()  { echo -e "\033[1;31m[error]\033[0m $*"; exit 1; }

[ "$(whoami)" = "liyang" ] || die "请用 liyang 用户执行"

# ---------- 1. 目录准备 ----------
log "创建部署目录 $OA_DIR ..."
mkdir -p "$OA_DIR"/{backend,dist,logs,sql}

# ---------- 2. 停止旧 OA 后端（可选，若用 8091 则取消注释） ----------
# log "停止旧版 ruoyi-backend..."
# docker stop ruoyi-backend || true

# ---------- 3. 停止本服务旧进程 ----------
OLD_PID=$(ss -tlnp 2>/dev/null | grep ":$BACKEND_PORT " | grep -oP 'pid=\K[0-9]+' | head -1 || true)
if [ -n "$OLD_PID" ]; then
    log "停止旧进程 $OLD_PID（端口 $BACKEND_PORT）..."
    kill "$OLD_PID" || true
    sleep 2
fi

# ---------- 4. 部署前端 ----------
log "部署前端 dist..."
rm -rf "$OA_DIR/dist"
cp -r "$OA_DIR/dist-tmp" "$OA_DIR/dist"

# ---------- 5. 启动后端 ----------
log "启动后端（端口 $BACKEND_PORT）..."
mkdir -p "$(dirname "$BACKEND_LOG")"
nohup java -jar "$OA_DIR/backend/ruoyi-admin.jar" \
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
log "等待后端 $BACKEND_PORT 就绪..."
for i in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:$BACKEND_PORT/" >/dev/null 2>&1; then
        log "后端已就绪"; break
    fi
    [ "$i" = 60 ] && die "后端 60s 内未就绪，请检查 $BACKEND_LOG"
    sleep 1
done

# ---------- 6. 配置 nginx ----------
log "写入 nginx 配置 $NGINX_CONF ..."
sudo tee "$NGINX_CONF" > /dev/null <<'EOF'
# OA 6x 生产部署
server {
    listen 80;
    server_name _;

    location ^~ /oa/ {
        alias /home/liyang/jvkit/oa/dist/;
        index index.html;
        try_files $uri $uri/ /oa/index.html;

        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot|otf)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    location ^~ /oa/prod-api/ {
        proxy_pass http://127.0.0.1:8092/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Prefix /oa;

        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF

log "测试并重载 nginx..."
sudo nginx -t || die "nginx 配置测试失败"
sudo nginx -s reload || die "nginx 重载失败"

log "部署完成：http://172.16.16.110/oa"
log "后端日志：$BACKEND_LOG"
