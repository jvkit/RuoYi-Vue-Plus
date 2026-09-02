# 000 知识点笔记

> 角标：000（元笔记，持续更新）  
> 作用：学习 RuoYi / Warm-Flow / Sa-Token 等框架过程中，记录关键知识点、易混淆点、与我们项目的关联。  
> 更新规则：学到哪记到哪，按模块分节，条目化记录。

---

## 一、RuoYi 权限体系核心认知

### 1.1 三大权限维度

RuoYi 的权限不是单一概念，而是分成三个独立维度：

| 维度 | 控制什么 | 由什么决定 | 典型使用 |
|---|---|---|---|
| 功能权限 | 能不能看到页面/按钮/调接口 | **角色** | `sys_menu.perms` + `@SaCheckPermission` |
| 数据权限 | 能看到哪些数据行 | **角色的 data_scope + 部门** | `@DataScope` + `sys_role.data_scope` |
| 流程权限 | 审批任务派给谁 | **用户/角色/部门/岗位/SpEL** | Warm-Flow `permission_flag` |

**一句话**：角色管功能，部门管数据，流程办理人可以灵活组合。

### 1.2 部门（sys_dept）

- 部门是一棵**组织机构树**。
- 每个用户有一个主部门 `sys_user.dept_id`。
- 数据权限默认按部门过滤。
- 部门也可以用于流程审批人（`dept:部门ID`）。
- **关键洞察**：RuoYi 把"部门"当作数据可见性的核心维度。如果要让项目成为数据可见维度，最规范的做法是把项目也建成部门（或挂到部门下）。

### 1.3 岗位（sys_post）

- 岗位是人事身份，如"团队上级"、"部门上级"、"仓库管理员"。
- 一个用户可以有多个岗位（`sys_user_post`）。
- 岗位**不直接控制菜单权限**（没有 `sys_post_menu`）。
- 岗位主要用于**流程审批人**（`post:岗位ID`）。
- **关键洞察**：岗位适合做"身份"，角色适合做"权限包"。

### 1.4 角色（sys_role）

- 角色是功能权限的载体。
- 一个角色对应一组菜单/按钮（`sys_role_menu`）。
- 一个用户可以有多个角色（`sys_user_role`）。
- 角色还有 `data_scope`，决定数据可见范围。
- **关键洞察**：RuoYi 里角色同时承担了"功能权限"和"数据权限范围"两个职责，这是我们觉得"怪怪的"原因之一。

### 1.5 用户（sys_user）

- 用户是最终操作人。
- 用户属于一个主部门。
- 用户可以关联多个角色、多个岗位。
- 登录后，Token 里会携带：`userId`、`userName`、`deptId`、`deptName`、`tenantId` 等。

---

## 二、功能权限详解

### 2.1 权限标识 perms

- 每个菜单/按钮在 `sys_menu` 表里有一个 `perms` 字段。
- 格式：`模块:功能:操作`，如 `system:user:list`、`procurement:request:submit`。
- 支持通配符 `*`，如 `system:user:*`。

### 2.2 后端校验

两种写法：

```java
// 注解
@SaCheckPermission("system:user:list")

// 工具类
StpUtil.checkPermission("system:user:list");
StpUtil.hasPermission("system:user:list"); // 返回 boolean
```

### 2.3 前端控制

```vue
<!-- 有权限才显示按钮 -->
<el-button v-hasPermi="['procurement:request:add']">新增</el-button>

<!-- 有角色才显示 -->
<el-button v-hasRole="['admin']">管理员操作</el-button>
```

**注意**：前端隐藏只是体验优化，真正安全靠后端 `@SaCheckPermission`。

### 2.4 OR/AND 模式

```java
// 满足任意一个即可
@SaCheckPermission(value = {"a:list", "a:query"}, mode = SaMode.OR)

// 必须满足所有
@SaCheckPermission(value = {"a:list", "a:query"}, mode = SaMode.AND)
```

### 2.5 角色权限双重 OR

```java
// 有权限码，或者属于 admin 角色，都可以通过
@SaCheckPermission(value = "system:user:list", orRole = "admin")
```

---

## 三、数据权限详解

### 3.1 核心组件

| 类/注解 | 作用 |
|---|---|
| `@DataPermission` | 开启数据权限注解，加在 Mapper 方法上 |
| `DataScopeType` | 定义 6 种数据范围模板 |
| `PlusDataPermissionInterceptor` | SQL 拦截器 |
| `PlusDataPermissionHandler` | 生成数据权限 SQL |
| `DataPermissionHelper` | 临时忽略数据权限 |
| `ISysDataScopeService` | 自定义 Bean 扩展 |

### 3.2 六种数据范围

| code | 名称 | 实际效果 |
|---|---|---|
| 1 | 全部数据 | 无过滤 |
| 2 | 自定义数据 | 只查看角色关联部门的数据 |
| 3 | 本部门数据 | `dept_id = 当前用户部门` |
| 4 | 本部门及以下 | `dept_id in (本部门及子部门)` |
| 5 | 仅本人数据 | `create_by = 当前用户ID` |
| 6 | 本部门及以下或本人 | 组合 |

### 3.3 使用方式

1. 角色管理里设置数据权限范围。
2. Mapper 方法加注解：

```java
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "create_by")
})
List<PmsProject> selectList(...);
```

3. 框架自动拼接 SQL。

