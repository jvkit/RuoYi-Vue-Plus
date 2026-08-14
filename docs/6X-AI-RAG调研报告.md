# 6X AI（RAG）接入调研报告

> 调研日期：2026-08-11
> 目标：把 `owu-feed/gateway` 里已验证的 OCR 服务与嵌入模型，落地配置到 6.0.0 参考项目（`ruoyi-6x` / `plus-ui-6x`）的 AI/RAG 能力中。
> 状态：**调研完成，待确认接入路径后实施**

---

## 1. 背景

- 6x 项目（`ruoyi-6x`）自带 AI 能力，但走的是一套 **Snail AI（aizuda）** 体系，与 `owu-feed` 的 **Open WebUI** 体系不同。
- 老板希望把 owu 里已经在用的 **OCR 服务** 和 **嵌入模型** 复用到 6x 的 RAG 上。
- 本文先弄清 owu 侧到底用了什么、6x 侧现状如何、两者如何对接，再给出可落地的配置方案。

---

## 2. owu-feed/gateway 的 OCR 到底调用什么服务

### 2.1 一句话结论

gateway **不自己跑 OCR**，而是调用公司内网一台**基于 vLLM Qwen 视觉大模型的「图片/PDF → Markdown」外部 HTTP 服务**。

### 2.2 关键配置

| 项 | 值 | 环境变量（`gateway/src/config.ts` / `.env` / `stack/gateway.env`） |
|---|---|---|
| OCR 服务地址 | `http://172.16.16.110:3309/api/ocr/tasks` | `OCR_TASKS_URL` |
| OCR API Key | `dilab2025ocr` | `OCR_API_KEY` |
| 其背后的 vLLM | `http://192.168.236.13:47501/v1` | `OCR_VLLM_URL` |
| vLLM API Key | `sk-Dg178ahJjKa6gOUa096` | `OCR_VLLM_API_KEY` |
| 视觉模型 | `Qwen-Qwen3.5-35B-A3B-GPTQ-Int4-tool-image-4080s` | `OCR_MODEL` |

### 2.3 处理流程（`gateway/src/modules/files/workers.ts`）

1. 上传文件后按扩展名分流（`config.ts`）：
   - OCR 类：`.pdf / .docx / .pptx / .xlsx / .step / .stp` → `parse_strategy = 'ocr'`
   - 透传类：`.md / .txt / .csv / .json / .yaml / .html` 等 → `passthrough`
2. OCR 任务：
   - `POST {OCR_TASKS_URL}?key=&filename=&vllm_url=&api_key=&model=` 上传原文件，创建任务，拿 `task_id`
   - 轮询 `{OCR_TASKS_URL}/{task_id}?key=` 直到 `succeeded`
   - `GET {OCR_TASKS_URL}/{task_id}/result?key=` 拿回 **Markdown**
3. 结果写本地 `parsed/` 目录
4. 上传 Open WebUI（`owuUploadFile`）→ 加入知识库 collection（`owuAddFileToCollection`）→ 触发 RAG 模型同步（`syncUserModel`）

### 2.4 Open WebUI 交互（`gateway/src/lib/owu.ts`）

- 知识库（collection）：`GET /api/v1/knowledge/`、`POST /api/v1/knowledge/create`、`.../file/add`、`.../access/update`
- 文件：`POST /api/v1/files/`、`GET .../process/status`、`DELETE ...`
- RAG 模型：`POST /api/v1/models/create` 自动建一个绑定 knowledge 的模型（`rag_<email>`），base model 用 `btbtyler09-Qwen3-Coder-Next-GPTQ-4bit-kv16-tool`，参数 `top_k=40` 等

---

## 3. 嵌入模型（向量化）

嵌入模型**不在 gateway，而在 Open WebUI**，配置在 `owu-feed/stack/docker-compose.yml` / `docker-compose.dev.yml`：

```yaml
RAG_EMBEDDING_ENGINE: ollama
RAG_EMBEDDING_MODEL: qwen3-embedding:8b
RAG_OLLAMA_BASE_URL: http://192.168.61.21:11434
```

