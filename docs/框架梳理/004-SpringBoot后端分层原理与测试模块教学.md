# 004 Spring Boot 后端分层原理与测试模块教学

> 角标：004  
> 目标：回答"为什么后端代码全是定义，没有执行"这个核心疑问，并在 `ruoyi-demo` 模块中通过测试模块实战教学，让你彻底理解每一层是干什么的、修改时该改哪里。

---

## 一、先回答你的核心疑问：代码到底在哪里执行？

你观察到：

> "创建表、创建实体、创建 VO/BO、创建 Mapper、创建 Service、创建 Controller，但全程好像没有执行，全是定义与声明。"

这个观察非常敏锐。**你说得对，确实全是定义。**

但执行不是由我们写的，而是由 **Spring Boot 框架** 在运行时替我们执行的。

### 1.1 传统写法 vs Spring Boot 写法

#### 传统写法（你自己 new 对象）

```java
// 你自己创建对象、自己调用
PmsTestService service = new PmsTestService();
service.insertByBo(bo);
```

这里你自己执行了 `new` 和 `insertByBo()`。

#### Spring Boot 写法（框架帮你 new 对象）

```java
@Service
public class PmsTestServiceImpl implements IPmsTestService {
    // 框架会自动注入这个 Mapper
    private final PmsTestMapper baseMapper;
    
    public Boolean insertByBo(PmsTestBo bo) {
        PmsTest entity = MapstructUtils.convert(bo, PmsTest.class);
        return baseMapper.insert(entity) > 0;
    }
}
```

```java
@RestController
public class PmsTestController {
    // 框架会自动注入这个 Service
    private final IPmsTestService testService;
    
    @PostMapping
    public R<Void> add(@RequestBody PmsTestBo bo) {
        return toAjax(testService.insertByBo(bo));
    }
}
```

你自己没有写 `new PmsTestServiceImpl()`，也没有写 `new PmsTestMapper()`。

### 1.2 那么对象是谁创建的？

**Spring IoC 容器**（Inversion of Control，控制反转容器）。

你可以把 Spring 理解成一个"对象工厂"：

```text
你写：@Service、@Controller、@Mapper、@Component
Spring：好，我记下来，启动时我帮你创建这些对象。

你写：private final XxxService xxxService;
Spring：好，我帮你把需要的对象塞进来。

HTTP 请求来了：POST /procurement/test
Spring：我找到对应的 Controller 方法，调用它。
```

所以：

- **你负责写定义**（类、方法、注解）。
- **Spring 负责创建对象、组装对象、调用方法**。

### 1.3 执行链长什么样？

当用户点击"新增"按钮时：

```text
前端点击新增
  ↓ 发送 HTTP POST /procurement/test
  ↓ Spring 收到请求
  ↓ Spring 找到 PmsTestController.add() 方法
  ↓ Spring 调用 testService.insertByBo(bo)
  ↓ Service 调用 baseMapper.insert(entity)
  ↓ Mapper 执行 SQL INSERT
  ↓ 返回结果给前端
```

你看到的新增方法里的代码 `baseMapper.insert(entity)`，就是你写的**执行代码**。

只是对象的创建、方法的调用入口，由 Spring 框架负责。

---

## 二、Spring IoC 容器：对象的"自动工厂"

### 2.1 什么是 IoC

IoC = Inversion of Control，控制反转。

- **传统方式**：你控制对象的创建和依赖关系。
- **Spring 方式**：Spring 控制对象的创建和依赖关系，你只负责声明需要什么。

### 2.2 哪些类会被 Spring 管理？

只要加了以下注解，Spring 启动时就会创建对象（称为 Bean）并管理：

| 注解 | 含义 | 通常用在哪一层 |
|---|---|---|
| `@Controller` / `@RestController` | 控制器 Bean | Controller 层 |
| `@Service` | 业务 Bean | Service 实现类 |
| `@Repository` | 数据访问 Bean | Mapper 层（MyBatis 自动处理） |
| `@Component` | 通用 Bean | 工具类、监听器等 |
| `@Mapper` | MyBatis Mapper | Mapper 接口 |
| `@Configuration` | 配置类 | 配置类 |

### 2.3 Bean 的生命周期

```text
Spring 启动
  ↓ 扫描所有 @Service、@Controller、@Mapper 等注解
  ↓ 创建对象（调用构造方法）
  ↓ 解决依赖关系（把 A 需要的 B 塞进去）
  ↓ 放到 IoC 容器里（一个 Map：id → 对象）
  ↓ 等待 HTTP 请求或事件触发
  ↓ HTTP 请求来了
  ↓ 从容器里找对象，调用方法
```

### 2.4 为什么感觉"没有 main 方法"？

Spring Boot 应用有一个统一的入口：`DromaraApplication.java` 里的 `main` 方法。

```java
@SpringBootApplication
public class DromaraApplication {
    public static void main(String[] args) {
        SpringApplication.run(DromaraApplication.class, args);
    }
}
```

