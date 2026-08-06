# 发票 AI 审核与查重工作汇报

## 一、核心结论

**已完成发票管理模块的 AI 审核闭环与重复提交校验能力，员工可上传发票图片自动识别填表，系统实时查重并提示已关联订单；管理员可查验真伪、查看 AI 审核意见、追溯发票使用记录。**

- ✅ 员工端：上传发票 → AI 识别 → 自动填表 → 提交查重 → AI 审核
- ✅ 管理端：发票列表 → 真伪查验 → AI 意见查看 → 使用记录追溯
- ✅ 重复发票提交时，界面明确提示"该发票已在订单 XX 下提交"
- ✅ 后端 API 与 Dify 工作流已打通，支持多模型识图与审核

---

## 二、业务价值

1. **降低财务风险**：AI 预审不合规发票，减少人工漏审；查重机制防止同一张发票重复报销。
2. **提升员工效率**：上传图片即可自动识别发票代码、号码、金额、销售方等字段，减少手填。
3. **流程可追溯**：每张发票关联订单号、生成财务查询单号，便于后续审计与反查。

---

## 三、已完成工作清单

### 3.1 后端开发

| 模块 | 内容 | 文件 |
|---|---|---|
| 数据模型 | `invoice_info` 表扩展真伪状态、查验时间、财务查询单号、关联订单号 | `script/sql/invoice_table.sql` |
| 数据模型 | 新增 `invoice_usage_record` 发票使用记录表 | `script/sql/invoice_table.sql` |
| Domain/BO/VO | 扩展 `verifyStatus`、`verifyTime`、`finQueryNo`、`orderNo` 字段 | `domain/InvoiceInfo.java`、`domain/bo/InvoiceInfoBo.java`、`domain/vo/InvoiceInfoVo.java` |
| 查重接口 | `GET /invoice/info/check-duplicate`：按发票代码+号码查重，返回完整重复记录信息 | `controller/InvoiceInfoController.java` |
| 真伪查验 | `POST /invoice/info/verify/{id}`：财务单号末位奇数为真、偶数为假（mock） | `service/impl/InvoiceInfoServiceImpl.java` |
| 财务反查 | `GET /invoice/info/query/{finQueryNo}`：通过财务查询单号反查发票 | `controller/InvoiceInfoController.java` |
| 使用记录 | 完整 CRUD：增删改查发票使用记录 | `controller/UsageRecordController.java` 等 |
| AI 审核 | 对接 Dify 工作流，图片上传后返回通过/驳回意见 | `service/impl/InvoiceInfoServiceImpl.java` |

### 3.2 前端开发

| 页面 | 内容 | 文件 |
|---|---|---|
| 员工上传页 | 发票图片上传、AI 识别自动填表、表单常驻可手填、查重弹窗提示 | `src/views/invoice/employee/index.vue` |
| 管理端列表 | 发票列表展示、真伪状态列、财务单号列、查验按钮 | `src/views/invoice/info/index.vue` |
| 使用记录 | 使用记录完整 CRUD、新增/编辑弹窗 | `src/views/invoice/usage/index.vue` |
| API 类型 | 扩展 `orderNo` 字段到 VO/Form 类型定义 | `src/api/invoice/info/types.ts` |

### 3.3 Dify 配置

- 服务器地址：`http://172.16.16.110:8090`
- 已配置 OpenAI 兼容供应商，接入 `glm-5.2` / `deepseek-v4-pro` / `qwen3.6` 等模型
- 已创建发票审核应用，支持图片输入与结构化审核意见输出

---

## 四、关键功能细节

### 4.1 查重弹窗（本次重点修复）

**修复前问题**：
- 查重判断逻辑 `if (dupRes.data)` 永远为 true，每次提交都弹窗提示重复
- 弹窗只写"该发票已存在"，未指明具体是哪条记录、哪个订单

**修复后效果**：
- 判断改为 `if (dup && dup.isDuplicate)`，仅真正重复时提示
- 弹窗展示重复记录的完整信息：
  - 发票代码 / 发票号码
  - 状态（已提交 / 已驳回等）
  - 销售方、价税合计
  - 财务查询单号
  - **关联订单号**：`⚠️ 该发票已在关联订单「ORD-2026-001」下提交`
  - 记录 ID

### 4.2 关联订单号

- 员工提交发票时可填写"关联订单号"
- 一个订单号可关联多张发票
- 同一张发票（代码+号码）重复上传时，系统提示其已关联的订单号

### 4.3 AI 审核流程

1. 员工上传发票图片
2. 后端调用 Dify 多模态模型识别票面信息并预审
3. 预审结果回填到表单（同时保留手动修正能力）
4. 提交时查重
5. 通过后再次调用 AI 给出最终审核意见
6. 结果分两类：
   - **通过**：状态 `submitted`，可进入后续财务流程
   - **驳回**：状态 `rejected`，提示具体问题并要求员工修正后重新提交

---

## 五、技术栈

- **前端**：Vue 3 + TypeScript + Element Plus + Vite
- **后端**：Spring Boot 3 + MyBatis-Plus + Warm-Flow
- **AI 平台**：Dify + OpenAI 兼容模型（GLM / DeepSeek / Qwen）
- **数据库**：MySQL 8 + Redis 7
- **运行环境**：Docker + host 网络模式

---

## 六、访问地址

| 服务 | 地址 | 说明 |
|---|---|---|
| OA 前端 | `http://localhost:8082` | 开发环境 |
| OA 后端 | `http://localhost:8088` | API 服务 |
| API 文档 | `http://localhost:8088/swagger-ui/index.html` | OpenAPI |
| Dify | `http://172.16.16.110:8090` | AI 工作流平台 |

测试账号：
- 员工：`test_employee` / `admin123`，租户 `721855`
- 管理员：`admina` / `admin123`，租户 `721855`

---

## 七、后续计划

1. **替换 mock 查验**：接入真实发票查验 API（如税务局接口或第三方服务）。
2. **订单号下拉选择**：关联订单号从已有订单列表选择，减少手填错误。
3. **使用记录联动**：提交发票时自动根据关联订单生成使用记录，减少二次录入。
4. **PPT 演示素材**：基于前端页面截图整理 3-4 页案例说明 PPT。

---

*汇报日期：2026-08-04*
