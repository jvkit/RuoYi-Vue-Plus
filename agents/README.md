# OA Agents 服务

OA 的智能体服务：**代码做确定性的脏活，AI 只做认知节点**。单步 Agent，不做 RAG（知识库归 owu）。

## 技术栈

- FastAPI：API 层
- PydanticAI：Agent / 结构化输出
- Langfuse：追踪 / prompt 版本（自托管）
- PyMuPDF + Pillow：PDF 转图、图片压缩（确定性工具）

> 注：LiteLLM 已从依赖中移除（多模型直连 OpenAI 兼容端点即可，不需要网关）。

## 启动

```bash
cd /home/jvkit/workspace/oa/agents
uv sync            # 首次安装依赖（创建 .venv）
cp .env.example .env   # 填入 LLM_API_KEY 等（不提交仓库）
uv run uvicorn main:app --host 127.0.0.1 --port 8093
```

- 健康检查：`curl http://127.0.0.1:8093/health`
- 文档：`http://127.0.0.1:8093/docs`

## Langfuse（自托管，tracing + prompt 管理）

```bash
cd /home/jvkit/workspace/oa/agents/langfuse
docker-compose up -d      # 宿主机用的是旧版 docker-compose（无 docker compose 子命令）
docker-compose down       # 改配置后必须 down 再 up -d，否则报 KeyError: ContainerConfig
```

- Web UI：`http://localhost:3000`，登录 `admin@langfuse.com` / `Langfuse@12345`
- 项目：`oa-agents`（id `bcc58c89-fdb0-4ded-8e10-b0ef406bec5f`）
- API key：public `pk-lf-oa-agents-0001` / secret `sk-lf-oa-agents-0001`（已写入 `agents/.env`）
- 健康检查：`curl http://localhost:3000/api/public/health`

端口只暴露 3000（Web）和 9092（MinIO 读媒体），存储组件（postgres/clickhouse/redis/minio 9000）只在 docker 内网，避开宿主机 3306/6379/9000 冲突。

**改提示词不用改代码**：网页 Prompts 页编辑 `invoice_extract` → 发布新版本 → 服务下次调用自动生效（`get_prompt` 按版本加载，加载失败回退源码默认值）。

**看 AI 调用**：网页 Tracing 页能看到每次 `invoice.extract` 的完整 input（文件名/模型/图片字节数）与 output（结构化字段），用于区分"程序问题"还是"AI 问题"。

## 目录结构

```
src/
├── main.py              # FastAPI 入口
├── core/
│   ├── config.py        # 全部配置（环境变量，不硬编码）
│   └── langfuse.py      # Langfuse tracing + prompt 管理出口
├── agents/
│   └── invoice_agent.py # 发票识别 Agent（只看图，输出字段）
├── services/
│   └── invoice_service.py # 并发编排 + 程序匹配 + 报告
├── tools/
│   └── pdf.py           # PDF 第一页转图（确定性）
└── api/
    └── routes.py        # /invoice/match 等路由
```

## 接口

### `POST /invoice/match`

批量上传发票 PDF，AI 提取字段 + 程序匹配采购申请明细，返回匹配报告。

```bash
curl -X POST http://127.0.0.1:8093/invoice/match \
  -F 'items=[{"id":1,"itemName":"GPU显卡","spec":"RTX4090","unitPrice":12000,"quantity":1}]' \
  -F 'files=@/path/to/invoice1.pdf'
```

响应：`data.results[]`（每张发票的提取字段 + 匹配结果）+ `data.summary`（程序汇总报告）。

## 配置（环境变量，见 `.env.example`）

| 变量 | 说明 |
|---|---|
| `AGENTS_HOST` / `AGENTS_PORT` | 服务监听地址/端口（默认 127.0.0.1:8093） |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | 文本模型端点（暂未使用，预留） |
| `VISION_BASE_URL` / `VISION_API_KEY` / `VISION_MODEL` | 视觉模型端点（发票识别），与文本模型分离 |
| `MAX_PDF_PAGES` | 发票只看第一页（默认 1，超过提示人工核对） |
| `PDF_DPI` / `MAX_IMAGE_LONG_EDGE` | 转图参数 |
| `CONCURRENCY` | 并发识别数 |
| `AGENT_TIMEOUT_SECONDS` | 单发票识别超时秒 |
| `LANGFUSE_PUBLIC_KEY` / `LANGFUSE_SECRET_KEY` / `LANGFUSE_HOST` | 追踪 + prompt（未配置则关闭） |

## 设计约定

- AI 只提取字段，**不做任何业务判断**（是否冲红、是否超标、属于哪个商品都由程序算）。
- 匹配是**纯程序**的：商品名归一化后相等/包含即匹配（当前假设用户填的商品名与发票一致）。
- 发票超过 `MAX_PDF_PAGES` 页时只取第一页，返回 warning 提示人工核对。
- 所有可能因部署环境变化的项（端点、key、并发、页数、DPI）都在 `.env` 配置。
