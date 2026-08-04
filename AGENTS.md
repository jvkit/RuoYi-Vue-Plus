# 工作区指引

本工作区包含 RuoYi-Vue-Plus 后端与 plus-ui 前端，用于学习、二次开发以及采购/SOP 流程的扩展。

## 仓库位置

- 后端：`/home/jvkit/workspace/oa/RuoYi-Vue-Plus`（版本 5.6.2）
- 前端：`/home/jvkit/workspace/oa/plus-ui`（版本 5.6.2-2.6.2）
- 前端是官方仓库，GitHub 账号已由 `JavaLionLi` 改名为 `CrazyLionCat`，代码本身与后端版本一一对应。

## 根目录结构速查

```
/home/jvkit/workspace/oa/
├── RuoYi-Vue-Plus/        # 后端 Java 代码（Spring Boot）
├── plus-ui/                 # 前端 Vue3 代码
├── docs/                    # 教学文档、经验总结
├── scripts/                 # Python 辅助脚本（Dify 配置、模型管理、发票图片生成等）
├── logs/                    # 后端运行日志
├── test-invoices/           # 发票识别测试图片
├── ruoyi-api-docs.html      # 离线 API 文档（ReDoc）
└── ruoyi-api-docs.json      # 离线 OpenAPI JSON
```

注意：
- `RuoYi-Vue-Plus` 是**后端**，名字里的 `Vue` 只是项目名，不是前端代码。
- `plus-ui` 才是**前端**。
- 新增辅助脚本、一次性工具脚本统一放到 `scripts/`，不要再放根目录，保持根目录整洁。

## 教学文档

详细的学习/讲解文档统一放在：

```
/home/jvkit/workspace/oa/docs/教学/
```

约定：

- 每个主题单独一个 Markdown 文件，按序号命名，例如 `01-前端整体结构与代码位置.md`。
- 已发布的教学文档**不二次修改**。如果用户对某篇内容提出疑惑或需要补充，应**新开一篇文档**继续讲解，而不是改写旧文档。
- 目前已有的文档：
  - `01-前端整体结构与代码位置.md`
  - `02-如何新增一个完整业务模块.md`
  - `03-后端分层详解：实体-BO-VO-Mapper-Service-Controller.md`
  - `04-给零基础讲后端分层：Controller、校验、SQL、复制粘贴.md`
  - `05-后端三层无比喻版：Controller-Service-Mapper-到底是干什么的.md`
  - `06-权限、角色、用户与多租户概念.md`
  - `07-如何为公司A搭建一个员工端：前端视角.md`
  - `08-租户、租户套餐、父子联动是什么意思.md`
  - `09-实操：创建租户、套餐、角色并分配权限（长三角物理研究示例）.md`
  - `10-用户导入Excel模板字段合法值说明.md`
  - `11-用户导入后如何批量分配角色.md`
  - `12-导入模板优化建议：用角色名称和部门名称代替编号.md`
  - `13-字典怎么用：从创建到前后端显示.md`
  - `14-代码生成器到底能干啥、怎么用.md`
  - `15-系统监控里任务调度中心和Admin监控为什么是空的.md`
  - `16-AI 员工审批助手设计方案.md`
  - `17-病假 AI 审批 MVP 设计方案.md`
- `AGENTS.md` 本身记录项目整体状态和快速参考，不涉及具体教学细节。

## 当前运行状态

| 服务 | 地址/位置 | 说明 |
|---|---|---|
| 前端（nginx 容器） | `http://127.0.0.1:8090/index` | 容器名 `nginx-web`，使用 host 网络；因 80 被系统 nginx/Dify 占用，改为 8090 |
| 后端 | `http://127.0.0.1:8088` | 端口因 8080 被占改用 8088 |
| API 文档 | `http://127.0.0.1:8088/swagger-ui/index.html` | SpringDoc/OpenAPI |
| 发票管理菜单 | `http://127.0.0.1/index` → 发票管理 → 发票信息 | 登录后可见 |
| 离线 API 文档 | `/home/jvkit/workspace/oa/ruoyi-api-docs.html` | ReDoc 静态页 |
| 离线 OpenAPI JSON | `/home/jvkit/workspace/oa/ruoyi-api-docs.json` | 可导入 Postman/Apifox |

默认登录账号：`admin` / `admin123`。

## 启动/停止命令

### 后端

```bash
cd /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin
nohup java -jar target/ruoyi-admin.jar --server.port=8088 > /tmp/ruoyi-admin.log 2>&1 &
```

停止：找到 `java -jar target/ruoyi-admin.jar --server.port=8088` 进程并 kill。

### 前端

```bash
# 开发调试
cd /home/jvkit/workspace/oa/plus-ui
pnpm dev
```

生产部署使用 nginx 容器：

