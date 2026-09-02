# sp_demo - Spring Boot 学习项目

这是一个最小化的 Spring Boot 学习项目，用于理解 Java 后端的基础概念。

**特点：**
- 同时提供 **HTTP 接口** 和 **交互式 CLI** 两种测试方式
- 不依赖数据库，数据存在内存里，启动即用
- 代码量少，适合一步一步看

---

## 一、环境要求

- Java 21（`java -version` 查看）
- Maven 3.8+（`mvn -v` 查看）

---

## 二、启动方式

### 方式 1：用 Maven 直接启动（推荐，开发时用）

```bash
cd /home/jvkit/workspace/oa/sp_demo
mvn spring-boot:run
```

### 方式 2：先打包，再运行 jar

```bash
cd /home/jvkit/workspace/oa/sp_demo
mvn package -DskipTests
java -jar target/sp-demo-1.0.0.jar
```

---

## 三、启动后你会看到什么

启动成功后，控制台会显示：

```
==============================================
  sp_demo CLI 已启动
  输入 /help 查看所有命令
  同时 HTTP 服务也在 http://localhost:8080 运行
==============================================

sp_demo>
```

这表示两件事都准备好了：
1. **CLI 交互命令行**：在 `sp_demo>` 后面输入命令即可测试后端。
2. **HTTP 服务**：打开浏览器访问 `http://localhost:8080` 可以测接口。

---

## 四、CLI 斜杠命令

在 `sp_demo>` 提示符后输入以下命令：

| 命令 | 说明 | 示例 |
|------|------|------|
| `/help` | 显示所有命令 | `/help` |
| `/users` | 列出所有用户 | `/users` |
| `/user <id>` | 查询指定用户 | `/user 1` |
| `/add <name> <age>` | 新增用户 | `/add 王五 30` |
| `/update <id> <name> <age>` | 更新用户 | `/update 1 张三 21` |
| `/delete <id>` | 删除用户 | `/delete 1` |
| `/hello` | 打个招呼 | `/hello` |
| `/exit` | 退出程序 | `/exit` |

### 一次完整的 CLI 测试流程

```
sp_demo> /users
  [1] 张三，年龄：20
  [2] 李四，年龄：25

sp_demo> /add 王五 30
新增成功，id = 3

sp_demo> /users
  [1] 张三，年龄：20
  [2] 李四，年龄：25
  [3] 王五，年龄：30

sp_demo> /user 3
  [3] 王五，年龄：30

sp_demo> /delete 3
删除成功：3

sp_demo> /exit
再见！
```

---

## 五、HTTP 接口测试

如果你更喜欢用浏览器、Postman 或 curl，也可以用这些接口：

### 5.1 Hello World

```bash
curl http://localhost:8080/hello/world
```

### 5.2 查询所有用户

```bash
curl http://localhost:8080/users
```

### 5.3 查询单个用户

```bash
curl http://localhost:8080/users/1
```

### 5.4 新增用户

```bash
curl -X POST http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"王五","age":30}'
```

### 5.5 更新用户

```bash
curl -X PUT http://localhost:8080/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"张三","age":21}'
```

### 5.6 删除用户

```bash
curl -X DELETE http://localhost:8080/users/1
```

---

## 六、项目结构

```
sp_demo/
├── pom.xml                                 # Maven 配置
├── README.md                               # 本文件
└── src/main/
    ├── java/cn/jvkit/spdemo/               # Java 源代码
    │   ├── SpDemoApplication.java          # 启动类
    │   ├── cli/
    │   │   └── CliRunner.java              # 交互式 CLI
    │   ├── common/
    │   │   └── Result.java                 # 统一返回结果
    │   ├── controller/
    │   │   ├── HelloController.java        # Hello World 示例
    │   │   └── UserController.java         # 用户管理 HTTP 接口
    │   ├── entity/
    │   │   └── User.java                   # 用户实体类
    │   └── service/
    │       ├── UserService.java            # 用户业务接口
    │       └── impl/
    │           └── UserServiceImpl.java    # 用户业务实现类
    └── resources/
        └── application.yml                 # 配置文件
```

---

## 七、常见问题

### 7.1 端口 8080 被占用

如果报错 `Port 8080 was already in use`，说明有其他程序占用了 8080 端口。

查看占用进程：

```bash
ss -tlnp | grep 8080
```

停止它（把 `<pid>` 换成实际的进程号）：

```bash
kill <pid>
```

或者改端口，编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8081
```

### 7.2 CLI 不显示提示符

CLI 必须在**前台运行**才能交互。如果你用 `&` 放到后台，或者用 `nohup`，就无法输入命令。

正确做法：

```bash
mvn spring-boot:run
```

### 7.3 数据重启后消失

因为现在数据存在内存里，程序一关就没了。这是故意的，方便学习。后面接 MySQL 就会持久化。

---

## 八、下一步学习

看懂这个项目后，可以继续：

1. 把内存里的 `Map` 换成 MySQL 数据库（MyBatis-Plus）。
2. 增加参数校验、异常处理。
3. 理解 RuoYi 里的 `BO`、`VO`、`Mapper` 是怎么对应的。

对应文档在：

- `docs/框架梳理/005-Java与SpringBoot基础学习.md`
- `docs/框架梳理/006-IoC与依赖注入实战教学.md`
