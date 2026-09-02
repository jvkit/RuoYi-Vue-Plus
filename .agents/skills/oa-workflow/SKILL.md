---
name: oa-workflow
version: 2.0.0
description: "OA Warm-Flow 工作流：流程定义 SQL 结构（flow_definition/node/skip）、条件分支必须用网关节点、角色审批、流程改造标准步骤、流程监控/流程图权限。当用户要求修改审批流程、加审批节点、排查流程不动/流程图打不开时使用。"
---

# OA 工作流（Warm-Flow）

## 基本结构

流程定义存在三张表（**均带 `tenant_id='000000'`，6x 虽移除多租户但这些表保留字段**）：

```sql
SELECT flow_code, flow_name, version, is_publish, form_path FROM flow_definition WHERE del_flag='0';
-- 节点：flow_node（node_type: 0 start / 1 BETWEEN普通 / 3 SERIAL网关 / 4 PARALLEL / 5 INCLUSIVE / 2 end）
-- 连线：flow_skip（skip_type: PASS / REJECT；skip_condition 条件）
```

现有业务流程：`pms_request`（采购申请）、`pms_acceptance`（采购验收）、`pms_issue_request`（领用申请）。示例参考 `TestLeave`。

## 核心规则（违反会静默不流转）

1. **条件分支只在网关节点（node_type=3/4/5）生效**。普通 BETWEEN 节点出边只取第一条 PASS 的 `flow_skip`，不判条件。要做金额分支必须：普通节点 → 网关(SERIAL) → 条件出边。
2. `skip_condition` 格式：`<比较符>@@<流程变量>|<字面值>`，如 `lt@@amount|1000`、`ge@@amount|1000`。比较符 eq/ge/gt/le/lt/ne/like/not_like，语义以 `MathUtil.determineSize` 为准（`lt@@X|Y` 即 X<Y）。
3. 角色审批：节点 `permission_flag` 写 `role:<角色id>`，引擎经 `WorkflowPermissionHandler.convertPermissions` → `fetchUsersByStorageIds` → `selectUsersByRoleIds` 展开为拥有该角色的全部用户。**该角色下任一用户审批即通过**（Warm-Flow 会签比例 `node_ratio` 默认 0）。
4. 动态指定审批人：`permission_flag` 写 `${变量名}`（如 `${applicantId}`），提交时通过 `StartProcessDTO.variables` 传入用户 ID 字符串。
5. 第一个业务节点应是「申请人」节点，`startCompleteTask` 提交时会自动完成它。

## 当前流程设计（v5）

```
pms_request（采购申请）:
  start → apply(申请人) → leader(项目负责人) → ceo(CEO,role:team_leader)
  → gateway_amount(网关) → [lt@@amount|1000 → end | ge@@amount|1000 → supreme(最高决策人,role:dept_leader) → end]

pms_acceptance（采购验收）:
  start → apply(发起人) → applicant(采购申请人,${applicantId} 动态取申请 create_by)
  → leader(项目负责人,${leaderId}) → team_leader(CEO) → end

pms_issue_request（领用申请）:
  start → apply(发起人) → leader(项目负责人) → warehouse(仓库管理员,role:warehouse_admin) → end
```

## 流程图坐标（设计器数据格式）

流程图完全由数据库驱动（设计器 iframe 加载 `/warm-flow-ui/index.html`，源码在 Maven 依赖 `warm-flow-plugin-vue3-ui`）。**坐标是节点中心点，不是左上角**：

| 表 | 格式 | 说明 |
|---|---|---|
| `flow_node.coordinate` | `x,y` 或 `x,y\|tx,ty` | `(x,y)`=节点**中心**；`\|`后为文字坐标（通常与节点中心相同，文字自动水平居中）。普通/开始/结束节点尺寸 **100×80**，网关 **40×40** |
| `flow_skip.coordinate` | `x1,y1;x2,y2;…\|tx,ty` | 折点序列：起点=源节点**右边缘中点**（中心 x+50，y 不变），终点=目标节点**左边缘中点**（中心 x−50）；中间折点用于绕行。`\|`后为连线标签（如"通过/退回"）坐标 |
| 缺省行为 | coordinate=NULL | 连线自动直线（可能穿节点）、文字可能全部叠在画布左上角 |

