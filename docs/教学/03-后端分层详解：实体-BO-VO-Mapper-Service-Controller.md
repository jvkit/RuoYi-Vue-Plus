> 用户反馈：第四章（后端开发）看不懂 `domain / bo / vo` 到底是干啥的。本文专门拆开讲，每一层用发票管理模块的真实代码做例子，追踪一条数据从前端提交到写入数据库，再返回前端的完整流程。
>
> 本文只写一次，后续补充会另开新文档。

---

# 03-后端分层详解：实体 / BO / VO / Mapper / Service / Controller

## 1. 为什么要分这么多层？

一个「新增发票」请求，最简单粗暴的写法可能是这样：

```java
@PostMapping("/invoice/add")
public void add(HttpServletRequest request) {
    String sql = "INSERT INTO invoice_info(invoice_number, amount, ...) VALUES (?, ?, ...)";
    // 直接从 request 取参数，拼 SQL，JDBC 执行
}
```

这在 DEMO 里可以跑，但在真实项目里会爆炸：

- 参数校验写在 Controller 里，又臭又长。
- SQL 散落在各处，改个表结构要全文搜索。
- 业务逻辑（比如「价税合计 = 金额 + 税额」）和接口代码混在一起。
- 返回给前端的数据可能包含敏感字段，没法统一控制。
- 同一套逻辑可能被 N 个接口复制粘贴。

所以项目把它拆成了几层，每层只做一件事：

```
前端请求
   ↓
Controller（接请求、做校验、调 Service、返回结果）
   ↓
Service（写业务逻辑：计算、判断、调用 Mapper）
   ↓
Mapper（操作数据库）
   ↓
数据库
```

返回时方向反过来，数据逐层包装。

---

## 2. 一句话概括每一层

| 层 | 文件名示例 | 作用 | 类比 |
|---|---|---|---|
| **Domain / Entity（实体）** | `InvoiceInfo.java` | 数据库表在 Java 里的「映射对象」，一行记录就是一个对象 | Excel 里的一行数据 |
| **BO（Business Object，业务对象）** | `InvoiceInfoBo.java` | 接收前端参数、承载查询条件、做参数校验 | 前端填的表单 + 搜索条件 |
| **VO（View Object，视图对象）** | `InvoiceInfoVo.java` | 返回给前端看的数据结构，可以隐藏/加工字段 | 给前端展示的表格行 |
| **Mapper** | `InvoiceInfoMapper.java` | 和数据库打交道，执行增删改查 | 数据库操作员 |
| **Service** | `InvoiceInfoServiceImpl.java` | 写业务规则，决定「怎么算、怎么存、怎么组合」 | 业务经理 |
| **Controller** | `InvoiceInfoController.java` | HTTP 入口，接收请求、校验权限、调用 Service | 前台接待 |

---

## 3. 实体 Domain：`InvoiceInfo.java`

### 3.1 它是数据库表的影子

数据库有这张表：

```sql
CREATE TABLE invoice_info (
  id bigint PRIMARY KEY,
  invoice_number varchar(50),
  invoice_type varchar(20),
  amount decimal(18,2),
  tax_amount decimal(18,2),
  total_amount decimal(18,2),
  ...
);
```

Java 里就对应一个类：

```java
@Data
@TableName("invoice_info")
public class InvoiceInfo extends BaseEntity {
    @TableId(value = "id")
    private Long id;

    private String invoiceNumber;   // 对应 invoice_number
    private String invoiceType;     // 对应 invoice_type
    private BigDecimal amount;      // 对应 amount
    private BigDecimal taxAmount;   // 对应 tax_amount
    private BigDecimal totalAmount; // 对应 total_amount
    ...
}
```

- `@TableName("invoice_info")`：告诉 MyBatis-Plus，这个类对应哪张表。
- `@TableId(value = "id")`：哪一列是主键。
- 字段命名用驼峰，MyBatis-Plus 会自动映射成数据库的下划线命名。
- 继承 `BaseEntity`，所以自动带有 `createBy`、`createTime`、`updateBy`、`updateTime`、`delFlag` 这些通用字段。

### 3.2 它的核心作用

**Domain 对象只负责「描述数据库里的一行数据长什么样」**，不做业务计算，也不直接返回给前端。

你可以把它理解成：

> 一个从数据库取出来的「发票记录容器」。

Service 拿到它之后，再决定怎么加工、怎么返回。

---

## 4. BO：`InvoiceInfoBo.java`

### 4.1 BO 是「前端表单 + 查询条件」

