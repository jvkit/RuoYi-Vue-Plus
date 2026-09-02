# 005 - Java 与 Spring Boot 基础学习

> 目标：从零基础开始，理解我们项目里每天打交道的 Java + Spring Boot 代码到底在干什么。不需要背下来，先建立"地图"。

---

## 一、写在前面：为什么你看得懂 Python 却看不懂 Java

Python 是"脚本语言"：你写 `print("hello")`，保存成 `.py`，直接运行，解释器一行一行执行。

Java 是"编译型语言"：你写 `.java` 文件，先被编译成 `.class`（字节码），再被 JVM（Java 虚拟机）执行。Spring Boot 本质上就是一大堆 Java 类 + 一个帮你自动配置项目的框架。

我们项目（RuoYi-Vue-Plus / ruoyi-6x）的后端，看起来文件很多，但其实**结构非常规律**：

```
entity      数据表映射 → Java 对象
bo          业务对象（接收前端传参）
vo          视图对象（返回给前端）
mapper      数据库访问层
service     业务逻辑层
controller  接口层（对外暴露 HTTP 接口）
```

这一篇先不讲数据库，先从"一个类怎么变成 HTTP 接口"讲起。

---

## 二、Java 最基础概念（5 分钟版）

### 2.1 类（Class）= 模板

```java
public class User {
    // 属性（成员变量）
    private Long id;
    private String name;
    private Integer age;

    // 构造方法：new User(1L, "张三", 20) 时调用
    public User(Long id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // 普通方法
    public String sayHello() {
        return "你好，我是" + this.name;
    }
}
```

**类是模板，对象是实例。** 就像"汽车设计图"和"停车场里那辆特斯拉"。

### 2.2 对象（Object）= 类的实例

```java
User user = new User(1L, "张三", 20);
System.out.println(user.sayHello());
```

`user` 就是一个对象，它在内存里真实存在。

### 2.3 接口（Interface）= 约定

```java
public interface Animal {
    void speak();
}
```

接口只定义"要做什么"，不定义"怎么做"。谁实现（`implements`）了接口，谁就必须提供具体方法。

```java
public class Dog implements Animal {
    @Override
    public void speak() {
        System.out.println("汪汪");
    }
}
```

**为什么要用接口？** 方便替换实现。比如 `UserService` 接口可以由 `UserServiceImpl` 实现，Spring 帮你自动选择用哪个实现。

### 2.4 包（Package）= 文件夹

`package cn.jvkit.spdemo.controller;` 就是文件路径 `cn/jvkit/spdemo/controller/`。作用是避免类名冲突、组织代码。

### 2.5 访问修饰符

| 修饰符 | 同一类 | 同一包 | 子类 | 任何地方 |
|--------|--------|--------|------|----------|
| public | ✓ | ✓ | ✓ | ✓ |
| protected | ✓ | ✓ | ✓ | ✗ |
| 默认（不写）| ✓ | ✓ | ✗ | ✗ |
| private | ✓ | ✗ | ✗ | ✗ |

**常见习惯**：属性用 `private`，通过 `getter/setter` 访问；方法根据情况用 `public` 或 `private`。

---

## 三、Maven 是做什么的

Maven 是 Java 项目的"管家"：

1. **管理依赖**：你要用 Spring Boot？在 `pom.xml` 里写几行，Maven 自动去中央仓库下载 jar 包。
2. **管理编译**：`mvn package` 一键编译、打包。
3. **约定目录结构**：

```
sp_demo/
├── pom.xml                          # Maven 配置文件
├── src/
│   ├── main/
│   │   ├── java/                    # Java 源代码
│   │   │   └── cn/jvkit/spdemo/
│   │   └── resources/               # 配置文件、静态资源
│   └── test/                        # 测试代码
└── target/                          # 编译产物（自动生成）
```

常用命令：

```bash
mvn package -DskipTests     # 编译并打包，跳过测试
mvn clean package -DskipTests   # 清理后重新打包
mvn spring-boot:run         # 直接运行 Spring Boot 应用
```

---

## 四、第一个 Spring Boot 程序：Hello World

### 4.1 启动类

```java
package cn.jvkit.spdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpDemoApplication.class, args);
    }
}
```

**这行 `main` 方法是整个程序的入口。** 执行它时，Spring Boot 会做三件事：

1. 启动 Spring 容器（IoC 容器）。
2. 扫描 `cn.jvkit.spdemo` 包及其子包下的所有组件。
3. 启动内嵌的 Tomcat，监听 `application.yml` 里配置的端口。

