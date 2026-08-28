# OA 6x 部署后续问题跟踪

> 当前阶段：服务器已与本地数据库同步，核心页面可正常加载数据，Playwright 全链路测试通过。
> 以下问题尚未彻底解决或需要后续验证，先记录在此，避免与主流程混淆。

---

## 1. PRIME AI 菜单需要实际上线验证

- 已修改 `procurement_6x_server_patch.sql`：`menu_id=1805000`，`is_frame='Y'`，`visible='1'`，`status='0'`。
- 已同步到本地和服务器数据库。
- **待验证**：
  - 登录后侧边栏是否出现"PRIME AI"菜单。
  - 点击后是否以内嵌 iframe 方式在 RuoYi 布局内打开 `http://172.16.16.110:3305`。
  - iframe 内 PRIME AI 页面是否能正常加载（可能涉及 X-Frame-Options / CSP 限制）。

## 2. nginx reload 仍需 sudo

- `deploy.sh` 中 `nginx -s reload` 会失败，因为 `liyang` 没有 passwordless sudo。
- 当前 nginx 配置未改动，不影响访问。
- **待处理**：
  - 方案 A：给 `liyang` 配置 `sudo nginx -s reload` 的 passwordless 权限。
  - 方案 B：改 nginx 配置时由有 root 权限的人手动 reload。

## 3. 部署后浏览器缓存问题

- 前端更新后，浏览器可能缓存旧的 `index.html` 或 assets。
- 用户反馈"打不开"可能是缓存了旧的错误版本。
- **待处理**：
  - 在 nginx 配置中为 `/oa/` 增加 no-cache 头，或在前端 build 时加版本号/hash。
  - 部署后建议用户强制刷新（Ctrl+F5 或 Cmd+Shift+R）。

## 4. Playwright 测试需要覆盖数据加载检查

- 当前 `full-flow-test.mjs` 主要检查页面元素和流程是否跑通。
- 之前出现过列表页面 loading 转圈但测试仍"通过"的情况。
- **待处理**：
  - 在关键页面（采购申请、采购验收、仓库库存）增加等待表格数据行出现的断言。
  - 增加对 API 返回状态码和 `total` 字段的检查。

## 5. 数据库同步机制需要制度化

- 目前靠手动 dump/restore 保持一致，风险高。
- **待处理**：
  - 以后所有数据库改动必须写成幂等 SQL 脚本。
  - 本地和服务器都通过 `apply-sql.sh` 执行同一份脚本。
  - 不再直接手工改库。
  - 全量同步只在必要时进行，且要同步更新 `applied-sql.log`。

## 6. 测试账号数据需要清理

- `proc_applier`、`proc_leader`、`proc_contact`、`proc_keeper` 等测试账号用于 Playwright 测试。
- **待处理**：
  - 确认这些账号是否要保留在生产环境。
  - 定期清理 Playwright 全链路测试产生的垃圾数据。

## 7. 服务器 MySQL/Redis 容器命名

- MySQL 容器名为 `ruoyi-mysql`，Redis 容器名为 `ruoyi-redis`。
- `deploy.sh` 和 `apply-sql.sh` 已适配。
- **待处理**：文档化容器名，避免以后混淆。
