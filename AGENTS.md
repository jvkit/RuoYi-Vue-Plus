# 工作区指引

本工作区是一个 **OA 协同办公系统**的学习、二次开发与扩展项目，基于 **RuoYi-Vue-Plus**（多租户中后台管理脚手架）搭建，在其上扩展了采购（SOP）流程与发票 AI 审核等业务。

本仓库当前以 **6.0.0** 为活跃开发/未来生产版本（后端 `ruoyi-6x` + 前端 `plus-ui-6x`）。原 5.6.2 版本（后端 `RuoYi-Vue-Plus` + 前端 `plus-ui`）已归档到 `archive/`，仅作历史回溯，不再运行。

阅读本文件的 AI 代理：下面的信息面向对本项目一无所知的读者，可按"总览 → 技术栈 → 目录 → 构建/运行 → 自定义模块 → Dify 集成 → 数据库 → 部署 → 脚本 → 文档 → 约定"的顺序理解。

---

## 1. 项目总览

| 项 | 说明 |
|---|---|
| 后端 | Spring Boot 3 + MyBatis-Plus + Sa-Token + Warm-Flow（多租户中后台） |
| 前端 | Vue 3 + TypeScript + Element Plus + Vite + Pinia + vxe-table |
| 业务扩展 | 发票管理（含 AI 识别/审核/查重）、采购管理（项目/供应商/BOM/申请/订单） |
| 当前开发版本 | 后端 `ruoyi-6x` 6.0.0 / 前端 `plus-ui-6x` 6.0.0 |
| 历史归档版本 | 后端 `RuoYi-Vue-Plus` 5.6.2 / 前端 `plus-ui` 5.6.2-2.6.2（已移入 `archive/`） |

## 2. 根目录结构速查

```
/home/jvkit/workspace/oa/
├── ruoyi-6x/                # 后端 6.0.0（当前开发/未来生产，已迁移采购模块 procurement）
├── plus-ui-6x/              # 前端 6.0.0（已迁移采购页面 procurement）
├── docs/                    # 教学文档 + 工作报告 + 框架梳理
├── scripts/                 # Python 辅助脚本（Dify 配置、发票图片生成、演示视频、启停脚本）
├── archive/                 # 历史归档（5.x 后端/前端、过时文档）
├── logs/                    # 后端/前端运行日志
├── temp/                    # 本地临时文件（不进入版本控制）
├── test-invoices/           # 发票识别测试图片（good/bad_invoice.png）
└── oa.code-workspace        # VSCode 多根工作区
```

注意：
- `ruoyi-6x` / `plus-ui-6x` 是同一外层 Git 仓库的两个 checkout（git worktree / 嵌套 .git），被外层 `.gitignore` 忽略。
- 原 5.x 版本（`RuoYi-Vue-Plus` / `plus-ui`）已移入 `archive/`，不再运行，仅供历史代码回溯。
- 新增辅助脚本、一次性工具脚本统一放到 `scripts/`，保持根目录整洁。

## 3. 技术栈

### 后端（ruoyi-6x 6.0.0，当前活跃版本）
- **Java 21**，Spring Boot **4.1.0**，Maven 多模块，groupId `org.dromara`，version `${revision}=6.0.0`
- 关键依赖版本：MyBatis-Plus 3.5.16、Sa-Token 1.45.0（认证/授权）、Redisson 3.52.0、dynamic-datasource 4.3.1、Warm-Flow 1.8.5（工作流）、MapStruct-Plus 1.5.0（BO/VO 映射）、SnailJob 1.10.0（分布式任务）
- Web 容器：Undertow；接口加密（AES+RSA，dev 环境关闭）；**已移除多租户**；数据权限、逻辑删除默认开启
- 打包默认 `skipTests=true`

### 前端（plus-ui-6x 6.0.0，当前活跃版本）
- Vue 3.5.30、TypeScript ~6.x、Vite 8（Rolldown）、Element Plus 2.14.3、vxe-table 4.18.1、Pinia 4、vue-router 5.0.3、axios 1.13.6、UnoCSS 66.6.6
- 包管理器 **pnpm**（有 `pnpm-lock.yaml`、`pnpm-workspace.yaml`）
- 开发端口 `VITE_APP_PORT=8082`，dev 代理 `/dev-api` → `http://127.0.0.1:8091`（后端）
- 传输加密：dev `VITE_APP_ENCRYPT=false`，生产 `true`（AES+RSA）

