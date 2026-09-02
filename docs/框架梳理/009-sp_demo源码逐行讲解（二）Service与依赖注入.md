# 009 - sp_demo 源码逐行讲解（二）Service 与依赖注入

> 目标：看懂 `UserService.java`、`UserServiceImpl.java` 和 `UserController.java` 是怎么协作的。
>
> 重点理解：为什么有接口？为什么用 `@Service`？Spring 怎么把对象塞进来？

---

## 一、接上节课

上节课我们说 Spring 启动时会做三件事：

1. 创建 IoC 容器
2. **扫描组件**
3. 启动 Tomcat

这节课就讲第二步里最重要的一类组件：**`@Service` 业务层**。

---

## 二、先看文件结构

```
src/main/java/cn/jvkit/spdemo/
├── service/
│   ├── UserService.java              ← 接口
│   └── impl/
│       └── UserServiceImpl.java      ← 实现类
└── controller/
    └── UserController.java           ← 使用方
```

为什么不是直接一个 `UserService.java` 类，而要分成**接口 + 实现类**？

这是 Java 企业项目的标准写法。下面拆开讲。

---

## 三、接口：UserService.java

代码：

```java
package cn.jvkit.spdemo.service;

import cn.jvkit.spdemo.entity.User;
import java.util.List;

public interface UserService {

    List<User> list();
    User getById(Long id);
    Long add(User user);
    void update(Long id, User user);
    void delete(Long id);

}
```

### 3.1 什么是接口

接口就是一份**合同/规范**：

> 凡是实现 `UserService` 的类，都必须提供这 5 个方法。

接口里只有方法签名，没有具体实现。

### 3.2 为什么要先定义接口

想象你是一家公司老板，你找乙方做网站。

- 你不会直接说"怎么实现我不管"，你会先写一份**需求文档**：要有登录、注册、查询功能。
- 乙方按需求文档做具体实现。

在代码里：

- `UserService` 接口 = 需求文档
- `UserServiceImpl` = 乙方的具体实现
- `UserController` = 甲方，只关心能不能调用功能，不关心你怎么做

**好处：甲方（Controller）和乙方（Service 实现）解耦。**

---

## 四、实现类：UserServiceImpl.java

代码：

```java
package cn.jvkit.spdemo.service.impl;

import cn.jvkit.spdemo.entity.User;
import cn.jvkit.spdemo.service.UserService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(2);

    public UserServiceImpl() {
        userStore.put(1L, new User(1L, "张三", 20));
        userStore.put(2L, new User(2L, "李四", 25));
        System.out.println("UserServiceImpl 被创建了，只会创建一次（单例）");
    }

    @Override
    public List<User> list() {
        return new ArrayList<>(userStore.values());
    }

    @Override
    public User getById(Long id) {
        return userStore.get(id);
    }

    @Override
    public Long add(User user) {
        Long id = idGenerator.incrementAndGet();
        user.setId(id);
        userStore.put(id, user);
        return id;
    }

    @Override
    public void update(Long id, User user) {
        user.setId(id);
        userStore.put(id, user);
    }

    @Override
    public void delete(Long id) {
        userStore.remove(id);
    }
}
```

### 4.1 第 13 行：`@Service`

**含义**：这个类是一个业务逻辑类，请 Spring 把它创建成 Bean（对象），放到 IoC 容器里。

Spring 扫描到 `@Service` 后，会：

1. 调用 `UserServiceImpl()` 构造方法，创建一个对象。
2. 把这个对象放进 IoC 容器，名字叫 `userServiceImpl`（类名首字母小写）。
3. 整个应用运行期间，这个对象只创建一次。

### 4.2 第 14 行：`implements UserService`

**含义**：这个类实现了 `UserService` 接口。

因为实现了接口，所以必须提供接口里所有方法的具体实现。

### 4.3 第 16-17 行：内存数据存储

```java
private final Map<Long, User> userStore = new ConcurrentHashMap<>();
private final AtomicLong idGenerator = new AtomicLong(2);
```

- `userStore`：一个内存里的 HashMap，模拟数据库。
- `idGenerator`：自增 ID 生成器。
- `final`：表示这个引用一旦赋值就不能再改变。

> 注意：这里的数据存在内存里，程序一关就没了。后面接 MySQL 时，会把 `Map` 换成数据库操作。

### 4.4 第 19-24 行：构造方法

```java
public UserServiceImpl() {
    userStore.put(1L, new User(1L, "张三", 20));
    userStore.put(2L, new User(2L, "李四", 25));
    System.out.println("UserServiceImpl 被创建了，只会创建一次（单例）");
}
```

构造方法在 Spring 创建对象时调用。

你启动项目时，控制台只会打印一次：

```text
UserServiceImpl 被创建了，只会创建一次（单例）
```

这说明 Spring 只创建了一个 `UserServiceImpl` 对象。**这叫单例模式（Singleton）。**

### 4.5 `@Override`

```java
@Override
public List<User> list() {
    return new ArrayList<>(userStore.values());
}
```

`@Override` 表示：这个方法是**重写接口里的方法**。

