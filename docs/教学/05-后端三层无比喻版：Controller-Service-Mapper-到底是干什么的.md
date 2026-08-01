> 用户反馈：比喻看懂了，但不知道对应到代码里到底是什么。本文不用比喻，直接用发票管理模块的真实代码，说明 Controller / Service / Mapper 各层到底在干什么。
>
> 本文只写一次，后续补充会另开新文档。

---

# 05-后端三层无比喻版：Controller / Service / Mapper 到底是干什么的

## 0. 先把大框架说清楚

一个 HTTP 请求进来后，后端的处理顺序是固定的：

```
浏览器/前端
   ↓ 发送 HTTP 请求
Controller（接收请求）
   ↓ 调用
Service（处理业务）
   ↓ 调用
Mapper（操作数据库）
   ↓
数据库
```

返回时方向反过来。

这三层不是可有可无的包装，而是职责划分：

| 层 | 职责 | 不应该做的事 |
|---|---|---|
| **Controller** | 接收 HTTP 请求、权限校验、参数校验、调用 Service、返回统一响应 | 不写业务计算、不写 SQL |
| **Service** | 实现业务逻辑、数据加工、调用 Mapper | 不直接处理 HTTP 请求、不直接拼 SQL |
| **Mapper** | 执行数据库增删改查 | 不写业务规则、不做权限判断 |

下面每一层都用 `ruoyi-invoice` 发票模块的真实代码说明。

---

## 1. Controller：HTTP 请求的入口

### 1.1 它是前端唯一能直接调用的后端代码

前端发请求：

```
POST /invoice/info
```

后端能接收到这个请求的，就是 Controller 里的方法：

```java
@PostMapping()
public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
    return R.ok(invoiceInfoService.insertByBo(bo));
}
```

### 1.2 Controller 里做三件事

#### 第一件事：权限校验

```java
@SaCheckPermission("invoice:info:add")
```

这行代码的意思是：当前登录用户必须拥有 `invoice:info:add` 权限，才能调用这个接口。

权限从哪来？来自 `sys_menu` 表里的 `perms` 字段，以及用户角色关联。

#### 第二件事：参数校验

```java
@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo
```

- `@RequestBody`：把前端传来的 JSON 转成 Java 对象 `InvoiceInfoBo`。
- `@Validated(AddGroup.class)`：按「新增分组」的规则校验 BO 里的字段。

校验规则定义在 BO 里：

```java
public class InvoiceInfoBo {
    @NotBlank(message = "发票号码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceNumber;
}
```

如果前端没传 `invoiceNumber`，Spring 会自动拦截，返回：

```json
{"code":500,"msg":"发票号码不能为空"}
```

**Controller 本身不需要写 `if (invoiceNumber == null)` 这种判断。**

#### 第三件事：调用 Service 并返回

```java
return R.ok(invoiceInfoService.insertByBo(bo));
```

- 把校验通过的 BO 交给 Service。
- Service 处理完后返回一个 VO。
- Controller 把 VO 包装成统一响应 `R.ok(...)` 返回给前端。

### 1.3 Controller 里不应该有什么

不应该有：

```java
// 错误示例：业务计算写在 Controller 里
if (bo.getAmount() != null && bo.getTaxAmount() != null) {
    bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
}

// 错误示例：SQL 写在 Controller 里
String sql = "INSERT INTO invoice_info ...";
```

这些都要交给 Service 和 Mapper。

---

## 2. Service：业务逻辑的实现层

### 2.1 什么是业务逻辑？

业务逻辑就是「这个系统应该怎么处理这件事」。

以发票新增为例：

- 前端只传了 `amount`（金额）和 `taxAmount`（税额）。
- 系统要自动算出 `totalAmount`（价税合计）。
- 如果前端没传状态，系统默认给 `draft`（草稿）。
- 然后才写入数据库。

这些规则就是业务逻辑。

### 2.2 Service 里的真实代码

```java
@Service
public class InvoiceInfoServiceImpl implements IInvoiceInfoService {

    private final InvoiceInfoMapper baseMapper;

    @Override
    public InvoiceInfoVo insertByBo(InvoiceInfoBo bo) {
        // 业务规则 1：自动计算价税合计
        if (bo.getAmount() != null && bo.getTaxAmount() != null) {
            bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
        }

        // 业务规则 2：默认草稿状态
        if (StringUtils.isBlank(bo.getStatus())) {
            bo.setStatus(BusinessStatusEnum.DRAFT.getStatus());
        }

        // BO → Domain（数据库实体）
        InvoiceInfo add = MapstructUtils.convert(bo, InvoiceInfo.class);

        // 调用 Mapper 写入数据库
        baseMapper.insert(add);

        // Domain → VO，返回给 Controller
        return MapstructUtils.convert(add, InvoiceInfoVo.class);
    }
}
```

### 2.3 Service 为什么要接口 + 实现类？

