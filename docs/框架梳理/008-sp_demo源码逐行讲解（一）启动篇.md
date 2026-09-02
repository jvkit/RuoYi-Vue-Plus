# 008 - sp_demo 源码逐行讲解（一）启动篇

> 目标：看懂 `SpDemoApplication.java` 这一行代码，明白 Spring Boot 到底是怎么跑起来的。
>
> 只讲一个文件，讲透为止。

---

## 一、先别看代码，先看你是怎么启动的

你输入这行命令：

```bash
cd /home/jvkit/workspace/oa/sp_demo
mvn spring-boot:run
```

然后控制台开始滚动，最后出现：

```text
Started SpDemoApplication in 2.304 seconds
sp_demo CLI 已启动
```

**问题：从按下回车，到程序跑起来，中间发生了什么？**

---

## 二、Maven 做了什么

`mvn spring-boot:run` 这条命令，本质上是：

1. **编译代码**：把你写的 `.java` 文件变成 `.class` 字节码。
2. **找启动类**：从 `pom.xml` 里知道项目主类是 `cn.jvkit.spdemo.SpDemoApplication`。
3. **运行主类**：调用这个类的 `main` 方法。

所以核心就是：**运行 `SpDemoApplication.main()`**。

---

## 三、打开启动类

文件位置：

```
src/main/java/cn/jvkit/spdemo/SpDemoApplication.java
```

代码：

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

---

## 四、逐行拆解

### 第 1 行：`package cn.jvkit.spdemo;`

**含义**：这个文件属于 `cn.jvkit.spdemo` 这个包。

**对应目录**：

```
src/main/java/cn/jvkit/spdemo/SpDemoApplication.java
```

包名 = 文件夹路径，只是用点 `.` 代替斜杠 `/`。

> 回忆：因为你有域名 `jvkit.cn`，所以包名前缀是 `cn.jvkit`。

---

### 第 2-3 行：import

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```

**含义**：我要用 Spring Boot 提供的两个类。

- `SpringApplication`：负责启动整个应用。
- `@SpringBootApplication`：一个注解（标签）。

类似 Python 里的 `from spring_boot import SpringApplication`。

---

### 第 5 行：`@SpringBootApplication`

**这是最重要的一个注解。**

它贴在 `SpDemoApplication` 类上，相当于同时贴了三个标签：

| 标签 | 含义 |
|------|------|
| `@Configuration` | 这是一个配置类 |
| `@EnableAutoConfiguration` | 开启自动配置（自动帮你配 Tomcat、JSON 转换等） |
| `@ComponentScan` | 自动扫描当前包及子包下的组件 |

**用大白话说**：

> Spring，请你扫描 `cn.jvkit.spdemo` 这个包下面的所有类。看到带 `@Controller`、`@Service`、`@Component` 标签的，就创建成对象放到容器里。同时，Tomcat、JSON 转换器这些基础设施也自动帮我配好。

---

### 第 6 行：`public class SpDemoApplication`

**含义**：定义一个公开的类，类名叫 `SpDemoApplication`。

`public` 表示这个类可以被其他代码访问。Spring Boot 的启动类必须是 `public`。

---

### 第 8-10 行：main 方法

```java
public static void main(String[] args) {
    SpringApplication.run(SpDemoApplication.class, args);
}
```

这是 Java 程序的**入口**。所有 Java 程序都从 `main` 方法开始执行。

拆解：

| 部分 | 含义 |
|------|------|
| `public static void` | 固定写法，表示这是一个静态、公开、无返回值的方法 |
| `main` | 方法名，JVM 只认这个名字 |
| `String[] args` | 命令行参数，比如 `java -jar xxx.jar --server.port=8081` |
| `SpringApplication.run(...)` | 启动 Spring 应用 |

`SpringApplication.run(SpDemoApplication.class, args)` 做了三件大事：

1. **创建 Spring 容器（IoC 容器）**
   - 这是一个大仓库，用来放所有被 Spring 管理的对象。

2. **扫描组件**
   - 从 `cn.jvkit.spdemo` 开始，递归扫描子包。
   - 发现 `CliRunner`（带 `@Component`）、`UserServiceImpl`（带 `@Service`）、`UserController`（带 `@RestController`）等，就把它们创建成对象放进容器。

3. **启动内嵌 Tomcat**
   - Tomcat 是一个 Web 服务器。
   - Spring Boot 把它内嵌到项目里，不用单独安装。
   - 默认监听 `8080` 端口。

---

## 五、启动流程图

```
你输入 mvn spring-boot:run
        │
        ▼
Maven 编译代码
        │
        ▼
调用 SpDemoApplication.main()
        │
        ▼
SpringApplication.run()
        │
        ├── 1. 创建 Spring 容器（IoC 容器）
        │
        ├── 2. 扫描 cn.jvkit.spdemo 及其子包
        │      发现 @Component / @Service / @RestController
        │      创建对象，放进容器
        │
        ├── 3. 自动配置 Tomcat、JSON 转换器等
        │
        └── 4. 启动 Tomcat，监听 8080 端口
                    │
                    ▼
        控制台显示 Started SpDemoApplication
                    │
                    ▼
              CliRunner 启动交互式命令行
```

---

## 六、为什么 main 方法只有一行

你之前问过："代码里好像都是定义，没有执行？"

答案就在这里。

`main` 方法里的这一行：

```java
SpringApplication.run(SpDemoApplication.class, args);
```

**就是执行的起点。** 它触发了 Spring 框架里的一大堆执行逻辑。

你的代码（`HelloController`、`UserServiceImpl`、`CliRunner`）本质上是"声明"：

> 我声明这个类是一个 Controller。  
> 我声明这个类是一个 Service。  
> 我声明这个类要在启动后运行。

Spring 读到这些声明后，在启动过程中自动执行：

- 创建对象
- 注入依赖
- 绑定 URL
- 启动 CLI

**你的代码写"规则"，Spring 负责"执行"。**

---

## 七、这节课的关键结论

1. **`SpDemoApplication.java` 是整个项目的入口。**
2. **`@SpringBootApplication` 让 Spring 自动扫描并配置项目。**
3. **`SpringApplication.run()` 做了三件事：建容器、扫组件、启 Tomcat。**
4. **你的代码主要是"声明"，Spring 负责"执行"。**
5. **Tomcat 是内嵌的，不需要单独安装。**

---

## 八、思考题

看下面这行代码：

```java
SpringApplication.run(SpDemoApplication.class, args);
```

问题：

1. 为什么第一个参数是 `SpDemoApplication.class`，而不是 `new SpDemoApplication()`？
2. 如果把 `SpDemoApplication` 放到 `cn.jvkit.spdemo.old` 包里，启动时会发生什么？
3. 启动类里的 `@SpringBootApplication` 可以去掉吗？去掉会怎样？

**想不明白没关系，下一单元讲 `@Service`、`@Autowired` 和依赖注入时，这些问题会更清楚。**

---

## 九、对应代码位置

- 启动类：`sp_demo/src/main/java/cn/jvkit/spdemo/SpDemoApplication.java`
- 配置文件：`sp_demo/src/main/resources/application.yml`

---

**下一单元预告**：`UserServiceImpl.java` —— `@Service` 是什么意思？Spring 怎么创建这个对象？为什么它只会创建一次？
