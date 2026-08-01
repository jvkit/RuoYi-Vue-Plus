> 用户反馈：没有 Java 基础，对文档 03 里的几句话完全看不懂：
> - "参数校验写在 Controller 里" 是什么意思？
> - 为什么粗暴写法会导致 SQL 散落？
> - "同一套逻辑被 N 个接口复制粘贴" 是为什么？
> - `InvoiceInfo.java` 是后端代码，为什么会有 template？
>
> 本文用最生活化的比喻，不讲 Java 语法，只讲「为什么这样设计」。

---

# 04-给零基础讲后端分层

## 0. 先记住一个比喻

把整个后端系统想象成一家**餐厅**：

| 后端组件 | 餐厅角色 | 职责 |
|---|---|---|
| **Controller** | 前台服务员 | 客人来了，接单、确认菜单、把单子传给厨房 |
| **Service** | 厨师长 | 决定菜怎么做、加什么料、调用谁 |
| **Mapper** | 仓库管理员 | 负责从仓库取食材、存东西 |
| **数据库** | 仓库 | 真正存数据的地方 |
| **Domain（实体）** | 标准食材盒 | 仓库里一格一格的盒子，贴上标签 |
| **BO（业务对象）** | 客人点的菜单 | 客人勾选「少辣、加蛋」的要求 |
| **VO（视图对象）** | 端上桌的成品菜 | 摆好盘、装饰好，给客人看 |

下面所有解释都会回到这个比喻。

---

## 1. "参数校验写在 Controller 里" 是什么意思？

### 1.1 什么是参数校验？

客人点菜时，服务员要检查：

- 你点的菜我们有没有？
- 你点的辣度是不是在可选范围？
- 你填的手机号是不是 11 位？

这些检查就是**参数校验**。

对应到系统里：

- 发票号码不能为空。
- 金额必须是数字，不能是负数。
- 发票类型只能是 `normal / special / electronic` 之一。

### 1.2 粗暴写法：校验写在 Controller 里

如果都堆在 Controller（服务员）身上，代码会变成这样：

```java
@PostMapping("/invoice/add")
public R add(@RequestBody InvoiceInfoBo bo) {
    // 以下全是校验代码
    if (bo.getInvoiceNumber() == null || bo.getInvoiceNumber().isEmpty()) {
        return R.fail("发票号码不能为空");
    }
    if (bo.getInvoiceType() == null || bo.getInvoiceType().isEmpty()) {
        return R.fail("发票类型不能为空");
    }
    if (bo.getAmount() == null || bo.getAmount().compareTo(BigDecimal.ZERO) < 0) {
        return R.fail("金额不能为负数");
    }
    // ... 可能还有十几条

    // 真正的业务才开始
    invoiceInfoService.insertByBo(bo);
    return R.ok();
}
```

问题：

- Controller 里 80% 是 `if (...) return fail`，很乱。
- 同一个字段在「新增」要校验，在「修改」也要校验，每次都要重写一遍。
- 新增一个字段，要改 N 个接口。

### 1.3 RuoYi 的写法：校验写在 BO 上

RuoYi 把校验规则直接写在「菜单」（BO）上：

```java
public class InvoiceInfoBo {

    @NotBlank(message = "发票号码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceNumber;

    @NotBlank(message = "发票类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceType;
}
```

Controller 只需要一行：

```java
@PostMapping()
public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
    return R.ok(invoiceInfoService.insertByBo(bo));
}
```

- `@Validated(AddGroup.class)`：自动按 BO 里的规则检查。
- 规则违反时，Spring 自动返回错误提示，Controller 里不用写一堆 `if`。

**区别**：

- 粗暴写法 = 服务员自己背下所有菜单规则，一个个问客人。
- RuoYi 写法 = 餐厅把规则印在菜单上，客人点完单机器自动检查。

---

## 2. 为什么粗暴写法会导致 "SQL 散落在各处"？

### 2.1 SQL 是什么？

SQL 就是操作数据库的指令，比如：

```sql
INSERT INTO invoice_info (invoice_number, amount) VALUES ('INV001', 1000);
SELECT * FROM invoice_info WHERE invoice_type = 'normal';
```

### 2.2 粗暴写法的问题

如果每个 Controller（服务员）都直接写 SQL，项目里会出现：