- 向量化用 **Ollama `qwen3-embedding:8b`**。
- 实测 `192.168.61.21:11434`（`/api/tags`）上可用模型：
  - 嵌入：`qwen3-embedding:8b`、`qwen3-embedding:8b-q8_0`、`8b-fp16`、`4b-fp16`、`bge-m3:latest`
  - 重排：`dengcao/bge-reranker-v2-m3:latest`、`dengcao/Qwen3-Reranker-4B:Q4_K_M`、`bge-reranker-large:latest`
  - 视觉：`qwen3-vl:8b`（本地其实也有一台 8B 视觉模型）
- 实测 `/api/embeddings` 对 `qwen3-embedding:8b` 返回向量，**嵌入模型可用**。
- OpenAI 兼容端点：`http://192.168.61.21:11434/v1/embeddings`（Ollama 自带 `/v1` OpenAI 兼容层）。

---

## 4. 6x 侧 AI 现状

### 4.1 后端

| 模块 | 说明 |
|---|---|
| `ruoyi-modules/ruoyi-ai` | 目前只有 `POST /snail-ai/user/register`（把当前登录用户注册成 Snail OpenAPI 用户，`SnailAiController.java`） |
| `ruoyi-common/ruoyi-common-ai` | `SnailAiConfig`：`@ConditionalOnProperty(snail-ai.enabled=true)` 时开启 Snail AI agent + openapi client |
| `ruoyi-extend/ruoyi-snailai-server` | Snail AI 独立服务端（gRPC `18888` / HTTP `8900`，context-path `/snail-ai`），**自带 RAG** |
| `ruoyi-common-elasticsearch` | ES 模块（Snail AI 向量库可选项之一，需在 Snail 后台配置） |

### 4.2 6x 的 RAG（`ruoyi-extend/ruoyi-snailai-server/src/main/resources/application.yml`）

```yaml
snail-ai:
  rag:
    docling:
      enabled: true
      url: http://127.0.0.1:5100          # Docling 文档解析服务
      paddle-ocr-enabled: true
      paddle-ocr-url: http://127.0.0.1:8866/ocr   # PaddleOCR 图片文字识别
      vision-ocr-fallback-enabled: false  # 视觉模型 OCR 兜底（默认关）
```

> 即 Snail AI 的文档解析走 **Docling**、图片 OCR 走 **PaddleOCR**，是另一套本地服务，与 owu 的「vLLM 视觉 OCR 服务」不是一回事。
> 注：Snail AI 的 **embedding 模型配置不在 yml 里**，通常在其**管理后台 / 数据库**中设置（需在 Snail AI 后台接入向量模型）。

### 4.3 前端（`plus-ui-6x`）

- `src/views/ai/chat/index.vue`：iframe 嵌入 Snail AI 聊天（先 `registerCurrentSnailUser()` 拿 openId，再拼 `/snail-chat/?openId=&trustedCredential=`）
- `src/views/monitor/snailai/index.vue`：iframe 嵌入 Snail AI 管理后台（`VITE_APP_SNAILAI_ADMIN`）
- `src/api/ai/agent/index.ts`：目前只有 `registerCurrentSnailUser`

---

## 5. 连通性实测（本机）

| 端点 | 结果 |
|---|---|
| OCR 服务 `172.16.16.110:3309/api/ocr/tasks` | 在线（GET 返回 405 = 只收 POST，服务活着） |
| vLLM `192.168.236.13:47501/v1` | `200` 可达 |
| Ollama `192.168.61.21:11434`（`/api/embeddings`） | `200`，返回向量正常 |

**结论：owu 那套 OCR / vLLM / Ollama 基础设施从本机全部可达，给 6x 用没有网络障碍。**

---

## 6. 两种体系对比