你只需要运行这个 main 方法，剩下的 Spring 全部帮你搞定。

---

## 三、依赖注入：Spring 怎么把对象塞进来？

### 3.1 什么是依赖注入

依赖注入 = Dependency Injection，DI。

意思是：**类需要的依赖（其他对象），不是由自己创建，而是由外部注入进来。**

### 3.2 RuoYi 中的两种注入方式

#### 方式一：构造方法注入（推荐，RuoYi 主要用）

```java
@Service
@RequiredArgsConstructor
public class PmsTestServiceImpl implements IPmsTestService {
    
    private final PmsTestMapper baseMapper;
    
    // 构造方法由 Lombok 的 @RequiredArgsConstructor 自动生成
}
```

`@RequiredArgsConstructor` 是 Lombok 注解，会自动生成一个构造方法：

```java
public PmsTestServiceImpl(PmsTestMapper baseMapper) {
    this.baseMapper = baseMapper;
}
```

Spring 启动时：

1. 先创建 `PmsTestMapper` 的实现对象。
2. 创建 `PmsTestServiceImpl` 时，把 `PmsTestMapper` 对象作为参数传入构造方法。

#### 方式二：字段注入

```java
@Service
public class PmsTestServiceImpl implements IPmsTestService {
    
    @Autowired
    private PmsTestMapper baseMapper;
}
```

效果一样，但构造方法注入更好，因为：

1. `final` 字段不可变，线程安全。
2. 依赖明确，测试方便。
3. 循环依赖时能更早发现。

### 3.3 接口注入

注意我们写的是：

```java
private final IPmsTestService testService;
```

不是：

```java
private final PmsTestServiceImpl testService;
```

为什么？

- **面向接口编程**：Controller 只依赖 Service 接口，不依赖具体实现。
- Spring 会自动找到 `IPmsTestService` 的唯一实现类 `PmsTestServiceImpl`，注入进来。
- 好处：解耦、方便替换实现、方便单元测试。

---

## 四、分层详解：每一层是干什么的？

### 4.1 Entity（实体层）

**职责**：和数据库表一一对应。

```java
@TableName("pms_test")
public class PmsTest extends BaseEntity {
    @TableId(value = "id")
    private Long id;
    private String name;
    private String remark;
}
```

**什么时候改？**

- 数据库表加字段时，Entity 要加对应字段。
- 修改字段类型时同步修改。

**不改什么？**

- 不加业务逻辑。
- 不加计算字段（那是 VO 的事）。

### 4.2 Mapper（数据访问层）

**职责**：操作数据库。增删改查都这里。

```java
public interface PmsTestMapper extends BaseMapperPlus<PmsTest, PmsTestVo> {
}
```

**BaseMapperPlus 已经提供了什么？**

- `insert(entity)`：插入
- `updateById(entity)`：按 ID 更新
- `deleteById(id)`：按 ID 删除
- `selectById(id)`：按 ID 查询
- `selectVoById(id)`：按 ID 查询并转 VO
- `selectVoList(wrapper)`：条件查询列表并转 VO
- `selectVoPage(page, wrapper)`：分页查询并转 VO

**什么时候改？**

- 简单 CRUD 不需要改，继承 BaseMapperPlus 就够了。
- 复杂 SQL 需要自定义时，写接口方法 + XML 文件。

### 4.3 Service（业务逻辑层）

**职责**：处理业务逻辑，协调多个 Mapper，控制事务。

```java
@Service
public class PmsTestServiceImpl implements IPmsTestService {
    
    private final PmsTestMapper baseMapper;
    
    public PageResult<PmsTestVo> queryPageList(PmsTestBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsTest> lqw = buildQueryWrapper(bo);
        Page<PmsTestVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }
    
    public Boolean insertByBo(PmsTestBo bo) {
        PmsTest entity = MapstructUtils.convert(bo, PmsTest.class);
        return baseMapper.insert(entity) > 0;
    }
}
```

**什么时候改？**

- 新增业务规则时（如保存前校验、保存后记录日志）。
- 需要事务控制时（加 `@Transactional`）。
- 需要调用多个 Mapper 组合数据时。

### 4.4 Controller（控制层）

**职责**：接收 HTTP 请求，调用 Service，返回结果。

```java
@RestController
@RequestMapping("/procurement/test")
public class PmsTestController extends BaseController {
    
    private final IPmsTestService testService;
    
    @SaCheckPermission("procurement:test:list")
    @GetMapping("/list")
    public R<PageResult<PmsTestVo>> list(PmsTestBo bo, PageQuery pageQuery) {
        return R.ok(testService.queryPageList(bo, pageQuery));
    }
}
```

**什么时候改？**

- 新增/修改接口时。
- 调整接口路径、权限字符时。
- 入参校验规则变化时。

**不改什么？**