你看到两个文件：

```
IInvoiceInfoService.java       ← 接口，定义「能做什么」
InvoiceInfoServiceImpl.java    ← 实现类，定义「怎么做」
```

Controller 里依赖的是接口：

```java
private final IInvoiceInfoService invoiceInfoService;
```

而不是实现类。

原因：

- 方便以后换实现（比如加缓存、换数据源）。
- Spring 的 AOP 代理（事务、日志）更自然。
- 测试时可以 Mock 接口。

对初学者来说，暂时可以理解为「项目约定」：Service 都写成接口 + 实现。

### 2.4 Service 还负责拼查询条件

列表查询时，前端可能传来多个查询条件：

```
GET /invoice/info/list?invoiceNumber=INV&invoiceType=normal&status=draft
```

Service 负责判断哪些条件要加入 SQL：

```java
private LambdaQueryWrapper<InvoiceInfo> buildQueryWrapper(InvoiceInfoBo bo) {
    LambdaQueryWrapper<InvoiceInfo> lqw = Wrappers.lambdaQuery();

    // 如果发票号码不为空，就加 LIKE 条件
    lqw.like(StringUtils.isNotBlank(bo.getInvoiceNumber()),
             InvoiceInfo::getInvoiceNumber, bo.getInvoiceNumber());

    // 如果发票类型不为空，就加 = 条件
    lqw.eq(StringUtils.isNotBlank(bo.getInvoiceType()),
           InvoiceInfo::getInvoiceType, bo.getInvoiceType());

    // 如果状态不为空，就加 = 条件
    lqw.eq(StringUtils.isNotBlank(bo.getStatus()),
           InvoiceInfo::getStatus, bo.getStatus());

    // 按创建时间倒序
    lqw.orderByDesc(BaseEntity::getCreateTime);

    return lqw;
}
```

注意：这里并没有写 SQL 字符串，而是用 MyBatis-Plus 的 Wrapper 对象来描述查询条件。

---

## 3. Mapper：真正执行数据库操作的地方

### 3.1 Mapper 是什么？

Mapper 是 Java 代码和数据库之间的翻译官。

你的 Java 对象是：

```java
InvoiceInfo info = new InvoiceInfo();
info.setInvoiceNumber("INV001");
info.setAmount(new BigDecimal("1000"));
```

数据库里存的是：

```sql
INSERT INTO invoice_info (invoice_number, amount) VALUES ('INV001', 1000);
```

Mapper 负责把 Java 对象转换成 SQL，发给数据库执行。

### 3.2 发票模块的 Mapper 长什么样？

```java
public interface InvoiceInfoMapper extends BaseMapperPlus<InvoiceInfo, InvoiceInfoVo> {
}
```

这个接口**没有任何方法体**，但它继承了 `BaseMapperPlus`，框架已经自动实现了常用的增删改查：

| 方法 | 作用 |
|---|---|
| `insert(entity)` | 插入一条记录 |
| `updateById(entity)` | 按 ID 更新 |
| `deleteById(id)` | 按 ID 删除 |
| `deleteByIds(ids)` | 批量删除 |
| `selectById(id)` | 按 ID 查询 |
| `selectVoPage(page, wrapper)` | 分页查询并返回 VO |
| `selectVoList(wrapper)` | 列表查询并返回 VO |

所以简单 CRUD 不需要写任何 SQL。

### 3.3 什么时候需要写 XML？

当查询复杂时，比如多表联查、分组统计，就需要手写 SQL，写在 XML 文件里：

```xml
<select id="selectInvoiceWithDetail" resultType="org.dromara.invoice.domain.vo.InvoiceInfoVo">
    SELECT i.*, d.item_name
    FROM invoice_info i
    LEFT JOIN invoice_detail d ON i.id = d.invoice_id
    WHERE i.status = #{status}
</select>
```

发票模块目前是单表 CRUD，所以 `InvoiceInfoMapper.xml` 可以是空的，甚至可以没有。

### 3.4 Mapper 和 JDBC 的关系

如果没有 MyBatis-Plus，你就要自己写：

```java
Connection conn = DriverManager.getConnection(...);
PreparedStatement stmt = conn.prepareStatement("INSERT INTO invoice_info ...");
stmt.setString(1, info.getInvoiceNumber());
stmt.executeUpdate();
```

MyBatis-Plus 帮你把这些重复代码封装掉了，你只需要定义 Mapper 接口，剩下的框架自动做。

---

## 4. 一个完整请求的代码追踪

以「新增发票」为例，看看每一层分别做了什么。

### 4.1 前端发送请求

