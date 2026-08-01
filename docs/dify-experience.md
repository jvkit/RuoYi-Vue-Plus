# Dify 部署与使用经验

> 记录人：Kimi Code
> 环境：服务器 Docker Compose / 本地 WSL 联调
> 对应 OA 项目：RuoYi-Vue-Plus 发票 AI 审核

---

## 一、Dify 是什么

Dify 是一个开源 LLM 应用开发平台，可以把它当成“AI 应用编排器”：

- 里面可以配置多个模型供应商（OpenAI、Claude、Gemini、自定义 OpenAI-compatible 等）。
- 可以创建“应用（App）”，每个应用有独立的工作流或对话流。
- 每个应用暴露一个 HTTP API（`/v1/chat-messages`），后端直接调用即可。
- 支持知识库、文件上传、多 Agent、工作流等。

一句话：**Dify 负责把“模型 + 提示词 + 知识库”封装成一个可调用的 AI 服务，我们的 OA 只负责调接口。**

---

## 二、服务器部署（Docker Compose）

### 1. 前置条件

- Docker + Docker Compose 已安装
- 服务器配置较强（Dify 默认会启动多个容器：web、api、worker、db、redis、weaviate 等）

### 2. 部署步骤

```bash
# 1. 创建目录
cd /home/liyang/jvkit/dify   # 按实际用户调整
mkdir -p dify && cd dify

# 2. 下载官方 docker-compose 文件
wget https://github.com/langgenius/dify/archive/refs/tags/1.0.0.tar.gz
tar -xzf 1.0.0.tar.gz
cd dify-1.0.0/docker

# 3. 复制环境变量
cp .env.example .env

# 4. 关键配置修改
vim .env
```

需要修改的地方：

```env
# 外部访问地址，不要用 localhost，写服务器 IP 或域名
CONSOLE_API_URL=http://172.16.16.110:8090
CONSOLE_WEB_URL=http://172.16.16.110:8090
SERVICE_API_URL=http://172.16.16.110:8090
APP_API_URL=http://172.16.16.110:8090

# WebSocket 地址，同样用服务器 IP，否则前端会一直连 localhost
NEXT_PUBLIC_SOCKET_URL=ws://172.16.16.110:8090

# 端口映射，确保不和现有服务冲突
NGINX_PORT=8090
NGINX_SSL_PORT=8443
```

### 3. 启动

```bash
docker compose up -d
```

首次启动会拉取多个镜像，耗时较长（5~20 分钟，看网络）。

### 4. 初始化

打开 `http://服务器IP:8090/install`，设置管理员账号。

---

## 三、常见问题与解决

### 问题 1：install 页面白屏 / 一直加载

原因：前端请求了 `http://localhost:8090/console/api/setup`，但浏览器访问的是服务器 IP。

解决：`.env` 里的 `CONSOLE_API_URL` 等必须改成服务器 IP，然后重启容器：

```bash
docker compose down
docker compose up -d
```

### 问题 2：CORS 报错

报错类似：

```
Access to fetch at 'http://localhost:8090/console/api/setup' from origin 'http://172.16.16.110:8090'
has been blocked by CORS policy
```

原因：还是 `.env` 里的 API URL 配成了 `localhost`。

解决：同问题 1，把所有 URL 改成服务器 IP/域名。

### 问题 3：WebSocket 连接失败，应用一直“同步数据”

报错类似：

```
WebSocket connection to 'ws://localhost/socket.io/...' failed
```

原因：`.env` 里的 `NEXT_PUBLIC_SOCKET_URL` 是 `ws://localhost`，浏览器在本地访问不到服务器的 localhost。

解决：

```env
NEXT_PUBLIC_SOCKET_URL=ws://172.16.16.110:8090
```

然后重启。

### 问题 4：Dify 启动后内存/CPU 占用高

Dify 默认启动的容器较多（nginx、api、worker、db、redis、weaviate、sandbox 等）。

