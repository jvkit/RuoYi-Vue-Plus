# 18. RuoYi-Vue-Plus 后端项目结构详解

## 1. 后端在哪里

很多同学第一次看到这个项目会迷糊：

- `RuoYi-Vue-Plus/` 听起来像前端，其实它是**后端 Java 项目**。
- `plus-ui/` 才是前端。

后端的完整路径：

```
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/
```

技术栈：

- 核心框架：Spring Boot 3.x + Spring Security
- ORM：MyBatis-Plus
- 数据库：MySQL
- 缓存：Redis
- 权限：Sa-Token + JWT
- 多租户：RuoYi-Vue-Plus 自带的多租户体系
- 工作流：Warm-Flow
- 任务调度：SnailJob / PowerJob

## 2. 整体目录结构

打开 `RuoYi-Vue-Plus/` 目录，顶层结构如下：

```
RuoYi-Vue-Plus/
├── pom.xml                          # Maven 父工程配置
├── ruoyi-admin/                     # 启动入口模块
├── ruoyi-common/                    # 公共模块
│   ├── ruoyi-common-core/           # 核心工具类
│   ├── ruoyi-common-doc/            # 文档相关
│   ├── ruoyi-common-excel/          # Excel 导入导出
│   ├── ruoyi-common-log/            # 日志注解
│   ├── ruoyi-common-mail/           # 邮件
│   ├── ruoyi-common-mybatis/        # MyBatis-Plus 封装
│   ├── ruoyi-common-oss/            # 对象存储
│   ├── ruoyi-common-redis/          # Redis 封装
│   ├── ruoyi-common-satoken/        # Sa-Token 认证
│   ├── ruoyi-common-security/       # 安全工具
│   ├── ruoyi-common-sensitive/      # 数据脱敏
│   ├── ruoyi-common-social/         # 第三方登录
│   ├── ruoyi-common-swagger/        # API 文档
│   ├── ruoyi-common-translation/    # 翻译（字典回显等）
│   ├── ruoyi-common-web/            # Web 通用（全局异常、拦截器等）
│   └── ...
├── ruoyi-modules/                   # 业务模块
│   ├── ruoyi-system/                # 系统管理：用户、角色、菜单、部门、岗位、参数、字典
│   ├── ruoyi-gen/                   # 代码生成器
│   ├── ruoyi-job/                   # 定时任务
│   ├── ruoyi-workflow/              # 工作流模块（含采购申请示例）
│   ├── ruoyi-invoice/               # 发票管理模块（我们开发的）
│   └── ruoyi-demo/                  # 示例模块
├── ruoyi-extend/                    # 扩展模块
│   ├── ruoyi-monitor-admin/         # Spring Boot Admin 监控
│   ├── ruoyi-snailjob-server/       # SnailJob 调度中心
│   └── ...
└── script/                          # 脚本目录
    └── sql/                         # SQL 脚本
```

## 3. Maven 多模块设计

RuoYi-Vue-Plus 采用 Maven 多模块工程：

- **父工程 `pom.xml`**：统一版本号、依赖管理、子模块聚合。
- **`ruoyi-admin`**：唯一的可运行模块，依赖所有其他模块，打包成 `ruoyi-admin.jar`。
- **`ruoyi-common` 下的子模块**：被业务模块依赖，提供通用能力。
- **`ruoyi-modules` 下的子模块**：真正的业务代码，每个模块对应一个独立功能域。

依赖关系（简化版）：

```
ruoyi-admin
├── ruoyi-modules/ruoyi-system
├── ruoyi-modules/ruoyi-invoice
├── ruoyi-modules/ruoyi-workflow
└── ...
    └── ruoyi-common-core / ruoyi-common-mybatis / ruoyi-common-satoken ...
```

编译时，执行：

```bash
cd /home/jvkit/workspace/oa/RuoYi-Vue-Plus
mvn package -pl ruoyi-admin -am -DskipTests
```

参数说明：

- `-pl ruoyi-admin`：只构建 `ruoyi-admin` 模块。
- `-am`（also-make）：同时构建 `ruoyi-admin` 依赖的所有模块。
- `-DskipTests`：跳过单元测试。

## 4. 业务模块内部结构

以 `ruoyi-modules/ruoyi-invoice` 为例，一个标准业务模块的内部结构：

