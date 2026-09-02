---
name: oa-deploy
version: 2.0.0
description: "OA 6x 服务器部署：一键部署到 172.16.16.110 /oa/、SQL 增量应用、部署后验证、踩坑排查。当用户要求部署/更新服务器、同步服务器版本、排查服务器访问问题时使用。部署前必须先读本 skill 的《服务器部署指南.md》。"
---

# OA 6x 服务器部署

> **铁律：部署前必须先读本目录下《服务器部署指南.md》**（与 `docs/OA-6X-服务器部署指南.md` 同源的完整副本），并随改动同步维护两份。
> 已知未决问题清单见《部署后续问题跟踪.md》。

## 核心信息速查

| 项 | 值 |
|---|---|
| 服务器 | `172.16.16.110`，SSH 别名 `ssh c`，登录后 `su - liyang`（密码 test2good） |
| 访问入口 | `http://172.16.16.110/oa/` |
| 源码目录 | `/home/liyang/jvkit/oa-workspace/`（git 管理，分支 ruoyi-6x: `6.X-procurement` / plus-ui-6x: `6.X-Vue`） |
| 运行目录 | `/home/liyang/jvkit/oa/`（dist、jar、logs、sql/applied-sql.log） |
| 后端端口 | `8092`（注意：本地开发是 8091，服务器是 8092） |
| MySQL | docker 容器 `ruoyi-mysql`，库 `ry-vue-6x`，root/ruoyi123 |
| Redis | docker 容器 `ruoyi-redis`，密码 `ruoyi123` |
| nginx | `/etc/nginx/sites-available/default`，80 → `/oa/` |

## 部署流程（标准链路）

```bash
ssh c
su - liyang
cd /home/liyang/jvkit/oa-workspace/ruoyi-6x
bash deploy.sh
```

`deploy.sh` 自动完成：git pull → 后端构建（`./mvnw`，服务器无系统 mvn）→ 前端构建（`corepack pnpm build:prod`）→ `apply-sql.sh` SQL 增量 → 替换 jar/dist → chmod 755 → 重启后端 → 冒烟验证。

**耗时参考**（有缓存时）：后端构建 30~90s、前端 10~20s、启动 15~25s，全链路约 2~4 分钟。

## SQL 增量机制

- 所有数据库改动必须是 `ruoyi-6x/script/sql/` 下**幂等脚本**（服务器上同一份）。
- 服务器执行 `bash /home/liyang/jvkit/oa-workspace/ruoyi-6x/script/sql/apply-sql.sh`：
  - 读 `/home/liyang/jvkit/oa/sql/applied-sql.log`，只跑未记录的脚本。
  - **重要**：整库 dump/restore 后必须重置 `applied-sql.log` 并回归增量管理。
- 本地库与服务器库的同步靠同一份脚本，**禁止手工改库不留脚本**。

## 部署后验证清单（必须逐项做，不能只看脚本 exit 0）

```bash
curl -sI http://172.16.16.110/oa/                                    # 期望 200
curl -s http://172.16.16.110/oa/prod-api/                            # 期望 欢迎使用RuoYi-Vue-Plus
curl -s http://127.0.0.1:8092/                                       # 后端直连（服务器上执行）
```

然后用 Playwright 或浏览器验证：登录成功 → 采购申请列表**加载出数据**（不是转圈）→ 至少一个审批页可打开。全链路测试见 `oa-test-e2e` skill。

## 高频踩坑（按出现频率排序）

1. **前端三件套**（`.env.production`）：`VITE_APP_CONTEXT_PATH=/oa/`、`VITE_APP_BASE_API=/oa/prod-api`（绝对路径！相对或缺失都会 404/白屏）、`VITE_APP_ENCRYPT=false`（与后端 api-decrypt 配置一致，否则登录 500）。
2. **dist 权限**：nginx worker 是 www-data，`/home/liyang/jvkit/oa/dist` 及各级父目录需 755，否则 500 Permission denied。deploy.sh 会自动 chmod，手动复制后要补。
3. **nginx reload 要 sudo**：liyang 无免密 sudo，deploy.sh 里 reload 失败仅告警。改了 nginx 配置需手动：`echo "password" | sudo -S nginx -s reload`。
4. **菜单/权限漏同步**：部署后新页面 404 或「无权访问」，八成是 SQL 脚本没跑到或没写（见 `oa-menu-perms` skill）。
5. **浏览器缓存**：前端更新后用户可能加载旧 index.html，部署后提示强制刷新（Ctrl+F5）。
6. **SSE 超时**：`/resource/message` 长连接在 nginx error log 报 upstream timed out，属正常，可忽略。

## 服务器与本地的差异对照

| 项 | 本地 | 服务器 |
|---|---|---|
| 后端端口 | 8091 | 8092 |
| 启动方式 | `scripts/start-6x.sh` | `deploy.sh` / 手动 nohup |
| MySQL 容器名 | `mysql` | `ruoyi-mysql` |
| agents 服务 | `uvicorn :8093` | 服务器暂未部署 agents（发票 AI 匹配在服务器不可用，属已知限制） |
| 数据库 | `ry-vue-6x` root/root | `ry-vue-6x` root/ruoyi123 |

## 检查清单（每次部署走一遍）

- [ ] 通读《服务器部署指南.md》当前版本，确认配置匹配
- [ ] 本地改动已 commit + push（deploy.sh 靠 git pull 拿代码）
- [ ] 新 SQL 脚本已放进 `ruoyi-6x/script/sql/` 且幂等
- [ ] `.env.production` 三件套未被误改
- [ ] 部署后 curl 三连通过
- [ ] Playwright 冒烟：登录 + 列表页有数据
- [ ] 发现新问题 → 回写《部署后续问题跟踪.md》和指南