- 最小建议：4C8G
- 流畅建议：8C16G 以上
- 如果仅做实验，可以停掉不用的容器，但建议保留 core 服务

---

## 四、配置模型供应商

### 1. 安装 OpenAI-API-Compatible 插件

Dify 原生不一定支持国内的 GLM / DeepSeek / Qwen 等模型，但可以通过 **OpenAI API Compatible** 插件接入。

路径：**设置 → 模型供应商 → 添加模型供应商 → 搜索 OpenAI API Compatible → 安装**

### 2. 添加供应商凭证

点击 OpenAI API Compatible 的“配置”：

- **API Base URL**：你的代理地址，例如 `http://127.0.0.1:8000/v1`（本地模型）或第三方转发地址。
- **API Key**：代理要求的 key。
- **Model Name**：实际模型名，例如 `glm-5.2`、`deepseek-v4-pro`、`qwen3.6`。
- **Model Type**：选 `LLM`。
- **Vision**：如果模型支持识图，勾选。

### 3. 保存后“添加模型”

配置好凭证后，点击“添加模型”，把每个模型单独加进去。

注意：

- **凭证（Credential）** 只是连接信息。
- **模型（Model）** 才是真正可以在应用里选择的。
- 如果只配了凭证没添加模型，应用里选不到。

### 4. 验证模型

添加模型时，一般会有“验证”按钮。如果验证失败：

- 检查 API Base URL 是否以 `/v1` 结尾。
- 检查 API Key 是否有效。
- 检查模型名是否在代理端真实存在。
- 代理不稳定时多试几次。

---

## 五、创建应用并获取 API Key

### 1. 创建应用

- 点击左侧“工作室” → “创建空白应用”。
- 选择 **Chatflow / Chatbot / Agent** 等，根据需求来。
- 发票审核用的是 Chatbot/Agent，系统提示词里写明审核规则。

### 2. 配置提示词

示例（发票审核）：

```text
你是一名财务审核助手，负责审核员工提交的发票。

请根据以下规则判断发票是否合规：
1. 发票代码、发票号码必须填写。
2. 金额逻辑正确：价税合计 = 不含税金额 + 税额。
3. 销售方不能是个人、小卖部、便利店等不合规主体。
4. 专票（special）金额过大需要额外关注。
5. 若发现任何问题，必须明确给出“驳回”结论和具体原因。
6. 若通过，给出“通过”结论和简要意见。

输出格式：
结论：通过 / 驳回
意见：...
```

### 3. 发布应用

配置好后，点击右上角“发布”。

### 4. 获取 API Key

- 进入应用 → 左侧“API 访问”。
- 点击“新建 API Key”，复制 key。
- 格式类似：`app-xxxxx...`。

### 5. 记录 App ID

API Key 页面上方会显示应用 ID，格式为 UUID：`fc04cb5d-0d6c-42b1-8dfd-7c6755827d2d`。

---

## 六、后端调用 Dify API

### 1. 接口地址

```
POST http://服务器IP:8090/v1/chat-messages
Authorization: Bearer {API Key}
Content-Type: application/json
```

### 2. 请求体示例

```json
{
  "inputs": {},
  "query": "请审核以下发票：...",
  "response_mode": "blocking",
  "user": "invoice-system",
  "files": [
    {
      "type": "image",
      "transfer_method": "remote_url",
      "url": "https://example.com/invoice.jpg"
    }
  ]
}
```

### 3. 字段说明

| 字段 | 说明 |
|------|------|
| `inputs` | 工作流输入变量，普通 Chatbot 可留空 |
| `query` | 发给模型的完整问题 |
| `response_mode` | `blocking` 同步返回，`streaming` 流式返回 |
| `user` | 用户标识，任意字符串 |
| `files` | 可选，上传图片等文件 |

### 4. 返回示例

```json
{
  "answer": "结论：驳回\n意见：销售方为个人小卖部，不符合报销规定。",
  "conversation_id": "..."
}
```

