# 17-病假 AI 审批 MVP 设计方案

> 目标：在 RuoYi-Vue-Plus 上实现一个最简的「AI 员工审批」原型，以病假审批为例，验证整套流程跑通即可。

---

## 一、MVP 边界：做什么 / 不做什么

### 做（最小闭环）

1. 普通员工提交病假申请，上传病假条图片。
2. AI 读取图片并给出预审结论：通过 / 拒绝 / 补材料 / 转人工。
3. 管理员在列表页看到 AI 建议，一键确认。
4. 申请人收到结果通知（站内信）。
5. 所有判断依据留痕。

### 不做（后续迭代再加）

- 不接入 Warm-Flow 工作流引擎（先用服务层状态机跑通）。
- 不做多租户下「AI 资源分配给哪个公司」的配置（当前默认系统有 AI 资源）。
- 不做权限细分配（当前管理员全可见，普通员工只能提交自己的）。
- 不做 AI 规则配置页面（规则先写死在代码/YAML 里）。
- 不做多渠道通知（只做站内信或系统消息提示）。
- 不做复杂的证据防伪（只做形式审查）。

> 原则：除非顺手，否则只做最小闭环。

---

## 二、技术栈选型

| 层级 | 选型 | 理由 |
|---|---|---|
| 后端框架 | Spring Boot 3.5.15（现有） | 不引入新基础框架 |
| 新模块 | `ruoyi-modules/ruoyi-ai-leave` | 独立模块，和发票模块平行 |
| 数据库 | MySQL 8.0（现有） | 新增 `ai_leave_request` 表 |
| AI 调用 | 直接用 `RestTemplate` 调 Moonshot Kimi API | 最简，无需引入 LangChain4j/Spring AI |
| 模型 | `kimi-k2.5` 或 `kimi-k2.6` | 中文 OCR 和文档理解能力强，OpenAI 兼容 |
| 图片上传 | 复用 RuoYi 文件上传（MinIO/本地） | 和发票模块一致 |
| 前端 | Vue3 + TS + Element Plus（现有） | 复用发票管理页面结构 |
| 权限 | 暂不细分，使用 `ai:leave:*` | 后续再拆 |

### 为什么不引入 LangChain4j / Spring AI？

MVP 阶段 AI 逻辑只有一步：

```
读取病假条图片 → 按规则检查 → 返回 JSON 结论
```

这种「单轮调用 + 结构化输出」用 `RestTemplate` 直接调 API 就够了。引入框架反而增加依赖和配置成本。等后续需要多轮对话、工具调用、记忆能力时，再考虑 LangChain4j。

---

## 三、复用发票管理模块的地方

发票模块已经完整实现了单表 CRUD，病假 AI 审批可以直接 copy 它的骨架：

| 发票模块 | 病假 AI 审批模块 | 复用程度 |
|---|---|---|
| `InvoiceInfo.java` | `AiLeaveRequest.java` | 字段不同，结构照抄 |
| `InvoiceInfoBo.java` | `AiLeaveRequestBo.java` | 结构照抄 |
| `InvoiceInfoVo.java` | `AiLeaveRequestVo.java` | 增加 AI 相关字段 |
| `InvoiceInfoMapper.java` | `AiLeaveRequestMapper.java` | 结构照抄 |
| `InvoiceInfoMapper.xml` | `AiLeaveRequestMapper.xml` | 结构照抄 |
| `IInvoiceInfoService.java` | `IAiLeaveRequestService.java` | 增加 AI 审核方法 |
| `InvoiceInfoServiceImpl.java` | `AiLeaveRequestServiceImpl.java` | 增加 AI 调用逻辑 |
| `InvoiceInfoController.java` | `AiLeaveRequestController.java` | 增加提交/确认接口 |
| 前端 `invoice/info/index.vue` | `ai/leave/index.vue` | 页面结构照抄，增加 AI 建议展示 |

> 复用不是复制粘贴后不改，而是「字段和逻辑换一换，骨架保持一致」。

---

## 四、数据库表设计

