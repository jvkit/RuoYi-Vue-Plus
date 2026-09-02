# Warm-Flow 工作流机制入门（带代码版）

> 前置：先读《Warm-Flow工作流机制入门（零基础版）》把概念建立起来，再看这份。
> 这份把"概念 → 代码"对应起来，逐行讲采购审批的提交、流转、状态同步。
> 建议对照着文件边读边看。
>
> 涉及代码：
> - 采购提交：`ruoyi-6x/ruoyi-modules/ruoyi-procurement/src/main/java/org/dromara/procurement/service/impl/PmsProcurementRequestServiceImpl.java`
> - 工作流封装：`ruoyi-6x/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/WorkflowServiceImpl.java`
> - 引擎启动：`ruoyi-6x/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/start/StartExecuteComponent.java`
> - 状态监听：`ruoyi-6x/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java`

---

## 一、从调用链看全貌

提交一次采购申请，代码是这样一层层调下去的：

```
前端「提交申请」按钮
   → 调接口 POST /procurement/request/submit（前端 request/index.vue 的 submitFlow）
   → PmsProcurementRequestServiceImpl.submitAndStartFlow()   ← 采购业务层
   → WorkflowService.startWorkFlow()  /  startCompleteTask() ← 工作流封装层
   → FlwTaskServiceImpl.startWorkFlow()                      ← 引擎任务服务
   → StartExecuteComponent（LiteFlow 组件，真正干活）          ← 引擎启动
```

下面一层层看。

---

## 二、采购提交方法（业务层入口）

文件：`PmsProcurementRequestServiceImpl.java:170`

```java
public PmsProcurementRequestVo submitAndStartFlow(PmsProcurementRequestBo bo) {
    // ① 保存申请单（没有 id 就新建，有 id 就更新）
    if (ObjectUtil.isNull(bo.getId())) {
        insertByBo(bo);
    } else {
        PmsProcurementRequest exist = baseMapper.selectById(bo.getId());
        BusinessStatusEnum.checkStartStatus(exist.getStatus());  // 检查当前状态能否提交
        updateByBo(bo);
    }

    PmsProcurementRequest request = baseMapper.selectById(bo.getId());
    checkBudget(bo);  // ② 校验资金：总金额 ≤ 项目剩余资金

    PmsProject project = projectMapper.selectById(request.getProjectId());
    if (project == null || project.getLeaderId() == null) {
        throw new ServiceException("请先为项目配置负责人（用户）");  // ③ 必须有负责人
    }

    // ④ 组装"启动流程"的请求
    StartProcessDTO startProcess = new StartProcessDTO();
    startProcess.setBusinessId(request.getId().toString());   // 告诉引擎：这个实例对应哪张业务单
    startProcess.setFlowCode("pms_request");                  // 用哪张图纸（流程定义）
    Map<String, Object> variables = new HashMap<>();
    variables.put("leaderId", project.getLeaderId().toString());  // 图纸里 ${leaderId} 用这个
    variables.put("contactId", "1761100000000000001");            // 图纸里 ${contactId} 用这个
    startProcess.setVariables(variables);

    // ⑤ 启动流程 ← 关键点在这里！
    StartProcessReturnDTO result = workflowService.startWorkFlow(startProcess);

    // ⑥ 手动把业务状态改成"待审核"，并记录流程实例ID
    request.setStatus(BusinessStatusEnum.WAITING.getStatus());
    request.setProcessInstanceId(result.processInstanceId());
    baseMapper.updateById(request);
    return queryById(request.getId());
}
```

### 逐段解释

- **① 保存申请单**：先落库，才有 `requestId` 可给引擎用。
- **② 校验资金**：业务规则，跟引擎无关。
- **③ 必须有项目负责人**：因为流程里 `leader` 节点要用 `leaderId` 这个变量，没有负责人就没法派给谁。
- **④ 组装启动请求**：
  - `businessId` = 业务单 id，让引擎的实例能回溯到这张采购单
  - `flowCode` = `pms_request`，就是数据库 `flow_definition` 里那张流程定义
  - `variables` = 流程变量，图纸上的 `${leaderId}`、`${contactId}` 就靠这里填充
- **⑤ 启动流程**：调 `workflowService.startWorkFlow()` —— **这里就是 bug 所在**（见第五节）。
- **⑥ 手动改业务状态**：⚠️ 这一行**试图手动**把业务状态改成 waiting。

### 关键认知

第 ⑤ 和第 ⑥ 是**互相矛盾的**：
- 第 ⑤ 只用 `startWorkFlow`（只建实例 + 首任务，引擎内部还是"待提交"）
- 第 ⑥ 却想手动把业务状态改成"待审核"（waiting）

**业务状态是你手动改的，引擎那边其实还没走到"待审核"** —— 两边对不上，这正是 bug 的根源。

---

## 三、`startWorkFlow` 到底干了什么

### 3.1 封装层

文件：`FlwTaskServiceImpl.java:108`