| 维度 | owu-feed（生产在用） | 6x（参考项目） |
|---|---|---|
| RAG 平台 | Open WebUI | Snail AI（aizuda） |
| 文档解析 | 外部 vLLM 视觉 OCR 服务 | Docling |
| 图片 OCR | 同一 vLLM 视觉模型 | PaddleOCR（可开 vision 兜底） |
| 嵌入模型 | Ollama `qwen3-embedding:8b`（配在 OWU） | 需在 Snail AI 后台配置 |
| 对话模型 | Open WebUI RAG 模型 | Snail AI 应用/智能体 |

---

## 7. 落地配置方案（待确认后实施）

### 方向 A：复用 owu 的 OCR 服务（推荐，省事、已验证）

- 6x 侧新增/改造一个「文件 → Markdown」解析能力，直接调 `172.16.16.110:3309/api/ocr/tasks`（携带 `OCR_VLLM_URL` / `OCR_VLLM_API_KEY` / `OCR_MODEL` 参数），把返回的 Markdown 交给 Snail AI 知识库做向量化。
- 可行性前提：需确认 **Snail AI 是否支持外部文档解析/OCR 端点对接**。若不支持，则在 `ruoyi-6x` 里自写一个解析 controller，把 owu 的 `workers.ts` 逻辑用 Java 复刻（文件上传 → 建任务 → 轮询 → 拿 result）。

### 方向 B：嵌入模型接入

- 把 Ollama `qwen3-embedding:8b` 以 **OpenAI 兼容端点** `http://192.168.61.21:11434/v1/embeddings` 接入 Snail AI 的向量模型配置（在 Snail AI 管理后台配置）。
- 备选嵌入模型：`bge-m3:latest`、`qwen3-embedding:8b-q8_0`。
- 若要重排，可再配 `dengcao/bge-reranker-v2-m3`。

### 方向 C：只配嵌入模型（不动 OCR）

- 先解决 embedding 接入，OCR 暂沿用 Snail 自带 docling/paddleocr。

---

## 8. 待确认项 / 风险

1. **Snail AI 是否支持外部 OCR 服务对接**——决定方向 A 是「改配置」还是「新写 Java 解析器」。
2. **Snail AI 的 embedding 模型配置入口**——在管理后台（`VITE_APP_SNAILAI_ADMIN`）而非 yml，需要进入后台接入 OpenAI 兼容嵌入端点。
3. 6x 是**纯上游参考代码**，与生产 5.6.2 并行；本次改动属 6x 侧，不影响生产。

---

## 9. 建议下一步

1. 确认方向（A/B/C 或组合）。
2. 若选 A：先查 Snail AI 是否有外部解析/OCR 对接点；无则按 owu `workers.ts` 逻辑在 6x 写 Java 解析器。
3. 若选 B：进 Snail AI 后台把 `qwen3-embedding:8b`（OpenAI 兼容 `192.168.61.21:11434/v1`）配为向量模型。
4. 端到端验证：上传一个 PDF/图片 → OCR 出 Markdown → 向量化入库 → 问答命中。

---

## 附：关键文件索引

- owu OCR/解析：`~/workspace/owu-feed/gateway/src/modules/files/workers.ts`、`.../files/service.ts`
- owu 配置：`~/workspace/owu-feed/gateway/src/config.ts`、`.../gateway/.env`
- owu Open WebUI 交互：`~/workspace/owu-feed/gateway/src/lib/owu.ts`
- owu RAG 模型同步：`~/workspace/owu-feed/gateway/src/modules/rag/service.ts`
- owu 嵌入模型配置：`~/workspace/owu-feed/stack/docker-compose.yml` / `docker-compose.dev.yml`
- 6x Snail AI 客户端：`ruoyi-6x/ruoyi-modules/ruoyi-ai/`、`ruoyi-6x/ruoyi-common/ruoyi-common-ai/`
- 6x Snail AI Server：`ruoyi-6x/ruoyi-extend/ruoyi-snailai-server/src/main/resources/application.yml`（RAG/docling/paddleocr 配置）
- 6x 前端：`plus-ui-6x/src/views/ai/chat/index.vue`、`plus-ui-6x/src/views/monitor/snailai/index.vue`
