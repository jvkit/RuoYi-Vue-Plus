---
name: oa-test-e2e
version: 2.0.0
description: "OA Playwright 全链路测试：测试项目位置与运行方式、测试账号、localhost 代理坑、全链路场景（申请→审批→验收→手机端→领用）、截图检查要求。当用户要求测试 OA 功能、跑全链路验证、复现页面 bug 时使用。运行前必须读 /home/jvkit/workspace/work_twst/AGENTS.md。"
---

# OA Playwright 全链路测试

## 测试项目位置（不在本仓库！）

`/home/jvkit/workspace/work_twst` — Playwright + Node 浏览器自动化项目。**动手前必须先读其 `AGENTS.md`**（运行约定、目录规范、proxy 处理）。

已有 OA 相关目录：
- `app/ruoyi6x/` — 本地 OA 测试脚本
- `app/ruoyi6x-server/` — 服务器 OA 测试（`outputs/full-flow/` 有全链路截图产物）
- `app/oa-invoice/` — 发票演示

## 运行方式

```bash
cd /home/jvkit/workspace/work_twst
PLAYWRIGHT_BROWSERS_PATH=$PWD/.pw-browsers node app/<site>/scripts/<script>.mjs
```

- 浏览器二进制在项目内 `.pw-browsers/`，必须设 `PLAYWRIGHT_BROWSERS_PATH`
- 脚本用 ESM（.mjs），从项目根运行
- WSLg 有头模式可用（`headless: false` 调试时直观），CI/批量用 headless

## 两大环境坑（必记）

1. **localhost 必须清代理**：shell 有 `http_proxy=127.0.0.1:7897`，访问 localhost 会 502。脚本开头删 proxy 环境变量：
   ```js
   for (const k of ['http_proxy','https_proxy','HTTP_PROXY','HTTPS_PROXY','all_proxy','ALL_PROXY']) delete process.env[k];
   ```
2. **测服务器保留代理**：`http://172.16.16.110/oa` 若直连不通，保留系统代理变量。

## 测试目标与账号

| 环境 | 地址 |
|---|---|
| 本地 | `http://localhost:8082` |
| 服务器 | `http://172.16.16.110/oa`（注意 /oa 前缀） |

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 超级管理员 |
| wangjianlong | 666666 | 团队上级（CEO 审批） |
| lidi | 666666 | 最高决策人（≥1000 审批） |
| peitianzi | 666666 | 仓库管理员 + 验收对接人 |
| pengsaiwei | 666666 | 普通用户 |
| test_employee | - | 员工（发票测试） |

登录页：`/login`，`input[placeholder="用户名"]` / `input[placeholder="密码"]`，dev 验证码关闭直接点登录。服务器地址是 `/oa/login`。

## 脚本骨架（参考 app/ruoyi6x/scripts/test-acceptance-upload.mjs）

- 监听三类事件：`console`（error/无权）、`pageerror`、`response`（status>=400 记录 URL）
- 日志分级 STEP/HTTP/CONSOLE/PAGEERROR，结尾汇总
- 截图输出到 `app/<site>/outputs/`

## 全链路场景清单

1. admin 登录 → 创建项目（若需）
2. 普通用户（pengsaiwei）提交采购申请（选项目、加明细、提交）
3. 项目负责人审批 → 金额 <1000：CEO 审批后结束；≥1000：再过最高决策人（lidi）
4. 验收：对已完成申请创建验收单 → 上传实物图片 → 提交 → 流程审批
5. 手机端验收：`/mobile/acceptance`（手机视口 390x844）
6. 仓库：验收完成后库存增加检查
7. 领用申请：普通用户提交 → 仓库管理员审批
8. 全程检查：无 404、无「没有访问权限」弹窗

## 截图检查要求（重要教训）

**截图 ≠ 通过**。历史上出现过列表页一直转圈但脚本判 pass 的情况。必须：

- 断言等待**表格数据行**出现（如 `page.waitForSelector('.el-table__row')`）
- 检查 API 响应的 `code===200` 且 `total>0` 或 `rows.length>0`
- 每个关键页面截图后**人工/AI 亲眼看内容**再下结论
- 等待时间要足够（后端冷启动首次请求慢，等 100s 也可能还在转圈——那是真有问题）

## 服务器测试注意

- 服务器后端端口 8092、URL 带 `/oa` 前缀
- 服务器上暂无 agents 服务，发票 AI 匹配按钮会报错（属已知限制，不算 bug）
- 测试产生的数据会留在服务器库，测试后可清理或告知用户