### 5.6.2 历史版本（已归档）
- 已移入 `archive/RuoYi-Vue-Plus-5x/` 与 `archive/plus-ui-5x/`，不再运行
- 原发票管理模块（`ruoyi-invoice`）仍在此版本中，后续如需迁移可参考

### 6.0.0 参考版差异（对比 5.6.2）
- 后端：Java **21**、Spring Boot **4.1.0**；`ruoyi-generator` 改名 `ruoyi-gen`；新增 `ruoyi-ai`、`ruoyi-common-mcp/mqtt/elasticsearch/liteflow/push/ai`、`ruoyi-extend/ruoyi-snailai-server`；**移除多租户**与 `ruoyi-invoice`；新增 `ruoyi-api` 模块做跨模块 API 契约。**采购扩展已迁移**（见 6.2 与 `docs/采购模块6X迁移报告.md`），发票未迁移。
- 前端：Vite 8（Rolldown）、TypeScript 6、Pinia 4、Element Plus 2.14.3；ESLint/Prettier 换成 **oxlint/oxfmt**；新增 `hooks/` 组合层；新增 `gen/`（FreeMarker 代码生成模板）。dev 代理默认指向 8080（本机后端实际跑在 8088，6x 未适配）。

## 4. 后端结构（ruoyi-6x）

### Maven 模块
- `ruoyi-admin`：web 入口，聚合所有业务模块，`mvn package` 产出可执行 jar `ruoyi-admin/target/ruoyi-admin.jar`；主类 `org.dromara.DromaraApplication`
- `ruoyi-common`：基础能力，`ruoyi-common-core/mybatis/redis/satoken/security/tenant/log/excel/translation/web/idempotent/ratelimiter/oss/mail/sms/sensitive/encrypt/json/sse/websocket/social/doc/bom/job` 等
- `ruoyi-modules`：业务模块
  - **stock（上游自带）**：`ruoyi-system`、`ruoyi-generator`、`ruoyi-job`、`ruoyi-demo`、`ruoyi-workflow`（Warm-Flow，示例 `TestLeave`）
  - **custom（本项目新增）**：`ruoyi-invoice`（发票）、`ruoyi-procurement`（采购，PMS 一套）
- `ruoyi-extend`：`ruoyi-monitor-admin`（Spring Boot Admin 服务端）、`ruoyi-snailjob-server`（任务调度服务端）

### 配置文件（`ruoyi-admin/src/main/resources/`）
- `application.yml`：主配置（端口、Sa-Token、租户、MyBatis-Plus、warm-flow、springdoc 分组）
- `application-dev.yml` / `application-prod.yml`：环境配置（数据源、Redis、**Dify**、邮件、短信）
- 激活 profile 由根 pom 的 `@profiles.active@` 注入，默认 `dev`

### 数据库访问约定
- ORM：MyBatis-Plus；Mapper 继承 `BaseMapperPlus<T, V>`（提供 `selectVoById/selectVoList/selectVoPage` 等）
- 实体继承 `org.dromara.common.mybatis.core.domain.BaseEntity`（含 create_by/time、update_by/time、del_flag、tenant_id）
- Mapper XML：`resources/mapper/<模块>/<Entity>Mapper.xml`；`@MapperScan("${mybatis-plus.mapperPackage}")`
- 主键 `idType: ASSIGN_ID`（雪花）；逻辑删除 `del_flag`；分页 `PageQuery` + `TableDataInfo`
- 插件顺序：租户 → 数据权限 → 分页 → 乐观锁；所有业务表带 `tenant_id`
- BO/VO 映射：MapStruct-Plus `MapstructUtils.convert`

### 安全
- 认证/授权：**Sa-Token**（`token-name: Authorization`，JWT），权限注解 `@SaCheckPermission("xxx:*")`
- 接口/响应加密 `api-decrypt`（dev 关闭）；XSS 过滤；验证码（dev 关闭）；登录限流（密码错 5 次锁 10 分钟）

## 5. 前端结构（plus-ui-6x）

