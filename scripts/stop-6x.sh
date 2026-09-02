#!/bin/bash
# ============================================================
# 6x 项目一键停止脚本
# 停止：后端 + Snail-AI + 前端 vite（MySQL/Redis 容器保持运行）
# 用法：scripts/stop-6x.sh
# ============================================================
set -u
log()  { echo -e "\033[1;36m[stop]\033[0m $*"; }

log "停止 6x 服务进程..."
for pat in "ruoyi-admin.jar" "ruoyi-snailai-server.jar" "plus-ui-6x.*vite"; do
  pkill -f "$pat" 2>/dev/null && log "  已停止 $pat" || log "  $pat 未运行"
done
sleep 2

log "校验残留进程..."
REMAIN=$(pgrep -af "ruoyi-admin.jar|ruoyi-snailai-server.jar|plus-ui-6x.*vite" 2>/dev/null | grep -v grep || true)
if [ -n "$REMAIN" ]; then
  warn() { echo -e "\033[1;33m[warn]\033[0m $*"; }
  warn "以下进程仍在运行（2s 后强杀）："
  echo "$REMAIN"
  sleep 2
  for pat in "ruoyi-admin.jar" "ruoyi-snailai-server.jar" "plus-ui-6x.*vite"; do
    pkill -9 -f "$pat" 2>/dev/null || true
  done
  log "已强杀残留进程"
else
  log "全部已停止"
fi

log "提示：MySQL/Redis 容器仍在运行（如需停止：docker stop mysql redis）"