### 4.2 `@SpringBootApplication` 是什么

它是一个**组合注解**，相当于同时写了：

- `@Configuration`：这是一个配置类。
- `@EnableAutoConfiguration`：开启自动配置（比如自动配置 Tomcat、Jackson）。
- `@ComponentScan`：自动扫描当前包及子包下的 `@Component`、`@Service`、`@Controller` 等。

> 注解 = 贴在类/方法/属性上的"标签"，Spring 看到标签就知道该怎么处理。

### 4.3 第一个接口

```java
package cn.jvkit.spdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping("/world")
    public String helloWorld() {
        return "Hello, Spring Boot!";
    }
}
```

启动后访问 `http://localhost:8080/hello/world`，就能看到：

```
Hello, Spring Boot!
```

---

## 五、常用注解速查

| 注解 | 作用 |
|------|------|
| `@SpringBootApplication` | 启动类组合注解 |
| `@RestController` | 声明这是一个 REST 接口类（返回数据，不是页面） |
| `@Controller` | 声明这是一个控制器类（通常返回页面） |
| `@RequestMapping("/xxx")` | 给类或方法指定 URL 前缀/路径 |
| `@GetMapping("/xxx")` | 处理 GET 请求 |
| `@PostMapping("/xxx")` | 处理 POST 请求 |
| `@PutMapping("/xxx")` | 处理 PUT 请求 |
| `@DeleteMapping("/xxx")` | 处理 DELETE 请求 |
| `@Service` | 声明这是一个业务逻辑类，交给 Spring 管理 |
| `@Component` | 通用组件注解，交给 Spring 管理 |
| `@Autowired` | 自动注入依赖（Spring 帮你找对应的对象） |
| `@RequestBody` | 把请求体的 JSON 转成 Java 对象 |
| `@PathVariable` | 从 URL 路径里取参数 |
| `@RequestParam` | 从 URL 查询参数里取参数 |

---

## 六、核心概念：IoC 容器与依赖注入

这是你之前最困惑的地方：**代码里好像都是定义，没有执行？**

### 6.1 什么是 IoC（控制反转）

传统写法：

```java
public class OrderController {
    // 自己 new 一个 service
    private OrderService orderService = new OrderServiceImpl();
}
```

Spring 写法：

```java
public class OrderController {
    @Autowired
    private OrderService orderService;  // Spring 帮你创建并塞进来
}
```

**控制反转**：本来由你自己 `new` 对象，现在交给 Spring 容器来创建、管理、分配。你只管"声明我需要什么"。

### 6.2 什么是依赖注入（DI）

Spring 启动时，会扫描所有带 `@Service`、`@Component`、`@RestController` 的类，把它们创建成**单例对象**放进容器里。

当你在某处写了 `@Autowired`，Spring 就从容器里找到对应的对象，**注入**进来。

```java
@Service
public class UserService {
    public String getName() {
        return "张三";
    }
}

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/name")
    public String getName() {
        return userService.getName();
    }
}
```

执行流程：

1. Spring 启动，创建 `UserService` 对象。
2. Spring 创建 `UserController` 对象。
3. Spring 发现 `UserController` 里需要 `UserService`，把第 1 步创建的对象塞进去。
4. 请求 `/user/name` 时，调用 `UserController.getName()`，内部调用 `UserService.getName()`。

### 6.3 为什么这样好

- **解耦**：Controller 不依赖具体实现，只依赖接口。
- **方便替换**：今天用 `UserServiceImplA`，明天改成 `UserServiceImplB`，只需改一处注解。
- **方便测试**：可以给 `UserController` 注入一个假的 `UserService`。

### 6.4 "代码里好像没有执行"的真相

你的感觉是对的：**业务代码确实主要是"声明"**。

- 类 = 声明一个模板
- 注解 = 声明这个类交给 Spring 管
- `@Autowired` = 声明我需要某个对象
- Controller 方法 = 声明某个 URL 对应什么处理

**真正的执行由 Spring 框架在启动时和请求到达时完成**：

- Spring 扫描 → 创建对象 → 注入依赖 → 启动 Tomcat
- 请求到达 → Tomcat 找到对应 Controller → 调用方法 → 返回结果

你的代码是"规则"，Spring 是"执行引擎"。

---

## 七、Controller 如何接收 HTTP 请求