```
src/
├── main.ts / App.vue / permission.ts   # 入口 + 路由守卫
├── api/                 # 接口层，按模块分目录（invoice/procurement/system/monitor/tool/workflow...）
├── components/          # 通用组件（Pagination/DictTag/RightToolbar/FileUpload...）
├── directive/           # 自定义指令（v-hasPermi 权限指令等）
├── hooks/  enums/  lang/  plugins/  types/  utils/
├── layout/              # 整体布局（侧边栏/顶栏/标签页）
├── router/index.ts      # 静态路由 + 动态路由容器
├── store/modules/       # Pinia（app/dict/notice/permission/settings/tagsView/user）
└── views/               # 页面（system/invoice/procurement/workflow/demo/...）
```

### 路由/菜单机制（后端驱动）
- 前端只有少量静态路由（`/login`、`/redirect`、404/401 等），业务路由由后端 **`sys_menu` 表**驱动
- `src/permission.ts` 登录后调 `usePermissionStore().generateRoutes()` → `GET /system/menu/getRouters` 拿菜单树 → `filterAsyncRouter()` 通过 `import.meta.glob` 扫描 `src/views/**/*.vue` 按路径映射组件
- **约定：后端菜单的 `component` 字段必须与 `src/views/` 下实际文件路径对应**，页面组件必须存在于 views 中

### HTTP 封装（`src/utils/request.ts`）
- `baseURL = VITE_APP_BASE_API`（dev `/dev-api`），`timeout: 50000`
- 请求拦截器：加 `Authorization: Bearer <token>`、`clientid` 头；GET 参数序列化；POST/PUT 防重复提交；可选 `isEncrypt` AES+RSA
- 响应拦截器：统一解包 `res.data`，处理 401/500/警告/成功
- **定制点**：对 `invoice/info/(extract|ai-review)` 的 500 **静默处理**（AI 服务不可用时不弹错，前端有兜底数据）

## 6. 自定义业务模块

### 6.1 发票管理（`ruoyi-invoice` + `src/views/invoice/`）
完整 CRUD + AI 集成示例，是本项目最完整的"从零新增业务模块"范例（教学文档 `02`、`18`）。

> **已迁移到 6x**（2026-08-13）：后端 `ruoyi-6x/ruoyi-modules/ruoyi-invoice` + 前端 `plus-ui-6x/src/views/invoice`（info/employee/usage）+ `src/api/invoice`；SQL 固化脚本 `ruoyi-6x/script/sql/invoice_6x.sql`（幂等：建表去租户 + 字典 1805/1806 段 + 菜单 1804xxx）。适配点：去 `ruoyi-common-tenant`、`@RepeatSubmit` 改 `ruoyi-common-redis`、`@TableLogic delFlag`、Excel 注解改 Fesod、`TableDataInfo`→`PageResult`、`request.ts` 已移植 AI 端点 500 静默。本段以下描述为 5.6.2 生产版。

后端：`ruoyi-modules/ruoyi-invoice/src/main/java/org/dromara/invoice/`
- 分层：`controller/ domain/{bo,vo} mapper/ service/{impl}`；业务对象 `InvoiceInfo`（发票信息）、`InvoiceUsageRecord`（使用记录）
- 表：`invoice_info`、`invoice_usage_record`；接口前缀 `/invoice/info`，权限 `invoice:info:*`
- Controller 能力：列表/导出/详情/增删改、`@SaCheckPermission`、`@Log`、`@RepeatSubmit`、AI 审核（`/ai-review/{id}`）、AI 字段识别（`/extract`）、查重（`/check-duplicate`）、真伪查验（`/verify/{id}`，mock：财务单号末位奇数=真、偶数=假）
- `DifyInvoiceReviewService.java`：AI 识别 + 审核核心，见第 7 节
- SQL 迁移：`src/main/resources/db/migration/V1__add_ai_opinion.sql`（为 `invoice_info` 加 `ai_opinion` TEXT 列）

前端页面（`src/views/invoice/`）：
- `info/index.vue`：发票信息 CRUD 列表（含 AI 审核意见、真伪状态、财务查询单号列）
- `employee/index.vue`：**员工端发票上传与 AI 识别**（三步向导：上传 → AI 识别填表 → 确认提交；含演示兜底逻辑）
- `usage/index.vue`：发票使用记录 CRUD