```bash
# 构建
cd /home/jvkit/workspace/oa/plus-ui
pnpm build:prod

# 复制到 nginx 静态目录
sudo rm -rf /docker/nginx/html/*
sudo cp -r dist/* /docker/nginx/html/

# 启动/重启容器
docker start nginx-web
# 或
docker restart nginx-web
```

## 端口冲突处理

本机 80 端口可能被系统 nginx（OpenWebUI 反代）占用，8080 被 `open-webui-feedback-owu` 占用。

临时停止冲突服务（不删容器）：

```bash
sudo systemctl stop nginx          # 系统 nginx，反向代理了 OpenWebUI
docker stop open-webui-feedback-owu # OpenWebUI 容器
docker start nginx-web             # 启动 RuoYi 前端容器
```

恢复 OpenWebUI：

```bash
sudo systemctl start nginx
docker start open-webui-feedback-owu
```

## 数据库

- MySQL 容器：`mysql`，host 网络，端口 3306
- 数据库名：`ry-vue`
- 账号/密码：`root` / `root`
- 执行 SQL 方式：`docker exec -i mysql mysql -uroot -proot -D ry-vue --default-character-set=utf8mb4 < xxx.sql`
  - 所有 SQL 脚本已加入 `SET NAMES utf8mb4;`，防止中文写入时出现双重编码乱码。

已初始化的脚本：
- `ry_vue_5.X.sql`
- `ry_job.sql`
- `ry_workflow.sql`

## 已生成的扩展

### 1. 采购申请后端代码

位于 `ruoyi-modules/ruoyi-workflow`，仿照 `TestLeave` 实现：

- `domain/ProcurementRequest.java`
- `domain/bo/ProcurementRequestBo.java`
- `domain/vo/ProcurementRequestVo.java`
- `mapper/ProcurementRequestMapper.java`
- `service/IProcurementRequestService.java`
- `service/impl/ProcurementRequestServiceImpl.java`
- `controller/ProcurementRequestController.java`
- `resources/mapper/workflow/ProcurementRequestMapper.xml`

数据库表：`procurement_request`。

接口前缀：`/workflow/procurement`，权限标识：`workflow:procurement:*`。

菜单：侧边栏 **采购管理 → 采购申请**。

### 2. 发票管理模块

位于 `ruoyi-modules/ruoyi-invoice`，是一个完整 CRUD 示例：

- `domain/InvoiceInfo.java`
- `domain/bo/InvoiceInfoBo.java`
- `domain/vo/InvoiceInfoVo.java`
- `mapper/InvoiceInfoMapper.java`
- `service/IInvoiceInfoService.java`
- `service/impl/InvoiceInfoServiceImpl.java`
- `controller/InvoiceInfoController.java`
- `resources/mapper/invoice/InvoiceInfoMapper.xml`

数据库表：`invoice_info`。

字典：`invoice_type`（发票类型）、`invoice_status`（发票状态）。

接口前缀：`/invoice/info`，权限标识：`invoice:info:*`。

菜单：侧边栏 **发票管理 → 发票信息**。

相关 SQL：
- 建表：`RuoYi-Vue-Plus/script/sql/invoice_table.sql`
- 字典/菜单/权限：`RuoYi-Vue-Plus/script/sql/invoice_module.sql`

教学文档：`docs/教学/02-如何新增一个完整业务模块.md`。

### 3. 部署配置

- nginx 配置：`/docker/nginx/conf/nginx.conf`（已指向 127.0.0.1:8088）
- 前端开发代理：`/home/jvkit/workspace/oa/plus-ui/vite.config.ts`（目标 127.0.0.1:8088）
- 备份配置：`/docker/nginx/conf/nginx.conf.bak.*`

## 常见非致命告警

后端日志中可能出现，不影响登录和常规接口：

1. `UnknownHostException: net` — Spring Boot Admin 客户端注册失败，本机主机名 `net` 解析失败。
2. `SnailJob gRPC 连不上 127.0.0.1:17888` — `ruoyi-snailjob-server` 未启动。

如需完整监控和定时任务调度，再启动对应模块。

## 学习建议路线

1. 登录与用户/角色/权限管理
2. 菜单管理、字典、参数、部门、岗位
3. 工作流模块：流程定义、请假示例、我的任务
4. 代码生成器：导入表 → 预览 → 生成 zip
5. 前后端代码结构梳理
6. 采购/SOP 流程二次开发

## 注意事项

- 本项目采用前后端分离，前端路由/菜单由后端 `sys_menu` 表驱动，但页面组件必须存在于前端 `plus-ui/src/views` 中。
- 菜单类型：`M` 目录、`C` 菜单、`F` 按钮。
- 外链配置：菜单类型为目录时，`是否外链=是` + `路由地址=http(s)://xxx` 可实现点击跳转外部站点。
- 工作流基于 Warm-Flow，示例 `TestLeave` 是最直接的参考。