### 7.1 接收路径参数

```java
@GetMapping("/user/{id}")
public User getById(@PathVariable Long id) {
    return new User(id, "张三", 20);
}
```

访问 `/user/100`，`id` 就是 `100`。

### 7.2 接收查询参数

```java
@GetMapping("/users")
public List<User> list(@RequestParam(required = false) String name) {
    // 访问 /users?name=张三
    return userService.list(name);
}
```

### 7.3 接收 JSON 请求体

```java
@PostMapping("/user")
public String add(@RequestBody User user) {
    // 前端传 {"id":1,"name":"张三","age":20}
    // Spring 自动转成 User 对象
    return "添加成功：" + user.getName();
}
```

### 7.4 组合使用

```java
@PutMapping("/user/{id}")
public String update(@PathVariable Long id, @RequestBody User user) {
    return "更新 ID=" + id + " 的用户：" + user.getName();
}
```

---

## 八、返回 JSON 数据

`@RestController` 已经默认把所有返回值序列化成 JSON。所以：

```java
@GetMapping("/user/json")
public User getUser() {
    return new User(1L, "张三", 20);
}
```

浏览器看到的就是：

```json
{
  "id": 1,
  "name": "张三",
  "age": 20
}
```

### 统一返回结果

实际项目中通常会包装一个统一响应体：

```java
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;
}
```

```java
@GetMapping("/user/result")
public Result<User> getUserResult() {
    User user = new User(1L, "张三", 20);
    return Result.ok(user);
}
```

---

## 九、分层：Controller → Service

这是我们项目最核心的结构。

### 9.1 为什么分层

| 层 | 职责 | 举例 |
|----|------|------|
| Controller | 接收请求、返回响应、参数校验 | `@RestController` |
| Service | 业务逻辑 | 计算金额、判断状态、调用多个 Mapper |
| Mapper/DAO | 数据库操作 | 增删查改 |
| Entity | 数据表映射 | 一个类对应一张表 |

**原则**：Controller 薄，Service 厚。Controller 只负责"接进来、传下去、返回去"，复杂逻辑放 Service。

### 9.2 示例

```java
// Controller
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<Long> create(@RequestBody Order order) {
        Long id = orderService.create(order);
        return Result.ok(id);
    }
}
```

```java
// Service 接口
public interface OrderService {
    Long create(Order order);
}
```

```java
// Service 实现
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Long create(Order order) {
        // 业务逻辑：计算总价、校验库存等
        order.setTotalPrice(order.getPrice() * order.getQuantity());
        orderMapper.insert(order);
        return order.getId();
    }
}
```

---

## 十、实战：内存中的 CRUD

在 `sp_demo` 项目中，我们已经写了一个 `HelloController`。下一步请跟着 `006-IoC与依赖注入实战教学.md` 继续，我们会：

1. 创建一个 `UserService`，用 `Map` 模拟数据库。
2. 创建一个 `UserController`，实现增删查改。
3. 体会 "Controller 调用 Service，Service 处理数据" 的分层感觉。

---

## 十一、关键问题自查清单

读完后请确认自己能否回答：

- [ ] Java 里"类"和"对象"的区别是什么？
- [ ] `@SpringBootApplication` 内部包含哪些注解？
- [ ] 为什么 `main` 方法里只写了一行 `SpringApplication.run` 就能启动整个项目？
- [ ] `@Autowired` 是做什么的？
- [ ] `@RestController` 和 `@Controller` 的区别？
- [ ] `@RequestBody`、`@PathVariable`、`@RequestParam` 分别用于什么场景？
- [ ] 为什么 Controller 方法返回值会自动变成 JSON？

---

## 十二、本课代码位置

- 启动类：`sp_demo/src/main/java/cn/jvkit/spdemo/SpDemoApplication.java`
- Hello 接口：`sp_demo/src/main/java/cn/jvkit/spdemo/controller/HelloController.java`
- 配置文件：`sp_demo/src/main/resources/application.yml`

运行方式：

```bash
cd /home/jvkit/workspace/oa/sp_demo
mvn spring-boot:run
```

或：

```bash
cd /home/jvkit/workspace/oa/sp_demo
mvn package -DskipTests
java -jar target/sp-demo-1.0.0.jar
```

访问测试：

```bash
curl http://localhost:8080/hello/world
```

---

**下一篇**：[006 - IoC 与依赖注入实战教学](006-IoC与依赖注入实战教学.md)