### 5. 代码封装建议

- 把 `api-url`、`app-id`、`api-key` 放到 `application-dev.yml`。
- 写一个 `DifyInvoiceReviewService`，封装 HTTP 调用和结果解析。
- 解析 `answer` 时，根据关键词（驳回/不通过/拒绝）判断 passed。

---

## 七、发票 AI 审核集成到 OA

### 1. 数据表增加字段

```sql
ALTER TABLE invoice_info ADD COLUMN ai_opinion TEXT COMMENT 'AI审核意见' AFTER remark;
```

### 2. 实体/VO/BO 增加字段

在 `InvoiceInfo.java`、`InvoiceInfoVo.java`、`InvoiceInfoBo.java` 中增加：

```java
private String aiOpinion;
```

### 3. 新增 AI 审核接口

`InvoiceInfoController` 中新增：

```java
@PostMapping("/ai-review/{id}")
public R<Map<String, Object>> aiReview(@PathVariable Long id, @RequestParam(required = false) MultipartFile imageFile)

@GetMapping("/ai-review/{id}")
public R<Map<String, Object>> getAiReview(@PathVariable Long id)
```

### 4. 审核流程

1. 员工提交发票（状态 `draft`）。
2. 调用 `POST /invoice/info/ai-review/{id}`。
3. Dify 返回审核意见：
   - 通过 → 状态改为 `submitted`，写入 `ai_opinion`。
   - 驳回 → 状态改为 `rejected`，写入 `ai_opinion`。
4. 员工查看 AI 意见，修改发票后重新提交。
5. 负责人审核时可参考 `ai_opinion`。

---

## 八、调试技巧

### 1. 直接用 curl 测试 Dify

```bash
curl -s -X POST http://172.16.16.110:8090/v1/chat-messages \
  -H "Authorization: Bearer app-xxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "inputs": {},
    "query": "请审核以下发票：发票代码 011001900211，发票号码 12345678，金额 1000，税额 130，价税合计 1130，销售方 合规科技有限公司",
    "response_mode": "blocking",
    "user": "test"
  }'
```

### 2. 查看 Dify 容器日志

```bash
cd /home/liyang/jvkit/dify/dify-1.0.0/docker
docker compose logs -f api
```

### 3. 本地测试 OA 的 AI 审核

因为 RuoYi-Vue-Plus 默认开启了接口加密（`api-decrypt.enabled=true`）和验证码，本地 API 测试比较麻烦。推荐：

- 写一个 `InvoiceAiReviewMain` 直接启动 Spring Boot 上下文调用 Service 层。
- 或临时在 `application-dev.yml` 关闭 `api-decrypt` 和 `captcha` 后重启后端。

---

## 九、当前项目已配置好的信息

| 项目 | 值 |
|------|-----|
| Dify 地址 | `http://172.16.16.110:8090` |
| 发票 AI 审核应用 ID | `fc04cb5d-0d6c-42b1-8dfd-7c6755827d2d` |
| 发票 AI 审核 API Key | `app-22cb3e89ff524629af078ee3af77ac08` |
| 已配置模型 | `glm-5.2`、`deepseek-v4-pro`、`qwen3.6` |
| OA 本地后端 | `http://127.0.0.1:8088` |

---

## 十、后续扩展建议

1. **图片上传审核**：当前 Dify 文件上传用 `remote_url` 最稳定；如果用 base64，需要先把图片传到可访问的 URL，或改成 Dify 的文件上传接口先拿到 `upload_file_id`。
2. **结构化输出**：可以让 Dify 应用直接返回 JSON（`{"passed": true, "opinion": "..."}`），后端解析更稳定。
3. **知识库**：把公司的报销规则做成知识库，Dify 应用引用后审核更准确。
4. **工作流**：复杂的审核流程可以在 Dify 里用 Workflow 节点编排，而不只是单轮对话。
