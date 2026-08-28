# OA 6x 服务器部署指南

> 适用范围：基于 RuoYi-Vue-Plus 6.0.0 的 OA 采购系统，服务器地址 `172.16.16.110`，访问路径 `http://172.16.16.110/oa`。

---

## 0. 部署前必读（维护约定）

1. **本指南是服务器部署的唯一依据**。每次部署前必须通读当前版本，确认配置项与本次改动匹配。
2. **所有数据库改动必须固化为 SQL 脚本**，放在 `ruoyi-6x/script/sql/`，并保持幂等。禁止直接在生产/服务器库手工改数据。
3. **本地与服务器数据库同步方式**：
   - 日常增量：通过 `apply-sql.sh` 应用同一份脚本。
   - 首次基线或重大重构：可整库 dump/restore，但之后必须重置 `applied-sql.log` 并回归增量脚本管理。
4. **前端生产配置三件套必须与服务器路径对齐**：
   - `VITE_APP_CONTEXT_PATH = '/oa/'`
   - `VITE_APP_BASE_API = '/oa/prod-api'`
   - `VITE_APP_ENCRYPT = false`
5. **部署后必须验证**：首页 200、登录成功、至少一个列表页加载出数据。

---

## 一、总体架构

| 组件 | 位置 | 端口/路径 | 说明 |
|---|---|---|---|
| 前端静态资源 | `/var/www/oa/` | `http://172.16.16.110/oa/` | nginx 直接服务 |
| 后端 jar | `/home/liyang/jvkit/oa/backend/ruoyi-admin.jar` | `127.0.0.1:8092` | Java 进程 |
| nginx 配置 | `/etc/nginx/sites-available/default` | 80 → `/oa/` | 手动维护 |
| 源码 | `/home/liyang/jvkit/oa-workspace/` | — | `ruoyi-6x` + `plus-ui-6x` |
| 运行目录 | `/home/liyang/jvkit/oa/` | — | dist、jar、logs |
| MySQL | docker 容器 `ruoyi-mysql` | `127.0.0.1:3306/ry-vue-6x` | 与本地开发库同名 |
| Redis | docker 容器 `ruoyi-redis` | `127.0.0.1:6379` | 密码 `ruoyi123` |

---

## 二、目录约定

```
/home/liyang/jvkit/
├── oa-workspace/           # 源码目录（git 管理）
│   ├── ruoyi-6x/          # 后端源码，分支 6.X-procurement
│   │   ├── deploy.sh      # 一键部署脚本（见第七节）
│   │   └── script/sql/    # 幂等 SQL 脚本
│   └── plus-ui-6x/        # 前端源码，分支 6.X-Vue
│
└── oa/                    # 运行目录（部署产物）
    ├── backend/           # ruoyi-admin.jar
    ├── dist/              # 前端构建产物（base=/oa/）
    ├── logs/              # oa-backend.log
    └── sql/
        └── applied-sql.log  # 已执行 SQL 记录
```

**原则**：源码与运行目录分离，部署时从 workspace 构建并复制到 oa。

---

## 三、端口一览

| 服务 | 端口 | 修改位置 |
|---|---|---|
| nginx | 80 | 系统服务，一般不动 |
| OA 后端 | 8092 | `deploy.sh` 中 `BACKEND_PORT` |
| MySQL | 3306 | docker run 参数 |
| Redis | 6379 | docker run 参数 |

如果需要换后端端口：
1. 修改 `ruoyi-6x/deploy.sh` 中的 `BACKEND_PORT`。
2. 修改 `/etc/nginx/sites-available/default` 中 `proxy_pass http://127.0.0.1:8092/;` 的端口。
3. 重载 nginx：`sudo nginx -s reload`。

---

## 四、启动/停止

### 一键部署（推荐）

```bash
ssh c
su - liyang
cd /home/liyang/jvkit/oa-workspace/ruoyi-6x
bash deploy.sh
```

脚本会完成：git pull → 后端构建 → 前端构建 → SQL 增量 → 替换 jar/dist → 重启后端。

### 手动启动后端

```bash
su - liyang
cd /home/liyang/jvkit/oa
nohup java -jar backend/ruoyi-admin.jar \
  --server.port=8092 \
  --spring.profiles.active=prod \
  --spring.datasource.dynamic.datasource.master.url="jdbc:mysql://127.0.0.1:3306/ry-vue-6x?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true" \
  --spring.datasource.dynamic.datasource.master.username=root \
  --spring.datasource.dynamic.datasource.master.password=ruoyi123 \
  --spring.data.redis.host=127.0.0.1 \
  --spring.data.redis.password=ruoyi123 \
  --spring.data.redis.port=6379 \
  > logs/oa-backend.log 2>&1 &
```

### 手动停止后端

```bash
ps -ef | grep ruoyi-admin.jar | grep -v grep
kill <PID>
```

### 启动 MySQL/Redis

```bash
docker start mysql redis
```

---

## 五、验证访问

```bash
# 前端
curl -sI http://172.16.16.110/oa/
# 期望 HTTP/1.1 200 OK

# 后端（经 nginx）
curl -s http://172.16.16.110/oa/prod-api/
# 期望：欢迎使用RuoYi-Vue-Plus...

# 后端直连
curl -s http://127.0.0.1:8092/
# 期望同上
```

---

## 六、SQL 增量管理

所有数据库改动必须写成幂等 SQL 脚本，放在 `ruoyi-6x/script/sql/`。

执行由 `apply-sql.sh` 统一处理：

```bash
bash /home/liyang/jvkit/oa-workspace/ruoyi-6x/script/sql/apply-sql.sh
```