前端新增发票时，会提交一个 JSON：

```json
{
  "invoiceNumber": "INV001",
  "invoiceType": "normal",
  "amount": 1000,
  "taxAmount": 130,
  "invoiceDate": "2024-07-24",
  "sellerName": "卖家A",
  "buyerName": "买家B"
}
```

后端 Controller 需要用一个 Java 对象来接收这个 JSON，这个对象就是 **BO**。

```java
@Data
@AutoMapper(target = InvoiceInfo.class)
public class InvoiceInfoBo extends BaseEntity {

    @NotBlank(message = "发票号码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceNumber;

    @NotBlank(message = "发票类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceType;

    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date invoiceDate;

    private String sellerName;
    private String buyerName;
    private String status;
    private String remark;
}
```

### 4.2 BO 和 Domain 的区别

看起来字段差不多，但作用完全不同：

| | Domain | BO |
|---|---|---|
| 来源 | 从数据库来 | 从前端请求来 |
| 校验 | 一般不加校验注解 | 加 `@NotBlank`、`@NotNull` 等校验 |
| 用途 | 描述数据库结构 | 接收参数、做查询条件 |
| 分组 | 无 | 有 `AddGroup` / `EditGroup` |

比如「新增」时要求 `invoiceNumber` 必填，但「查询列表」时可能只传 `invoiceType`，所以 BO 上才有校验分组。

### 4.3 分组校验是什么意思？

看这一行：

```java
@NotBlank(message = "发票号码不能为空", groups = {AddGroup.class, EditGroup.class})
private String invoiceNumber;
```

- `AddGroup.class`：新增时校验。
- `EditGroup.class`：修改时校验。
- 如果某个字段只在新增时必填，可以只写 `AddGroup.class`。

Controller 里这样控制：

```java
@PostMapping()
public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) { ... }

@PutMapping()
public R<InvoiceInfoVo> edit(@Validated(EditGroup.class) @RequestBody InvoiceInfoBo bo) { ... }
```

- `add` 方法触发 `AddGroup` 的校验。
- `edit` 方法触发 `EditGroup` 的校验。

这样同一个 BO 类，既能新增用，也能修改用，不用写两个类。

### 4.4 BO 还用作查询条件

列表查询时，前端可能传：

```json
{
  "invoiceNumber": "INV",
  "invoiceType": "normal",
  "pageNum": 1,
  "pageSize": 10
}
```

Controller 直接用 `InvoiceInfoBo` 接收：

```java
@GetMapping("/list")
public TableDataInfo<InvoiceInfoVo> list(InvoiceInfoBo bo, PageQuery pageQuery) { ... }
```

Service 里再根据 BO 的字段拼查询条件：

```java
lqw.like(StringUtils.isNotBlank(bo.getInvoiceNumber()),
         InvoiceInfo::getInvoiceNumber, bo.getInvoiceNumber());
lqw.eq(StringUtils.isNotBlank(bo.getInvoiceType()),
       InvoiceInfo::getInvoiceType, bo.getInvoiceType());
```

**所以 BO 既是表单对象，也是查询条件对象。**

---

## 5. VO：`InvoiceInfoVo.java`

### 5.1 VO 是专门给前端看的数据

Domain 对象是从数据库取出来的原始数据，但直接返回给前端可能会有问题：

- 字段名是驼峰，前端表格要显示中文标题。
- 有些字段不想给前端看（比如内部状态码、密码等）。
- 有些字段需要格式化（比如日期 `2024-07-24 10:00:00` 想只显示 `2024-07-24`）。
- 导出 Excel 时，需要标注列名。

所以专门定义一个 VO：

```java
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = InvoiceInfo.class)
public class InvoiceInfoVo implements Serializable {

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "发票号码")
    private String invoiceNumber;

    @ExcelProperty(value = "发票类型")
    private String invoiceType;

    @ExcelProperty(value = "价税合计")
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ExcelProperty(value = "开票日期")
    private Date invoiceDate;

    ...
}
```

### 5.2 VO 和 Domain 的区别

| | Domain | VO |
|---|---|---|
| 方向 | 数据库 ↔ Java | Java → 前端 |
| 字段 | 全量表字段 | 可以只放前端需要的 |
| 注解 | 表映射注解 | 导出注解、JSON 格式化注解 |
| 继承 | `BaseEntity` | 通常实现 `Serializable` |

比如 Domain 里有 `delFlag`（逻辑删除标志），但 VO 里可以没有，因为前端不需要知道这条记录是否被删除。

