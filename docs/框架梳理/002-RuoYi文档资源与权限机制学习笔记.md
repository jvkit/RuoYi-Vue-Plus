# 002 RuoYi 文档资源与权限机制学习笔记

> 角标：002  
> 阶段：只学习、梳理、讨论，不改代码  
> 目标：先把 RuoYi 6.x 能做什么、不能做什么摸清楚，再决定我们的权限/部门/项目模型怎么设计。

---

## 一、已找到的核心文档资源

### 1.1 官方文档站

| 资源 | 地址 | 说明 |
|---|---|---|
| plus-doc 文档门户 | [plus-doc.dromara.org](https://plus-doc.dromara.org/#/_readme) | 总入口，含 RuoYi-Vue-Plus / RuoYi-Cloud-Plus / plus-ui |
| RuoYi-Vue-Plus 文档 | [plus-doc.dromara.org/#/ruoyi-vue-plus/readme](https://plus-doc.dromara.org/#/ruoyi-vue-plus/readme) | 当前项目用的后端版本 |
| plus-ui 前端文档 | [plus-doc.dromara.org/#/plus-ui/readme](https://plus-doc.dromara.org/#/plus-ui/readme) | 当前项目用的前端版本 |
| Warm-Flow 官网 | [www.warm-flow.com](https://www.warm-flow.com/master/introduction/introduction.html) | 工作流引擎官方文档 |
| Sa-Token 官方 | [sa-token.cc](https://sa-token.cc/doc.html#/) | 权限认证框架官方文档 |

### 1.2 文档源码仓库（可直接拉取离线看）

```bash
# plus-doc 是 docsify 写的 Markdown，可直接 clone
# 地址：https://github.com/dromara/plus-doc
# 本地启动 docsify 即可离线浏览
git clone https://github.com/dromara/plus-doc.git
cd plus-doc
docsify serve
```

仓库结构（和官网一一对应）：

```
plus-doc/
├── ruoyi-vue-plus/        # 后端文档
│   ├── home.md            # 项目简介
│   ├── changlog.md        # 更新日志
│   ├── quickstart/        # 快速开始（含工作流初始化）
│   ├── framework/         # 框架功能
│   │   ├── basic/         # 基础功能
│   │   │   ├── permissions.md        # 数据权限
│   │   │   ├── permissions_control.md # 功能权限/角色权限
│   │   │   ├── user.md                # 系统用户相关
│   │   │   ├── database.md            # 数据库表设计
│   │   │   └── ...
│   │   ├── explain/       # 功能说明
│   │   ├── extend/        # 扩展功能
│   │   └── association/   # 框架相关
│   └── ...
├── ruoyi-cloud-plus/      # 微服务版文档
└── plus-ui/               # 前端文档
```

### 1.3 是否拉取到本地？

**建议拉取**。原因：

1. 文档不大（约 2MB），离线看方便。
2. 可以直接用 VSCode / Obsidian 搜索关键词。
3. 后续我写设计文档时可以直接引用文件路径。
4. 不用每次联网查。

**操作**：

```bash
cd /home/jvkit/workspace/oa/docs
mkdir -p 外部文档
# 不建议把 plus-doc 直接放进我们仓库，可以用 git submodule 或单独 clone
git clone https://github.com/dromara/plus-doc.git 外部文档/plus-doc
```

然后在外层 `.gitignore` 里加一行：

```
docs/外部文档/
```

这样既能在本地查，又不污染我们仓库。

---

## 二、RuoYi 权限机制：完整版

### 2.1 一句话总结

RuoYi 的权限 = **功能权限（角色）+ 数据权限（角色.data_scope + 部门）+ 流程办理人（用户/角色/部门/岗位/SpEL）**。三者是独立的。

### 2.2 功能权限：只认角色

依据：`ruoyi-vue-plus/framework/basic/permissions_control.md`

- 每个菜单/按钮对应一个权限码 `perms`，如 `system:user:list`。
- 权限码通过**角色**分配给用户。
- 后端用 `@SaCheckPermission("system:user:list")` 校验。
- 前端用 `v-hasPermi` / `v-hasRoles` 控制显隐（只是界面控制）。
- 支持通配符 `*`。
- 支持权限 OR/AND 校验：

```java
@SaCheckPermission(
    value = {"system:user:list", "system:user:query"},
    mode = SaMode.OR    // 或模式
)
```

**结论**：菜单权限只能通过角色分配，不能通过部门、岗位、用户直接分配。

### 2.3 角色校验：也是独立的

依据：`ruoyi-vue-plus/framework/basic/permissions_control.md`

- 每个角色有唯一的权限字符（role_key），如 `admin`、`common`。
- 后端用 `@SaCheckRole("admin")` 校验。
- 支持 OR/AND。

**结论**：角色既用于菜单权限，也用于角色身份判断。一个用户可以有多个角色。

### 2.4 数据权限：原生按部门过滤

依据：`ruoyi-vue-plus/framework/basic/permissions.md`

RuoYi 的数据权限机制：

```
用户 -> 多角色 -> 每个角色一个数据权限范围
```

六种数据范围（`DataScopeType`）：

| code | 类型 | 说明 |
|---|---|---|
| 1 | ALL | 全部数据 |
| 2 | CUSTOM | 自定义数据（角色关联部门） |
| 3 | DEPT | 本部门数据 |
| 4 | DEPT_AND_CHILD | 本部门及以下数据 |
| 5 | SELF | 仅本人数据 |
| 6 | DEPT_AND_CHILD_OR_SELF | 本部门及以下或本人 |

使用方式：

1. 在角色管理里设置角色的数据权限范围。
2. 在 Mapper 方法上加 `@DataPermission` 注解。
3. 框架自动在 SQL 里拼接 `dept_id` 过滤条件。

**关键限制**：

- 模板变量是 `#{#deptName}`，默认按部门过滤。
- 我们的业务表没有 `dept_id`，是按 `project_id` 过滤。
- 所以要按项目过滤，**必须自定义 `DataScopeType` 模板**，或者干脆不用 `@DataScope` 自己写 Wrapper。

### 2.5 岗位（post）：不是权限主体，但流程可以用

依据：代码里 `SysPost.java` / `SysUserPost.java` / `TaskAssigneeEnum.java`

- RuoYi 有 `sys_post` 岗位表和 `sys_user_post` 用户岗位关联表。
- 岗位**不直接参与菜单权限**（没有 `sys_post_menu`）。
- 岗位**可以参与工作流审批**（Warm-Flow 支持 `post:岗位ID`）。
- 岗位的意义：人事身份，用于审批分派。

**结论**：

- 岗位 = 身份标签（你是谁）。
- 角色 = 权限集合（你能干啥）。
- 部门 = 组织归属（你在哪）。
- 用户 = 最终操作人。

### 2.6 流程办理人：五种方式

依据：`TaskAssigneeEnum.java` + `FlwTaskAssigneeServiceImpl.java`

Warm-Flow 节点办理人支持：

| 类型 | 前缀 | 示例 | 说明 |
|---|---|---|---|
| 用户 | `user:` 或省略 | `user:100` / `100` | 指定某个人 |
| 角色 | `role:` | `role:5` | 拥有该角色的人 |
| 部门 | `dept:` | `dept:3` | 该部门下的人 |
| 岗位 | `post:` | `post:4` | 该岗位下的人 |
| SpEL | `${}` / `#{}` | `${leaderId}` | 动态取流程变量 |

**办理人解析逻辑**：

```java
// FlwTaskAssigneeServiceImpl.getUsersByType
switch (type) {
    case USER -> userService.selectListByIds(longIds);
    case ROLE -> userService.selectUsersByRoleIds(longIds);
    case DEPT -> userService.selectUsersByDeptIds(longIds);
    case POST -> userService.selectUsersByPostIds(longIds);
}
```

**结论**：流程审批人可以按角色、部门、岗位、用户、变量五种维度配置，比菜单权限灵活得多。

---

## 三、回答你的具体问题

### 3.1 "RuoYi 只有通过部门与角色来管控权限吗？感觉有点垃圾"

**不是**。完整机制是：

- **功能权限**：只通过角色。
- **数据权限**：通过角色的 `data_scope` + 部门。
- **流程审批人**：通过用户/角色/部门/岗位/SpEL。

所以你说"只有部门和角色"不完全对，岗位在流程里是能用的。但菜单权限确实只认角色。

说它"垃圾"也不至于，这是 RBAC 的标准设计，只是对我们的**项目制业务**不够贴身。需要扩展的是数据权限维度（从部门扩展到项目）。

### 3.2 "职位是干啥的？支持其他方式吗？"

职位/岗位在 RuoYi 里的定位：

- 不是权限主体（不能直接控制菜单）。
- 是人事身份标识。
- 可以被 Warm-Flow 用来指派审批任务。

"其他方式"指的是什么？

- 如果指菜单权限：不支持岗位，只支持角色。
- 如果指审批人：支持用户、角色、部门、岗位、SpEL 表达式。
- 如果指数据权限：原生只支持按部门过滤，可以自定义模板扩展到项目。

### 3.3 "不操作角色权限，而是操作事务权限，可行吗？"

"事务权限"如果指的是**对象级权限**（谁能操作哪张采购单、哪个项目），RuoYi 原生不支持，需要自己实现。

可行方案：

1. **在业务表里加权限字段**：

```sql
pms_project
  - leader_id          -- 项目负责人
  - member_user_ids    -- 项目成员（JSON）
  - member_role_ids    -- 可见角色（JSON）
```

2. **查询时自定义过滤**：

```java
// 伪代码
List<Long> visibleProjectIds = ...; // 从自己参与的项目中算
queryWrapper.in("project_id", visibleProjectIds)
            .or().eq("create_by", userId);
```

3. **接口层加业务校验**：

```java
// 查看某张采购单前，校验是否与自己相关
if (!canView(requestId, userId)) {
    throw new ServiceException("无权查看");
}
```

**我的判断**：

- 完全抛弃 RuoYi 的角色权限，全面转向事务权限，**成本太高**，每个接口都要重写。
- 更务实的做法是：**功能权限继续用角色，数据权限补充项目级过滤**。
- 这样既能遵循 RuoYi 规范，又能满足项目制需求。

### 3.4 "采购申请按钮如果无权化，会怎样？"

如果你把 `procurement:request:submit` 从所有角色移除：

- 前端按钮隐藏。
- 后端接口返回 403。
- 没人能提交采购申请。

如果你绕过权限校验，让所有人都能调接口：

- 前端按钮可能不存在，体验不一致。
- 安全上依赖"前端隐藏"，不可靠。

**正确的做法不是"无权化"，而是"默认有权"**：

- 所有用户自动关联一个 `default_user` 角色。
- `default_user` 包含基础操作权限：提交申请、提交验收、提交领用、查看自己的单据。
- 审批、管理、全部数据查看等高级权限再单独分配角色。

### 3.5 "可以把角色权限打包，分配给对象吗？"

**角色本身就是一种权限打包**。一个角色 = 一组菜单/按钮权限的集合。

如果你想把**多个角色再打包**分配给对象，有几种方案：

#### 方案 A：一个用户挂多个角色（RuoYi 原生支持）

```
王建龙：common_user + team_leader + project_manager
```

**优点**：无需改代码。  
**缺点**：用户角色多的时候管理麻烦。

#### 方案 B：岗位-角色映射（推荐未来做）

加一张 `sys_post_role` 表：

```sql
sys_post_role
  - post_id
  - role_id
```

给某岗位分配一组角色，用户关联岗位即自动获得角色。

```
岗位：团队上级
  → 角色：common_user + team_leader + project_manager
```

**优点**：

- 人事变动只需改岗位。
- 岗位是身份，角色是权限，职责清晰。

**缺点**：

- 需要改用户管理逻辑，创建用户时同步角色。

#### 方案 C：角色组（更抽象）

加 `sys_role_group` 表，用户关联角色组。

**优点**：批量赋权。  
**缺点**：多一层抽象，复杂度更高。

**当前建议**：先用方案 A，等人员规模变大后再做方案 B。

---

## 四、关于"项目是否应该挂在部门下"

这是你今天提到的一个关键问题。我们需要结合 RuoYi 规范来判断。

### 4.1 RuoYi 的部门设计

RuoYi 的 `sys_dept` 是**组织机构树**：

```
公司
  └── 研发中心
        └── 前端组
        └── 后端组
  └── 财务部
        └── 会计组
```

部门用途：

1. 用户归属（`sys_user.dept_id`）。
2. 数据权限过滤（`@DataScope` 按部门）。
3. 流程审批人按部门分派（`dept:部门ID`）。

### 4.2 我们当前的问题

当前 `pms_project` 有 `dept_id` 字段，表示"项目归属部门"。但你说：

> "归属于部门，但是与部门无关，相当于只是用了它的名字"

意思是："长三角"、"天目湖"、"北京"这些不是真正的部门，只是资金来源/项目归属。

### 4.3 两种设计思路

#### 思路 A：把"长三角/天目湖/北京"建成部门（符合 RuoYi 规范）

```
sys_dept
  └── 长三角
  └── 天目湖
  └── 北京
```

项目 `pms_project.dept_id` 指向这些"部门"。

**优点**：

- 完全遵循 RuoYi 规范。
- 可以直接用 `@DataScope` 做数据权限。
- 可以直接用 `dept:` 做流程审批人。
- 项目负责人、部门负责人都在 RuoYi 的框架内。

**缺点**：

- "长三角"不是传统意义上的部门，概念上有点别扭。
- 如果以后部门结构复杂了，项目和真实部门混在一起会乱。

#### 思路 B：项目独立，另建资金来源表

```sql
pms_fund_source
  - id
  - source_name    -- 长三角、天目湖、北京
  - parent_id      -- 支持层级

pms_project
  - fund_source_id  -- 关联资金来源
  - dept_id         -- 可选，关联真实部门（用于审批线）
```

**优点**：

- 概念清晰，项目归属和部门解耦。
- 后续按资金来源统计预算更准确。

**缺点**：

- 数据权限和流程审批人都需要自己扩展，不能直接用 RuoYi 原生。
- 需要写更多自定义代码。

### 4.4 我的建议

**如果追求"尽可能遵循 RuoYi 规范"，选思路 A**：把"长三角/天目湖/北京"作为部门建进去。

理由：

1. RuoYi 的数据权限、流程审批人都是围绕部门设计的。
2. 既然原 OA 里项目也属于部门，说明这个抽象在原系统里是成立的。
3. 我们只是把"部门"的含义从"行政组织"扩展为"项目归属/资金来源"，这在 RuoYi 的模型里完全可行。
4. 后续如果要把项目负责人作为审批人，可以直接用 `${leaderId}` 变量，不需要改部门模型。

**但要做一个区分**：

- **真实行政部门**（如财务部、研发部）：用于人事归属、汇报线。
- **项目/资金部门**（如长三角、天目湖）：用于项目归属、资金统计、数据权限。

这两类可以共存于 `sys_dept`，只是类型不同（可加一个 `dept_type` 字段区分，或用父节点区分）。

---

## 五、RuoYi 权限机制对我们设计的启示

### 5.1 应该遵循的

1. **功能权限用角色**：菜单/按钮权限通过角色分配，这是 RuoYi 标准。
2. **数据权限可以考虑用部门**：如果把项目归属建成部门，就能复用 `@DataScope`。
3. **流程审批人用岗位/角色/SpEL**：Warm-Flow 原生支持，不要硬编码用户 ID。
4. **用户-多角色**：一个用户可以挂多个角色，这是 RuoYi 原生支持的"权限打包"。

### 5.2 需要扩展的

1. **项目负责人的动态审批**：用 `${leaderId}` SpEL 变量，从 `pms_project.leader_id` 取。
2. **默认角色**：创建用户时自动关联 `default_user`。
3. **如果需要项目与部门解耦**：自定义 `pms_fund_source` 和项目级数据权限过滤。

### 5.3 不建议做的

1. **全面事务权限**：成本高，和 RuoYi 原生机制冲突大。
2. **岗位控制菜单权限**：需要改 RuoYi 核心，收益不大。
3. **完全抛弃角色**：流程审批人可以用岗位/部门，但菜单权限仍然需要角色。

---

## 六、建议的下一步

1. **拉取 plus-doc 到本地**：方便离线查阅。
2. **重点精读以下几篇**：
   - `ruoyi-vue-plus/framework/basic/permissions.md`（数据权限）
   - `ruoyi-vue-plus/framework/basic/permissions_control.md`（功能权限）
   - `ruoyi-vue-plus/framework/basic/user.md`（LoginUser API）
   - `ruoyi-vue-plus/quickstart/` 下的工作流初始化文档
   - [Warm-Flow 办理人表达式](https://www.warm-flow.com/master/advanced/variableStategy.html)
3. **确认关键决策**：
   - 是否把"长三角/天目湖/北京"作为部门建在 `sys_dept` 里？
   - 是否引入 `default_user` 默认角色？
   - 数据权限是用 RuoYi 原生 `@DataScope`（需把项目归属建成部门），还是自己写项目级过滤？

---

## 七、待确认决策（002）

| 序号 | 决策点 | 当前推荐 |
|---|---|---|
| 002-1 | 是否拉取 plus-doc 到本地 `docs/外部文档/` 并加 `.gitignore`？ | 是 |
| 002-2 | 是否把"长三角/天目湖/北京"作为 `sys_dept` 部门建立？ | 倾向是 |
| 002-3 | 是否保留 `pms_project.dept_id` 作为项目归属部门？ | 若 002-2 成立，则保留 |
| 002-4 | 是否新增 `default_user` 默认角色，新用户自动关联？ | 是 |
| 002-5 | 数据权限用原生 `@DataScope` 还是自己写项目级过滤？ | 若 002-2 成立，用 `@DataScope`；否则自己写 |
| 002-6 | 流程审批人优先用 `post:` 还是 `role:`？ | 岗位稳定后用 `post:`，当前可用 `role:` |
| 002-7 | 是否当前先做"多角色"方案，后续再做"岗位-角色映射"？ | 是 |

---

*请重点确认 002-2（项目是否建在部门下）和 002-5（数据权限方式）。这两个决策会直接影响数据库设计和代码实现方向。*