相关 SQL（`RuoYi-Vue-Plus/script/sql/`）：
- `invoice_table.sql`：`invoice_info` 建表
- `invoice_module.sql`：字典（invoice_type/invoice_status）+ 菜单/按钮权限（含员工发票提交菜单 `invoice/employee/index`）

### 6.2 采购管理（`ruoyi-procurement` + `src/views/procurement/`）
较新、较完整的一整套 PMS 采购模块（**注意与第 6.3 节旧版 workflow 采购并存**）。

后端：`ruoyi-modules/ruoyi-procurement/src/main/java/org/dromara/procurement/`
- 实体带 `Pms` 前缀：`PmsProject`、`PmsSupplier`、`PmsBomItem`、`PmsProcurementRequest(+Item)`、`PmsPurchaseOrder(+Item)`
- 表：`pms_project`、`pms_supplier`、`pms_bom_item`、`pms_procurement_request(_item)`、`pms_purchase_order(_item)`
- Controller 5 个，权限 `procurement:project:*`、`procurement:supplier:*`、`procurement:bom:*`、`procurement:request:*`、`procurement:order:*`
- Mapper XML：`resources/mapper/procurement/`（BomItem/ProcurementRequest/PurchaseOrder 有自定义 SQL）

前端页面（`src/views/procurement/`）：`request/`（采购申请，提交启动工作流）、`order/`（采购订单）、`project/`、`supplier/`、`bom/`

相关 SQL：`script/sql/procurement_init.sql`、`procurement_order_init.sql`（菜单 12000-12059 + 各表建表 + 流程定义）

### 6.3 工作流采购申请（旧版，`ruoyi-workflow` 内）
一套更早的采购申请，与第 6.2 节并存（历史演进结果）：
- 位置：`ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/{controller,domain/bo/vo,mapper,service/impl}/ProcurementRequest*`
- 表：`procurement_request`（注意与 `pms_procurement_request` 不同）；接口前缀 `/workflow/procurement`，权限 `workflow:procurement:*`
- 特点：仿 `TestLeave`，与 Warm-Flow 集成，有 `submitAndFlowStart` 提交并启动流程
- SQL：`script/sql/procurement_menu.sql`；前端对应 `views/procurement/request/index.vue`（旧）及 `views/workflow/procurement/index`

## 7. Dify AI 集成（发票）

发票模块通过外部 **Dify** 服务做 AI 识别与审核。核心代码当前在归档版本 `archive/RuoYi-Vue-Plus-5x/ruoyi-modules/ruoyi-invoice/.../service/DifyInvoiceReviewService.java`，后续迁移到 6.x 时可参考复用。

- **配置**（`application-dev.yml` 的 `--- # Dify AI配置` 段）：
  ```yaml
  dify:
    api-url: http://172.16.16.110:8090
    app-id: fc04cb5d-0d6c-42b1-8dfd-7c6755827d2d   # 注：代码里实际未使用 app-id
    api-key: app-22cb3e89ff524629af078ee3af77ac08
  ```
- **调用方式**（Hutool `HttpRequest`，非 Spring RestTemplate）：
  1. `POST {api-url}/v1/files/upload`（header `Authorization: Bearer {key}`）上传图片，取返回 `id` 作为 `upload_file_id`
  2. `POST {api-url}/v1/chat-messages`（`response_mode: blocking`，`user: invoice-system`，files 用 `type: image, transfer_method: local_file`），超时 120s