### 5.3 一个具体的例子

数据库里存的 `invoiceType` 是字符串：`normal` / `special` / `electronic`。

前端表格不能直接显示 `normal`，要显示「增值税普通发票」。

这个转换是在前端用 `<dict-tag>` 字典组件做的，但 VO 里仍然只返回 `normal` 这个原始值。因为：

- 后端保持简单，只返回数据。
- 前端根据字典做显示转换，更灵活。

如果你希望后端直接返回中文，也可以在 VO 里加一个 `invoiceTypeName` 字段，在 Service 里查字典赋值。

---

## 6. 三者的关系：BO → Domain → VO

一条新增发票的数据流向：

```
前端 JSON
   ↓
Controller 用 @RequestBody InvoiceInfoBo 接收（BO）
   ↓
Service 把 BO 转成 Domain 对象（InvoiceInfo）
   ↓
Mapper 把 Domain 对象写入数据库
   ↓
数据库保存一行记录
```

返回时：

```
数据库一行记录
   ↓
Mapper 查出来变成 Domain 对象（InvoiceInfo）
   ↓
Service 把 Domain 转成 VO（InvoiceInfoVo）
   ↓
Controller 把 VO 包装成 R.ok(vo) 返回给前端
   ↓
前端展示
```

对应代码：

```java
// Service 新增方法
public InvoiceInfoVo insertByBo(InvoiceInfoBo bo) {
    // BO → Domain
    InvoiceInfo add = MapstructUtils.convert(bo, InvoiceInfo.class);

    // 业务计算
    if (bo.getAmount() != null && bo.getTaxAmount() != null) {
        add.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
    }

    // Mapper 写入数据库
    baseMapper.insert(add);

    // Domain → VO，返回给前端
    return MapstructUtils.convert(add, InvoiceInfoVo.class);
}
```

`MapstructUtils.convert(a, B.class)` 就是自动类型转换，靠 `@AutoMapper(target = ...)` 注解生成转换代码。

---

## 7. Mapper：数据库操作员

### 7.1 Mapper 只干一件事

**和数据库交互。**

```java
public interface InvoiceInfoMapper extends BaseMapperPlus<InvoiceInfo, InvoiceInfoVo> {
}
```

这个接口继承了 `BaseMapperPlus`，框架已经自动实现了：

- `insert(entity)`：插入
- `updateById(entity)`：按 ID 修改
- `deleteById(id)` / `deleteByIds(ids)`：删除
- `selectById(id)`：按 ID 查询
- `selectVoPage(page, wrapper)`：分页查询并返回 VO
- `selectVoList(wrapper)`：列表查询并返回 VO

所以简单 CRUD 不需要写 XML。

### 7.2 什么时候需要 XML？

当查询逻辑复杂，比如多表联查、复杂 `WHERE`、聚合统计时，才需要写 XML：

```xml
<select id="selectInvoiceWithDetail" resultType="...InvoiceInfoVo">
    SELECT i.*, d.item_name, d.item_amount
    FROM invoice_info i
    LEFT JOIN invoice_detail d ON i.id = d.invoice_id
    WHERE i.status = #{status}
</select>
```

发票模块目前只是单表 CRUD，所以 `InvoiceInfoMapper.xml` 可以是空的，甚至不写也可以。

---

## 8. Service：业务逻辑层

### 8.1 Service 是「业务经理」

Controller 把请求交给 Service，Service 决定：

- 数据要不要加工？
- 要不要调用多个 Mapper？
- 要不要抛异常？
- 返回值是什么？

发票模块的 Service 里有一个典型业务规则：

```java
// 自动计算价税合计
if (bo.getAmount() != null && bo.getTaxAmount() != null) {
    bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
}

// 默认草稿状态
if (StringUtils.isBlank(bo.getStatus())) {
    bo.setStatus(BusinessStatusEnum.DRAFT.getStatus());
}
```

这就是业务逻辑：

- 前端可能没传 `totalAmount`，后端自动算。
- 前端可能没传 `status`，后端默认给 `draft`。

如果把这些逻辑写在 Controller 里，每个新增发票的接口都要复制一遍；写在 Service 里，只写一次。

### 8.2 Service 还负责拼查询条件

