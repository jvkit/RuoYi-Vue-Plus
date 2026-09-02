# OA 6x 服务器部署说明

## 目标
将当前 6x 版本部署到 `http://172.16.16.110/oa`，通过 nginx 代理，不影响服务器上的其他项目。

## 已准备的产物
- 后端 jar：`ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar`
- 前端 dist：`plus-ui-6x/dist/`（已配置 `VITE_APP_CONTEXT_PATH=/oa/`）

## 服务器要求
- 已有 nginx 监听 80/443
- 后端端口使用 `8091`（与本地一致）
- 前端静态目录使用 `/docker/nginx/html/oa`（可改）

## 部署方式

### 方式 A：代码推送 + 服务器构建（推荐后续迭代）
1. 本地 commit + push 到 GitHub
2. 服务器 `git pull`
3. 服务器执行 `deploy-to-server.sh`

### 方式 B：本地打包 + 上传执行（当前快速部署）
1. 本地确保 jar/dist 已构建
2. 将以下文件上传到服务器对应目录：
   - `ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar` → `/home/jvkit/workspace/oa/ruoyi-6x/ruoyi-admin/target/ruoyi-admin.jar`
   - `plus-ui-6x/dist/` → `/home/jvkit/workspace/oa/plus-ui-6x/dist/`
3. 在服务器执行：`bash /home/jvkit/workspace/oa/deploy/deploy-to-server.sh`

## nginx 配置
将 `deploy/nginx-oa.conf` 中的内容合并到服务器 nginx 主配置中，或执行：
```bash
cat /home/jvkit/workspace/oa/deploy/nginx-oa.conf >> /docker/nginx/conf/nginx.conf
nginx -t && nginx -s reload
```

## 验证
- 前端：`http://172.16.16.110/oa`
- 后端 API：`http://172.16.16.110/oa/prod-api/`
- 登录默认账号：`admin` / `admin123`

## 注意事项
- 当前未包含 Snail-AI 模块，如需 AI 功能需额外启动 `ruoyi-snailai-server`
- 后端使用 `prod` profile，请确保服务器 `application-prod.yml` 数据源/Redis 配置正确