```java
// Controller A：新增发票
String sql = "INSERT INTO invoice_info (...) VALUES (...)";

// Controller B：查询发票
String sql = "SELECT * FROM invoice_info WHERE ...";

// Controller C：导出发票
String sql = "SELECT * FROM invoice_info WHERE ... ORDER BY ...";

// Controller D：统计发票
String sql = "SELECT COUNT(*) FROM invoice_info WHERE ...";
```

这些 SQL 字符串分散在十几个文件里。

### 2.3 改表结构时有多痛苦？

假设你现在要给 `invoice_info` 表加一个字段 `tax_rate`（税率）。

粗暴写法下，你要：

1. 全文搜索 `invoice_info`，找到所有 SQL。
2. 每个 SQL 都改一遍。
3. 漏改一个，某个接口就报错。

### 2.4 RuoYi 的写法：SQL 集中在 Mapper

RuoYi 把数据库操作全部交给 Mapper：

```java
public interface InvoiceInfoMapper extends BaseMapperPlus<InvoiceInfo, InvoiceInfoVo> {
}
```

简单 CRUD 由框架自动生成，复杂查询才写 XML：

```xml
<!-- 所有复杂 SQL 都在这里 -->
<select id="selectXXX" resultType="...">
    SELECT * FROM invoice_info WHERE ...
</select>
```

**好处**：

- 改表结构时，主要改 Domain（实体类）和 Mapper/XML。
- Controller 和 Service 基本不动。
- 找 SQL 只去 Mapper 目录，不用全文搜索。

**回到餐厅比喻**：

- 粗暴写法 = 每个服务员都自己进仓库取食材，仓库规则变了他不一定知道。
- RuoYi 写法 = 只有仓库管理员（Mapper）能进仓库，服务员只负责传话。

---

## 3. "同一套逻辑被 N 个接口复制粘贴" 是什么意思？

### 3.1 举个例子：价税合计

发票有个业务规则：

```
价税合计 = 不含税金额 + 税额
```

也就是 `totalAmount = amount + taxAmount`。

### 3.2 粗暴写法下的复制粘贴

如果你的系统里有这些接口：

- 新增发票
- 修改发票
- 批量导入发票
- 从 Excel 导入发票
- 第三方系统推送发票

每个接口的 Controller 里都可能写：

```java
if (amount != null && taxAmount != null) {
    totalAmount = amount.add(taxAmount);
}
```

这就叫**复制粘贴**。同样的计算逻辑，出现在 5 个地方。

### 3.3 问题在哪里？

- 某一天老板说："价税合计要四舍五入到分"。
- 你得把 5 个地方都改一遍。
- 漏改一个，数据就不一致，有的发票合计对不上。

### 3.4 RuoYi 的写法：逻辑放在 Service

在 Service 里只写一次：

```java
public InvoiceInfoVo insertByBo(InvoiceInfoBo bo) {
    if (bo.getAmount() != null && bo.getTaxAmount() != null) {
        bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
    }
    // ... 写入数据库
}
```

所有新增/修改/导入的入口，最终都调用这个 Service 方法。

**规则只写一次，到处复用。**

**回到餐厅比喻**：

- 粗暴写法 = 每个服务员都知道"蛋炒饭要先打蛋"，但每个人都自己操作，标准不统一。
- RuoYi 写法 = 厨师长（Service）统一规定怎么做，所有单子都交给厨师长。

---

## 4. "返回给前端的数据可能包含敏感字段" 是什么意思？

### 4.1 什么是敏感字段？

数据库里可能存了：

- 逻辑删除标志 `del_flag`（0 正常 / 2 已删除）
- 创建人 ID `create_by`
- 更新人 ID `update_by`
- 某些内部状态码

这些字段后端需要，但前端用户不需要看到，甚至不应该看到。

### 4.2 粗暴写法的问题

如果直接把数据库查出来的对象返回给前端：

```java
@GetMapping("/{id}")
public InvoiceInfo getInfo(Long id) {
    return invoiceInfoMapper.selectById(id);  // 直接返回数据库对象
}
```

前端会收到：

```json
{
  "id": 1,
  "invoiceNumber": "INV001",
  "delFlag": "0",
  "createBy": 1,
  "updateBy": 1
}
```

`delFlag`、`createBy` 这些内部字段暴露给了前端。

### 4.3 RuoYi 的写法：用 VO 控制返回内容

Service 把需要给前端看的字段，专门放到 VO 里：

```java
public class InvoiceInfoVo {
    private Long id;
    private String invoiceNumber;
    private String invoiceType;
    private BigDecimal totalAmount;
    // ... 没有 delFlag，没有 createBy
}
```