```java
private LambdaQueryWrapper<InvoiceInfo> buildQueryWrapper(InvoiceInfoBo bo) {
    LambdaQueryWrapper<InvoiceInfo> lqw = Wrappers.lambdaQuery();
    lqw.like(StringUtils.isNotBlank(bo.getInvoiceNumber()),
             InvoiceInfo::getInvoiceNumber, bo.getInvoiceNumber());
    lqw.eq(StringUtils.isNotBlank(bo.getInvoiceType()),
           InvoiceInfo::getInvoiceType, bo.getInvoiceType());
    lqw.eq(StringUtils.isNotBlank(bo.getStatus()),
           InvoiceInfo::getStatus, bo.getStatus());
    lqw.orderByDesc(BaseEntity::getCreateTime);
    return lqw;
}
```

这里根据 BO 里的字段，动态拼 `WHERE` 条件：

- 发票号码不为空，就 `LIKE '%INV%'`。
- 发票类型不为空，就 `= 'normal'`。
- 最后按创建时间倒序。

### 8.3 为什么需要接口 + 实现类？

```java
public interface IInvoiceInfoService { ... }

@Service
public class InvoiceInfoServiceImpl implements IInvoiceInfoService { ... }
```

这是一种约定：

- `IInvoiceInfoService` 定义「能做什么」。
- `InvoiceInfoServiceImpl` 定义「具体怎么做」。

好处是：

- Controller 里依赖的是接口，方便以后换实现（比如换数据源、加缓存）。
- 符合 Spring 的 AOP 代理习惯。
- 便于单元测试时 Mock。

---

## 9. Controller：HTTP 入口

### 9.1 Controller 只做三件事

1. **接请求**：把 HTTP 请求参数转成 BO。
2. **做权限/参数校验**：`@SaCheckPermission`、`@Validated`、`@RepeatSubmit`。
3. **调 Service 并返回**：`R.ok(...)`。

```java
@SaCheckPermission("invoice:info:add")
@Log(title = "发票信息", businessType = BusinessType.INSERT)
@RepeatSubmit()
@PostMapping()
public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
    return R.ok(invoiceInfoService.insertByBo(bo));
}
```

### 9.2 每个注解的作用

| 注解 | 作用 |
|---|---|
| `@SaCheckPermission("invoice:info:add")` | 校验当前登录用户是否有「发票信息新增」权限 |
| `@Log(...)` | 记录操作日志，谁在什么时间新增了发票 |
| `@RepeatSubmit()` | 防止用户连续点两次按钮导致重复插入 |
| `@Validated(AddGroup.class)` | 触发 BO 的 AddGroup 校验规则 |
| `@RequestBody` | 把前端 JSON 转成 BO 对象 |
| `@PostMapping()` | 处理 POST 请求 |

### 9.3 Controller 不写业务逻辑

Controller 里不应该出现：

```java
// 不好的写法
if (bo.getAmount() != null && bo.getTaxAmount() != null) {
    bo.setTotalAmount(...);
}
```

这些应该放在 Service。Controller 只负责「接请求、调 Service、返回」。

---

## 10. 一个完整请求的追踪

假设前端新增一张发票，JSON 如下：

```json
{
  "invoiceNumber": "INV20240724001",
  "invoiceType": "normal",
  "amount": 1000,
  "taxAmount": 130,
  "invoiceDate": "2024-07-24",
  "sellerName": "卖家A",
  "buyerName": "买家B"
}
```

### 10.1 第一步：Controller 接收

```java
@PostMapping()
public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
    return R.ok(invoiceInfoService.insertByBo(bo));
}
```

- Spring 自动把 JSON 转成 `InvoiceInfoBo`。
- `@Validated(AddGroup.class)` 检查 `invoiceNumber` 和 `invoiceType` 是否为空。
- `@SaCheckPermission("invoice:info:add")` 检查用户权限。

### 10.2 第二步：Service 处理业务

```java
public InvoiceInfoVo insertByBo(InvoiceInfoBo bo) {
    // 1. 自动计算价税合计
    if (bo.getAmount() != null && bo.getTaxAmount() != null) {
        bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
    }

    // 2. 默认草稿状态
    if (StringUtils.isBlank(bo.getStatus())) {
        bo.setStatus(BusinessStatusEnum.DRAFT.getStatus());
    }

    // 3. BO → Domain
    InvoiceInfo add = MapstructUtils.convert(bo, InvoiceInfo.class);

    // 4. 写入数据库
    baseMapper.insert(add);

    // 5. Domain → VO 返回
    return MapstructUtils.convert(add, InvoiceInfoVo.class);
}
```

此时 `totalAmount` 被自动填成 `1130.00`，`status` 被填成 `draft`。

### 10.3 第三步：Mapper 写入数据库

