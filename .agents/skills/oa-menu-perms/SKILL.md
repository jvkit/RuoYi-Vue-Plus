---
name: oa-menu-perms
version: 2.0.0
description: "OA 菜单/权限/角色体系：菜单驱动路由机制、新页面三件套（菜单+组件+角色绑定）、「没有访问权限」排查流程、角色设计约定。当用户报告页面 404、无权访问、菜单显示不全/显示多余、需要调整角色可见范围时使用。"
---

# OA 菜单 / 权限 / 角色

## 机制核心（一句话）

前端业务路由由后端 `sys_menu` 表驱动：登录后 `GET /system/menu/getRouters` 返回菜单树 → 前端 `filterAsyncRouter` 用 `import.meta.glob` 扫 `src/views/**/*.vue` 按 `component` 字段映射组件。**菜单没绑给角色 → 路由不生成 → 404；接口权限按钮没绑 → 前端能打开但 API 403「没有访问权限」。**

## 新增页面的三件套（缺一不可）

1. **组件文件**：`plus-ui-6x/src/views/<路径>/index.vue`，路径与菜单 `component` 字段一一对应
2. **菜单记录**（SQL 脚本）：
   - 菜单类型 `M` 目录 / `C` 页面 / `F` 按钮
   - 页面菜单的 `perms` 如 `procurement:xxx:list` 对应后端 `@SaCheckPermission`
   - 按钮菜单（F）挂在页面菜单下，提供接口级权限
3. **角色绑定**：`sys_role_menu` 插入目标角色 → 菜单 ID

## 隐藏页面（审批详情页）的约定

- `visible='1'` 隐藏，但仍生成路由
- **必须挂在目录（M）下且 path 带子路径**（如 `acceptance/detail`），目录才会递归生成路由
- 例：`1801067 采购验收审批详情`（父 1801000、path `acceptance/detail`、component `procurement/acceptance/detail`）
- 用途：「我的任务」点办理/查看 → `router.push(formPath)` 跳转，formPath 取 `flow_definition.form_path`
- 隐藏菜单必须绑给**所有可能审批/发起该流程的角色**，否则那些用户点待办就 404

## 「没有访问权限，请联系管理员授权」排查流程

这是后端 `@SaCheckPermission` 403，说明**页面打开了但某个接口权限缺失**：

1. 打开浏览器 Network（或 Playwright 监听 403 响应），找到 403 的接口 URL
2. 在后端代码搜该 URL 对应 Controller 方法的 `@SaCheckPermission("xxx:yyy:zzz")`
3. 查这个权限串对应的按钮菜单：
   ```sql
   SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE perms='procurement:project:tree';
   ```
4. 把菜单 ID 绑给目标角色（幂等 SQL）

**历史案例**：采购申请页选项目调 `/procurement/project/tree`（需 `procurement:project:tree` = 菜单 1801016），common_user 没绑 → 普通用户打开页面就弹无权限。

## 「页面 404」排查流程

1. 菜单是否绑给了当前用户的**任一角色**（多角色是并集）
2. `component` 字段与 `src/views/` 实际文件路径是否一致（大小写敏感）
3. 隐藏页是否挂在**目录**下且 path 带子路径
4. 用户是否重新登录过（路由缓存）

## 「菜单显示不全 / 显示多余」排查流程

- 显示不全 → 用户的角色缺菜单绑定（注意：superadmin 角色如果 `sys_role_menu` 绑定不全，看到的一样不全；admin 账号有代码级 all 权限，普通账号即使绑 superadmin 角色也只看绑定了的菜单）
- 显示多余 → 多角色是**并集**；老角色（v2/v3 时代绑了全套采购菜单）与 common_user 叠加就会多出供应商/BOM 等。精简时要删老角色的绑定，不只是 common_user 的
- 菜单表结构：`sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon)`

## 角色设计约定（v5 现状）

| 角色 | 定位 | 菜单范围 |
|---|---|---|
| superadmin | 管理员（李迪/admin 绑定） | 应该全量（历史问题：绑定不全待修） |
| team_leader（CEO） | 流程审批节点 | 采购核心菜单 |
| dept_leader（最高决策人） | ≥1000 元审批 | 采购核心菜单 |
| acceptance_contact（验收对接人） | 验收流程 | 采购核心菜单（历史上被绑了全套，待精简） |
| warehouse_admin（仓库管理员） | 领用审批、库存管理 | 采购核心菜单 + 库存操作 |
| common_user（普通用户） | 所有基础用户 | 首页、PRIME AI、我的任务、手机端专属、采购申请/验收、仓库库存、领用申请、发票台账、OSS 上传/下载按钮 |

**新用户默认只绑 common_user。**

权限原则（用户已确认）：角色控制菜单/功能可见性 + 流程审批节点；数据过滤暂不做（全员可见全量数据，后续再设计）。

## OSS 权限的特殊性

文件上传/下载按钮（`system:oss:upload/download`，菜单 1761400000000001601/1602）必须在「系统管理→OSS 管理」菜单树下，但**普通用户不应看到 OSS 管理菜单本身**。做法：绑按钮菜单给角色、不绑父页面菜单。历史上漏这个导致上传图片弹无权限（王建龙案例）。

## 修改菜单后的生效

- SQL 改完即生效，但**用户要退出重登**（路由和权限随登录缓存）
- 前端按钮级显隐用 `v-hasPermi` 指令，同样吃权限缓存