### ai_leave_request（病假申请表）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `tenant_id` | VARCHAR(20) | 租户 ID（先预留） |
| `user_id` | BIGINT | 申请人 ID |
| `dept_id` | BIGINT | 部门 ID |
| `start_time` | DATETIME | 请假开始时间 |
| `end_time` | DATETIME | 请假结束时间 |
| `reason` | VARCHAR(500) | 请假事由 |
| `attachment_url` | VARCHAR(500) | 病假条图片 URL |
| `status` | TINYINT | 状态：0待审核 1AI已通过 2AI已拒绝 3AI建议补材料 4AI转人工 5已通过 6已拒绝 |
| `ai_suggestion` | TINYINT | AI 建议：1通过 2拒绝 3补材料 4转人工 |
| `ai_reason` | TEXT | AI 判断依据 |
| `ai_confidence` | DECIMAL(3,2) | AI 置信度 |
| `ai_raw_response` | TEXT | AI 原始返回（用于排查） |
| `confirm_result` | TINYINT | 管理员最终确认：1通过 2拒绝 3补材料 |
| `confirm_remark` | VARCHAR(500) | 管理员备注 |
| `confirm_time` | DATETIME | 确认时间 |
| `confirm_user_id` | BIGINT | 确认人 ID |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |
| `create_by` / `update_by` | VARCHAR(64) | 标准审计字段 |
| `del_flag` | TINYINT | 逻辑删除 |

---

## 五、AI 判断规则（代码/YAML 写死）

MVP 只检查病假条：

```yaml
规则名称: 病假审批形式审查
必填材料:
  - 病假条图片（attachment_url）
检查点:
  - 图片中是否出现"诊断"、"病假"、"建议休息"、"医院"等关键字
  - 图片中是否有日期信息
  - 日期是否覆盖请假起止时间
  - 是否加盖医院公章或医生签字（出现相关字样即可）
输出:
  - suggestion: 1通过 / 2拒绝 / 3补材料 / 4转人工
  - confidence: 0.0 ~ 1.0
  - reason: 判断依据说明
```

Prompt 示例（发给 Kimi）：

```text
你是一位公司行政助手，负责形式审查病假申请。
请根据用户上传的病假条图片，判断以下事项：
1. 是否包含医院名称、诊断结果、建议休息/病假等关键字；
2. 是否包含日期，且日期覆盖请假时间段 {start_time} 到 {end_time}；
3. 是否有医院公章或医生签字相关字样。

你只负责形式审查，不负责验证材料真伪。如果材料齐全且日期匹配，建议通过；如果缺少关键信息，建议补材料；如果完全看不出是病假条，建议转人工。

请严格按以下 JSON 格式输出，不要输出其他内容：
{
  "suggestion": 1,
  "confidence": 0.92,
  "reason": "病假条包含医院名称、诊断建议和日期，日期覆盖请假时间段。"
}

suggestion 取值：1=通过，2=拒绝，3=补材料，4=转人工。
confidence 取值：0.00 到 1.00。
```

---

## 六、流程时序

```
普通员工
   │
   ▼
填写病假申请 + 上传病假条
   │
   ▼
提交 → Controller → Service 保存记录（status=0 待审核）
   │
   ▼
调用 AI 审核服务
   │
   ▼
AI 返回 suggestion / confidence / reason
   │
   ▼
更新记录 status（1/2/3/4）并保存 ai_raw_response
   │
   ▼
通知管理员：有新的 AI 待确认申请
   │
管理员上线
   │
   ▼
打开「病假 AI 审批」列表，看到 AI 建议
   │
   ▼
一键确认：通过 / 拒绝 / 补材料
   │
   ▼
更新记录 status（5/6/3），记录 confirm 信息
   │
   ▼
通知申请人结果
```

---

## 七、接口设计

### 员工端

| 接口 | 方法 | 说明 |
|---|---|---|
| `/ai/leave` | GET | 查询自己的请假列表 |
| `/ai/leave/{id}` | GET | 查看详情 |
| `/ai/leave` | POST | 提交病假申请（同步触发 AI 审核） |
| `/ai/leave` | PUT | 修改未确认的申请 |
| `/ai/leave/{ids}` | DELETE | 删除自己的申请 |

### 管理端