```
ruoyi-invoice/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/org/dromara/invoice/
    │   │   ├── controller/              # 控制器：接收 HTTP 请求
    │   │   │   └── InvoiceInfoController.java
    │   │   ├── domain/                  # 实体类
    │   │   │   ├── InvoiceInfo.java
    │   │   │   ├── bo/                  # 业务对象：接收前端参数
    │   │   │   │   └── InvoiceInfoBo.java
    │   │   │   └── vo/                  # 视图对象：返回给前端
    │   │   │       └── InvoiceInfoVo.java
    │   │   ├── mapper/                  # 数据访问层
    │   │   │   └── InvoiceInfoMapper.java
    │   │   ├── service/                 # 服务接口
    │   │   │   ├── IInvoiceInfoService.java
    │   │   │   └── impl/                # 服务实现
    │   │   │       └── InvoiceInfoServiceImpl.java
    │   │   └── enums/                   # 枚举（可选）
    │   └── resources/
    │       └── mapper/invoice/            # MyBatis XML（可选）
    │           └── InvoiceInfoMapper.xml
    └── test/                            # 单元测试（可选）
```

### 4.1 Controller（控制器）

职责：接收前端 HTTP 请求，参数校验，调用 Service，返回统一响应。

位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/controller/InvoiceInfoController.java
```

典型写法：

```java
@RestController
@RequestMapping("/invoice/info")
public class InvoiceInfoController extends BaseController {

    @Autowired
    private IInvoiceInfoService invoiceInfoService;

    @SaCheckPermission("invoice:info:list")
    @GetMapping("/list")
    public TableDataInfo<InvoiceInfoVo> list(InvoiceInfoBo bo, PageQuery pageQuery) {
        return invoiceInfoService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("invoice:info:add")
    @Log(title = "发票信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
        return R.ok(invoiceInfoService.insertByBo(bo));
    }
}
```

关键注解：

- `@RestController` + `@RequestMapping("/invoice/info")`：定义接口前缀。
- `@SaCheckPermission("invoice:info:list")`：Sa-Token 权限控制，只有拥有该权限的用户才能访问。
- `@Log(title = "...", businessType = BusinessType.INSERT)`：记录操作日志。
- `@RepeatSubmit()`：防止重复提交。
- `@Validated(AddGroup.class)`：分组校验，新增时使用 `AddGroup` 规则。
- `R.ok(...)` / `R.fail(...)`：项目封装的统一响应体。

### 4.2 Domain（实体）

职责：与数据库表一一对应，用于 MyBatis-Plus 的 CRUD。

位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/domain/InvoiceInfo.java
```

示例：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice_info")
public class InvoiceInfo extends BaseEntity {

    @TableId(value = "id")
    private Long id;