- 不写业务逻辑，业务逻辑在 Service。
- 不直接调用 Mapper。

### 4.5 BO（Business Object，业务对象）

**职责**：接收前端传参，做校验。

```java
@Data
@AutoMapper(target = PmsTest.class)
public class PmsTestBo extends BaseEntity {
    private Long id;
    private String name;
    private String remark;
}
```

**什么时候改？**

- 新增/修改表单字段变化时。
- 查询条件变化时。

**和 Entity 的区别？**

- Entity = 数据库表结构。
- BO = 业务入参，可能和 Entity 不完全一样。

### 4.6 VO（View Object，视图对象）

**职责**：返回给前端展示的数据。

```java
@Data
@AutoMapper(target = PmsTest.class)
public class PmsTestVo implements Serializable {
    private Long id;
    private String name;
    private String remark;
    private LocalDateTime createTime;
}
```

**什么时候改？**

- 前端列表/详情需要展示更多字段时。
- 需要加计算字段时（如状态名称、创建人姓名）。

**和 Entity 的区别？**

- Entity = 数据库有什么就有什么。
- VO = 前端需要什么就有什么，可以组合多个 Entity 的字段。

### 4.7 分层调用关系

```text
前端 HTTP 请求
  ↓
Controller（接收请求，参数校验）
  ↓
Service（业务逻辑）
  ↓
Mapper（数据库访问）
  ↓
数据库

数据返回：
  数据库 → Mapper → Entity
  Entity → @AutoMapper → VO
  VO → Controller → 前端

数据保存：
  前端 → Controller → BO
  BO → @AutoMapper → Entity
  Entity → Mapper → 数据库
```

---

## 五、测试模块实战教学

你说采购模块代码太多了，不想污染。我们放到 **`ruoyi-demo`** 模块里教学。

### 5.1 为什么要用 ruoyi-demo？

- 它是 RuoYi 自带的示例模块，不会被业务代码污染。
- 已经被 `ruoyi-admin` 依赖，不需要改 pom。
- 适合学习练手。

### 5.2 实战目标

创建一个"测试管理"功能：

- 表：`demo_test`
- 接口前缀：`/demo/test`
- 功能：新增、列表查询
- 权限字符：`demo:test:list`、`demo:test:add`

### 5.3 步骤总览

```text
1. 创建数据库表 demo_test
2. 创建 Entity：DemoTest.java
3. 创建 BO：DemoTestBo.java
4. 创建 VO：DemoTestVo.java
5. 创建 Mapper：DemoTestMapper.java
6. 创建 Service 接口：IDemoTestService.java
7. 创建 Service 实现：DemoTestServiceImpl.java
8. 创建 Controller：DemoTestController.java
9. 前端创建页面：src/views/demo/test/index.vue
10. 后端菜单添加：测试管理 + 新增按钮
```

### 5.4 为什么这是"0 侵入"的？

因为新增一个完整功能：

- 不需要改 RuoYi 核心代码。
- 不需要改现有模块的代码。
- 只需要在自己的包路径下新增文件。
- Spring 会自动扫描到新加的类。

---

## 六、每一层修改指南

以后你要改一个功能，按这个顺序思考：

### 6.1 改字段

```text
数据库表加字段
  ↓
Entity 加字段
  ↓
BO 加字段（如果前端要传）
  ↓
VO 加字段（如果前端要展示）
  ↓
Mapper XML 改 SQL（如果有自定义 SQL）
  ↓
前端表单/表格加字段
```

### 6.2 改接口权限

```text
后端 Controller 改 @SaCheckPermission
  ↓
sys_menu 改权限字符
  ↓
角色管理重新分配权限
  ↓
前端按钮改 v-hasPermi
```

### 6.3 改业务逻辑

```text
改 ServiceImpl 方法
  ↓
如需新依赖，在构造方法里加 Mapper/Service
  ↓
如需事务，加 @Transactional
```

### 6.4 改查询条件

```text
BO 加查询字段
  ↓
ServiceImpl.buildQueryWrapper 加条件
  ↓
前端搜索表单加字段
```

---

## 七、总结

| 问题 | 答案 |
|---|---|
| 为什么全是定义？ | Spring IoC 容器负责创建对象和调用方法，我们只写定义。 |
| 代码在哪里执行？ | 在 Controller/Service/Mapper 的方法里，由 Spring 调用。 |
| 对象怎么来的？ | Spring 通过 `@Service`、`@Controller` 等注解自动创建，通过构造方法注入依赖。 |
| 分层有什么用？ | 职责分离，Controller 管请求、Service 管业务、Mapper 管数据库、BO/VO 管数据转换。 |
| 修改时先改哪？ | 从数据库出发，依次改 Entity → BO → VO → Mapper → Service → Controller → 前端。 |

---

*下一步：我们将在 `ruoyi-demo` 模块中实际创建一个 `DemoTest` 测试功能，手把手走一遍完整流程。*