- **两类方法**：
  - `extractAndReviewFromImage(MultipartFile)`：上传图片 → 中文提示词要求返回严格 JSON（invoiceCode/invoiceNumber/invoiceType/amount/taxAmount/totalAmount/invoiceDate/sellerName/buyerName/passed/opinion）；解析时兼容 ```json 代码块、snake_case→驼峰归一化
  - `reviewInvoice(...)`：旧接口兼容；`passed` 判定较粗糙（不含"驳回/不通过/拒绝"即通过）
- 已知脆弱点：`Could not extract JSON from Dify answer: null` 曾出现，AI 服务不可用时应走前端兜底/默认通过
- 前端侧无 Dify 字符串，只通过 `src/api/invoice/info/index.ts` 暴露 `POST /invoice/info/extract`、`POST|GET /invoice/info/ai-review/{id}`、`GET /invoice/info/check-duplicate`、`POST /invoice/info/verify/{id}` 等

## 8. 数据库

- MySQL 容器：`mysql`，host 网络，端口 3306
- 数据库名：`ry-vue`；账号/密码：`root` / `root`
- Redis：`localhost:6379`，密码 `ruoyi123`（Redisson）
- 执行 SQL 方式：
  ```bash
  docker exec -i mysql mysql -uroot -proot -D ry-vue --default-character-set=utf8mb4 < xxx.sql
  ```
  **所有 SQL 脚本须加 `SET NAMES utf8mb4;`**，防止中文双重编码乱码。
- 已初始化脚本：`ry_vue_5.X.sql`（全量建库）、`ry_job.sql`（SnailJob）、`ry_workflow.sql`（工作流表）
- 自定义脚本见 6.1/6.2/6.3 节所列

## 9. 当前运行状态

| 服务 | 地址/位置 | 说明 |
|---|---|---|
| 前端（dev server） | `http://127.0.0.1:8082` | `plus-ui-6x` 的 `pnpm dev` |
| 后端 | `http://127.0.0.1:8091` | `ruoyi-6x`，库 `ry-vue-6x` |
| API 文档 | `http://127.0.0.1:8091/swagger-ui/index.html` | SpringDoc/OpenAPI |
| Spring Boot Admin | `http://127.0.0.1:9090` | 可选启动 |
| SnailJob | `http://127.0.0.1:8800` | 可选启动 |
| 手机端验收 | `http://<本机IP>:8082/mobile/acceptance` | 移动端专用验收页 |

默认登录账号：`admin` / `admin123`。
测试账号（见 `docs/发票AI审核与查重工作汇报.md`）：员工 `test_employee`，管理员 `admina`（租户 `721855`）。

## 10. 启动/停止命令

### 6x 一键启动
```bash
cd /home/jvkit/workspace/oa
bash scripts/start-6x.sh [--no-front] [--no-ai]
```
- 启动 MySQL/Redis 容器、放行 UFW、启动后端(8091) + 前端(8082)
- 日志：`/tmp/ruoyi-6x-admin.log`、`/tmp/plus-ui-6x-dev.log`
- 停止：`bash scripts/stop-6x.sh`

### 后端（ruoyi-6x）
```bash
cd /home/jvkit/workspace/oa/ruoyi-6x
mvn package -DskipTests -pl ruoyi-admin -am
cd ruoyi-admin
nohup java -jar target/ruoyi-admin.jar --server.port=8091 > /tmp/ruoyi-6x-admin.log 2>&1 &
```

### 前端（plus-ui-6x）
```bash
cd /home/jvkit/workspace/oa/plus-ui-6x
pnpm dev
```

### 服务器生产部署

服务器部署**必须先读** `docs/OA-6X-服务器部署指南.md`，并随该文档的更新同步调整部署步骤。

```bash
# 服务器一键部署（liyang 用户）
cd /home/liyang/jvkit/oa-workspace/ruoyi-6x
bash deploy.sh
```

部署约定：
- 任何数据库改动必须写成 `ruoyi-6x/script/sql/` 下的幂等 SQL 脚本，由 `apply-sql.sh` 统一应用。
- 前端生产构建配置（`plus-ui-6x/.env.production`）中的 `VITE_APP_CONTEXT_PATH`、`VITE_APP_BASE_API`、`VITE_APP_ENCRYPT` 必须与服务器 nginx 路径和后端 `api-decrypt` 配置保持一致。
- 部署后必须用 `curl` 或 Playwright 验证 `http://172.16.16.110/oa/` 可正常访问、登录、加载数据。
- 禁止手动在服务器数据库里改数据而不留脚本，否则本地与服务器会再次不同步。

## 11. 端口冲突处理

本机 80 端口可能被系统 nginx（OpenWebUI 反代）占用，8080 被 `open-webui-feedback-owu` 占用。

```bash
# 临时停止冲突服务（不删容器）
sudo systemctl stop nginx           # 系统 nginx，反向代理了 OpenWebUI
docker stop open-webui-feedback-owu # OpenWebUI 容器
docker start nginx-web              # 启动 RuoYi 前端容器

# 恢复 OpenWebUI
sudo systemctl start nginx
docker start open-webui-feedback-owu
```

## 12. scripts/ 辅助脚本