机制：
- 读取 `/home/liyang/jvkit/oa/sql/applied-sql.log` 中已执行的脚本名。
- 只执行新增脚本。
- 脚本本身必须幂等（`IF EXISTS`、先删后插、`information_schema` 判断等）。

---

## 七、deploy.sh 说明

位置：`ruoyi-6x/deploy.sh`

关键步骤：

1. `git pull` 前后端代码。
2. `mvn package -DskipTests -pl ruoyi-admin -am` 构建后端。
3. `pnpm install` + `pnpm build:prod` 构建前端。
4. `apply-sql.sh` 执行 SQL 增量。
5. 停止旧后端进程。
6. 复制 jar 到 `~/jvkit/oa/backend/`。
7. 复制 dist 到 `~/jvkit/oa/dist/`。
8. **同步 dist 到 `/var/www/oa/`**（nginx 实际服务目录）。
9. 启动后端。
10. 尝试 `nginx -s reload`（见第八节）。
11. 冒烟验证。

---

## 八、踩坑与注意事项

### 8.1 前端 base 路径与 API 路径

生产构建时，Vite 的 `base` 决定资源引用路径。

- 文件：`plus-ui-6x/.env.production`
- 必须设置：
  - `VITE_APP_CONTEXT_PATH = '/oa/'`
  - `VITE_APP_BASE_API = '/oa/prod-api'`（与子路径一致的绝对路径）
  - `VITE_APP_ENCRYPT = false`（与后端 `api-decrypt.enabled=false` 保持一致）

**坑 1**：如果 `VITE_APP_CONTEXT_PATH` 保持默认 `/`，部署到 `/oa/` 子目录后，浏览器加载 `/assets/xxx.js` 会 404，页面白屏。

**坑 2**：如果 `VITE_APP_BASE_API = '/prod-api'`（绝对路径），前端会请求 `http://172.16.16.110/prod-api/xxx`，而 nginx 只代理了 `/oa/prod-api/`，导致所有 API 404，登录失败。

**坑 3**：如果 `VITE_APP_BASE_API = 'prod-api'`（相对路径），在 `/procurement/request` 页面会变成 `/procurement/prod-api/xxx`，列表接口 401/404，页面一直 loading。

**坑 4**：如果 `VITE_APP_ENCRYPT = true` 但后端 `api-decrypt.enabled = false`，登录接口 500，提示"发生未知异常"。

### 8.2 `/var/www/oa/` 的权限

nginx worker 以 `www-data` 运行，需要能读取 `/var/www/oa/` 中的文件。

- `/var/www/` 目录本身属主是 `root:root`，部署脚本**不能**删除 `/var/www/oa/` 这个目录本身。
- 当前方案：只清空目录内的内容，再复制新文件。
- `/var/www/oa/` 已改为 `liyang:liyang` 所有，确保脚本可无 sudo 写入。

如果需要重建权限：

```bash
sudo chown -R liyang:liyang /var/www/oa
sudo chmod -R 755 /var/www/oa
```

### 8.3 nginx reload 需要 sudo

`liyang` 用户默认没有 passwordless sudo，所以 `deploy.sh` 里的 nginx reload 会失败。

**当前处理**：脚本会尝试 reload，失败后仅给出警告，不阻塞部署。

**什么时候需要手动 reload**：修改了 `/etc/nginx/sites-available/default` 等 nginx 配置时。

```bash
ssh c
su - liyang
echo "password" | sudo -S nginx -s reload
```

或让有 root 权限的人执行：

```bash
sudo nginx -s reload
```

### 8.4 部署瞬间的 403/白屏

`deploy.sh` 在同步 `/var/www/oa/` 时会先清空再复制。如果用户恰好在这一瞬间访问，可能看到 403 或白屏。

**建议**：在低峰期执行部署，或未来改为 rsync 原地覆盖减少空窗期。

### 8.5 SSE / resource/message 超时

浏览器登录后会建立 SSE 长连接 `/oa/prod-api/resource/message`，nginx error log 里可能出现 upstream timed out。这是长连接特性，不影响业务，可忽略。如要消除，可在 nginx 中给该路径加更长的 proxy_read_timeout。

### 8.6 首次部署数据库

首次部署时需要把本地 `ry-vue-6x` 库整体迁移到服务器：

```bash
# 本地导出
mysqldump -uroot -proot -B ry-vue-6x > ry-vue-6x.sql

# 服务器导入
docker exec -i mysql mysql -uroot -proot < ry-vue-6x.sql
```

---

## 九、Playwright 全链路测试

测试项目位于 `/home/jvkit/workspace/work_twst`（服务器上暂无，需在本地运行后访问服务器地址）。

测试入口目标：`http://172.16.16.110/oa/procurement/request`

建议覆盖：

1. admin 登录并创建项目。
2. 普通用户提交采购申请。
3. 项目负责人/团队上级/部门上级审批（根据金额分支）。
4. 验收人发起验收、上传图片。
5. 仓库管理员审批领用申请。
6. 检查页面无 404、无"无权访问"弹窗。

---

## 十、源码与脚本速查

| 文件 | 作用 |
|---|---|
| `ruoyi-6x/deploy.sh` | 服务器一键部署 |
| `ruoyi-6x/script/sql/apply-sql.sh` | 幂等 SQL 增量执行 |
| `plus-ui-6x/.env.production` | 生产环境 base 路径、API 前缀、RSA 密钥 |
| `ruoyi-6x/ruoyi-admin/src/main/resources/application-prod.yml` | 后端生产配置 |
| `/etc/nginx/sites-available/default` | nginx `/oa/` 路由配置 |