```
POST /prod-api/invoice/info
Authorization: Bearer xxx
Content-Type: application/json

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

### 4.2 Controller 接收并校验

```java
@SaCheckPermission("invoice:info:add")
@PostMapping()
public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
    return R.ok(invoiceInfoService.insertByBo(bo));
}
```

- `@SaCheckPermission`：检查权限。
- `@Validated`：检查 `invoiceNumber`、`invoiceType` 是否为空。
- 把 BO 传给 Service。

### 4.3 Service 处理业务并调用 Mapper

```java
public InvoiceInfoVo insertByBo(InvoiceInfoBo bo) {
    // 计算价税合计：1000 + 130 = 1130
    if (bo.getAmount() != null && bo.getTaxAmount() != null) {
        bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
    }

    // 默认状态为 draft
    if (StringUtils.isBlank(bo.getStatus())) {
        bo.setStatus(BusinessStatusEnum.DRAFT.getStatus());
    }

    // BO → Domain
    InvoiceInfo add = MapstructUtils.convert(bo, InvoiceInfo.class);

    // Mapper 写入数据库
    baseMapper.insert(add);

    // Domain → VO
    return MapstructUtils.convert(add, InvoiceInfoVo.class);
}
```

### 4.4 Mapper 执行 SQL

`baseMapper.insert(add)` 内部会生成并执行类似这样的 SQL：

```sql
INSERT INTO invoice_info (
  id, tenant_id, invoice_number, invoice_type, amount, tax_amount, total_amount,
  invoice_date, seller_name, buyer_name, status, create_by, create_time, del_flag
) VALUES (
  2080473463294623745, '000000', 'INV20240724001', 'normal',
  1000.00, 130.00, 1130.00, '2024-07-24', '卖家A', '买家B', 'draft',
  1, '2024-07-24 10:00:00', '0'
);
```

### 4.5 返回给前端

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "2080473463294623745",
    "invoiceNumber": "INV20240724001",
    "invoiceType": "normal",
    "amount": "1000.00",
    "taxAmount": "130.00",
    "totalAmount": "1130.00",
    "invoiceDate": "2024-07-24",
    "sellerName": "卖家A",
    "buyerName": "买家B",
    "status": "draft"
  }
}
```

---

## 5. Domain / BO / VO 和 SQL 的关系

你前面说：

> "VO 里面的字段看起来像是 SQL 里面定义的重写一遍"

这句话是对的，但不完全准确。

### 5.1 三者和 SQL 的关系

| 对象 | 和 SQL 的关系 | 作用 |
|---|---|---|
| **Domain (`InvoiceInfo`)** | 字段基本和表字段一一对应 | 描述数据库里的一行记录 |
| **BO (`InvoiceInfoBo`)** | 字段大多来自表，但可能有查询专用字段 | 接收前端参数、做校验 |
| **VO (`InvoiceInfoVo`)** | 字段来自表，但可能隐藏敏感字段、加导出注解 | 返回给前端/导出 Excel |

### 5.2 为什么要写三遍字段？

因为三个对象的**职责不同**，字段会慢慢分化：

- BO 以后可能加 `beginTime`、`endTime`（查询时间范围），这两个字段数据库表里没有。
- VO 以后可能加 `invoiceTypeName`（发票类型中文名），数据库表里也没有，是 Service 查字典赋值的。
- Domain 里可能有 `delFlag`，但 VO 里可以没有，因为前端不需要知道。

现在看起来重复，是为了以后不扯皮。

---

## 6. 三层划分带来的好处

假设老板提了两个需求：

### 需求 1：价税合计要四舍五入到分

你只需要改 Service：

```java
bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()).setScale(2, RoundingMode.HALF_UP));
```

Controller 和 Mapper 都不用动。

### 需求 2：发票表加个 `tax_rate` 字段

你只需要改：

1. `InvoiceInfo.java`（Domain 加字段）
2. `InvoiceInfoBo.java`（BO 加字段）
3. `InvoiceInfoVo.java`（VO 加字段）
4. 数据库表结构

Controller 和 Service 基本不用改，Mapper 如果是简单 CRUD 也不用改（BaseMapperPlus 自动适配）。

---

## 7. 你现在的理解是对的

你总结的三点基本正确：

> - Controller：前端请求过来后做一些校验，比如鉴权、字段是否合法。
> - Service：实现某种功能，比如一个运算，就把它写到 Service 里。
> - Mapper：看起来就是执行 SQL 语句的那些。

补充一句话：

> **Controller 负责「能不能做」，Service 负责「怎么做」，Mapper 负责「存到哪」。**

---

## 8. 推荐下一步

打开这三个文件，按顺序看：

1. `RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/src/main/java/org/dromara/invoice/controller/InvoiceInfoController.java`
2. `.../service/impl/InvoiceInfoServiceImpl.java`
3. `.../mapper/InvoiceInfoMapper.java`

边看边想：

- 这个方法是哪个 HTTP 请求触发的？（Controller）
- 真正的计算/判断在哪里？（Service）
- 数据最后是怎么进数据库的？（Mapper）

如果还有具体哪一行看不懂，把文件名和行号发我，我接着开新文档讲。