### Dify 配置脚本（在 Dify API 容器内运行，直接操作其 PostgreSQL）
这些脚本 `sys.path.insert(0, '/app/api')` 并 import Dify 内部模块，需在 Dify API 容器内执行。目标应用：`发票AI审核`。
- `install_openai_compatible.py`：安装 Dify 市场插件 `langgenius/openai_api_compatible`
- `add_oai_compat_models.py`：向 OpenAI 兼容 provider 注册模型（endpoint `https://chat.iphy.ac.cn/api/v1`，含硬编码 key，**仅服务器运维用**）
- `set_dify_model.py` / `configure_dify_app.py`：为 `发票AI审核` 应用设置模型（glm-5.2）与金融发票审核预置提示词
- `create_dify_app.py` / `create_dify_api_key.py` / `link_app_config.py` / `fix_dify_app_mode.py`：建应用/建 API key/绑定模型配置/改应用模式
- `list_compat_models.py` / `check_manifest.py` / `check_plugin_task.py` / `uninstall_openai.py`：查询/校验/清理
- `generate_invoice_images.py`：**纯本地**（用 `.venv-pptx` 里的 Pillow）生成 `test-invoices/bad_invoice.png`、`good_invoice.png`

### scripts/demo-video/（演示视频录制流水线）
- Node + Playwright，对开发前端录制发票/采购流程演示视频，通过 `page.route` **mock 后端**（不调用真实 AI）
- `rec_lib.js`（录制库）、`prepare.js`（预热+登录态）、`invoice_rec.js` / `purchase_rec.js`（录制）、`post_*.js`（ffmpeg 裁剪+ASS 字幕）、`verify_fb.js`（冒烟验证）
- 产物：`invoice.mp4`、`purchase.mp4`

### scripts/start-6x.sh / stop-6x.sh（6x 一键启停）
- `start-6x.sh`：放行 UFW 端口 → 启动 MySQL/Redis 容器并等待就绪 → 清理旧进程 → 启动后端(8091) → Snail-AI(8900) → 前端(8082)，均带就绪等待与日志；参数 `--no-front`/`--no-ai` 可跳过对应服务
- `stop-6x.sh`：停止 6x 后端/Snail-AI/前端 vite（MySQL/Redis 容器保留）
- **注意**：本机 UFW 默认拒绝入站，曾因漏放 6379 导致 Redis 连不上、后端启动失败——脚本会自动 `ufw allow` 所需端口；手工排查端口连不上时先看 `dmesg | grep UFW`
- 与 `docs/6x启动与端口配置指南.md` 配套，日常启停优先用这两个脚本

## 13. docs/ 文档

### 顶层报告
- `dify-experience.md`：Dify 部署与发票 AI 集成经验（Dify 1.0.0 @ `172.16.16.110:8090`、OpenAI 兼容插件、模型 glm-5.2/deepseek-v4-pro/qwen3.6、文件上传/JSON 输出经验）
- `发票AI审核与查重工作汇报.md`（2026-08-04）：发票 AI 审核与查重功能状态报告
- `RuoYi-6X-升级评估汇报.md`（2026-08-06）：5.6.2 → 6.0.0 升级评估，**结论：暂缓升级**，先在 5.6.2 上完成采购

### 教学文档（docs/教学/，01-19）
按序号命名，已发布文档**不二次修改**，需补充时新开一篇。01-19 覆盖：前端结构、如何新增完整业务模块、后端分层（实体-BO-VO-Mapper-Service-Controller）、权限/角色/用户/多租户、租户套餐、用户导入 Excel、字典、代码生成器、系统监控、AI 审批助手/病假 AI MVP 设计、后端项目结构详解（18）、Java-Maven 跨模块 import（19）等。

## 14. 测试策略

- **后端单元/集成测试**：默认 `skipTests=true`，日常 `mvn package` 不跑测试；如需执行去掉该属性或 `mvn test`
- **前端类型检查**：`vue-tsc`（`tsconfig.json` 中 `baseUrl` 有 TS7 弃用告警，属已知噪音，见 `temp/erro*.md`）
- **AI 功能**：无自动化测试，靠演示兜底 + 冒烟脚本（`scripts/demo-video/verify_fb.js`）+ 手工验证
- **演示视频**：`scripts/demo-video/` 用 Playwright 录屏并 mock 后端
- 发票测试图片在 `test-invoices/`