### 3.4 忽略数据权限

```java
// Mapper 层忽略
@InterceptorIgnore(dataPermission = "true")

// 业务层临时忽略
DataPermissionHelper.ignore(() -> { 业务代码 });
```

### 3.5 自定义数据权限模板

可以在 `DataScopeType` 枚举里新增自定义模板，用 SpEL 调用 Bean：

```java
CUSTOM_PROJECT("7", " #{#deptName} IN ( #{@sdss.getRoleCustom(#roleId)} ) ", " 1 = 0 ");
```

---

## 四、Warm-Flow 流程基础

### 4.1 Warm-Flow 是什么

- 国产轻量级工作流引擎。
- 仅 7 张表，集成简单。
- 支持通过 jar 包引入流程设计器。
- 支持经典模式和仿钉钉模式。

### 4.2 核心概念

| 概念 | 说明 |
|---|---|
| 流程定义（Definition） | 流程模板，如"采购申请流程" |
| 流程实例（Instance） | 一次具体流程运行，如"张三的采购申请" |
| 任务（Task） | 流程实例中的待办节点 |
| 节点（Node） | 流程定义中的步骤，如申请、审批、网关 |
| 跳转（Skip） | 节点之间的连线 |

### 4.3 办理人五种方式

| 类型 | 前缀 | 示例 |
|---|---|---|
| 用户 | `user:` 或省略 | `user:100` / `100` |
| 角色 | `role:` | `role:5` |
| 部门 | `dept:` | `dept:3` |
| 岗位 | `post:` | `post:4` |
| SpEL | `${}` / `#{}` | `${leaderId}` |

### 4.4 办理人表达式

- 默认策略：`${handler}`，流程变量替换。
- SpEL 策略：`#{@user.evalVar(#handler)}`，可调用 Spring Bean。
- 变量在节点前任意任务办理时传入。

### 4.5 条件表达式

- 用于网关节点（如排他网关）。
- 格式：`lt@@amount|1000` 表示 `amount < 1000`。
- 支持：`eq/ne/gt/ge/lt/le/like/not_like`。

### 4.6 监听器

Warm-Flow 提供四种监听器：

1. 流程监听器：流程创建、完成、删除。
2. 节点监听器：节点进入、离开。
3. 任务监听器：任务创建、完成、转办。
4. 跳转监听器：连线执行前后。

**与我们项目的关联**：可以用监听器发布领域事件，解耦流程与业务。

---

## 五、Sa-Token 基础

### 5.1 Sa-Token 是什么

- 轻量级 Java 权限认证框架。
- RuoYi 用它做登录认证、权限校验、角色校验。
- 核心类：`StpUtil`、`StpLogic`。

### 5.2 常用 API

```java
// 登录
StpUtil.login(10001);

// 校验登录
StpUtil.checkLogin();

// 校验权限
StpUtil.checkPermission("user:add");
StpUtil.hasPermission("user:add");

// 校验角色
StpUtil.checkRole("admin");
StpUtil.hasRole("admin");

// 踢人下线
StpUtil.kickout(10001);
```

### 5.3 RuoYi 的封装

RuoYi 在 Sa-Token 基础上封装了 `LoginHelper`：

```java
// 获取当前登录用户
LoginUser user = LoginHelper.getLoginUser();

// 常用字段
Long userId = LoginHelper.getUserId();
String username = LoginHelper.getUsername();
Long deptId = LoginHelper.getDeptId();
String tenantId = LoginHelper.getTenantId();
```

### 5.4 Token 扩展信息

RuoYi 把常见上下文放进 token：

- `tenantId`
- `userId`
- `userName`
- `deptId`
- `deptName`
- `deptCategory`
- `clientid`

---

## 六、与我们项目的关联思考

### 6.1 权限设计

- 功能权限：用角色（`default_user`、`project_manager`、`team_leader` 等）。
- 数据权限：如果项目归属建成部门，可以直接用 `@DataScope`；否则自己写项目级过滤。
- 流程权限：项目负责人用 `${leaderId}`，审批角色/岗位用 `role:` 或 `post:`。

### 6.2 项目/部门关系

- RuoYi 的核心假设：数据可见性 = 部门。
- 如果我们要严格遵循 RuoYi，就把"长三角/天目湖/北京"建成部门。
- 如果要概念清晰，就新增 `pms_fund_source`，但数据权限要自定义。

### 6.3 流程设计

- 采购申请：`start → apply → leader → gateway_amount → team_leader/dept_leader → end`
- 验收：`start → apply → applicant → leader → team_leader → end`
- 领用：`start → apply → warehouse → end`

### 6.4 事件解耦

- Warm-Flow 监听器 + Spring Event = 流程与业务解耦。
- 资金、库存强一致操作直接调用 Service。
- 通知、日志、外部同步用事件监听。

---

## 七、待深入学习的点

- [ ] `@DataScope` 在 Mapper 层的具体生效机制
- [ ] Warm-Flow 监听器与 Spring Event 的结合方式
- [ ] RuoYi 前端路由/菜单如何由后端 `sys_menu` 驱动
- [ ] `LoginUser` 和 `LoginHelper` 的扩展方式
- [ ] `BaseMapperPlus` 和 `PageQuery` 的用法
- [ ] RuoYi 多模块如何扩展（新增 `ruoyi-xxx` 模块）

---

*本文档随学习进度持续更新。*
