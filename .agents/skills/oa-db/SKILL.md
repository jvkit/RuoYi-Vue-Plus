---
name: oa-db
version: 2.0.0
description: "OA 数据库操作：MySQL 连接（本地 docker / 服务器 ruoyi-mysql）、VSCode 插件连接参数、幂等 SQL 编写规范、菜单 ID 段位表、常用诊断 SQL。当用户要求执行 SQL、查数据库、设计新表/加列/加菜单的 SQL、排查数据问题时使用。"
---

# OA 数据库操作与 SQL 规范

## 连接信息

### 本地（开发）

```bash
# 执行 SQL 脚本（务必带 --default-character-set=utf8mb4，防中文乱码）
docker exec -i mysql mysql -uroot -proot -D ry-vue-6x --default-character-set=utf8mb4 < xxx.sql

# 交互查询
docker exec -it mysql mysql -uroot -proot -D ry-vue-6x --default-character-set=utf8mb4
```

### 服务器（生产）

```bash
# 容器名是 ruoyi-mysql，密码不同！
docker exec -i ruoyi-mysql mysql -uroot -pruoyi123 -D ry-vue-6x --default-character-set=utf8mb4 < xxx.sql
```

### VSCode 插件连接（用户偏好）

| 参数 | 本地 | 服务器 |
|---|---|---|
| Host | 127.0.0.1 | 172.16.16.110 |
| Port | 3306 | 3306 |
| User | root | root |
| Password | root | ruoyi123 |
| Database | ry-vue-6x | ry-vue-6x |

## 幂等 SQL 铁律

**任何数据库改动都必须写成 `ruoyi-6x/script/sql/` 下的幂等脚本，禁止手工改库不留脚本。**

1. **脚本第一行必须 `SET NAMES utf8mb4;`**
2. 加列/加索引用 information_schema 判断：

```sql
SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'xxx' AND column_name = 'yyy';
SET @sql = IF(@cnt = 0, 'ALTER TABLE xxx ADD COLUMN yyy ...', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

3. 菜单/字典/角色绑定用**先删后插**：`DELETE FROM sys_menu WHERE menu_id = X; INSERT ...`
4. 更新类用 UPDATE（配 WHERE 条件），存量回填要写明
5. 脚本尾部加 SELECT 校验语句，执行后人工确认结果
6. 本地应用后，同一脚本随部署上服务器（`apply-sql.sh` 增量机制见 `oa-deploy` skill）

## 脚本命名约定

`procurement_6x_v<版本>_<主题>.sql`，例如 `procurement_6x_v5_invoice_menu.sql`。

## 菜单 ID 段位表（自定义段）

| 段 | 内容 |
|---|---|
| 1801000 | 采购管理目录（父） |
| 1801010~1801016 | 项目管理（1801016 项目树选择按钮） |
| 1801020~1801025 | 供应商管理 |
| 1801030~1801035 | BOM 物料清单 |
| 1801040~1801046 | 采购申请（1801046 审批详情隐藏页） |
| 1801050~1801055 | 采购订单（已隐藏） |
| 1801060~1801067 | 采购验收（1801066 提交、1801067 审批详情隐藏页） |
| 1801070~1801074 | 仓库库存 |
| 1801080~1801087 | 领用申请（1801087 审批详情隐藏页） |
| 1801090~1801094 | BOM 物料库 |
| 1801100~1801104 | BOM 表(产品) |
| 1801110~1801111 | 流转记录 |
| 1801120~1801123 | 采购合同 |
| 1801130~1801133 | 报销导出 |
| 1801140~1801142 | 资金管理 |
| 1801150~1801151 | 手机端专属 / 移动验收（一级目录） |
| 1801160~1801162 | 发票台账 |
| 1805000 | PRIME AI 外链 |
| 1761100000000000001 | admin 用户 id |
| 1761300000000000001~13 | 角色：superadmin(01)/team_leader(02→CEO)/dept_leader(10→最高决策人)/acceptance_contact(11)/warehouse_admin(12)/common_user(13) |
| 1761400000000011xxx | 工作流菜单（1616 工作流目录、1618 我的任务、1700 流程设计隐藏页、1630 流程监控） |
| 17617000000000001xx | sys_config 自定义参数（101~103 采购导出配置） |

## 关键角色 ID 速查

```sql
SELECT role_id, role_key, role_name FROM sys_role WHERE del_flag='0';
```

| role_id | role_key | 显示名 |
|---|---|---|
| 1761300000000000001 | superadmin | 超级管理员 |
| 1761300000000000002 | team_leader | CEO（原团队上级） |
| 1761300000000000010 | dept_leader | 最高决策人（原部门上级） |
| 1761300000000000011 | acceptance_contact | 验收对接人 |
| 1761300000000000012 | warehouse_admin | 仓库管理员 |
| 1761300000000000013 | common_user | 普通用户 |

## 常用诊断 SQL

```sql
-- 某用户的角色
SELECT u.user_name, r.role_key, r.role_name FROM sys_user u
JOIN sys_user_role ur ON u.user_id=ur.user_id
JOIN sys_role r ON ur.role_id=r.role_id WHERE u.user_name='lidi';

-- 某角色绑定的采购菜单
SELECT m.menu_id, m.menu_name, m.path, m.perms, m.visible FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id=m.menu_id
WHERE rm.role_id=1761300000000000013
  AND (m.parent_id=1801000 OR m.menu_id BETWEEN 1801000 AND 1801999) ORDER BY m.menu_id;

-- 流程定义（Warm-Flow 表带 tenant_id='000000'）
SELECT flow_code, flow_name, version, is_publish FROM flow_definition WHERE del_flag='0';
```

## sys_config 自定义参数

| key | 用途 | 默认值 |
|---|---|---|
| procurement.export.project_belong | 导出 Excel 项目归属列 | 李迪科学家工作室 |
| procurement.export.applicant | 导出申请人 | 彭赛威 |
| procurement.export.keeper | 导出保管人 | 裴天姿 |

admin 在系统管理→参数配置页面直接改，导出逻辑读 `configService.selectConfigByKey()`。

## 数据说明

本地和服务器的 OA 数据都视为测试数据（用户明确允许重置），但**结构性改动（表/菜单/角色）必须走脚本**，否则换环境无法复现。