## 15. 常见非致命告警

后端日志中可能出现，不影响登录和常规接口：
1. `UnknownHostException: net` — Spring Boot Admin 客户端注册失败，本机主机名 `net` 解析失败
2. `SnailJob gRPC 连不上 127.0.0.1:17888` — `ruoyi-snailjob-server` 未启动
3. `Could not extract JSON from Dify answer: null` — Dify 返回非预期格式，AI 审核可能走兜底
如需完整监控和定时任务调度，再启动对应模块（monitor-admin / snailjob-server）。

4. **UFW 拦截导致连不上** — 本机 UFW 默认拒绝入站。若 MySQL/Redis/后端/前端端口"能监听但连不上/超时"，先查 `dmesg | grep UFW` 确认是否被拦，再用 `sudo ufw allow <port>/tcp` 放行（6379/3306/8091/8082/8900 等）。一键脚本 `scripts/start-6x.sh` 已内置放行。

## 16. 约定与注意事项

- 本项目前后端分离，前端路由/菜单由后端 `sys_menu` 表驱动，但**页面组件必须存在于前端 `plus-ui-6x/src/views` 中**，且 `component` 字段与文件路径一一对应
- 菜单类型：`M` 目录、`C` 菜单、`F` 按钮；外链：目录类型 + `是否外链=是` + 路由地址=http(s)://xxx
- 工作流基于 **Warm-Flow**，示例 `TestLeave` 是最直接参考
- **Warm-Flow 条件分支**：`skip_condition` 只在**网关节点**（`node_type=3` SERIAL / `4` PARALLEL / `5` INCLUSIVE）上生效，普通 `BETWEEN`（1）节点出边选择只取第一条 `PASS` 的 `flow_skip`，不判条件。`skip_condition` 格式：`<比较符>@@<流程变量名>|<字面值>`（如 `lt@@amount|1000`），支持 eq/ge/gt/le/lt/ne/like/not_like；比较符语义以 `MathUtil.determineSize(a,b)` 为准（`lt@@X|Y` 即 X<Y）。角色审批：`permission_flag` 写 `role:<角色id>`，引擎经 `WorkflowPermissionHandler.convertPermissions` → `fetchUsersByStorageIds` → `selectUsersByRoleIds` 自动展开为拥有该角色的用户列表
- 新增 Python 辅助脚本放 `scripts/`；Pillow/pptx 相关用 `.venv-pptx`（`.venv` 是空的）
- 教学文档放 `docs/教学/` 按序号命名，已发布不二次修改
- `ruoyi-6x` / `plus-ui-6x` 已迁移采购模块（见 `docs/采购模块6X迁移报告.md`），当前所有开发均在此版本进行；5.x 已归档，不再并行维护
- **SQL 一律固化为脚本**：任何数据库改动（建表、加列、字典、菜单、角色等）都必须写入 `script/sql/` 下的脚本，并保持**幂等可重复执行**（加列用 `information_schema` 判断是否已存在、增删类先删后插、更新用 `UPDATE`），禁止只在库里手工改而不留脚本，否则换环境/上生产无法复现
- 6x 采购 SQL 脚本：`ruoyi-6x/script/sql/procurement_6x.sql`（基线：建表 + 菜单 + 字典 + 流程定义）；`procurement_6x_increment.sql`（增量，幂等：`pms_procurement_request` 加 `title_type/title_name` 列、采购类型字典改为材料优先、采购菜单图标纠正为 `ep:` 前缀、新增「采购专员」角色）；`procurement_6x_category.sql`（增量，幂等：`flow_category` 加「采购>采购申请」分类、`pms_request` 流程定义 `category/form_path` 关联、注册采购申请审批详情隐藏菜单 1801046）；`procurement_6x_v3.sql`（v3：4 角色——团队上级 team_leader / 部门上级 dept_leader / 验收对接人 acceptance_contact / 仓库管理员 warehouse_admin，3 账号——王建龙 wangjianlong / 李迪 lidi / 裴天姿 peitianzi，密码 666666，角色绑采购菜单 1801000~1801999）；`procurement_6x_v3_projects.sql`（v3 项目初始化：长三角/天目湖项目树，按名称查重幂等，复用已有旧项目 id）；`procurement_6x_v3_flow.sql`（v3 流程改造：`pms_request` 改为 `start→apply→leader→gateway_amount(排他网关,node_type=3)→` 按金额 `lt@@amount|1000` 走团队上级 / `ge@@amount|1000` 走部门上级（`role:<角色id>` 角色审批）→`end`，移除旧 contact 节点与 contactId 硬编码；**注意 warm-flow 条件分支必须走 SERIAL 网关节点，普通节点出边只取第一条 PASS skip**）；`procurement_6x_v3_common.sql`（v3 公共角色：`common_user` 角色绑「我的任务」树菜单 17614xxx，王建龙/李迪/裴天姿绑 common_user，裴天姿加绑 warehouse_admin，warehouse_admin 绑采购菜单 1801000~1801999，项目资金按层级一级 100 万 / 二级 10 万无条件更新）；`procurement_6x_v3_acceptance_flow.sql`（v3 验收流程：`pms_acceptance` 定义 start→apply(验收发起人,${initiator})→applicant(采购申请人,${applicantId} 动态取采购申请 create_by)→leader(项目负责人,${leaderId})→team_leader(role:团队上级)→end，pms_acceptance 加 process_instance_id 列）；`procurement_6x_v3_acceptance_menu.sql`（验收提交按钮 1801066 + 验收审批详情隐藏菜单 1801067 + 角色绑定 + 验收流程 category 关联采购分类）；`procurement_6x_v3_issue_flow.sql`（v3 领用流程：`pms_issue_request` 定义 start→apply(发起人,${initiator})→warehouse(仓库管理员,role:warehouse_admin)→end，pms_issue_request 加 process_instance_id 列，领用提交按钮 1801086 + 领用审批详情隐藏菜单 1801087 + 角色绑定）；`procurement_6x_v3_acceptance_flag.sql`（v3 验收标志：`pms_procurement_request` 加 `acceptance_status` 列 none未验收/processing验收中/done已完成验收，存量回填——已有验收单置 processing、验收 finished 置 done；验收新建置 processing、流程 finish 置 done、删除唯一验收单恢复 none；采购验收页「关联采购申请」下拉走 `/procurement/request/acceptableList`（只列验收标志 none/null 的已完成申请），同一采购申请只能验收一次）
- **「我的任务」办理/查看跳转约定**：待办/已办点「办理/查看」→ `workflowCommon.routerJump` → `router.push(formPath)`；formPath 取 `flow_definition.form_path`（非节点），需配业务详情页路由（如 `/procurement/request/detail`），且该详情页必须：① 存在前端组件 `src/views/procurement/request/detail.vue`；② 挂一条隐藏菜单（父为**目录 M**、path 带子路径如 `request/detail`，目录才会递归生成路由）；③ 页面接收 `type(approval/view)&taskId&id` 路由参数，用 `approvalButton`+`submitVerify` 完成审批
- 6x 发票 SQL 脚本：`ruoyi-6x/script/sql/invoice_6x.sql`（幂等：发票两表建表去租户、字典 invoice_type/invoice_status、菜单 1804xxx 含发票信息/发票提交/发票使用记录）
- **服务器部署补全检查清单**：部署到新环境或服务器后，除了执行基线脚本，还需核对本地开发库与服务器库的差异，常见漏项：
  - 发票管理模块：`invoice_6x.sql`
  - PRIME AI 外链菜单：`procurement_6x_server_patch.sql` 中插入菜单 1805000，外链 `http://172.16.16.110:3305`
  - 菜单名同步：如"采购项目"→"项目管理"，以及采购订单隐藏（visible=1）
  - 测试/临时菜单：如 `test` 菜单是否需同步到生产（通常生产可移除）
  - 每次补差异都应写幂等 SQL 脚本并提交，禁止只在库里手工改

## 17. 学习建议路线

1. 登录与用户/角色/权限管理
2. 菜单管理、字典、参数、部门、岗位
3. 工作流模块：流程定义、请假示例、我的任务
4. 代码生成器：导入表 → 预览 → 生成 zip
5. 前后端代码结构梳理（文档 01/03/05/18/19）
6. 发票模块（作为完整 CRUD + AI 集成范例，文档 02）
7. 采购/SOP 流程二次开发