| 接口 | 方法 | 说明 |
|---|---|---|
| `/ai/leave/manage/list` | GET | 管理员查看全部待确认列表 |
| `/ai/leave/manage/{id}` | GET | 查看详情（含 AI 建议） |
| `/ai/leave/manage/confirm` | POST | 管理员确认结果：`{id, result, remark}` |
| `/ai/leave/manage/reAudit` | POST | 重新触发 AI 审核 |

### 权限字符

- 员工：`ai:leave:query`, `ai:leave:add`, `ai:leave:edit`, `ai:leave:remove`
- 管理：`ai:leave:manage`, `ai:leave:confirm`

MVP 阶段先统一用 `ai:leave:*`，后续再细分。

---

## 八、前端页面

### 8.1 员工端：我的病假申请

复用发票管理页面结构，列表字段：

- 请假开始时间
- 请假结束时间
- 事由
- 状态（待审核 / AI建议通过 / AI建议拒绝 / 需补材料 / 已通过 / 已拒绝）
- AI 建议
- 操作：查看 / 编辑 / 删除

新增/编辑表单：

- 请假时间范围（日期时间选择器）
- 请假事由（文本框）
- 病假条图片上传（单张图片）

### 8.2 管理端：AI 审批确认

列表字段：

- 申请人
- 部门
- 请假时间
- AI 建议（通过 / 拒绝 / 补材料 / 转人工）
- AI 置信度
- AI 判断依据
- 操作：查看 / 通过 / 拒绝 / 补材料

查看详情弹窗：

- 左侧：申请表单信息 + 病假条图片
- 右侧：AI 建议卡片（高亮显示）
- 底部：确认按钮 + 备注输入框

---

## 九、实现步骤

### Step 1：建库建表

在 MySQL 中执行 `ai_leave_request` 建表 SQL。

### Step 2：后端模块搭建

1. 复制发票模块代码到 `ruoyi-modules/ruoyi-ai-leave`。
2. 修改包名、类名、字段。
3. 新增 `AiLeaveAuditService` 负责调用 Kimi API。
4. 新增管理员确认接口。
5. 配置 Kimi API Key（先放 `application.yml`，后续迁移到参数管理）。

### Step 3：前端页面

1. 复制 `src/views/invoice/info/index.vue` 到 `src/views/ai/leave/index.vue`。
2. 修改字段和表单。
3. 新增管理端页面或同一页面通过角色区分视图。

### Step 4：菜单和权限

在系统管理 → 菜单管理中新增：

- 目录：AI 办公
  - 菜单：病假审批（员工端）
  - 菜单：AI 审批确认（管理端）

### Step 5：联调测试

1. 员工提交一张真实病假条照片。
2. 查看 AI 返回的结论和置信度。
3. 管理员点击确认。
4. 检查通知和日志。

---

## 十、风险与应对

| 风险 | 应对 |
|---|---|
| Kimi API 调用失败 | try-catch，status 保持「待审核」，记录错误日志，管理员可手动触发重审 |
| AI 返回格式不对 | 用 JSON Schema / strict mode 约束；解析失败时转人工 |
| AI 误判 | 置信度低于 0.8 自动转人工；管理员可随时推翻 |
| 图片太大 | 上传时压缩，或限制单张 5MB |
| API Key 泄露 | 后续迁移到系统参数管理，不在代码中 hardcode |
| 成本过高 | MVP 阶段调用量小；后续可加缓存，相同图片不再重复识别 |

---

## 十一、后续扩展方向

1. 接入 Warm-Flow，把 AI 审批变成工作流中的一个节点。
2. 前端增加「AI 规则配置」页面，业务管理员自己配置检查点。
3. 增加多租户下 AI 资源分配。
4. 扩展到事假、采购、报销等其他审批类型。
5. 接入企业微信/钉钉/飞书通知。
6. 引入 LangChain4j，实现多轮对话、工具调用、记忆能力。

---

## 十二、需要用户确认的事

1. **Kimi API Key**：你是否有可用的 Moonshot/Kimi API Key？没有的话需要申请一个。
2. **模型选择**：`kimi-k2.5` 够用且便宜，是否先用这个？
3. **模块名**：`ruoyi-ai-leave` 是否 OK？
4. **先做管理端还是员工端一起做？** 建议一起做，因为闭环最短。
5. **是否立刻开始写代码？** 方案 OK 的话我直接开干。