```java
public StartProcessReturnDTO startWorkFlow(StartProcessBo startProcessBo) {
    StartProcessContext context = new StartProcessContext(startProcessBo);
    LiteFlowUtils.execute(START_PROCESS_CHAIN, context);   // 走一条 LiteFlow 编排链
    return context.getStartProcessReturn();
}
```

它通过 LiteFlow 跑一条"启动流程"的编排链，链上有几个组件。

### 3.2 真正干活的组件

文件：`StartExecuteComponent.java`

```java
public void process() {
    StartProcessContext context = getContextBean(StartProcessContext.class);
    FlowParams flowParams = FlowParams.build()
        .handler(context.getStartProcessBo().getHandler())
        .flowCode(context.getStartProcessBo().getFlowCode())
        .variable(context.getVariables())
        .flowStatus(BusinessStatusEnum.DRAFT.getStatus());   // ← 引擎把实例初始化为"草稿"
    Instance instance = insService.start(context.getBusinessId(), flowParams);
    context.setInstance(instance);

    // 启动后查一下生成了哪些任务
    List<Task> taskList = taskService.list(new FlowTask().setInstanceId(instance.getId()));
    if (CollUtil.isEmpty(taskList)) {
        throw new ServiceException("流程启动失败，未生成任务");
    }
    if (taskList.size() > 1) {
        throw new ServiceException("请检查流程第一个环节是否为申请人！");  // 预期只有1个首任务
    }
    context.setTaskList(taskList);
    context.setStartProcessReturn(new StartProcessReturnDTO(instance.getId(), taskList.getFirst().getId()));
}
```

### 关键点

1. `.flowStatus(BusinessStatusEnum.DRAFT.getStatus())` → **引擎建实例时，初始化状态是"草稿"**。
2. `insService.start(...)` → 建实例 + **建第一个任务**（首任务）。
3. `taskList.size() > 1` 抛异常"请检查流程第一个环节是否为申请人" → 引擎**预期启动后只有 1 个首任务**，而且第一个环节应该是"申请人"（发起人自己确认提交的那个环节）。

所以 `startWorkFlow` 做完的状态是：

```
实例 = 草稿(draft)
首任务 = 已生成（通常对应"申请人确认提交"这个环节）
```

**到此为止，流程还没真正"走"起来。** 它只是把"比赛"登记上了，接力棒还在第一棒手里，第一棒还没跑。

---

## 四、`startCompleteTask` 才是完整提交

文件：`WorkflowServiceImpl.java:157`

```java
public boolean startCompleteTask(StartProcessDTO startProcess) {
    StartProcessBo processBo = new StartProcessBo();
    processBo.setBusinessId(startProcess.getBusinessId());
    processBo.setFlowCode(startProcess.getFlowCode());
    processBo.setVariables(startProcess.getVariables());
    processBo.setHandler(startProcess.getHandler());
    processBo.setBizExt(BeanUtil.toBean(startProcess.getBizExt(), FlowInstanceBizExt.class));

    StartProcessReturnDTO result = flwTaskService.startWorkFlow(processBo);  // ① 先启动

    CompleteTaskBo taskBo = new CompleteTaskBo();
    taskBo.setTaskId(result.taskId());       // ② 拿到首任务的 id
    taskBo.setMessageType(...);
    taskBo.setVariables(startProcess.getVariables());
    taskBo.setHandler(startProcess.getHandler());
    return flwTaskService.completeTask(taskBo);  // ③ 办理首任务！
}
```

### 关键点

`startCompleteTask` = **`startWorkFlow`（启动） + `completeTask`（办理首任务）**，一步到位：

```
① 启动 → 实例=草稿，生成首任务（申请人确认提交）
② 拿首任务 id
③ 办理首任务 = 申请人"确认提交" → skip 一下 → 走到负责人节点
   → 实例状态 草稿→待审核(waiting)
   → 给负责人生成待办
```

这正好补上第三节 `startWorkFlow` 没做的那步。

### 对比

| | `startWorkFlow` | `startCompleteTask` |
|---|---|---|
| 建实例 | ✅ | ✅ |
| 建首任务 | ✅ | ✅ |
| **办理首任务（真正提交）** | ❌ | ✅ |
| 实例状态 | 停在草稿 | 推进到待审核 |
| 负责人有待办 | ❌ | ✅ |

**采购现在用的是 `startWorkFlow`，所以卡在草稿、负责人没有待办。改成 `startCompleteTask` 就通了。**

---

## 五、为什么"手动改成 waiting"不行

采购代码里第 ⑥ 行写的是：

```java
request.setStatus(BusinessStatusEnum.WAITING.getStatus());  // 改的是"业务状态"
baseMapper.updateById(request);
```

它改的是**业务表**（`pms_procurement_request.status`），而：

- **待办列表**查的是流程实例的业务状态（`flow_instance.flow_status`）
- **审批办理**走的是引擎的 `skip()`，引擎内部自己算状态

所以：