写不写 `@Override` 不影响运行，但写上后：
- 代码更清晰。
- 编译器会帮你检查是否真的重写了接口方法（如果你方法名写错了，会报错）。

---

## 五、使用方：UserController.java

代码（只看关键部分）：

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<User>> list() {
        return Result.ok(userService.list());
    }

    @PostMapping
    public Result<Long> add(@RequestBody User user) {
        Long id = userService.add(user);
        return Result.ok(id);
    }
}
```

### 5.1 为什么声明的是接口，不是实现类

```java
private final UserService userService;
```

注意：这里是 `UserService`，不是 `UserServiceImpl`。

Controller 说：**我需要一个能处理用户业务的对象，具体是谁实现的我不关心。**

这就是**面向接口编程**。

### 5.2 构造方法注入

```java
public UserController(UserService userService) {
    this.userService = userService;
}
```

**问题：这个 `userService` 是谁传进来的？**

答案：**Spring 自动传进来的。**

Spring 启动时：

1. 先创建 `UserServiceImpl` 对象（因为它有 `@Service`）。
2. 再创建 `UserController` 对象（因为它有 `@RestController`）。
3. 发现 `UserController` 的构造方法需要一个 `UserService` 类型的参数。
4. Spring 在容器里找：谁实现了 `UserService` 接口？找到 `UserServiceImpl`。
5. 把 `UserServiceImpl` 对象作为参数，传给 `UserController` 的构造方法。

**这就是依赖注入（Dependency Injection，简称 DI）。**

用图表示：

```
IoC 容器
├── UserServiceImpl 对象  ← 实现了 UserService 接口
└── UserController 对象    ← 构造时需要 UserService

Spring 发现后：
UserController.userService = UserServiceImpl 对象
```

### 5.3 为什么用构造方法注入

构造方法注入是 Spring **官方推荐**的方式。

好处：
- **依赖明确**：从构造方法一眼看出这个类需要什么。
- `final` 修饰：对象创建后不能改，更安全。
- **方便测试**：测试时可以手动传一个假的 `UserService`。

---

## 六、请求来了之后怎么流转

以 `/users` 这个 HTTP 请求为例：

```
浏览器/curl 访问 http://localhost:8080/users
        │
        ▼
Tomcat 收到请求
        │
        ▼
找到 @RequestMapping("/users") 对应的 UserController
        │
        ▼
调用 UserController.list()
        │
        ▼
userService.list()   ← 这里调用的是 UserServiceImpl.list()
        │
        ▼
返回 List<User>
        │
        ▼
Spring 自动转成 JSON 返回给浏览器
```

CLI 命令 `/users` 的流转类似，只是入口从 HTTP 变成了 `CliRunner`：

```
你在控制台输入 /users
        │
        ▼
CliRunner 解析命令
        │
        ▼
调用 userService.list()
        │
        ▼
打印结果到控制台
```

**关键发现**：

> 无论是 HTTP 还是 CLI，最终都调用同一个 `UserService.list()`。业务逻辑只写一次，可以被多种入口复用。

---

## 七、单例再强调

你在控制台看到：

```text
UserServiceImpl 被创建了，只会创建一次（单例）
```

只打印一次，证明整个应用只有一个 `UserServiceImpl` 对象。

这意味着：
- HTTP 请求用的是这个对象。
- CLI 命令用的也是这个对象。
- 内存里的 `userStore` 是同一个 Map，所以 HTTP 新增的用户，CLI 能查到；反过来也一样。

**你可以验证**：

```bash
# 1. 启动项目
mvn spring-boot:run

# 2. 在 CLI 里添加用户
sp_demo> /add 王五 30

# 3. 另开一个终端，用 curl 查 HTTP 接口
curl http://localhost:8080/users
```

你会看到刚添加的 `王五` 也出现在 HTTP 返回里，因为用的是同一个对象。

---

## 八、本课关键结论

1. **接口定义规范，实现类提供具体逻辑。**
2. **`@Service` 让 Spring 创建实现类的单例对象。**
3. **Controller 里注入接口，不依赖具体实现类。**
4. **构造方法注入 = Spring 自动把需要的对象传进来。**
5. **HTTP 和 CLI 共享同一个 Service 对象，业务逻辑只写一次。**

---

## 九、思考题

1. 如果把 `UserController` 里的 `UserService` 改成 `UserServiceImpl`，代码还能跑吗？有什么坏处？
2. 为什么 `UserServiceImpl` 的构造方法只执行一次？
3. 如果让你再写一个 `OrderService`，你会怎么设计它的接口和实现类？

---

## 十、对应代码位置

- 接口：`sp_demo/src/main/java/cn/jvkit/spdemo/service/UserService.java`
- 实现类：`sp_demo/src/main/java/cn/jvkit/spdemo/service/impl/UserServiceImpl.java`
- 使用方：`sp_demo/src/main/java/cn/jvkit/spdemo/controller/UserController.java`

---

**下一单元预告**：`UserController.java` —— HTTP 请求是怎么映射到方法的？`@GetMapping`、`@PostMapping`、`@RequestBody` 到底在干嘛？
