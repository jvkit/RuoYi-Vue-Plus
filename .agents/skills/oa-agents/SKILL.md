---
name: oa-agents
version: 2.0.0
description: "OA AI 智能体服务（agents/ 目录）：FastAPI + PydanticAI + Langfuse 架构、启动命令、发票识别匹配链路、提示词管理、追踪排查。当用户要求改 AI 功能、排查发票识别问题、启动/调试 agents 或 Langfuse 时使用。"
---

# OA AI 智能体服务（agents/）

## 架构原则（用户拍板）

**确定性的脏活累活交给代码，不确定的认知任务交给 AI。**

- PDF 转图/压缩/多页合并/字段校验/金额比对/商品匹配 → 全是**程序**
- 看图提取发票字段 → **AI**（PydanticAI 单步 Agent，结构化输出）
- **不做 RAG**（知识库归 owu 项目），**不用 Dify**（已弃用）
- LiteLLM 已移除（多模型直连 OpenAI 兼容端点即可）

```
ruoyi 后端 ──HTTP──> agents(FastAPI :8093) ──> 视觉模型(OCR)
                        │
                        └──> Langfuse(:3000) tracing + prompt 版本
```

## 目录与启动

```
agents/
├── src/
│   ├── main.py              # FastAPI 入口
│   ├── api/routes.py        # POST /invoice/match
│   ├── services/invoice_service.py   # 编排：并发识别 + 程序匹配 + 报告
│   ├── agents/invoice_agent.py       # PydanticAI Agent（只做看图→结构化）
│   ├── tools/pdf.py         # PDF 转图（PyMuPDF，只取第一页）
│   └── core/{config,langfuse}.py
├── langfuse/                # 自托管 Langfuse docker-compose
└── .env                     # 配置（不提交）
```

```bash
cd /home/jvkit/workspace/oa/agents
uv run uvicorn main:app --host 127.0.0.1 --port 8093 --app-dir src
curl http://127.0.0.1:8093/health   # {"ok": true}
```

ruoyi 侧配置（`application-dev.yml`）：`agents.invoice-match-url: http://127.0.0.1:8093/invoice/match`（服务器部署时改地址）。

## Langfuse（自托管）

```bash
cd agents/langfuse
docker-compose up -d      # 旧版 docker-compose（无空格子命令！）
docker-compose down && docker-compose up -d   # 改配置后必须 down 再 up，否则 KeyError: ContainerConfig
```

- Web：`http://localhost:3000`，`admin@langfuse.com / Langfuse@12345`
- 项目 `oa-agents`，key 已在 `agents/.env`
- 端口只暴露 3000（Web）和 9092（MinIO 读媒体），存储组件在 docker 内网，避开宿主机 3306/6379/9000
- 本地访问要清 proxy 环境变量

**改提示词不用改代码**：Langfuse 网页 Prompts 页编辑 `invoice_extract` → 发布新版本 → 下次调用自动生效（`get_prompt` 失败回退源码默认值 `invoice_agent.py` 里的 `_SYSTEM_PROMPT`）。
**看 AI 调用**：Tracing 页看每次 `invoice.extract` 的完整 input/output，用于区分程序问题还是 AI 问题。

## 配置项（agents/.env，都不硬编码）

| 项 | 说明 | 默认 |
|---|---|---|
| VISION_BASE_URL / VISION_API_KEY / VISION_MODEL | 视觉模型端点 | 192.168.236.13:47501 |
| MAX_PDF_PAGES | 只取第一页，超页标记提醒 | 1 |
| PDF_DPI / MAX_IMAGE_LONG_EDGE | 转图参数 | 200 / 1536 |
| CONCURRENCY | 并发识别数 | 3 |
| AGENT_TIMEOUT_SECONDS | 单发票超时 | 60 |
| LANGFUSE_* | tracing 配置 | - |

## 发票识别匹配链路（端到端）

1. 前端验收页「AI 识别发票」→ `POST /procurement/acceptance/ai-invoice-match`（items + files + acceptanceId）
2. ruoyi `PmsAcceptanceInvoiceService`：先上传 PDF 到 OSS（sysOssService，ossId 存 invoice_info.pdf_oss_id）→ 转发 agents `/invoice/match`
3. agents：PDF 首页转图 → 并发调视觉模型提取结构化字段（InvoiceExtractResult）→ 程序按商品名归一化匹配（NFKC 去空格 lower，双向包含）→ 金额核对（不含税口径，±5% 容差；负单价强制判冲红）
4. ruoyi 持久化 `invoice_info`：匹配成功=有效；未匹配=无效「未匹配到本订单商品」；发票代码+号码与已有有效发票重复=无效「发票号重复」；冲红标 red_flag
5. 返回报告（summary.lines + 每张票的 matchedItemIds/matchedUnitPrices/ossId），前端回填发票金额和附件、展示 ✅/⚠️/❌ 行
6. **支持多轮补充上传**：前端只送尚未填金额的明细

## 金额口径与冲红规则

- 口径：**不含税金额**（提示词里明确 `amount_without_tax` 填票面"金额"栏，不是价税合计）
- 冲红：`is_red_invoice`（票面红字/负数）或程序兜底（明细单价为负强制冲红）
- 超标：发票单价 > 申请单价 × 1.05 → over；< ×0.95 → amount_lower

## 排查思路

| 症状 | 查哪里 |
|---|---|
| 识别慢/超时 | agents 日志 + Langfuse trace 的耗时；调 CONCURRENCY/TIMEOUT |
| 字段识别错 | Langfuse trace 看 AI 原始输出——是 AI 看错（改提示词）还是程序匹配错 |
| 匹配不上 | 商品名归一化逻辑（invoice_service.py `_match_item`）；发票商品名通常很长，双向包含已有容错 |
| 报「发票识别服务调用失败」 | ruoyi 日志 + `curl agents:8093/health`；检查 ruoyi 配置的 invoice-match-url |
| 部署到服务器后不可用 | 服务器暂未部署 agents，属已知限制 |