布局经验（以 pms_acceptance 为正确参考）：
- 主链水平排列，节点中心间距 **250**（100 宽 + 150 空隙），如 `80,120 → 330,120 → 580,120…`
- 分支：网关出边在中间 x 处折线上/下展开，再水平进目标节点；标签放折点附近
- 退回（REJECT）线：从审批节点**底边缘**（中心 y+40）向下绕到主链下方一条公共横线（如 y=220），再回到 start 底边缘（y+48）；多条退回线用不同 y 高度分层避免重叠
- 连线文字坐标取线段中点附近；竖直短线取线段中点左侧

历史教训：曾把 `coordinate` 写成 `x,y|x,y` 且文字坐标=节点坐标的变体值导致**所有节点文字全部叠加渲染在画布左上角**（LogicFlow 解析到无效 text 坐标时的退化行为）；修正为合法坐标后即恢复正常。若出现该现象，检查 coordinate 格式与数值。

**修正流程图显示只改坐标、不动结构**：写幂等 UPDATE SQL（按 `definition_id`+`node_code` / `now_node_code`+`next_node_code` 定位），参考脚本 `ruoyi-6x/script/sql/procurement_6x_v6_flow_coordinates.sql`。改完用 Playwright 打开 `/workflow/design/index?definitionId=<id>&disabled=true&activeName=0` 截图验证（测试脚本在 `~/workspace/work_twst/app/ruoyi6x/scripts/`）。

## 流程改造标准步骤

1. **写幂等 SQL**（`procurement_6x vX_flow.sql`）：
   - `DELETE FROM flow_definition WHERE flow_code='xxx' AND tenant_id='000000'`
   - 按新 id 段 DELETE + INSERT `flow_node` / `flow_skip`（先删后插）
   - 每个节点的 `form_path` 都要设为业务详情页路由（如 `/procurement/request/detail`）
2. **流程分类关联**：`flow_category` 里建/关联分类，待办列表才能按采购筛选
3. **隐藏详情菜单**：确保审批详情隐藏菜单存在并绑给所有相关角色（见 `oa-menu-perms` skill）
4. **业务代码**：提交时传齐流程变量（如 `variables.put("applicantId", request.getCreateBy().toString())`）
5. **状态监听**：`@EventListener(condition = "#processEvent.flowCode.startsWith('pms_xxx')")` 同步业务状态
6. 改完本地验证：提交单据 → 各角色待办出现 → 审批流转 → 完成后业务状态正确

## 常见问题

| 现象 | 原因 |
|---|---|
| 提交后流程不动 | 第一个节点不是申请人节点 / 变量没传 / `startCompleteTask` 失败（看后端日志） |
| 金额分支不走预期 | 条件写在普通节点上（必须网关）/ 变量名与 skip_condition 不一致 / amount 是字符串比较 |
| 待办里点办理 404 | form_path 对应的隐藏菜单没绑给当前角色 |
| 流程图文字全叠在画布左上角 | `coordinate` 格式/数值非法（见上文坐标格式节），按 `procurement_6x_v6_flow_coordinates.sql` 的写法修正 |
| 流程图连线穿节点/拐弯怪异 | `flow_skip.coordinate` 缺折点或折点算错；连线的起终点必须落在节点边缘（中心±50） |
| 流程图/流程监控打不开 | 缺 `workflow:instance:query` 权限（菜单 1761400000000011653）或流程设计隐藏菜单 1761400000000011700 未绑定 |
| 驳回后单据打不开 | 业务表状态被置为终结态，详情页按状态禁用了入口；审批详情页应支持查看历史实例 |

## 业务状态同步

`ProcessEvent`（`org.dromara.workflow.api.event`）字段：`flowCode / businessId / instanceId / status / submit`。业务侧监听后更新自己的状态字段（如 `pms_acceptance.status`），完成时可触发后续动作（验收 finish → 自动入库 `stockInOnFinish`）。

当前审批人显示：查 `flow_task`（`flow_status='waiting'`）关联 `flow_user`（`type='1'` 已处理人）取 `processed_by` 转用户名，参考 `PmsFlowApproverMapper.selectCurrentApproverNames`。