1. 手动改业务单的 status → 列表页看到的是"待审核"了，但**引擎实例还是草稿**
2. 就算把 `flow_instance.flow_status` 也改成 waiting → 待办能冒出来，但 admin 点"通过"时，引擎 `skip()` 按自己规则重算状态 → 对不上 → 报错

**结论：手改状态是"骗过列表"，改代码走 `startCompleteTask` 才是"让引擎真的走完一步"。**

---

## 六、状态是怎么同步的（监听器）

### 6.1 引擎会"广播"每一步

文件：`WorkflowGlobalListener.java`（实现 `GlobalListener` 接口）

引擎每 skip 一步，会触发监听器的回调。以 `finish`（任务完成）为例：

```java
public void finish(ListenerVariable listenerVariable) {
    ...
    // 判断流程走到什么状态
    String status = determineFlowStatus(instance);
    // 通知业务方
    flowProcessEventHandler.processHandler(definition.getFlowCode(), instance, status, ...);
    ...
}
```

它把"这个流程定义（flowCode）、这个实例、走到什么状态"通过 `FlowProcessEventHandler` 抛出去。

### 6.2 业务方收到通知后更新自己

文件：`PmsProcurementRequestServiceImpl.java:211`

```java
@EventListener(condition = "#processEvent.flowCode.startsWith('pms_request')")
public void processHandler(ProcessEvent processEvent) {
    PmsProcurementRequest request = baseMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
    ...
    String oldStatus = request.getStatus();
    request.setStatus(processEvent.getStatus());   // 用引擎通知的状态覆盖业务状态
    request.setProcessInstanceId(processEvent.getInstanceId());
    ...
    if (BusinessStatusEnum.FINISH.getStatus().equals(request.getStatus())
        && !BusinessStatusEnum.FINISH.getStatus().equals(oldStatus)) {
        accumulateUsedAmount(request);   // 审批通过时累加项目已用金额
    }
}
```

### 关键点

- 业务状态**不是业务自己设的**，是引擎 skip 后**通知**业务去改的
- 所以正确流程里，`submitAndStartFlow` 根本**不需要**第 ⑥ 行手动 `setStatus(WAITING)` —— 引擎走完首任务后，监听器自然会把它改成 waiting
- 采购现在手动改了业务状态，但引擎实例还停在草稿，两边不一致 —— 这就是 bug 的直接表现

---

## 七、一张"提交动作"前后对照表

| 观察点 | 现在（startWorkFlow） | 修复后（startCompleteTask） |
|---|---|---|
| 流程实例状态 | 草稿 | 待审核 |
| 负责人是否有待办 | ❌ | ✅ |
| 业务单状态 | 手动改成 waiting（假象） | 引擎通知改成 waiting（真实） |
| admin 审批能否进行 | ❌（点了会报错） | ✅ |
| 累计项目已用金额 | 不会触发 | 审批通过时触发 |

---

## 八、修复方案（一行改动）

把 `PmsProcurementRequestServiceImpl.java:197` 的：

```java
StartProcessReturnDTO result = workflowService.startWorkFlow(startProcess);
```

改成：

```java
boolean result = workflowService.startCompleteTask(startProcess);
```

（`startCompleteTask` 返回 boolean；下面用到 `result.processInstanceId()` 的地方需要换成从其他地方取，或稍作调整——具体以编译为准。）

**注意**：
1. 这是后端 Java 改动，改完**必须重启后端**，热更新不生效。
2. 顺手把第 ⑥ 行手动 `setStatus(WAITING)` 那两行删掉——让引擎/监听器来同步状态，避免再次不一致。
3. 数据库里流程定义**实际是两级**（`start→leader→contact→end`），但 `procurement_6x.sql` 脚本里还是旧的单级 `manager` 定义——**脚本和数据库不一致**，改完功能后要找时间把脚本同步成两级（幂等），否则换环境会回到旧流程。

---

## 九、看完代码版，你该自己会做的事

1. 打开 `PmsProcurementRequestServiceImpl.java` 找到 `submitAndStartFlow`，对照本文第二节，能说出每段在干嘛
2. 打开 `WorkflowServiceImpl.java` 对比 `startWorkFlow` 和 `startCompleteTask` 两个方法
3. 打开 `WorkflowGlobalListener.java` 找 `finish`，理解"引擎 → 监听器 → 业务"这条通知链
4. 在系统里走一遍：提交 → 负责人待办 → 通过 → 对接人待办 → 通过 → 完成
5. 试着自己把第 197 行改掉，重启后端，验证流程通了

---

## 附：常见误区自查

| 误区 | 正解 |
|---|---|
| "startWorkFlow 就是提交" | 它只建实例+首任务，没办理首任务，流程没真正走起来 |
| "改业务状态 waiting 就通了" | 只骗过列表，引擎内部还停在草稿，审批会报错 |
| "业务状态是我设置的" | 业务状态是引擎 skip 后通过监听器通知业务更新的 |
| "流程是一次性创建的" | 是一步步 skip 出来的，每步一个任务，办完才走下一步 |