Controller 返回 VO：

```java
@GetMapping("/{id}")
public R<InvoiceInfoVo> getInfo(Long id) {
    return R.ok(invoiceInfoService.queryById(id));
}
```

前端只会收到：

```json
{
  "id": 1,
  "invoiceNumber": "INV001",
  "invoiceType": "normal",
  "totalAmount": 1130
}
```

**回到餐厅比喻**：

- 粗暴写法 = 服务员把厨房里的原材料标签一起端给客人。
- RuoYi 写法 = 服务员只端成品菜，后厨标签留在后厨。

---

## 5. `InvoiceInfo.java` 是后端代码，为什么会有 template？

### 5.1 先澄清：这个文件里没有 template

`InvoiceInfo.java` 是一个纯 Java 类文件，不是模板。你看到的内容大概是这样：

```java
@Data
@TableName("invoice_info")
public class InvoiceInfo extends BaseEntity {
    @TableId(value = "id")
    private Long id;

    private String invoiceNumber;
    private String invoiceType;
    ...
}
```

这里面没有 `template` 关键字。

### 5.2 你可能把什么看成了 template？

有几种可能：

1. **IDE 编辑器显示**：某些 IDE 会把 Lombok 生成的 `getter/setter/toString` 折叠显示成 "Generated by Lombok"，看起来像模板代码，但实际上是框架自动帮你生成的。

2. **Feishu/飞书预览限制**：你在飞书文档里粘贴代码链接或截图时，飞书可能显示"暂时无法在飞书文档外展示此内容"，这是飞书的限制，不是代码里有 template。

3. **把 `@Data` 看成了模板**：`@Data` 是 Lombok 的注解，它的作用相当于自动给这个类加上：
   - `getter` 方法
   - `setter` 方法
   - `toString()`
   - `equals()` / `hashCode()`

   这样你就不用手写一堆 `getInvoiceNumber()`、`setInvoiceNumber(...)`。

### 5.3 一句话总结

> `InvoiceInfo.java` 就是一个普通的 Java 类，用 `@Data` 省略了繁琐的 getter/setter。它不是模板，也没有模板语法。

如果你想确认，可以直接用 `cat` 命令看这个文件的真实内容：

```bash
cat /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/src/main/java/org/dromara/invoice/domain/InvoiceInfo.java
```

---

## 6. 再看那个对比表格

文档 03 里的表格：

| 层 | 文件名示例 | 作用 | 类比 |
|---|---|---|---|
| Domain / Entity | `InvoiceInfo.java` | 数据库表在 Java 里的映射对象 | Excel 里的一行数据 |
| BO | `InvoiceInfoBo.java` | 接收前端参数、做校验 | 前端填的表单 |
| VO | `InvoiceInfoVo.java` | 返回给前端看的数据 | 给前端展示的表格行 |

用餐厅再翻译一次：

- **Domain**：仓库里的标准食材盒（贴上标签：肉、菜、调料）。
- **BO**：客人勾选的菜单（要微辣、加蛋、不要葱）。
- **VO**：端上桌的成品菜（摆盘、装饰、去掉厨房标签）。

---

## 7. 总结：为什么要分层？

粗暴写法把所有事情都堆在 Controller 里，就像让服务员同时做：

- 接待客人
- 检查菜单有没有问题
- 自己进仓库搬食材
- 自己炒菜
- 决定上什么菜给客人看

餐厅早就分工了：服务员、厨师长、仓库管理员各干各的。

软件分层也是同样的道理：

- **Controller**：只接请求、调 Service、返回结果。
- **Service**：只写业务规则。
- **Mapper**：只操作数据库。
- **Domain / BO / VO**：各自描述不同场景下的数据。

这样每个人（每层）职责单一，改起来不容易出错，代码也容易复用。

---

## 8. 你可以做的下一步

1. 打开 `InvoiceInfo.java`、`InvoiceInfoBo.java`、`InvoiceInfoVo.java`，对比三个文件的字段和注解。
2. 打开 `InvoiceInfoController.java`，数一数里面有几行是真正和业务相关的代码。
3. 打开 `InvoiceInfoServiceImpl.java`，找到「自动计算价税合计」那一行，想想如果不用 Service，要在多少个地方写这段代码。

如果看完还有哪句不懂，直接复制那句话给我，我接着开新文档讲。
