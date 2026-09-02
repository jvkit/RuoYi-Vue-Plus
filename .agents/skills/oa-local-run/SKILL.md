---
name: oa-local-run
version: 2.0.0
description: "OA 6x 本地开发环境：一键启停前后端、编译打包、日志查看、端口/代理配置、UFW 拦截排查、本地 API 冒烟。当用户要求启动/停止/重启本地项目、编译后端、排查本地端口连不上时使用。完整细节见本目录《启动与端口配置指南.md》。"
---

# OA 6x 本地启停与编译

> 完整文档在本目录《启动与端口配置指南.md》（与 `docs/6x启动与端口配置指南.md` 同源，需同步维护）。

## 端口总览

| 服务 | 端口 | 说明 |
|---|---|---|
| 6x 后端 | **8091** | `ruoyi-admin.jar` |
| 6x 前端 dev | **8082** | `pnpm dev`（代理 `/dev-api` → 8091） |
| Snail-AI | 8900 / gRPC 18888 | 可选 AI 服务 |
| agents 服务 | 8093 | 发票 AI 匹配（见 `oa-agents` skill） |
| Langfuse | 3000 | 自托管（见 `oa-agents` skill） |
| MySQL | 3306 | docker 容器 `mysql`，root/root，库 `ry-vue-6x` |
| Redis | 6379 | docker 容器 `redis`，密码 ruoyi123 |

注意：本地 80 被系统 nginx（OpenWebUI 反代）占用、8080 被 open-webui-feedback-owu 占用，别撞。

## 一键启停（日常用这个）

```bash
cd /home/jvkit/workspace/oa
bash scripts/start-6x.sh              # 全量（含前端、Snail-AI）
bash scripts/start-6x.sh --no-front   # 只起后端
bash scripts/stop-6x.sh               # 停后端/前端/Snail-AI（保留 MySQL/Redis 容器）
```

脚本自带：UFW 放行、容器等待就绪、旧进程清理、后端就绪等待。启动汇总里若某端口显示「未监听」，等 10~20 秒再 `ss -tlnp | grep <port>` 复查，仍不通再看日志。

## 编译

```bash
cd /home/jvkit/workspace/oa/ruoyi-6x
mvn package -DskipTests -pl ruoyi-admin -am    # 只编后端聚合模块，产物 ruoyi-admin/target/ruoyi-admin.jar
```

- 打包默认 skipTests；要跑测试用 `mvn test`。
- **编译报错先看是否跨模块依赖缺失**：procurement 模块用 system 模块类需在 `ruoyi-modules/ruoyi-procurement/pom.xml` 加 `ruoyi-system` 依赖（已有先例）。
- 前端检查：`pnpm lint`（oxlint，秒级）；完整类型检查 vue-tsc 有已知 TS7 告警噪音，可忽略。

## 热更新说明

- 前端 vite dev 热更新，改完即生效，**不需要 build**。
- 后端改 Java 必须重新 `mvn package` 并重启后端进程（无热加载）。
- 菜单/权限类改动（sys_menu/sys_role_menu）不重启也生效，但**前端需要重新登录**刷新路由缓存。

## 日志位置

| 日志 | 路径 |
|---|---|
| 后端 | `/tmp/ruoyi-6x-admin.log` |
| 前端 | `/tmp/plus-ui-6x-dev.log` |
| Snail-AI | `/tmp/ruoyi-snailai-server.log` |
| 历史 sys 日志 | `logs/sys-*.log` |

排查后端异常：`grep -n "ERROR\|Exception\|Caused by" /tmp/ruoyi-6x-admin.log | tail -30`

## 端口连不上排查顺序

1. `ss -tlnp | grep <port>` 确认在监听
2. `dmesg | grep UFW` 看是否被防火墙拦（本机 UFW 默认拒绝入站；start-6x.sh 已内置放行，手工启的服务要自己 `sudo ufw allow <port>/tcp`）
3. 看对应日志是否启动失败（常见：MySQL/Redis 没起、端口被占）

## 本地 API 冒烟（curl 直测后端）

登录接口是 `/auth/login`（不是 /login），必须带 clientId + grantType：

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8091/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","clientId":"e5cd7e4891bf95d1d19206ce24a7b32e","grantType":"password"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])")

curl -s http://127.0.0.1:8091/procurement/invoice/list?pageNum=1&pageSize=10 \
  -H "Authorization: Bearer $TOKEN" \
  -H "clientid: e5cd7e4891bf95d1d19206ce24a7b32e"
```

**必须同时带 `Authorization` 和 `clientid` 两个头**，缺 clientid 会 401。

## 常见非致命告警（可忽略）

- `UnknownHostException: net` — Spring Boot Admin 客户端注册失败
- `SnailJob gRPC 127.0.0.1:17888 连不上` — snailjob-server 未启动
- `snail-ai-heartbeat ... attempting reconnect` 偶发后自恢复
- vue-tsc 的 TS7 baseUrl 弃用告警