`baseMapper.insert(add)` 内部生成 SQL：

```sql
INSERT INTO invoice_info (
  id, tenant_id, invoice_number, invoice_type, amount, tax_amount, total_amount,
  invoice_date, seller_name, buyer_name, status, create_by, create_time, del_flag
) VALUES (
  2080473463294623745, '000000', 'INV20240724001', 'normal',
  1000, 130, 1130, '2024-07-24', '卖家A', '买家B', 'draft',
  1, NOW(), '0'
);
```

### 10.4 第四步：返回给前端

`R.ok(vo)` 包装成：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "2080473463294623745",
    "invoiceNumber": "INV20240724001",
    "invoiceType": "normal",
    "amount": "1000",
    "taxAmount": "130",
    "totalAmount": "1130",
    "invoiceDate": "2024-07-24",
    "sellerName": "卖家A",
    "buyerName": "买家B",
    "status": "draft"
  }
}
```

---

## 11. 列表查询的追踪

前端请求列表：

```
GET /invoice/info/list?pageNum=1&pageSize=10&invoiceType=normal
```

### 11.1 Controller

```java
@GetMapping("/list")
public TableDataInfo<InvoiceInfoVo> list(InvoiceInfoBo bo, PageQuery pageQuery) {
    return invoiceInfoService.queryPageList(bo, pageQuery);
}
```

- `bo.getInvoiceType()` = `normal`
- `pageQuery` 包含 `pageNum=1, pageSize=10`

### 11.2 Service

```java
public TableDataInfo<InvoiceInfoVo> queryPageList(InvoiceInfoBo bo, PageQuery pageQuery) {
    LambdaQueryWrapper<InvoiceInfo> lqw = buildQueryWrapper(bo);
    Page<InvoiceInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
    return TableDataInfo.build(result);
}
```

生成的 SQL 类似：

```sql
SELECT *
FROM invoice_info
WHERE invoice_type = 'normal' AND del_flag = '0'
ORDER BY create_time DESC
LIMIT 0, 10;
```

返回的 `Page<InvoiceInfoVo>` 直接就是 VO 列表，不需要 Service 再转换。

---

## 12. 常见疑问

### 12.1 BO 和 VO 字段几乎一样，能不能合并？

可以，但不推荐。原因：

- BO 有校验注解、分组，VO 有导出注解、JSON 注解，混在一起会很乱。
- 以后业务复杂了，BO 可能有「查询专用字段」（比如时间范围 `beginTime` / `endTime`），VO 可能有「显示专用字段」（比如 `invoiceTypeName`），并不完全重合。
- 分层清晰后，改需求时不容易牵一发而动全身。

### 12.2 Domain 能不能直接返回给前端？

可以，但不推荐：

- Domain 继承 `BaseEntity`，包含 `delFlag`、`createBy` 等内部字段，前端通常不需要。
- 返回 Domain 会让前端过度依赖数据库结构，表结构一变接口就变了。
- VO 可以灵活控制返回内容，做脱敏、格式化、聚合。

### 12.3 为什么新增返回 VO，而不是直接返回 "success"？

返回 VO 的好处是前端新增后能立刻拿到生成的主键 `id`，方便后续操作（比如跳转详情页、继续新增明细）。

---

## 13. 总结口诀

```
Domain：数据库里长啥样
BO：前端传了啥、查啥条件
VO：前端看啥样
Mapper：跑腿存取数据库
Service：动脑处理业务
Controller：张嘴接请求、闭嘴返回结果
```

记住这个流程：

```
前端 → BO → Service → Domain → Mapper → 数据库
数据库 → Domain → Service → VO → Controller → 前端
```

---

## 14. 小练习

建议你打开这几个文件，对照着看：

1. `RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/src/main/java/org/dromara/invoice/domain/InvoiceInfo.java`
2. `.../domain/bo/InvoiceInfoBo.java`
3. `.../domain/vo/InvoiceInfoVo.java`
4. `.../mapper/InvoiceInfoMapper.java`
5. `.../service/impl/InvoiceInfoServiceImpl.java`
6. `.../controller/InvoiceInfoController.java`

然后回答这几个问题，检验自己是否理解：

1. 前端新增发票时，数据先变成哪一层对象？
2. `totalAmount` 是在哪一层被计算出来的？
3. 为什么要先 `BO → Domain`，再 `Domain → VO`？能不能跳过 Domain？
4. 列表查询返回的是 BO、Domain 还是 VO？

如果你能把这 4 题答出来，这一层就基本通了。