    private String invoiceCode;
    private String invoiceNumber;
    private String invoiceType;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Date invoiceDate;
    private String sellerName;
    private String buyerName;
    private String status;
    private String remark;
    private String aiOpinion;
    private String verifyStatus;
    private Date verifyTime;
    private String finQueryNo;
    private String orderNo;
}
```

注意：

- `@TableName("invoice_info")`：指定数据库表名。
- `@TableId(value = "id")`：指定主键字段。
- 继承 `BaseEntity`：自动拥有 `createBy`、`createTime`、`updateBy`、`updateTime`、`remark` 等通用字段。

### 4.3 BO（Business Object，业务对象）

职责：接收前端传参，用于新增、编辑等业务操作。

位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/domain/bo/InvoiceInfoBo.java
```

BO 与 Domain 字段几乎一致，但会加上校验注解：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InvoiceInfo.class, reverseConvertGenerate = false)
public class InvoiceInfoBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    private String invoiceCode;

    @NotBlank(message = "发票号码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceNumber;

    @NotBlank(message = "发票类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceType;

    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Date invoiceDate;
    private String sellerName;
    private String buyerName;
    private String status;
    private String remark;
    private String aiOpinion;
    private String verifyStatus;
    private Date verifyTime;
    private String finQueryNo;
    private String orderNo;
}
```

注意：

- `@AutoMapper(target = InvoiceInfo.class, reverseConvertGenerate = false)`：MapStruct-plus 自动生成 `Bo → Entity` 的转换代码。
- `@NotBlank`、`@NotNull` 等校验注解配合 `@Validated` 使用。
- `AddGroup.class` / `EditGroup.class`：分组校验，新增和编辑用不同规则。

### 4.4 VO（View Object，视图对象）

职责：返回给前端的数据结构，通常比 Entity 更精简或更适合展示。

位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/domain/vo/InvoiceInfoVo.java
```

示例：

```java
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = InvoiceInfo.class)
public class InvoiceInfoVo implements Serializable {

    private Long id;
    private String invoiceCode;
    private String invoiceNumber;
    private String invoiceType;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date invoiceDate;

    private String sellerName;
    private String buyerName;
    private String status;
    private String remark;
    private String aiOpinion;
    private String verifyStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    private String finQueryNo;
    private String orderNo;
}
```

注意：

- `@ExcelIgnoreUnannotated` + `@ExcelProperty`：用于 Excel 导出。
- `@JsonFormat(pattern = "yyyy-MM-dd")`：控制日期返回格式。
- `@AutoMapper(target = InvoiceInfo.class)`：自动生成 `Entity → Vo` 的转换。

### 4.5 Mapper（数据访问层）

职责：定义数据库操作方法。MyBatis-Plus 提供了大量内置方法，所以 Mapper 接口往往很简洁。

位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/mapper/InvoiceInfoMapper.java
```

示例：

```java
public interface InvoiceInfoMapper extends BaseMapperPlus<InvoiceInfoMapper, InvoiceInfo, InvoiceInfoVo> {
}
```

`BaseMapperPlus` 是 RuoYi-Vue-Plus 对 MyBatis-Plus `BaseMapper` 的扩展，内置了：

- `selectVoById`
- `selectVoList`
- `selectVoPage`
- `selectVoOne`
- `insert`
- `updateById`
- `deleteById`
- ...

如果内置方法不够用，可以在 `resources/mapper/invoice/InvoiceInfoMapper.xml` 中写自定义 SQL。

### 4.6 Service（业务逻辑层）

职责：处理业务逻辑，协调多个 Mapper 或调用其他 Service。

接口位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/service/IInvoiceInfoService.java
```

实现位置：

```
ruoyi-invoice/src/main/java/org/dromara/invoice/service/impl/InvoiceInfoServiceImpl.java
```

典型方法：

```java
@Override
public TableDataInfo<InvoiceInfoVo> queryPageList(InvoiceInfoBo bo, PageQuery pageQuery) {
    LambdaQueryWrapper<InvoiceInfo> lqw = buildQueryWrapper(bo);
    Page<InvoiceInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
    return TableDataInfo.build(result);
}

@Override
public Boolean insertByBo(InvoiceInfoBo bo) {
    InvoiceInfo add = MapstructUtils.convert(bo, InvoiceInfo.class);
    validEntityBeforeSave(add);
    return baseMapper.insert(add) > 0;
}
```

## 5. 分层调用关系

一次前端新增请求的处理流程：

```
浏览器
  ↓ HTTP POST /invoice/info
Controller（参数校验、权限检查）
  ↓ 调用
Service（业务逻辑）
  ↓ 调用
Mapper（MyBatis-Plus / 自定义 SQL）
  ↓ 操作
数据库
  ↑ 返回结果
Mapper
  ↑ 返回 Entity/Vo
Service
  ↑ 返回处理结果
Controller
  ↑ 包装成 R.ok(...)
浏览器
```

参数转换：

```
前端 JSON → Controller @RequestBody → BO
BO → MapStruct → Entity
Entity → MyBatis-Plus → 数据库
数据库 → Entity → MapStruct → VO
VO → Controller → R.ok(vo) → 前端 JSON
```

## 6. 启动入口：ruoyi-admin

`ruoyi-admin` 是整个后端的启动模块：

```
RuoYi-Vue-Plus/ruoyi-admin/
├── src/main/java/org/dromara/
│   └── DromaraApplication.java      # Spring Boot 启动类
└── src/main/resources/
    ├── application.yml              # 主配置
    ├── application-dev.yml          # 开发环境配置
    ├── application-prod.yml         # 生产环境配置
    └── banner.txt                   # 启动 Banner
```

启动类：

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class DromaraApplication {
    public static void main(String[] args) {
        SpringApplication.run(DromaraApplication.class, args);
    }
}
```

注意 `exclude = { DataSourceAutoConfiguration.class }`，表示数据源不是由 Spring Boot 自动配置，而是由框架自己的多数据源 / 动态数据源配置接管。

启动命令：

```bash
cd /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin
nohup java -jar target/ruoyi-admin.jar --server.port=8088 > /tmp/ruoyi-admin.log 2>&1 &
```

## 7. 核心配置文件

### 7.1 application.yml

主配置文件，包含：

- 服务端口
- 日志级别
- Spring 配置
- MyBatis-Plus 配置
- 多租户开关
- Sa-Token 配置
- Redis 配置引用

### 7.2 application-dev.yml

开发环境配置，包含：

- MySQL 连接：`jdbc:mysql://127.0.0.1:3306/ry-vue`
- Redis 连接：`127.0.0.1:6379`
- 文件上传路径
- 日志路径

### 7.3 多数据源与动态数据源

RuoYi-Vue-Plus 支持多数据源，不同租户可以落到不同数据库。主从库、读写分离也可以在这里配置。

多租户隔离通过 MyBatis-Plus 的租户拦截器实现，默认在 SQL 中自动追加 `tenant_id = ?` 条件。

## 8. 权限与菜单体系

RuoYi-Vue-Plus 的权限核心是 **角色-权限-菜单** 模型：

- **用户**：`sys_user`
- **角色**：`sys_role`
- **菜单**：`sys_menu`（目录、菜单、按钮三种类型）
- **部门**：`sys_dept`
- **岗位**：`sys_post`
- **字典**：`sys_dict_type` / `sys_dict_data`
- **参数**：`sys_config`

权限字符串格式：`模块:功能:操作`，例如：

- `invoice:info:list` — 查看发票列表
- `invoice:info:add` — 新增发票
- `invoice:info:edit` — 编辑发票
- `invoice:info:remove` — 删除发票

Controller 方法上通过 `@SaCheckPermission("invoice:info:list")` 控制访问权限。

菜单由后端 `sys_menu` 表驱动，前端根据当前用户的菜单列表渲染侧边栏。

## 9. 多租户机制

多租户是 RuoYi-Vue-Plus 的重要特性：

- 每个租户有独立的 `tenant_id`。
- 登录时需要选择租户。
- 大多数业务表都有 `tenant_id` 字段。
- MyBatis-Plus 租户拦截器会自动在 SQL 中追加 `tenant_id = 当前租户`。
- 超级管理员可以跨租户管理。

登录时的租户选择器，对应 `sys_tenant` 表。

## 10. 代码生成器

`ruoyi-modules/ruoyi-gen` 是代码生成器模块：

1. 在数据库中建好表。
2. 进入系统管理 → 代码生成 → 导入表。
3. 选择表，预览生成的代码。
4. 生成后端 Controller、Service、Mapper、Domain、BO、VO 和前端页面。
5. 将代码复制到对应位置即可。

生成的代码结构与我们手写的发票模块完全一致，是快速搭CURD的利器。

## 11. 工作流模块

`ruoyi-modules/ruoyi-workflow` 基于 Warm-Flow 工作流引擎：

- 流程定义：设计审批流程。
- 请假示例 `TestLeave`：最简单的参考。
- 采购申请 `ProcurementRequest`：我们自己扩展的示例。

工作流的核心表：

- `flow_definition`：流程定义
- `flow_instance`：流程实例
- `flow_task`：待办任务
- `flow_his_task`：历史任务

## 12. 发票模块是如何放进去的

发票模块 `ruoyi-invoice` 是一个完整的业务模块，它的新增步骤：

1. **建表**：`RuoYi-Vue-Plus/script/sql/invoice_table.sql`
2. **建模块**：在 `ruoyi-modules/` 下新建 `ruoyi-invoice/`，包含 pom.xml
3. **写后端代码**：
   - `domain/InvoiceInfo.java`
   - `domain/bo/InvoiceInfoBo.java`
   - `domain/vo/InvoiceInfoVo.java`
   - `mapper/InvoiceInfoMapper.java`
   - `service/IInvoiceInfoService.java`
   - `service/impl/InvoiceInfoServiceImpl.java`
   - `controller/InvoiceInfoController.java`
4. **注册模块**：在 `ruoyi-admin/pom.xml` 中添加 `ruoyi-invoice` 依赖
5. **前端代码**：在 `plus-ui/src/views/invoice/` 下写页面
6. **菜单/权限 SQL**：执行 `invoice_module.sql` 插入菜单和权限
7. **重启后端**，登录后即可看到发票管理菜单

## 13. 常见目录约定

| 目录/文件 | 作用 |
|---|---|
| `ruoyi-admin/src/main/resources/application.yml` | 主配置文件 |
| `ruoyi-common/ruoyi-common-mybatis` | MyBatis-Plus 封装、分页、多租户 |
| `ruoyi-common/ruoyi-common-satoken` | 登录认证、权限校验 |
| `ruoyi-common/ruoyi-common-log` | 操作日志注解 |
| `ruoyi-common/ruoyi-common-web` | 全局异常、统一响应体 `R`、Web 工具 |
| `ruoyi-modules/ruoyi-system` | 系统管理（用户、角色、菜单、部门等） |
| `ruoyi-modules/ruoyi-gen` | 代码生成器 |
| `ruoyi-modules/ruoyi-invoice` | 发票管理模块 |
| `RuoYi-Vue-Plus/script/sql/` | 数据库初始化脚本和业务模块 SQL |

## 14. 总结

RuoYi-Vue-Plus 后端的核心设计：

- **Maven 多模块**：便于扩展和维护。
- **分层清晰**：Controller → Service → Mapper → Domain/BO/VO。
- **权限驱动**：通过 `@SaCheckPermission` 控制接口访问。
- **菜单驱动前端**：`sys_menu` 决定侧边栏结构。
- **多租户隔离**：自动追加 `tenant_id` 条件。
- **代码生成器**：快速生成标准 CURD 代码。

理解了这套结构，新增一个业务模块就是"复制粘贴 + 改名字 + 加字段"的重复劳动，非常适合快速原型开发。
